package com.tomst.lolly.ui.home

import androidx.lifecycle.ViewModel
import com.tomst.lolly.R
import com.tomst.lolly.core.TDeviceType
import com.tomst.lolly.core.TMeteo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun updateSerialNumber(sn: String) {
        _uiState.update { it.copy(serialNumber = sn) }
    }

    fun updateDeviceVersion(ver: String) {
        _uiState.update { it.copy(appVersion = ver) }
    }

    fun updateConnectionStatus(status: String, isConnected: Boolean) {
        _uiState.update {
            it.copy(
                connectionStatus = status,
                isAdapterConnected = isConnected,
                adapterImageRes = if (isConnected) R.drawable.adapter_green else R.drawable.adapter_red
            )
        }
    }

    fun updateDiagnostics(t1: Double, t2: Double, t3: Double, humAd: Double) {
        // Konverze AD na procenta (převzato z původního kódu)
        val proc = ((humAd / com.tomst.lolly.core.Constants.MAX_HUM) * 100).toInt()

        _uiState.update {
            it.copy(
                temp1 = String.format("%.1f", t1),
                temp2 = String.format("%.1f", t2),
                temp3 = String.format("%.1f", t3),
                humAD = proc.toString(),
                humProc = proc
            )
        }
    }

    fun updateMemory(capacityUsed: Int) {
        _uiState.update { it.copy(memoryUsage = capacityUsed) }
    }

    fun updateTime(devTime: String, phoneTime: String, diff: String) {
        _uiState.update {
            it.copy(
                deviceTime = devTime,
                phoneTime = phoneTime,
                timeDiff = diff
            )
        }
    }

    fun updateProgress(progress: Int, max: Int = 100, remainDaysInfo: String = "") {
        _uiState.update {
            it.copy(
                downloadProgress = progress,
                remainDays = remainDaysInfo
            )
        }
    }

    fun updateHeartbeat(symbol: String) {
        _uiState.update { it.copy(heartbeat = symbol) }
    }

    fun setDeviceImage(devType: TDeviceType) {
        val res = when (devType) {
            TDeviceType.dLolly3, TDeviceType.dLolly4 -> R.drawable.dev_lolly
            TDeviceType.dTermoChron -> R.drawable.dev_wurst
            TDeviceType.dAD, TDeviceType.dAdMicro -> R.drawable.dev_ad
            else -> R.drawable.shape_circle
        }
        _uiState.update { it.copy(deviceImageRes = res) }
    }

    fun setMeteoMode(meteo: TMeteo, desc: String) {
        val res = when (meteo) {
            TMeteo.mBasic -> R.drawable.home_basic
            TMeteo.mMeteo -> R.drawable.home_meteo
            TMeteo.mSmart -> R.drawable.home_smart
            TMeteo.mIntensive -> R.drawable.home_5min // Předpokládám názvy z původního kódu
            TMeteo.mExperiment -> R.drawable.home_1min
            else -> R.drawable.shape_circle
        }
        _uiState.update {
            it.copy(
                mode = desc,
                modeImageRes = res
            )
        }
    }
}