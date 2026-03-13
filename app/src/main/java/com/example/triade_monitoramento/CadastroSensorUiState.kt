package com.example.monitoramento.ui.sensor

data class CadastroSensorUiState(
    val sensorId: String = "",
    val nome: String = "",
    val loading: Boolean = false,
    val sucesso: Boolean = false,
    val erro: String? = null
)