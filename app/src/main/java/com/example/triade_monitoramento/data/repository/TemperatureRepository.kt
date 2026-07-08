package com.example.triade_monitoramento.data.repository

import com.example.triade_monitoramento.data.api.TemperatureApi
import com.example.triade_monitoramento.data.model.LatestTemperatureDto
import com.example.triade_monitoramento.data.model.TemperaturePointDto

class TemperatureRepository(
    private val api: TemperatureApi
) {

    suspend fun latest(
        id: String
    ): LatestTemperatureDto {
        return api.getLatest(id)
    }

    suspend fun history(
        id: String,
        range: String,
        every: String
    ): List<TemperaturePointDto> {
        return api.getHistory(
            id = id,
            range = normalizeRange(range),
            every = every
        )
    }
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

    private fun normalizeRange(range: String): String {
        val value = range.trim()

        return if (value.startsWith("-")) {
            value
        } else {
            "-$value"
        }
    }
}