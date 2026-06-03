package com.example.triade_monitoramento.ui.sensor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.data.remote.SupabaseClientProvider
import com.example.triade_monitoramento.data.repository.SensorRepository
import com.example.triade_monitoramento.ui.navigation.ScreenContainer
import kotlinx.coroutines.launch

private val TriadeGreen = Color(0xFF769F86)
private val TriadeRed = Color(0xFFC75C5C)
private val SuccessGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracaoAlarmeDeTemperaturaScreen(
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

    val repository = remember {
        SensorRepository(SupabaseClientProvider.client)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(sensorId) {
        val sensor = repository.buscarSensorPorId(sensorId)

        if (sensor != null) {
            nomeSensor = sensor.nome ?: ""
            temperaturaMaxima = sensor.tempLimitMax?.toString()?.replace(".", ",") ?: ""
            temperaturaMinima = sensor.tempLimitMin?.toString()?.replace(".", ",") ?: ""
        } else {
            mensagem = "Sensor não encontrado."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar alarmes") },
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
                    text = "Defina o nome do sensor e os limites de temperatura para disparo de alertas.",
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
                    colors = campoColorsAlarme()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nomeSensor,
                    onValueChange = { nomeSensor = it },
                    label = { Text("Nome do Sensor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = campoColorsAlarme()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = temperaturaMaxima,
                    onValueChange = { temperaturaMaxima = it },
                    label = { Text("Temperatura Máxima") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = campoColorsAlarme()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = temperaturaMinima,
                    onValueChange = { temperaturaMinima = it },
                    label = { Text("Temperatura Mínima") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = campoColorsAlarme()
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
                    val cor = if (it.contains("sucesso", ignoreCase = true)) {
                        SuccessGreen
                    } else {
                        TriadeRed
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
private fun campoColorsAlarme() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TriadeGreen,
    focusedLabelColor = TriadeGreen,
    cursorColor = TriadeGreen
)