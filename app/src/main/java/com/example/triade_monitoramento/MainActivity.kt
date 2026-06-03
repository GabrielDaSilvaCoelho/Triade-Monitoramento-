package com.example.triade_monitoramento

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.triade_monitoramento.data.SessionManager
import com.example.triade_monitoramento.data.model.UserSensor
import com.example.triade_monitoramento.data.remote.NetworkModule
import com.example.triade_monitoramento.data.remote.SupabaseClientProvider
import com.example.triade_monitoramento.data.repository.PortaEventosResumo
import com.example.triade_monitoramento.data.repository.PortaRepository
import com.example.triade_monitoramento.data.repository.SensorConfigData
import com.example.triade_monitoramento.data.repository.SensorRepository
import com.example.triade_monitoramento.data.repository.TemperatureRepository
import com.example.triade_monitoramento.data.repository.UsuarioRepository
import com.example.triade_monitoramento.ui.cadastro.CadastroScreen
import com.example.triade_monitoramento.ui.login.LoginScreen
import com.example.triade_monitoramento.ui.login.RecuperarSenhaScreen
import com.example.triade_monitoramento.ui.perfil.PerfilScreen
import com.example.triade_monitoramento.ui.perfil.UsuarioPerfil
import com.example.triade_monitoramento.ui.sensor.CadastroSensorScreen
import com.example.triade_monitoramento.ui.sensor.ConfiguracaoAlarmeDeTemperaturaScreen
import com.example.triade_monitoramento.ui.sensor.PortasAbertasScreen
import com.example.triade_monitoramento.ui.sensor.SensorConfigScreen
import com.example.triade_monitoramento.ui.sensor.SensorContatosScreen
import com.example.triade_monitoramento.ui.sensor.SensorListItemUi
import com.example.triade_monitoramento.ui.sensor.SensoresScreen
import com.example.triade_monitoramento.ui.temperature.TemperatureScreen
import com.example.triade_monitoramento.ui.temperature.TemperatureViewModel
import com.example.triade_monitoramento.ui.temperature.TemperatureVmFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private enum class Tela {
        LOGIN,
        CADASTRO,
        TEMPERATURE,
        CADASTRO_SENSOR,
        SENSORES,
        CONFIG_SENSOR,
        CONFIG_CONTATOS_SENSOR,
        CONFIG_ALARME_TEMPERATURA,
        PORTAS_ABERTAS,
        PERFIL,
        RECUPERAR_SENHA
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
                    val portaRepository = remember {
                        PortaRepository(NetworkModule.temperatureApi)
                    }

                    var portasResumo by remember {
                        mutableStateOf(
                            PortaEventosResumo(
                                sensorId = "",
                                sensorNome = "",
                                yellowAfterMinutes = 1,
                                redAfterMinutes = 5,
                                amarelos = 0,
                                vermelhos = 0,
                                eventos = emptyList()
                            )
                        )
                    }

                    var carregandoPortas by remember { mutableStateOf(false) }
                    var erroPortas by remember { mutableStateOf<String?>(null) }

                    var mostrarDialogConfigPorta by remember { mutableStateOf(false) }
                    var yellowConfigText by remember { mutableStateOf("1") }
                    var redConfigText by remember { mutableStateOf("5") }

                    val temperatureRepo = remember {
                        TemperatureRepository(NetworkModule.temperatureApi)
                    }

                    val factory = remember {
                        TemperatureVmFactory(temperatureRepo)
                    }

                    val vm: TemperatureViewModel = viewModel(factory = factory)

                    val state by vm.state.collectAsState()
                    val sensoresRealtime by vm.sensoresRealtime.collectAsState()

                    val usuarioRepository = remember { UsuarioRepository() }

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

                    var filtroStartIso by rememberSaveable {
                        mutableStateOf<String?>(null)
                    }

                    var filtroStopIso by rememberSaveable {
                        mutableStateOf<String?>(null)
                    }

                    val sensoresUserSensor = sensoresDaConta.map {
                        UserSensor(
                            sensorId = it.sensorId,
                            nome = it.nome ?: it.sensorId
                        )
                    }

                    LaunchedEffect(Unit) {
                        val userIdSalvo = SessionManager.getLoggedUserId(this@MainActivity)

                        Log.d("SESSION_DEBUG", "UserId salvo: $userIdSalvo")

                        if (userIdSalvo != null) {
                            Session.userId = userIdSalvo

                            val usuario = usuarioRepository.buscarUsuarioLogado()

                            if (usuario != null) {
                                usuarioLogado = UsuarioPerfil(
                                    nome = usuario.nome ?: "Sem nome",
                                    email = usuario.email ?: "Sem email",
                                    telefone = usuario.telefone ?: "Não informado"
                                )

                                val repository = SensorRepository(SupabaseClientProvider.client)
                                val sensores = repository.buscarSensoresDoUsuario()

                                sensoresDaConta = sensores

                                sensorSelecionado = sensores.firstOrNull()?.let {
                                    UserSensor(
                                        sensorId = it.sensorId,
                                        nome = it.nome ?: it.sensorId
                                    )
                                }

                                telaAtual = Tela.TEMPERATURE
                            } else {
                                SessionManager.logout(this@MainActivity)
                                Session.userId = null
                                telaAtual = Tela.LOGIN
                            }
                        }
                    }

                    BackHandler {
                        if (mostrarDialogConfigPorta) {
                            mostrarDialogConfigPorta = false
                            return@BackHandler
                        }

                        when (telaAtual) {
                            Tela.LOGIN, Tela.TEMPERATURE -> {
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

                            Tela.CADASTRO -> telaAtual = Tela.LOGIN
                            Tela.RECUPERAR_SENHA -> telaAtual = Tela.LOGIN
                            Tela.PERFIL -> telaAtual = Tela.TEMPERATURE
                            Tela.SENSORES -> telaAtual = Tela.TEMPERATURE
                            Tela.CONFIG_SENSOR -> telaAtual = Tela.SENSORES
                            Tela.CONFIG_CONTATOS_SENSOR -> telaAtual = Tela.CONFIG_SENSOR
                            Tela.CONFIG_ALARME_TEMPERATURA -> telaAtual = Tela.CONFIG_SENSOR
                            Tela.CADASTRO_SENSOR -> telaAtual = Tela.TEMPERATURE
                            Tela.PORTAS_ABERTAS -> telaAtual = Tela.CONFIG_SENSOR
                        }
                    }

                    if (mostrarDialogConfigPorta) {
                        AlertDialog(
                            onDismissRequest = {
                                mostrarDialogConfigPorta = false
                            },
                            title = {
                                Text("Configurar tempos de alerta")
                            },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = yellowConfigText,
                                        onValueChange = {
                                            yellowConfigText = it
                                        },
                                        label = {
                                            Text("Tempo amarelo (min)")
                                        },
                                        singleLine = true
                                    )

                                    Spacer(
                                        modifier = Modifier.height(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = redConfigText,
                                        onValueChange = {
                                            redConfigText = it
                                        },
                                        label = {
                                            Text("Tempo vermelho (min)")
                                        },
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val yellow = yellowConfigText.toIntOrNull()
                                        val red = redConfigText.toIntOrNull()

                                        if (
                                            yellow == null ||
                                            red == null ||
                                            yellow <= 0 ||
                                            red <= 0 ||
                                            yellow >= red
                                        ) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "O tempo amarelo deve ser menor que o vermelho",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            return@TextButton
                                        }

                                        val sensorId = sensorSelecionadoConfig?.sensorId

                                        if (sensorId == null) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Sensor não selecionado",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            return@TextButton
                                        }

                                        lifecycleScope.launch {
                                            try {
                                                portaRepository.salvarConfigPorta(
                                                    sensorId = sensorId,
                                                    yellowAfterMinutes = yellow,
                                                    redAfterMinutes = red
                                                )

                                                portasResumo = portaRepository.buscarEventosPorta(
                                                    sensorId = sensorId,
                                                    yellow = yellow,
                                                    red = red
                                                )

                                                yellowConfigText = portasResumo.yellowAfterMinutes.toString()
                                                redConfigText = portasResumo.redAfterMinutes.toString()

                                                mostrarDialogConfigPorta = false

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Configuração salva",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                mostrarDialogConfigPorta = false
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    e.message ?: "Erro ao salvar configuração",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                ) {
                                    Text("Salvar")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        mostrarDialogConfigPorta = false
                                    }
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    LaunchedEffect(sensoresDaConta, sensorSelecionado) {
                        vm.setSensores(
                            sensores = sensoresUserSensor,
                            sensorSelecionado = sensorSelecionado
                        )
                    }

                    LaunchedEffect(
                        sensorSelecionado?.sensorId,
                        filtroStartIso,
                        filtroStopIso
                    ) {
                        val sensorId = sensorSelecionado?.sensorId.orEmpty()

                        if (sensorId.isBlank()) return@LaunchedEffect

                        val start = filtroStartIso
                        val stop = filtroStopIso

                        if (!start.isNullOrBlank() && !stop.isNullOrBlank()) {
                            vm.loadHistoryByPeriod(
                                id = sensorId,
                                startIso = start,
                                stopIso = stop
                            )
                        } else {
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
                                        SessionManager.saveLogin(this@MainActivity, usuario.id)

                                        usuarioLogado = UsuarioPerfil(
                                            nome = usuario.nome ?: "Sem nome",
                                            email = usuario.email ?: "Sem email",
                                            telefone = usuario.telefone ?: "Não informado"
                                        )

                                        val repository =
                                            SensorRepository(SupabaseClientProvider.client)

                                        val sensores = repository.buscarSensoresDoUsuario()

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
                                },
                                onEsqueciSenha = {
                                    telaAtual = Tela.RECUPERAR_SENHA
                                }
                            )
                        }

                        Tela.RECUPERAR_SENHA -> {
                            RecuperarSenhaScreen(
                                onVoltarLogin = {
                                    telaAtual = Tela.LOGIN
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
                                    filtroStartIso = start
                                    filtroStopIso = stop
                                },
                                onBackToRealtime = {
                                    filtroStartIso = null
                                    filtroStopIso = null

                                    sensorSelecionado?.sensorId?.let { id ->
                                        vm.stopStreaming()
                                        vm.startStreaming(
                                            id = id,
                                            historyRange = "1h",
                                            historyEvery = "10s",
                                            pollLatestMs = 5_000L,
                                            maxPoints = 360
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
                                    SessionManager.logout(this@MainActivity)

                                    Session.userId = null
                                    usuarioLogado = null
                                    sensoresDaConta = emptyList()
                                    sensorSelecionado = null
                                    sensorSelecionadoConfig = null
                                    filtroStartIso = null
                                    filtroStopIso = null

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
                                },
                                onSalvarPerfil = { novoPerfil ->
                                    lifecycleScope.launch {
                                        val sucesso = usuarioRepository.atualizarPerfil(
                                            nome = novoPerfil.nome,
                                            email = novoPerfil.email,
                                            telefone = novoPerfil.telefone
                                        )

                                        if (sucesso) {
                                            usuarioLogado = novoPerfil

                                            Toast.makeText(
                                                this@MainActivity,
                                                "Perfil atualizado com sucesso",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Erro ao atualizar perfil",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
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

                                        val sensores = repository.buscarSensoresDoUsuario()

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
                                    onGerenciarContatos = {
                                        telaAtual = Tela.CONFIG_CONTATOS_SENSOR
                                    },
                                    onRelatorioPortas = {
                                        val sensorId = sensorSelecionadoConfig?.sensorId

                                        if (sensorId != null) {
                                            telaAtual = Tela.PORTAS_ABERTAS

                                            lifecycleScope.launch {
                                                carregandoPortas = true
                                                erroPortas = null

                                                try {
                                                    val config =
                                                        portaRepository.buscarConfigPorta(sensorId)

                                                    portasResumo =
                                                        portaRepository.buscarEventosPorta(
                                                            sensorId = sensorId,
                                                            yellow = config.yellowAfterMinutes,
                                                            red = config.redAfterMinutes
                                                        )

                                                    yellowConfigText =
                                                        portasResumo.yellowAfterMinutes.toString()

                                                    redConfigText =
                                                        portasResumo.redAfterMinutes.toString()
                                                } catch (e: Exception) {
                                                    erroPortas =
                                                        e.message ?: "Erro ao carregar eventos de porta"

                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        erroPortas,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } finally {
                                                    carregandoPortas = false
                                                }
                                            }
                                        }
                                    },
                                    onGerenciarAlarmes = {
                                        telaAtual = Tela.CONFIG_ALARME_TEMPERATURA
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

                        Tela.CONFIG_ALARME_TEMPERATURA -> {
                            val sensor = sensorSelecionadoConfig

                            if (sensor != null) {
                                ConfiguracaoAlarmeDeTemperaturaScreen(
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

                                            telaAtual = Tela.CONFIG_SENSOR
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

                        Tela.CONFIG_CONTATOS_SENSOR -> {
                            val sensor = sensorSelecionadoConfig

                            if (sensor != null) {
                                SensorContatosScreen(
                                    sensorId = sensor.sensorId,
                                    sensorNome = sensor.nome,
                                    onBack = {
                                        telaAtual = Tela.CONFIG_SENSOR
                                    }
                                )
                            } else {
                                telaAtual = Tela.SENSORES
                            }
                        }

                        Tela.PORTAS_ABERTAS -> {
                            PortasAbertasScreen(
                                sensorNome = portasResumo.sensorNome,
                                amarelos = portasResumo.amarelos,
                                vermelhos = portasResumo.vermelhos,
                                yellowAfterMinutes = portasResumo.yellowAfterMinutes,
                                redAfterMinutes = portasResumo.redAfterMinutes,
                                portasAbertas = portasResumo.eventos,
                                onConfigurarTempos = {
                                    yellowConfigText = portasResumo.yellowAfterMinutes.toString()
                                    redConfigText = portasResumo.redAfterMinutes.toString()
                                    mostrarDialogConfigPorta = true
                                },
                                onRefresh = {
                                    sensorSelecionadoConfig?.sensorId?.let { sensorId ->
                                        lifecycleScope.launch {
                                            val config = portaRepository.buscarConfigPorta(sensorId)

                                            portasResumo =
                                                portaRepository.buscarEventosPorta(
                                                    sensorId = sensorId,
                                                    yellow = config.yellowAfterMinutes,
                                                    red = config.redAfterMinutes
                                                )

                                            yellowConfigText = portasResumo.yellowAfterMinutes.toString()
                                            redConfigText = portasResumo.redAfterMinutes.toString()
                                        }
                                    }
                                },
                                onBack = {
                                    telaAtual = Tela.CONFIG_SENSOR
                                }
                            )
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