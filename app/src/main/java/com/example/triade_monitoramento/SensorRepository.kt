package com.example.triade_monitoramento.data.repository

import android.util.Log
import com.example.triade_monitoramento.Session
import com.example.triade_monitoramento.Session.userId
import com.example.triade_monitoramento.UserSensor
import com.example.triade_monitoramento.data.model.SensorDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns


class SensorRepository(


    private val supabase: SupabaseClient
) {

    suspend fun cadastrarSensor(id: String, nome: String): Boolean {
        return try {
            val userId = Session.userId

            if (userId == null) {
                Log.e("DEBUG_USER", "Session.userId NULL")
                return false
            }

            val sensor = mapOf(
                "id" to id,
                "nome" to nome,
                "owner_id" to userId
            )


            supabase
                .from("sensores")
                .insert(sensor)

            true
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao cadastrar", e)
            false
        }
    }

    suspend fun buscarSensoresDoUsuario(): List<UserSensor> {

        val userId = Session.userId

        if (userId == null) {
            Log.e("SENSOR_DEBUG", "UserId NULL")
            return emptyList()
        }

        val response = supabase
            .from("sensores")
            .select {
                filter {
                    eq("owner_id", userId)
                }
            }
            .decodeList<SensorDTO>()
        Log.d("SENSOR_DEBUG", "Sensores encontrados: ${response.size}")

        return response.map {
            UserSensor(
                sensorId = it.id,
            )
        }
    }
}
