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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.data.model.SensorNotificacaoDTO
import com.example.triade_monitoramento.data.remote.SupabaseClientProvider
import com.example.triade_monitoramento.data.repository.SensorRepository
import com.example.triade_monitoramento.ui.navigation.ScreenContainer
import kotlinx.coroutines.launch

private val TriadeGreen = Color(0xFF769F86)
private val TriadeRed = Color(0xFFC75C5C)
private val CardLight = Color(0xFFF7F9F8)
private val SuccessGreen = Color(0xFF2E7D32)

class WhatsappVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text.take(11) // estado sempre tem no máx 11 dígitos

        // Monta a string exibida: +55 (XX) XXXXX-XXXX
        val formatted = buildString {
            append("+55 ")
            digitos.forEachIndexed { i, c ->
                when (i) {
                    0    -> append("($c")
                    2    -> append(") $c")
                    7    -> append("-$c")
                    else -> append(c)
                }
            }
        }

        // "+55 " tem 4 chars fixos no início
        val prefixo = 4

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val pos = when {
                    offset == 0  -> 0
                    offset == 1  -> 1
                    offset == 2  -> 3
                    offset <= 7  -> offset + 3
                    offset <= 11 -> offset + 4
                    else         -> formatted.length - prefixo
                }
                return (pos + prefixo).coerceIn(prefixo, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val pos = (offset - prefixo).coerceAtLeast(0)
                return when {
                    pos <= 1  -> 0
                    pos <= 3  -> pos - 1
                    pos <= 10 -> pos - 3
                    else      -> pos - 4
                }.coerceIn(0, digitos.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorContatosScreen(
    sensorId: String,
    sensorNome: String,
    onBack: () -> Unit
) {
    val repository = remember {
        SensorRepository(SupabaseClientProvider.client)
    }

    val scope = rememberCoroutineScope()

    var notificacoes by remember {
        mutableStateOf<List<SensorNotificacaoDTO>>(emptyList())
    }

    // Estado guarda APENAS dígitos — sem máscara
    var whatsappNovo by remember { mutableStateOf("") }
    var emailNovo by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf<String?>(null) }

    fun recarregar() {
        scope.launch {
            loading = true
            notificacoes = repository.listarNotificacoes(sensorId)
            loading = false
        }
    }

    LaunchedEffect(sensorId) {
        recarregar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contatos de Alerta") },
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = sensorNome.ifBlank { sensorId },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sensor ID: $sensorId",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Todos os contatos ativos cadastrados aqui receberão alertas quando a temperatura sair dos limites.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = TriadeGreen, strokeWidth = 2.dp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                ContatoGrupoCard(
                    titulo = "WhatsApp",
                    tipo = "whatsapp",
                    iconeTipo = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = TriadeGreen
                        )
                    },
                    contatos = notificacoes.filter { it.tipo == "whatsapp" },
                    novoValor = whatsappNovo,
                    labelNovoValor = "Novo WhatsApp",
                    keyboardType = KeyboardType.Phone,

                    onNovoValorChange = {
                        whatsappNovo = it.filter { c -> c.isDigit() }.take(11)
                    },
                    onAdicionar = {
                        scope.launch {
                            mensagem = null

                            if (whatsappNovo.length < 11) {
                                mensagem = "Informe um número de WhatsApp completo."
                                return@launch
                            }

                            val sucesso = repository.adicionarNotificacao(
                                sensorId = sensorId,
                                tipo = "whatsapp",
                                destino = whatsappNovo // já são só dígitos
                            )

                            if (sucesso) {
                                whatsappNovo = ""
                                mensagem = "WhatsApp adicionado com sucesso."
                                recarregar()
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
                                mensagem = "Contato inválido."
                                return@launch
                            }
                            val sucesso = repository.excluirNotificacao(id)
                            if (sucesso) {
                                mensagem = "WhatsApp removido com sucesso."
                                recarregar()
                            } else {
                                mensagem = "Erro ao remover WhatsApp."
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                ContatoGrupoCard(
                    titulo = "Email",
                    tipo = "email",
                    iconeTipo = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = TriadeGreen
                        )
                    },
                    contatos = notificacoes.filter { it.tipo == "email" },
                    novoValor = emailNovo,
                    labelNovoValor = "Novo email",
                    keyboardType = KeyboardType.Email,
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
                                recarregar()
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
                                mensagem = "Contato inválido."
                                return@launch
                            }
                            val sucesso = repository.excluirNotificacao(id)
                            if (sucesso) {
                                mensagem = "Email removido com sucesso."
                                recarregar()
                            } else {
                                mensagem = "Erro ao remover email."
                            }
                        }
                    }
                )

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
private fun ContatoGrupoCard(
    titulo: String,
    tipo: String,
    iconeTipo: @Composable () -> Unit,
    contatos: List<SensorNotificacaoDTO>,
    novoValor: String,
    labelNovoValor: String,
    keyboardType: KeyboardType,
    onNovoValorChange: (String) -> Unit,
    onAdicionar: () -> Unit,
    onExcluir: (SensorNotificacaoDTO) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardLight)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                iconeTipo()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${contatos.size} contato(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (contatos.isEmpty()) {
                Text(
                    text = "Nenhum contato cadastrado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                contatos.forEach { contato ->
                    ContatoItem(contato = contato, onExcluir = { onExcluir(contato) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = novoValor,
                onValueChange = onNovoValorChange,
                label = { Text(labelNovoValor) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                // Aplica máscara visual só no WhatsApp
                visualTransformation = if (tipo == "whatsapp")
                    WhatsappVisualTransformation()
                else
                    VisualTransformation.None,
                colors = campoColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onAdicionar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TriadeGreen,
                    contentColor = Color.White
                )
            ) {
                Text("Adicionar $titulo")
            }
        }
    }
}

@Composable
private fun ContatoItem(
    contato: SensorNotificacaoDTO,
    onExcluir: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contato.destino,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (contato.enabled) "Ativo para alertas" else "Desativado",
                style = MaterialTheme.typography.bodySmall,
                color = if (contato.enabled) TriadeGreen else Color.Gray
            )
        }
        IconButton(onClick = onExcluir) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Excluir contato",
                tint = TriadeRed
            )
        }
    }
}

@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TriadeGreen,
    focusedLabelColor = TriadeGreen,
    cursorColor = TriadeGreen
)