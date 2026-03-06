package com.example.triade_monitoramento

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TemperatureScreen(
    background: Color = Color.White,
    state: TemperatureUiState,
    onApplyPeriod: (startIso: String, stopIso: String) -> Unit,
    onBackToRealtime: () -> Unit
) {
    var showFilter by remember { mutableStateOf(false) }
    var showHumidity by remember { mutableStateOf(false) }

    if (showFilter) {
        FiltroPeriodoScreen(
            titulo = "Filtrar histórico",
            onBack = { showFilter = false },
            onApply = { startMillis, endMillis ->
                val startIso = millisToIsoSaoPaulo(startMillis)
                val stopIso = millisToIsoSaoPaulo(endMillis)
                showFilter = false
                onApplyPeriod(startIso, stopIso)
            }
        )
        return
    }

    val hasFilterApplied = state.periodStartIso != null && state.periodStopIso != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo do app",
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Monitoramento",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(16.dp)) {
                val temp = state.latestTemp
                val hum = state.latestHum

                val tempColor = when {
                    temp == null -> Color.Gray
                    temp > 29.15 -> Color.Red
                    else -> Color(0xFF2E7D32)
                }

                Text(
                    text = buildAnnotatedString {

                        append("Atual: ")

                        if (temp != null) {
                            withStyle(
                                style = SpanStyle(color = Color(0xFF769F86))
                            ) {
                                append("%.2f °C".format(temp))
                            }
                        } else {
                            append("-- °C")
                        }

                        append(" | ")

                        if (hum != null) {
                            withStyle(
                                style = SpanStyle(color = Color(0xFFC9BF5A))
                            ) {
                                append("%.1f %%".format(hum))
                            }
                        } else {
                            append("-- %")
                        }
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (showHumidity) "Mostrando: Umidade (%)" else "Mostrando: Temperatura (°C)",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Switch(
                        checked = showHumidity,
                        onCheckedChange = { showHumidity = it },
                        enabled = !state.isLoading,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFC9BF5A),
                            checkedTrackColor = Color(0xFFC9BF5A).copy(alpha = 0.5f),
                            uncheckedThumbColor = Color(0xFF769F86),
                            uncheckedTrackColor = Color(0xFF769F86).copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = if (showHumidity)
                            Color(0xFFC9BF5A)
                        else
                            Color(0xFF769F86)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        onClick = { showFilter = true },
                        enabled = !state.isLoading,
                        modifier = Modifier.padding(horizontal = 1.dp)
                    ) {
                        Text("Filtrar período")
                    }

                    OutlinedButton(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9BF5A)),
                        onClick = onBackToRealtime,
                        shape = RoundedCornerShape(8.dp),
                        enabled = !state.isLoading && hasFilterApplied
                    ) {
                        Text("Tempo real")
                    }
                }

                val periodText = remember(state.periodStartIso, state.periodStopIso) {
                    formatPeriodPtBr(state.periodStartIso, state.periodStopIso)
                }

                if (periodText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = periodText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                state.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text("Erro: $it", color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))

                TemperatureLineChart(
                    points = state.chartPoints,
                    showHumidity = showHumidity
                )
            }
        }
    }
}

@Composable
private fun TemperatureLineChart(
    points: List<TemperaturePointDto>,
    showHumidity: Boolean
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .border(
                width = 2.dp,
                color = if (showHumidity)
                    Color(0xFFC9BF5A)
                else
                    Color(0xFF769F86),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(top = 12.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                legend.isEnabled = false

                val ds = LineDataSet(emptyList(), "Temperatura (°C)").apply {
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 2f
                }
                data = LineData(ds)
            }
        },
        update = { chart ->
            val data = chart.data ?: return@AndroidView
            val ds = data.getDataSetByIndex(0) as? LineDataSet ?: return@AndroidView

            val lineColor = if (showHumidity) {
                android.graphics.Color.parseColor("#C9BF5A") // Umidade
            } else {
                android.graphics.Color.parseColor("#769F86") // Temperatura
            }

            ds.color = lineColor

            ds.label = if (showHumidity) "Umidade (%)" else "Temperatura (°C)"

            val entries = points.mapIndexed { index, p ->
                val y = if (showHumidity) {
                    p.umidade.toFloat()
                } else {
                    p.temperatura.toFloat()
                }
                Entry(index.toFloat(), y)
            }

            ds.values = entries
            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )
}


private fun millisToIsoSaoPaulo(millis: Long): String {
    val zone = ZoneId.of("America/Sao_Paulo")
    val odt = Instant.ofEpochMilli(millis).atZone(zone).toOffsetDateTime()
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(odt)
}

private fun formatPeriodPtBr(startIso: String?, stopIso: String?): String? {
    if (startIso.isNullOrBlank() || stopIso.isNullOrBlank()) return null

    return try {
        val start = OffsetDateTime.parse(startIso)
        val stop = OffsetDateTime.parse(stopIso)
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        "Período: ${start.format(fmt)} → ${stop.format(fmt)}"
    } catch (_: Exception) {
        "Período: $startIso → $stopIso"
    }
}