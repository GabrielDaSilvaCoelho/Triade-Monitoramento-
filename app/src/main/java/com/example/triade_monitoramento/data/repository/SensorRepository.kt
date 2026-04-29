package com.example.triade_monitoramento.data.repository

import android.util.Log
import com.example.triade_monitoramento.Session
import com.example.triade_monitoramento.data.model.SensorDTO
import com.example.triade_monitoramento.data.model.SensorInsert
import io.github.jan.supabase.SupabaseClient
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

            val sensorExiste = supabase
                .from("sensores")
                .select {
                    filter {
                        eq("id", id)
                        eq("owner_id", userId)
                    }
                }
                .decodeList<SensorDTO>()
                .isNotEmpty()

            if (!sensorExiste) {
                val sensor = SensorInsert(
                    id = id,
                    nome = nome,
                    owner_id = userId
                )

                supabase
                    .from("sensores")
                    .insert(sensor)
            } else {
                supabase
                    .from("sensores")
                    .update(
                        {
                            set("nome", nome)
                        }
                    ) {
                        filter {
                            eq("id", id)
                            eq("owner_id", userId)
                        }
                    }
            }

            val regraExistente = buscarRegraAlerta(id)

            if (regraExistente == null) {
                val regra = AlertRuleInsert(
                    sensor_id = id,
                    enabled = true,
                    temp_limit = null,
                    cooldown_sec = 0,
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

    suspend fun buscarSensoresDoUsuario(): List<SensorConfigData> {
        val userId = Session.userId ?: return emptyList()

        return try {
            val sensores = supabase
                .from("sensores")
                .select {
                    filter {
                        eq("owner_id", userId)
                    }
                }
                .decodeList<SensorDTO>()

            sensores.map { sensor ->
                val regra = buscarRegraAlerta(sensor.id)

                SensorConfigData(
                    sensorId = sensor.id,
                    nome = sensor.nome,
                    tempLimitMax = regra?.temp_limit_max,
                    tempLimitMin = regra?.temp_limit_min
                )
            }
        } catch (e: Exception) {
            Log.e("SENSOR_DEBUG", "Erro ao buscar sensores", e)
            emptyList()
        }
    }

    suspend fun buscarSensorPorId(sensorId: String): SensorConfigData? {
        return try {
            val userId = Session.userId ?: return null

            val sensor = supabase
                .from("sensores")
                .select {
                    filter {
                        eq("id", sensorId)
                        eq("owner_id", userId)
                    }
                }
                .decodeList<SensorDTO>()
                .firstOrNull()
                ?: return null

            val regra = buscarRegraAlerta(sensorId)

            SensorConfigData(
                sensorId = sensor.id,
                nome = sensor.nome,
                tempLimitMax = regra?.temp_limit_max,
                tempLimitMin = regra?.temp_limit_min
            )
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao buscar sensor por ID", e)
            null
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

            val regraExistente = buscarRegraAlerta(sensorId)

            if (regraExistente == null) {
                val regra = AlertRuleInsert(
                    sensor_id = sensorId,
                    enabled = true,
                    temp_limit = null,
                    cooldown_sec = 0,
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

    private suspend fun buscarRegraAlerta(sensorId: String): AlertRuleDTO? {
        return try {
            supabase
                .from("alert_rules")
                .select {
                    filter {
                        eq("sensor_id", sensorId)
                    }
                }
                .decodeList<AlertRuleDTO>()
                .firstOrNull()
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao buscar regra de alerta", e)
            null
        }
    }
}

@Serializable
data class SensorConfigData(
    val sensorId: String,
    val nome: String? = null,
    val tempLimitMax: Double? = null,
    val tempLimitMin: Double? = null
)

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
    val id: Int? = null,
    val sensor_id: String,
    val temp_limit_max: Double? = null,
    val temp_limit_min: Double? = null
)