package com.tomst.lolly.ui.graph

data class GraphUiState(
    val title: String = "Device:",
    val progress: Int = 0,
    val maxProgress: Int = 0,
    val isLoading: Boolean = false,
    // Viditelnost jednotlivých grafů
    val showT1: Boolean = true,
    val showT2: Boolean = true,
    val showT3: Boolean = true,
    val showGrowth: Boolean = true,
    // Trigger pro překreslení grafu (když se změní data)
    val dataTimestamp: Long = 0L,
    val isHighlightingEnabled: Boolean = false
)