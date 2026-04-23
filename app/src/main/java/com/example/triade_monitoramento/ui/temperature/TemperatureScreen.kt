package com.example.triade_monitoramento.ui.temperature

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.example.triade_monitoramento.data.api.TemperatureApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.triade_monitoramento.R
import com.example.triade_monitoramento.data.model.TemperaturePointDto
import com.example.triade_monitoramento.data.model.UserSensor
import com.example.triade_monitoramento.ui.components.ChartMarkerView
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

private enum class ChartMetric {
    TEMPERATURE,
    HUMIDITY
}

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
    var sensorMenuExpanded by remember { mutableStateOf(false) }
    var metricMenuExpanded by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf(ChartMetric.TEMPERATURE) }
    var clearChartPopupSignal by remember { mutableIntStateOf(0) }

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

    val showHumidity = selectedMetric == ChartMetric.HUMIDITY
    val accentColor = if (showHumidity) Color(0xFFC9BF5A) else Color(0xFF769F86)
    val accentSoft = if (showHumidity) Color(0xFFFFF8E1) else Color(0xFFF3F8F5)
    val accentBorder = if (showHumidity) Color(0xFFE0C95A) else Color(0xFFB7CEC0)
    val accentText = if (showHumidity) Color(0xFF8A6D1F) else Color(0xFF4E6B5A)

    val periodText = remember(state.periodStartIso, state.periodStopIso) {
        formatPeriodPtBr(state.periodStartIso, state.periodStopIso)
    }

    val isFiltered = periodText != null
    val scrollState = rememberScrollState()

    MenuLateralTriade(
        onCadastrarSensor = onGoToSensorRegister,
        onPerfil = onGoToPerfil,
        onSensores = onGoToSensores,
        onSair = onSair
    ) { openDrawer ->

        ScreenContainer(background = background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            clearChartPopupSignal++
                        }
                    }
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            clearChartPopupSignal++
                            openDrawer()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Abrir menu"
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo do app",
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Monitoramento",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        SensorSelector(
                            currentSensor = currentSensor,
                            availableSensors = availableSensors,
                            expanded = sensorMenuExpanded,
                            onExpandedChange = {
                                clearChartPopupSignal++
                                sensorMenuExpanded = it
                            },
                            onSelectSensor = {
                                clearChartPopupSignal++
                                sensorMenuExpanded = false
                                onSelectSensor(it)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                                        append("%.1f °C".format(temp))
                                    }
                                } else {
                                    append("-- °C")
                                }

                                append(" | ")

                                if (hum != null) {
                                    withStyle(style = SpanStyle(color = Color(0xFFC9BF5A))) {
                                        append("%d %% UR".format(hum.roundToInt()))
                                    }
                                } else {
                                    append("-- %")
                                }
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        MetricSelector(
                            selectedMetric = selectedMetric,
                            expanded = metricMenuExpanded,
                            onExpandedChange = {
                                clearChartPopupSignal++
                                metricMenuExpanded = it
                            },
                            onSelectMetric = {
                                clearChartPopupSignal++
                                selectedMetric = it
                                metricMenuExpanded = false
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor
                                ),
                                shape = RoundedCornerShape(14.dp),
                                onClick = {
                                    clearChartPopupSignal++
                                    showFilter = true
                                },
                                enabled = !state.isLoading,
                                modifier = Modifier
                                    .weight(if (isFiltered) 1.15f else 1f)
                                    .height(54.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Filtrar período",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            AnimatedVisibility(
                                visible = isFiltered,
                                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clearChartPopupSignal++
                                        onBackToRealtime()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !state.isLoading,
                                    border = BorderStroke(1.dp, accentColor),
                                    modifier = Modifier
                                        .widthIn(min = 152.dp)
                                        .height(54.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "Limpar filtro",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = accentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (periodText != null) {
                            Spacer(Modifier.height(12.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = accentSoft),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, accentBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Período selecionado",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accentText,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = periodText.removePrefix("Período: "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
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
                            showHumidity = showHumidity,
                            clearPopupSignal = clearChartPopupSignal
                        )

                        Spacer(Modifier.height(12.dp))
                    }
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
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
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
private fun MetricSelector(
    selectedMetric: ChartMetric,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectMetric: (ChartMetric) -> Unit
) {
    Column {
        Text(
            text = "Visualização:",
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
                        color = when (selectedMetric) {
                            ChartMetric.TEMPERATURE -> Color(0xFF769F86)
                            ChartMetric.HUMIDITY -> Color(0xFFC9BF5A)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (selectedMetric) {
                        ChartMetric.TEMPERATURE -> "Temperatura (°C)"
                        ChartMetric.HUMIDITY -> "Umidade (%)"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )

                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Abrir opções de visualização"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                DropdownMenuItem(
                    text = { Text("Temperatura (°C)") },
                    onClick = { onSelectMetric(ChartMetric.TEMPERATURE) }
                )
                DropdownMenuItem(
                    text = { Text("Umidade (%)") },
                    onClick = { onSelectMetric(ChartMetric.HUMIDITY) }
                )
            }
        }
    }
}

@Composable
private fun TemperatureLineChart(
    points: List<TemperaturePointDto>,
    showHumidity: Boolean,
    clearPopupSignal: Int
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
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
                isHighlightPerTapEnabled = true

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
                    lineWidth = 1.8f
                    mode = LineDataSet.Mode.LINEAR
                    color = android.graphics.Color.parseColor("#769F86")
                    highLightColor = android.graphics.Color.RED
                    setDrawHorizontalHighlightIndicator(false)
                }

                data = LineData(ds)
            }
        },
        update = { chart ->
            val data = chart.data ?: return@AndroidView
            val ds = data.getDataSetByIndex(0) as? LineDataSet ?: return@AndroidView

            val sortedPoints = points.sortedBy {
                parseChartTimeToEpochMillis(it.ts) ?: Long.MAX_VALUE
            }

            val marker = ChartMarkerView(
                context = chart.context,
                layoutResource = R.layout.chart_marker_view,
                points = sortedPoints,
                showHumidity = showHumidity
            )

            chart.marker = marker

            if (clearPopupSignal > 0) {
                marker.isVisibleMarker = false
                chart.highlightValues(null)
                chart.highlightValue(null)
                chart.invalidate()
            }

            val lineColor = if (showHumidity) {
                android.graphics.Color.parseColor("#C9BF5A")
            } else {
                android.graphics.Color.parseColor("#769F86")
            }

            ds.color = lineColor
            ds.label = if (showHumidity) "Umidade (%)" else "Temperatura (°C)"

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

            chart.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_UP -> {
                        val h = chart.getHighlightByTouchPoint(event.x, event.y)
                        if (h == null) {
                            marker.isVisibleMarker = false
                            chart.highlightValues(null)
                            chart.highlightValue(null)
                            chart.invalidate()
                        } else {
                            marker.isVisibleMarker = true
                        }
                    }
                }
                false
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

private fun millisToIsoSaoPaulo(millis: Long): String {
    val zone = ZoneId.of("America/Sao_Paulo")
    val odt = Instant.ofEpochMilli(millis).atZone(zone).toOffsetDateTime()
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(odt)
}

private fun formatPeriodPtBr(startIso: String?, stopIso: String?): String? {
    if (startIso.isNullOrBlank() || stopIso.isNullOrBlank()) return null

    val zone = ZoneId.of("America/Sao_Paulo")
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

    return try {
        val start = OffsetDateTime.parse(startIso).atZoneSameInstant(zone)
        val stop = OffsetDateTime.parse(stopIso).atZoneSameInstant(zone)
        "Período: ${start.format(fmt)} até ${stop.format(fmt)}"
    } catch (_: Exception) {
        try {
            val start = Instant.parse(startIso).atZone(zone)
            val stop = Instant.parse(stopIso).atZone(zone)
            "Período: ${start.format(fmt)} até ${stop.format(fmt)}"
        } catch (_: Exception) {
            "Período: $startIso até $stopIso"
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