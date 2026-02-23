package com.tomst.lolly.ui.viewfile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ListItemDefaults.contentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomst.lolly.fileview.FileDetail

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
    val selectedFiles = state.files.filter { it.isSelected }
    val hasSelection = selectedFiles.isNotEmpty()

    // Hlavní obal pro umožnění plovoucí lišty
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {

        // --- 1. VRSTVA: HLAVNÍ OBSAH (Statistiky + Seznam) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // --- Karta: Statistiky ---
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

                    IconButton(onClick = onSelectFolderClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Změnit složku",
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

            // --- Seznam souborů ---
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
                            Text("Prázdná složka", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp)
                    ) {
                        items(
                            items = state.files,
                            key = { file -> file.internalFullName }
                        ) { file ->
                            FileRowItem(
                                file = file,
                                onToggleSelection = { isSelected ->
                                    onToggleSelection(file.internalFullName, isSelected)
                                },
                                onClick = {
                                    onSelectSingleFile(file.internalFullName)
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // --- 2. VRSTVA: KONTEXTOVÁ LIŠTA NAHOŘE ---
        AnimatedVisibility(
            visible = hasSelection,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    ActionButtonWithText(
                        icon = Icons.Default.BarChart,
                        label = "Graph",
                        enabled = selectedFiles.size == 1, // <-- Tlačítko bude aktivní jen pro 1, 2 nebo 3 soubory
                        onClick = {
                            val fileNames = selectedFiles.joinToString(";") { it.name }
                            onGraphClick(fileNames)
                        }
                    )

                    // Akce: ZIP DATA
                    ActionButtonWithText(
                        icon = Icons.Default.Archive,
                        label = "Data",
                        onClick = onZipAllClick
                    )

                    // Akce: ZIP LOGY
                    ActionButtonWithText(
                        icon = Icons.Default.Description,
                        label = "Logs",
                        onClick = onZipLogsClick
                    )
                }
            }
        }
    }
}

// --- POMOCNÁ KOMPONENTA PRO TLAČÍTKA ---
// --- POMOCNÁ KOMPONENTA PRO TLAČÍTKA ---
@Composable
fun ActionButtonWithText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true, // <-- TADY JE TEN CHYBĚJÍCÍ PARAMETR
    onClick: () -> Unit
) {
    // Pokud je zakázáno, barva zešedne (Material standard je alpha 0.38f)
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
    }

    Column(
        modifier = Modifier
            .clickable(
                enabled = enabled, // <-- Tady se fyzicky vypíná reakce na dotyk
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor // Aplikace barvy na ikonu
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor // Aplikace barvy na text
        )
    }
}


@Preview(showBackground = true)
@Composable
fun FilesScreenPreview() {
    val sampleFiles = listOf(
        FileDetail(name = "TD_20231027_103000.csv", internalFullName = "TD_20231027_103000.csv").apply { niceName = "Data 1"; isSelected = true; iCount = 100 },
        FileDetail(name = "TD_20231027_113000.csv", internalFullName = "TD_20231027_113000.csv").apply { niceName = "Data 2"; isSelected = true; iCount = 200 },
        FileDetail(name = "TD_20231027_123000.csv", internalFullName = "TD_20231027_123000.csv").apply { niceName = "Data 3"; isSelected = false; iCount = 0 }
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