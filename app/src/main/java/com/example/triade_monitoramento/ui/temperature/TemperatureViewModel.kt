package com.example.triade_monitoramento.ui.temperature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.Duration
import java.time.OffsetDateTime

class TemperatureViewModel(
    private val repo: TemperatureRepository
) : ViewModel() {

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

    private val _sensoresRealtime = MutableStateFlow<List<SensorListItemUi>>(emptyList())
    val sensoresRealtime: StateFlow<List<SensorListItemUi>> = _sensoresRealtime

    private var loopJob: Job? = null
    private var sensoresRealtimeJob: Job? = null

    fun startStreaming(
        id: String,
        historyRange: String = "1h",
        historyEvery: String = "5s",
        pollLatestMs: Long = 5_000L,
        maxPoints: Int? = 360
    ) {
        stopStreaming()

        loopJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    periodStartIso = null,
                    periodStopIso = null
                )
            }

            try {
                val history = repo.history(id, historyRange, historyEvery)
                val preparedHistory = applyLimit(history, maxPoints)
                val latest = repo.latest(id)

                val latestPoint =
                    if (latest.ts != null && latest.temperatura != null && latest.umidade != null) {
                        TemperaturePointDto(
                            ts = latest.ts,
                            temperatura = latest.temperatura,
                            umidade = latest.umidade
                        )
                    } else {
                        null
                    }

                val initialPoints = appendPointIfNew(preparedHistory, latestPoint)
                    .let { applyLimit(it, maxPoints) }

                _state.update {
                    it.copy(
                        isLoading = false,
                        latestTemp = latest.temperatura,
                        latestHum = latest.umidade,
                        latestTs = latest.ts,
                        chartPoints = initialPoints,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erro desconhecido"
                    )
                }
            }

            while (isActive) {
                try {
                    val latest = repo.latest(id)

                    val newPoint =
                        if (latest.ts != null && latest.temperatura != null && latest.umidade != null) {
                            TemperaturePointDto(
                                ts = latest.ts,
                                temperatura = latest.temperatura,
                                umidade = latest.umidade
                            )
                        } else {
                            null
                        }

                    _state.update { currentState ->
                        val appended = appendPointIfNew(currentState.chartPoints, newPoint)
                        val prepared = applyLimit(appended, maxPoints)

                        currentState.copy(
                            latestTemp = latest.temperatura ?: currentState.latestTemp,
                            latestHum = latest.umidade ?: currentState.latestHum,
                            latestTs = latest.ts ?: currentState.latestTs,
                            chartPoints = prepared,
                            error = null
                        )
                    }
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            error = e.message ?: "Erro desconhecido"
                        )
                    }
                }

                delay(pollLatestMs)
            }
        }
    }

    fun refreshRealtime(
        id: String,
        historyRange: String = "1h",
        historyEvery: String = "10s",
        maxPoints: Int? = 360
    ) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    periodStartIso = null,
                    periodStopIso = null
                )
            }

            try {
                val history = repo.history(id, historyRange, historyEvery)
                val latest = repo.latest(id)

                val latestPoint =
                    if (latest.ts != null && latest.temperatura != null && latest.umidade != null) {
                        TemperaturePointDto(
                            ts = latest.ts,
                            temperatura = latest.temperatura,
                            umidade = latest.umidade
                        )
                    } else {
                        null
                    }

                val prepared = appendPointIfNew(history, latestPoint)
                    .let { applyLimit(it, maxPoints) }

                _state.update {
                    it.copy(
                        isLoading = false,
                        latestTemp = latest.temperatura,
                        latestHum = latest.umidade,
                        latestTs = latest.ts,
                        chartPoints = prepared,
                        error = null,
                        periodStartIso = null,
                        periodStopIso = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erro desconhecido"
                    )
                }
            }
        }
    }

    fun startSensoresRealtime(
        sensores: List<UserSensor>,
        pollMs: Long = 5_000L
    ) {
        stopSensoresRealtime()

        sensoresRealtimeJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val listaAtualizada = sensores.map { sensor ->
                        val latest = try {
                            repo.latest(sensor.sensorId)
                        } catch (_: Exception) {
                            null
                        }

                        SensorListItemUi(
                            sensorId = sensor.sensorId,
                            nome = sensor.displayName(),
                            temperaturaAtual = latest?.temperatura,
                            umidadeAtual = latest?.umidade,
                            tempLimitMax = null,
                            tempLimitMin = null
                        )
                    }

                    _sensoresRealtime.value = listaAtualizada
                } catch (_: Exception) {
                    _sensoresRealtime.value = sensores.map { sensor ->
                        SensorListItemUi(
                            sensorId = sensor.sensorId,
                            nome = sensor.displayName(),
                            temperaturaAtual = null,
                            umidadeAtual = null,
                            tempLimitMax = null,
                            tempLimitMin = null
                        )
                    }
                }

                delay(pollMs)
            }
        }
    }

    fun stopSensoresRealtime() {
        sensoresRealtimeJob?.cancel()
        sensoresRealtimeJob = null
    }

    fun stopStreaming() {
        loopJob?.cancel()
        loopJob = null
    }

    fun loadHistoryByPeriod(
        id: String,
        startIso: String,
        stopIso: String,
        every: String? = null,
        maxPoints: Int? = null
    ) {
        stopStreaming()

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    periodStartIso = startIso,
                    periodStopIso = stopIso
                )
            }

            try {
                val resolvedEvery = every ?: suggestEveryForPeriod(startIso, stopIso)
                val history = repo.historyByPeriod(id, startIso, stopIso, resolvedEvery)
                val preparedHistory = applyLimit(history, maxPoints)
                val last = preparedHistory.lastOrNull()

                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        chartPoints = preparedHistory,
                        latestTemp = last?.temperatura ?: currentState.latestTemp,
                        latestHum = last?.umidade ?: currentState.latestHum,
                        latestTs = last?.ts ?: currentState.latestTs,
                        error = null,
                        periodStartIso = startIso,
                        periodStopIso = stopIso
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erro desconhecido",
                        periodStartIso = startIso,
                        periodStopIso = stopIso
                    )
                }
            }
        }
    }

    fun setSensores(
        sensores: List<UserSensor>,
        sensorSelecionado: UserSensor? = sensores.firstOrNull()
    ) {
        _state.update {
            it.copy(
                sensores = sensores,
                sensorSelecionado = sensorSelecionado
            )
        }

        if (sensores.isNotEmpty()) {
            startSensoresRealtime(sensores)
        } else {
            stopSensoresRealtime()
            _sensoresRealtime.value = emptyList()
        }
    }

    fun selecionarSensor(sensor: UserSensor) {
        _state.update {
            it.copy(sensorSelecionado = sensor)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
        stopSensoresRealtime()
    }

    private fun appendPointIfNew(
        currentPoints: List<TemperaturePointDto>,
        newPoint: TemperaturePointDto?
    ): List<TemperaturePointDto> {
        if (newPoint == null) return currentPoints

        val alreadyExists = currentPoints.any { it.ts == newPoint.ts }
        if (alreadyExists) return currentPoints

        return currentPoints + newPoint
    }

    private fun applyLimit(
        points: List<TemperaturePointDto>,
        maxPoints: Int?
    ): List<TemperaturePointDto> {
        if (maxPoints == null || maxPoints <= 0) return points
        return if (points.size > maxPoints) points.takeLast(maxPoints) else points
    }

    private fun suggestEveryForPeriod(startIso: String, stopIso: String): String {
        return try {
            val start = OffsetDateTime.parse(startIso)
            val stop = OffsetDateTime.parse(stopIso)
            val minutes = Duration.between(start, stop).toMinutes().coerceAtLeast(1)

            when {
                minutes <= 120 -> "10s"
                minutes <= 360 -> "30s"
                minutes <= 720 -> "1m"
                minutes <= 1440 -> "5m"
                minutes <= 10080 -> "15m"
                else -> "1h"
            }
        } catch (_: Exception) {
            "1m"
        }
    }
}