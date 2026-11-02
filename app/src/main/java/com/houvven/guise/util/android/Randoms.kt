package com.houvven.guise.util.android

import java.util.UUID
import java.util.Locale
import kotlin.math.round
import kotlin.random.Random


object Randoms {

    private const val ALPHANUMERIC =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private const val HEX = "0123456789abcdef"
    private const val HEX_UPPER = "0123456789ABCDEF"

    // IEEE 注册表（standards-oui.ieee.org/oui/oui.csv）里的真实 MA-L 前缀。两个池不相交：
    //   网卡侧给 WiFi-MAC，路由器侧给 BSSID —— 拿路由器厂商的 OUI 当手机网卡地址会自相矛盾。
    //   池内每条都满足 U/L 位 = 0（全局唯一）。IEEE 全部 4 万条里有 18 条历史遗留的 U/L=1，
    //   所以这里不假设「OUI 天然全局」，只收录核对过的条目。
    internal val NIC_OUIS = listOf(
        "444988", "e4c767", "a002a5",             // Intel Corporate
        "88124e", "a0bd71", "00a0c6",             // Qualcomm
        "00620b", "5c6f69",                       // Broadcom
        "000c43", "000ce7", "0017a5",             // MediaTek
        "00e04c", "fc934e",                       // Realtek
        "cceb5e", "b8ea98", "f41a9c",             // Xiaomi Communications
        "641b2f", "9c73b1", "388a06",             // Samsung Electronics
        "e00630", "d8daf1", "54443b",             // HUAWEI TECHNOLOGIES
        "e44097", "dcb4ca",                       // GUANGDONG OPPO
    )
    internal val ROUTER_OUIS = listOf(
        "34f716", "54a703", "b0be76",             // TP-LINK
        "488ad2", "503aa0", "5cde34",             // MERCURY
        "b40f3b", "cc2d21", "b83a08",             // Tenda
        "002618", "049226", "1831bf",             // ASUSTek
        "405d82", "dcef09", "100c6b",             // NETGEAR
        "04a959", "708185", "148477",             // New H3C
        "f09fc2", "802aa8", "788a20",             // Ubiquiti
    )
    internal val SSID_PREFIXES = listOf(
        "ChinaNet-", "CMCC-", "ChinaUnicom-", "TP-LINK_", "MERCURY_", "FAST_",
        "Tenda_", "HUAWEI-", "Xiaomi_", "Redmi_", "OPPO_", "vivo_", "H3C_", "Ruijie_",
    )

    /**
     * 生成一个指定长度的随机字符串
     * @param length 长度
     * @return 随机字符串
     */
    fun randomString(length: Int): String {
        require(length >= 0)
        return buildString(length) {
            repeat(length) { append(ALPHANUMERIC.random()) }
        }
    }

    /**
     * 生成一个随机长度的随机字符串 长度在[10, 100]内
     */
    fun randomString(): String {
        return randomString(Random.nextInt(10, 101))
    }


    fun uuid(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * 生成一个不带横线的UUID
     */
    fun uuidNoDash(): String {
        return uuid().replace("-", "")
    }

    /**
     * 随机生成 WiFi 网卡的 MAC 地址（手机侧）。
     *
     * 用真实厂商 OUI 而不是 locally-administered 地址：第一个字节的 bit1 = 0 是全局唯一
     * （厂商烧录）、= 1 是本地管理。本地管理地址的第二位十六进制必然是 2/6/a/e，任何 App
     * 一行正则就能认出这是伪造，而真实网卡不会长这样。
     */
    fun randomMacAddress(): String = randomOuiAddress(NIC_OUIS)

    /** 随机生成 BSSID（AP 侧地址），池子与 [randomMacAddress] 分开，见 NIC_OUIS/ROUTER_OUIS 注释。 */
    fun randomBssid(): String = randomOuiAddress(ROUTER_OUIS)

    /**
     * 随机生成 WiFi 名称。
     *
     * 走常见热点前缀 + 4 位十六进制尾巴，而不是纯随机串：真实 SSID 由厂商默认规则生成
     * （光猫 ChinaNet-XXXX、路由 TP-LINK_XXXX），纯随机字符在扫到的热点列表里一眼假。
     * 不含引号 —— WifiHook 给 getSSID 的返回值外面还会再包一层引号。
     */
    fun randomWifiSsid(): String {
        val prefix = SSID_PREFIXES.random()
        val suffix = buildString(4) { repeat(4) { append(HEX_UPPER.random()) } }
        return if (Random.nextInt(4) == 0) "$prefix${suffix}_5G" else "$prefix$suffix"
    }

    private fun randomOuiAddress(pool: List<String>): String {
        val oui = pool.random()
        val tail = (0 until 3).joinToString(":") {
            Random.nextInt(256).toString(16).padStart(2, '0')
        }
        return "${oui.substring(0, 2)}:${oui.substring(2, 4)}:${oui.substring(4, 6)}:$tail"
    }


    /**
     * 随机生成IMEI
     */
    fun randomIMEI(): String {
        val body = randomDigits(14)
        return body + imeiCheckDigit(body)
    }

    /** Android ID/SSAID is conventionally exposed as 16 lower-case hexadecimal characters. */
    fun randomAndroidId(): String = buildString(16) {
        repeat(16) { append(HEX.random()) }
    }

    fun randomPhoneNum(): String {
        return "1${randomDigits(10)}"
    }

    fun randomBuildId(androidVersion: String = ""): String {
        val major = androidVersion.substringBefore('.').toIntOrNull()
        val (prefix, firstYear) = when (major) {
            10 -> "QP1A" to 19
            11 -> "RP1A" to 20
            12 -> "SP1A" to 21
            13 -> "TP1A" to 22
            14 -> "UP1A" to 23
            15 -> "AP3A" to 24
            16 -> "BP2A" to 25
            17 -> "CP1A" to 26
            else -> "GU1A" to 24
        }
        return String.format(
            Locale.ROOT,
            "$prefix.%02d%02d%02d.%03d",
            Random.nextInt(firstYear, firstYear + 2),
            Random.nextInt(1, 13),
            Random.nextInt(1, 29),
            Random.nextInt(1_000),
        )
    }

    fun randomFingerprint(
        brand: String,
        product: String,
        device: String,
        androidVersion: String,
        buildId: String,
    ): String {
        val safeBrand = brand.fingerprintPart("generic")
        val safeDevice = device.fingerprintPart("device")
        val safeProduct = product.fingerprintPart(safeDevice)
        val safeRelease = androidVersion.fingerprintPart("16")
        val safeBuildId = buildId.fingerprintPart(randomBuildId(androidVersion))
        return "$safeBrand/$safeProduct/$safeDevice:$safeRelease/$safeBuildId/" +
            "${randomDigits(7)}:user/release-keys"
    }

    fun randomBatteryLevel(): Int = Random.nextInt(101)

    fun randomCoordinates(): Pair<Double, Double> {
        val latitude = round(Random.nextDouble(-90.0, 90.0) * 1_000_000) / 1_000_000
        val longitude = round(Random.nextDouble(-180.0, 180.0) * 1_000_000) / 1_000_000
        return latitude to longitude
    }

    private fun randomDigits(length: Int): String = buildString(length) {
        repeat(length) { append(Random.nextInt(10)) }
    }

    private fun imeiCheckDigit(body: String): Int {
        require(body.length == 14 && body.all(Char::isDigit))
        val sum = body.mapIndexed { index, char ->
            val digit = char.digitToInt()
            if (index % 2 == 1) (digit * 2).let { if (it > 9) it - 9 else it } else digit
        }.sum()
        return (10 - sum % 10) % 10
    }

    private fun String.fingerprintPart(fallback: String): String =
        trim().takeIf(String::isNotEmpty)
            ?.replace(Regex("[\\s/:]+"), "_")
            ?: fallback

}
