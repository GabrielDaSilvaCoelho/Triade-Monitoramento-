package com.example.triade_monitoramento.data.repository

import android.util.Log
import com.example.triade_monitoramento.Session
import com.example.triade_monitoramento.data.model.SensorDTO
import com.example.triade_monitoramento.data.model.SensorInsert
import com.example.triade_monitoramento.data.model.UserSensor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class SensorRepository(
    private val supabase: SupabaseClient
) {

    suspend fun cadastrarSensor(
        id: String,
        nome: String,
        tempLimitMax: Double,
        tempLimitMin: Double
    ): Boolean {
        return try {
            val userId = Session.userId
            Log.d("SENSOR_DEBUG", "Session.userId = $userId")

            if (userId == null) {
                Log.e("SENSOR_DEBUG", "userId nulo")
                return false
            }

            val sensorId = id.trim()
            val sensorNome = nome.trim()

            Log.d(
                "SENSOR_DEBUG",
                "Tentando cadastrar sensor: id=$sensorId, nome=$sensorNome, owner_id=$userId, temp_min=$tempLimitMin, temp_max=$tempLimitMax"
            )

            val sensorExistente = supabase
                .from("sensores")
                .select {
                    filter {
                        eq("id", sensorId)
                    }
                }
                .decodeList<SensorDTO>()
                .firstOrNull()

            if (sensorExistente == null) {
                val sensor = SensorInsert(
                    id = sensorId,
                    nome = sensorNome,
                    owner_id = userId,
                    temp_min = tempLimitMin,
                    temp_max = tempLimitMax
                )

                Log.d("SENSOR_DEBUG", "Inserindo sensor novo: $sensor")

                supabase
                    .from("sensores")
                    .insert(sensor)

                Log.d("SENSOR_DEBUG", "Insert executado com sucesso")
            } else {
                if (sensorExistente.owner_id != userId) {
                    Log.e("SENSOR_DEBUG", "Sensor já pertence a outro usuário")
                    return false
                }

                supabase
                    .from("sensores")
                    .update(
                        {
                            set("nome", sensorNome)
                            set("temp_min", tempLimitMin)
                            set("temp_max", tempLimitMax)
                        }
                    ) {
                        filter {
                            eq("id", sensorId)
                            eq("owner_id", userId)
                        }
                    }

                Log.d("SENSOR_DEBUG", "Update executado com sucesso")
            }

            true
        } catch (e: Exception) {
            Log.e("SENSOR_DEBUG", "Erro ao cadastrar sensor", e)
            false
        }
    }

    suspend fun buscarSensoresDoUsuario(): List<UserSensor> {
        val userId = Session.userId ?: return emptyList()

        return try {
            val response = supabase
                .from("sensores")
                .select {
                    filter {
                        eq("owner_id", userId)
                    }
                }
                .decodeList<SensorDTO>()

            response.map {
                UserSensor(
                    sensorId = it.id,
                    nome = it.nome
                )
            }
        } catch (e: Exception) {
            Log.e("SENSOR_DEBUG", "Erro ao buscar sensores", e)
            emptyList()
        }
    }

    suspend fun atualizarConfiguracaoSensor(
        sensorId: String,
        nome: String,
        tempLimitMax: Double,
        tempLimitMin: Double
    ): Boolean {
        return try {
            val userId = Session.userId ?: return false

            supabase
                .from("sensores")
                .update(
                    {
                        set("nome", nome.trim())
                        set("temp_min", tempLimitMin)
                        set("temp_max", tempLimitMax)
                    }
                ) {
                    filter {
                        eq("id", sensorId)
                        eq("owner_id", userId)
                    }
                }

            true
        } catch (e: Exception) {
            Log.e("SENSOR_DEBUG", "Erro ao atualizar configuração", e)
            false
        }
    }

    suspend fun excluirSensorDaConta(sensorId: String): Boolean {
        return try {
            val userId = Session.userId ?: return false

            supabase
                .from("sensores")
                .delete {
                    filter {
                        eq("id", sensorId)
                        eq("owner_id", userId)
                    }
                }

            true
        } catch (e: Exception) {
            Log.e("SENSOR_DEBUG", "Erro ao excluir sensor", e)
            false
        }
    }
}