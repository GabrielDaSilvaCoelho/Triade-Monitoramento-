package com.example.triade_monitoramento.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TemperatureLatestDto(
    @Json(name = "timestamp")
    val ts: String?,
    val temperatura: Double?,
    val umidade: Double?,
    val porta: Double?
)

@JsonClass(generateAdapter = true)
data class TemperaturePointDto(
    @Json(name = "timestamp")
    val ts: String,
    val temperatura: Double,
    val umidade: Double,
    val porta: Double? = null
)
