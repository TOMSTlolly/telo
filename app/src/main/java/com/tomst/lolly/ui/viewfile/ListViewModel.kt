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

class ListViewModel(application: Application) : AndroidViewModel(application) {

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
                        .atZone(ZoneId.systemDefault())
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

            // Závěrečná aktualizace po načtení všeho (schováme spinner/progress)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        files = loadedFiles,
                        isLoading = false,
                        progress = 0
                    )
                }
            }
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

    // Přidat do ListViewModel.kt
    fun updateProgress(newProgress: Int) {
        _uiState.update { it.copy(progress = newProgress) }
    }

    fun setLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }
}