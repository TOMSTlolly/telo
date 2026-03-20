package com.tomst.lolly.ui.viewfile

import android.app.Application
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomst.lolly.LollyActivity
import com.tomst.lolly.core.CSVReader
import com.tomst.lolly.core.Constants
import com.tomst.lolly.core.DatabaseHandler
import com.tomst.lolly.core.shared
import com.tomst.lolly.fileview.FileDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import com.tomst.lolly.core.TDeviceType

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.tomst.lolly.BuildConfig
import com.tomst.lolly.core.ZipFiles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListViewModel(application: Application) : AndroidViewModel(application) {

    fun zipLogsDirectory(zipFileName: String, context: Context) {
        val cacheDir = context.cacheDir
        val logsDir = File(cacheDir, "Logs")

        if (!logsDir.exists() || !logsDir.isDirectory ) {
            Toast.makeText(context, "Logs directory does not exist.", Toast.LENGTH_SHORT).show()
            return
        }

        if (logsDir.listFiles()?.isEmpty() == true) {
            Toast.makeText(context, "Logs directory is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val zipFile = File(cacheDir, zipFileName)

        viewModelScope.launch(Dispatchers.IO) {
            updateInfoText("Zipping logs...")

            val zipFiles = ZipFiles()
            val progressListener = com.tomst.lolly.core.OnProListener { progress ->
                updateProgress(progress)
            }

            val success = zipFiles.zipDirectory(logsDir, zipFile.absolutePath, progressListener)

            withContext(Dispatchers.Main) {
                if (success) {
                    val body = getDeviceDiagnostics(context)
                    shareZipFile(
                        zipFile,
                        "Lolly App Logs",
                        "Attached are the zipped log files from the Lolly phone app.\r\n$body",
                        context
                    )
                    updateInfoText("Logs zipped successfully")
                } else {
                    Toast.makeText(context, "Failed to create zip file.", Toast.LENGTH_SHORT).show()
                    updateInfoText("Failed to zip logs")
                }
            }
        }
    }

    fun shareExportedData(note: String, context: Context) {
        val selectedFiles = uiState.value.files.filter { it.isSelected }

        if (selectedFiles.isEmpty()) {
            Toast.makeText(context, "No files selected to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val datePart = sdf.format(Date())
        val timePart = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())

        val finalNote = if (note.isEmpty()) timePart else note.trim()
        val zipFileName = "lolly_export_${datePart}_${selectedFiles.size}_$finalNote.zip"
        val zipFile = File(context.cacheDir, zipFileName)

        val documentFiles = selectedFiles.mapNotNull { fileDetail ->
            try {
                DocumentFile.fromSingleUri(context, Uri.parse(fileDetail.internalFullName))
            } catch (e: Exception) {
                null
            }
        }

        if (documentFiles.isEmpty()) {
            Toast.makeText(context, "Could not resolve selected files.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            updateInfoText("Zipping selected data...")

            val zipFiles = ZipFiles()
            val progressListener = com.tomst.lolly.core.OnProListener { progress ->
                updateProgress(progress)
            }

            val success = zipFiles.zipDocumentFileList(documentFiles, zipFile.absolutePath, context, progressListener)

            withContext(Dispatchers.Main) {
                if (success) {
                    val emailBody = "Here is the exported data from the Lolly phone app.\n\n" +
                            "--- Device Info ---\n${getDeviceDiagnostics(context)}"

                    shareZipFile(
                        zipFile,
                        "Lolly App Export - ${Build.MODEL}",
                        emailBody,
                        context
                    )
                    updateInfoText("Data exported successfully")
                } else {
                    updateInfoText("Export failed")
                    Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareZipFile(zipFile: File, subject: String, text: String, context: Context) {
        if (!zipFile.exists()) {
            Toast.makeText(context, "File to share not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val zipUri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.provider",
            zipFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("krata@tomst.com"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, zipUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share ZIP File"))
    }

    private fun getDeviceDiagnostics(context: Context): String {
        val sb = StringBuilder()
        sb.append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        sb.append("OS Version: ${System.getProperty("os.version")} (${Build.VERSION.INCREMENTAL})\n")
        sb.append("OS API Level: ${Build.VERSION.SDK_INT}\n")
        sb.append("Device: ${Build.DEVICE}\n")
        sb.append("Model (and Product): ${Build.MODEL} (${Build.PRODUCT})\n")

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        if (activityManager != null) {
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            sb.append("Total Memory: ${memInfo.totalMem / (1024 * 1024)} MB\n")
            sb.append("Available Memory: ${memInfo.availMem / (1024 * 1024)} MB\n")
        }

        sb.append("\nTimestamp: ${Date()}\n")
        return sb.toString()
    }

    private val db = DatabaseHandler(application)
    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            loadFiles()
        }
    }

    fun updateInfoText(newText: String) {
        _uiState.update { it.copy(infoText = newText) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val sharedPath = LollyActivity.getInstance().getPrefExportFolder() ?: ""
            val folderDisplay = shared.extractFolderNameFromEncodedUri(sharedPath)
            val humanReadablePath = getHumanReadablePath(sharedPath)

            _uiState.update {
                it.copy(
                    isLoading = true,
                    progress = 0,
                    error = null,
                    currentFolderDisplay = folderDisplay,
                    fullPath = humanReadablePath
                )
            }

            val sharedFolder = getFolderFromPath(sharedPath)

            if (sharedFolder == null || !sharedFolder.isDirectory) {
                _uiState.update { it.copy(isLoading = false, error = "Invalid export folder") }
                return@launch
            }

            val files = sharedFolder.listFiles()
            if (files.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, files = emptyList()) }
                return@launch
            }

            // Optimalizace: Načtení všech detailů z DB najednou
            val fileDetailsMap = db.allFileDetailsAsMap
            val reader = CSVReader()

            // Vláknově bezpečné proměnné pro sběr statistik
            val progressCounter = AtomicInteger(0)
            val statTotal = AtomicInteger(0)
            val statTms4 = AtomicInteger(0)
            val statTms3 = AtomicInteger(0)
            val statDendro = AtomicInteger(0)
            val statThermo = AtomicInteger(0)

            val deferredResults = files.map { file ->
                async(Dispatchers.IO) {
                    val fileName = file.name ?: ""
                    if (fileName.isBlank() || !fileName.lowercase().endsWith(".csv") || file.length() == 0L) {
                        return@async null // Skip invalid files
                    }

                    val fdet: FileDetail
                    if (fileDetailsMap.containsKey(fileName)) {
                        // Cache HIT
                        fdet = fileDetailsMap[fileName]!!
                        fdet.niceName = getNiceName(fileName)
                    } else {
                        // Cache MISS
                        try {
                            fdet = reader.readFileContent(file.uri)
                            fdet.name = fileName
                            fdet.niceName = getNiceName(fileName)
                            fdet.fileSize = file.length()
                            if (fdet.errFlag == Constants.PARSER_OK || fdet.errFlag == Constants.PARSER_HOLE_ERR) {
                                db.addFile(fdet, null)
                            }
                        } catch (e: Exception) {
                            Log.e("ListViewModel", "Error parsing ${file.name}", e)
                            return@async FileDetail(
                                name = file.name ?: "Error File",
                                iconID = 0,
                                internalFullName = file.uri.toString()
                            ).apply { errFlag = Constants.PARSER_ERROR }
                        }
                    }

                    // Společná logika pro oba případy
                    fdet.internalFullName = file.uri.toString()
                    fdet.createdDt = Instant.ofEpochMilli(file.lastModified())
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDateTime()

                    // --- VÝPOČET STATISTIKY (Thread-safe) ---
                    if (fdet.errFlag == Constants.PARSER_OK || fdet.errFlag == Constants.PARSER_HOLE_ERR) {
                        when (fdet.deviceType) {
                            TDeviceType.dLolly4 -> statTms4.incrementAndGet()
                            TDeviceType.dLolly3 -> statTms3.incrementAndGet()
                            TDeviceType.dAD, TDeviceType.dAdMicro -> statDendro.incrementAndGet()
                            TDeviceType.dTermoChron -> statThermo.incrementAndGet()
                            else -> {}
                        }
                        statTotal.incrementAndGet()
                    }

                    // Průběžná aktualizace UI (živý nárůst čísel a progress baru)
                    val currentProgress = progressCounter.incrementAndGet()
                    val progressPercentage = (currentProgress.toFloat() / files.size * 100).toInt()

                    _uiState.update {
                        it.copy(
                            progress = progressPercentage,
                            statTotal = statTotal.get(),
                            statTms4 = statTms4.get(),
                            statTms3 = statTms3.get(),
                            statDendro = statDendro.get(),
                            statThermo = statThermo.get()
                        )
                    }

                    fdet
                }
            }


            // Čekáme na dokončení analýzy všech souborů
            val loadedFiles = deferredResults.awaitAll().filterNotNull()
            val sortedFiles = sortFiles(loadedFiles, _uiState.value.sortOrder)

            // Závěrečná aktualizace po načtení všeho (schováme spinner/progress)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        files = sortedFiles,
                        isLoading = false,
                        progress = 0
                    )
                }
            }
        }


    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            state.copy(
                sortOrder = order,
                files = sortFiles(state.files, order)
            )
        }
    }

    private fun sortFiles(files: List<FileDetail>, order: SortOrder): List<FileDetail> {
        return when (order) {
            SortOrder.DATE_DESC -> files.sortedByDescending { it.createdDt }
            SortOrder.DATE_ASC -> files.sortedBy { it.createdDt }
            SortOrder.SERIAL_ASC -> files.sortedWith(compareBy<FileDetail> { it.serialNumber?.toLongOrNull() ?: Long.MAX_VALUE }.thenBy { it.name })
            SortOrder.SERIAL_DESC -> files.sortedWith(compareByDescending<FileDetail> { it.serialNumber?.toLongOrNull() ?: Long.MIN_VALUE }.thenByDescending { it.name })
        }
    }

    private fun getFolderFromPath(path: String): DocumentFile? {
        val context = getApplication<Application>()
        return if (path.startsWith("content")) {
            try {
                var correctedPath = path
                if (correctedPath.contains("tree/primary:")) {
                    correctedPath = correctedPath.replace("tree/primary:", "tree/primary%3A")
                }
                val uri = Uri.parse(correctedPath)
                DocumentFile.fromTreeUri(context, uri)
            } catch (e: Exception) {
                Log.e("ListViewModel", "Chyba při parsování URI: $path", e)
                null
            }
        } else {
            DocumentFile.fromFile(File(path))
        }
    }

    private fun getHumanReadablePath(rawPath: String): String {
        try {
            var path = Uri.decode(rawPath)
            if (path.contains("primary:")) {
                val split = path.split("primary:")
                if (split.size > 1) {
                    return "Internal Storage/" + split[1]
                }
            }
            if (path.contains("/storage/emulated/0/")) {
                return path.replace("/storage/emulated/0/", "Internal Storage/")
            }
            path = path.replace("content://com.android.externalstorage.documents/tree/", "")
            return path
        } catch (e: Exception) {
            return rawPath
        }
    }

    private fun getNiceName(name: String): String {
        if (!name.contains("_")) return name
        val parts = name.split("_")
        if (parts.size != 8) return name
        if (parts[1].isEmpty()) return name
        val suffix = shared.bef(parts[7], "\\.")
        return "${parts[1]}-${parts[2]}${parts[3]}${parts[4]}$suffix"
    }

    // Nová metoda pro exkluzivní výběr (klik na řádek)
    fun selectSingleFile(fileFullPath: String) {
        _uiState.update { state ->
            val updatedList = state.files.map { file ->
                if (file.internalFullName == fileFullPath) {
                    // Tento jsme zaklikli -> VYBRAT
                    file.cloneWithSelection(true)
                } else if (file.isSelected) {
                    // Tento byl vybraný dřív, ale už není -> ODZNAČIT
                    file.cloneWithSelection(false)
                } else {
                    // Ostatní necháme beze změny
                    file
                }
            }
            state.copy(files = updatedList)
        }
    }

    fun toggleSelection(fileFullPath: String, isSelected: Boolean) {
        _uiState.update { state ->
            // Vytvoříme nový seznam, kde vyměníme jen ten jeden změněný soubor
            val updatedList = state.files.map { file ->
                if (file.internalFullName == fileFullPath) {
                    // Použijeme naši novou metodu pro bezpečnou kopii
                    file.cloneWithSelection(isSelected)
                } else {
                    file
                }
            }
            // Uložíme nový seznam do StateFlow -> Compose pozná změnu a překreslí UI
            state.copy(files = updatedList)
        }
    }

    fun toggleSelectionForDate(dateStr: String, isSelected: Boolean) {
        _uiState.update { state ->
            val updatedList = state.files.map { file ->
                val fileDate = file.getFormattedCreatedDateOnly() // Použití Creation Time format
                if (fileDate == dateStr) {
                    file.cloneWithSelection(isSelected)
                } else {
                    file
                }
            }
            state.copy(files = updatedList)
        }
    }

    fun toggleSelectAll(isSelected: Boolean) {
        _uiState.update { state ->
            val updatedList = state.files.map { file ->
                file.cloneWithSelection(isSelected)
            }
            state.copy(files = updatedList)
        }
    }

    // Přidat do ListViewModel.kt
    fun updateProgress(newProgress: Int) {
        _uiState.update { it.copy(progress = newProgress) }
    }

    fun setLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }
}