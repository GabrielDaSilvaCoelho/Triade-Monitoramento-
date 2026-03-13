package com.example.triade_monitoramento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {

    private enum class Tela {
        LOGIN,
        CADASTRO,
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

                    // ID do usuário autenticado
                    var usuarioLogadoId by rememberSaveable {
                        mutableStateOf("TRD1001")

                    }

                    /*
                     * Lista de sensores da conta.
                     * Aqui está mockada para compilar e funcionar.
                     * Depois você troca por carregamento real do Supabase.
                     */
                    var sensoresDaConta by remember {
                        mutableStateOf(
                            listOf(
                                UserSensor(sensorId = "TRD1001", name = "Sensor Principal"),
                                UserSensor(sensorId = "TRD1002", name = "Sensor Reserva")
                            )
                        )
                    }

                    // Sensor atualmente selecionado
                    var sensorSelecionado by remember {
                        mutableStateOf<UserSensor?>(sensoresDaConta.firstOrNull())
                    }

                    when (telaAtual) {

                        Tela.LOGIN -> {
                            LoginScreen(
                                onLogado = { usuario ->
                                    usuarioLogadoId = usuario.id.orEmpty()

                                    /*
                                     * Aqui, futuramente, você deve carregar os sensores
                                     * vinculados a esse usuário no Supabase.
                                     *
                                     * Exemplo:
                                     * sensoresDaConta = buscarSensoresDoUsuario(usuarioLogadoId)
                                     * sensorSelecionado = sensoresDaConta.firstOrNull()
                                     */

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

                        Tela.TEMPERATURE -> {
                            val repo = TemperatureRepository(NetworkModule.temperatureApi)
                            val factory = TemperatureVmFactory(repo)
                            val vm: TemperatureViewModel = viewModel(factory = factory)
                            val state = vm.state.collectAsState().value

                            /*
                             * Sempre que o sensor selecionado mudar,
                             * inicia o streaming dele.
                             */
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
                                    /*
                                     * Quando você criar a tela de vínculo de sensor,
                                     * troque aqui para navegar até ela.
                                     *
                                     * Exemplo:
                                     * telaAtual = Tela.CADASTRO_SENSOR
                                     */
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