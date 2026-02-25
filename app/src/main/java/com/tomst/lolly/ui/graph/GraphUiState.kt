package com.tomst.lolly.ui.graph

import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer

data class GraphUiState(
    val title: String = "Device:",
    val progress: Int = 0,
    val maxProgress: Int = 0,
    val showT1: Boolean = true,
    val showT2: Boolean = false,
    val showT3: Boolean = false,
    val showGrowth: Boolean = true,
    // Ve Vico 2 používáme CartesianChartModelProducer
    val chartEntryModelProducer: CartesianChartModelProducer? = null,
    val shouldAnimate: Boolean = false,
    val isLoading: Boolean = false
)