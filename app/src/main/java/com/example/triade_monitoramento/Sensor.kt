package com.example.triade_monitoramento.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SensorInsert(
    val id: String,
    val nome: String,
    val owner_id: Int
)

@Serializable
data class SensorDTO(
    val id: String,
    val nome: String?,
    val owner_id: Int
)

