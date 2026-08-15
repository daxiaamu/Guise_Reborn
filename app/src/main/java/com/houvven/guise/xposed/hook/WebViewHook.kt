package com.houvven.guise.xposed.hook

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.afterHookAllMethods
import com.houvven.ktx_xposed.hook.beforeHookSomeSameNameMethod
import com.houvven.ktx_xposed.hook.setMethodResult
import java.util.Collections
import java.util.WeakHashMap

class WebViewHook : LoadPackageHandler {
    private val configuredSettings = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<WebSettings, Boolean>())
    )

    override fun onHook() {
        val userAgent = config.webViewUserAgent
        if (userAgent.isBlank()) return

        WebSettings::class.java.setMethodResult(
            "getDefaultUserAgent",
            userAgent,
            parameterTypes = arrayOf(Context::class.java),
        )
        WebView::class.java.afterHookAllMethods("getSettings") { param ->
            applyUserAgent(param.result as? WebSettings, userAgent)
        }
        WebView::class.java.beforeHookSomeSameNameMethod(
            "loadUrl",
            "loadData",
            "loadDataWithBaseURL",
        ) { param ->
            applyUserAgent((param.thisObject as? WebView)?.settings, userAgent)
        }
    }

    private fun applyUserAgent(settings: WebSettings?, userAgent: String) {
        if (settings == null || !configuredSettings.add(settings)) return
        runCatching { settings.userAgentString = userAgent }
            .onFailure { configuredSettings.remove(settings) }
    }
}
