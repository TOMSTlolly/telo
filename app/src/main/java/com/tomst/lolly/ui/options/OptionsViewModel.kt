package com.tomst.lolly.ui.options

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.tomst.lolly.R
import com.tomst.lolly.LollyActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OptionsUiState(
    val readFrom: Int = 0,
    val commandBookmark: String = "",
    val checkboxBookmark: Boolean = false,
    val mode: Int = 0,
    val disableAutoGraph: Boolean = true,
    val rotateGraph: Boolean = true,
    val noLedLight: Boolean = true,
    val showMicro: Boolean = true,
    val setTime: Boolean = true,
    val decimalSeparator: String = ",",
    val exportFolder: String = "",
    val bookmarkVal: String = "",
    val fromDate: String = "",
    val userEmail: String? = null,
    val initialLoadedState: OptionsUiState? = null
)

class OptionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OptionsUiState())
    val uiState: StateFlow<OptionsUiState> = _uiState.asStateFlow()

    fun updateState(transform: (OptionsUiState) -> OptionsUiState) {
        _uiState.update(transform)
    }

    fun setInitialState(state: OptionsUiState) {
        _uiState.update { it.copy(initialLoadedState = state) }
    }

    fun markSaved() {
        _uiState.update { it.copy(initialLoadedState = it) }
    }

    fun getHardwareChanges(modeOptions: List<String>): List<String> {
        val current = _uiState.value
        val initial = current.initialLoadedState ?: return emptyList()
        val changes = mutableListOf<String>()

        if (current.mode != initial.mode) {
            val modeName = if (current.mode == 0) "Keep current" else modeOptions.getOrElse(current.mode) { "Unknown" }
            changes.add("Measurement mode set to $modeName")
        }
        if (current.noLedLight != initial.noLedLight) {
            changes.add(if (current.noLedLight) "LED Disabled" else "LED Enabled")
        }
        if (current.setTime != initial.setTime) {
            changes.add(if (current.setTime) "Sync with phone time: ON" else "Sync with phone time: OFF")
        }
        if (current.decimalSeparator != initial.decimalSeparator) {
            changes.add("Decimal separator set to '${current.decimalSeparator}'")
        }

        return changes
    }

    fun hasUnsavedChanges(): Boolean {
        val current = _uiState.value
        val initial = current.initialLoadedState ?: return false
        
        return current.readFrom != initial.readFrom ||
               current.commandBookmark != initial.commandBookmark ||
               current.checkboxBookmark != initial.checkboxBookmark ||
               current.mode != initial.mode ||
               current.disableAutoGraph != initial.disableAutoGraph ||
               current.rotateGraph != initial.rotateGraph ||
               current.noLedLight != initial.noLedLight ||
               current.showMicro != initial.showMicro ||
               current.setTime != initial.setTime ||
               current.decimalSeparator != initial.decimalSeparator ||
               current.exportFolder != initial.exportFolder ||
               current.bookmarkVal != initial.bookmarkVal ||
               current.fromDate != initial.fromDate
    }

    fun saveToDevice(context: Context) {
        val sharedPref = context.getSharedPreferences(context.getString(R.string.save_options), Context.MODE_PRIVATE)
        val state = _uiState.value

        with(sharedPref.edit()) {
            putInt("readFrom", state.readFrom)
            putString("commandBookmark", state.commandBookmark)
            putBoolean("checkboxBookmark", state.checkboxBookmark)
            putInt("mode", state.mode)
            putBoolean("showgraph", state.disableAutoGraph)
            putBoolean("rotategraph", state.rotateGraph)
            putBoolean("noledlight", state.noLedLight)
            putBoolean("showmicro", state.showMicro)
            putBoolean("settime", state.setTime)
            putString("decimalseparator", state.decimalSeparator)

            state.bookmarkVal.toIntOrNull()?.let {
                putInt("bookmarkVal", it)
            }
            putString("fromDate", state.fromDate)
            apply()
        }
        markSaved()
    }

    fun loadFromDevice(context: Context) {
        val sharedPref = context.getSharedPreferences(context.getString(R.string.save_options), Context.MODE_PRIVATE)

        val folderUri = sharedPref.getString("prefExportFolder", "") ?: ""
        val folderName = extractFolderNameFromEncodedUri(folderUri)

        val loadedState = OptionsUiState(
            readFrom = sharedPref.getInt("readFrom", 0),
            commandBookmark = sharedPref.getString("commandBookmark", "") ?: "",
            checkboxBookmark = sharedPref.getBoolean("checkboxBookmark", false),
            mode = sharedPref.getInt("mode", 0),
            disableAutoGraph = sharedPref.getBoolean("showgraph", true),
            rotateGraph = sharedPref.getBoolean("rotategraph", true),
            noLedLight = sharedPref.getBoolean("noledlight", true),
            showMicro = sharedPref.getBoolean("showmicro", true),
            setTime = sharedPref.getBoolean("settime", true),
            decimalSeparator = sharedPref.getString("decimalseparator", ",") ?: ",",
            exportFolder = folderName,
            bookmarkVal = sharedPref.getInt("bookmarkVal", 0).let { v -> if (v == 0) "" else v.toString() },
            fromDate = sharedPref.getString("fromDate", "") ?: ""
        )
        
        updateState { loadedState }
        setInitialState(loadedState)
    }

    fun setPrefExportFolder(folder: String, context: Context) {
        val sharedPref = context.getSharedPreferences(context.getString(R.string.save_options), Context.MODE_PRIVATE)
        sharedPref.edit().putString("prefExportFolder", folder).apply()

        val folderName = extractFolderNameFromEncodedUri(folder)
        updateState { it.copy(exportFolder = folderName) }
        LollyActivity.getInstance().prefExportFolder = folder
    }

    private fun extractFolderNameFromEncodedUri(uriPath: String): String {
        val spath = Uri.decode(uriPath)
        val pathSeparator = ":"
        return if (spath.contains(pathSeparator)) {
            val spathParts = spath.split(pathSeparator)
            spathParts.last()
        } else spath
    }
}
