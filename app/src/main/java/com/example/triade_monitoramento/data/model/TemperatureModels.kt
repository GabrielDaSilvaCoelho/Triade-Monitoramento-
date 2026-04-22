package com.example.triade_monitoramento.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TemperatureLatestDto(
    val ts: String?,
    val temperatura: Double?,
    val umidade: Double?
)

@JsonClass(generateAdapter = true)
data class TemperaturePointDto(
    val ts: String,
    val temperatura: Double,
    val umidade: Double
)