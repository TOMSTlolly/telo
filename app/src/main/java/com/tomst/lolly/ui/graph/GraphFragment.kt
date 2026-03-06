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
import com.tomst.lolly.LollyActivity
import com.tomst.lolly.core.CSVReader
import com.tomst.lolly.core.DmdViewModel
import com.tomst.lolly.core.TDeviceType
import com.tomst.lolly.core.TMereni
import com.tomst.lolly.core.shared.getSerialNumberFromFileName

@RequiresApi(Build.VERSION_CODES.O)
class GraphFragment : Fragment() {

    private val TAG = "GraphFragment"
    //private val viewModel: GraphViewModel by viewModels()
    private val viewModel: GraphViewModel by activityViewModels()
    private val dmd: DmdViewModel by activityViewModels() // Sdílený s aktivitou

    private var fSerialNumber = ""

    //private var lastLoadedFileName = ""

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Pozorovat zprávy z DmdViewModel (stejně jako v Javě)
        dmd.messageContainerToFragment.observe(viewLifecycleOwner) { msg ->
                Log.d("GRAPH", "Received: $msg")
            val parts = msg.split(" ")

            if (parts[0] == "TMD") {
                // Data z USB
                if (parts.size > 1) {
                    fSerialNumber = parts[1]
                }
                setGraphTitleAndCheckboxes()
                viewModel.notifyDataChanged()
            } else {
                // Data ze souboru (kliknutí v ListFragment)
                val fileNames = msg.split(";")
                if (fileNames.isNotEmpty() && fileNames[0].isNotEmpty()) {
                    val fileName = fileNames[0]
                    if (loadCSVFil(fileName)) {
                        fSerialNumber = getSerialNumberFromFileName(fileName)
                        //lastLoadedFileName = fileName // Uložíme si, že tento soubor už máme
                    }
                }
            }
            // Odregistrovat, aby se nenačítalo znovu při rotaci
            //dmd.messageContainerToFragment.removeObservers(viewLifecycleOwner)
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    GraphScreen(
                            viewModel = viewModel,
                            dmdViewModel = dmd
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (LollyActivity.getInstance()?.prefRotateGraph == true) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        // PŘIDÁNO: Vynucený repaint po návratu z jiné záložky
        viewModel.notifyDataChanged()
    }

    override fun onStop() {
      //  dmd.sendMessageToFragment("")
      //  dmd.ClearMereni()
        super.onStop()
    }

    // Handler pro CSV Reader (musí zůstat, protože CSVReader používá starý Android Handler)
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val mer = msg.obj as TMereni
            dmd.AddMereni(mer)
        }
    }

    // Přidej si do třídy GraphFragment proměnnou pro sledování kroku:
    private var lastDrawnStep = 0

    // A takto uprav metodu loadCSVFil:
    private fun loadCSVFil(uriPath: String): Boolean {
        dmd.ClearMereni()
        lastDrawnStep = 0 // Reset kroku při novém souboru

        val fileUri = Uri.parse(uriPath)
        val csv = CSVReader(fileUri.toString())
        csv.SetHandler(handler)

        csv.SetProgressListener { value ->
            if (value < 0) {
                // Max hodnota
                viewModel.updateProgress(0, max = -value)
            } else {
                // Aktuální progress (updatuje jen čáru nahoře, graf už ne)
                viewModel.updateProgress(value)

                // --- LOGIKA PRO 5 KROKŮ ---
                val max = viewModel.uiState.value.maxProgress
                if (max > 0) {
                    // Vypočítáme procenta
                    val percent = (value * 100) / max

                    // Každých 20% znamená jeden krok (0, 1, 2, 3, 4, 5)
                    val step = percent / 20

                    // Pokud jsme překročili hranici nového kroku, překreslíme graf
                    if (step > lastDrawnStep) {
                        lastDrawnStep = step
                        viewModel.notifyDataChanged() // Změní dataTimestamp a povolí AndroidView update
                    }
                }
            }
        }

        csv.SetFinListener { _ ->
            val det = csv.fileDetail
            dmd.setDeviceType(det.deviceType)
            setGraphTitleAndCheckboxes()
            Log.d(TAG, "Finished loading CSV")

            lastDrawnStep = 0 // Uklidíme
            viewModel.notifyDataChanged() // Finální stoprocentní překreslení po dokončení načítání
        }

        csv.start()

        viewModel.setHighlightingEnabled(true)

        return true
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
        //viewModel.setTitle("$fSerialNumber / $title")
        //activity?.title = "$fSerialNumber / $title"
        viewModel.setTitle("$fSerialNumber / $title")
    }
}