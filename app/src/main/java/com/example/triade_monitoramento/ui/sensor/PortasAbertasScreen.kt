package com.example.triade_monitoramento.ui.sensor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class PortaAbertaItem(
    val sensorId: String,
    val sensorNome: String,
    val local: String,
    val dataHora: String,
    val status: String = "Porta aberta",
    val openedAt: String? = dataHora,
    val closedAt: String? = null,
    val durationSeconds: Double = 0.0,
    val nivel: String = "normal"
)

private enum class PeriodoFiltro(val titulo: String) {
    HOJE("Hoje"),
    SETE_DIAS("7 dias"),
    TRINTA_DIAS("30 dias"),
    TODOS("Todos")
}

private enum class OrdenacaoFiltro(val titulo: String) {
    MAIS_RECENTE("Mais recente"),
    MAIOR_DURACAO("Maior duração"),
    APENAS_VERMELHOS("Só vermelhos"),
    APENAS_AMARELOS("Só amarelos")
}

private val Amarelo = Color(0xFFFFB300)
private val Vermelho = Color(0xFFE53935)
private val Azul = Color(0xFF355DAB)
private val Fundo = Color(0xFFF5F7FA)
private val TextoSecundario = Color(0xFF5F6368)

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material.ExperimentalMaterialApi::class
)
@Composable
fun PortasAbertasScreen(
    sensorNome: String,
    amarelos: Int,
    vermelhos: Int,
    yellowAfterSeconds: Int,
    redAfterSeconds: Int,
    portasAbertas: List<PortaAbertaItem>,
    onConfigurarTempos: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Job?
) {
    var refreshing by remember { mutableStateOf(false) }
    var periodoSelecionado by remember { mutableStateOf(PeriodoFiltro.TRINTA_DIAS) }
    var ordenacaoSelecionada by remember { mutableStateOf(OrdenacaoFiltro.MAIS_RECENTE) }

    val eventosFiltrados = remember(portasAbertas, periodoSelecionado, ordenacaoSelecionada) {
        portasAbertas
            .filtrarPorPeriodo(periodoSelecionado)
            .filtrarPorNivel(ordenacaoSelecionada)
            .ordenarEventos(ordenacaoSelecionada)
    }

    val totalEventos = eventosFiltrados.size
    val totalAmarelos = eventosFiltrados.count { it.nivel.equals("amarelo", ignoreCase = true) }
    val totalVermelhos = eventosFiltrados.count { it.nivel.equals("vermelho", ignoreCase = true) }
    val totalAbertoSeconds = eventosFiltrados.sumOf { it.durationSeconds }
    val maiorAberturaSeconds = eventosFiltrados.maxOfOrNull { it.durationSeconds } ?: 0
    val mediaAberturaSeconds = if (eventosFiltrados.isNotEmpty()) {
        eventosFiltrados.map { it.durationSeconds }.average()
    } else {
        0.0
    }

    val eventosPorDia = eventosFiltrados.groupBy { item ->
        item.openedAt.toDataAgrupamento()
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            val job = onRefresh()
            if (job == null) {
                refreshing = false
            } else {
                job.invokeOnCompletion { refreshing = false }
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Relatório de portas abertas",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Voltar"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Fundo)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = sensorNome.ifBlank { "Sensor" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Resumo das ocorrências por tempo de abertura da porta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }

                item {
                    FiltrosPeriodo(
                        selecionado = periodoSelecionado,
                        onSelecionar = { periodoSelecionado = it }
                    )
                }

                item {
                    FiltrosOrdenacao(
                        selecionado = ordenacaoSelecionada,
                        onSelecionar = { ordenacaoSelecionada = it }
                    )
                }

                item {
                    ResumoGeralCard(
                        totalEventos = totalEventos,
                        totalAmarelos = totalAmarelos,
                        totalVermelhos = totalVermelhos,
                        totalAbertoSeconds = totalAbertoSeconds.toDouble(),
                        mediaAberturaSeconds = mediaAberturaSeconds,
                        maiorAberturaSeconds = maiorAberturaSeconds.toDouble()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ResumoAlertaCard(
                            modifier = Modifier.weight(1f),
                            titulo = "Amarelos",
                            total = totalAmarelos,
                            descricao = "≥ $yellowAfterSeconds seg",
                            cor = Amarelo
                        )

                        ResumoAlertaCard(
                            modifier = Modifier.weight(1f),
                            titulo = "Vermelhos",
                            total = totalVermelhos,
                            descricao = "≥ $redAfterSeconds seg",
                            cor = Vermelho
                        )
                    }
                }

                item {
                    ConfiguracaoAtualCard(
                        yellowAfterSeconds = yellowAfterSeconds,
                        redAfterSeconds = redAfterSeconds,
                        onConfigurarTempos = onConfigurarTempos
                    )
                }

                item {
                    Text(
                        text = "Ocorrências",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (eventosFiltrados.isEmpty()) {
                    item { EstadoVazioCard() }
                } else {
                    eventosPorDia.forEach { (dia, eventos) ->
                        item {
                            Text(
                                text = dia,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextoSecundario,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(eventos) { item ->
                            PortaAbertaCard(item = item)
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun FiltrosPeriodo(
    selecionado: PeriodoFiltro,
    onSelecionar: (PeriodoFiltro) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = Azul,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Período", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodoFiltro.values().forEach { periodo ->
                ElevatedAssistChip(
                    onClick = { onSelecionar(periodo) },
                    label = { Text(periodo.titulo) },
                    colors = AssistChipDefaults.elevatedAssistChipColors(
                        containerColor = if (selecionado == periodo) Azul else Color.White,
                        labelColor = if (selecionado == periodo) Color.White else Color.DarkGray
                    )
                )
            }
        }
    }
}

@Composable
private fun FiltrosOrdenacao(
    selecionado: OrdenacaoFiltro,
    onSelecionar: (OrdenacaoFiltro) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = Azul,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Ordenação e tipo", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrdenacaoFiltro.values().forEach { ordenacao ->
                ElevatedAssistChip(
                    onClick = { onSelecionar(ordenacao) },
                    label = { Text(ordenacao.titulo) },
                    colors = AssistChipDefaults.elevatedAssistChipColors(
                        containerColor = if (selecionado == ordenacao) Azul else Color.White,
                        labelColor = if (selecionado == ordenacao) Color.White else Color.DarkGray
                    )
                )
            }
        }
    }
}

@Composable
private fun ResumoGeralCard(
    totalEventos: Int,
    totalAmarelos: Int,
    totalVermelhos: Int,
    totalAbertoSeconds: Double,
    mediaAberturaSeconds: Double,
    maiorAberturaSeconds: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Resumo do período",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinhaResumo("Total de ocorrências", totalEventos.toString())
            LinhaResumo("Amarelos", totalAmarelos.toString())
            LinhaResumo("Vermelhos", totalVermelhos.toString())
            LinhaResumo("Tempo total aberta", formatarDuracao(totalAbertoSeconds))
            LinhaResumo("Tempo médio", formatarDuracao(mediaAberturaSeconds))
            LinhaResumo("Maior abertura", formatarDuracao(maiorAberturaSeconds))
        }
    }
}

@Composable
private fun LinhaResumo(label: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextoSecundario)
        Text(text = valor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ResumoAlertaCard(
    modifier: Modifier = Modifier,
    titulo: String,
    total: Int,
    descricao: String,
    cor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cor
            )

            Text(text = titulo, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = descricao,
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }
    }
}

@Composable
private fun ConfiguracaoAtualCard(
    yellowAfterSeconds: Int,
    redAfterSeconds: Int,
    onConfigurarTempos: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Configuração atual", fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Amarelo: porta aberta por $yellowAfterSeconds segundo(s)",
                color = TextoSecundario
            )

            Text(
                text = "Vermelho: porta aberta por $redAfterSeconds segundo(s)",
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onConfigurarTempos,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configurar tempos de alerta")
            }
        }
    }
}

@Composable
private fun EstadoVazioCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Nenhuma ocorrência encontrada", fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Não existem ocorrências para os filtros selecionados.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun PortaAbertaCard(item: PortaAbertaItem) {
    val nivel = item.nivel.lowercase()
    val isVermelho = nivel == "vermelho"
    val isAmarelo = nivel == "amarelo"

    val cor = when {
        isVermelho -> Vermelho
        isAmarelo -> Amarelo
        else -> Azul
    }

    val tituloStatus = when {
        isVermelho -> "Alerta vermelho"
        isAmarelo -> "Alerta amarelo"
        else -> "Ocorrência normal"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = cor,
                modifier = Modifier.size(34.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.sensorNome.ifBlank { item.sensorId },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Aberta: ${formatarDataHora(item.openedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )

                Text(
                    text = "Fechada: ${formatarDataHora(item.closedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )

                Text(
                    text = "Duração: ${formatarDuracao(item.durationSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                AssistChip(
                    onClick = {},
                    label = { Text(tituloStatus) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = cor)
                )
            }
        }
    }
}

private fun List<PortaAbertaItem>.filtrarPorPeriodo(periodo: PeriodoFiltro): List<PortaAbertaItem> {
    if (periodo == PeriodoFiltro.TODOS) return this

    val hoje = LocalDate.now()
    val dataMinima = when (periodo) {
        PeriodoFiltro.HOJE -> hoje
        PeriodoFiltro.SETE_DIAS -> hoje.minusDays(6)
        PeriodoFiltro.TRINTA_DIAS -> hoje.minusDays(29)
        PeriodoFiltro.TODOS -> LocalDate.MIN
    }

    return filter { item ->
        val data = item.openedAt.toLocalDateOrNull()
        data == null || !data.isBefore(dataMinima)
    }
}

private fun List<PortaAbertaItem>.filtrarPorNivel(ordenacao: OrdenacaoFiltro): List<PortaAbertaItem> {
    return when (ordenacao) {
        OrdenacaoFiltro.APENAS_VERMELHOS -> filter {
            it.nivel.equals("vermelho", ignoreCase = true)
        }

        OrdenacaoFiltro.APENAS_AMARELOS -> filter {
            it.nivel.equals("amarelo", ignoreCase = true)
        }

        else -> this
    }
}

private fun List<PortaAbertaItem>.ordenarEventos(ordenacao: OrdenacaoFiltro): List<PortaAbertaItem> {
    return when (ordenacao) {
        OrdenacaoFiltro.MAIOR_DURACAO -> sortedByDescending { it.durationSeconds }
        else -> sortedByDescending { it.openedAt.toLocalDateTimeOrNull() }
    }
}

private fun String?.toDataAgrupamento(): String {
    val data = this.toLocalDateOrNull() ?: return "Sem data"
    val hoje = LocalDate.now()

    return when (data) {
        hoje -> "Hoje"
        hoje.minusDays(1) -> "Ontem"
        else -> data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
}

private fun formatarDataHora(valor: String?): String {
    if (valor.isNullOrBlank()) return "--"

    val dataHora = valor.toLocalDateTimeOrNull() ?: return valor
    return dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}

private fun formatarDuracao(segundosDouble: Double): String {
    val segundosTotais = segundosDouble.toInt()
    val horas = segundosTotais / 3600
    val minutos = (segundosTotais % 3600) / 60
    val segundos = segundosTotais % 60

    return when {
        horas > 0 && minutos > 0 -> "${horas}h ${minutos}min"
        horas > 0 -> "${horas}h"
        minutos > 0 && segundos > 0 -> "${minutos}min ${segundos}s"
        minutos > 0 -> "${minutos}min"
        else -> "${segundos}s"
    }
}

private fun formatarDuracao(segundos: Int): String = formatarDuracao(segundos.toDouble())

private fun String?.toLocalDateOrNull(): LocalDate? {
    return this.toLocalDateTimeOrNull()?.toLocalDate()
}

private fun String?.toLocalDateTimeOrNull(): LocalDateTime? {
    if (this.isNullOrBlank()) return null

    val texto = this.trim()

    return try {
        Instant.parse(texto).atZone(ZoneId.systemDefault()).toLocalDateTime()
    } catch (_: Exception) {
        tentarParseLocalDateTime(texto)
    }
}

private fun tentarParseLocalDateTime(texto: String): LocalDateTime? {
    val formatos = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    )

    formatos.forEach { formato ->
        try {
            return LocalDateTime.parse(texto.replace("T", " "), formato)
        } catch (_: DateTimeParseException) {
            // tenta o próximo formato
        }
    }

    return null
}