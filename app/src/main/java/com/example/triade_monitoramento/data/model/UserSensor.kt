package com.example.triade_monitoramento.data.model

data class UserSensor(
    val sensorId: String,
    val nome: String
) {
    fun displayName(): String {
        return if (nome.isNotBlank()) nome else sensorId
    }
}