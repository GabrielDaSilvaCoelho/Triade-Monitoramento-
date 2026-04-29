package com.example.triade_monitoramento

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.triade_monitoramento.data.model.UserSensor
import com.example.triade_monitoramento.data.remote.NetworkModule
import com.example.triade_monitoramento.data.remote.SupabaseClientProvider
import com.example.triade_monitoramento.data.repository.SensorConfigData
import com.example.triade_monitoramento.data.repository.SensorRepository
import com.example.triade_monitoramento.data.repository.TemperatureRepository
import com.example.triade_monitoramento.ui.cadastro.CadastroScreen
import com.example.triade_monitoramento.ui.login.LoginScreen
import com.example.triade_monitoramento.ui.perfil.PerfilScreen
import com.example.triade_monitoramento.ui.perfil.UsuarioPerfil
import com.example.triade_monitoramento.ui.sensor.CadastroSensorScreen
import com.example.triade_monitoramento.ui.sensor.SensorConfigScreen
import com.example.triade_monitoramento.ui.sensor.SensorListItemUi
import com.example.triade_monitoramento.ui.sensor.SensoresScreen
import com.example.triade_monitoramento.ui.temperature.TemperatureScreen
import com.example.triade_monitoramento.ui.temperature.TemperatureViewModel
import com.example.triade_monitoramento.ui.temperature.TemperatureVmFactory
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
class MainActivity : ComponentActivity() {

    private enum class Tela {
        LOGIN,
        CADASTRO,
        TEMPERATURE,
        CADASTRO_SENSOR,
        SENSORES,
        CONFIG_SENSOR,
        PERFIL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = 0xFF769F86.toInt()

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = true

        setContent {
            MaterialTheme {
                Surface {
                    val temperatureRepo = remember {
                        TemperatureRepository(NetworkModule.temperatureApi)
                    }

                    val factory = remember {
                        TemperatureVmFactory(temperatureRepo)
                    }

                    val vm: TemperatureViewModel = viewModel(factory = factory)

                    val state by vm.state.collectAsState()
                    val sensoresRealtime by vm.sensoresRealtime.collectAsState()

                    var telaAtual by rememberSaveable {
                        mutableStateOf(Tela.LOGIN)
                    }

                    var ultimoCliqueVoltar by remember {
                        mutableStateOf(0L)
                    }

                    var sensoresDaConta by remember {
                        mutableStateOf<List<SensorConfigData>>(emptyList())
                    }

                    var sensorSelecionado by remember {
                        mutableStateOf<UserSensor?>(null)
                    }

                    var sensorSelecionadoConfig by remember {
                        mutableStateOf<SensorListItemUi?>(null)
                    }

                    var usuarioLogado by remember {
                        mutableStateOf<UsuarioPerfil?>(null)
                    }

                    val sensoresUserSensor = sensoresDaConta.map {
                        UserSensor(
                            sensorId = it.sensorId,
                            nome = it.nome ?: it.sensorId
                        )
                    }

                    BackHandler {
                        when (telaAtual) {
                            Tela.LOGIN -> {
                                val agora = System.currentTimeMillis()

                                if (agora - ultimoCliqueVoltar < 2000) {
                                    finish()
                                } else {
                                    ultimoCliqueVoltar = agora
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Pressione voltar novamente para sair",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            Tela.TEMPERATURE -> {
                                val agora = System.currentTimeMillis()

                                if (agora - ultimoCliqueVoltar < 2000) {
                                    finish()
                                } else {
                                    ultimoCliqueVoltar = agora
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Pressione voltar novamente para sair",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            Tela.CADASTRO -> {
                                telaAtual = Tela.LOGIN
                            }

                            Tela.PERFIL -> {
                                telaAtual = Tela.TEMPERATURE
                            }

                            Tela.SENSORES -> {
                                telaAtual = Tela.TEMPERATURE
                            }

                            Tela.CONFIG_SENSOR -> {
                                telaAtual = Tela.SENSORES
                            }

                            Tela.CADASTRO_SENSOR -> {
                                telaAtual = Tela.TEMPERATURE
                            }
                        }
                    }

                    LaunchedEffect(sensoresDaConta, sensorSelecionado) {
                        vm.setSensores(
                            sensores = sensoresUserSensor,
                            sensorSelecionado = sensorSelecionado
                        )
                    }

                    LaunchedEffect(sensorSelecionado?.sensorId) {
                        val sensorId = sensorSelecionado?.sensorId.orEmpty()

                        if (sensorId.isNotBlank()) {
                            vm.startStreaming(
                                id = sensorId,
                                historyRange = "1h",
                                historyEvery = "10s",
                                pollLatestMs = 5_000L,
                                maxPoints = null
                            )
                        }
                    }

                    when (telaAtual) {
                        Tela.LOGIN -> {
                            LoginScreen(
                                onLogado = { usuario ->
                                    lifecycleScope.launch {
                                        Session.userId = usuario.id

                                        usuarioLogado = UsuarioPerfil(
                                            nome = usuario.nome ?: "Sem nome",
                                            email = usuario.email ?: "Sem email",
                                            telefone = usuario.telefone ?: "Não informado"
                                        )

                                        val repository =
                                            SensorRepository(SupabaseClientProvider.client)

                                        val sensores =
                                            repository.buscarSensoresDoUsuario()

                                        sensoresDaConta = sensores

                                        sensorSelecionado = sensores.firstOrNull()?.let {
                                            UserSensor(
                                                sensorId = it.sensorId,
                                                nome = it.nome ?: it.sensorId
                                            )
                                        }

                                        Log.d("DEBUG_USER", "Usuario logado ID: ${usuario.id}")
                                        Log.d("DEBUG_SESSION", "Session ID: ${Session.userId}")

                                        telaAtual = Tela.TEMPERATURE
                                    }
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
                            TemperatureScreen(
                                state = state,
                                currentSensor = sensorSelecionado,
                                availableSensors = sensoresUserSensor,
                                onSelectSensor = { sensor ->
                                    sensorSelecionado = sensor
                                    vm.selecionarSensor(sensor)
                                },
                                onGoToSensorRegister = {
                                    telaAtual = Tela.CADASTRO_SENSOR
                                },
                                onApplyPeriod = { start, stop ->
                                    val sensorId = sensorSelecionado?.sensorId.orEmpty()

                                    if (sensorId.isNotBlank()) {
                                        vm.loadHistoryByPeriod(
                                            id = sensorId,
                                            startIso = start,
                                            stopIso = stop
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
                                            maxPoints = null
                                        )
                                    }
                                },
                                onGoToPerfil = {
                                    telaAtual = Tela.PERFIL
                                },
                                onGoToSensores = {
                                    telaAtual = Tela.SENSORES
                                },
                                onSair = {
                                    Session.userId = null
                                    usuarioLogado = null
                                    sensoresDaConta = emptyList()
                                    sensorSelecionado = null
                                    sensorSelecionadoConfig = null

                                    vm.stopStreaming()
                                    vm.stopSensoresRealtime()

                                    telaAtual = Tela.LOGIN
                                }
                            )
                        }

                        Tela.PERFIL -> {
                            PerfilScreen(
                                usuario = usuarioLogado ?: UsuarioPerfil(
                                    nome = "Usuário",
                                    email = "email@exemplo.com",
                                    telefone = "Não informado"
                                ),
                                onVoltar = {
                                    telaAtual = Tela.TEMPERATURE
                                }
                            )
                        }

                        Tela.SENSORES -> {
                            val sensoresComLimites = sensoresRealtime.map { sensorRealtime ->
                                val config = sensoresDaConta.find {
                                    it.sensorId == sensorRealtime.sensorId
                                }

                                sensorRealtime.copy(
                                    nome = config?.nome ?: sensorRealtime.nome,
                                    tempLimitMax = config?.tempLimitMax,
                                    tempLimitMin = config?.tempLimitMin,
                                    acknowledged = config?.acknowledged ?: false
                                )
                            }

                            SensoresScreen(
                                sensores = sensoresComLimites,
                                onBack = {
                                    telaAtual = Tela.TEMPERATURE
                                },
                                onAbrirConfiguracao = { sensor ->
                                    sensorSelecionadoConfig = sensor
                                    telaAtual = Tela.CONFIG_SENSOR
                                },
                                onRefresh = {
                                    lifecycleScope.launch {
                                        val repository =
                                            SensorRepository(SupabaseClientProvider.client)

                                        val sensores =
                                            repository.buscarSensoresDoUsuario()

                                        sensoresDaConta = sensores

                                        vm.startSensoresRealtime(
                                            sensores = sensores.map {
                                                UserSensor(
                                                    sensorId = it.sensorId,
                                                    nome = it.nome ?: it.sensorId
                                                )
                                            }
                                        )
                                    }
                                },
                                onReconhecerAlerta = { sensorId ->
                                    lifecycleScope.launch {
                                        val repository =
                                            SensorRepository(SupabaseClientProvider.client)

                                        val sucesso =
                                            repository.marcarAlertaComoCiente(sensorId)

                                        if (sucesso) {
                                            val sensores =
                                                repository.buscarSensoresDoUsuario()

                                            sensoresDaConta = sensores

                                            vm.startSensoresRealtime(
                                                sensores = sensores.map {
                                                    UserSensor(
                                                        sensorId = it.sensorId,
                                                        nome = it.nome ?: it.sensorId
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        Tela.CONFIG_SENSOR -> {
                            val sensor = sensorSelecionadoConfig

                            if (sensor != null) {
                                SensorConfigScreen(
                                    sensorId = sensor.sensorId,
                                    nomeInicial = sensor.nome,
                                    tempMaxInicial = sensor.tempLimitMax,
                                    tempMinInicial = sensor.tempLimitMin,
                                    onBack = {
                                        lifecycleScope.launch {
                                            val repository =
                                                SensorRepository(SupabaseClientProvider.client)

                                            val sensores =
                                                repository.buscarSensoresDoUsuario()

                                            sensoresDaConta = sensores

                                            vm.startSensoresRealtime(
                                                sensores = sensores.map {
                                                    UserSensor(
                                                        sensorId = it.sensorId,
                                                        nome = it.nome ?: it.sensorId
                                                    )
                                                }
                                            )

                                            sensorSelecionadoConfig = null

                                            telaAtual = Tela.SENSORES
                                        }
                                    },
                                    onSensorExcluido = {
                                        lifecycleScope.launch {
                                            val repository =
                                                SensorRepository(SupabaseClientProvider.client)

                                            val sensores =
                                                repository.buscarSensoresDoUsuario()

                                            sensoresDaConta = sensores

                                            sensorSelecionado = sensores.firstOrNull()?.let {
                                                UserSensor(
                                                    sensorId = it.sensorId,
                                                    nome = it.nome ?: it.sensorId
                                                )
                                            }

                                            sensorSelecionadoConfig = null

                                            telaAtual = Tela.SENSORES
                                        }
                                    }
                                )
                            } else {
                                telaAtual = Tela.SENSORES
                            }
                        }

                        Tela.CADASTRO_SENSOR -> {
                            CadastroSensorScreen(
                                onBack = {
                                    lifecycleScope.launch {
                                        val repository =
                                            SensorRepository(SupabaseClientProvider.client)

                                        val sensores =
                                            repository.buscarSensoresDoUsuario()

                                        sensoresDaConta = sensores

                                        sensorSelecionado = sensores.firstOrNull()?.let {
                                            UserSensor(
                                                sensorId = it.sensorId,
                                                nome = it.nome ?: it.sensorId
                                            )
                                        }

                                        telaAtual = Tela.TEMPERATURE
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