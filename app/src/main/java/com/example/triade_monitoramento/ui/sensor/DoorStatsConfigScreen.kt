package com.example.triade_monitoramento.ui.sensor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.ui.navigation.ScreenContainer

private val TriadeGreen = Color(0xFF769F86)
private val TriadeBorder = Color(0xFF8AA796)
private val TextDark = Color(0xFF1F1F1F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoorStatsConfigScreen(
    currentRange: String = "24h",
    currentShortLimitMin: Int = 1,
    currentLongLimitMin: Int = 5,
    onBack: () -> Unit,
    onSave: (range: String, shortLimitMin: Int, longLimitMin: Int) -> Unit
) {
    var selectedRange by remember { mutableStateOf(currentRange) }
    var shortLimitText by remember { mutableStateOf(currentShortLimitMin.toString()) }
    var longLimitText by remember { mutableStateOf(currentLongLimitMin.toString()) }

    val shortLimit = shortLimitText.toIntOrNull()
    val longLimit = longLimitText.toIntOrNull()

    val isValid =
        !selectedRange.isBlank() &&
                shortLimit != null &&
                longLimit != null &&
                shortLimit >= 0 &&
                longLimit > 0 &&
                shortLimit < longLimit

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar abertura da porta") },
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
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Defina o período analisado e os limites usados para classificar as aberturas da porta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.8.dp,
                                color = TriadeBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Período de análise",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        RangeSelector(
                            selectedRange = selectedRange,
                            onSelectRange = { selectedRange = it }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Classificação das aberturas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = shortLimitText,
                            onValueChange = { value ->
                                shortLimitText = value.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("Abertura curta menor que") },
                            suffix = { Text("min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = longLimitText,
                            onValueChange = { value ->
                                longLimitText = value.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("Abertura longa maior que") },
                            suffix = { Text("min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = buildString {
                                append("Regra atual: ")
                                append("curta < ${shortLimitText.ifBlank { "--" }} min")
                                append(" | ")
                                append("longa > ${longLimitText.ifBlank { "--" }} min")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isValid) TextDark.copy(alpha = 0.75f) else Color.Red
                        )

                        if (!isValid) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Informe valores válidos. O limite curto precisa ser menor que o limite longo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (shortLimit != null && longLimit != null) {
                                    onSave(selectedRange, shortLimit, longLimit)
                                }
                            },
                            enabled = isValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TriadeGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("Salvar configuração")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(
    selectedRange: String,
    onSelectRange: (String) -> Unit
) {
    val ranges = listOf(
        "1h" to "1h",
        "6h" to "6h",
        "12h" to "12h",
        "24h" to "24h",
        "7d" to "7 dias",
        "15d" to "15 dias"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ranges.chunked(3).forEach { rowItems ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (value, label) ->
                    FilterChip(
                        selected = selectedRange == value,
                        onClick = { onSelectRange(value) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }

                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}