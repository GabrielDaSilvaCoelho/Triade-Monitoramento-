package com.example.triade_monitoramento.data.repository

import com.example.triade_monitoramento.data.api.TemperatureApi
import com.example.triade_monitoramento.ui.sensor.PortaAbertaItem
import com.example.triade_monitoramento.ui.sensor.PortaConfigDto
import com.example.triade_monitoramento.ui.sensor.PortaConfigRequestDto


data class PortaEventosResumo(
    val sensorId: String,
    val sensorNome: String,
    val yellowAfterMinutes: Int,
    val redAfterMinutes: Int,
    val amarelos: Int,
    val vermelhos: Int,
    val eventos: List<PortaAbertaItem>
)

class PortaRepository(
    private val api: TemperatureApi
) {
    suspend fun buscarEventosPorta(
        sensorId: String,
        yellow: Int = 1,
        red: Int = 5
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
                sensorNome = evento.sensorNome,
                local = "Sensor ${evento.sensorNome}",
                dataHora = evento.openedAt ?: "--",
                status = when (evento.nivel) {
                    "amarelo" -> "Alerta amarelo - ${evento.durationMin} min"
                    "vermelho" -> "Alerta vermelho - ${evento.durationMin} min"
                    else -> "Normal - ${evento.durationMin} min"
                }
            )
        }

        return PortaEventosResumo(
            sensorId = response.sensorId ?: sensorId,
            sensorNome = nomeSensor,
            yellowAfterMinutes = response.yellowAfterMinutes,
            redAfterMinutes = response.redAfterMinutes,
            amarelos = response.amarelos,
            vermelhos = response.vermelhos,
            eventos = eventos
        )
    }

    suspend fun buscarConfigPorta(
        sensorId: String
    ): PortaConfigDto {
        return api.buscarConfigPorta(sensorId)
    }

    suspend fun salvarConfigPorta(
        sensorId: String,
        yellowAfterMinutes: Int,
        redAfterMinutes: Int
    ): PortaConfigDto {

        return api.salvarConfigPorta(
            PortaConfigRequestDto(
                sensorId = sensorId,
                yellowAfterMinutes = yellowAfterMinutes,
                redAfterMinutes = redAfterMinutes
            )
        )
    }
}