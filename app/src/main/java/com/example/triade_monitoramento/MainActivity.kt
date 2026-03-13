package com.example.triade_monitoramento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel


class MainActivity : ComponentActivity() {

    private enum class Tela {
        LOGIN,
        CADASTRO,
        CADASTRO_SENSOR,
        TEMPERATURE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = 0xFF769F86.toInt()

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    var telaAtual by rememberSaveable {
                        mutableStateOf(Tela.TEMPERATURE)
                    }

                    var usuarioLogadoId by rememberSaveable {
                        mutableStateOf("TRD1001")
                    }

                    var sensoresDaConta by remember {
                        mutableStateOf(
                            listOf(
                                UserSensor(sensorId = "TRD1001", name = "Sensor Principal"),
                                UserSensor(sensorId = "TRD1002", name = "Sensor Reserva")
                            )
                        )
                    }

                    var sensorSelecionado by remember {
                        mutableStateOf<UserSensor?>(sensoresDaConta.firstOrNull())
                    }

                    when (telaAtual) {

                        Tela.LOGIN -> {
                            LoginScreen(
                                onLogado = { usuario ->
                                    usuarioLogadoId = usuario.id.orEmpty()
                                    sensorSelecionado = sensoresDaConta.firstOrNull()
                                    telaAtual = Tela.TEMPERATURE
                                },
                                onIrParaCadastro = {
                                    telaAtual = Tela.CADASTRO
                                }
                            )
                        }

                        Tela.CADASTRO -> {
                            CadastroScreen(
                                onCadastrado = {
                                    telaAtual = Tela.LOGIN
                                },
                                onIrParaLogin = {
                                    telaAtual = Tela.LOGIN
                                }
                            )
                        }

                        Tela.CADASTRO_SENSOR -> {
                            CadastroSensorScreen(
                                onSalvar = { novoSensor ->

                                    // Exemplo: adiciona o sensor novo na lista local
                                    val sensorCriado = UserSensor(
                                        sensorId = novoSensor.sensorId,
                                        name = novoSensor.nomeSensor
                                    )

                                    sensoresDaConta = sensoresDaConta + sensorCriado
                                    sensorSelecionado = sensorCriado

                                    // Depois você pode salvar no Supabase aqui
                                    telaAtual = Tela.TEMPERATURE
                                },
                                onVoltar = {
                                    telaAtual = Tela.TEMPERATURE
                                }
                            )
                        }

                        Tela.TEMPERATURE -> {
                            val repo = TemperatureRepository(NetworkModule.temperatureApi)
                            val factory = TemperatureVmFactory(repo)
                            val vm: TemperatureViewModel = viewModel(factory = factory)
                            val state = vm.state.collectAsState().value

                            LaunchedEffect(sensorSelecionado?.sensorId) {
                                val sensorId = sensorSelecionado?.sensorId.orEmpty()

                                if (sensorId.isNotBlank()) {
                                    vm.startStreaming(
                                        id = sensorId,
                                        historyRange = "1h",
                                        historyEvery = "10s",
                                        pollLatestMs = 5_000L,
                                        maxPoints = 300
                                    )
                                }
                            }

                            TemperatureScreen(
                                state = state,
                                currentSensor = sensorSelecionado,
                                availableSensors = sensoresDaConta,
                                onSelectSensor = { sensor ->
                                    sensorSelecionado = sensor
                                },
                                onGoToSensorRegister = {
                                    telaAtual = Tela.CADASTRO_SENSOR
                                },
                                onApplyPeriod = { startIso, stopIso ->
                                    val sensorId = sensorSelecionado?.sensorId.orEmpty()

                                    if (sensorId.isNotBlank()) {
                                        vm.loadHistoryByPeriod(
                                            id = sensorId,
                                            startIso = startIso,
                                            stopIso = stopIso,
                                            every = "10s",
                                            maxPoints = 300
                                        )
                                    }
                                },
                                onBackToRealtime = {
                                    val sensorId = sensorSelecionado?.sensorId.orEmpty()

                                    if (sensorId.isNotBlank()) {
                                        vm.startStreaming(
                                            id = sensorId,
                                            historyRange = "1h",
                                            historyEvery = "10s",
                                            pollLatestMs = 5_000L,
                                            maxPoints = 300
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}