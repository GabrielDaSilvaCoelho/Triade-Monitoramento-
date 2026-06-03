package com.example.triade_monitoramento.ui.sensor

data class PortaConfigRequestDto(
    val sensorId: String,
    val yellowAfterMinutes: Int,
    val redAfterMinutes: Int
)