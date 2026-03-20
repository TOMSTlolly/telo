package com.tomst.lolly.ui.home

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import com.tomst.lolly.LollyActivity
import com.tomst.lolly.LollyService
import com.tomst.lolly.R
import com.tomst.lolly.core.CSVReader
import com.tomst.lolly.core.Constants
import com.tomst.lolly.core.DmdViewModel
import com.tomst.lolly.core.TDevState
import com.tomst.lolly.core.TDeviceType
import com.tomst.lolly.core.TInfo
import com.tomst.lolly.core.TMSRec
import com.tomst.lolly.core.TMereni
import com.tomst.lolly.core.TMeteo
import com.tomst.lolly.core.shared
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayList

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // --- HARDWARE BACKEND STATE ---
    private var odometer: LollyService? = null
    var bound = false
        private set

    private var csv: CSVReader? = null
    var readWasFinished = false
        private set
        
    private var ALogFileName = ""
    private var AErrFileName = ""
    private var merold: TMereni? = null
    private var savelog: MutableList<String>? = LollyActivity.SAVE_LOG
    private var logs: MutableList<TMSRec> = ArrayList()
    private var heartIdx = 0

    var dmd: DmdViewModel? = null
    var onFinishedDataCallback: (() -> Unit)? = null

    // --- HANDLERS ---
    private val datahandler = object : Handler(Looper.getMainLooper()) {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun handleMessage(msg: Message) {
            val mer = msg.obj as? TMereni ?: return

            if (merold != null && mer.dtm != null && merold?.dtm != null) {
                val delta = Duration.between(merold!!.dtm, mer.dtm).seconds
                if (delta > Constants.MAX_DELTA) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val meroFormatted = merold!!.dtm.format(formatter)
                    val merFormatted = mer.dtm.format(formatter)
                    val ss = "Between messages >$delta seconds (from $meroFormatted to $merFormatted)"
                    savelog?.add(ss)
                    updateConnectionStatus(ss, bound)
                }
            }
            merold = mer
            dmd?.AddMereni(mer)

            if (csv == null) {
                val ss = "CSV is null, cannot save data"
                savelog?.add(ss)
            } else {
                csv?.AddMerToCsv(mer)
            }
        }
    }

    private val loghandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val log = msg.obj as? TMSRec
            if (log != null) {
                logs.add(log)
            }
        }
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun handleMessage(msg: Message) {
            val info = msg.obj as? TInfo ?: return

            when (info.stat) {
                TDevState.tNoHardware -> {
                    updateConnectionStatus("NO HARDWARE !!!", false)
                    updateProgress(0, 100)
                }
                TDevState.tAdapterDisconnected -> {
                    updateConnectionStatus("Adapter disconnected", false)
                    updateProgress(0, 100)
                }
                TDevState.tAdapterDead -> {
                    updateConnectionStatus("Adapter needs firmware reflash", false)
                    updateProgress(0, 100)
                }
                TDevState.tWaitForAdapter -> {
                    if (info.msg.length > 5) { // MIN_ADANUMBER
                        updateConnectionStatus(info.msg, true)
                        updateProgress(0, 100)
                    }
                }
                TDevState.tTMDCycling -> updateConnectionStatus(info.msg, true)
                TDevState.tBlockNumber -> updateConnectionStatus(info.msg, true)
                TDevState.tHead -> {
                    info.fw.DeviceType?.let {
                        setDeviceImage(it)
                        dmd?.setDeviceType(it)
                    }
                    updateDeviceVersion("Device fw: ${info.fw.Fw}.${info.fw.Sub}")
                }
                TDevState.tError -> {
                    updateConnectionStatus(info.msg, bound)
                    savelog?.add(info.msg)
                }
                TDevState.tFirmwareIsActual -> updateDeviceVersion(info.msg)
                TDevState.tFirmware -> {
                    updateConnectionStatus(info.msg, bound)
                    savelog?.add(info.msg)
                }
                TDevState.tSerial -> {
                    val serialNumber = info.msg
                    updateSerialNumber(info.msg)
                    LollyActivity.getInstance().serialNumber = serialNumber
                    dmd?.ClearMereni() // Ensure old graph data is wiped before new data streams in
                    val trackDir = LollyActivity.getInstance().prefExportFolder
                    val csvFileName = shared.CompileFileName("data_", serialNumber, trackDir)
                    csv = CSVReader(csvFileName)
                    csv?.OpenForWrite(csvFileName)
                    ALogFileName = LollyActivity.getInstance().cacheLogPath + "/command_" + shared.aft(csvFileName, "data_")
                    AErrFileName = LollyActivity.getInstance().cacheLogPath + "/err_" + shared.aft(csvFileName, "data_")
                }
                TDevState.tInfo -> {
                    updateDiagnostics(info.t1, info.t2, info.t3, info.humAd)
                    csv?.SetupFormat(info.devType)
                }
                TDevState.tCapacity -> {
                    val capUsed = try { info.msg.toInt() } catch (e: Exception) { 0 }
                    updateMemory(capUsed)
                }
                TDevState.tCompareTime -> {
                    val formatter = DateTimeFormatter.ofPattern(Constants.DEVICE_FORMAT).withZone(ZoneId.systemDefault())
                    val phTime = LocalDateTime.now().format(formatter)
                    val delta = try { info.msg.toFloat() } catch (e: Exception) { 0f }
                    val diff = "%.1f".format(delta / 1000.0)
                    updateTime(
                        devTime = _uiState.value.deviceTime,
                        phoneTime = phTime,
                        diff = diff
                    )
                }
                TDevState.tReadMeteo -> setMeteoMode(info.meteo, info.msg)
                TDevState.tProgress -> {
                    val progress = info.idx
                    var remainStr = ""
                    if (info.currDay != null) {
                        val buttonFormat = DateTimeFormatter.ofPattern("YY-MM-dd").withZone(ZoneId.of("UTC"))
                        val sFmt = buttonFormat.format(info.currDay.atZone(ZoneId.of("UTC")))
                        remainStr = "$sFmt rem:${info.remainDays} days"
                    }

                    if (progress < 0) {
                        updateProgress(0, -progress, remainStr)
                    } else {
                        updateProgress(progress, -1, remainStr)
                    }
                    handleHeartbeat()
                }
                TDevState.tVrtule -> handleHeartbeat()
                TDevState.tReadType -> updateConnectionStatus(info.msg, bound)
                TDevState.tFinishedData -> {
                    saveLogAndData()
                    readWasFinished = true
                    dmd?.sendMessageToFragment("TMD ${_uiState.value.serialNumber}")
                    onFinishedDataCallback?.invoke()
                }
                else -> {}
            }
        }
    }

    // --- SERVICE CONNECTION ---
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LollyService.LollyBinder
            odometer = binder.odometer
            odometer?.let {
                it.SetHandler(handler)
                it.SetDataHandler(datahandler)
                it.SetLogHandler(loghandler)
                it.SetContext(getApplication())
                it.startBindService()
                it.enableLoop(true)
            }
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            odometer = null
        }
    }

    fun bindHardwareService() {
        if (!bound) {
            val intent = Intent(getApplication(), LollyService::class.java)
            getApplication<Application>().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindHardwareService() {
        if (bound) {
            odometer?.enableLoop(false)
            getApplication<Application>().unbindService(connection)
            bound = false
        }
    }

    // --- LOGIC ---
    private fun handleHeartbeat() {
        val symbol = when (heartIdx) {
            0 -> "\\"
            1 -> "|"
            2 -> "/"
            3 -> "-"
            else -> "\\"
        }
        heartIdx = (heartIdx + 1) % 4
        updateHeartbeat(symbol)
    }

    fun handleDebugAction(action: String) {
        when (action) {
            "t.Tup" -> {
                ALogFileName = LollyActivity.getInstance().cacheLogPath + "/testlog.csv"
                saveLogAndData()
            }
            "t.Blowfish" -> {
                if (bound && odometer != null) {
                    odometer?.enableLoop(false)
                    val fwFile = File(LollyActivity.DIRECTORY_FW, "lolly.tau")
                    if (fwFile.exists()) {
                        Toast.makeText(getApplication(), "Zahajuji flashování...", Toast.LENGTH_SHORT).show()
                        odometer?.startFirmwareFlash(fwFile.absolutePath)
                    } else {
                        Toast.makeText(getApplication(), "Soubor lolly.tau nenalezen", Toast.LENGTH_LONG).show()
                    }
                }
            }
            "t.Crash" -> throw RuntimeException("Test Crash")
        }
    }

    fun cleanup() {
        if (!readWasFinished) saveLogAndData()
        unbindHardwareService()
    }

    private fun saveLogAndData() {
        csv?.CloseExternalCsv()
        csv = null
        saveLogToFile(ALogFileName)
        saveLogErr(AErrFileName)
    }

    private fun saveLogErr(fileName: String) {
        if (savelog == null || fileName.isEmpty()) return
        try {
            BufferedWriter(FileWriter(fileName)).use { writer ->
                for (log in savelog!!) {
                    writer.write(log)
                    writer.newLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveLogToFile(fileName: String) {
        if (fileName.isEmpty()) return
        try {
            BufferedWriter(FileWriter(fileName)).use { writer ->
                for (log in logs) {
                    writer.write("<<${log.sCmd}")
                    writer.newLine()
                    writer.write(">>${log.sRsp}")
                    writer.newLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        logs.clear()
    }

    // --- UI UPDATES ---
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
        val proc = ((humAd / com.tomst.lolly.core.Constants.MAX_HUM) * 100).toInt()
        _uiState.update {
            it.copy(
                temp1 = "%.1f".format(t1),
                temp2 = "%.1f".format(t2),
                temp3 = "%.1f".format(t3),
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

    fun updateProgress(progress: Int, max: Int = -1, remainDaysInfo: String = "") {
        _uiState.update {
            it.copy(
                downloadProgress = progress,
                maxProgress = if (max != -1) max else it.maxProgress,
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
            TDeviceType.dAD, TDeviceType.dAdMicro -> R.drawable.dev_dendro
            else -> R.drawable.shape_circle
        }
        _uiState.update { it.copy(deviceImageRes = res) }
    }

    fun setMeteoMode(meteo: TMeteo, desc: String) {
        val res = when (meteo) {
            TMeteo.mBasic -> R.drawable.home_basic
            TMeteo.mMeteo -> R.drawable.home_meteo
            TMeteo.mSmart -> R.drawable.home_smart
            TMeteo.mIntensive -> R.drawable.home_5min
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
