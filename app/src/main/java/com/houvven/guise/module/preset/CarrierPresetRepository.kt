package com.houvven.guise.module.preset

import android.content.Context
import com.houvven.guise.module.PresetAdapter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CarrierPreset(
    val name: String,
    val plmn: String,
    val countryCode: String,
    val countryName: String,
) : PresetAdapter {
    override val label: String get() = "$countryName · $name · $plmn"
    override val value: String get() = plmn
}

object CarrierPresetRepository {
    @Volatile
    private var cached: List<CarrierPreset>? = null

    fun get(context: Context): List<CarrierPreset> = cached ?: synchronized(this) {
        cached ?: context.assets.open("carriers.json").bufferedReader().use { reader ->
            Json.decodeFromString<List<CarrierPreset>>(reader.readText()).also { cached = it }
        }
    }
}

// PLMN 长度 5–6 是 SimHook 切 MCC/MNC 的前提；国家码必须是两位 ISO-3166-1 alpha-2。
// carriers.json 里有 77 条脏数据（"AU/CC/CX"、空串、14 字符等），无条件 random() 会把它们
// 写进 getSimCountryIso —— 这就是「运营商与国家对不上」的具体来源。
private val VALID_PLMN = Regex("^\\d{5,6}$")
private val VALID_COUNTRY_CODE = Regex("^[A-Z]{2}$")

val CarrierPreset.hasValidIdentity: Boolean
    get() = VALID_PLMN.matches(plmn) && VALID_COUNTRY_CODE.matches(countryCode)

/** 从干净条目里随机抽一条；一条都没有时抛异常，绝不返回可能污染配置的值。 */
fun List<CarrierPreset>.randomValidCarrier(): CarrierPreset {
    val valid = filter { it.hasValidIdentity }
    require(valid.isNotEmpty()) { "carriers.json 里没有合法条目（PLMN 5–6 位数字 + 国家码两位）" }
    return valid.random()
}
