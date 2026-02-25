package com.tomst.lolly.ui.graph

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Importy Vico 2.0.0 ---
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEndAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    GraphScreenContent(
        state = state,
        onToggleLineVisibility = { lineTag, isChecked ->
            viewModel.toggleLineVisibility(lineTag, isChecked)
        },
        modifier = modifier
    )
}

@Composable
fun GraphScreenContent(
    state: GraphUiState,
    onToggleLineVisibility: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = state.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.isLoading || state.maxProgress > 0) {
                LinearProgressIndicator(
                    progress = { if (state.maxProgress > 0) state.progress.toFloat() / state.maxProgress else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (state.chartEntryModelProducer != null) {

                val t1Color = if (state.showT1) Color(0xFF14532D) else Color.Transparent
                val t2Color = if (state.showT2) Color(0xFFF1A833) else Color.Transparent
                val t3Color = if (state.showT3) Color.Red else Color.Transparent
                val soilColor = if (state.showGrowth) Color(0xFF00C5FF) else Color.Transparent

                // 1. Vrstva pro Teploty (Mapovaná na levou osu - Start)
                val tempLayer = rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        rememberLine(fill = LineCartesianLayer.LineFill.single(fill(t1Color))),
                        rememberLine(fill = LineCartesianLayer.LineFill.single(fill(t2Color))),
                        rememberLine(fill = LineCartesianLayer.LineFill.single(fill(t3Color)))
                    ),
                    verticalAxisPosition = Axis.Position.Vertical.Start
                )

                // 2. Vrstva pro Vlhkost (Mapovaná na pravou osu - End)
                val humLayer = rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        rememberLine(fill = LineCartesianLayer.LineFill.single(fill(soilColor)))
                    ),
                    verticalAxisPosition = Axis.Position.Vertical.End
                )

                val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm")

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        tempLayer,
                        humLayer,
                        // BOD 1: guidline = null odstraní horizontální mřížku pro obě osy
                        startAxis = rememberStartAxis(title = "Temp (°C)", guideline = null),
                        endAxis = rememberEndAxis(title = "Humidity", guideline = null),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = { value: Double, _, _ ->
                                LocalDateTime.now().plusHours(value.toLong()).format(dateTimeFormatter)
                            },
                            labelRotationDegrees = -45f,
                            // BOD 3: ItemPlacer řídí hustotu značek na ose X
                            itemPlacer = remember { HorizontalAxis.ItemPlacer.default() }
                        )
                    ),
                    modelProducer = state.chartEntryModelProducer,
                    scrollState = rememberVicoScrollState(scrollEnabled = true),
                    // BOD 2: initialZoom = Zoom.Content zaručí zobrazení všech dat
                    zoomState = rememberVicoZoomState(
                        zoomEnabled = true,
                        initialZoom = Zoom.Content
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Načítám data grafu...", modifier = Modifier.align(Alignment.Center))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 56.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GraphCheckbox("T1:", state.showT1) { onToggleLineVisibility(1, it) }
            GraphCheckbox("T2:", state.showT2) { onToggleLineVisibility(2, it) }
            GraphCheckbox("T3:", state.showT3) { onToggleLineVisibility(3, it) }
            GraphCheckbox("Soil", state.showGrowth) { onToggleLineVisibility(4, it) }
        }
    }
}

@Composable
fun GraphCheckbox(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
        Text(text = label)
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun GraphScreenPreview() {
    val mockProducer = remember {
        val producer = CartesianChartModelProducer.build()
        val t1 = mutableListOf<Float>()
        val t2 = mutableListOf<Float>()
        val t3 = mutableListOf<Float>()
        val hum = mutableListOf<Float>()

        var temp1 = 20.0f
        var temp2 = 18.0f
        var temp3 = 15.0f
        var humidity = 2000.0f

        for (i in 0..100) {
            temp1 += (Random.nextFloat() * 2 - 1)
            temp2 += (Random.nextFloat() * 1.5f - 0.7f)
            temp3 += (Random.nextFloat() * 1 - 0.5f)

            humidity += (Random.nextFloat() * 200 - 100)
            if (humidity > 4000f) humidity = 4000f
            if (humidity < 1f) humidity = 1f

            t1.add(temp1)
            t2.add(temp2)
            t3.add(temp3)
            hum.add(humidity)
        }

        producer.tryRunTransaction {
            lineSeries {
                series(t1)
                series(t2)
                series(t3)
            }
            lineSeries {
                series(hum)
            }
        }
        producer
    }

    val mockState = GraphUiState(
        title = "12345678 / TMS-4 (Náhled)",
        progress = 100,
        maxProgress = 100,
        showT1 = true,
        showT2 = true,
        showT3 = true,
        showGrowth = true,
        chartEntryModelProducer = mockProducer,
        isLoading = false
    )

    MaterialTheme {
        GraphScreenContent(state = mockState, onToggleLineVisibility = { _, _ -> })
    }
}