package com.example.triade_monitoramento.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SensorInsert(
    @SerialName("sensor_id")
    val sensorId: String,
    val name: String? = null,
    @SerialName("owner_id")
    val ownerId: String
)
@Serializable
data class SensorDTO(
    val id: String,
    val nome: String?,
    val owner_id: String
)

