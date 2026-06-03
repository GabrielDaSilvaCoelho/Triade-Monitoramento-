package com.example.triade_monitoramento.ui.sensor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.ui.navigation.ScreenContainer

private val TriadeGreen = Color(0xFF769F86)
private val TriadeBorder = Color(0xFF8AA796)
private val TriadeRed = Color(0xFFD84C3E)
private val TriadeYellow = Color(0xFFC9BF5A)
private val TextDark = Color(0xFF1F1F1F)
private val CardBg = Color.White

data class SensorListItemUi(
    val sensorId: String,
    val nome: String,
    val temperaturaAtual: Double?,
    val umidadeAtual: Double?,
    val tempLimitMax: Double?,
    val tempLimitMin: Double?,

    val totalOpenings: Int = 0,
    val shortOpenings: Int = 0,
    val longOpenings: Int = 0,

    val acknowledged: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SensoresScreen(
    sensores: List<SensorListItemUi>,
    onBack: () -> Unit,
    onAbrirConfiguracao: (SensorListItemUi) -> Unit,
    onRefresh: () -> Unit,
    onReconhecerAlerta: (String) -> Unit
) {
    var refreshing by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            onRefresh()
            refreshing = false
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Áreas") },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pullRefresh(pullRefreshState)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            text = "Selecione um sensor para visualizar ou editar suas configurações.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (sensores.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = CardBg
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.5.dp,
                                            TriadeBorder,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhum sensor cadastrado ou carregando dados...",
                                        color = TextDark,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = sensores,
                            key = { it.sensorId }
                        ) { sensor ->
                            SensorListCard(
                                sensor = sensor,
                                onClick = { onAbrirConfiguracao(sensor) },
                                onReconhecerAlerta = {
                                    onReconhecerAlerta(sensor.sensorId)
                                }
                            )
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = refreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = Color.White,
                    contentColor = TriadeGreen
                )
            }
        }
    }
}

@Composable
private fun SensorListCard(
    sensor: SensorListItemUi,
    onClick: () -> Unit,
    onReconhecerAlerta: () -> Unit
) {
    val statusColor = calcularCorStatus(
        temperatura = sensor.temperaturaAtual,
        tempMin = sensor.tempLimitMin,
        tempMax = sensor.tempLimitMax
    )

    val estaForaDoLimite =
        sensor.temperaturaAtual != null &&
                sensor.tempLimitMin != null &&
                sensor.tempLimitMax != null &&
                (
                        sensor.temperaturaAtual < sensor.tempLimitMin ||
                                sensor.temperaturaAtual > sensor.tempLimitMax
                        )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sensor.nome.ifBlank { sensor.sensorId },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Abrir configuração",
                    tint = TriadeGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(color = TriadeBorder.copy(alpha = 0.35f))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorInfoBloco(
                    titulo = "Status",
                    customContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(statusColor, CircleShape)
                            )

                            Spacer(modifier = Modifier.size(8.dp))

                            Text(
                                text = when {
                                    sensor.temperaturaAtual == null -> "Sem dados"
                                    sensor.tempLimitMin == null || sensor.tempLimitMax == null -> "Sem limite"
                                    estaForaDoLimite -> "Fora do limite"
                                    else -> "Normal"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDark
                            )
                        }
                    }
                )

                SensorInfoBloco(
                    titulo = "Temp",
                    valor = sensor.temperaturaAtual?.let {
                        "%.1f °C".format(it)
                    } ?: "-- °C"
                )

                SensorInfoBloco(
                    titulo = "Umid",
                    valor = sensor.umidadeAtual?.let {
                        "%.1f %%".format(it)
                    } ?: "-- %"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = TriadeBorder.copy(alpha = 0.35f))

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Porta (24h)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "< 1 min: ${sensor.shortOpenings}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "> 5 min: ${sensor.longOpenings}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Total: ${sensor.totalOpenings}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = buildString {
                    append("Limites: ")
                    append(sensor.tempLimitMin?.let { "%.1f".format(it) } ?: "--")
                    append(" °C até ")
                    append(sensor.tempLimitMax?.let { "%.1f".format(it) } ?: "--")
                    append(" °C")
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.75f)
            )

            if (estaForaDoLimite) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (!sensor.acknowledged) {
                            onReconhecerAlerta()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !sensor.acknowledged,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sensor.acknowledged) Color.Gray else TriadeGreen,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (sensor.acknowledged) {
                            "Aguardando normalização"
                        } else {
                            "Reconhecer alerta"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorInfoBloco(
    titulo: String,
    valor: String? = null,
    customContent: @Composable (() -> Unit)? = null
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = "$titulo:",
            style = MaterialTheme.typography.labelMedium,
            color = TextDark.copy(alpha = 0.75f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (customContent != null) {
            customContent()
        } else {
            Text(
                text = valor.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (titulo == "Umid") TriadeYellow else TextDark
            )
        }
    }
}

private fun calcularCorStatus(
    temperatura: Double?,
    tempMin: Double?,
    tempMax: Double?
): Color {
    if (temperatura == null) return TriadeRed
    if (tempMin == null || tempMax == null) return TriadeYellow

    return if (temperatura < tempMin || temperatura > tempMax) {
        TriadeRed
    } else {
        TriadeGreen
    }
}