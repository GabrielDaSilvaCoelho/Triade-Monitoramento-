package com.example.triade_monitoramento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    private var loopJob: Job? = null

    fun startStreaming(
        id: String,
        historyRange: String = "1h",
        historyEvery: String = "10s",
        pollLatestMs: Long = 5_000L,
        maxPoints: Int = 300
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
                val trimmed = if (history.size > maxPoints) {
                    history.takeLast(maxPoints)
                } else {
                    history
                }

                val latest = repo.latest(id)

                _state.update {
                    it.copy(
                        isLoading = false,
                        latestTemp = latest.temperatura,
                        latestHum = latest.umidade,
                        latestTs = latest.ts,
                        chartPoints = trimmed,
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
                        val currentPoints = currentState.chartPoints

                        val appended =
                            if (newPoint == null) {
                                currentPoints
                            } else if (
                                currentPoints.isNotEmpty() &&
                                currentPoints.last().ts == newPoint.ts
                            ) {
                                currentPoints
                            } else {
                                currentPoints + newPoint
                            }

                        val limited =
                            if (appended.size > maxPoints) {
                                appended.takeLast(maxPoints)
                            } else {
                                appended
                            }

                        currentState.copy(
                            latestTemp = latest.temperatura ?: currentState.latestTemp,
                            latestHum = latest.umidade ?: currentState.latestHum,
                            latestTs = latest.ts ?: currentState.latestTs,
                            chartPoints = limited,
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

    fun stopStreaming() {
        loopJob?.cancel()
        loopJob = null
    }

    fun loadHistoryByPeriod(
        id: String,
        startIso: String,
        stopIso: String,
        every: String = "10s",
        maxPoints: Int = 300
    ) {
        stopStreaming()

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                val history = repo.historyByPeriod(id, startIso, stopIso, every)
                val trimmed = if (history.size > maxPoints) {
                    history.takeLast(maxPoints)
                } else {
                    history
                }

                val last = trimmed.lastOrNull()

                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        chartPoints = trimmed,
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
                        error = e.message ?: "Erro desconhecido"
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
    }

    fun selecionarSensor(sensor: UserSensor) {
        _state.update {
            it.copy(sensorSelecionado = sensor)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
    }
}