package com.tomst.lolly.ui.viewfile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    onToggleSelection: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Jemně šedé pozadí celé obrazovky
            .padding(4.dp)
    ) {
        // --- 1. Sekce akčních tlačítek (KOMPAKTNÍ VERZE) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp), // Menší mezera pod tlačítky
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Zip ALL (Méně výrazné - Outlined)
            CompactButton(
                text = stringResource(R.string.btn_zip_all),
                icon = Icons.Default.Folder, // Nebo jiná ikona
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = onZipAllClick
            )

            // 2. Graph (Výrazné - Primary, ale malé)
            CompactButton(
                text = stringResource(R.string.btn_graph),
                icon = Icons.Default.BarChart,
                isPrimary = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    val selectedFiles = state.files.filter { it.isSelected }
                    val fileNames = selectedFiles.joinToString(";") { it.name }
                    if (fileNames.isNotEmpty()) onGraphClick(fileNames)
                }
            )

            // 3. Zip LOGS (Méně výrazné - Outlined)
            CompactButton(
                text = stringResource(R.string.btn_zip_logs),
                icon = Icons.Default.Info, // Nebo jiná ikona
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = onZipLogsClick
            )
        }

        //Spacer(modifier = Modifier.height(16.dp))
        // --- 2. Karta: Statistiky (hlavní) + Cesta (pod tím) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 2.dp,top=2.dp,end=2.dp,bottom=2.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. IKONA SLOŽKY (Vlevo)
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

               // Spacer(modifier = Modifier.width(12.dp))

                // 2. PROSTŘEDNÍ ČÁST (Statistiky + Cesta)
                Column(modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                ) {

                    // A) STATISTIKY (Místo nápisu "Documents")
                    // Použijeme Row s 'Arrangement.SpaceBetween' nebo jen 'spacedBy', aby se to vešlo
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Total (Tučně)
                        Text(
                            text = "Total: ${state.statTotal}",
                            style = MaterialTheme.typography.bodyMedium, // Nebo titleSmall
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text("|", color = Color.DarkGray)
                        Spacer(modifier = Modifier.width(8.dp))

                        // Jednotlivé typy (Barevně a menší)
                        Text(
                            text = "T4:${state.statTms4}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32) // Tmavě zelená
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "T3:${state.statTms3}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20) // Světle zelená
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "D:${state.statDendro}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF795548) // Hnědá
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "Th:${state.statThermo}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0) // Modrá
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // B) CESTA K ADRESÁŘI (Storage location)
                    // Vrácena zpět, jak jsi chtěl
                    Text(
                        text = "${state.fullPath}",
                        style = MaterialTheme.typography.labelSmall, // Menší písmo pro cestu
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. TLAČÍTKO EDIT (Vpravo)
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
                        //Spacer(modifier = Modifier.height(8.dp))
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
                    contentPadding = PaddingValues(top = 2.dp, bottom= 50.dp)
                ) {
                    items(state.files) { file ->
                        FileRowItem(
                            file = file,
                            onToggleSelection = { isSelected ->
                                onToggleSelection(file.internalFullName, isSelected)
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
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



@Preview(showBackground = true)
@Composable
fun FilesScreenPreview() {
    val sampleFiles = listOf(
        FileDetail(
            name = "TD_20231027_103000.csv",
            iconID = 0,
            internalFullName = "/path/to/TD_20231027_103000.csv",
            isSelected = true
        ).apply {
            niceName = "data_96648721_2025_12_15_0.csv"
            fileSize = 12345
            createdDt = Calendar.getInstance().time.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            iCount = 100
        },
        FileDetail(
            name = "TD_20231027_113000.csv",
            iconID = 0,
            internalFullName = "/path/to/TD_20231027_113000.csv"
        ).apply {
            niceName = "data_96648721_2025_12_16_0.csv"
            fileSize = 23456
            createdDt = Calendar.getInstance().time.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            iCount = 200
        },
        FileDetail(
            name = "TD_20231027_123000.csv",
            iconID = 0,
            internalFullName = "/path/to/TD_20231027_123000.csv"
        ).apply {
            niceName = "data_96648748_2025_12_01_11_0.csv"
            fileSize = 34567
            createdDt = Calendar.getInstance().time.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            iCount = 0
        }
    )

    val sampleState = FilesUiState(
        files = sampleFiles,
        currentFolderDisplay = "Download",
        fullPath = "Internal Storage/Download",
        isLoading = false,
        progress = 0
    )

    FilesScreenContent(
        state = sampleState,
        onGraphClick = {},
        onZipLogsClick = {},
        onZipAllClick = {},
        onSelectFolderClick = {},
        onToggleSelection = { _, _ -> }
    )
}

@Composable
fun StatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
fun CompactButton(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Rozhodnutí o barvách a typu tlačítka
    val colors = if (isPrimary) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    } else {
        ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    }

    // Pokud je Primary, použijeme Button (plný), jinak OutlinedButton (rámeček)
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp).widthIn(max = 320.dp), // Fixní malá výška (standard je 48dp+)
            shape = RoundedCornerShape(8.dp),
            colors = colors,
            contentPadding = PaddingValues(horizontal = 8.dp) // Žádný vertikální padding
        ) {
            CompactButtonContent(text, icon)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp).widthIn(max = 320.dp),
            shape = RoundedCornerShape(8.dp),
            colors = colors,
            border = BorderStroke(1.dp, Color.LightGray),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            CompactButtonContent(text, icon)
        }
    }
}

@Composable
fun CompactButtonContent(text: String, icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(16.dp) // Menší ikona
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
        text = text,
        fontSize = 12.sp, // Menší písmo
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}