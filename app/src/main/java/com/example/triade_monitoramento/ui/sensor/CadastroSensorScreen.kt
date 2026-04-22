package com.example.triade_monitoramento.ui.sensor

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.ui.navigation.ScreenContainer
import com.example.triade_monitoramento.data.remote.SupabaseClientProvider
import com.example.triade_monitoramento.data.repository.SensorRepository
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

private val TriadeGreen = Color(0xFF769F86)
private val SuccessGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroSensorScreen(
    onBack: () -> Unit
) {
    var sensorId by remember { mutableStateOf("") }
    var nomeSensor by remember { mutableStateOf("") }
    var temperaturaMaxima by remember { mutableStateOf("") }
    var temperaturaMinima by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val repository = remember {
        SensorRepository(SupabaseClientProvider.client)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }

    val scanner = remember {
        GmsBarcodeScanning.getClient(context, scannerOptions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastrar Sensor") },
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
                    text = "Cadastre o sensor e defina os limites de alerta",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = sensorId,
                        onValueChange = { sensorId = it },
                        label = { Text("ID do Sensor") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = campoColors()
                    )

                    OutlinedButton(
                        onClick = {
                            if (activity == null) {
                                mensagem = "Não foi possível abrir a câmera"
                                return@OutlinedButton
                            }

                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    sensorId = barcode.rawValue.orEmpty()
                                    mensagem = if (sensorId.isNotBlank()) {
                                        "QR Code lido com sucesso!"
                                    } else {
                                        "QR Code sem conteúdo válido"
                                    }
                                }
                                .addOnCanceledListener {
                                    mensagem = "Leitura cancelada"
                                }
                                .addOnFailureListener { e ->
                                    mensagem = e.message ?: "Erro ao ler QR Code"
                                }
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TriadeGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Ler QR Code",
                            tint = TriadeGreen
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Ler QR", color = TriadeGreen)
                    }
                }

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

                Button(
                    onClick = {
                        scope.launch {
                            mensagem = null

                            if (
                                sensorId.isBlank() ||
                                nomeSensor.isBlank() ||
                                temperaturaMaxima.isBlank() ||
                                temperaturaMinima.isBlank()
                            ) {
                                mensagem = "Preencha todos os campos"
                                return@launch
                            }

                            val tempMax = temperaturaMaxima.replace(",", ".").toDoubleOrNull()
                            val tempMin = temperaturaMinima.replace(",", ".").toDoubleOrNull()

                            if (tempMax == null || tempMin == null) {
                                mensagem = "Informe valores numéricos válidos para as temperaturas"
                                return@launch
                            }

                            if (tempMin >= tempMax) {
                                mensagem = "A temperatura mínima deve ser menor que a máxima"
                                return@launch
                            }

                            loading = true

                            val sucesso = repository.cadastrarSensor(
                                id = sensorId.trim(),
                                nome = nomeSensor.trim(),
                                tempLimitMax = tempMax,
                                tempLimitMin = tempMin
                            )

                            if (sucesso) {
                                mensagem = "Sensor cadastrado com sucesso!"
                                sensorId = ""
                                nomeSensor = ""
                                temperaturaMaxima = ""
                                temperaturaMinima = ""
                            } else {
                                mensagem = "Erro ao cadastrar sensor. Caso o problema persista, contate a equipe técnica."
                            }

                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TriadeGreen,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Cadastrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                mensagem?.let {
                    Text(
                        text = it,
                        color = if (it.contains("sucesso", ignoreCase = true)) {
                            SuccessGreen
                        } else {
                            Color.Red
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TriadeGreen,
    focusedLabelColor = TriadeGreen,
    cursorColor = TriadeGreen
)
