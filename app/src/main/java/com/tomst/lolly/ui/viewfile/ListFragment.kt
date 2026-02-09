package com.tomst.lolly.ui.viewfile

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tomst.lolly.BuildConfig
import com.tomst.lolly.LollyActivity
import com.tomst.lolly.R
import com.tomst.lolly.core.DmdViewModel
import com.tomst.lolly.core.EventBusMSG
import com.tomst.lolly.core.ZipFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ListFragment : Fragment() {

    // Inicializace ViewModelu (nahrazuje `new ViewModelProvider...`)
    private val viewModel: ListViewModel by viewModels()
    private val dmd: DmdViewModel by activityViewModels()

    // Registrace EventBusu při startu
    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    // Odregistrace při stopu
    override fun onStop() {
        super.onStop()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    // Metoda, která se zavolá, když Activity pošle signál UPDATE_TRACKLIST
    @RequiresApi(Build.VERSION_CODES.O)
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: Short) { // V Javě posíláme Short
        if (event == EventBusMSG.UPDATE_TRACKLIST) {
            // Tady donutíme ViewModel znovu načíst data s novou cestou
            viewModel.loadFiles()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Vynucení orientace (tvůj starý kód)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewModel.loadFiles()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Místo XML layoutu vracíme ComposeView
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    // Zde voláme naši Compose obrazovku
                    FilesScreen(
                        viewModel = viewModel,
                        onGraphClick = { fileName ->

                            dmd.sendMessageToFragment(fileName)
                            // Logika pro přepnutí na graf
                            switchToGraphFragment()
                        },
                        onZipLogsClick = {
                            showZipDialog()
                        },
                        onZipAllClick = {
                            shareExportedData()
                        },
                        onSelectFolderClick = {
                           // Zavoláme veřejnou metodu z LollyActivity
                           (requireActivity() as? LollyActivity)?.openDirectoryPicker()
                        }
                    )
                }
            }
        }
    }

    // --- Původní logika z Javy přepsaná do Kotlinu ---

    private fun switchToGraphFragment() {
        val activity = requireActivity()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNav?.findViewById<View>(R.id.navigation_graph)?.performClick()
    }

    private fun showZipDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setText("logs_${System.currentTimeMillis()}.zip")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Enter ZIP file name")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                var zipFileName = input.text.toString().trim()
                if (zipFileName.isEmpty()) {
                    Toast.makeText(context, "File name cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    if (!zipFileName.lowercase().endsWith(".zip")) {
                        zipFileName += ".zip"
                    }
                    zipLogsDirectory(zipFileName)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun zipLogsDirectory(zipFileName: String) {
        val context = context ?: return
        val cacheDir = context.cacheDir
        val logsDir = File(cacheDir, "Logs")

        if (!logsDir.exists() || !logsDir.isDirectory || (logsDir.listFiles()?.isEmpty() == true)) {
            Toast.makeText(context, "Logs directory is empty or does not exist.", Toast.LENGTH_SHORT).show()
            return
        }

        val zipFile = File(cacheDir, zipFileName)

        // Spuštění na pozadí pomocí Coroutines
        lifecycleScope.launch(Dispatchers.IO) {
            // Aktualizace UI - začátek (přes ViewModel)
            viewModel.updateInfoText("Zipping logs...")

            val zipFiles = ZipFiles()

            // Listener pro progress bar
            val progressListener = com.tomst.lolly.core.OnProListener { progress ->
                // Zde bychom mohli aktualizovat progress ve ViewModelu, pokud chceme
            }

            val success = zipFiles.zipDirectory(logsDir, zipFile.absolutePath, progressListener)

            withContext(Dispatchers.Main) {
                if (success) {
                    val body = getDeviceDiagnostics()
                    shareZipFile(
                        zipFile,
                        "Lolly App Logs",
                        "Attached are the zipped log files from the Lolly phone app.\r\n$body"
                    )
                    viewModel.updateInfoText("Logs zipped successfully")
                } else {
                    Toast.makeText(context, "Failed to create zip file.", Toast.LENGTH_SHORT).show()
                    viewModel.updateInfoText("Failed to zip logs")
                }
            }
        }
    }

    private fun shareExportedData() {
        val context = context ?: return
        val sharedPath = LollyActivity.getInstance().prefExportFolder ?: ""

        // Získání složky
        val exportFolder = if (sharedPath.startsWith("content")) {
            DocumentFile.fromTreeUri(context, Uri.parse(sharedPath))
        } else {
            DocumentFile.fromFile(File(sharedPath))
        }

        if (exportFolder == null || !exportFolder.isDirectory) {
            Toast.makeText(context, "Export folder not found", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val zipFileName = "exported_data_$timestamp.zip"
        val zipFile = File(context.cacheDir, zipFileName)

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.updateInfoText("Zipping all data...")

            val zipFiles = ZipFiles()
            val progressListener = com.tomst.lolly.core.OnProListener { progress ->
                // Update progress
            }

            val success = zipFiles.zipDocumentFileDirectory(exportFolder, zipFile.absolutePath, context, progressListener)

            withContext(Dispatchers.Main) {
                if (success) {
                    val emailBody = "Here is the content of my export from the Lolly phone app.\n\n" +
                            "--- Device Info ---\n${getDeviceDiagnostics()}"

                    shareZipFile(
                        zipFile,
                        "Lolly App Export - ${Build.MODEL}",
                        emailBody
                    )
                    viewModel.updateInfoText("Data exported successfully")
                } else {
                    viewModel.updateInfoText("Export failed")
                }
            }
        }
    }

    private fun shareZipFile(zipFile: File, subject: String, text: String) {
        val context = context ?: return
        if (!zipFile.exists()) {
            Toast.makeText(context, "File to share not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val zipUri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.provider",
            zipFile
        )

        // Intent pro email
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("krata@tomst.com"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, zipUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Obecný intent pro sdílení
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, zipUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Zip File via...")

        // Přidání emailových intentů do chooseru
        val resInfoList = context.packageManager.queryIntentActivities(emailIntent, 0)
        val extraIntents = resInfoList.map { resolveInfo ->
            Intent(emailIntent).apply {
                setPackage(resolveInfo.activityInfo.packageName)
            }
        }.toTypedArray()

        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)

        try {
            startActivity(chooser)
        } catch (ex: Exception) {
            Toast.makeText(context, "No app found to handle this action.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDeviceDiagnostics(): String {
        val context = context
        val sb = StringBuilder()

        sb.append("--- App Info ---\n")
        sb.append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        sb.append("App ID: ${BuildConfig.APPLICATION_ID}\n")

        sb.append("\n--- Device Info ---\n")
        sb.append("Manufacturer: ${Build.MANUFACTURER}\n")
        sb.append("Model: ${Build.MODEL}\n")
        sb.append("Product: ${Build.PRODUCT}\n")
        sb.append("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")

        if (context != null) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)

            sb.append("\n--- Memory ---\n")
            sb.append("Total: ${memInfo.totalMem / (1024 * 1024)} MB\n")
            sb.append("Available: ${memInfo.availMem / (1024 * 1024)} MB\n")
        }

        sb.append("\nTimestamp: ${Date()}\n")
        return sb.toString()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ListFragment()
    }
}
