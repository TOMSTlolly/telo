package com.tomst.lolly.ui.viewfile

import com.tomst.lolly.fileview.FileDetail

/**
 * Reprezentuje kompletní stav obrazovky (Data Model pro UI).
 * Nahrazuje jednotlivé LiveData (mText, seznamy, progress bary).
 */
data class FilesUiState(
    // Seznam načtených souborů
    val files: List<FileDetail> = emptyList(),

    // Indikátor načítání (pro progress bar/spinner)
    val isLoading: Boolean = false,

    // Procentuální progress (0-100)
    val progress: Int = 0,

    // Název aktuální složky
    val currentFolderDisplay: String = "",

    // Případná chybová hláška
    val error: String? = null,

    // --- TVŮJ POŽADAVEK Z JAVY (mText) ---
    // Původně: mText.setValue("This is notifications fragment");
    val infoText: String = "This is notifications fragment",

    val fullPath: String = ""
)