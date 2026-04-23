package com.example.triade_monitoramento.ui.components

import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.example.triade_monitoramento.R
import com.example.triade_monitoramento.data.model.TemperaturePointDto
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChartMarkerView(
    context: Context,
    layoutResource: Int,
    private val points: List<TemperaturePointDto>,
    private val showHumidity: Boolean
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.tvMarkerDate)
    private val tvValue: TextView = findViewById(R.id.tvMarkerValue)

    var isVisibleMarker: Boolean = false

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        val index = e.x.toInt()
        if (index in points.indices) {
            val point = points[index]

            tvDate.text = "Data: ${formatMarkerDateTime(point.ts)}"
            tvValue.text = if (showHumidity) {
                "Valor: %.1f %%".format(point.umidade)
            } else {
                "Valor: %.2f °C".format(point.temperatura)
            }
        }

        super.refreshContent(e, highlight)
    }

    override fun draw(canvas: Canvas, posX: Float, posY: Float) {
        if (!isVisibleMarker) return
        super.draw(canvas, posX, posY)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }

    private fun formatMarkerDateTime(ts: String?): String {
        if (ts.isNullOrBlank()) return "--"

        val zone = ZoneId.of("America/Sao_Paulo")

        return try {
            OffsetDateTime.parse(ts)
                .atZoneSameInstant(zone)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } catch (_: Exception) {
            try {
                Instant.parse(ts)
                    .atZone(zone)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            } catch (_: Exception) {
                ts
            }
        }
    }
}