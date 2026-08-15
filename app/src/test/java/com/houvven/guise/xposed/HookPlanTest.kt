package com.houvven.guise.xposed

import com.houvven.guise.xposed.config.HooksValue
import com.houvven.guise.xposed.config.ModuleConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class HookPlanTest {

    @Test
    fun defaultConfigurationInstallsNoHooks() {
        assertEquals(emptyList<HookFeature>(), ModuleConfig().activeHookFeatures())
    }

    @Test
    fun onlyConfiguredGroupsAreActivated() {
        val config = ModuleConfig(
            brand = "Xiaomi",
            densityDpi = 420,
            versionName = "2.0",
            passApplications = true,
        )

        assertEquals(
            listOf(
                HookFeature.OS_BUILD,
                HookFeature.SYSTEM_PROPERTIES,
                HookFeature.DISPLAY_DENSITY,
                HookFeature.APPLICATION_LIST,
                HookFeature.APP_VERSION,
            ),
            config.activeHookFeatures(),
        )
    }

    @Test
    fun networkSubfieldsActivateTheNetworkGroup() {
        assertEquals(
            listOf(HookFeature.NETWORK),
            ModuleConfig(simOperator = "46000").activeHookFeatures(),
        )
        assertEquals(
            listOf(HookFeature.NETWORK),
            ModuleConfig(networkType = HooksValue.NET_WIFI).activeHookFeatures(),
        )
    }

    @Test
    fun locationSourceBlockingCanBeEnabledWithoutCoordinates() {
        assertEquals(
            listOf(HookFeature.LOCATION),
            ModuleConfig(makeWifiLocationFail = true).activeHookFeatures(),
        )
        assertEquals(
            listOf(HookFeature.LOCATION),
            ModuleConfig(makeCellLocationFail = true).activeHookFeatures(),
        )
    }

    @Test
    fun blockingCellLocationTakesPrecedenceOverCellSpoofing() {
        assertEquals(
            listOf(HookFeature.LOCATION),
            ModuleConfig(
                lac = 460,
                cid = 10_001,
                makeCellLocationFail = true,
            ).activeHookFeatures(),
        )
    }

    @Test
    fun coordinatesAndCellIdentityInstallOnlyTheirOwnGroups() {
        assertEquals(
            listOf(HookFeature.LOCATION, HookFeature.CELL_LOCATION),
            ModuleConfig(
                latitude = 39.9042,
                longitude = 116.4074,
                lac = 460,
                cid = 10_001,
            ).activeHookFeatures(),
        )
    }

    @Test
    fun buildIdentityActivatesFieldAndPropertyHooks() {
        assertEquals(
            listOf(HookFeature.OS_BUILD, HookFeature.SYSTEM_PROPERTIES),
            ModuleConfig(manufacturer = "Xiaomi").activeHookFeatures(),
        )
        assertEquals(
            listOf(HookFeature.OS_BUILD, HookFeature.SYSTEM_PROPERTIES),
            ModuleConfig(buildId = "AP3A.250101.001").activeHookFeatures(),
        )
    }

    @Test
    fun optionalSurfaceHooksRemainIndependent() {
        assertEquals(
            listOf(
                HookFeature.GPU,
                HookFeature.WEBVIEW,
                HookFeature.CAMERA,
                HookFeature.AUDIO_DEVICES,
                HookFeature.ADVERTISING_ID,
            ),
            ModuleConfig(
                gpuVendor = "Qualcomm",
                webViewUserAgent = "Example/1.0",
                cameraCount = 1,
                hideExternalAudioDevices = true,
                advertisingId = "00000000-0000-0000-0000-000000000001",
            ).activeHookFeatures(),
        )
    }
}
