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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState // TOTO CHYBĚLO
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

    // Logování souborů
    private var csv: CSVReader? = null
    private var readWasFinished = false
    private var ALogFileName = ""
    private var AErrFileName = ""
    private var merold: TMereni? = null
    private var savelog: MutableList<String>? = null
    private var logs: MutableList<TMSRec> = ArrayList()
    private var serialNumber = "Unknown"
    private var heartIdx = 0

    // Konstanta z původního kódu
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
                    homeViewModel.updateConnectionStatus(ss, bound) // Zobrazíme chybu v statusu
                }
            }
            merold = mer
            dmd.AddMereni(mer)

            if (csv == null) {
                val ss = "CSV is null, cannot save data"
                savelog?.add(ss)
                // homeViewModel.updateConnectionStatus(ss, bound)
            } else {
                csv?.AddMerToCsv(mer)
            }
        }
    }

    // 2. Handler pro logování (LogHandler)
    private val loghandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val log = msg.obj as? TMSRec
            if (log != null) {
                logs.add(log)
            }
        }
    }

    // 3. Hlavní stavový handler (Handler)
    private val handler = object : Handler(Looper.getMainLooper()) {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun handleMessage(msg: Message) {
            val info = msg.obj as? TInfo ?: return
            Log.d(Constants.TAG, "${info.idx} ${info.msg}")

            when (info.stat) {
                TDevState.tNoHardware -> {
                    homeViewModel.updateConnectionStatus("NO HARDWARE !!!", false)
                    homeViewModel.updateProgress(0)
                }
                TDevState.tAdapterDisconnected -> {
                    homeViewModel.updateConnectionStatus("Adapter disconnected", false)
                    homeViewModel.updateProgress(0)
                }
                TDevState.tAdapterDead -> {
                    homeViewModel.updateConnectionStatus("Adapter needs firmware reflash", false)
                    homeViewModel.updateProgress(0)
                }
                TDevState.tWaitForAdapter -> {
                    if (info.msg.length > MIN_ADANUMBER) {
                        homeViewModel.updateConnectionStatus(info.msg, true)
                        homeViewModel.updateProgress(0)
                    }
                }
                TDevState.tTMDCycling -> {
                    homeViewModel.updateConnectionStatus(info.msg, true)
                }
                TDevState.tBlockNumber -> {
                    homeViewModel.updateConnectionStatus(info.msg, true)
                }
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
                TDevState.tFirmwareIsActual -> {
                    homeViewModel.updateDeviceVersion(info.msg)
                }
                TDevState.tFirmware -> {
                    homeViewModel.updateConnectionStatus(info.msg, bound)
                    savelog?.add(info.msg)
                }
                TDevState.tSerial -> {
                    serialNumber = info.msg
                    homeViewModel.updateSerialNumber(info.msg)

                    // Inicializace CSV
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
                TDevState.tGetTime -> {
                    // val time = if (info.msg.isNotEmpty()) info.msg else "Invalid time"
                    // homeViewModel.updateTime(...)
                }
                TDevState.tGetTimeError -> {
                    // Handle error
                }
                TDevState.tCompareTime -> {
                    val formatter = DateTimeFormatter.ofPattern(Constants.DEVICE_FORMAT).withZone(ZoneId.systemDefault())
                    val phTime = LocalDateTime.now().format(formatter)

                    val delta = try { info.msg.toFloat() } catch (e: Exception) { 0f }
                    val diff = String.format("%.1f", delta / 1000.0)

                    homeViewModel.updateTime(
                        devTime = homeViewModel.uiState.value.deviceTime,
                        phoneTime = phTime,
                        diff = diff
                    )
                }
                TDevState.tReadMeteo -> {
                    homeViewModel.setMeteoMode(info.meteo, info.msg)
                }
                TDevState.tProgress -> {
                    //val progress = if (info.idx < 0) -info.idx else info.idx
                    val progress = info.idx;

                    var remainStr = ""
                    if (info.currDay != null) {
                        val buttonFormat = DateTimeFormatter.ofPattern("YY-MM-dd").withZone(ZoneId.of("UTC"))
                        val sFmt = buttonFormat.format(info.currDay)
                        remainStr = "$sFmt rem:${info.remainDays} days"
                    }

                    homeViewModel.updateProgress(progress, 100, remainStr)
                    handleHeartbeat()
                }
                TDevState.tVrtule -> {
                    handleHeartbeat()
                }
                TDevState.tReadType -> {
                    homeViewModel.updateConnectionStatus(info.msg, bound)
                }
                TDevState.tFinishedData -> {
                    saveLogAndData()
                    readWasFinished = true

                    val showGraph = requireContext().getSharedPreferences("save_options", Context.MODE_PRIVATE)
                        .getBoolean("showgraph", false)

                    if (true) {
                        dmd.sendMessageToFragment("TMD $serialNumber")
                        switchToGraphFragment()
                    }
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
        // Inicializace
        dmd = ViewModelProvider(requireActivity())[DmdViewModel::class.java]
        dmd.ClearMereni()

        // OPRAVA: V Kotlinu přistupujeme ke statickým proměnným Javy přes třídu, ne přes instanci
        savelog = LollyActivity.SAVE_LOG

        // Nastavení defaultních hodnot
        homeViewModel.updateSerialNumber("0123456789")
        homeViewModel.updateTime("01.01.2000 12:34:56", "01.01.2000 12:34:56", "---")

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    // OPRAVA: Přidán import collectAsState
                    val uiState = homeViewModel.uiState.collectAsState().value

                    HomeScreen(
                        state = uiState,
                        onDebugAction = { action ->
                            handleDebugAction(action)
                        }
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
                    // OPRAVA: Přístup ke statické proměnné DIRECTORY_FW
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
        heartIdx = if (heartIdx >= 3) 0 else heartIdx + 1
        homeViewModel.updateHeartbeat(symbol)
    }

    // --- Lifecycle Metody ---

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

    // --- Pomocné metody (File I/O) ---

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
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNav?.findViewById<View>(R.id.navigation_graph)?.performClick()
    }
}