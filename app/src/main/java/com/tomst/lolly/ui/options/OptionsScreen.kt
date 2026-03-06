package com.tomst.lolly.ui.options

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomst.lolly.R

// --- 🎨 CONSISTENT PALETTE (Matched with HomeScreen) ---
private val AppBackground = Color(0xFFF2F6F3)
private val SurfaceColor = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF334155)
private val DividerColor = Color(0xFFCBD5E1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    viewModel: OptionsViewModel,
    onSaveClick: () -> Unit,
    onExportFolderClick: () -> Unit,
    onPickDateClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val downloadOptions = stringArrayResource(id = R.array.download_array)
    val intervalOptions = stringArrayResource(id = R.array.modes_array)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Save Header
            OptionsCard(padding = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFA1E1E1))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.cog),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "Save all settings below",
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onSaveClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.content_save),
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // 2. Download Options
            OptionsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_file_download),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )

                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        ) {
                            TextField(
                                value = downloadOptions.getOrElse(uiState.readFrom) { "" },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                downloadOptions.forEachIndexed { index, option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            viewModel.updateState { it.copy(readFrom = index) }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.readFrom == 1) {
                        HorizontalDivider(color = DividerColor, thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(R.drawable.ic_all_inbox), null, modifier = Modifier.size(24.dp), tint = TextSecondary)
                            Text("Bookmark Days", modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = TextPrimary, fontWeight = FontWeight.Medium)
                            TextField(
                                value = uiState.bookmarkVal,
                                onValueChange = { newValue -> viewModel.updateState { it.copy(bookmarkVal = newValue) } },
                                modifier = Modifier.width(120.dp),
                                placeholder = { Text("# days") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }

                    if (uiState.readFrom == 2) {
                        HorizontalDivider(color = DividerColor, thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(R.drawable.ic_all_inbox), null, modifier = Modifier.size(24.dp), tint = TextSecondary)
                            Text("From Date", modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = TextPrimary, fontWeight = FontWeight.Medium)
                            Button(onClick = onPickDateClick) {
                                Text(if (uiState.fromDate.isEmpty()) "Pick date" else uiState.fromDate)
                            }
                        }
                    }
                }
            }

            // 3. Interval
            OptionsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_sim_card), null, modifier = Modifier.size(48.dp), tint = Color.Gray)

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        TextField(
                            value = intervalOptions.getOrElse(uiState.mode) { "" },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            intervalOptions.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        viewModel.updateState { it.copy(mode = index) }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. More settings
            OptionsCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Local exportation folder",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onExportFolderClick() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_all_inbox), null, tint = Color(0xFF009688))
                        Text(
                            text = if (uiState.exportFolder.isEmpty()) "Exportation folder is empty" else uiState.exportFolder,
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))

                    OptionSwitch("show graph after reading data", uiState.showGraph, Color(0xFF3F51B5)) { newValue ->
                        viewModel.updateState { it.copy(showGraph = newValue) }
                    }
                    OptionSwitch("rotate graph after reading data", uiState.rotateGraph, Color(0xFF3F51B5)) { newValue ->
                        viewModel.updateState { it.copy(rotateGraph = newValue) }
                    }
                    OptionSwitch("No led light", uiState.noLedLight, Color(0xFFFBC02D)) { newValue ->
                        viewModel.updateState { it.copy(noLedLight = newValue) }
                    }
                    OptionSwitch("Use micrometers for dendrometer", uiState.showMicro, Color(0xFF2E7D32)) { newValue ->
                        viewModel.updateState { it.copy(showMicro = newValue) }
                    }
                    OptionSwitch("Set time", uiState.setTime, Color.Gray) { newValue ->
                        viewModel.updateState { it.copy(setTime = newValue) }
                    }

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_all_inbox), null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                        Text(
                            "Decimal separator",
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        TextField(
                            value = uiState.decimalSeparator,
                            onValueChange = { newValue -> viewModel.updateState { it.copy(decimalSeparator = newValue) } },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            OptionsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAboutClick)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OptionsCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun OptionSwitch(label: String, checked: Boolean, iconTint: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_all_inbox), null, tint = iconTint, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
