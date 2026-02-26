package com.tomst.lolly.ui.graph

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Přidali jsme parametr do konstruktoru
class DateAxisValueFormatter(private val timestamps: List<Long>) : ValueFormatter() {

    // Formát, jaký chceš vidět na obrazovce
    private val dateFormat = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault())

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        // Graf nám dává hodnotu 'x' z tvého Entry (což je fIdx, tedy 0, 1, 2, 3...)
        val index = value.toInt()

        // Zkontrolujeme, jestli takový index v našem poli časů existuje
        if (index >= 0 && index < timestamps.size) {
            val millis = timestamps[index]
            if (millis > 0L) {
                return dateFormat.format(Date(millis))
            }
        }
        return "" // Vrátí prázdný string, pokud na této pozici není čas
    }
}