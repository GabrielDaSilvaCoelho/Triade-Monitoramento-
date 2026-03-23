package com.example.triade_monitoramento.ui.sensor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triade_monitoramento.data.repository.SensorRepository
import com.example.triade_monitoramento.Session
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CadastroSensorViewModel(
    private val repository: SensorRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(CadastroSensorUiState())
    val uiState: StateFlow<CadastroSensorUiState> = _uiState.asStateFlow()

    fun onSensorIdChange(value: String) {
        _uiState.update { it.copy(sensorId = value, erro = null) }
    }

    fun onNomeChange(value: String) {
        _uiState.update { it.copy(nome = value, erro = null) }
    }

    fun vincularSensor() {
        val sensorId = _uiState.value.sensorId.trim()
        val nome = _uiState.value.nome.trim()

        if (sensorId.isBlank()) {
            _uiState.update { it.copy(erro = "Informe o ID do sensor") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, erro = null, sucesso = false) }

            try {
                val usuarioId = Session.userId


                if (usuarioId == null) {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            erro = "Usuário não está logado"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        loading = false,
                        sucesso = true,
                        sensorId = "",
                        nome = ""
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        erro = e.message ?: "Erro ao vincular sensor"
                    )
                }
            }
        }
    }

    fun limparSucesso() {
        _uiState.update { it.copy(sucesso = false) }
    }
}