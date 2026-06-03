package com.example.triade_monitoramento.ui.sensor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.triade_monitoramento.ui.navigation.ScreenContainer

private val TriadeGreen = Color(0xFF769F86)
private val TriadeBorder = Color(0xFF8AA796)
private val TextDark = Color(0xFF1F1F1F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorConfigScreen(
    sensorId: String,
    nomeInicial: String,
    tempMaxInicial: Double?,
    tempMinInicial: Double?,
    onBack: () -> Unit,
    onGerenciarContatos: () -> Unit,

    // NOVO: função que será chamada ao clicar no botão de relatório de portas abertas
    onRelatorioPortas: () -> Unit,

    onGerenciarAlarmes: () -> Unit,
    onSensorExcluido: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Área") },
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
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = nomeInicial.ifBlank { sensorId },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ID: $sensorId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SensorConfigMenuButton(
                    title = "Gerenciar contatos de alerta",
                    subtitle = "WhatsApp e e-mail para notificações.",
                    icon = Icons.Default.Contacts,
                    onClick = onGerenciarContatos
                )

                Spacer(modifier = Modifier.height(12.dp))

                // NOVO: botão que abre a tela de relatório de portas abertas
                SensorConfigMenuButton(
                    title = "Relatório de portas abertas",
                    subtitle = "Aberturas curtas, normais, longas e total.",
                    icon = Icons.Default.DoorFront,
                    onClick = onRelatorioPortas
                )

                Spacer(modifier = Modifier.height(12.dp))

                SensorConfigMenuButton(
                    title = "Gerenciar alarmes",
                    subtitle = buildString {
                        append("Nome, temperatura mínima e máxima. ")
                        append("Atual: ")
                        append(tempMinInicial?.let { "%.1f".format(it) } ?: "--")
                        append(" °C até ")
                        append(tempMaxInicial?.let { "%.1f".format(it) } ?: "--")
                        append(" °C")
                    },
                    icon = Icons.Default.Alarm,
                    onClick = onGerenciarAlarmes
                )
            }
        }
    }
}

@Composable
private fun SensorConfigMenuButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, TriadeBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = TextDark
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TriadeGreen,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.68f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TriadeGreen
            )
        }
    }
}