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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.ScreenContainer
import com.example.triade_monitoramento.SupabaseClient
import com.example.triade_monitoramento.data.repository.SensorRepository
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroSensorScreen(
    onBack: () -> Unit
) {
    var sensorId by remember { mutableStateOf("") }
    var nomeSensor by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val repository = remember {
        SensorRepository(SupabaseClient.client)
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
            Column(
                modifier = Modifier.padding(top = 24.dp)
            ) {
                TopAppBar(
                    title = {
                        Text("Voltar")
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color(0xFF769F86)
                            )
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        ScreenContainer {
            Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))

            Text(
                text = "Cadastro de Sensor",
                style = MaterialTheme.typography.headlineMedium
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
                    singleLine = true
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
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Ler QR Code",
                        tint = Color(0xFF769F86)

                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Ler QR",
                    color = Color(0xFF769F86)
                    )

                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = sensorId,
                onValueChange = { sensorId = it },
                label = { Text("ID do Sensor") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF769F86),
                    focusedLabelColor = Color(0xFF769F86),
                    cursorColor = Color(0xFF769F86)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        mensagem = null

                        if (sensorId.isBlank() || nomeSensor.isBlank()) {
                            mensagem = "Preencha todos os campos"
                            return@launch
                        }

                        loading = true

                        val sucesso = repository.cadastrarSensor(sensorId, nomeSensor)

                        if (sucesso) {
                            mensagem = "Sensor cadastrado com sucesso!"
                            sensorId = ""
                            nomeSensor = ""
                        } else {
                            mensagem = "Erro ao cadastrar sensor"
                        }

                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF769F86),
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
                        Color(0xFF2E7D32)
                    } else {
                        Color.Red
                    }
                )
            }
        }
    }
}