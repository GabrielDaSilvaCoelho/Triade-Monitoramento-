package com.example.triade_monitoramento.ui.temperature

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.example.triade_monitoramento.R
import com.example.triade_monitoramento.ui.temperature.TemperatureUiState
import com.example.triade_monitoramento.data.model.TemperaturePointDto
import com.example.triade_monitoramento.data.model.UserSensor
import com.example.triade_monitoramento.ui.components.MenuLateralTriade
import com.example.triade_monitoramento.ui.navigation.ScreenContainer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TemperatureScreen(
    background: Color = Color.White,
    state: TemperatureUiState,
    currentSensor: UserSensor?,
    availableSensors: List<UserSensor>,
    onSelectSensor: (UserSensor) -> Unit,
    onGoToSensorRegister: () -> Unit,
    onApplyPeriod: (startIso: String, stopIso: String) -> Unit,
    onBackToRealtime: () -> Unit,
    onGoToPerfil: () -> Unit,
    onGoToSensores: () -> Unit,
    onSair: () -> Unit
) {
    var showFilter by remember { mutableStateOf(false) }
    var showHumidity by remember { mutableStateOf(false) }
    var sensorMenuExpanded by remember { mutableStateOf(false) }

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

    MenuLateralTriade(
        onCadastrarSensor = onGoToSensorRegister,
        onPerfil = onGoToPerfil,
        onSensores = onGoToSensores,
        onSair = onSair
    ) { openDrawer ->

        ScreenContainer(background = background) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo do app",
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { openDrawer() }
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
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {

                    SensorSelector(
                        currentSensor = currentSensor,
                        availableSensors = availableSensors,
                        expanded = sensorMenuExpanded,
                        onExpandedChange = { sensorMenuExpanded = it },
                        onSelectSensor = {
                            sensorMenuExpanded = false
                            onSelectSensor(it)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                                withStyle(style = SpanStyle(color = tempColor)) {
                                    append("%.2f °C".format(temp))
                                }
                            } else {
                                append("-- °C")
                            }

                            append(" | ")

                            if (hum != null) {
                                withStyle(style = SpanStyle(color = Color(0xFFC9BF5A))) {
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
                            text = if (showHumidity) {
                                "Mostrando: Umidade (%)"
                            } else {
                                "Mostrando: Temperatura (°C)"
                            },
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showHumidity) {
                                    Color(0xFFC9BF5A)
                                } else {
                                    Color(0xFF769F86)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            onClick = { showFilter = true },
                            enabled = !state.isLoading,
                            modifier = Modifier.padding(horizontal = 1.dp)
                        ) {
                            Text("Filtrar período")
                        }

                        OutlinedButton(
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

                    state.error?.let { erro ->
                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Erro ao carregar os dados.\n$erro\nVerifique conexão do Sensor.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
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
}

@Composable
private fun SensorSelector(
    currentSensor: UserSensor?,
    availableSensors: List<UserSensor>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectSensor: (UserSensor) -> Unit
) {
    Column {
        Text(
            text = "Sensor:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(true) }
                    .border(
                        width = 1.dp,
                        color = Color(0xFF769F86),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentSensor?.displayName() ?: "Selecione um sensor",
                    style = MaterialTheme.typography.bodyLarge
                )

                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = "Abrir lista de sensores"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                if (availableSensors.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Nenhum sensor cadastrado") },
                        onClick = { onExpandedChange(false) }
                    )
                } else {
                    availableSensors.forEach { sensor ->
                        DropdownMenuItem(
                            text = { Text(sensor.displayName()) },
                            onClick = { onSelectSensor(sensor) }
                        )
                    }
                }
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
                color = if (showHumidity) Color(0xFFC9BF5A) else Color(0xFF769F86),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(top = 12.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setDragEnabled(true)
                setScaleEnabled(true)
                setScaleXEnabled(true)
                setScaleYEnabled(false)
                setPinchZoom(false)
                extraBottomOffset = 16f
                minOffset = 12f
                isDoubleTapToZoomEnabled = true
                setDragDecelerationEnabled(true)
                setNoDataText("Sem dados para exibir")
                legend.isEnabled = false

                axisRight.isEnabled = true

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    granularity = 1f
                    setLabelCount(3, true)
                    labelRotationAngle = -30f
                    textSize = 9f
                    yOffset = 8f
                    setAvoidFirstLastClipping(true)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                }

                axisRight.apply {
                    setDrawGridLines(false)
                }

                val ds = LineDataSet(emptyList(), "Temperatura (°C)").apply {
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 2.5f
                    mode = LineDataSet.Mode.LINEAR
                    highLightColor = android.graphics.Color.GRAY
                }

                data = LineData(ds)
            }
        },
        update = { chart ->
            val data = chart.data ?: return@AndroidView
            val ds = data.getDataSetByIndex(0) as? LineDataSet ?: return@AndroidView

            val lineColor = if (showHumidity) {
                android.graphics.Color.parseColor("#C9BF5A")
            } else {
                android.graphics.Color.parseColor("#769F86")
            }

            ds.color = lineColor
            ds.label = if (showHumidity) "Umidade (%)" else "Temperatura (°C)"

            val sortedPoints = points.sortedBy { parseChartTimeToEpochMillis(it.ts) ?: Long.MAX_VALUE }

            val entries = sortedPoints.mapIndexedNotNull { index, p ->
                val y = if (showHumidity) p.umidade.toFloat() else p.temperatura.toFloat()
                Entry(index.toFloat(), y)
            }

            ds.values = entries

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.roundToInt()
                    if (index < 0 || index >= sortedPoints.size) return ""
                    return formatChartTimeSeparated(sortedPoints[index].ts)
                }
            }

            if (entries.isNotEmpty()) {
                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum = (entries.size - 1).toFloat()

                val minY = entries.minOf { it.y }
                val maxY = entries.maxOf { it.y }
                val padding = ((maxY - minY) * 0.10f).coerceAtLeast(1f)

                chart.axisLeft.axisMinimum = minY - padding
                chart.axisLeft.axisMaximum = maxY + padding
                chart.axisRight.axisMinimum = minY - padding
                chart.axisRight.axisMaximum = maxY + padding

                chart.fitScreen()
                chart.xAxis.setLabelCount(calculateLabelCountForFullRange(sortedPoints), true)
            }

            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )
}

private fun calculateLabelCountForFullRange(points: List<TemperaturePointDto>): Int {
    val size = points.size

    return when {
        size <= 20 -> 3
        size <= 60 -> 4
        size <= 180 -> 4
        size <= 720 -> 5
        else -> 5
    }
}

private fun formatChartTimeCompact(ts: String?, totalPoints: Int): String {
    if (ts.isNullOrBlank()) return " "

    val zone = ZoneId.of("America/Sao_Paulo")

    val pattern = when {
        totalPoints <= 120 -> "HH:mm"
        totalPoints <= 1440 -> "HH:mm"
        else -> "dd/MM"
    }

    return try {
        OffsetDateTime.parse(ts)
            .atZoneSameInstant(zone)
            .format(DateTimeFormatter.ofPattern(pattern))
    } catch (_: Exception) {
        try {
            Instant.parse(ts)
                .atZone(zone)
                .format(DateTimeFormatter.ofPattern(pattern))
        } catch (_: Exception) {
            ""
        }
    }
}

private fun calculateInitialVisiblePoints(points: List<TemperaturePointDto>): Int {
    if (points.isEmpty()) return 10
    if (points.size <= 10) return points.size.coerceAtLeast(1)

    val first = parseChartTimeToEpochMillis(points.firstOrNull()?.ts)
    val last = parseChartTimeToEpochMillis(points.lastOrNull()?.ts)

    if (first == null || last == null || last <= first) {
        return points.size.coerceAtMost(60).coerceAtLeast(10)
    }

    val totalDurationMinutes = ((last - first) / 60000.0).coerceAtLeast(1.0)
    val avgMinutesPerPoint = totalDurationMinutes / (points.size - 1).coerceAtLeast(1)

    val targetWindowMinutes = when {
        totalDurationMinutes <= 120.0 -> 60.0
        totalDurationMinutes <= 720.0 -> 120.0
        totalDurationMinutes <= 1440.0 -> 180.0
        else -> 360.0
    }

    return (targetWindowMinutes / avgMinutesPerPoint)
        .roundToInt()
        .coerceAtLeast(10)
        .coerceAtMost(points.size)
}

private fun millisToIsoSaoPaulo(millis: Long): String {
    val zone = ZoneId.of("America/Sao_Paulo")
    val odt = Instant.ofEpochMilli(millis).atZone(zone).toOffsetDateTime()
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(odt)
}

private fun formatPeriodPtBr(startIso: String?, stopIso: String?): String? {
    if (startIso.isNullOrBlank() || stopIso.isNullOrBlank()) return null

    val zone = ZoneId.of("America/Sao_Paulo")
    val fmt = DateTimeFormatter.ofPattern("dd/MM\nHH:mm", Locale("pt", "BR"))

    return try {
        val start = OffsetDateTime.parse(startIso).atZoneSameInstant(zone)
        val stop = OffsetDateTime.parse(stopIso).atZoneSameInstant(zone)
        "Período: ${start.format(fmt)} → ${stop.format(fmt)}"
    } catch (_: Exception) {
        try {
            val start = Instant.parse(startIso).atZone(zone)
            val stop = Instant.parse(stopIso).atZone(zone)
            "Período: ${start.format(fmt)} → ${stop.format(fmt)}"
        } catch (_: Exception) {
            "Período: $startIso → $stopIso"
        }
    }
}

private fun formatChartTime(ts: String?): String {
    if (ts.isNullOrBlank()) return ""

    val zone = ZoneId.of("America/Sao_Paulo")

    return try {
        OffsetDateTime.parse(ts)
            .atZoneSameInstant(zone)
            .format(DateTimeFormatter.ofPattern("dd/MM\nHH:mm"))
    } catch (_: Exception) {
        try {
            Instant.parse(ts)
                .atZone(zone)
                .format(DateTimeFormatter.ofPattern("dd/MM\nHH:mm"))
        } catch (_: Exception) {
            ""
        }
    }
}

private fun parseChartTimeToEpochMillis(ts: String?): Long? {
    if (ts.isNullOrBlank()) return null

    return try {
        OffsetDateTime.parse(ts).toInstant().toEpochMilli()
    } catch (_: Exception) {
        try {
            Instant.parse(ts).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}

private fun formatChartTimeSeparated(ts: String?): String {
    if (ts.isNullOrBlank()) return ""

    val zone = ZoneId.of("America/Sao_Paulo")

    return try {
        OffsetDateTime.parse(ts)
            .atZoneSameInstant(zone)
            .format(DateTimeFormatter.ofPattern("dd/MM  •  HH:mm"))
    } catch (_: Exception) {
        try {
            Instant.parse(ts)
                .atZone(zone)
                .format(DateTimeFormatter.ofPattern("dd/MM  •  HH:mm"))
        } catch (_: Exception) {
            ""
        }
    }
}