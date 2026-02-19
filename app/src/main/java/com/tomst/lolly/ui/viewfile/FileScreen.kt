package com.tomst.lolly.ui.viewfile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomst.lolly.R
import com.tomst.lolly.fileview.FileDetail
import java.time.ZoneId
import java.util.Calendar

// STATEFUL
@Composable
fun FilesScreen(
    viewModel: ListViewModel,
    onGraphClick: (String) -> Unit,
    onZipLogsClick: () -> Unit,
    onZipAllClick: () -> Unit,
    onSelectFolderClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    FilesScreenContent(
        state = state,
        onGraphClick = onGraphClick,
        onZipLogsClick = onZipLogsClick,
        onZipAllClick = onZipAllClick,
        onSelectFolderClick = onSelectFolderClick,
        onToggleSelection = { path, isSelected ->
            viewModel.toggleSelection(path, isSelected)
        },
        onSelectSingleFile = { path ->
            // Klik na řádek -> Single select
            viewModel.selectSingleFile(path)
        }
    )
}

// STATELESS (Design)
@Composable
fun FilesScreenContent(
    state: FilesUiState,
    onGraphClick: (String) -> Unit,
    onZipLogsClick: () -> Unit,
    onZipAllClick: () -> Unit,
    onSelectFolderClick: () -> Unit,
    onToggleSelection: (String, Boolean) -> Unit,
    onSelectSingleFile: (String) -> Unit
) {
    // Zjištění stavu pro kontextovou lištu
    val selectedFiles = state.files.filter { it.isSelected }
    val hasSelection = selectedFiles.isNotEmpty()

    // Hlavní obal pro umožnění plovoucí lišty nad seznamem
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Jemně šedé pozadí
    ) {
        // --- KONTEXTOVÁ LIŠTA NAHOŘE (Top Contextual Action Bar) ---
        AnimatedVisibility(
            visible = hasSelection,
            // Změna: Animace shora dolů (všimni si mínus u initialOffsetY)
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter) // Změna zarovnání
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Počet vybraných položek
                    Text(
                        text = "Selected: ${selectedFiles.size}",
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Akce: GRAF
                    IconButton(onClick = {
                        val fileNames = selectedFiles.joinToString(";") { it.name }
                        onGraphClick(fileNames)
                    }) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.btn_graph))
                    }

                    // Akce: ZIP DATA
                    IconButton(onClick = onZipAllClick) {
                        Icon(Icons.Default.Archive, contentDescription = stringResource(R.string.btn_zip_all))
                    }

                    // Akce: ZIP LOGY
                    IconButton(onClick = onZipLogsClick) {
                        Icon(Icons.Default.Description, contentDescription = stringResource(R.string.btn_zip_logs))
                    }
                }
            }
        }
    }

        // --- HLAVNÍ OBSAH (Statistiky + Seznam) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Pokud je lišta viditelná, přidáme padding dolů, aby neposunula poslední položku mimo obrazovku
                .padding(top = if (hasSelection) 64.dp else 0.dp)
                .padding(4.dp)
        )

        {
            // --- 1. Karta: Statistiky (hlavní) + Cesta (pod tím) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 2.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // IKONA SLOŽKY (Vlevo)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // PROSTŘEDNÍ ČÁST (Statistiky + Cesta)
                    Column(modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total: ${state.statTotal}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("|", color = Color.DarkGray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "T4:${state.statTms4}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "T3:${state.statTms3}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "D:${state.statDendro}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF795548))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Th:${state.statThermo}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.fullPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // TLAČÍTKO EDIT (Vpravo)
                    IconButton(onClick = onSelectFolderClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.change_folder),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Progress Bar
            if (state.isLoading || state.progress > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = Color(0xFFE0E0E0)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // --- 2. Seznam souborů ---
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.files.isEmpty() && !state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Text(stringResource(R.string.empty_folder), color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text(
                                stringResource(R.string.empty_folder_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp)
                    ) {
                        items(state.files) { file ->
                            FileRowItem(
                                file = file,
                                onToggleSelection = { isSelected ->
                                    onToggleSelection(file.internalFullName, isSelected)
                                },
                                // Tady můžeš napojit zobrazení grafu na kliknutí řádku (rychlá volba)
                                onClick = {
                                    //onGraphClick(file.name)
                                    onSelectSingleFile(file.internalFullName)
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                        }
                    }
                }
            }
    }
}

@Preview(showBackground = true)
@Composable
fun FilesScreenPreview() {
    val sampleFiles = listOf(
        FileDetail("TD_20231027_103000.csv").apply { niceName = "Data 1"; isSelected = true; iCount = 100 },
        FileDetail("TD_20231027_113000.csv").apply { niceName = "Data 2"; isSelected = true; iCount = 200 },
        FileDetail("TD_20231027_123000.csv").apply { niceName = "Data 3"; isSelected = false; iCount = 0 }
    )

    val sampleState = FilesUiState(
        files = sampleFiles,
        currentFolderDisplay = "Download",
        fullPath = "Internal Storage/Download",
        statTotal = 3, statTms4 = 1, statTms3 = 1, statDendro = 1, statThermo = 0,
        isLoading = false, progress = 0
    )

    MaterialTheme {
        FilesScreenContent(
            state = sampleState,
            onGraphClick = {},
            onZipLogsClick = {},
            onZipAllClick = {},
            onSelectFolderClick = {},
            onToggleSelection = { _, _ -> },
            onSelectSingleFile = {}
        )
    }
}