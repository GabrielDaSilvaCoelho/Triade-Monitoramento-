package com.example.triade_monitoramento

data class TemperatureUiState(
    val latestTemp: Double? = null,
    val latestHum: Double? = null,
    val latestTs: String? = null,
    val chartPoints: List<TemperaturePointDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val periodStartIso: String? = null,
    val periodStopIso: String? = null,
    val sensores: List<UserSensor> = emptyList(),
    val sensorSelecionado: UserSensor? = null
)