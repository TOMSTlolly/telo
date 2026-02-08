package com.tomst.lolly.ui.home

import androidx.annotation.DrawableRes
import com.tomst.lolly.R

data class HomeUiState(
    // Hlavička
    val serialNumber: String = "Reading...",
    val appVersion: String = "v1.0.0", // Můžeš načíst z BuildConfig
    val tmdVersion: String = "1.92",
    val mode: String = "Basic",
    @DrawableRes val deviceImageRes: Int = R.drawable.dev_lollyhor, // Výchozí obrázek
    @DrawableRes val modeImageRes: Int = R.drawable.home_basic,

    // Stav adaptéru
    val connectionStatus: String = "Waiting for adapter...",
    val isAdapterConnected: Boolean = false,
    @DrawableRes val adapterImageRes: Int = R.drawable.adapter_red,

    // Průběh stahování
    val downloadProgress: Int = 0,
    val heartbeat: String = "/",
    val remainDays: String = "",

    // Diagnostika
    val memoryUsage: Int = 0,
    val humAD: String = "--",
    val humProc: Int = 0, // Pro progress bar
    val temp1: String = "--",
    val temp2: String = "--",
    val temp3: String = "--",

    // Čas
    val deviceTime: String = "--.--.----",
    val phoneTime: String = "--.--.----",
    val timeDiff: String = "---"
)