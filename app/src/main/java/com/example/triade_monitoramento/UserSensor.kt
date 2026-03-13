package com.example.triade_monitoramento

data class UserSensor(
    val sensorId: String,
    val name: String? = null
) {
    fun displayName(): String {
        return if (!name.isNullOrBlank()) {
            "$name ($sensorId)"
        } else {
            sensorId
        }
    }
}