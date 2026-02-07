package com.tomst.lolly.ui.viewfile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomst.lolly.R
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
        onItemClick = { path, isSelected ->
            viewModel.toggleSelection(path, isSelected)
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
    onItemClick: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Jemně šedé pozadí celé obrazovky
            .padding(16.dp)
    ) {
        // --- 1. Sekce akčních tlačítek ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zip ALL
            ActionButton(
                text = stringResource(R.string.btn_zip_all),
                icon = Icons.Default.Archive,
                modifier = Modifier.weight(1f),
                onClick = onZipAllClick
            )

            // Graph (Zvýrazněný)
            Button(
                onClick = {
                    val selectedFiles = state.files.filter { it.isSelected }
                    val fileNames = selectedFiles.joinToString(";") { it.name }
                    if (fileNames.isNotEmpty()) onGraphClick(fileNames)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_graph), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Zip LOGS
            ActionButton(
                text = stringResource(R.string.btn_zip_logs),
                icon = Icons.Default.BugReport,
                modifier = Modifier.weight(1f),
                onClick = onZipLogsClick
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Informační Karta o úložišti ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Horní řádek karty: Ikona složky + Název složky + Tlačítko Change
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ikona
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Název a Label
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.storage_location),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = state.currentFolderDisplay.ifEmpty { "N/A" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Tlačítko Edit (Change)
                    IconButton(onClick = onSelectFolderClick) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.change_folder), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Spodní řádek karty: Cesta pro Windows
                if (state.fullPath.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Lokalizovaná cesta s ikonkou PC
                    Text(
                        text = stringResource(R.string.pc_path_instruction, state.fullPath.replace("/", "\\")),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF555555),
                        modifier = Modifier
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .fillMaxWidth()
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

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Seznam souborů ---
        Card(
            modifier = Modifier
                .weight(1f) // Zabere zbytek místa
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White), // Bílé pozadí seznamu
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state.files.isEmpty() && !state.isLoading) {
                // Empty State
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.files) { file ->
                        FileRowItem(
                            file = file,
                            onToggleSelection = { isSelected ->
                                onToggleSelection(file.internalFullName, isSelected)
                            },
                            onItemClick = {
                                onItemClick(file.internalFullName, !file.isSelected)
                            }
                        )
                        Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

// Pomocná komponenta pro tlačítka (aby vypadala stejně)
@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            // Spacer(modifier = Modifier.width(4.dp))
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
    }
}

@Composable
fun FileRowItem(
    file: FileDetail,
    onToggleSelection: (Boolean) -> Unit,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .background(if (file.isSelected) Color(0xFFE8F0FE) else Color.Transparent) // Google Blue selection tint
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = file.isSelected,
            onCheckedChange = { onToggleSelection(it) },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.niceName ?: file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = file.getFormattedSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "|",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = file.getFormattedCreated(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            if (file.iCount > 0) {
                Text(
                    text = "${file.getDeviceTypeLabel()} · ${file.iCount} recs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}