package com.tomst.lolly.ui.graph

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.tomst.lolly.core.CSVReader
import com.tomst.lolly.core.DmdViewModel
import com.tomst.lolly.core.TDeviceType
import com.tomst.lolly.core.TMereni
import com.tomst.lolly.core.shared.getSerialNumberFromFileName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    var lastLoadedFileName: String = ""
    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()
    
    private var lastDrawnStep = 0
    private var fSerialNumber = ""

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

    fun setHighlightingEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isHighlightingEnabled = enabled) }
    }

    fun notifyDataChanged() {
        _uiState.update { it.copy(dataTimestamp = System.currentTimeMillis()) }
    }

    // --- MIGRATED FROM GRAPH FRAGMENT ---
    
    fun processGraphMessage(msg: String, dmd: DmdViewModel) {
        Log.d("GRAPH", "Received: $msg")
        val parts = msg.split(" ")

        if (parts[0] == "TMD") {
            // Data z USB
            if (parts.size > 1) {
                fSerialNumber = parts[1]
            }
            setGraphTitleAndCheckboxes(dmd)
            notifyDataChanged()
        } else {
            // Data ze souboru (kliknutí v ListFragment)
            val fileNames = msg.split(";")
            if (fileNames.isNotEmpty() && fileNames[0].isNotEmpty()) {
                val fileName = fileNames[0]
                if (loadCSVFil(fileName, dmd)) {
                    fSerialNumber = getSerialNumberFromFileName(fileName)
                }
            }
        }
    }

    private fun loadCSVFil(uriPath: String, dmd: DmdViewModel): Boolean {
        dmd.ClearMereni()
        lastDrawnStep = 0 // Reset kroku při novém souboru

        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val mer = msg.obj as TMereni
                dmd.AddMereni(mer)
            }
        }

        val fileUri = Uri.parse(uriPath)
        val csv = CSVReader(fileUri.toString())
        csv.SetHandler(handler)

        csv.SetProgressListener { value ->
            if (value < 0) {
                // Max hodnota
                updateProgress(0, max = -value)
            } else {
                // Aktuální progress
                updateProgress(value)

                val max = _uiState.value.maxProgress
                if (max > 0) {
                    val percent = (value * 100) / max
                    val step = percent / 20

                    if (step > lastDrawnStep) {
                        lastDrawnStep = step
                        notifyDataChanged()
                    }
                }
            }
        }

        csv.SetFinListener { _ ->
            val det = csv.fileDetail
            dmd.setDeviceType(det.deviceType)
            setGraphTitleAndCheckboxes(dmd)
            Log.d("GraphViewModel", "Finished loading CSV")

            lastDrawnStep = 0
            notifyDataChanged()
        }

        csv.start()
        setHighlightingEnabled(true)
        return true
    }

    private fun setGraphTitleAndCheckboxes(dmd: DmdViewModel) {
        val title: String
        when (dmd.GetDeviceType()) {
            TDeviceType.dLolly4, TDeviceType.dLolly3 -> {
                title = if (dmd.GetDeviceType() == TDeviceType.dLolly4) "TMS-4" else "TMS-3"
                toggleLineVisibility(1, true)
                toggleLineVisibility(2, true)
                toggleLineVisibility(3, true)
                toggleLineVisibility(4, true)
            }
            TDeviceType.dAD, TDeviceType.dAdMicro -> {
                title = "Dendrometer"
                toggleLineVisibility(1, true)
                toggleLineVisibility(2, false)
                toggleLineVisibility(3, false)
                toggleLineVisibility(4, true)
            }
            TDeviceType.dTermoChron -> {
                title = "Thermometer"
                toggleLineVisibility(1, true)
                toggleLineVisibility(2, false)
                toggleLineVisibility(3, false)
                toggleLineVisibility(4, false)
            }
            else -> title = "Unknown"
        }
        setTitle("$fSerialNumber / $title")
    }
}