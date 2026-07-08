package com.example.triade_monitoramento.ui.temperature

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import com.example.triade_monitoramento.ui.components.MenuLateralTriade
import com.example.triade_monitoramento.ui.navigation.ScreenContainer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ChartMetric {
    TEMPERATURE,
    HUMIDITY
}

@OptIn(ExperimentalMaterialApi::class)
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
    var selectedMetric by remember {
        mutableStateOf(ChartMetric.TEMPERATURE)
    }
    var clearChartSelectionSignal by remember {
        mutableIntStateOf(0)
    }
    var selectedChartPointTs by remember {
        mutableStateOf<String?>(null)
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = {
            selectedChartPointTs = null
            clearChartSelectionSignal++
            onBackToRealtime()
        }
    )

    if (showFilter) {
        FiltroPeriodoScreen(
            titulo = "Filtrar histórico",
            onBack = {
                showFilter = false
            },
            onApply = { startMillis, endMillis ->
                selectedChartPointTs = null
                clearChartSelectionSignal++
                showFilter = false

                onApplyPeriod(
                    millisToIsoSaoPaulo(startMillis),
                    millisToIsoSaoPaulo(endMillis)
                )
            }
        )

        return
    }

    val showHumidity =
        selectedMetric == ChartMetric.HUMIDITY

    val accentColor =
        if (showHumidity) {
            Color(0xFFC9BF5A)
        } else {
            Color(0xFF769F86)
        }

    val accentSoft =
        if (showHumidity) {
            Color(0xFFFFF8E1)
        } else {
            Color(0xFFF3F8F5)
        }

    val accentBorder =
        if (showHumidity) {
            Color(0xFFE0C95A)
        } else {
            Color(0xFFB7CEC0)
        }

    val accentText =
        if (showHumidity) {
            Color(0xFF8A6D1F)
        } else {
            Color(0xFF4E6B5A)
        }

    val periodText = remember(
        state.periodStartIso,
        state.periodStopIso
    ) {
        formatPeriodPtBr(
            state.periodStartIso,
            state.periodStopIso
        )
    }

    val selectedPointForCurrentSensor = remember(
        selectedChartPointTs,
        state.chartPoints
    ) {
        val selectedMillis =
            parseChartTimeToEpochMillis(
                selectedChartPointTs
            )

        if (selectedMillis == null) {
            null
        } else {
            state.chartPoints
                .mapNotNull { point ->
                    val pointMillis =
                        parseChartTimeToEpochMillis(
                            point.ts
                        )

                    if (pointMillis == null) {
                        null
                    } else {
                        point to abs(
                            pointMillis - selectedMillis
                        )
                    }
                }
                .minByOrNull { pair ->
                    pair.second
                }
                ?.first
        }
    }

    val isFiltered = periodText != null
    val scrollState = rememberScrollState()

    MenuLateralTriade(
        onCadastrarSensor = onGoToSensorRegister,
        onPerfil = onGoToPerfil,
        onSensores = onGoToSensores,
        onSair = onSair
    ) { openDrawer ->
        ScreenContainer(
            background = background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                openDrawer()
                            }
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Filled.Menu,
                                contentDescription =
                                    "Abrir menu"
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        Image(
                            painter = painterResource(
                                id = R.drawable.ic_logo
                            ),
                            contentDescription =
                                "Logo do app",
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = "Monitoramento",
                            style =
                                MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            SensorSelector(
                                currentSensor =
                                    currentSensor,
                                availableSensors =
                                    availableSensors,
                                expanded =
                                    sensorMenuExpanded,
                                onExpandedChange = {
                                    sensorMenuExpanded = it
                                },
                                onSelectSensor = {
                                    selectedChartPointTs =
                                        null
                                    clearChartSelectionSignal++
                                    sensorMenuExpanded = false
                                    onSelectSensor(it)
                                }
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            val temp =
                                state.latestTemp
                            val hum =
                                state.latestHum

                            val tempColor = when {
                                temp == null ->
                                    Color.Gray

                                temp > 29.15 ->
                                    Color.Red

                                else ->
                                    Color(0xFF2E7D32)
                            }

                            Text(
                                text = buildAnnotatedString {
                                    append("Atual: ")

                                    if (temp != null) {
                                        withStyle(
                                            SpanStyle(
                                                color =
                                                    tempColor
                                            )
                                        ) {
                                            append(
                                                "%.1f °C".format(
                                                    temp
                                                )
                                            )
                                        }
                                    } else {
                                        append("-- °C")
                                    }

                                    append(" | ")

                                    if (hum != null) {
                                        withStyle(
                                            SpanStyle(
                                                color =
                                                    Color(
                                                        0xFFC9BF5A
                                                    )
                                            )
                                        ) {
                                            append(
                                                "%d %% UR".format(
                                                    hum.roundToInt()
                                                )
                                            )
                                        }
                                    } else {
                                        append("-- %")
                                    }
                                },
                                style =
                                    MaterialTheme.typography
                                        .titleMedium
                                        .copy(
                                            fontSize = 24.sp
                                        ),
                                modifier =
                                    Modifier.fillMaxWidth()
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            MetricSelector(
                                selectedMetric =
                                    selectedMetric,
                                expanded =
                                    metricMenuExpanded,
                                onExpandedChange = {
                                    metricMenuExpanded = it
                                },
                                onSelectMetric = {
                                    selectedMetric = it
                                    selectedChartPointTs =
                                        null
                                    clearChartSelectionSignal++
                                    metricMenuExpanded = false
                                }
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        12.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Button(
                                    colors =
                                        ButtonDefaults
                                            .buttonColors(
                                                containerColor =
                                                    accentColor
                                            ),
                                    shape =
                                        RoundedCornerShape(
                                            14.dp
                                        ),
                                    onClick = {
                                        selectedChartPointTs =
                                            null
                                        clearChartSelectionSignal++
                                        showFilter = true
                                    },
                                    enabled =
                                        !state.isLoading,
                                    modifier = Modifier
                                        .weight(
                                            if (isFiltered) {
                                                1.15f
                                            } else {
                                                1f
                                            }
                                        )
                                        .height(54.dp),
                                    contentPadding =
                                        PaddingValues(
                                            horizontal =
                                                14.dp,
                                            vertical =
                                                10.dp
                                        )
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Filled
                                                .CalendarMonth,
                                        contentDescription =
                                            null,
                                        modifier =
                                            Modifier.size(
                                                18.dp
                                            )
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.width(
                                                8.dp
                                            )
                                    )

                                    Text(
                                        text =
                                            "Filtrar período",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium,
                                        maxLines = 1,
                                        overflow =
                                            TextOverflow
                                                .Ellipsis
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isFiltered,
                                    enter =
                                        expandHorizontally(
                                            expandFrom =
                                                Alignment.Start
                                        ) + fadeIn(),
                                    exit =
                                        shrinkHorizontally(
                                            shrinkTowards =
                                                Alignment.Start
                                        ) + fadeOut()
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedChartPointTs =
                                                null
                                            clearChartSelectionSignal++
                                            onBackToRealtime()
                                        },
                                        shape =
                                            RoundedCornerShape(
                                                14.dp
                                            ),
                                        enabled =
                                            !state.isLoading,
                                        border =
                                            BorderStroke(
                                                1.dp,
                                                accentColor
                                            ),
                                        modifier = Modifier
                                            .widthIn(
                                                min =
                                                    152.dp
                                            )
                                            .height(54.dp),
                                        contentPadding =
                                            PaddingValues(
                                                horizontal =
                                                    14.dp,
                                                vertical =
                                                    10.dp
                                            )
                                    ) {
                                        Icon(
                                            imageVector =
                                                Icons.Filled
                                                    .Close,
                                            contentDescription =
                                                null,
                                            tint =
                                                accentColor,
                                            modifier =
                                                Modifier.size(
                                                    18.dp
                                                )
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.width(
                                                    8.dp
                                                )
                                        )

                                        Text(
                                            text =
                                                "Limpar filtro",
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodyMedium,
                                            color =
                                                accentColor,
                                            maxLines = 1,
                                            overflow =
                                                TextOverflow
                                                    .Ellipsis
                                        )
                                    }
                                }
                            }

                            if (periodText != null) {
                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            12.dp
                                        )
                                )

                                InfoCard(
                                    title =
                                        "Período selecionado",
                                    body =
                                        periodText.removePrefix(
                                            "Período: "
                                        ),
                                    background =
                                        accentSoft,
                                    border =
                                        accentBorder,
                                    titleColor =
                                        accentText
                                )
                            }

                            selectedPointForCurrentSensor
                                ?.let { point ->
                                    Spacer(
                                        modifier =
                                            Modifier.height(
                                                8.dp
                                            )
                                    )

                                    val bodyText =
                                        if (showHumidity) {
                                            point.umidade
                                                ?.let {
                                                        umidade ->
                                                    """
                                                    Data/hora: ${
                                                        formatSelectedPointDateTime(
                                                            point.ts
                                                        )
                                                    }
                                                    Umidade: ${
                                                        "%.1f".format(
                                                            umidade
                                                        )
                                                    } %
                                                    """.trimIndent()
                                                }
                                                ?: """
                                                    Data/hora: ${
                                                    formatSelectedPointDateTime(
                                                        point.ts
                                                    )
                                                }
                                                    Umidade: -- %
                                                """.trimIndent()
                                        } else {
                                            point.temperatura
                                                ?.let {
                                                        temperatura ->
                                                    """
                                                    Data/hora: ${
                                                        formatSelectedPointDateTime(
                                                            point.ts
                                                        )
                                                    }
                                                    Temperatura: ${
                                                        "%.2f".format(
                                                            temperatura
                                                        )
                                                    } °C
                                                    """.trimIndent()
                                                }
                                                ?: """
                                                    Data/hora: ${
                                                    formatSelectedPointDateTime(
                                                        point.ts
                                                    )
                                                }
                                                    Temperatura: -- °C
                                                """.trimIndent()
                                        }

                                    InfoCard(
                                        title =
                                            "Ponto selecionado",
                                        body = bodyText,
                                        background =
                                            accentSoft,
                                        border =
                                            accentBorder,
                                        titleColor =
                                            accentText
                                    )
                                }

                            state.error?.let { error ->
                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            12.dp
                                        )
                                )

                                Text(
                                    text =
                                        "Erro ao carregar os dados:\n$error",
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .error,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(16.dp)
                            )

                            TemperatureLineChart(
                                points =
                                    state.chartPoints,
                                showHumidity =
                                    showHumidity,
                                clearSelectionSignal =
                                    clearChartSelectionSignal,
                                onPointSelected = {
                                        point ->
                                    selectedChartPointTs =
                                        point?.ts
                                }
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(16.dp)
                            )
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = state.isLoading,
                    state = pullRefreshState,
                    modifier =
                        Modifier.align(
                            Alignment.TopCenter
                        ),
                    contentColor =
                        Color(0xFF769F86)
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    background: Color,
    border: Color,
    titleColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = background
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = border
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            )
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.labelMedium,
                color = titleColor,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = body,
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )
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
            style =
                MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onExpandedChange(true)
                    }
                    .border(
                        width = 1.dp,
                        color = Color(0xFF769F86),
                        shape =
                            RoundedCornerShape(8.dp)
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text =
                        currentSensor?.displayName()
                            ?: "Selecione um sensor",
                    style =
                        MaterialTheme.typography
                            .bodyLarge
                )

                Icon(
                    imageVector =
                        if (expanded) {
                            Icons.Filled
                                .KeyboardArrowUp
                        } else {
                            Icons.Filled
                                .KeyboardArrowDown
                        },
                    contentDescription =
                        "Abrir lista de sensores"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    onExpandedChange(false)
                },
                modifier =
                    Modifier.fillMaxWidth(0.9f)
            ) {
                if (availableSensors.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Nenhum sensor cadastrado"
                            )
                        },
                        onClick = {
                            onExpandedChange(false)
                        }
                    )
                } else {
                    availableSensors.forEach {
                            sensor ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    sensor.displayName()
                                )
                            },
                            onClick = {
                                onSelectSensor(sensor)
                            }
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
            style =
                MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onExpandedChange(true)
                    }
                    .border(
                        width = 1.dp,
                        color = when (
                            selectedMetric
                        ) {
                            ChartMetric.TEMPERATURE ->
                                Color(0xFF769F86)

                            ChartMetric.HUMIDITY ->
                                Color(0xFFC9BF5A)
                        },
                        shape =
                            RoundedCornerShape(8.dp)
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (selectedMetric) {
                        ChartMetric.TEMPERATURE ->
                            "Temperatura (°C)"

                        ChartMetric.HUMIDITY ->
                            "Umidade (%)"
                    },
                    style =
                        MaterialTheme.typography
                            .bodyLarge
                )

                Icon(
                    imageVector =
                        if (expanded) {
                            Icons.Filled
                                .KeyboardArrowUp
                        } else {
                            Icons.Filled
                                .KeyboardArrowDown
                        },
                    contentDescription =
                        "Abrir opções de visualização"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    onExpandedChange(false)
                },
                modifier =
                    Modifier.fillMaxWidth(0.9f)
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Temperatura (°C)")
                    },
                    onClick = {
                        onSelectMetric(
                            ChartMetric.TEMPERATURE
                        )
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("Umidade (%)")
                    },
                    onClick = {
                        onSelectMetric(
                            ChartMetric.HUMIDITY
                        )
                    }
                )
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun TemperatureLineChart(
    points: List<TemperaturePointDto>,
    showHumidity: Boolean,
    clearSelectionSignal: Int,
    onPointSelected: (TemperaturePointDto?) -> Unit
) {
    val validPoints = remember(points, showHumidity) {
        points
            .filter { point ->
                !point.ts.isNullOrBlank() &&
                        if (showHumidity) {
                            point.umidade != null
                        } else {
                            point.temperatura != null
                        }
            }
            .sortedBy { point ->
                parseChartTimeToEpochMillis(point.ts) ?: Long.MAX_VALUE
            }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Pontos no gráfico: ${validPoints.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .border(
                    width = 2.dp,
                    color = if (showHumidity) {
                        Color(0xFFC9BF5A)
                    } else {
                        Color(0xFF769F86)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(8.dp),
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = true
                    setTouchEnabled(true)
                    setDragEnabled(true)
                    setScaleXEnabled(true)
                    setScaleYEnabled(false)
                    setPinchZoom(false)
                    isDoubleTapToZoomEnabled = true
                    isHighlightPerTapEnabled = true
                    setNoDataText("Sem dados para exibir")
                    axisRight.isEnabled = false

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        granularity = 1f
                        setDrawGridLines(true)
                        labelRotationAngle = -30f
                        textSize = 9f
                    }

                    axisLeft.apply {
                        setDrawGridLines(true)
                    }
                }
            },
            update = { chart ->
                if (validPoints.isEmpty()) {
                    chart.clear()
                    chart.setNoDataText("Sem pontos válidos para exibir")
                    chart.invalidate()
                    onPointSelected(null)
                    return@AndroidView
                }

                val entries = validPoints.mapIndexed { index, point ->
                    val value = if (showHumidity) {
                        point.umidade ?: 0.0
                    } else {
                        point.temperatura ?: 0.0
                    }

                    Entry(index.toFloat(), value.toFloat())
                }

                val lineColor = if (showHumidity) {
                    android.graphics.Color.parseColor("#C9BF5A")
                } else {
                    android.graphics.Color.parseColor("#769F86")
                }

                val dataSet = LineDataSet(
                    entries,
                    if (showHumidity) "Umidade (%)" else "Temperatura (°C)"
                ).apply {
                    axisDependency = YAxis.AxisDependency.LEFT
                    color = lineColor
                    lineWidth = 2.5f
                    mode = LineDataSet.Mode.LINEAR
                    setDrawCircles(entries.size <= 60)
                    circleRadius = 2.5f
                    setCircleColor(lineColor)
                    setDrawValues(false)
                    setDrawFilled(false)
                    highLightColor = android.graphics.Color.RED
                    setDrawHorizontalHighlightIndicator(false)
                    setDrawVerticalHighlightIndicator(true)
                }

                chart.data = LineData(dataSet)

                chart.xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.roundToInt()
                        return validPoints.getOrNull(index)?.ts
                            ?.let(::formatChartTimeSeparated)
                            .orEmpty()
                    }
                }

                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum =
                    if (validPoints.size == 1) 1f else (validPoints.size - 1).toFloat()
                chart.xAxis.setLabelCount(
                    calculateLabelCountForFullRange(validPoints),
                    false
                )

                val values = entries.map { it.y }
                val min = values.minOrNull() ?: 0f
                val max = values.maxOrNull() ?: 1f
                val padding = ((max - min) * 0.1f).coerceAtLeast(0.5f)
                chart.axisLeft.axisMinimum = min - padding
                chart.axisLeft.axisMaximum = max + padding

                chart.setOnChartValueSelectedListener(
                    object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                        override fun onValueSelected(
                            e: Entry?,
                            h: com.github.mikephil.charting.highlight.Highlight?
                        ) {
                            val index = e?.x?.roundToInt()
                            onPointSelected(index?.let(validPoints::getOrNull))
                        }

                        override fun onNothingSelected() {
                            onPointSelected(null)
                        }
                    }
                )

                if (chart.tag != clearSelectionSignal) {
                    chart.highlightValues(null)
                    chart.tag = clearSelectionSignal
                }

                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.fitScreen()

                if (validPoints.size > 120) {
                    chart.setVisibleXRangeMaximum(120f)
                    chart.moveViewToX((validPoints.size - 1).toFloat())
                }

                chart.invalidate()
            }
        )
    }
}

@Composable
private fun DoorStateChart(
    points: List<TemperaturePointDto>,
    clearSelectionSignal: Int
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(
                horizontal = 4.dp,
                vertical = 1.dp
            )
            .border(
                width = 2.dp,
                color = Color(0xFF607D8B),
                shape =
                    RoundedCornerShape(16.dp)
            )
            .padding(top = 8.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true
                setTouchEnabled(true)
                setDragEnabled(true)
                setScaleEnabled(true)
                setScaleXEnabled(true)
                setScaleYEnabled(false)
                setPinchZoom(false)
                isDoubleTapToZoomEnabled = true
                setNoDataText(
                    "Sem dados de porta para exibir"
                )

                extraBottomOffset = 14f
                minOffset = 12f

                axisLeft.apply {
                    axisMinimum = -0.1f
                    axisMaximum = 1.1f
                    granularity = 1f
                    setLabelCount(2, true)
                    setDrawGridLines(true)

                    valueFormatter =
                        object : ValueFormatter() {
                            override fun getFormattedValue(
                                value: Float
                            ): String {
                                return when (
                                    value.roundToInt()
                                ) {
                                    0 -> "Fechada"
                                    1 -> "Aberta"
                                    else -> ""
                                }
                            }
                        }
                }

                axisRight.isEnabled = false

                xAxis.apply {
                    position =
                        XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    granularity = 1f
                    setLabelCount(4, true)
                    labelRotationAngle = -30f
                    textSize = 9f
                    yOffset = 8f
                    setAvoidFirstLastClipping(true)
                }

                val dataSet = LineDataSet(
                    emptyList(),
                    "Porta"
                ).apply {
                    axisDependency =
                        YAxis.AxisDependency.LEFT
                    mode =
                        LineDataSet.Mode.STEPPED
                    setDrawCircles(false)
                    setDrawValues(false)
                    lineWidth = 2f
                    color =
                        android.graphics.Color
                            .parseColor("#607D8B")
                    highLightColor =
                        android.graphics.Color.RED
                    setDrawHorizontalHighlightIndicator(
                        false
                    )
                    setDrawVerticalHighlightIndicator(
                        true
                    )
                }

                data = LineData(dataSet)
            }
        },
        update = { chart ->
            val data =
                chart.data
                    ?: return@AndroidView

            val dataSet =
                data.getDataSetByIndex(0)
                        as? LineDataSet
                    ?: return@AndroidView

            val sortedPoints = points
                .filter { point ->
                    !point.ts.isNullOrBlank()
                }
                .sortedBy { point ->
                    parseChartTimeToEpochMillis(
                        point.ts
                    ) ?: Long.MAX_VALUE
                }

            val entries =
                sortedPoints.mapIndexedNotNull {
                        index,
                        point ->

                    point.porta?.let { porta ->
                        Entry(
                            index.toFloat(),
                            if (porta.toInt() == 1) {
                                1f
                            } else {
                                0f
                            }
                        )
                    }
                }

            dataSet.values = entries

            chart.xAxis.valueFormatter =
                object : ValueFormatter() {
                    override fun getFormattedValue(
                        value: Float
                    ): String {
                        val index =
                            value.roundToInt()

                        return sortedPoints
                            .getOrNull(index)
                            ?.ts
                            ?.let {
                                formatChartTimeSeparated(
                                    it
                                )
                            }
                            ?: ""
                    }
                }

            val previousClearSignal =
                chart.tag as? Int

            if (
                previousClearSignal !=
                clearSelectionSignal
            ) {
                chart.highlightValues(null)
                chart.highlightValue(null)
                chart.tag =
                    clearSelectionSignal
            }

            if (entries.isNotEmpty()) {
                chart.xAxis.axisMinimum = 0f

                chart.xAxis.axisMaximum =
                    if (sortedPoints.size == 1) {
                        1f
                    } else {
                        (
                                sortedPoints.size - 1
                                ).toFloat()
                    }

                chart.xAxis.setLabelCount(
                    calculateLabelCountForFullRange(
                        sortedPoints
                    ),
                    true
                )

                if (entries.size > 300) {
                    chart.setVisibleXRangeMaximum(
                        300f
                    )
                }

                chart.moveViewToX(
                    (
                            sortedPoints.size - 1
                            ).toFloat()
                )
            } else {
                dataSet.values = emptyList()
                chart.xAxis.valueFormatter = null
            }

            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )
}

private fun calculateLabelCountForFullRange(
    points: List<TemperaturePointDto>
): Int {
    return when {
        points.size <= 20 -> 3
        points.size <= 60 -> 4
        points.size <= 180 -> 4
        points.size <= 720 -> 5
        else -> 5
    }
}

private fun millisToIsoSaoPaulo(
    millis: Long
): String {
    val zone =
        ZoneId.of("America/Sao_Paulo")

    val offsetDateTime =
        Instant
            .ofEpochMilli(millis)
            .atZone(zone)
            .toOffsetDateTime()

    return DateTimeFormatter
        .ISO_OFFSET_DATE_TIME
        .format(offsetDateTime)
}

private fun formatPeriodPtBr(
    startIso: String?,
    stopIso: String?
): String? {
    if (
        startIso.isNullOrBlank() ||
        stopIso.isNullOrBlank()
    ) {
        return null
    }

    val zone =
        ZoneId.of("America/Sao_Paulo")

    val formatter =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy 'às' HH:mm",
            Locale("pt", "BR")
        )

    return try {
        val start =
            OffsetDateTime
                .parse(startIso)
                .atZoneSameInstant(zone)

        val stop =
            OffsetDateTime
                .parse(stopIso)
                .atZoneSameInstant(zone)

        "Período: ${
            start.format(formatter)
        } até ${
            stop.format(formatter)
        }"
    } catch (_: Exception) {
        try {
            val start =
                Instant
                    .parse(startIso)
                    .atZone(zone)

            val stop =
                Instant
                    .parse(stopIso)
                    .atZone(zone)

            "Período: ${
                start.format(formatter)
            } até ${
                stop.format(formatter)
            }"
        } catch (_: Exception) {
            "Período: $startIso até $stopIso"
        }
    }
}

private fun parseChartTimeToEpochMillis(
    timestamp: String?
): Long? {
    if (timestamp.isNullOrBlank()) {
        return null
    }

    return try {
        OffsetDateTime
            .parse(timestamp)
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        try {
            Instant
                .parse(timestamp)
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}

private fun formatChartTimeSeparated(
    timestamp: String?
): String {
    if (timestamp.isNullOrBlank()) {
        return ""
    }

    val zone =
        ZoneId.of("America/Sao_Paulo")

    val formatter =
        DateTimeFormatter.ofPattern(
            "dd/MM  •  HH:mm"
        )

    return try {
        OffsetDateTime
            .parse(timestamp)
            .atZoneSameInstant(zone)
            .format(formatter)
    } catch (_: Exception) {
        try {
            Instant
                .parse(timestamp)
                .atZone(zone)
                .format(formatter)
        } catch (_: Exception) {
            ""
        }
    }
}

private fun formatSelectedPointDateTime(
    timestamp: String?
): String {
    if (timestamp.isNullOrBlank()) {
        return "--"
    }

    val zone =
        ZoneId.of("America/Sao_Paulo")

    val formatter =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy 'às' HH:mm",
            Locale("pt", "BR")
        )

    return try {
        OffsetDateTime
            .parse(timestamp)
            .atZoneSameInstant(zone)
            .format(formatter)
    } catch (_: Exception) {
        try {
            Instant
                .parse(timestamp)
                .atZone(zone)
                .format(formatter)
        } catch (_: Exception) {
            timestamp
        }
    }
}