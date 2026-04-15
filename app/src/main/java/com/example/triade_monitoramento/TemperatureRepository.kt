package com.example.triade_monitoramento

class TemperatureRepository(
    private val api: TemperatureApi
) {
    suspend fun latest(id: String) =
        api.getLatest(id)

    suspend fun history(
        id: String,
        range: String,
        every: String
    ) = api.getHistory(
        id = id,
        range = range,
        every = every
    )

    suspend fun historyByPeriod(
        id: String,
        startIso: String,
        stopIso: String,
        every: String = "10s"
    ) = api.getHistoryByPeriod(
        id = id,
        start = startIso,
        stop = stopIso,
        every = every
    )
}