package com.tomst.lolly.ui.viewfile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomst.lolly.fileview.FileDetail

// STATEFUL
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort

// ... inside the file, I will update FilesScreen and FilesScreenContent parameters and the top bar

import com.tomst.lolly.ui.performLightTick
import com.tomst.lolly.ui.viewfile.SortOrder

@Composable
fun FilesScreen(
    viewModel: ListViewModel,
    onGraphClick: (String) -> Unit,
    onZipLogsClick: (String) -> Unit,
    onZipAllClick: (String) -> Unit,
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
        },
        onToggleSelectionForDate = { dateStr, isSelected ->
            viewModel.toggleSelectionForDate(dateStr, isSelected)
        },
        onToggleSelectAll = { isSelected ->
            viewModel.toggleSelectAll(isSelected)
        },
        onSortOrderChange = { order ->
            viewModel.setSortOrder(order)
        }
    )
}

// STATELESS (Design)
@Composable
fun FilesScreenContent(
    state: FilesUiState,
    onGraphClick: (String) -> Unit,
    onZipLogsClick: (String) -> Unit,
    onZipAllClick: (String) -> Unit,
    onSelectFolderClick: () -> Unit,
    onToggleSelection: (String, Boolean) -> Unit,
    onSelectSingleFile: (String) -> Unit,
    onToggleSelectionForDate: (String, Boolean) -> Unit,
    onToggleSelectAll: (Boolean) -> Unit = {},
    onSortOrderChange: (SortOrder) -> Unit = {}
) {
    val selectedFiles = state.files.filter { it.isSelected }
    val hasSelection = selectedFiles.isNotEmpty()
    val allSelected = state.files.isNotEmpty() && selectedFiles.size == state.files.size

    var showExportDialog by remember { mutableStateOf(false) }
    var exportNote by remember { mutableStateOf("") }
    
    var showLogsDialog by remember { mutableStateOf(false) }
    var logsNote by remember { mutableStateOf("") }

    // Hlavní obal pro umožnění plovoucí lišty
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = onSelectFolderClick
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = "Změnit složku",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total: ${state.statTotal}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("|", style = MaterialTheme.typography.titleMedium, color = Color.DarkGray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "T4:${state.statTms4}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "T3:${state.statTms3}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "D:${state.statDendro}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF795548))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Th:${state.statThermo}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.fullPath,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Sort Files",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest First", fontWeight = if(state.sortOrder == SortOrder.DATE_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { onSortOrderChange(SortOrder.DATE_DESC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First", fontWeight = if(state.sortOrder == SortOrder.DATE_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { onSortOrderChange(SortOrder.DATE_ASC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Serial (Low to High)", fontWeight = if(state.sortOrder == SortOrder.SERIAL_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { onSortOrderChange(SortOrder.SERIAL_ASC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Serial (High to Low)", fontWeight = if(state.sortOrder == SortOrder.SERIAL_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { onSortOrderChange(SortOrder.SERIAL_DESC); showSortMenu = false }
                            )
                        }
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.files.isEmpty() && !state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Empty folder", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp) // Extra padding for HUD
                    ) {
                        val groupedFiles = if (state.sortOrder == SortOrder.DATE_DESC || state.sortOrder == SortOrder.DATE_ASC) {
                            state.files.groupBy { it.getFormattedFrom() }
                        } else {
                            mapOf("All Files" to state.files)
                        }
                        
                        groupedFiles.forEach { (groupStr, filesInGroup) ->
                            if (groupStr != "All Files") {
                                item(key = "header_$groupStr") {
                                    val allSelected = filesInGroup.isNotEmpty() && filesInGroup.all { it.isSelected }
                                    DateSeparatorItem(
                                        dateStr = groupStr,
                                        isSelected = allSelected,
                                        onToggle = { isSelected ->
                                            onToggleSelectionForDate(groupStr, isSelected)
                                        }
                                    )
                                }
                            } else {
                                item(key = "header_all_files") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            
                            items(
                                items = filesInGroup,
                                key = { file -> file.internalFullName }
                            ) { file ->
                                FileRowItem(
                                    file = file,
                                    onToggleSelection = { isSelected ->
                                        onToggleSelection(file.internalFullName, isSelected)
                                    },
                                    onClick = {
                                        onSelectSingleFile(file.internalFullName)
                                    },
                                    onDoubleClick = {
                                        onGraphClick(file.name)
                                    }
                                )
                            }
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
                    // Select All Checkbox
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { onToggleSelectAll(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Počet vybraných položek
                    Text(
                        text = "Selected: ${selectedFiles.size}",
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

                    // Akce: EXPORT DATA
                    ActionButtonWithText(
                        icon = Icons.Default.IosShare,
                        label = "Export",
                        onClick = { showExportDialog = true }
                    )

                    // Akce: ZIP LOGY
                    ActionButtonWithText(
                        icon = Icons.Default.Description,
                        label = "Logs",
                        onClick = { showLogsDialog = true }
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(text = "Export Selected Files", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Add an optional note to the file name:",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = exportNote,
                        onValueChange = { exportNote = it },
                        label = { Text("Note (e.g. site_name)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        onZipAllClick(exportNote)
                        exportNote = "" // reset
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = { Text(text = "Export Logs", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter ZIP file name for the logs:",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = logsNote,
                        onValueChange = { logsNote = it },
                        label = { Text("File name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogsDialog = false
                        val finalName = if (logsNote.isEmpty()) "logs_${System.currentTimeMillis()}.zip" else logsNote
                        onZipLogsClick(finalName)
                        logsNote = "" // reset
                    }
                ) {
                    Text("Export Logs")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogsDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun DateSeparatorItem(
    dateStr: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { 
                    haptic.performLightTick()
                    onToggle(!isSelected) 
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { 
                haptic.performLightTick()
                onToggle(it) 
            },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Text(
            text = dateStr,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

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
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
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
            onZipLogsClick = { _ -> },
            onZipAllClick = { _ -> },
            onSelectFolderClick = {},
            onToggleSelection = { _, _ -> },
            onSelectSingleFile = {},
            onToggleSelectionForDate = { _, _ -> },
            onToggleSelectAll = { _ -> }
        )
    }
}