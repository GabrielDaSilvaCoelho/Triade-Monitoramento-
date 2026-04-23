package com.example.triade_monitoramento.data.model

data class UserSensor(
    val sensorId: String,
    val name: String? = null,
    val nome: String
) {
    fun displayName(): String {
        return if (!name.isNullOrBlank()) {
            "$name ($sensorId)"
        } else {
            sensorId
        }
    }
}