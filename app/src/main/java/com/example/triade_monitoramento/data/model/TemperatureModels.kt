package com.example.triade_monitoramento.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TemperatureLatestDto(
    @Json(name = "timestamp")
    val ts: String? = null,

    @Json(name = "temperatura")
    val temperatura: Double? = null,

    @Json(name = "umidade")
    val umidade: Double? = null,

    @Json(name = "porta")
    val porta: Double? = null
)

@JsonClass(generateAdapter = true)
data class TemperaturePointDto(
    @Json(name = "timestamp")
    val ts: String? = null,

    @Json(name = "temperatura")
    val temperatura: Double? = null,

    @Json(name = "umidade")
    val umidade: Double? = null,

    @Json(name = "porta")
    val porta: Double? = null
)