package com.tomst.lolly.ui.viewfile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tomst.lolly.LollyActivity
import com.tomst.lolly.core.CSVReader
import com.tomst.lolly.core.Constants
import com.tomst.lolly.core.shared
import com.tomst.lolly.fileview.FileDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    fun updateInfoText(newText: String) {
        _uiState.update { it.copy(infoText = newText) }
    }

    fun loadFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Získání cest HNED na začátku
            val sharedPath = LollyActivity.getInstance().getPrefExportFolder() ?: ""
            val folderDisplay = shared.extractFolderNameFromEncodedUri(sharedPath)
            val humanReadablePath = getHumanReadablePath(sharedPath)

            // 2. Okamžitý update UI (aby byla cesta vidět, i když se točí kolečko)
            _uiState.update {
                it.copy(
                    isLoading = true,
                    progress = 0,
                    error = null,
                    currentFolderDisplay = folderDisplay,
                    fullPath = humanReadablePath // <--- Tady to posíláme do UI
                )
            }

            val context = getApplication<Application>()
            val sharedFolder = if (sharedPath.startsWith("content")) {
                try {
                    // 1. KROK: Ruční oprava URI řetězce
                    // Pokud cesta obsahuje "tree/primary:", nahradíme ji za "tree/primary%3A"
                    var correctedPath = sharedPath
                    if (correctedPath.contains("tree/primary:")) {
                        correctedPath = correctedPath.replace("tree/primary:", "tree/primary%3A")
                    }

                    // 2. KROK: Teď parsujeme opravenou cestu
                    val uri = Uri.parse(correctedPath)

                    // 3. KROK: Vytvoření DocumentFile
                    DocumentFile.fromTreeUri(context, uri)
                } catch (e: Exception) {
                    Log.e("ListViewModel", "Chyba při parsování URI: $sharedPath", e)
                    null
                }
            } else {
                DocumentFile.fromFile(File(sharedPath))
            }

            if (sharedFolder == null || !sharedFolder.isDirectory) {
                _uiState.update { it.copy(isLoading = false, error = "Invalid export folder") }
                return@launch
            }

            val files = sharedFolder.listFiles()
            if (files.isEmpty()) {
                // I když je seznam prázdný, cesta v 'it' už zůstává z předchozího kroku
                _uiState.update { it.copy(isLoading = false, files = emptyList()) }
                return@launch
            }

            // Parsování souborů...
            val loadedFiles = ArrayList<FileDetail>()
            val reader = CSVReader()

            files.filter { it.name?.lowercase()?.endsWith(".csv") == true && it.length() > 0 }
                .forEachIndexed { index, file ->
                    try {
                        val fdet = reader.FirstLast(file)

                        fdet.name = file.name ?: ""
                        fdet.niceName = getNiceName(fdet.name)
                        fdet.internalFullName = file.uri.toString()
                        fdet.fileSize = file.length()
                        fdet.createdDt = Instant.ofEpochMilli(file.lastModified())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()

                        if (fdet.errFlag == null) {
                            fdet.errFlag = Constants.PARSER_ERROR
                        }
                        loadedFiles.add(fdet)

                    } catch (e: Exception) {
                        Log.e("ListViewModel", "Error parsing ${file.name}", e)
                        val errorDetail = FileDetail(
                            name = file.name ?: "Error File",
                            iconID = 0,
                            internalFullName = file.uri.toString()
                        ).apply {
                            errFlag = Constants.PARSER_ERROR
                        }
                        loadedFiles.add(errorDetail)
                    }

                    if (index % 5 == 0) {
                        val progress = ((index + 1).toFloat() / files.size * 100).toInt()
                        _uiState.update { it.copy(progress = progress) }
                    }
                }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(files = loadedFiles, isLoading = false, progress = 0)
                }
            }
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

    fun toggleSelection(fileFullPath: String, isSelected: Boolean) {
        _uiState.update { state ->
            val updatedList = state.files.map {
                if (it.internalFullName == fileFullPath) it.copy(isSelected = isSelected) else it
            }
            state.copy(files = updatedList)
        }
    }
}