package com.tomst.lolly.ui.graph

import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.Utils
import com.tomst.lolly.core.DmdViewModel
import com.tomst.lolly.core.TPhysValue
import java.util.ArrayList
import kotlin.math.cos
import kotlin.math.sin

// Phase 5: MultilineXAxisRenderer (Canvas Override)
class MultilineXAxisRenderer(
    viewPortHandler: com.github.mikephil.charting.utils.ViewPortHandler?,
    xAxis: XAxis?,
    trans: com.github.mikephil.charting.utils.Transformer?
) : com.github.mikephil.charting.renderer.XAxisRenderer(viewPortHandler, xAxis, trans) {
    override fun drawLabel(
        c: android.graphics.Canvas?,
        formattedLabel: String?,
        x: Float,
        y: Float,
        anchor: com.github.mikephil.charting.utils.MPPointF?,
        angleDegrees: Float
    ) {
        val lines = formattedLabel?.split("\n") ?: return
        var currentY = y
        for (line in lines) {
            Utils.drawXAxisValue(
                c,
                line,
                x,
                currentY,
                mAxisLabelPaint,
                anchor,
                angleDegrees
            )
            currentY += mAxisLabelPaint.textSize + Utils.convertDpToPixel(2f)
        }
    }
}

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
    // Phase 4/6: Trigger to clear highlights from Compose
    var clearHighlightTrigger by remember { mutableIntStateOf(0) }
    val lastClearHighlightTrigger = remember { IntArray(1) { 0 } }
    
    // Phase 7: Store integer X-index of the crosshair
    var lockedXIndex by remember { mutableStateOf<Int?>(null) }
    // Phase 8: Strict Session Locking
    var lockedDataSetSession by remember { mutableStateOf<Int?>(null) }
    // Odkaz na graf pro manuální vyhledávání extrémů
    var chartRef by remember { mutableStateOf<CombinedChart?>(null) }

    val dateFormat = remember { java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()) }
    
    // Phase 9: Reactive Global Date Range
    val globalRange = remember(state.dataTimestamp) {
        if (dmdData.timestamps.isNotEmpty()) {
            val start = dateFormat.format(java.util.Date(dmdData.timestamps.first()))
            val end = dateFormat.format(java.util.Date(dmdData.timestamps.last()))
            "$start  –  $end"
        } else {
            ""
        }
    }

    // Dynamic Label: Soil vs Tree
    val growthLabel = remember(state.title) {
        if (state.title.contains("Dendrometer", ignoreCase = true)) "Tree" else "Soil"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Optimized insets: safeDrawing handles both status bars and side navigation bars in landscape
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        // Phase 7/9: Compact Header (Global Date Range + Device Title)
        if (globalRange.isNotEmpty()) {
            Text(
                text = globalRange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = state.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.DarkGray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Progress Bar Section
        if (state.maxProgress > 0) {
            LinearProgressIndicator(
                progress = { state.progress.toFloat() / state.maxProgress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Tri-State Color Buttons (Refined Look)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TriStateButton(
                label = "T1",
                hasData = dmdData.getT1().isNotEmpty(),
                isShown = state.showT1,
                activeColor = androidx.compose.ui.graphics.Color(0xFF14532D) // Dark Green
            ) { checked ->
                onToggleLine(1, checked)
                if (!checked) {
                    lockedDataSetSession = null
                    clearHighlightTrigger++
                }
            }
            TriStateButton(
                label = "T2",
                hasData = dmdData.getT2().isNotEmpty(),
                isShown = state.showT2,
                activeColor = androidx.compose.ui.graphics.Color(0xFF064E3B) // Slightly darker green
            ) { checked ->
                onToggleLine(2, checked)
                if (!checked) {
                    lockedDataSetSession = null
                    clearHighlightTrigger++
                }
            }
            TriStateButton(
                label = "T3",
                hasData = dmdData.getT3().isNotEmpty(),
                isShown = state.showT3,
                activeColor = androidx.compose.ui.graphics.Color(0xFF16A34A) // Slightly lighter green
            ) { checked ->
                onToggleLine(3, checked)
                if (!checked) {
                    lockedDataSetSession = null
                    clearHighlightTrigger++
                }
            }
            TriStateButton(
                label = growthLabel,
                hasData = dmdData.getHA().isNotEmpty(),
                isShown = state.showGrowth,
                activeColor = androidx.compose.ui.graphics.Color(0xFF1E3A8A) // Darker Blue
            ) { checked ->
                onToggleLine(4, checked)
                if (!checked) {
                    lockedDataSetSession = null
                    clearHighlightTrigger++
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    CombinedChart(context).apply {
                        val chart = this
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        description.isEnabled = false
                        // Phase 8: Static baseline room for multiline text
                        extraBottomOffset = 60f
                        
                        setTouchEnabled(true)
                        dragDecelerationFrictionCoef = 0.9f
                        isDragEnabled = true
                        isHighlightPerDragEnabled = false
                        setScaleEnabled(true)
                        setDrawGridBackground(false)
                        
                        // Phase 10: Industry-Standard Time-Series Zooming (Auto-Scale Y)
                        setPinchZoom(false)
                        isScaleXEnabled = true
                        isScaleYEnabled = false
                        isAutoScaleMinMaxEnabled = false // Eradicate CPU Lag
                        isDoubleTapToZoomEnabled = false
                        
                        // Phase 11: Atomic Resolution Anchor
                        setVisibleXRangeMinimum(2f)
                        
                        maxHighlightDistance = Utils.convertDpToPixel(100f)
                        setDrawMarkers(true)

                        setOnChartGestureListener(object : com.github.mikephil.charting.listener.OnChartGestureListener {
                            override fun onChartScale(me: android.view.MotionEvent?, scaleX: Float, scaleY: Float) {}
                            override fun onChartTranslate(me: android.view.MotionEvent?, dX: Float, dY: Float) {}
                            override fun onChartGestureEnd(me: android.view.MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                            override fun onChartGestureStart(me: android.view.MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                            override fun onChartLongPressed(me: android.view.MotionEvent?) {}
                            override fun onChartDoubleTapped(me: android.view.MotionEvent?) {}
                            override fun onChartSingleTapped(me: android.view.MotionEvent?) {}
                            override fun onChartFling(me1: android.view.MotionEvent?, me2: android.view.MotionEvent?, velocityX: Float, velocityY: Float) {}
                        })

                        // Proxy Touch Interceptor
                        var isScrubbing = false
                        val nativeTouchListener = onTouchListener
                        
                        setOnTouchListener { v, event ->
                            when (event.actionMasked) {
                                android.view.MotionEvent.ACTION_DOWN -> {
                                    val highlights = highlighted
                                    if (highlights != null && highlights.isNotEmpty()) {
                                        val h = highlights[0]
                                        if (Math.abs(event.x - h.drawX) < Utils.convertDpToPixel(40f)) {
                                            isScrubbing = true
                                            v.parent?.requestDisallowInterceptTouchEvent(true)
                                            return@setOnTouchListener true
                                        }
                                    }
                                    lockedXIndex = null
                                    lockedDataSetSession = null
                                    highlightValues(null)
                                    isScrubbing = false
                                }
                                android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                                    if (event.pointerCount >= 2) {
                                        val xDist = Math.abs(event.getX(0) - event.getX(1))
                                        val yDist = Math.abs(event.getY(0) - event.getY(1))
                                        if (xDist > yDist * 1.5f) {
                                            chart.setPinchZoom(false)
                                            chart.isScaleXEnabled = true
                                            chart.isScaleYEnabled = false
                                        } else {
                                            chart.setPinchZoom(true)
                                            chart.isScaleXEnabled = true
                                            chart.isScaleYEnabled = true
                                        }
                                    }
                                    isScrubbing = false
                                    return@setOnTouchListener nativeTouchListener?.onTouch(v, event) ?: false
                                }
                                android.view.MotionEvent.ACTION_MOVE -> {
                                    if (isScrubbing && lockedDataSetSession != null) {
                                        val trans = getTransformer(YAxis.AxisDependency.LEFT)
                                        val pts = floatArrayOf(event.x, 0f)
                                        trans.pixelsToValue(pts)
                                        var targetX = Math.round(pts[0]).toFloat()

                                        // Sticky Extrema Logic
                                        val lineData = chart.data?.lineData
                                        if (lineData != null) {
                                            val dataSet = lineData.getDataSetByIndex(lockedDataSetSession!!)
                                            if (dataSet != null) {
                                                // Nízký fixní radius pro okamžitou odezvu bez zamrzání
                                                val radius = 5
                                                
                                                val targetIdx = targetX.toInt()
                                                var bestIdx = targetIdx
                                                var highestScore = -1f

                                                val startIdx = Math.max(1, targetIdx - radius)
                                                val endIdx = Math.min(dataSet.entryCount - 2, targetIdx + radius)

                                                for (i in startIdx..endIdx) {
                                                    val yPrev = dataSet.getEntryForIndex(i - 1).y
                                                    val yCurr = dataSet.getEntryForIndex(i).y
                                                    val yNext = dataSet.getEntryForIndex(i + 1).y

                                                    val isMax = yCurr >= yPrev && yCurr >= yNext && (yCurr > yPrev || yCurr > yNext)
                                                    val isMin = yCurr <= yPrev && yCurr <= yNext && (yCurr < yPrev || yCurr < yNext)

                                                    if (isMax || isMin) {
                                                        val dist = Math.abs(i - targetIdx)
                                                        val magnitude = Math.abs(yCurr - yPrev) + Math.abs(yCurr - yNext)
                                                        val score = magnitude - (dist * 0.001f)
                                                        
                                                        if (score > highestScore) {
                                                            highestScore = score
                                                            bestIdx = i
                                                        }
                                                    }
                                                }
                                                targetX = bestIdx.toFloat()
                                            }
                                        }

                                        val h = com.github.mikephil.charting.highlight.Highlight(targetX, Float.NaN, lockedDataSetSession!!)
                                        h.dataIndex = 0 
                                        highlightValue(h, true)
                                        return@setOnTouchListener true
                                    }
                                }
                                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                    if (isScrubbing) {
                                        isScrubbing = false
                                        return@setOnTouchListener true
                                    }
                                }
                            }
                            nativeTouchListener?.onTouch(v, event) ?: false
                        }

                        setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                if (e != null && h != null) {
                                    lockedXIndex = e.x.toInt()
                                    if (lockedDataSetSession == null) {
                                        lockedDataSetSession = h.dataSetIndex
                                    }
                                }
                            }
                            override fun onNothingSelected() {
                                lockedXIndex = null
                                lockedDataSetSession = null
                            }
                        })

                        legend.apply {
                            isEnabled = false // Phase 13: Remove text legend to maximize graph space
                        }

                        axisRight.apply {
                            setDrawGridLines(false) // Phase 12: Grid Alignment
                            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                            setDrawLabels(true)
                            textColor = Color.BLACK
                            spaceTop = 15f
                            spaceBottom = 15f
                            setLabelCount(6, true)
                            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                                    return String.format(java.util.Locale.getDefault(), "%,.0f", value)
                                }
                            }
                        }
                        axisLeft.apply {
                            setDrawGridLines(true)
                            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                            textColor = Color.BLACK
                            spaceTop = 15f
                            spaceBottom = 15f
                            setLabelCount(6, true)
                            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                                    return String.format(java.util.Locale.getDefault(), "%.1f", value)
                                }
                            }
                        }
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            labelRotationAngle = 0f
                            textColor = Color.BLACK
                            setLabelCount(6, false)
                            textSize = 10f
                            setDrawAxisLine(true)
                            setDrawGridLines(true)
                            granularity = 1f
                            isGranularityEnabled = true
                            valueFormatter = DateAxisValueFormatter(dmdData.timestamps, chart)
                        }
                        
                        setXAxisRenderer(MultilineXAxisRenderer(viewPortHandler, xAxis, getTransformer(YAxis.AxisDependency.LEFT)))
                    }
                },
                update = { chart ->
                    if (clearHighlightTrigger != lastClearHighlightTrigger[0]) {
                        lastClearHighlightTrigger[0] = clearHighlightTrigger
                        chart.highlightValues(null)
                        lockedXIndex = null
                        lockedDataSetSession = null
                    }

                    chart.extraBottomOffset = if (chart.visibleXRange <= 500) 80f else 60f

                    val lineDataSets = ArrayList<LineDataSet>()
                    var currentIdx = 0
                    fun addSet(data: ArrayList<Entry>, type: TPhysValue, visible: Boolean) {
                        if (data.isNotEmpty()) {
                            val set = createLineDataSet(data, type)
                            set.isVisible = visible
                            set.isHighlightEnabled = if (lockedDataSetSession != null) {
                                lockedDataSetSession == currentIdx
                            } else {
                                visible
                            }
                            lineDataSets.add(set)
                            currentIdx++
                        }
                    }

                    addSet(dmdData.getT1(), TPhysValue.vT1, state.showT1)
                    addSet(dmdData.getT2(), TPhysValue.vT2, state.showT2)
                    addSet(dmdData.getT3(), TPhysValue.vT3, state.showT3)
                    addSet(dmdData.getHA(), TPhysValue.vHum, state.showGrowth)

                    val combinedData = CombinedData()
                    if (lineDataSets.isNotEmpty()) {
                        combinedData.setData(LineData(lineDataSets.toList()))
                        chart.data = combinedData
                        chart.notifyDataSetChanged()
                        chart.invalidate()
                        if (chart.scaleX <= 1.01f && chart.scaleY <= 1.01f) {
                            chart.fitScreen()
                        }
                    } else {
                        chart.clear()
                        chart.invalidate()
                    }

                    chart.isHighlightPerTapEnabled = state.isHighlightingEnabled
                    if (!state.isHighlightingEnabled) {
                        lockedXIndex = null
                        lockedDataSetSession = null
                        chart.highlightValues(null)
                    } else if (lockedXIndex != null && lockedDataSetSession != null) {
                        val h = Highlight(lockedXIndex!!.toFloat(), Float.NaN, lockedDataSetSession!!)
                        h.dataIndex = 0
                        chart.highlightValue(h, false)
                    }
                }
            )

            // Phase 7: Full-Width Yellow HUD
            if (state.isHighlightingEnabled && lockedXIndex != null) {
                HUDOverlay(
                    index = lockedXIndex!!,
                    dmdData = dmdData,
                    state = state,
                    onClose = {
                        lockedXIndex = null
                        lockedDataSetSession = null
                        clearHighlightTrigger++
                    },
                    onSeekToMin = {
                        val chart = chartRef ?: return@HUDOverlay
                        val lineData = chart.data?.lineData ?: return@HUDOverlay
                        val session = lockedDataSetSession ?: return@HUDOverlay
                        val dataSet = lineData.getDataSetByIndex(session) ?: return@HUDOverlay

                        val currentX = lockedXIndex!!.toFloat()
                        val currentEntryIdx = dataSet.getEntryIndex(currentX, Float.NaN, com.github.mikephil.charting.data.DataSet.Rounding.CLOSEST)

                        var foundX: Float? = null
                        // Dynamické okno podle přiblížení - hledá "makro" propady, ignoruje mikro-šum
                        val W = Math.max(10, (chart.visibleXRange * 0.05f).toInt())

                        val startI = Math.min(dataSet.entryCount - 1, currentEntryIdx + 1)
                        for (i in startI until dataSet.entryCount) {
                            val yCurr = dataSet.getEntryForIndex(i).y
                            var isValley = true

                            val checkStart = Math.max(0, i - W)
                            val checkEnd = Math.min(dataSet.entryCount - 1, i + W)

                            for (j in checkStart..checkEnd) {
                                if (j == i) continue
                                if (dataSet.getEntryForIndex(j).y < yCurr) {
                                    isValley = false
                                    break
                                }
                            }

                            if (isValley && i > currentEntryIdx + W / 2) {
                                foundX = dataSet.getEntryForIndex(i).x
                                break
                            }
                        }

                        if (foundX != null) {
                            val h = Highlight(foundX, Float.NaN, session)
                            h.dataIndex = 0
                            chart.highlightValue(h, true)
                            lockedXIndex = foundX.toInt()
                            // Pokud bod vyskočí ze zorného pole, posuneme kameru
                            if (foundX > chart.highestVisibleX || foundX < chart.lowestVisibleX) {
                                chart.moveViewToX(foundX - chart.visibleXRange / 2f)
                            }
                        }
                    },
                    onSeekToMax = {
                        val chart = chartRef ?: return@HUDOverlay
                        val lineData = chart.data?.lineData ?: return@HUDOverlay
                        val session = lockedDataSetSession ?: return@HUDOverlay
                        val dataSet = lineData.getDataSetByIndex(session) ?: return@HUDOverlay

                        val currentX = lockedXIndex!!.toFloat()
                        val currentEntryIdx = dataSet.getEntryIndex(currentX, Float.NaN, com.github.mikephil.charting.data.DataSet.Rounding.CLOSEST)

                        var foundX: Float? = null
                        // Dynamické okno pro ignorování šumu
                        val W = Math.max(10, (chart.visibleXRange * 0.05f).toInt())

                        val startI = Math.min(dataSet.entryCount - 1, currentEntryIdx + 1)
                        for (i in startI until dataSet.entryCount) {
                            val yCurr = dataSet.getEntryForIndex(i).y
                            var isPeak = true

                            val checkStart = Math.max(0, i - W)
                            val checkEnd = Math.min(dataSet.entryCount - 1, i + W)

                            for (j in checkStart..checkEnd) {
                                if (j == i) continue
                                if (dataSet.getEntryForIndex(j).y > yCurr) {
                                    isPeak = false
                                    break
                                }
                            }

                            if (isPeak && i > currentEntryIdx + W / 2) {
                                foundX = dataSet.getEntryForIndex(i).x
                                break
                            }
                        }

                        if (foundX != null) {
                            val h = Highlight(foundX, Float.NaN, session)
                            h.dataIndex = 0
                            chart.highlightValue(h, true)
                            lockedXIndex = foundX.toInt()
                            // Pokud bod vyskočí ze zorného pole, posuneme kameru
                            if (foundX > chart.highestVisibleX || foundX < chart.lowestVisibleX) {
                                chart.moveViewToX(foundX - chart.visibleXRange / 2f)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RowScope.TriStateButton(label: String, hasData: Boolean, isShown: Boolean, activeColor: androidx.compose.ui.graphics.Color, onClick: (Boolean) -> Unit) {
    val bgColor = when {
        !hasData -> androidx.compose.ui.graphics.Color.LightGray
        isShown -> activeColor
        else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red if off
    }
    val textColor = if (hasData) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.DarkGray
    
    // Improved button look with shadow and slight gradient simulation via surface elevation
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .shadow(
                elevation = if (hasData) 4.dp else 0.dp,
                shape = RoundedCornerShape(8.dp)
            ),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        onClick = { if (hasData) onClick(!isShown) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun HUDOverlay(
    index: Int,
    dmdData: DmdViewModel,
    state: GraphUiState,
    onClose: () -> Unit,
    onSeekToMin: () -> Unit = {},
    onSeekToMax: () -> Unit = {}
) {
    val timeStr = remember(index) {
        if (index in dmdData.timestamps.indices) {
            val preciseDateFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault())
            preciseDateFormat.format(java.util.Date(dmdData.timestamps[index]))
        } else ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color(0xD9FFF9C4))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = timeStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.Black,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            
            // Jasná navigační tlačítka pro hledání extrémů
            Button(
                onClick = onSeekToMin,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)), // Výrazná modrá
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(28.dp).padding(end = 4.dp)
            ) {
                Text("▼ Seek Min", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            }
            Button(
                onClick = onSeekToMax,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFEF4444)), // Výrazná červená
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(28.dp).padding(end = 8.dp)
            ) {
                Text("▲ Seek Max", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            HUDValue(dmdData.getT1(), index, state.showT1)
            HUDValue(dmdData.getT2(), index, state.showT2)
            HUDValue(dmdData.getT3(), index, state.showT3)
            HUDValue(dmdData.getHA(), index, state.showGrowth)
        }
    }
}

@Composable
fun RowScope.HUDValue(data: ArrayList<Entry>, index: Int, isShown: Boolean) {
    val entry = if (isShown) data.find { it.x.toInt() == index } else null
    val text = if (entry != null) String.format(java.util.Locale.US, "%.1f", entry.y) else "--"
    
    Text(
        text = text,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = androidx.compose.ui.graphics.Color.Black
    )
}

private fun createLineDataSet(entries: ArrayList<Entry>, type: TPhysValue): LineDataSet {
    val set = LineDataSet(entries, type.toString())
    set.setDrawValues(false)
    set.setDrawCircles(false)
    set.mode = LineDataSet.Mode.LINEAR
    set.lineWidth = 2f

    val color = when (type) {
        TPhysValue.vT1 -> Color.rgb(20, 83, 45) // Dark Green
        TPhysValue.vT2 -> Color.rgb(6, 78, 59) // Slightly darker green
        TPhysValue.vT3 -> Color.rgb(22, 163, 74) // Slightly lighter green
        TPhysValue.vHum, TPhysValue.vAD, TPhysValue.vMicro -> Color.rgb(30, 58, 138) // Darker Blue
        else -> Color.GRAY
    }
    set.color = color

    // Phase 3: Enhanced Crosshair
    set.highlightLineWidth = 2f
    set.highLightColor = Color.BLACK
    set.enableDashedHighlightLine(15f, 5f, 0f)
    set.setDrawHorizontalHighlightIndicator(true)
    set.setDrawVerticalHighlightIndicator(true)

    if (type == TPhysValue.vHum || type == TPhysValue.vAD || type == TPhysValue.vMicro) {
        set.axisDependency = YAxis.AxisDependency.RIGHT
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
        isHighlightingEnabled = true
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
