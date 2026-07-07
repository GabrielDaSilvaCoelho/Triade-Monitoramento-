package com.example.triade_monitoramento.data.repository

import android.util.Log
import com.example.triade_monitoramento.Session
import com.example.triade_monitoramento.data.model.SensorDTO
import com.example.triade_monitoramento.data.model.SensorInsert
import com.example.triade_monitoramento.data.model.SensorNotificacaoDTO
import com.example.triade_monitoramento.data.model.SensorNotificacaoInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

class SensorRepository(
    private val supabase: SupabaseClient
) {

    suspend fun marcarAlertaComoCiente(sensorId: String): Boolean {
        return try {
            supabase
                .from("alert_rules")
                .update({ set("acknowledged", true) }) {
                    filter { eq("sensor_id", sensorId) }
                }

            true
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao marcar alerta como ciente", e)
            false
        }
    }

    suspend fun listarNotificacoes(sensorId: String): List<SensorNotificacaoDTO> {
        return try {
            supabase
                .from("sensor_notificacoes")
                .select {
                    filter { eq("sensor_id", sensorId) }
                }
                .decodeList<SensorNotificacaoDTO>()
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao listar notificações", e)
            emptyList()
        }
    }

    suspend fun adicionarNotificacao(
        sensorId: String,
        tipo: String,
        destino: String
    ): Boolean {
        return try {
            val destinoLimpo = destino.trim()

            if (sensorId.isBlank() || tipo.isBlank() || destinoLimpo.isBlank()) {
                return false
            }

            val notificacao = SensorNotificacaoInsert(
                sensor_id = sensorId,
                tipo = tipo,
                destino = destinoLimpo,
                enabled = true
            )

            supabase
                .from("sensor_notificacoes")
                .insert(notificacao)

            true
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao adicionar notificação", e)
            false
        }
    }

    suspend fun excluirNotificacao(id: Long): Boolean {
        return try {
            supabase
                .from("sensor_notificacoes")
                .delete {
                    filter { eq("id", id) }
                }

            true
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao excluir notificação", e)
            false
        }
    }

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
                    owner_id = userId,
                    temp_min = tempLimitMin,
                    temp_max = tempLimitMax
                )

                supabase
                    .from("sensores")
                    .insert(sensor)
            } else {
                supabase
                    .from("sensores")
                    .update({ set("nome", nome) }) {
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
                    temp_limit_min = tempLimitMin,
                    acknowledged = false
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
                            set("acknowledged", false)
                        }
                    ) {
                        filter { eq("sensor_id", id) }
                    }
            }

            adicionarContatoBaseSeNaoExistir(id)

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
                    filter { eq("owner_id", userId) }
                }
                .decodeList<SensorDTO>()

            sensores.map { sensor ->
                val regra = buscarRegraAlerta(sensor.id)

                SensorConfigData(
                    sensorId = sensor.id,
                    nome = sensor.nome,
                    tempLimitMax = regra?.temp_limit_max,
                    tempLimitMin = regra?.temp_limit_min,
                    acknowledged = regra?.acknowledged ?: false
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
                tempLimitMin = regra?.temp_limit_min,
                acknowledged = regra?.acknowledged ?: false
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
                .update({ set("nome", nome) }) {
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
                    temp_limit_min = tempLimitMin,
                    acknowledged = false
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
                            set("acknowledged", false)
                        }
                    ) {
                        filter { eq("sensor_id", sensorId) }
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
                .from("sensor_notificacoes")
                .delete {
                    filter { eq("sensor_id", sensorId) }
                }

            supabase
                .from("alert_rules")
                .delete {
                    filter { eq("sensor_id", sensorId) }
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

    private suspend fun adicionarContatoBaseSeNaoExistir(sensorId: String) {
        try {
            val usuario = buscarContatoUsuarioAtual() ?: return
            val contatosAtuais = listarNotificacoes(sensorId)

            val telefone = usuario.telefone?.trim()
            val email = usuario.email?.trim()

            if (!telefone.isNullOrBlank()) {
                val telefoneLimpo = telefone.replace(Regex("\\D"), "")

                val jaExisteTelefone = contatosAtuais.any {
                    it.tipo == "whatsapp" &&
                            it.destino.replace(Regex("\\D"), "") == telefoneLimpo
                }

                if (!jaExisteTelefone) {
                    adicionarNotificacao(
                        sensorId = sensorId,
                        tipo = "whatsapp",
                        destino = telefoneLimpo
                    )
                }
            }

            if (!email.isNullOrBlank()) {
                val jaExisteEmail = contatosAtuais.any {
                    it.tipo == "email" &&
                            it.destino.equals(email, ignoreCase = true)
                }

                if (!jaExisteEmail) {
                    adicionarNotificacao(
                        sensorId = sensorId,
                        tipo = "email",
                        destino = email
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao adicionar contato base", e)
        }
    }

    private suspend fun buscarContatoUsuarioAtual(): UsuarioContatoDTO? {
        return try {
            val userId = Session.userId ?: return null

            supabase
                .from("usuario")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeList<UsuarioContatoDTO>()
                .firstOrNull()
        } catch (e: Exception) {
            Log.e("ERRO_SENSOR", "Erro ao buscar contato do usuário", e)
            null
        }
    }

    private suspend fun buscarRegraAlerta(sensorId: String): AlertRuleDTO? {
        return try {
            supabase
                .from("alert_rules")
                .select {
                    filter { eq("sensor_id", sensorId) }
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
    val tempLimitMin: Double? = null,
    val acknowledged: Boolean = false
)

@Serializable
data class AlertRuleInsert(
    val sensor_id: String,
    val enabled: Boolean = true,
    val temp_limit: Double? = null,
    val cooldown_sec: Int = 0,
    val temp_limit_max: Double? = null,
    val temp_limit_min: Double? = null,
    val acknowledged: Boolean = false
)

@Serializable
data class AlertRuleDTO(
    val id: Int? = null,
    val sensor_id: String,
    val temp_limit_max: Double? = null,
    val temp_limit_min: Double? = null,
    val acknowledged: Boolean = false
)

@Serializable
data class UsuarioContatoDTO(
    val id: Int,
    val email: String? = null,
    val telefone: String? = null
)
