package com.houvven.guise.util.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomsTest {

    @Test
    fun generatedIdentifiersUsePlatformShapes() {
        repeat(100) {
            assertTrue(Randoms.randomMacAddress().matches(Regex("[0-9a-f]{2}(:[0-9a-f]{2}){5}")))
            assertTrue(Randoms.randomAndroidId().matches(Regex("[0-9a-f]{16}")))
            assertTrue(isValidImei(Randoms.randomIMEI()))
        }
    }

    @Test
    fun generatedCoordinatesStayInsideGlobalBounds() {
        repeat(1_000) {
            val (latitude, longitude) = Randoms.randomCoordinates()
            assertTrue(latitude in -90.0..90.0)
            assertTrue(longitude in -180.0..180.0)
        }
    }

    @Test
    fun fingerprintKeepsConfiguredIdentityCoherent() {
        val fingerprint = Randoms.randomFingerprint(
            brand = "Xiaomi",
            product = "houji",
            device = "houji",
            androidVersion = "16",
            buildId = "AP3A.250101.001",
        )

        assertTrue(fingerprint.startsWith("Xiaomi/houji/houji:16/AP3A.250101.001/"))
        assertTrue(fingerprint.endsWith(":user/release-keys"))
    }

    @Test
    fun buildIdTracksTheSelectedAndroidGeneration() {
        assertTrue(Randoms.randomBuildId("14").startsWith("UP1A."))
        assertTrue(Randoms.randomBuildId("15").startsWith("AP3A."))
        assertTrue(Randoms.randomBuildId("16").startsWith("BP2A."))
        assertTrue(Randoms.randomBuildId("17").startsWith("CP1A."))
    }

    @Test
    fun wifiAddressesLookLikeRealHardware() {
        val localNibbles = setOf('2', '6', 'a', 'e')
        repeat(500) {
            listOf(Randoms.randomMacAddress(), Randoms.randomBssid()).forEach { address ->
                assertTrue(address.matches(Regex("[0-9a-f]{2}(:[0-9a-f]{2}){5}")))
                assertTrue(
                    "$address 的首字节第二低位是本地管理位（真实网卡不会是这个形态）",
                    address[1] !in localNibbles,
                )
            }
        }
    }

    @Test
    fun addressPoolsAreGloballyAdministeredAndDisjoint() {
        val localNibbles = setOf('2', '6', 'a', 'e')
        val pool = Randoms.NIC_OUIS + Randoms.ROUTER_OUIS
        assertEquals(pool.size, pool.distinct().size)
        pool.forEach { oui ->
            assertTrue("$oui 不是 6 位小写十六进制", oui.matches(Regex("[0-9a-f]{6}")))
            assertTrue("$oui 的 U/L 位是 1", oui[1] !in localNibbles)
        }
        // 网卡池与路由器池不能相交，否则 MAC 与 BSSID 可能撞成同一个地址
        assertTrue(Randoms.NIC_OUIS.intersect(Randoms.ROUTER_OUIS.toSet()).isEmpty())
    }

    @Test
    fun wifiNameKeepsRouterStylePrefixAndNoQuotes() {
        repeat(200) {
            val ssid = Randoms.randomWifiSsid()
            val prefix = Randoms.SSID_PREFIXES.firstOrNull(ssid::startsWith)
            assertNotNull("$ssid 没有命中任何真实热点前缀", prefix)
            // 前缀之后必须是 4 位大写十六进制（少数带 _5G），而不是随机长度的字符
            assertTrue(
                "$ssid 的后缀形态不对",
                Regex("[0-9A-F]{4}(_5G)?").matches(ssid.removePrefix(prefix!!)),
            )
            assertTrue("$ssid 带了引号，WifiHook 会再包一层", '"' !in ssid)
        }
    }

    private fun isValidImei(value: String): Boolean {
        if (!value.matches(Regex("\\d{15}"))) return false
        val sum = value.mapIndexed { index, char ->
            val digit = char.digitToInt()
            if (index % 2 == 1) (digit * 2).let { if (it > 9) it - 9 else it } else digit
        }.sum()
        return sum % 10 == 0
    }
}
