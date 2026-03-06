package com.example.triade_monitoramento

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.Layout.Alignment.ALIGN_CENTER
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.apply

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltroPeriodoScreen(
    background: Color = Color.White,
    titulo: String = "Filtrar historico",
    style: TextStyle = MaterialTheme.typography.titleMedium,
    initialStartMillis: Long = System.currentTimeMillis() - 60L * 60L * 1000L,
    initialEndMillis: Long = System.currentTimeMillis(),
    onBack: () -> Unit,
    onApply: (startMillis: Long, endMillis: Long) -> Unit
) {
    val ctx = LocalContext.current
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    var startMillis by remember { mutableLongStateOf(initialStartMillis) }
    var endMillis by remember { mutableLongStateOf(initialEndMillis) }

    val startCal = remember(startMillis) { Calendar.getInstance().apply { timeInMillis = startMillis } }
    val endCal = remember(endMillis) { Calendar.getInstance().apply { timeInMillis = endMillis } }

    val isValidRange = endMillis >= startMillis

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(40
                                .dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = titulo,
                            style = style,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Selecione o período de interesse \nData e Hora",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Card(Modifier.fillMaxWidth(),colors = CardDefaults.cardColors(
                colorResource(id = R.color.gray_200),
            )) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PeriodField(
                        label = "Início",
                        color = Color(0xFF769F86),
                        value = fmt.format(startCal.time),
                        onPickDateTime = {
                            pickDateTime(ctx, startCal) { selected ->
                                startMillis = selected
                                if (endMillis < startMillis) endMillis = startMillis
                            }
                        }
                    )

                    PeriodField(
                        label = "Fim",
                        color = Color(0xFF769F86),
                        value = fmt.format(endCal.time),
                        onPickDateTime = {
                            pickDateTime(ctx, endCal) { selected ->
                                endMillis = selected
                            }
                        }
                    )

                    if (!isValidRange) {
                        Text(
                            "Período invalido: o fim precisa ser maior ou igual ao inicio.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9BF5A)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    onClick = onBack
                ) {
                    Text("Cancelar")
                }

                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF769F86)),
                    modifier = Modifier.weight(1f),
                    enabled = isValidRange,
                    shape = RoundedCornerShape(8.dp),
                    onClick = { onApply(startMillis, endMillis) }
                ) {
                    Text("Aplicar")
                }
            }
        }
    }
}

@Composable
private fun PeriodField(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary,
    onPickDateTime: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color
        )

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onPickDateTime,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = color

            ),
            border = BorderStroke(1.dp, color)
        ) {
            Text(text = value,
                color = color)
        }
    }
}

private fun pickDateTime(
    ctx: Context,
    initial: Calendar,
    onSelected: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = initial.timeInMillis }

    DatePickerDialog(
        ctx,
        { _, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)

            TimePickerDialog(
                ctx,
                { _, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    onSelected(cal.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
