package com.example.triade_monitoramento.data.remote.dto

data class PortaEventosResponseDto(
    val sensorId: String?,
    val sensorNome: String?,
    val yellowAfterSeconds: Int,
    val redAfterSeconds: Int,
    val amarelos: Int,
    val vermelhos: Int,
    val eventos: List<PortaEventoDto>
)

data class PortaOcorrenciaDto(
    val sensorId: String,
    val sensorNome: String,
    val openedAt: String?,
    val closedAt: String?,
    val durationMin: Double,
    val nivel: String
)