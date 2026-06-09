package com.example.triade_monitoramento.data.remote.dto


data class PortaEventoDto(
    val sensorId: String,
    val sensorNome: String,
    val openedAt: String?,
    val closedAt: String?,
    val durationSeconds: Double,
    val nivel: String

)