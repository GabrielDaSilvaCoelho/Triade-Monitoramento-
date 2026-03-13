package com.example.monitoramento.data.repository

import com.example.monitoramento.data.model.SensorInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class SensorRepository(
    private val supabase: SupabaseClient
) {
    suspend fun vincularSensor(
        sensorId: String,
        nome: String?,
        ownerId: String
    ) {
        val sensor = SensorInsert(
            sensorId = sensorId,
            name = nome?.takeIf { it.isNotBlank() },
            ownerId = ownerId
        )

        supabase.from("sensors").insert(sensor)
    }
}