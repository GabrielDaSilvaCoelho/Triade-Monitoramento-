package com.example.triade_monitoramento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    private enum class Tela { LOGIN, CADASTRO, TEMPERATURE }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = 0xFF769F86.toInt()

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    var telaAtual by rememberSaveable {
                        mutableStateOf(Tela.LOGIN)
                    }

                    var userId by rememberSaveable {
                        mutableStateOf("")
                    }

                    when (telaAtual) {

                        Tela.LOGIN -> {
                            LoginScreen(
                                onLogado = { usuario ->
                                    userId = usuario.id!!
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

                            LaunchedEffect(userId) {
                                if (userId.isNotBlank()) {
                                    vm.startStreaming(
                                        id = userId,
                                        historyRange = "1h",
                                        historyEvery = "10s",
                                        pollLatestMs = 5_000L,
                                        maxPoints = 300
                                    )
                                }
                            }

                            TemperatureScreen(
                                state = state,
                                onApplyPeriod = { startIso, stopIso ->
                                    if (userId.isNotBlank()) {
                                        vm.loadHistoryByPeriod(
                                            id = userId,
                                            startIso = startIso,
                                            stopIso = stopIso,
                                            every = "10s",
                                            maxPoints = 300
                                        )
                                    }
                                },
                                onBackToRealtime = {
                                    if (userId.isNotBlank()) {
                                        vm.startStreaming(
                                            id = userId,
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