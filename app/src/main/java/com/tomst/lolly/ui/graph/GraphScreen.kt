package com.tomst.lolly.ui.graph

import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.SavedStateHandle
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.tomst.lolly.core.DmdViewModel
import com.tomst.lolly.core.TPhysValue
import java.util.ArrayList
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    dmdViewModel: DmdViewModel
) {
    val state by viewModel.uiState.collectAsState()

    GraphScreenContent(
        state = state,
        dmdData = dmdViewModel,
        onToggleLine = { tag, checked -> viewModel.toggleLineVisibility(tag, checked) }
    )
}

@Composable
fun GraphScreenContent(
    state: GraphUiState,
    dmdData: DmdViewModel,
    onToggleLine: (Int, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (state.maxProgress > 0) {
                LinearProgressIndicator(
                    progress = { state.progress.toFloat() / state.maxProgress.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
            }
        }

        // Zmenšený padding kolem checkboxů
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GraphCheckbox("T1:", state.showT1) { onToggleLine(1, it) }
            GraphCheckbox("T2:", state.showT2) { onToggleLine(2, it) }
            GraphCheckbox("T3:", state.showT3) { onToggleLine(3, it) }
            GraphCheckbox("Soil", state.showGrowth) { onToggleLine(4, it) }
        }



        // Zmenšený prostor pod checkboxy
        Spacer(modifier = Modifier.height(2.dp))

        var visibleTimeRange by remember { mutableStateOf("") }
        // --- NOVÉ: Stav pro zobrazení Min/Max ---
        var visibleStats by remember { mutableStateOf("") }
        var selectedPointInfo by remember { mutableStateOf("") } // Stav pro uložený klik
        val dateFormat = remember { java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()) }

        val updateTimeRange: (CombinedChart) -> Unit = { chart ->
            if (dmdData.timestamps.isNotEmpty()) {
                val lowX = chart.lowestVisibleX.toInt().coerceAtLeast(0)
                val highX = chart.highestVisibleX.toInt().coerceAtMost(dmdData.timestamps.size - 1)

                if (lowX <= highX && lowX < dmdData.timestamps.size) {
                    val start = dateFormat.format(java.util.Date(dmdData.timestamps[lowX]))
                    val end = dateFormat.format(java.util.Date(dmdData.timestamps[highX]))
                    val newRange = "$start  –  $end"

                    if (visibleTimeRange != newRange) {
                        visibleTimeRange = newRange
                    }
                }

                val statsBuilder = StringBuilder()

               fun appendStats(name: String, data: ArrayList<Entry>, isVisible: Boolean) {
                    if (isVisible && data.isNotEmpty()) {
                        val startIdx = lowX.coerceAtMost(data.size - 1)
                        val endIdx = highX.coerceAtMost(data.size - 1)
                        if (startIdx <= endIdx) {
                            var min = Float.MAX_VALUE
                            var max = -Float.MAX_VALUE
                            for (i in startIdx..endIdx) {
                                val y = data[i].y
                                if (y < min) min = y
                                if (y > max) max = y
                            }
                            if (min != Float.MAX_VALUE && max != -Float.MAX_VALUE) {
                                // Zformátujeme čísla na 1 desetinné místo a přidáme šipky ↓ a ↑
                                val minStr = String.format(java.util.Locale.US, "%.1f", min)
                                val maxStr = String.format(java.util.Locale.US, "%.1f", max)
                                statsBuilder.append("$name: ↓$minStr  ↑$maxStr    ")
                            }
                        }
                    }
                }
            }
        }

        if (visibleTimeRange.isNotEmpty()) {
            Text(
                text = visibleTimeRange,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color.DarkGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp), // Zmenšený padding pod časovým rozmezím
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // --- NOVÉ: Vykreslení statistik Min / Max ---
        if (visibleStats.isNotEmpty()) {
            Text(
                text = visibleStats,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = androidx.compose.ui.graphics.Color.Gray, // Světlejší šedá, aby to nekřičelo
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        val lastUpdateHash = remember { IntArray(1) { 0 } }
        val currentHash = listOf(
            state.showT1,
            state.showT2,
            state.showT3,
            state.showGrowth,
            state.dataTimestamp,
            state.isHighlightingEnabled // Nutné hlídat změnu pro překreslení chování grafu
        ).hashCode()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    CombinedChart(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        description.isEnabled = false
                        setTouchEnabled(true)
                        dragDecelerationFrictionCoef = 0.9f
                        isDragEnabled = true
                        setScaleEnabled(true)
                        setDrawGridBackground(false)
                        setViewPortOffsets(0f, 0f, 0f, 0f)
                        setPinchZoom(false)

                        // --- NOVÉ: Vlastní zaměřovač (Highlighter) se zámkem ---
                        val customHighlighter = object : com.github.mikephil.charting.highlight.CombinedHighlighter(this, this) {
                            var lockedDataSetIndex = -1 // Zde si držíme index vybrané čáry

                            override fun getHighlight(x: Float, y: Float): com.github.mikephil.charting.highlight.Highlight? {
                                val closest = super.getHighlight(x, y) ?: return null

                                if (lockedDataSetIndex == -1) {
                                    // 1. První dotyk prstem: zamkneme tu čáru, na kterou uživatel sáhl
                                    lockedDataSetIndex = closest.dataSetIndex
                                    return closest
                                } else {
                                    // 2. Tažení prstem: pokud se křivky překříží, graf se snaží skočit na jinou.
                                    // My mu ale vnutíme bod z naší zamknuté křivky na aktuální pozici prstu (X).
                                    if (closest.dataSetIndex == lockedDataSetIndex) {
                                        return closest
                                    } else {
                                        val lockedDataSet = this@apply.data?.getDataSetByIndex(lockedDataSetIndex)
                                        val entries = lockedDataSet?.getEntriesForXValue(closest.x)

                                        if (!entries.isNullOrEmpty()) {
                                            val entry = entries.first()
                                            // Dopočítáme fyzické pixely na displeji pro náš vnucený bod
                                            val pixels = this@apply.getTransformer(lockedDataSet.axisDependency)
                                                .getPixelForValues(entry.x, entry.y)

                                            return com.github.mikephil.charting.highlight.Highlight(
                                                entry.x, entry.y,
                                                pixels.x.toFloat(), pixels.y.toFloat(),
                                                lockedDataSetIndex,
                                                lockedDataSet.axisDependency
                                            )
                                        }
                                        return closest // Záloha
                                    }
                                }
                            }
                        }


                        setOnChartGestureListener(object : com.github.mikephil.charting.listener.OnChartGestureListener {
                            override fun onChartScale(me: android.view.MotionEvent?, scaleX: Float, scaleY: Float) {
                                updateTimeRange(this@apply)
                            }
                            override fun onChartTranslate(me: android.view.MotionEvent?, dX: Float, dY: Float) {
                                updateTimeRange(this@apply)
                            }
                            override fun onChartGestureEnd(me: android.view.MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {
                                updateTimeRange(this@apply)
                                customHighlighter.lockedDataSetIndex = -1
                            }

                            override fun onChartGestureStart(me: android.view.MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                            override fun onChartLongPressed(me: android.view.MotionEvent?) {}
                            override fun onChartDoubleTapped(me: android.view.MotionEvent?) {}
                            override fun onChartSingleTapped(me: android.view.MotionEvent?) {}
                            override fun onChartFling(me1: android.view.MotionEvent?, me2: android.view.MotionEvent?, velocityX: Float, velocityY: Float) {}
                        })
                        setHighlighter(customHighlighter) // Nasadíme náš nový zaměřovač


                        // Detekce kliknutí na bod v grafu
                        setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                                if (e != null && h != null) {
                                    val index = e.x.toInt()
                                    val timeString = if (index in dmdData.timestamps.indices) {
                                        val millis = dmdData.timestamps[index]
                                        if (millis > 0L) {
                                            java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(millis))
                                        } else ""
                                    } else ""

                                    val dataSetLabel = this@apply.data?.getDataSetByIndex(h.dataSetIndex)?.label ?: "Hodnota"
                                    val roundedValue = String.format(java.util.Locale.US, "%.2f", e.y)
                                    selectedPointInfo = "$timeString\n$dataSetLabel: $roundedValue"
                                }
                            }

                            override fun onNothingSelected() {
                                selectedPointInfo = ""
                            }
                        })

                        legend.apply {
                            form = Legend.LegendForm.LINE
                            textSize = 11f
                            textColor = Color.BLACK
                            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                            horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                            orientation = Legend.LegendOrientation.HORIZONTAL
                            setDrawInside(true)
                        }

                        axisRight.apply {
                            setDrawGridLines(true)
                            setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                        }
                        axisLeft.apply {
                            setDrawGridLines(false)
                            setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                        }
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM_INSIDE
                            labelRotationAngle = -90f
                            textColor = Color.BLACK
                            yOffset = 70f
                            setLabelCount(10, false)
                            textSize = 10f
                            setDrawAxisLine(true)
                            setDrawGridLines(true)
                            setCenterAxisLabels(false)
                            granularity = 1f
                            valueFormatter = DateAxisValueFormatter(dmdData.timestamps)
                        }
                    }
                },
                update = { chart ->
                    if (lastUpdateHash[0] == currentHash) return@AndroidView
                    lastUpdateHash[0] = currentHash

                    // Připojíme boolean z ViewModelu na nativní vlastnosti grafu
                    chart.isHighlightPerTapEnabled = state.isHighlightingEnabled
                    chart.isHighlightPerDragEnabled = state.isHighlightingEnabled

                    // Pokud uživatel zrovna funkci vypnul, okamžitě smažeme kříž i text
                    if (!state.isHighlightingEnabled) {
                        chart.highlightValues(null)
                        selectedPointInfo = ""
                    }

                    val dataSets = ArrayList<LineDataSet>()

                    fun addSet(data: ArrayList<Entry>, type: TPhysValue, visible: Boolean) {
                        if (data.isNotEmpty()) {
                            val set = createLineDataSet(data, type)
                            set.isVisible = visible
                            dataSets.add(set)
                        }
                    }

                    addSet(dmdData.getT1(), TPhysValue.vT1, state.showT1)
                    addSet(dmdData.getT2(), TPhysValue.vT2, state.showT2)
                    addSet(dmdData.getT3(), TPhysValue.vT3, state.showT3)
                    addSet(dmdData.getHA(), TPhysValue.vHum, state.showGrowth)

                    val combinedData = CombinedData()
                    if (dataSets.isNotEmpty()) {
                        combinedData.setData(LineData(dataSets.toList()))
                        chart.data = combinedData

                        chart.notifyDataSetChanged()
                        chart.invalidate()
                        chart.fitScreen()
                        updateTimeRange(chart)
                    } else {
                        chart.clear()
                        chart.invalidate()
                    }
                }
            )

            // VRSTVA PŘEKRYVU 1: Plovoucí titulek grafu
            if (state.title != "Device:" && state.title.isNotBlank()) {
                Text(
                    text = state.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.DarkGray,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // VRSTVA PŘEKRYVU 2: Hodnoty z vybraného bodu
            if (state.isHighlightingEnabled && selectedPointInfo.isNotEmpty()) {
                Text(
                    text = selectedPointInfo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = androidx.compose.ui.graphics.Color.Yellow.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Right
                )
            }
        }
    }
}

@Composable
fun GraphCheckbox(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            modifier = Modifier.offset(x = (-6).dp)
        )
    }
}

private fun createLineDataSet(entries: ArrayList<Entry>, type: TPhysValue): LineDataSet {
    val set = LineDataSet(entries, type.toString())
    set.setDrawValues(false)
    set.setDrawCircles(false)
    set.mode = LineDataSet.Mode.LINEAR
    set.setDrawFilled(false)
    set.lineWidth = 1f

    val color = when (type) {
        TPhysValue.vT1 -> Color.rgb(20, 83, 45)
        TPhysValue.vT2 -> Color.rgb(241, 168, 51)
        TPhysValue.vT3 -> Color.RED
        TPhysValue.vHum, TPhysValue.vAD, TPhysValue.vMicro -> Color.rgb(0, 197, 255)
        else -> Color.GRAY
    }
    set.color = color
    set.lineWidth = 2f

    // tloustka zamerovaciho krizku
    set.highlightLineWidth = 4f
    set.highLightColor = Color.YELLOW

    if (type == TPhysValue.vHum || type == TPhysValue.vAD || type == TPhysValue.vMicro) {
        set.axisDependency = YAxis.AxisDependency.RIGHT
        set.enableDashedLine(20f, 20f, 0f)
    } else {
        set.axisDependency = YAxis.AxisDependency.LEFT
    }

    return set
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
fun GraphScreenPreview() {
    val fakeState = GraphUiState(
        title = "92204299 / Dendrometer",
        progress = 50,
        maxProgress = 100,
        showT1 = true,
        showT2 = false,
        showT3 = false,
        showGrowth = true,
        dataTimestamp = System.currentTimeMillis(),
        isHighlightingEnabled = true // Zapnuto pro náhled
    )

    val fakeDmd = DmdViewModel(SavedStateHandle()).apply {
        val t1 = getT1()
        val hum = getHA()
        val times = timestamps

        val currentTime = System.currentTimeMillis()
        for (i in 0..100) {
            val xValue = i.toFloat()
            t1.add(Entry(xValue, (20 + sin(i.toDouble() / 5) * 10).toFloat()))
            hum.add(Entry(xValue, (4000 + cos(i.toDouble() / 10) * 2000).toFloat()))
            times.add(currentTime + (i * 3600000L))
        }
    }

    MaterialTheme {
        GraphScreenContent(
            state = fakeState,
            dmdData = fakeDmd,
            onToggleLine = { _, _ -> }
        )
    }
}