package com.tomst.lolly.ui.options

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OptionsUiState(
    val readFrom: Int = 0,
    val commandBookmark: String = "",
    val checkboxBookmark: Boolean = false,
    val mode: Int = 0,
    val showGraph: Boolean = true,
    val rotateGraph: Boolean = true,
    val noLedLight: Boolean = true,
    val showMicro: Boolean = true,
    val setTime: Boolean = true,
    val decimalSeparator: String = ",",
    val exportFolder: String = "",
    val bookmarkVal: String = "",
    val fromDate: String = "",
    val userEmail: String? = null
)

class OptionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OptionsUiState())
    val uiState: StateFlow<OptionsUiState> = _uiState.asStateFlow()

    fun updateState(transform: (OptionsUiState) -> OptionsUiState) {
        _uiState.update(transform)
    }
}
