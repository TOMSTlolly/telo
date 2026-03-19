package com.tomst.lolly.ui.options

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tomst.lolly.R

import kotlinx.coroutines.launch
import com.tomst.lolly.ui.performLightTick
import com.tomst.lolly.ui.performSuccessTick

// --- 🎨 PREMIUM PALETTE ---
private val AppBackground = Color(0xFFF8FAFC)
private val SurfaceColor = Color(0xFFFFFFFF)
private val UnsavedColor = Color(0xFFFF9800) // Hi-vis Orange for unsaved
private val SavedColor = Color(0xFF4CAF50)   // Green for saved
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF475569)
private val DividerColor = Color(0xFFE2E8F0)
private val LedOnColor = Color(0xFF22C55E)
private val LedOffColor = Color(0xFF94A3B8)
private val LedBlueColor = Color(0xFF2196F3)

@Composable
fun OptionsScreen(
    viewModel: OptionsViewModel,
    onSaveClick: () -> Unit,
    onExportFolderClick: () -> Unit,
    onPickDateClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAboutClick: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasUnsavedChanges = viewModel.hasUnsavedChanges()
    
    OptionsScreenContent(
        uiState = uiState,
        hasUnsavedChanges = hasUnsavedChanges,
        onUpdateState = { viewModel.updateState(it) },
        onSaveClick = onSaveClick,
        onExportFolderClick = onExportFolderClick,
        onPickDateClick = onPickDateClick,
        onLoginClick = onLoginClick,
        onLogoutClick = onLogoutClick,
        onAboutClick = onAboutClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreenContent(
    uiState: OptionsUiState,
    hasUnsavedChanges: Boolean,
    onUpdateState: ((OptionsUiState) -> OptionsUiState) -> Unit,
    onSaveClick: () -> Unit,
    onExportFolderClick: () -> Unit,
    onPickDateClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val downloadOptions = stringArrayResource(id = R.array.download_array)
    val intervalOptions = stringArrayResource(id = R.array.modes_array)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Sticky Header: Simple "Settings" with Save button next to it
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = {
                    scope.launch {
                        haptic.performSuccessTick()
                    }
                    onSaveClick()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasUnsavedChanges) UnsavedColor else SavedColor
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (hasUnsavedChanges) "Save" else "Saved", fontWeight = FontWeight.Bold)
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, bottom = 100.dp)
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Data Reading
            OptionsSection("DATA RETRIEVAL") {
                OptionsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OptionRow(
                            title = "Download Scope",
                            description = "Choose how much data to retrieve",
                            icon = R.drawable.ic_file_download
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = downloadOptions.getOrElse(uiState.readFrom) { "" },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    downloadOptions.forEachIndexed { index, option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                onUpdateState { it.copy(readFrom = index) }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.readFrom == 1) {
                            OutlinedTextField(
                                value = uiState.bookmarkVal,
                                onValueChange = { newValue -> onUpdateState { it.copy(bookmarkVal = newValue) } },
                                label = { Text("Bookmark Days") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        if (uiState.readFrom == 2) {
                            Button(
                                onClick = onPickDateClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = UnsavedColor.copy(alpha = 0.1f), contentColor = UnsavedColor)
                            ) {
                                Icon(painterResource(R.drawable.ic_schedule), null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (uiState.fromDate.isEmpty()) "Select Start Date" else "From: ${uiState.fromDate}")
                            }
                        }
                    }
                }
            }

            // Section: Device Configuration
            OptionsSection("DEVICE CONFIGURATION") {
                OptionsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Measurement mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = if (uiState.mode == 0) "Select a measurement mode" else intervalOptions.getOrElse(uiState.mode) { "Select a measurement mode" },
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Select a measurement mode") },
                                leadingIcon = {
                                    val iconId = getModeIcon(uiState.mode)
                                    if (iconId != null) {
                                        Icon(painterResource(iconId), null, modifier = Modifier.size(32.dp), tint = Color.Unspecified)
                                    }
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().heightIn(min = 64.dp),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .background(SurfaceColor)
                                    .fillMaxWidth(0.9f) // Slight inset to show the border better
                            ) {
                                Surface(
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        intervalOptions.forEachIndexed { index, option ->
                                            val label = if (index == 0) "Keep current" else option
                                            DropdownMenuItem(
                                                modifier = Modifier.heightIn(min = 56.dp),
                                                text = { 
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val iconId = getModeIcon(index)
                                                        if (iconId != null) {
                                                            Icon(painterResource(iconId), null, modifier = Modifier.size(28.dp), tint = Color.Unspecified)
                                                            Spacer(Modifier.width(16.dp))
                                                        } else {
                                                            // Provide spacing even if there's no icon for alignment
                                                            Spacer(Modifier.width(44.dp))
                                                        }
                                                        Text(label, fontSize = 16.sp)
                                                    }
                                                },
                                                onClick = {
                                                    onUpdateState { it.copy(mode = index) }
                                                    expanded = false
                                                }
                                            )
                                            // Add separation between items, except after the last one
                                            if (index < intervalOptions.size - 1) {
                                                HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = DividerColor)

                        OptionSwitch(
                            label = "Disable LED",
                            description = "disable LED",
                            checked = uiState.noLedLight,
                            onCheckedChange = { newValue -> onUpdateState { it.copy(noLedLight = newValue) } },
                            customLedLogic = true
                        )
                        
                        OptionSwitch(
                            label = "Sync System Time",
                            description = "Set device time to phone time",
                            checked = uiState.setTime,
                            onCheckedChange = { newValue -> onUpdateState { it.copy(setTime = newValue) } }
                        )
                    }
                }
            }

            // Section: Display & Files
            OptionsSection("DISPLAY & EXPORT") {
                OptionsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OptionSwitch(
                            label = "Auto-Open Graph",
                            description = "Show visualization after reading",
                            checked = uiState.showGraph,
                            onCheckedChange = { newValue -> onUpdateState { it.copy(showGraph = newValue) } }
                        )

                        OptionSwitch(
                            label = "Auto-Rotate Graph",
                            description = "Force landscape for charts",
                            checked = uiState.rotateGraph,
                            onCheckedChange = { newValue -> onUpdateState { it.copy(rotateGraph = newValue) } }
                        )

                        OptionSwitch(
                            label = "Use µm instead of mm",
                            description = "Use µm instead of mm for dendrometer",
                            checked = uiState.showMicro,
                            onCheckedChange = { newValue -> onUpdateState { it.copy(showMicro = newValue) } },
                            icon = R.drawable.ic_micrometer
                        )

                        HorizontalDivider(color = DividerColor)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Export Destination",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppBackground)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onExportFolderClick() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(painterResource(R.drawable.ic_folder_export), null, tint = UnsavedColor, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = if (uiState.exportFolder.isEmpty()) "Not set - tap to choose" else uiState.exportFolder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (uiState.exportFolder.isEmpty()) Color.Red else TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.decimalSeparator,
                            onValueChange = { newValue -> onUpdateState { it.copy(decimalSeparator = newValue) } },
                            label = { Text("Decimal Separator") },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.ic_separator), null, modifier = Modifier.size(24.dp), tint = TextSecondary)
                            },
                            modifier = Modifier.width(180.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // About
            OptionsCard(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onAboutClick() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "About Application",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

private fun getModeIcon(index: Int): Int? {
    return when (index) {
        1 -> R.drawable.home_basic
        2 -> R.drawable.home_meteo
        3 -> R.drawable.home_smart
        4 -> R.drawable.home_5min
        5 -> R.drawable.home_1min
        else -> null
    }
}

@Composable
private fun OptionsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
private fun OptionsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    description: String,
    icon: Int,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = TextSecondary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun OptionSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: Int? = null,
    customLedLogic: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LED Icon State
        val iconId = when {
            icon != null -> icon
            customLedLogic -> {
                // For "Disable LED" option:
                // checked (true) -> option is ON -> LED is physically OFF -> Gray
                // not checked (false) -> option is OFF -> LED is physically ON -> Blue
                if (checked) R.drawable.ic_led_off else R.drawable.ic_led_blue
            }
            else -> {
                // Default logic: Green when ON, Gray when OFF
                if (checked) R.drawable.ic_led_on else R.drawable.ic_led_off
            }
        }

        Icon(
            painter = painterResource(id = iconId),
            contentDescription = null,
            tint = if (icon != null) TextSecondary else Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = { 
                haptic.performLightTick()
                onCheckedChange(it) 
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SavedColor, // Make it green like the rest
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = DividerColor
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OptionsScreenPreview() {
    MaterialTheme {
        OptionsScreenContent(
            uiState = OptionsUiState(
                readFrom = 0,
                mode = 1,
                exportFolder = "/storage/emulated/0/Lolly",
                showGraph = true,
                rotateGraph = false,
                noLedLight = false,
                showMicro = false,
                setTime = true,
                decimalSeparator = ".",
                bookmarkVal = "7",
                fromDate = "01.01.2024"
            ),
            hasUnsavedChanges = true,
            onUpdateState = {},
            onSaveClick = {},
            onExportFolderClick = {},
            onPickDateClick = {},
            onLoginClick = {},
            onLogoutClick = {},
            onAboutClick = {}
        )
    }
}
