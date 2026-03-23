package com.example.triade_monitoramento

import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.example.triade_monitoramento.data.repository.SensorRepository
import com.example.triade_monitoramento.ui.sensor.CadastroSensorScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private enum class Tela {
        LOGIN,
        CADASTRO,
        TEMPERATURE,
        CADASTRO_SENSOR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = 0xFF769F86.toInt()

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    var telaAtual by rememberSaveable {
                        mutableStateOf(Tela.LOGIN)
                    }

                    // ID do usuário autenticado
                    var usuarioLogadoId by rememberSaveable {
                        mutableStateOf("")
                    }


                    /*
                     * Lista de sensores da conta.
                     * Aqui está mockada para compilar e funcionar.
                     * Depois você troca por carregamento real do Supabase.
                     */
                    var sensoresDaConta by remember {
                        mutableStateOf<List<UserSensor>>(emptyList())
                    }


                    // Sensor atualmente selecionado
                    var sensorSelecionado by remember {
                        mutableStateOf<UserSensor?>(sensoresDaConta.firstOrNull())
                    }

                    when (telaAtual) {

                        Tela.LOGIN -> {
                            LoginScreen(
                                onLogado = { usuario ->

                                    lifecycleScope.launch {

                                        Session.userId = usuario.id

                                        val repository = SensorRepository(SupabaseClientProvider.client)

                                        val sensores = repository.buscarSensoresDoUsuario()


                                        sensoresDaConta = sensores
                                        sensorSelecionado = sensores.firstOrNull()

                                        Log.d("DEBUG_USER", "Usuario logado ID: ${usuario.id}")
                                        Log.d("DEBUG_SESSION", "Session ID: ${Session.userId}")




                                        telaAtual = Tela.TEMPERATURE
                                    }
                                }
                                ,
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

                        Tela.CADASTRO_SENSOR -> {
                            CadastroSensorScreen()
                        }
                    }
                }
            }
        }
    }
}