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

@RequiresApi(Build.VERSION_CODES.O)
class ListViewModel(application: Application) : AndroidViewModel(application) {

    // Inicializace stavu
    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    // Funkce pro aktualizaci informačního textu (pokud bys ji potřeboval)
    fun updateInfoText(newText: String) {
        _uiState.update { it.copy(infoText = newText) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, progress = 0, error = null) }

            // 1. Získání cesty k exportní složce
            val sharedPath = LollyActivity.getInstance().getPrefExportFolder() ?: ""
            val folderDisplay = shared.extractFolderNameFromEncodedUri(sharedPath)

            _uiState.update { it.copy(currentFolderDisplay = folderDisplay) }

            val context = getApplication<Application>()
            val sharedFolder = if (sharedPath.startsWith("content")) {
                try {
                    DocumentFile.fromTreeUri(context, Uri.parse(sharedPath))
                } catch (e: Exception) {
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
                _uiState.update { it.copy(isLoading = false, files = emptyList()) }
                return@launch
            }

            val loadedFiles = ArrayList<FileDetail>()
            val reader = CSVReader() // Java třída pro parsování

            // 2. Procházení a parsování souborů
            files.filter { it.name?.lowercase()?.endsWith(".csv") == true && it.length() > 0 }
                .forEachIndexed { index, file ->
                    try {
                        // KROK A: CSVReader vytvoří instanci FileDetail a naplní ji daty zevnitř souboru
                        // (naplní: iFrom, iInto, iCount, errFlag, min/max teploty...)
                        val fdet = reader.FirstLast(file)

                        // KROK B: My do té samé instance doplníme metadata ze souborového systému
                        // Nemusíme nic kopírovat, pracujeme s tím samým objektem.

                        // Jméno souboru
                        fdet.name = file.name ?: ""

                        // Zkomprimované jméno pro zobrazení
                        fdet.niceName = getNiceName(fdet.name)

                        // Plná cesta (URI) - v Javě jsi používal setFull(), tady zapisujeme do property
                        fdet.internalFullName = file.uri.toString()

                        // Velikost souboru
                        fdet.fileSize = file.length()

                        // Datum vytvoření souboru
                        fdet.createdDt = Instant.ofEpochMilli(file.lastModified())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()

                        // Pojistka: Pokud by Java parser nevrátil chybu, ale ani OK stav
                        if (fdet.errFlag == null) {
                            fdet.errFlag = Constants.PARSER_ERROR
                        }

                        loadedFiles.add(fdet)

                    } catch (e: Exception) {
                        Log.e("ListViewModel", "Error parsing ${file.name}", e)

                        // Vytvoření chybového záznamu ručně, pokud parser selže úplně
                        val errorDetail = FileDetail(
                            filename = file.name ?: "Error File",
                            fullName = file.uri.toString(),
                            iconID = 0
                        ).apply {
                            errFlag = Constants.PARSER_ERROR
                        }
                        loadedFiles.add(errorDetail)
                    }

                    // Aktualizace progress baru každých 5 souborů
                    if (index % 5 == 0) {
                        val progress = ((index + 1).toFloat() / files.size * 100).toInt()
                        _uiState.update { it.copy(progress = progress) }
                    }
                }

            // 3. Odeslání hotového seznamu do UI (na hlavním vlákně)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(files = loadedFiles, isLoading = false, progress = 0)
                }
            }
        }
    }

    // Pomocná metoda pro formátování názvu
    private fun getNiceName(name: String): String {
        if (!name.contains("_")) return name
        val parts = name.split("_")
        // Očekává formát: data_Serial_YYYY_MM_DD_X.csv (8 částí)
        if (parts.size != 8) return name
        if (parts[1].isEmpty()) return name

        // Odstranění přípony .csv z poslední části
        val suffix = shared.bef(parts[7], "\\.")

        return "${parts[1]}-${parts[2]}${parts[3]}${parts[4]}$suffix"
    }

    // Metoda pro přepínání výběru (checkbox)
    fun toggleSelection(fileFullPath: String, isSelected: Boolean) {
        _uiState.update { state ->
            val updatedList = state.files.map {
                // Porovnáváme podle unikátní cesty (internalFullName)
                if (it.internalFullName == fileFullPath) it.copy(isSelected = isSelected) else it
            }
            state.copy(files = updatedList)
        }
    }
}