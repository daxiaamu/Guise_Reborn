@file:Suppress("DEPRECATION")

package com.houvven.guise.xposed.hook.location

import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.beforeHookAllMethods
import com.houvven.ktx_xposed.hook.beforeHookedMethod
import com.houvven.ktx_xposed.hook.findClassIfExists
import com.houvven.ktx_xposed.hook.setMethodResult


@Suppress("DEPRECATION")
class LocationHook : LoadPackageHandler, LocationHookBase() {


    private var latitude = config.latitude
    private var longitude = config.longitude

    override fun onHook() {
        val spoofCoordinates = longitude != UNSET_COORDINATE || latitude != UNSET_COORDINATE
        if (spoofCoordinates) {
            if (config.randomOffset) {
                if (latitude != UNSET_COORDINATE) latitude += randomOffset()
                if (longitude != UNSET_COORDINATE) longitude += randomOffset()
            }
            fakeCoordinates()
            hookFrameworkLocationCallbacks()
            hookKnownSdkLocations()
            if (hasCompleteCoordinates()) setLastLocation()
        }
        if (config.makeWifiLocationFail) makeWifiLocationFail()
        if (config.makeCellLocationFail) makeCellLocationFail()
    }

    private fun fakeCoordinates() {
        Location::class.java.run {
            if (longitude != UNSET_COORDINATE) setMethodResult("getLongitude", longitude)
            if (latitude != UNSET_COORDINATE) setMethodResult("getLatitude", latitude)
        }
    }

    private fun hookFrameworkLocationCallbacks() {
        FRAMEWORK_LOCATION_TRANSPORTS
            .mapNotNull(::findClassIfExists)
            .forEach { transport ->
                transport.beforeHookAllMethods("onLocationChanged") { param ->
                    param.args.forEach(::rewriteLocations)
                }
            }
    }

    private fun hookKnownSdkLocations() {
        findClassIfExists(AMAP_LOCATION_CLASS)?.run {
            if (latitude != UNSET_COORDINATE) {
                setMethodResult("getLatitude", latitude)
                beforeHookedMethod("setLatitude", Double::class.javaPrimitiveType!!) { param ->
                    param.args[0] = latitude
                }
            }
            if (longitude != UNSET_COORDINATE) {
                setMethodResult("getLongitude", longitude)
                beforeHookedMethod("setLongitude", Double::class.javaPrimitiveType!!) { param ->
                    param.args[0] = longitude
                }
            }
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

    private fun setLastLocation() {
        val location = modifyLocation(Location(LocationManager.GPS_PROVIDER))
        LocationManager::class.java.setMethodResult(
            methodName = "getLastLocation",
            value = location,
        )
        LocationManager::class.java.setMethodResult(
            methodName = "getLastKnownLocation",
            value = location,
            parameterTypes = arrayOf(String::class.java),
        )
    }

    private fun modifyLocation(location: Location): Location {
        return location.also {
            it.longitude = longitude
            it.latitude = latitude
            it.provider = LocationManager.GPS_PROVIDER
            it.accuracy = 10.0f
            it.time = System.currentTimeMillis()
            it.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }

    private companion object {
        const val UNSET_COORDINATE = -1.0
        const val AMAP_LOCATION_CLASS = "com.amap.api.location.AMapLocation"
        val FRAMEWORK_LOCATION_TRANSPORTS = listOf(
            "android.location.LocationManager\$LocationListenerTransport",
            "android.location.LocationManager\$ListenerTransport",
        )
    }
}
