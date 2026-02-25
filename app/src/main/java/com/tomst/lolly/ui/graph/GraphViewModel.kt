package com.tomst.lolly.ui.graph

import androidx.lifecycle.ViewModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GraphViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    fun updateProgress(progress: Int, max: Int? = null) {
        _uiState.update {
            it.copy(
                progress = progress,
                maxProgress = max ?: it.maxProgress,
                isLoading = progress < it.maxProgress
            )
        }
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun toggleLineVisibility(lineTag: Int, isChecked: Boolean) {
        _uiState.update { state ->
            when (lineTag) {
                1 -> state.copy(showT1 = isChecked)
                2 -> state.copy(showT2 = isChecked)
                3 -> state.copy(showT3 = isChecked)
                4 -> state.copy(showGrowth = isChecked)
                else -> state
            }
        }
    }

    fun setGraphData(producer: CartesianChartModelProducer) {
        _uiState.update { it.copy(chartEntryModelProducer = producer, shouldAnimate = true) }
    }

    fun onAnimationFinished() {
        _uiState.update { it.copy(shouldAnimate = false) }
    }
}