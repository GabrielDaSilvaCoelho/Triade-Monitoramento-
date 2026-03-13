package com.example.triade_monitoramento

import com.squareup.moshi.Json

data class LatestTemperatureDto(
    @Json(name = "_time")
    val ts: String? = null,

    @Json(name = "temperatura")
    val temperatura: Double = 0.0,

    @Json(name = "umidade")
    val umidade: Double = 0.0
)