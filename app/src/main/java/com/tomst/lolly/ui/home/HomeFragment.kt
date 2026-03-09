package com.tomst.lolly.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tomst.lolly.LollyActivity
import com.tomst.lolly.LollyService
import com.tomst.lolly.R
import com.tomst.lolly.core.CSVReader
import com.tomst.lolly.core.Constants
import com.tomst.lolly.core.DmdViewModel
import com.tomst.lolly.core.TDevState
import com.tomst.lolly.core.TInfo
import com.tomst.lolly.core.TMSRec
import com.tomst.lolly.core.TMereni
import com.tomst.lolly.core.shared
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayList

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var dmd: DmdViewModel

    private var odometer: LollyService? = null
    private var bound = false

    private var csv: CSVReader? = null
    private var readWasFinished = false
    private var ALogFileName = ""
    private var AErrFileName = ""
    private var merold: TMereni? = null
    private var savelog: MutableList<String>? = null
    private var logs: MutableList<TMSRec> = ArrayList()
    private var serialNumber = "Unknown"
    private var heartIdx = 0

    private val MIN_ADANUMBER: Byte = 5

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
                    homeViewModel.updateConnectionStatus(ss, bound)
                }
            }
            merold = mer
            dmd.AddMereni(mer)

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
                    homeViewModel.updateConnectionStatus("NO HARDWARE !!!", false)
                    homeViewModel.updateProgress(0, 100)
                }
                TDevState.tAdapterDisconnected -> {
                    homeViewModel.updateConnectionStatus("Adapter disconnected", false)
                    homeViewModel.updateProgress(0, 100)
                }
                TDevState.tAdapterDead -> {
                    homeViewModel.updateConnectionStatus("Adapter needs firmware reflash", false)
                    homeViewModel.updateProgress(0, 100)
                }
                TDevState.tWaitForAdapter -> {
                    if (info.msg.length > MIN_ADANUMBER) {
                        homeViewModel.updateConnectionStatus(info.msg, true)
                        homeViewModel.updateProgress(0, 100)
                    }
                }
                TDevState.tTMDCycling -> homeViewModel.updateConnectionStatus(info.msg, true)
                TDevState.tBlockNumber -> homeViewModel.updateConnectionStatus(info.msg, true)
                TDevState.tHead -> {
                    info.fw.DeviceType?.let {
                        homeViewModel.setDeviceImage(it)
                        dmd.setDeviceType(it)
                    }
                    homeViewModel.updateDeviceVersion("Device fw: ${info.fw.Fw}.${info.fw.Sub}")
                }
                TDevState.tError -> {
                    homeViewModel.updateConnectionStatus(info.msg, bound)
                    savelog?.add(info.msg)
                }
                TDevState.tFirmwareIsActual -> homeViewModel.updateDeviceVersion(info.msg)
                TDevState.tFirmware -> {
                    homeViewModel.updateConnectionStatus(info.msg, bound)
                    savelog?.add(info.msg)
                }
                TDevState.tSerial -> {
                    serialNumber = info.msg
                    homeViewModel.updateSerialNumber(info.msg)
                    LollyActivity.getInstance().serialNumber = serialNumber
                    val trackDir = LollyActivity.getInstance().prefExportFolder
                    val csvFileName = shared.CompileFileName("data_", serialNumber, trackDir)
                    csv = CSVReader(csvFileName)
                    csv?.OpenForWrite(csvFileName)
                    ALogFileName = LollyActivity.getInstance().cacheLogPath + "/command_" + shared.aft(csvFileName, "data_")
                    AErrFileName = LollyActivity.getInstance().cacheLogPath + "/err_" + shared.aft(csvFileName, "data_")
                }
                TDevState.tInfo -> {
                    homeViewModel.updateDiagnostics(info.t1, info.t2, info.t3, info.humAd)
                    csv?.SetupFormat(info.devType)
                }
                TDevState.tCapacity -> {
                    val capUsed = try { info.msg.toInt() } catch (e: Exception) { 0 }
                    homeViewModel.updateMemory(capUsed)
                }
                TDevState.tCompareTime -> {
                    val formatter = DateTimeFormatter.ofPattern(Constants.DEVICE_FORMAT).withZone(ZoneId.systemDefault())
                    val phTime = LocalDateTime.now().format(formatter)
                    val delta = try { info.msg.toFloat() } catch (e: Exception) { 0f }
                    val diff = "%.1f".format(delta / 1000.0)
                    homeViewModel.updateTime(
                        devTime = homeViewModel.uiState.value.deviceTime,
                        phoneTime = phTime,
                        diff = diff
                    )
                }
                TDevState.tReadMeteo -> homeViewModel.setMeteoMode(info.meteo, info.msg)
                TDevState.tProgress -> {
                    val progress = info.idx
                    var remainStr = ""
                    if (info.currDay != null) {
                        val buttonFormat = DateTimeFormatter.ofPattern("YY-MM-dd").withZone(ZoneId.of("UTC"))
                        val sFmt = buttonFormat.format(info.currDay.atZone(ZoneId.of("UTC")))
                        remainStr = "$sFmt rem:${info.remainDays} days"
                    }

                    if (progress < 0) {
                        // Negative value indicates total number of records (max)
                        homeViewModel.updateProgress(0, -progress, remainStr)
                    } else {
                        // Positive value indicates current record index
                        homeViewModel.updateProgress(progress, -1, remainStr)
                    }
                    handleHeartbeat()
                }
                TDevState.tVrtule -> handleHeartbeat()
                TDevState.tReadType -> homeViewModel.updateConnectionStatus(info.msg, bound)
                TDevState.tFinishedData -> {
                    saveLogAndData()
                    readWasFinished = true
                    dmd.sendMessageToFragment("TMD $serialNumber")
                    switchToGraphFragment()
                }
                else -> {}
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LollyService.LollyBinder
            odometer = binder.odometer
            odometer?.let {
                it.SetHandler(handler)
                it.SetDataHandler(datahandler)
                it.SetLogHandler(loghandler)
                it.SetContext(context)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dmd = ViewModelProvider(requireActivity())[DmdViewModel::class.java]
        dmd.ClearMereni()
        savelog = LollyActivity.SAVE_LOG
        homeViewModel.updateSerialNumber("--------")

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val uiState by homeViewModel.uiState.collectAsState()
                    HomeScreen(
                        state = uiState,
                        onDebugAction = { action -> handleDebugAction(action) }
                    )
                }
            }
        }
    }

    private fun handleDebugAction(action: String) {
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
                        Toast.makeText(context, "Zahajuji flashování...", Toast.LENGTH_SHORT).show()
                        odometer?.startFirmwareFlash(fwFile.absolutePath)
                    } else {
                        Toast.makeText(context, "Soubor lolly.tau nenalezen", Toast.LENGTH_LONG).show()
                    }
                }
            }
            "t.Crash" -> throw RuntimeException("Test Crash")
        }
    }

    private fun handleHeartbeat() {
        val symbol = when (heartIdx) {
            0 -> "\\"
            1 -> "|"
            2 -> "/"
            3 -> "-"
            else -> "\\"
        }
        heartIdx = (heartIdx + 1) % 4
        homeViewModel.updateHeartbeat(symbol)
    }

    override fun onResume() {
        super.onResume()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(context, LollyService::class.java)
        context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        if (bound && odometer != null) {
            odometer?.enableLoop(false)
            context?.unbindService(connection)
            bound = false
        }
        super.onStop()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroyView() {
        if (!readWasFinished) saveLogAndData()
        super.onDestroyView()
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

    private fun switchToGraphFragment() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNav.selectedItemId = R.id.navigation_graph
    }
}
