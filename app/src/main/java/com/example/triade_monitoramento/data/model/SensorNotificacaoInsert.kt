package com.example.triade_monitoramento.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SensorNotificacaoInsert(
    val sensor_id: String,
    val tipo: String,
    val destino: String,
    val nome_contato: String? = null,
    val enabled: Boolean = true
)