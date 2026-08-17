package com.houvven.guise.xposed.hook

import android.app.LocaleManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.afterHookedMethod
import com.houvven.ktx_xposed.hook.beforeHookedMethod
import com.houvven.ktx_xposed.hook.setMethodResult
import java.util.Locale

class LocalHook : LoadPackageHandler {

    override fun onHook() {
        val locale = config.language.toLocaleOrNull() ?: return
        val locales = LocaleList(locale)

        Locale.setDefault(locale)
        LocaleList.setDefault(locales)
        hookJavaLocales(locale, locales)
        hookResourceLocales(locale, locales)
        hookPerAppLocales(locales)
    }

    private fun hookJavaLocales(locale: Locale, locales: LocaleList) {
        Locale::class.java.run {
            setMethodResult("getDefault", locale)
            // Preserve the original Guise behavior: callers may cache a Locale before
            // PackageReady, so changing only the process default cannot affect that object.
            setMethodResult("getLanguage", locale.language)
            setMethodResult("getCountry", locale.country)
            setMethodResult("getVariant", locale.variant)
            setMethodResult("getScript", locale.script)
            setMethodResult("getDisplayLanguage", locale.displayLanguage)
            setMethodResult("getDisplayCountry", locale.displayCountry)
            setMethodResult("getDisplayName", locale.displayName)
            setMethodResult("getDisplayVariant", locale.displayVariant)
            setMethodResult("getDisplayScript", locale.displayScript)
            setMethodResult("toLanguageTag", locale.toLanguageTag())
            setMethodResult("toString", locale.toString())
            beforeHookedMethod("getDefault", Locale.Category::class.java) { it.result = locale }
            beforeHookedMethod("setDefault", Locale::class.java) { it.args[0] = locale }
            beforeHookedMethod("setDefault", Locale.Category::class.java, Locale::class.java) {
                it.args[1] = locale
            }
        }
        LocaleList::class.java.run {
            setMethodResult("getDefault", locales)
            setMethodResult("getAdjustedDefault", locales)
            beforeHookedMethod("setDefault", LocaleList::class.java) { it.args[0] = locales }
        }
    }

    private fun hookResourceLocales(locale: Locale, locales: LocaleList) {
        Configuration::class.java.afterHookedMethod("getLocales") { it.result = locales }
        Resources::class.java.afterHookedMethod("getConfiguration") { param ->
            (param.result as? Configuration)?.applyLocale(locale)
        }
        Resources.getSystem().configuration.applyLocale(locale)
    }

    private fun hookPerAppLocales(locales: LocaleList) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        LocaleManager::class.java.run {
            setMethodResult("getApplicationLocales", locales)
            beforeHookedMethod("getApplicationLocales", String::class.java) { it.result = locales }
            setMethodResult("getSystemLocales", locales)
            beforeHookedMethod("setApplicationLocales", LocaleList::class.java) { it.args[0] = locales }
        }
    }

    private fun Configuration.applyLocale(locale: Locale) {
        if (this.locale != locale || locales.size() != 1 || locales[0] != locale) setLocale(locale)
    }
}

private fun String.toLocaleOrNull(): Locale? {
    val parts = trim().replace('-', '_').split('_').filter(String::isNotBlank)
    if (parts.isEmpty()) return null
    return runCatching {
        Locale.Builder().setLanguage(parts[0]).apply {
            parts.drop(1).forEach { part ->
                when {
                    part.length == 4 -> setScript(part)
                    part.length == 2 || part.length == 3 -> setRegion(part)
                    else -> setVariant(part)
                }
            }
        }.build()
    }.getOrNull()
}
