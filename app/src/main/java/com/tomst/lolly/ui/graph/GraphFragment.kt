package com.tomst.lolly.ui.graph

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels

// --- Importy Vico 2 ---
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

// --- Importy Lolly ---
import com.tomst.lolly.LollyActivity
import com.tomst.lolly.core.CSVReader
import com.tomst.lolly.core.DmdViewModel
import com.tomst.lolly.core.TDeviceType
import com.tomst.lolly.core.TMereni
import com.tomst.lolly.core.shared.getSerialNumberFromFileName

@RequiresApi(Build.VERSION_CODES.O)
class GraphFragment : Fragment() {

    private val TAG = "GraphFragment"
    private val viewModel: GraphViewModel by viewModels()
    private val dmd: DmdViewModel by activityViewModels()

    private var fSerialNumber = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dmd.messageContainerToFragment.observe(viewLifecycleOwner) { msg ->
            Log.d("GRAPH", "Received: $msg")
            val parts = msg.split(" ")

            if (parts[0] == "TMD") {
                if (parts.size > 1) {
                    fSerialNumber = parts[1]
                }
                setGraphTitleAndCheckboxes()
                loadDmdDataIntoViewModel()
            } else {
                val fileNames = msg.split(";")
                if (fileNames.isNotEmpty() && fileNames[0].isNotEmpty()) {
                    val fileName = fileNames[0]
                    if (loadCSVFil(fileName)) {
                        fSerialNumber = getSerialNumberFromFileName(fileName)
                    }
                }
            }
            dmd.messageContainerToFragment.removeObservers(viewLifecycleOwner)
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    GraphScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (LollyActivity.getInstance()?.prefRotateGraph == true) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onStop() {
        dmd.sendMessageToFragment("")
        dmd.ClearMereni()
        super.onStop()
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val mer = msg.obj as TMereni
            dmd.AddMereni(mer)
        }
    }

    private fun loadCSVFil(uriPath: String): Boolean {
        val fileUri = Uri.parse(uriPath)
        val csv = CSVReader(fileUri.toString())
        csv.SetHandler(handler)

        csv.SetProgressListener { value ->
            if (value < 0) {
                viewModel.updateProgress(0, max = -value)
            } else {
                viewModel.updateProgress(value)
            }
        }

        csv.SetFinListener { _ ->
            val det = csv.fileDetail
            dmd.setDeviceType(det.deviceType)
            setGraphTitleAndCheckboxes()
            Log.d(TAG, "Finished loading CSV")
            loadDmdDataIntoViewModel()
        }

        csv.start()
        return true
    }

    private fun loadDmdDataIntoViewModel() {
        val t1 = dmd.getT1().associate { it.x to it.y }
        val t2 = dmd.getT2().associate { it.x to it.y }
        val t3 = dmd.getT3().associate { it.x to it.y }
        val hum = dmd.getHA().associate { it.x to it.y }

        val producer = CartesianChartModelProducer.build()

        producer.tryRunTransaction {
            // První vrstva (Teploty) se přidá pouze pokud má ALESPOŇ JEDNA řada nějaká data
            if (t1.isNotEmpty() || t2.isNotEmpty() || t3.isNotEmpty()) {
                lineSeries {
                    // Přidáváme konkrétní sérii, jen pokud není prázdná
                    if (t1.isNotEmpty()) series(t1.keys, t1.values)
                    if (t2.isNotEmpty()) series(t2.keys, t2.values)
                    if (t3.isNotEmpty()) series(t3.keys, t3.values)
                }
            }

            // Druhá vrstva (Vlhkost) se přidá POUZE pokud pro ni máme data
            if (hum.isNotEmpty()) {
                lineSeries {
                    series(hum.keys, hum.values)
                }
            }
        }

        viewModel.setGraphData(producer)
    }

    private fun setGraphTitleAndCheckboxes() {
        val title: String
        when (dmd.GetDeviceType()) {
            TDeviceType.dLolly4, TDeviceType.dLolly3 -> {
                title = if (dmd.GetDeviceType() == TDeviceType.dLolly4) "TMS-4" else "TMS-3"
                viewModel.toggleLineVisibility(1, true)
                viewModel.toggleLineVisibility(2, true)
                viewModel.toggleLineVisibility(3, true)
                viewModel.toggleLineVisibility(4, true)
            }
            TDeviceType.dAD, TDeviceType.dAdMicro -> {
                title = "Dendrometer"
                viewModel.toggleLineVisibility(1, true)
                viewModel.toggleLineVisibility(2, false)
                viewModel.toggleLineVisibility(3, false)
                viewModel.toggleLineVisibility(4, true)
            }
            TDeviceType.dTermoChron -> {
                title = "Thermochron"
                viewModel.toggleLineVisibility(1, true)
                viewModel.toggleLineVisibility(2, false)
                viewModel.toggleLineVisibility(3, false)
                viewModel.toggleLineVisibility(4, false)
            }
            else -> title = "Unknown"
        }
        viewModel.setTitle("$fSerialNumber / $title")
    }
}