package com.example.triade_monitoramento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

data class SensorFormData(
    val nomeSensor: String,
    val sensorId: String,
    val temperaturaMinima: String,
    val temperaturaMaxima: String,
    val emailAlerta: String,
    val whatsappAlerta: String
)

@Composable
fun CadastroSensorScreen(
    onSalvar: (SensorFormData) -> Unit,
    onVoltar: () -> Unit
) {
    var nomeSensor by rememberSaveable { mutableStateOf("") }
    var sensorId by rememberSaveable { mutableStateOf("") }
    var temperaturaMinima by rememberSaveable { mutableStateOf("") }
    var temperaturaMaxima by rememberSaveable { mutableStateOf("") }
    var emailAlerta by rememberSaveable { mutableStateOf("") }
    var whatsappAlerta by rememberSaveable { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cadastro de Sensor",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nomeSensor,
            onValueChange = { nomeSensor = it },
            label = { Text("Nome do sensor") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = sensorId,
            onValueChange = { sensorId = it },
            label = { Text("ID do sensor") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = temperaturaMinima,
            onValueChange = { temperaturaMinima = it },
            label = { Text("Temperatura mínima") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = temperaturaMaxima,
            onValueChange = { temperaturaMaxima = it },
            label = { Text("Temperatura máxima") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = emailAlerta,
            onValueChange = { emailAlerta = it },
            label = { Text("Email para alerta") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = whatsappAlerta,
            onValueChange = { whatsappAlerta = it },
            label = { Text("WhatsApp para alerta") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSalvar(
                    SensorFormData(
                        nomeSensor = nomeSensor,
                        sensorId = sensorId,
                        temperaturaMinima = temperaturaMinima,
                        temperaturaMaxima = temperaturaMaxima,
                        emailAlerta = emailAlerta,
                        whatsappAlerta = whatsappAlerta
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar sensor")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}