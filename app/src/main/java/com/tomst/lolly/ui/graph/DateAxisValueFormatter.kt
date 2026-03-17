package com.tomst.lolly.ui.graph

import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateAxisValueFormatter(
    private val timestamps: List<Long>,
    private val chart: CombinedChart
) : ValueFormatter() {

    private val zoomOutFormat = SimpleDateFormat("dd/MM\nyyyy", Locale.getDefault())
    private val zoomInFormat = SimpleDateFormat("dd/MM\nyyyy\nHH:mm", Locale.getDefault())

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val index = value.toInt()

        if (index >= 0 && index < timestamps.size) {
            val millis = timestamps[index]
            if (millis > 0L) {
                // Phase 5: Zoom-Aware Dynamic Date Formatter
                val format = if (chart.visibleXRange > 500) {
                    zoomOutFormat
                } else {
                    zoomInFormat
                }
                return format.format(Date(millis))
            }
        }
        return ""
    }
}
