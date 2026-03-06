package com.example.triade_monitoramento

data class TemperatureUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    val latestTemp: Double? = null,
    val latestHum: Double? = null,
    val latestTs: String? = null,

    val chartPoints: List<TemperaturePointDto> = emptyList(),

    val periodStartIso: String? = null,
    val periodStopIso: String? = null
)