package com.example.triade_monitoramento.ui.temperature

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triade_monitoramento.data.model.LatestTemperatureDto
import com.example.triade_monitoramento.data.model.TemperaturePointDto
import com.example.triade_monitoramento.data.model.UserSensor
import com.example.triade_monitoramento.data.repository.TemperatureRepository
import com.example.triade_monitoramento.ui.sensor.SensorListItemUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

class TemperatureViewModel(
    private val repo: TemperatureRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TEMPERATURE_VM"

        private const val DEFAULT_HISTORY_RANGE = "-30m"
        private const val DEFAULT_HISTORY_EVERY = "10s"
        private const val DEFAULT_POLL_MS = 5_000L
        private const val DEFAULT_MAX_POINTS = 180
    }

    private val _state = MutableStateFlow(
        TemperatureUiState(
            latestTemp = null,
            latestHum = null,
            latestTs = null,
            chartPoints = emptyList(),
            isLoading = false,
            error = null,
            periodStartIso = null,
            periodStopIso = null,
            sensores = emptyList(),
            sensorSelecionado = null
        )
    )

    val state: StateFlow<TemperatureUiState> = _state

    private val _sensoresRealtime =
        MutableStateFlow<List<SensorListItemUi>>(emptyList())

    val sensoresRealtime: StateFlow<List<SensorListItemUi>> =
        _sensoresRealtime

    private var loopJob: Job? = null
    private var sensoresRealtimeJob: Job? = null

    private var activeSensorId: String? = null
    private var streamingGeneration: Long = 0L

    fun startStreaming(
        id: String,
        historyRange: String = DEFAULT_HISTORY_RANGE,
        historyEvery: String = DEFAULT_HISTORY_EVERY,
        pollLatestMs: Long = DEFAULT_POLL_MS,
        maxPoints: Int? = DEFAULT_MAX_POINTS
    ) {
        val sensorId = id.trim()

        if (sensorId.isBlank()) {
            stopStreaming()

            _state.update {
                it.copy(
                    isLoading = false,
                    error = "ID do sensor não informado"
                )
            }

            return
        }

        /*
         * Evita iniciar outro job quando já existe um streaming
         * em tempo real para o mesmo sensor.
         */
        if (
            loopJob?.isActive == true &&
            activeSensorId == sensorId &&
            _state.value.periodStartIso == null &&
            _state.value.periodStopIso == null
        ) {
            Log.d(
                TAG,
                "Streaming já está ativo para $sensorId"
            )

            return
        }

        stopStreaming()

        activeSensorId = sensorId
        streamingGeneration++

        val currentGeneration = streamingGeneration

        Log.d(
            TAG,
            "startStreaming: id=$sensorId, " +
                    "range=$historyRange, " +
                    "every=$historyEvery, " +
                    "generation=$currentGeneration"
        )

        loopJob = viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    latestTemp = null,
                    latestHum = null,
                    latestTs = null,
                    chartPoints = emptyList(),
                    isLoading = true,
                    error = null,
                    periodStartIso = null,
                    periodStopIso = null
                )
            }

            var initialLatest: LatestTemperatureDto? = null
            var latestError: String? = null

            try {
                initialLatest = repo.latest(sensorId)

                if (
                    !isCurrentRequest(
                        sensorId = sensorId,
                        generation = currentGeneration
                    )
                ) {
                    return@launch
                }

                Log.d(
                    TAG,
                    "Latest recebido: " +
                            "id=$sensorId, " +
                            "temperatura=${initialLatest.temperatura}, " +
                            "umidade=${initialLatest.umidade}, " +
                            "timestamp=${initialLatest.ts}"
                )

                _state.update { current ->
                    current.copy(
                        latestTemp = initialLatest.temperatura,
                        latestHum = initialLatest.umidade,
                        latestTs = initialLatest.ts
                    )
                }
            } catch (e: Exception) {
                if (
                    !isCurrentRequest(
                        sensorId = sensorId,
                        generation = currentGeneration
                    )
                ) {
                    return@launch
                }

                latestError =
                    if (isNotFound(e)) {
                        "Sensor sem leitura atual"
                    } else {
                        "Leitura atual: ${
                            e.message ?: "erro desconhecido"
                        }"
                    }

                Log.w(
                    TAG,
                    "Não foi possível obter latest de " +
                            "$sensorId: $latestError"
                )
            }

            try {
                val history = repo.history(
                    id = sensorId,
                    range = normalizeRange(historyRange),
                    every = historyEvery
                )

                if (
                    !isCurrentRequest(
                        sensorId = sensorId,
                        generation = currentGeneration
                    )
                ) {
                    return@launch
                }

                val validHistory =
                    prepareHistory(history)

                val points = appendPointIfNew(
                    currentPoints = validHistory,
                    newPoint =
                        initialLatest
                            ?.toTemperaturePointOrNull()
                )

                val limitedPoints = applyLimit(
                    points = points,
                    maxPoints = maxPoints
                )

                Log.d(
                    TAG,
                    "Histórico preparado: " +
                            "id=$sensorId, " +
                            "recebidos=${history.size}, " +
                            "válidos=${validHistory.size}, " +
                            "gráfico=${limitedPoints.size}"
                )

                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        chartPoints = limitedPoints,
                        error =
                            if (limitedPoints.isEmpty()) {
                                latestError
                                    ?: "Nenhum dado encontrado para $sensorId"
                            } else {
                                null
                            }
                    )
                }
            } catch (e: Exception) {
                if (
                    !isCurrentRequest(
                        sensorId = sensorId,
                        generation = currentGeneration
                    )
                ) {
                    return@launch
                }

                Log.e(
                    TAG,
                    "Erro ao buscar histórico de $sensorId",
                    e
                )

                val latestPoint =
                    initialLatest
                        ?.toTemperaturePointOrNull()

                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        chartPoints =
                            latestPoint
                                ?.let(::listOf)
                                ?: emptyList(),
                        error =
                            "Histórico: ${
                                e.message ?: "erro desconhecido"
                            }"
                    )
                }
            }

            while (
                isActive &&
                isCurrentRequest(
                    sensorId = sensorId,
                    generation = currentGeneration
                )
            ) {
                delay(
                    pollLatestMs.coerceAtLeast(1_000L)
                )

                try {
                    val latest =
                        repo.latest(sensorId)

                    if (
                        !isCurrentRequest(
                            sensorId = sensorId,
                            generation = currentGeneration
                        )
                    ) {
                        return@launch
                    }

                    val newPoint =
                        latest.toTemperaturePointOrNull()

                    _state.update { current ->
                        current.copy(
                            latestTemp =
                                latest.temperatura
                                    ?: current.latestTemp,
                            latestHum =
                                latest.umidade
                                    ?: current.latestHum,
                            latestTs =
                                latest.ts
                                    ?: current.latestTs,
                            chartPoints =
                                applyLimit(
                                    points =
                                        appendPointIfNew(
                                            currentPoints =
                                                current.chartPoints,
                                            newPoint =
                                                newPoint
                                        ),
                                    maxPoints = maxPoints
                                ),
                            error = null
                        )
                    }

                    Log.d(
                        TAG,
                        "Polling atualizado: " +
                                "id=$sensorId, " +
                                "ts=${latest.ts}, " +
                                "pontos=${_state.value.chartPoints.size}"
                    )
                } catch (e: Exception) {
                    if (
                        !isCurrentRequest(
                            sensorId = sensorId,
                            generation = currentGeneration
                        )
                    ) {
                        return@launch
                    }

                    val message =
                        if (isNotFound(e)) {
                            "Sensor sem leitura registrada"
                        } else {
                            "Atualização: ${
                                e.message ?: "erro desconhecido"
                            }"
                        }

                    Log.w(
                        TAG,
                        "Erro ao atualizar $sensorId: $message"
                    )

                    _state.update { current ->
                        current.copy(
                            error = message
                        )
                    }
                }
            }
        }
    }

    fun refreshRealtime(
        id: String,
        historyRange: String = DEFAULT_HISTORY_RANGE,
        historyEvery: String = DEFAULT_HISTORY_EVERY,
        maxPoints: Int? = DEFAULT_MAX_POINTS
    ) {
        startStreaming(
            id = id,
            historyRange = historyRange,
            historyEvery = historyEvery,
            pollLatestMs = DEFAULT_POLL_MS,
            maxPoints = maxPoints
        )
    }

    fun loadHistoryByPeriod(
        id: String,
        startIso: String,
        stopIso: String,
        every: String? = null,
        maxPoints: Int? = DEFAULT_MAX_POINTS
    ) {
        val sensorId = id.trim()

        if (sensorId.isBlank()) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = "ID do sensor não informado"
                )
            }

            return
        }

        stopStreaming()

        activeSensorId = sensorId
        streamingGeneration++

        val currentGeneration =
            streamingGeneration

        loopJob = viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    isLoading = true,
                    error = null,
                    chartPoints = emptyList(),
                    periodStartIso = startIso,
                    periodStopIso = stopIso
                )
            }

            try {
                val resolvedEvery =
                    every ?: suggestEveryForPeriod(
                        startIso = startIso,
                        stopIso = stopIso
                    )

                val history =
                    repo.historyByPeriod(
                        id = sensorId,
                        startIso = startIso,
                        stopIso = stopIso,
                        every = resolvedEvery
                    )

                if (
                    !isCurrentRequest(
                        sensorId = sensorId,
                        generation = currentGeneration
                    )
                ) {
                    return@launch
                }

                val preparedHistory =
                    applyLimit(
                        points =
                            prepareHistory(history),
                        maxPoints = maxPoints
                    )

                val last =
                    preparedHistory.lastOrNull()

                Log.d(
                    TAG,
                    "Histórico por período: " +
                            "id=$sensorId, " +
                            "recebidos=${history.size}, " +
                            "gráfico=${preparedHistory.size}, " +
                            "every=$resolvedEvery"
                )

                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        chartPoints = preparedHistory,
                        latestTemp =
                            last?.temperatura
                                ?: current.latestTemp,
                        latestHum =
                            last?.umidade
                                ?: current.latestHum,
                        latestTs =
                            last?.ts
                                ?: current.latestTs,
                        error =
                            if (preparedHistory.isEmpty()) {
                                "Nenhum dado encontrado no período selecionado"
                            } else {
                                null
                            },
                        periodStartIso = startIso,
                        periodStopIso = stopIso
                    )
                }
            } catch (e: Exception) {
                if (
                    !isCurrentRequest(
                        sensorId = sensorId,
                        generation = currentGeneration
                    )
                ) {
                    return@launch
                }

                Log.e(
                    TAG,
                    "Erro no histórico por período: $sensorId",
                    e
                )

                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        chartPoints = emptyList(),
                        error =
                            e.message
                                ?: "Erro ao carregar histórico",
                        periodStartIso = startIso,
                        periodStopIso = stopIso
                    )
                }
            }
        }
    }

    fun startSensoresRealtime(
        sensores: List<UserSensor>,
        pollMs: Long = DEFAULT_POLL_MS
    ) {
        stopSensoresRealtime()

        val validSensors =
            sensores
                .filter { sensor ->
                    sensor.sensorId
                        .trim()
                        .isNotBlank()
                }
                .distinctBy { sensor ->
                    sensor.sensorId.trim()
                }

        if (validSensors.isEmpty()) {
            _sensoresRealtime.value =
                emptyList()

            return
        }

        sensoresRealtimeJob =
            viewModelScope.launch {
                while (isActive) {
                    val selectedId =
                        activeSensorId

                    val temperatureState =
                        _state.value

                    val updatedList =
                        validSensors.map { sensor ->
                            val sensorId =
                                sensor.sensorId.trim()

                            /*
                             * O sensor aberto no gráfico já é atualizado
                             * pelo startStreaming. Assim evitamos duas
                             * chamadas latest simultâneas.
                             */
                            val latest =
                                if (sensorId == selectedId) {
                                    null
                                } else {
                                    try {
                                        repo.latest(sensorId)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }

                            SensorListItemUi(
                                sensorId = sensorId,
                                nome =
                                    sensor.displayName(),
                                temperaturaAtual =
                                    if (sensorId == selectedId) {
                                        temperatureState.latestTemp
                                    } else {
                                        latest?.temperatura
                                    },
                                umidadeAtual =
                                    if (sensorId == selectedId) {
                                        temperatureState.latestHum
                                    } else {
                                        latest?.umidade
                                    },
                                tempLimitMax = null,
                                tempLimitMin = null
                            )
                        }

                    _sensoresRealtime.value =
                        updatedList

                    delay(
                        pollMs.coerceAtLeast(1_000L)
                    )
                }
            }
    }

    fun stopSensoresRealtime() {
        sensoresRealtimeJob?.cancel()
        sensoresRealtimeJob = null
    }

    fun stopStreaming() {
        streamingGeneration++

        loopJob?.cancel()
        loopJob = null

        activeSensorId = null
    }

    fun setSensores(
        sensores: List<UserSensor>,
        sensorSelecionado: UserSensor? = null
    ) {
        val normalizedSensors =
            sensores
                .filter { sensor ->
                    sensor.sensorId
                        .trim()
                        .isNotBlank()
                }
                .distinctBy { sensor ->
                    sensor.sensorId.trim()
                }

        if (normalizedSensors.isEmpty()) {
            stopStreaming()
            stopSensoresRealtime()

            _sensoresRealtime.value =
                emptyList()

            _state.update { current ->
                current.copy(
                    sensores = emptyList(),
                    sensorSelecionado = null,
                    latestTemp = null,
                    latestHum = null,
                    latestTs = null,
                    chartPoints = emptyList(),
                    isLoading = false,
                    error = null,
                    periodStartIso = null,
                    periodStopIso = null
                )
            }

            return
        }

        val currentId =
            _state.value
                .sensorSelecionado
                ?.sensorId
                ?.trim()

        val requestedId =
            sensorSelecionado
                ?.sensorId
                ?.trim()

        val selected =
            normalizedSensors.firstOrNull { sensor ->
                sensor.sensorId.trim() == currentId
            }
                ?: normalizedSensors.firstOrNull { sensor ->
                    sensor.sensorId.trim() == requestedId
                }
                ?: normalizedSensors.firstOrNull { sensor ->
                    sensor.sensorId.trim() == "TRD1003"
                }
                ?: normalizedSensors.first()

        val selectedId =
            selected.sensorId.trim()

        val sensorChanged =
            currentId != selectedId

        if (sensorChanged) {
            stopStreaming()
        }

        _state.update { current ->
            current.copy(
                sensores = normalizedSensors,
                sensorSelecionado = selected,
                latestTemp =
                    if (sensorChanged) {
                        null
                    } else {
                        current.latestTemp
                    },
                latestHum =
                    if (sensorChanged) {
                        null
                    } else {
                        current.latestHum
                    },
                latestTs =
                    if (sensorChanged) {
                        null
                    } else {
                        current.latestTs
                    },
                chartPoints =
                    if (sensorChanged) {
                        emptyList()
                    } else {
                        current.chartPoints
                    },
                periodStartIso =
                    if (sensorChanged) {
                        null
                    } else {
                        current.periodStartIso
                    },
                periodStopIso =
                    if (sensorChanged) {
                        null
                    } else {
                        current.periodStopIso
                    },
                error = null
            )
        }

        startSensoresRealtime(
            normalizedSensors
        )

        Log.d(
            TAG,
            "Sensores definidos. " +
                    "Selecionado=$selectedId, " +
                    "alterado=$sensorChanged"
        )
    }

    fun selecionarSensor(
        sensor: UserSensor
    ) {
        val sensorId =
            sensor.sensorId.trim()

        if (sensorId.isBlank()) {
            _state.update { current ->
                current.copy(
                    error =
                        "O sensor selecionado não possui ID"
                )
            }

            return
        }

        val currentSensorId =
            _state.value
                .sensorSelecionado
                ?.sensorId
                ?.trim()

        if (currentSensorId == sensorId) {
            return
        }

        stopStreaming()

        _state.update { current ->
            current.copy(
                sensorSelecionado = sensor,
                latestTemp = null,
                latestHum = null,
                latestTs = null,
                chartPoints = emptyList(),
                isLoading = true,
                error = null,
                periodStartIso = null,
                periodStopIso = null
            )
        }

        Log.d(
            TAG,
            "Sensor selecionado: $sensorId"
        )
    }

    fun clearSession() {
        stopStreaming()
        stopSensoresRealtime()

        _sensoresRealtime.value =
            emptyList()

        _state.value =
            TemperatureUiState(
                latestTemp = null,
                latestHum = null,
                latestTs = null,
                chartPoints = emptyList(),
                isLoading = false,
                error = null,
                periodStartIso = null,
                periodStopIso = null,
                sensores = emptyList(),
                sensorSelecionado = null
            )
    }

    override fun onCleared() {
        stopStreaming()
        stopSensoresRealtime()
        super.onCleared()
    }

    private fun isCurrentRequest(
        sensorId: String,
        generation: Long
    ): Boolean {
        return activeSensorId == sensorId &&
                streamingGeneration == generation
    }

    private fun isNotFound(
        exception: Exception
    ): Boolean {
        return (
                exception as? HttpException
                )?.code() == 404
    }

    private fun prepareHistory(
        history: List<TemperaturePointDto>
    ): List<TemperaturePointDto> {
        return history
            .filter { point ->
                val hasTimestamp =
                    !point.ts.isNullOrBlank()

                val hasMeasurement =
                    point.temperatura != null ||
                            point.umidade != null

                hasTimestamp &&
                        hasMeasurement &&
                        parseTimestampMillis(
                            point.ts
                        ) != null
            }
            .distinctBy { point ->
                point.ts
            }
            .sortedBy { point ->
                parseTimestampMillis(
                    point.ts
                ) ?: Long.MAX_VALUE
            }
    }

    private fun appendPointIfNew(
        currentPoints: List<TemperaturePointDto>,
        newPoint: TemperaturePointDto?
    ): List<TemperaturePointDto> {
        if (newPoint == null) {
            return currentPoints
        }

        val alreadyExists =
            currentPoints.any { point ->
                point.ts == newPoint.ts
            }

        if (alreadyExists) {
            return currentPoints
        }

        return currentPoints + newPoint
    }

    private fun applyLimit(
        points: List<TemperaturePointDto>,
        maxPoints: Int?
    ): List<TemperaturePointDto> {
        val sortedPoints =
            points.sortedBy { point ->
                parseTimestampMillis(
                    point.ts
                ) ?: Long.MAX_VALUE
            }

        if (
            maxPoints == null ||
            maxPoints <= 0
        ) {
            return sortedPoints
        }

        return if (sortedPoints.size > maxPoints) {
            sortedPoints.takeLast(maxPoints)
        } else {
            sortedPoints
        }
    }

    private fun parseTimestampMillis(
        timestamp: String?
    ): Long? {
        if (timestamp.isNullOrBlank()) {
            return null
        }

        return try {
            OffsetDateTime
                .parse(timestamp)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            try {
                Instant
                    .parse(timestamp)
                    .toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun normalizeRange(
        range: String
    ): String {
        val value =
            range.trim()

        if (value.isBlank()) {
            return DEFAULT_HISTORY_RANGE
        }

        return if (value.startsWith("-")) {
            value
        } else {
            "-$value"
        }
    }

    private fun suggestEveryForPeriod(
        startIso: String,
        stopIso: String
    ): String {
        return try {
            val start =
                OffsetDateTime.parse(startIso)

            val stop =
                OffsetDateTime.parse(stopIso)

            val minutes =
                Duration
                    .between(start, stop)
                    .toMinutes()
                    .coerceAtLeast(1)

            when {
                minutes <= 120 -> "10s"
                minutes <= 360 -> "30s"
                minutes <= 720 -> "1m"
                minutes <= 1_440 -> "5m"
                minutes <= 10_080 -> "15m"
                else -> "1h"
            }
        } catch (_: Exception) {
            "1m"
        }
    }
}

private fun LatestTemperatureDto
        .toTemperaturePointOrNull():
        TemperaturePointDto? {

    val timestamp = ts
    val temperature = temperatura
    val humidity = umidade

    if (
        timestamp.isNullOrBlank() ||
        (
                temperature == null &&
                        humidity == null
                )
    ) {
        return null
    }

    return TemperaturePointDto(
        ts = timestamp,
        temperatura = temperatura,
        umidade = umidade,
        porta = porta
    )
}