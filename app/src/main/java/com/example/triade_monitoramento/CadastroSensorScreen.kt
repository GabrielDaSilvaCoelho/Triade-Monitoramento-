package com.example.triade_monitoramento.ui.sensor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.SupabaseClient
import com.example.triade_monitoramento.data.repository.SensorRepository
import kotlinx.coroutines.launch

@Composable
fun CadastroSensorScreen() {

    var sensorId by remember { mutableStateOf("") }
    var nomeSensor by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val repository = remember {
        SensorRepository(SupabaseClient.client)
    }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Cadastrar Sensor",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = sensorId,
            onValueChange = { sensorId = it },
            label = { Text("ID do Sensor") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nomeSensor,
            onValueChange = { nomeSensor = it },
            label = { Text("Nome do Sensor") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
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
            enabled = !loading
        ) {

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Cadastrar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        mensagem?.let {
            Text(
                text = it,
                color = if (it.contains("sucesso")) Color(0xFF2E7D32) else Color.Red
            )
        }
    }
}
