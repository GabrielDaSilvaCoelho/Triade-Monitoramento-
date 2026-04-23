package com.example.triade_monitoramento.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SensorInsert(
    val id: String,
    val nome: String,
    val owner_id: Int,
    val temp_min: Double,
    val temp_max: Double
)

@Serializable
data class SensorDTO(
    val id: String,
    val nome: String,
    val owner_id: Int,
    val created_at: String? = null,
    val temp_min: Double? = null,
    val temp_max: Double? = null
)

