package com.tomst.lolly.ui.graph

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    var lastLoadedFileName: String = ""
    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    fun setTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun updateProgress(current: Int, max: Int? = null) {
        _uiState.update {
            it.copy(
                progress = current,
                maxProgress = max ?: it.maxProgress,
                isLoading = current < (max ?: it.maxProgress)
            )
        }
    }

    fun toggleLineVisibility(tag: Int, isChecked: Boolean) {
        _uiState.update { state ->
            when (tag) {
                1 -> state.copy(showT1 = isChecked)
                2 -> state.copy(showT2 = isChecked)
                3 -> state.copy(showT3 = isChecked)
                4 -> state.copy(showGrowth = isChecked)
                else -> state
            }
        }
    }

    // --- NOVÉ: Funkce pro zapnutí/vypnutí odečítání z grafu ---
    fun setHighlightingEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isHighlightingEnabled = enabled) }
    }

    fun notifyDataChanged() {
        _uiState.update { it.copy(dataTimestamp = System.currentTimeMillis()) }
    }
}