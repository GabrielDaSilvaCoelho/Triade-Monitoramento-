package com.example.triade_monitoramento.data.repository

import com.example.triade_monitoramento.data.api.TemperatureApi
import com.example.triade_monitoramento.ui.sensor.PortaAbertaItem
import com.example.triade_monitoramento.ui.sensor.PortaConfigDto
import com.example.triade_monitoramento.ui.sensor.PortaConfigRequestDto
import com.example.triade_monitoramento.data.model.TemperaturePointDto

data class PortaEventosResumo(
    val sensorId: String,
    val sensorNome: String,
    val yellowAfterSeconds: Int,
    val redAfterSeconds: Int,
    val amarelos: Int,
    val vermelhos: Int,
    val eventos: List<PortaAbertaItem>
)

class PortaRepository(
    private val api: TemperatureApi
) {
    suspend fun historyByPeriod(
        id: String,
        startIso: String,
        stopIso: String,
        every: String
    ): List<TemperaturePointDto> {
        return api.getHistoryByPeriod(
            id = id,
            startIso = startIso,
            stopIso = stopIso,
            every = every
        )
    }
    suspend fun buscarEventosPorta(
        sensorId: String,
        yellow: Int = 60,
        red: Int = 300
    ): PortaEventosResumo {
        val response = api.buscarEventosPorta(
            sensorId = sensorId,
            yellow = yellow,
            red = red
        )

        val nomeSensor = response.sensorNome ?: response.sensorId ?: sensorId

        val eventos = response.eventos.map { evento ->
            PortaAbertaItem(
                sensorId = evento.sensorId,
                sensorNome = evento.sensorNome.ifBlank { nomeSensor },
                local = evento.sensorNome.ifBlank { nomeSensor },
                dataHora = evento.openedAt ?: "--",
                openedAt = evento.openedAt,
                closedAt = evento.closedAt,
                durationSeconds = evento.durationSeconds,
                nivel = evento.nivel,
                status = when (evento.nivel.lowercase()) {
                    "amarelo" -> "Alerta amarelo"
                    "vermelho" -> "Alerta vermelho"
                    else -> "Normal"
                }
            )
        }

        return PortaEventosResumo(
            sensorId = response.sensorId ?: sensorId,
            sensorNome = nomeSensor,
            yellowAfterSeconds = response.yellowAfterSeconds,
            redAfterSeconds = response.redAfterSeconds,
            amarelos = response.amarelos,
            vermelhos = response.vermelhos,
            eventos = eventos
        )
    }

    suspend fun buscarConfigPorta(sensorId: String): PortaConfigDto {
        return api.buscarConfigPorta(sensorId)
    }

    suspend fun salvarConfigPorta(
        sensorId: String,
        yellowAfterSeconds: Int,
        redAfterSeconds: Int
    ): PortaConfigDto {
        return api.salvarConfigPorta(
            PortaConfigRequestDto(
                sensorId = sensorId,
                yellowAfterSeconds = yellowAfterSeconds,
                redAfterSeconds = redAfterSeconds
            )
        )
    }
}