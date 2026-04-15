package com.example.triade_monitoramento.data.repository

import android.util.Log
import com.example.triade_monitoramento.Session
import com.example.triade_monitoramento.UserSensor
import com.example.triade_monitoramento.data.model.SensorDTO
import com.example.triade_monitoramento.data.model.SensorInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

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
            val userId = Session.userId ?: return false

            val sensorExiste = try {
                supabase
                    .from("sensores")
                    .select {
                        filter {
                            eq("id", id)
                        }
                    }
                    .decodeList<SensorDTO>()
                    .isNotEmpty()
            } catch (_: Exception) {
                false
            }

            if (!sensorExiste) {
                val sensor = SensorInsert(
                    id = id,
                    nome = nome,
                    owner_id = userId
                )

                supabase
                    .from("sensores")
                    .insert(sensor)
            }

            val regraExistente = try {
                supabase
                    .from("alert_rules")
                    .select {
                        filter {
                            eq("sensor_id", id)
                        }
                    }
                    .decodeList<AlertRuleDTO>()
                    .firstOrNull()
            } catch (_: Exception) {
                null
            }

            if (regraExistente == null) {
                val regra = AlertRuleInsert(
                    sensor_id = id,
                    enabled = true,
                    cooldown_sec = 0,
                    temp_limit = null,
                    temp_limit_max = tempLimitMax,
                    temp_limit_min = tempLimitMin
                )

                supabase
                    .from("alert_rules")
                    .insert(regra)
            } else {
                supabase
                    .from("alert_rules")
                    .update(
                        {
                            set("temp_limit_max", tempLimitMax)
                            set("temp_limit_min", tempLimitMin)
                            set("enabled", true)
                        }
                    ) {
                        filter {
                            eq("sensor_id", id)
                        }
                    }
            }

            true
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao cadastrar sensor", e)
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
                    sensorId = it.id
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
                        set("nome", nome)
                    }
                ) {
                    filter {
                        eq("id", sensorId)
                        eq("owner_id", userId)
                    }
                }

            val regraExistente = supabase
                .from("alert_rules")
                .select {
                    filter {
                        eq("sensor_id", sensorId)
                    }
                }
                .decodeList<AlertRuleDTO>()
                .firstOrNull()

            if (regraExistente == null) {
                val regra = AlertRuleInsert(
                    sensor_id = sensorId,
                    enabled = true,
                    cooldown_sec = 0,
                    temp_limit = null,
                    temp_limit_max = tempLimitMax,
                    temp_limit_min = tempLimitMin
                )

                supabase
                    .from("alert_rules")
                    .insert(regra)
            } else {
                supabase
                    .from("alert_rules")
                    .update(
                        {
                            set("temp_limit_max", tempLimitMax)
                            set("temp_limit_min", tempLimitMin)
                            set("enabled", true)
                        }
                    ) {
                        filter {
                            eq("sensor_id", sensorId)
                        }
                    }
            }

            true
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao atualizar configuração", e)
            false
        }
    }

    suspend fun excluirSensorDaConta(sensorId: String): Boolean {
        return try {
            val userId = Session.userId ?: return false

            supabase
                .from("alert_rules")
                .delete {
                    filter {
                        eq("sensor_id", sensorId)
                    }
                }

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
            Log.e("ERRO_SENSOR", "Erro ao excluir sensor", e)
            false
        }
    }
}

@Serializable
data class AlertRuleInsert(
    val sensor_id: String,
    val enabled: Boolean = true,
    val temp_limit: Double? = null,
    val cooldown_sec: Int = 0,
    val temp_limit_max: Double? = null,
    val temp_limit_min: Double? = null
)

@Serializable
data class AlertRuleDTO(
    val id: Int,
    val sensor_id: String,
    val temp_limit_max: Double? = null,
    val temp_limit_min: Double? = null
)
