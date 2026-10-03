@file:Suppress("DEPRECATION")

package com.houvven.guise.xposed.hook.location

import android.location.GnssStatus
import android.location.GpsStatus
import android.location.GpsStatus.GPS_EVENT_FIRST_FIX
import android.location.GpsStatus.GPS_EVENT_STARTED
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.afterHookAllMethods
import com.houvven.ktx_xposed.hook.afterHookedMethod
import com.houvven.ktx_xposed.hook.beforeHookAllMethods
import com.houvven.ktx_xposed.hook.beforeHookConstructor
import com.houvven.ktx_xposed.hook.beforeHookedMethod
import com.houvven.ktx_xposed.hook.beforeHookSomeSameNameMethod
import com.houvven.ktx_xposed.hook.callMethod
import com.houvven.ktx_xposed.hook.findClassIfExists
import com.houvven.ktx_xposed.hook.findMethodExactIfExists
import com.houvven.ktx_xposed.hook.setAllMethodResult
import com.houvven.ktx_xposed.hook.setMethodResult
import com.houvven.ktx_xposed.hook.setSomeSameNameMethodResult
import com.houvven.ktx_xposed.logger.XposedLogger
import java.lang.reflect.Constructor
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer


/**
 * Spoofs the device location.
 *
 * The design goal, inherited from the original (working) Guise, is that the fake fix must
 * not depend on any real location source. The target app may run with GPS cold, Wi-Fi
 * scanning hidden and cell identities hidden all at once; under those conditions the
 * system produces no fix and never calls back, so a hook that only *rewrites* incoming
 * locations has nothing to rewrite and the spoof silently does nothing. (Observed on
 * Baidu LocSDK 8.x: the `:remote` process gave up with locType=62 — "no valid location
 * basis" — 339 ms after start.)
 *
 * So, like the original, we *push* the fake fix into the app's own listeners the moment
 * they register, instead of waiting for the framework. Rewriting is still installed as a
 * second line of defense for the paths that do call back.
 */
@Suppress("DEPRECATION")
class LocationHook : LoadPackageHandler, LocationHookBase() {


    private var latitude = config.latitude
    private var longitude = config.longitude

    // Pretend a healthy GPS constellation is being tracked. Apps that gate on "are there
    // any satellites?" before trusting a fix would otherwise reject the spoofed location.
    private val svCount = 5
    private val satelliteIds = intArrayOf(1, 2, 3, 4, 5)
    private val svidWithFlags = satelliteIds.map { svid ->
        (svid shl SVID_SHIFT_WIDTH) or
            (GnssStatus.CONSTELLATION_GPS shl CONSTELLATION_TYPE_SHIFT_WIDTH) or
            SVID_FLAGS_FULLY_USABLE
    }.toIntArray()
    private val cn0s = floatArrayOf(0F, 0F, 0F, 0F, 0F)
    private val elevations = cn0s.clone()
    private val azimuths = cn0s.clone()

    /** Listener classes whose delivery methods have already been hooked. */
    private val hookedListenerClasses =
        Collections.synchronizedSet(mutableSetOf<Class<*>>())

    /** Listeners receiving a periodic fake fix, mapped to the handler that feeds them. */
    private val activeDeliveries =
        Collections.synchronizedMap(mutableMapOf<LocationListener, Handler>())

    /**
     * Guards the one-shot "a fake fix actually reached the app" log. Whether a hook was
     * *installed* and whether it ever *fired* are different questions, and only the
     * second one proves the spoof works — especially in a separate location process.
     */
    private val deliveryReported = AtomicBoolean()

    override fun onHook() {
        val spoofCoordinates = longitude != UNSET_COORDINATE || latitude != UNSET_COORDINATE
        if (spoofCoordinates) {
            if (config.randomOffset) {
                if (latitude != UNSET_COORDINATE) latitude += randomOffset()
                if (longitude != UNSET_COORDINATE) longitude += randomOffset()
            }
            log("Spoofing coordinates lat=$latitude lon=$longitude")
            fakeCoordinates()
            fakeProviderState()
            hookFrameworkLocationCallbacks()
            hookLocationRequestListeners()
            hookCurrentLocationRequests()
            hookKnownSdkLocations()
            fakeGnssSatellites()
            if (hasCompleteCoordinates()) setLastLocation()
        }
        if (config.makeWifiLocationFail) makeWifiLocationFail()
        if (config.makeCellLocationFail) makeCellLocationFail()
    }

    private fun fakeCoordinates() {
        Location::class.java.run {
            if (longitude != UNSET_COORDINATE) {
                setMethodResult("getLongitude", longitude)
                beforeHookedMethod("setLongitude", Double::class.javaPrimitiveType!!) { param ->
                    param.args[0] = longitude
                }
            }
            if (latitude != UNSET_COORDINATE) {
                setMethodResult("getLatitude", latitude)
                beforeHookedMethod("setLatitude", Double::class.javaPrimitiveType!!) { param ->
                    param.args[0] = latitude
                }
            }
        }
    }

    /**
     * Reports location as available but collapses the provider set to GPS only: GPS is
     * reported enabled, while NETWORK / FUSED / PASSIVE are reported unavailable. This
     * funnels the app onto the one provider we actively push the fake fix into and cuts
     * off the real network/fused location side-channels. Mirrors the original Guise.
     */
    private fun fakeProviderState() {
        LocationManager::class.java.run {
            setAllMethodResult("isLocationEnabled", true)
            setAllMethodResult("isLocationEnabledForUser", true)
            beforeHookSomeSameNameMethod(
                "isProviderEnabled",
                "isProviderEnabledForUser",
                "hasProvider",
            ) { param ->
                when (param.args.getOrNull(0) as? String) {
                    LocationManager.GPS_PROVIDER -> param.result = true
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.FUSED_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER,
                    -> param.result = false
                }
            }
            setAllMethodResult("getBestProvider", LocationManager.GPS_PROVIDER)
            setSomeSameNameMethodResult(
                "getProviders",
                "getAllProviders",
                value = listOf(LocationManager.GPS_PROVIDER),
            )
        }
    }

    private fun hookFrameworkLocationCallbacks() {
        val transports = FRAMEWORK_LOCATION_TRANSPORTS.mapNotNull { name ->
            val clazz = findClassIfExists(name)
            if (clazz == null) log("Framework transport absent: $name")
            clazz
        }
        if (transports.isEmpty()) {
            // Expected on releases where the framework renamed these internals; the
            // listener-class hook below is what actually covers delivery there.
            log("No framework location transport matched, relying on listener hooks")
        }
        transports.forEach { transport ->
            transport.beforeHookAllMethods("onLocationChanged") { param ->
                param.args.forEach(::rewriteLocations)
            }
        }
    }

    /**
     * Hooks the target app's own [LocationListener] implementations at registration time
     * *and* starts pushing the fake fix into them — the core of the fix. See the class
     * doc for why rewriting alone is not enough once location identifiers are hidden.
     */
    private fun hookLocationRequestListeners() {
        LocationManager::class.java.methods
            .asSequence()
            .filter { it.name in LOCATION_REQUEST_METHODS }
            .filter { method ->
                method.parameterTypes.any { LocationListener::class.java.isAssignableFrom(it) }
            }
            .forEach { method ->
                val listenerIndex = method.parameterTypes.indexOfFirst {
                    LocationListener::class.java.isAssignableFrom(it)
                }
                val isSingle = method.name == "requestSingleUpdate"
                LocationManager::class.java.afterHookedMethod(
                    method.name,
                    *method.parameterTypes,
                ) { param ->
                    val listener = param.args.getOrNull(listenerIndex) as? LocationListener
                        ?: return@afterHookedMethod
                    hookLocationListenerClass(listener.javaClass)
                    deliver(listener, param.args, periodic = !isSingle)
                }
            }
        stopDeliveringOnRemoval()
    }

    private fun hookLocationListenerClass(listenerClass: Class<*>) {
        if (!hookedListenerClasses.add(listenerClass)) return
        val hooked = listenerClass.methods
            .asSequence()
            .filter { !it.isBridge && !it.isSynthetic }
            .filter { method ->
                method.parameterTypes.any { Location::class.java.isAssignableFrom(it) }
            }
            .onEach { method ->
                listenerClass.beforeHookedMethod(method.name, *method.parameterTypes) { param ->
                    param.args.indices.forEach { index ->
                        when (val arg = param.args[index]) {
                            is Location -> modifyLocation(arg)
                            is List<*> -> arg.forEach { if (it is Location) modifyLocation(it) }
                        }
                    }
                }
            }
            .count()
        log("Hooked $hooked delivery method(s) on listener ${listenerClass.name}")
    }

    /**
     * Pushes a fake fix to [listener] on whichever Looper/Executor it registered with.
     * A single-update request is served exactly once; a continuous request is then fed
     * at the interval it asked for until it is removed.
     */
    private fun deliver(listener: LocationListener, args: Array<Any?>, periodic: Boolean) {
        val looper = args.filterIsInstance<Looper>().firstOrNull() ?: Looper.getMainLooper()
        val executor = args.filterIsInstance<Executor>().firstOrNull()
        val handler = Handler(looper)

        // Some SDKs wait for the provider to come up before they trust a fix at all.
        runCatching { listener.onProviderEnabled(LocationManager.GPS_PROVIDER) }

        fun pushOnce() {
            val fix = buildFakeLocation()
            val dispatch = Runnable { runCatching { listener.onLocationChanged(fix) } }
            if (executor != null) runCatching { executor.execute(dispatch) } else dispatch.run()
        }

        if (!periodic) {
            handler.post { pushOnce() }
            return
        }
        if (activeDeliveries.put(listener, handler) != null) return
        val requested = args.filterIsInstance<Long>().firstOrNull() ?: 0L
        val period = requested.coerceIn(MIN_DELIVERY_INTERVAL_MS, MAX_DELIVERY_INTERVAL_MS)
        val task = object : Runnable {
            override fun run() {
                if (!activeDeliveries.containsKey(listener)) return
                pushOnce()
                handler.postDelayed(this, period)
            }
        }
        handler.post(task)
        log("Active delivery started for ${listener.javaClass.name} every ${period}ms")
    }

    private fun stopDeliveringOnRemoval() {
        LocationManager::class.java.beforeHookAllMethods("removeUpdates") { param ->
            val listener = param.args.getOrNull(0) as? LocationListener
                ?: return@beforeHookAllMethods
            activeDeliveries.remove(listener)?.let {
                log("Active delivery stopped for ${listener.javaClass.name}")
            }
        }
    }

    /**
     * Answers one-shot `getCurrentLocation` requests directly. The real call would wait
     * on a provider that has nothing to report once identifiers are hidden, and many SDKs
     * treat its null result as a hard failure.
     */
    private fun hookCurrentLocationRequests() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        LocationManager::class.java.beforeHookAllMethods("getCurrentLocation") { param ->
            @Suppress("UNCHECKED_CAST")
            val consumer = param.args.filterIsInstance<Consumer<*>>().firstOrNull()
                as? Consumer<Any?> ?: return@beforeHookAllMethods
            val executor = param.args.filterIsInstance<Executor>().firstOrNull()
            val fix = buildFakeLocation()
            val dispatch = Runnable { runCatching { consumer.accept(fix) } }
            if (executor != null) runCatching { executor.execute(dispatch) }
            else Handler(Looper.getMainLooper()).post(dispatch)
            param.result = null
        }
    }

    /**
     * Spoofs vendor location SDKs whose result objects do not extend [Location]
     * (`BDLocation` is the notable one), so hooking the framework class alone misses them.
     */
    private fun hookKnownSdkLocations() {
        val pending = Collections.synchronizedSet(SDK_LOCATION_CLASSES.toMutableSet())
        pending.toList().forEach { className ->
            val clazz = findClassIfExists(className) ?: return@forEach
            pending.remove(className)
            applySdkLocationSpoof(className, clazz)
        }
        if (pending.isEmpty()) return
        // Packed apps (Legu, 360, …) have not decrypted their business dex yet at
        // onPackageReady, so these classes genuinely do not exist to hook. Observed on
        // the Legu-packed target: liblocSDK8a.so only loaded ~2.5 s into the process.
        log("SDK classes not loaded yet, watching class loading: $pending")
        watchClassLoading(pending)
    }

    private fun watchClassLoading(pending: MutableSet<String>) {
        ClassLoader::class.java.afterHookAllMethods("loadClass") { param ->
            if (pending.isEmpty() || reentrantClassWatch.get() == true) return@afterHookAllMethods
            val loaded = param.result as? Class<*> ?: return@afterHookAllMethods
            if (!pending.remove(loaded.name)) return@afterHookAllMethods
            reentrantClassWatch.set(true)
            try {
                applySdkLocationSpoof(loaded.name, loaded)
            } finally {
                reentrantClassWatch.set(false)
            }
        }
    }

    private fun applySdkLocationSpoof(className: String, clazz: Class<*>) {
        log("Hooking SDK location class $className")
        clazz.spoofCoordinateAccessors()
        if (className == BAIDU_LOCATION_CLASS) {
            // Baidu reports the outcome through locType, and the app branches on it.
            // Leaving it at 62 ("no valid location basis") makes the app discard the
            // coordinates we just wrote, so the status has to agree with the fix.
            clazz.setAllMethodResult("getLocType", BAIDU_LOC_TYPE_GPS)
            clazz.findMethodExactIfExists("getRadius")?.let {
                clazz.setAllMethodResult("getRadius", FAKE_ACCURACY_METERS)
            }
        }
    }

    /** Forces getLatitude/getLongitude to the fake fix and rewrites incoming setters. */
    private fun Class<*>.spoofCoordinateAccessors() {
        if (latitude != UNSET_COORDINATE) {
            setAllMethodResult("getLatitude", latitude)
            beforeHookedMethod("setLatitude", Double::class.javaPrimitiveType!!) { param ->
                param.args[0] = latitude
            }
        }
        if (longitude != UNSET_COORDINATE) {
            setAllMethodResult("getLongitude", longitude)
            beforeHookedMethod("setLongitude", Double::class.javaPrimitiveType!!) { param ->
                param.args[0] = longitude
            }
        }
    }

    /**
     * Makes GNSS report a healthy constellation instead of the real (now contradictory)
     * satellite data, and actively fires the "GPS started / first fix" events some apps
     * wait for. Mirrors the original Guise GNSS handling. The hidden constructor's
     * signature varies across releases and is discovered at runtime; any mismatch in
     * the remaining hooks degrades silently.
     */
    private fun fakeGnssSatellites() {
        LocationManager::class.java.setAllMethodResult("addNmeaListener", false)
        hookGnssStatusConstructor()
        hookGpsStatus()
        hookGpsStatusListener()
    }

    /**
     * The data-bearing GnssStatus constructor is hidden, and its signature drifts between
     * releases (7 args through Android 16; Android 17 dropped that one entirely). Calling
     * beforeHookConstructor with a hard-coded signature makes the constructor lookup
     * inside it throw NoSuchMethodException the moment a release changes it, so discover
     * the constructor instead: it is the declared constructor that starts with the
     * satellite count (int) and carries the most parameters.
     */
    @Volatile
    private var gnssConstructor: Constructor<*>? = null

    private fun hookGnssStatusConstructor() {
        val ctor = GnssStatus::class.java.declaredConstructors
            .filter { it.parameterTypes.firstOrNull() == Int::class.javaPrimitiveType }
            .maxByOrNull { it.parameterTypes.size }
        if (ctor == null) {
            // Exotic build with no data-bearing constructor: the public Builder path in
            // buildGnssStatus() still covers the getGpsStatus() injection.
            log("No data-bearing GnssStatus constructor found, skipping constructor hook")
            return
        }
        gnssConstructor = ctor
        log("Hooking GnssStatus constructor ${ctor.parameterTypes.joinToString { it.simpleName }}")
        GnssStatus::class.java.beforeHookConstructor(*ctor.parameterTypes) { param ->
            fillFakeConstellation(param.args, ctor.parameterTypes)
        }
    }

    /**
     * Fills a GnssStatus constructor's arguments by type: the leading int is the satellite
     * count, every trailing array is sized to match. Mirrors the original Guise's five
     * fake SVs — zero signal strengths, fully usable flags.
     */
    private fun fillFakeConstellation(args: Array<Any?>, types: Array<out Class<*>>) {
        types.forEachIndexed { index, type ->
            val fake: Any? = when {
                index == 0 -> svCount
                type == IntArray::class.java -> svidWithFlags
                type == FloatArray::class.java -> FloatArray(svCount)
                type == BooleanArray::class.java -> BooleanArray(svCount) { true }
                else -> null
            }
            if (fake != null) args[index] = fake
        }
    }

    private fun buildGnssStatus(): GnssStatus? {
        gnssConstructor?.let { ctor ->
            val args = arrayOfNulls<Any?>(ctor.parameterTypes.size)
            fillFakeConstellation(args, ctor.parameterTypes)
            runCatching {
                ctor.apply { isAccessible = true }.newInstance(*args) as GnssStatus
            }.getOrNull()?.let { return it }
        }
        return buildGnssStatusWithBuilder()
    }

    /** Public-API fallback (API 30+), used when no data-bearing constructor was found. */
    @android.annotation.SuppressLint("NewApi")
    private fun buildGnssStatusWithBuilder(): GnssStatus? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            GnssStatus.Builder().apply {
                for (index in 0 until svCount) {
                    // Only the 12-arg overload exists on current SDKs; the has* flags
                    // (carrier frequency, baseband C/N0) stay false like the original.
                    // AOSP signature: addSatellite(constellationType, svid, ...) — do not swap.
                    addSatellite(
                        GnssStatus.CONSTELLATION_GPS,
                        satelliteIds[index],
                        cn0s[index],
                        elevations[index],
                        azimuths[index],
                        true,
                        true,
                        true,
                        false,
                        0F,
                        false,
                        0F,
                    )
                }
            }.build()
        }.getOrNull()
    }

    private fun hookGpsStatusListener() {
        LocationManager::class.java.afterHookedMethod(
            "addGpsStatusListener",
            GpsStatus.Listener::class.java,
        ) { param ->
            (param.args[0] as? GpsStatus.Listener)?.run {
                callMethod("onGpsStatusChanged", GPS_EVENT_STARTED)
                callMethod("onGpsStatusChanged", GPS_EVENT_FIRST_FIX)
            }
        }
    }

    private fun hookGpsStatus() {
        LocationManager::class.java.beforeHookedMethod(
            "getGpsStatus",
            GpsStatus::class.java,
        ) { param ->
            val status = param.args[0] as? GpsStatus ?: return@beforeHookedMethod
            GpsStatus::class.java.findMethodExactIfExists(
                "setStatus", GnssStatus::class.java, Int::class.javaPrimitiveType!!,
            ) ?: return@beforeHookedMethod
            val gnss = buildGnssStatus() ?: return@beforeHookedMethod
            status.callMethod("setStatus", gnss, System.currentTimeMillis().toInt())
            param.result = status
        }
    }

    private fun rewriteLocations(value: Any?) {
        when (value) {
            is Location -> modifyLocation(value)
            is Iterable<*> -> value.forEach(::rewriteLocations)
            is Array<*> -> value.forEach(::rewriteLocations)
        }
    }

    private fun hasCompleteCoordinates(): Boolean =
        longitude != UNSET_COORDINATE && latitude != UNSET_COORDINATE

    private fun randomOffset(): Double = (Math.random() - 0.5) * 0.0001

    private fun buildFakeLocation(): Location =
        modifyLocation(Location(LocationManager.GPS_PROVIDER))

    private fun setLastLocation() {
        val manager = LocationManager::class.java
        val location = buildFakeLocation()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            manager.findMethodExactIfExists("getLastLocation") != null
        ) {
            manager.setMethodResult("getLastLocation", location)
        }
        manager.setMethodResult(
            methodName = "getLastKnownLocation",
            value = location,
            parameterTypes = arrayOf(String::class.java),
        )
    }

    private fun modifyLocation(location: Location): Location {
        return location.also {
            if (longitude != UNSET_COORDINATE) it.longitude = longitude
            if (latitude != UNSET_COORDINATE) it.latitude = latitude
            it.provider = LocationManager.GPS_PROVIDER
            it.accuracy = FAKE_ACCURACY_METERS
            it.time = System.currentTimeMillis()
            it.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (deliveryReported.compareAndSet(false, true)) {
                log("First fake fix delivered to the app")
            }
        }
    }

    private fun log(message: String) = XposedLogger.i(message, CATEGORY)

    private companion object {
        // Hidden GnssStatus constructors take packed values; Builder takes raw SVIDs.
        const val SVID_SHIFT_WIDTH = 12
        const val CONSTELLATION_TYPE_SHIFT_WIDTH = 8
        const val SVID_FLAGS_FULLY_USABLE = 0b111

        const val CATEGORY = "Location"
        const val UNSET_COORDINATE = -1.0
        const val FAKE_ACCURACY_METERS = 10.0f
        const val MIN_DELIVERY_INTERVAL_MS = 1000L
        const val MAX_DELIVERY_INTERVAL_MS = 10_000L
        const val BAIDU_LOCATION_CLASS = "com.baidu.location.BDLocation"

        /** BDLocation.TypeGpsLocation — a successful GNSS fix. */
        const val BAIDU_LOC_TYPE_GPS = 61

        val SDK_LOCATION_CLASSES = listOf(
            "com.amap.api.location.AMapLocation",
            BAIDU_LOCATION_CLASS,
        )
        val FRAMEWORK_LOCATION_TRANSPORTS = listOf(
            "android.location.LocationManager\$LocationListenerTransport",
            "android.location.LocationManager\$ListenerTransport",
        )
        val LOCATION_REQUEST_METHODS = listOf("requestSingleUpdate", "requestLocationUpdates")

        /** Guards against the class-loading hook re-entering itself. */
        val reentrantClassWatch: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    }
}
