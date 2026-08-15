package com.houvven.guise.constant

import com.tencent.mmkv.MMKV

object AppConfigKey {

    const val APP_SORT_TYPE = "app.sort.type"
    const val APP_SEARCH_BY_PACKAGE_NAME = "app.search.by.package.name"
    const val APP_REVERSE_SORT = "app.reverse.sort"
    const val DISPLAY_SYSTEM_APP = "display.system.app"

    const val THEME_MODE = "theme.mode"
    const val DYNAMIC_COLOR = "dynamic.color"
    const val CUSTOM_THEME_COLOR = "custom.theme.color"
    const val PREDICTIVE_BACK = "predictive.back"


    @JvmStatic
    val mmkv: MMKV  =  MMKV.defaultMMKV(MMKV.MULTI_PROCESS_MODE, null)
}
