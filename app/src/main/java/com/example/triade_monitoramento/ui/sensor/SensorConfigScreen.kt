package com.example.triade_monitoramento.ui.sensor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.data.model.SensorNotificacaoDTO
import com.example.triade_monitoramento.data.remote.SupabaseClientProvider
import com.example.triade_monitoramento.data.repository.SensorRepository
import com.example.triade_monitoramento.ui.navigation.ScreenContainer
import kotlinx.coroutines.launch

private val TriadeGreen = Color(0xFF769F86)
private val TriadeRed = Color(0xFFC75C5C)
private val TriadeRedLight = Color(0xFFFFF3F3)
private val SuccessGreen = Color(0xFF2E7D32)
private val CardLight = Color(0xFFF7F9F8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorConfigScreen(
    sensorId: String,
    nomeInicial: String,
    tempMaxInicial: Double?,
    tempMinInicial: Double?,
    onBack: () -> Unit,
    onSensorExcluido: () -> Unit
) {
    var nomeSensor by remember { mutableStateOf(nomeInicial) }
    var temperaturaMaxima by remember {
        mutableStateOf(tempMaxInicial?.toString()?.replace(".", ",") ?: "")
    }
    var temperaturaMinima by remember {
        mutableStateOf(tempMinInicial?.toString()?.replace(".", ",") ?: "")
    }

    var mensagem by remember { mutableStateOf<String?>(null) }
    var loadingSalvar by remember { mutableStateOf(false) }
    var loadingExcluir by remember { mutableStateOf(false) }
    var loadingContatos by remember { mutableStateOf(false) }

    var whatsappNovo by remember { mutableStateOf("") }
    var emailNovo by remember { mutableStateOf("") }

    var notificacoes by remember {
        mutableStateOf<List<SensorNotificacaoDTO>>(emptyList())
    }

    val repository = remember {
        SensorRepository(SupabaseClientProvider.client)
    }

    val scope = rememberCoroutineScope()

    fun recarregarNotificacoes() {
        scope.launch {
            loadingContatos = true
            notificacoes = repository.listarNotificacoes(sensorId)
            loadingContatos = false
        }
    }

    LaunchedEffect(sensorId) {
        loadingContatos = true

        val sensor = repository.buscarSensorPorId(sensorId)

        if (sensor != null) {
            nomeSensor = sensor.nome ?: ""
            temperaturaMaxima = sensor.tempLimitMax?.toString()?.replace(".", ",") ?: ""
            temperaturaMinima = sensor.tempLimitMin?.toString()?.replace(".", ",") ?: ""
        } else {
            mensagem = "Sensor não encontrado."
        }

        notificacoes = repository.listarNotificacoes(sensorId)
        loadingContatos = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Sensor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TriadeGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        ScreenContainer {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Edite os dados do sensor, limites e contatos que receberão alertas.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = sensorId,
                    onValueChange = { },
                    label = { Text("ID do Sensor") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    colors = campoColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nomeSensor,
                    onValueChange = { nomeSensor = it },
                    label = { Text("Nome do Sensor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = campoColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = temperaturaMaxima,
                    onValueChange = { temperaturaMaxima = it },
                    label = { Text("Temperatura Máxima") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = campoColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = temperaturaMinima,
                    onValueChange = { temperaturaMinima = it },
                    label = { Text("Temperatura Mínima") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = campoColors()
                )

                Spacer(modifier = Modifier.height(24.dp))

                SecaoNotificacoes(
                    titulo = "WhatsApps de alerta",
                    subtitulo = "Adicione os números que receberão mensagens de alerta.",
                    tipo = "whatsapp",
                    novoValor = whatsappNovo,
                    labelNovoValor = "Novo WhatsApp",
                    keyboardType = KeyboardType.Phone,
                    notificacoes = notificacoes,
                    loading = loadingContatos,
                    onNovoValorChange = { whatsappNovo = it },
                    onAdicionar = {
                        scope.launch {
                            mensagem = null

                            val numero = whatsappNovo.trim()

                            if (numero.isBlank()) {
                                mensagem = "Informe um número de WhatsApp."
                                return@launch
                            }

                            val sucesso = repository.adicionarNotificacao(
                                sensorId = sensorId,
                                tipo = "whatsapp",
                                destino = numero
                            )

                            if (sucesso) {
                                whatsappNovo = ""
                                mensagem = "WhatsApp adicionado com sucesso."
                                recarregarNotificacoes()
                            } else {
                                mensagem = "Erro ao adicionar WhatsApp."
                            }
                        }
                    },
                    onExcluir = { contato ->
                        scope.launch {
                            mensagem = null

                            val id = contato.id
                            if (id == null) {
                                mensagem = "Contato inválido para exclusão."
                                return@launch
                            }

                            val sucesso = repository.excluirNotificacao(id)

                            if (sucesso) {
                                mensagem = "WhatsApp removido com sucesso."
                                recarregarNotificacoes()
                            } else {
                                mensagem = "Erro ao remover WhatsApp."
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                SecaoNotificacoes(
                    titulo = "Emails de alerta",
                    subtitulo = "Adicione os emails que receberão mensagens de alerta.",
                    tipo = "email",
                    novoValor = emailNovo,
                    labelNovoValor = "Novo email",
                    keyboardType = KeyboardType.Email,
                    notificacoes = notificacoes,
                    loading = loadingContatos,
                    onNovoValorChange = { emailNovo = it },
                    onAdicionar = {
                        scope.launch {
                            mensagem = null

                            val email = emailNovo.trim()

                            if (email.isBlank()) {
                                mensagem = "Informe um email."
                                return@launch
                            }

                            if (!email.contains("@") || !email.contains(".")) {
                                mensagem = "Informe um email válido."
                                return@launch
                            }

                            val sucesso = repository.adicionarNotificacao(
                                sensorId = sensorId,
                                tipo = "email",
                                destino = email
                            )

                            if (sucesso) {
                                emailNovo = ""
                                mensagem = "Email adicionado com sucesso."
                                recarregarNotificacoes()
                            } else {
                                mensagem = "Erro ao adicionar email."
                            }
                        }
                    },
                    onExcluir = { contato ->
                        scope.launch {
                            mensagem = null

                            val id = contato.id
                            if (id == null) {
                                mensagem = "Contato inválido para exclusão."
                                return@launch
                            }

                            val sucesso = repository.excluirNotificacao(id)

                            if (sucesso) {
                                mensagem = "Email removido com sucesso."
                                recarregarNotificacoes()
                            } else {
                                mensagem = "Erro ao remover email."
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            mensagem = null

                            if (
                                nomeSensor.isBlank() ||
                                temperaturaMaxima.isBlank() ||
                                temperaturaMinima.isBlank()
                            ) {
                                mensagem = "Preencha todos os campos."
                                return@launch
                            }

                            val tempMax = temperaturaMaxima.replace(",", ".").toDoubleOrNull()
                            val tempMin = temperaturaMinima.replace(",", ".").toDoubleOrNull()

                            if (tempMax == null || tempMin == null) {
                                mensagem = "Informe valores numéricos válidos."
                                return@launch
                            }

                            if (tempMin >= tempMax) {
                                mensagem = "A temperatura mínima deve ser menor que a máxima."
                                return@launch
                            }

                            loadingSalvar = true

                            val sucesso = repository.atualizarConfiguracaoSensor(
                                sensorId = sensorId,
                                nome = nomeSensor.trim(),
                                tempLimitMax = tempMax,
                                tempLimitMin = tempMin
                            )

                            loadingSalvar = false

                            if (sucesso) {
                                mensagem = "Configuração salva com sucesso!"
                                onBack()
                            } else {
                                mensagem = "Erro ao salvar configuração. Caso o problema persista, contate a equipe técnica."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !loadingSalvar && !loadingExcluir,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TriadeGreen,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    if (loadingSalvar) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Salvar alterações")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            mensagem = null
                            loadingExcluir = true

                            val sucesso = repository.excluirSensorDaConta(sensorId)

                            loadingExcluir = false

                            if (sucesso) {
                                onSensorExcluido()
                            } else {
                                mensagem = "Erro ao excluir sensor. Caso o problema persista, contate a equipe técnica."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !loadingSalvar && !loadingExcluir,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TriadeRed
                    )
                ) {
                    if (loadingExcluir) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = TriadeRed
                        )
                    } else {
                        Text("Excluir sensor da conta")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                mensagem?.let {
                    val cor = when {
                        it.contains("sucesso", ignoreCase = true) ||
                                it.contains("adicionado", ignoreCase = true) ||
                                it.contains("removido", ignoreCase = true) -> SuccessGreen

                        else -> TriadeRed
                    }

                    Text(
                        text = it,
                        color = cor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SecaoNotificacoes(
    titulo: String,
    subtitulo: String,
    tipo: String,
    novoValor: String,
    labelNovoValor: String,
    keyboardType: KeyboardType,
    notificacoes: List<SensorNotificacaoDTO>,
    loading: Boolean,
    onNovoValorChange: (String) -> Unit,
    onAdicionar: () -> Unit,
    onExcluir: (SensorNotificacaoDTO) -> Unit
) {
    val contatos = notificacoes.filter { it.tipo == tipo }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CardLight,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitulo,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    strokeWidth = 2.dp,
                    color = TriadeGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (contatos.isEmpty() && !loading) {
            Text(
                text = "Nenhum contato cadastrado.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        contatos.forEach { contato ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contato.destino,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )

                IconButton(
                    onClick = {
                        onExcluir(contato)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir contato",
                        tint = TriadeRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = novoValor,
            onValueChange = onNovoValorChange,
            label = { Text(labelNovoValor) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = campoColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onAdicionar,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TriadeGreen,
                contentColor = Color.White
            )
        ) {
            Text("Adicionar")
        }
    }
}

@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TriadeGreen,
    focusedLabelColor = TriadeGreen,
    cursorColor = TriadeGreen
)