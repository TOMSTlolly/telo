package com.tomst.lolly.ui.viewfile

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomst.lolly.R
import com.tomst.lolly.core.Constants
import com.tomst.lolly.fileview.FileDetail
import com.tomst.lolly.ui.performLightTick
import java.time.LocalDateTime
import com.tomst.lolly.core.TDeviceType

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileRowItem(
    file: FileDetail,
    onToggleSelection: (Boolean) -> Unit,
    onClick: () -> Unit = {},
    onDoubleClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var showDetail by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor = if (file.isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (file.holeCount > 0) {
        Color(0xFFFFF9C4) // Warning Yellow for the entire file row
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (file.isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (file.isSelected) 4.dp else 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = backgroundColor, contentColor = contentColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = file.isSelected,
                onCheckedChange = { isChecked ->
                    haptic.performLightTick()
                    onToggleSelection(isChecked)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )

            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = if (file.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                        else if (file.holeCount > 0) Color(0xFFFFF176) // Warning Yellow
                        else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (file.isSelected) MaterialTheme.colorScheme.primary 
                               else if (file.holeCount > 0) Color(0xFFE65100) // Deep Orange/Brown
                               else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val iconRes = when (file.deviceType) {
                        TDeviceType.dLolly3, TDeviceType.dLolly4 -> R.drawable.dev_lolly
                        TDeviceType.dAD, TDeviceType.dAdMicro -> R.drawable.dev_dendro
                        TDeviceType.dTermoChron -> R.drawable.dev_wurst
                        else -> null
                    }
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = "Device Icon",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.InsertDriveFile,
                            contentDescription = "File Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptic.performLightTick()
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 300) {
                            onDoubleClick()
                        } else {
                            onClick()
                        }
                        lastClickTime = currentTime
                    }
            ) {
                val serialToDisplay = file.serialNumber?.takeIf { it.isNotEmpty() }
                    ?: com.tomst.lolly.core.shared.getSerialNumberFromFileName(file.name)
                Text(
                    text = "$serialToDisplay - ${file.getFullDeviceTypeName()}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${file.getFormattedCreated()} UTC • ${file.getFormattedSize()}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (file.isSelected) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { showDetail = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "File Details",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDetail) {
        FileDetailDialog(
            file = file,
            onDismiss = { showDetail = false }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Všechny stavy řádku")
@Composable
fun FileRowItemPreview() {
    val fileNormal = FileDetail("data_2024.csv").apply {
        niceName = "Měření_Les_Únor"
        fileSize = 51200
        createdDt = LocalDateTime.of(2024, 2, 10, 14, 30)
        deviceType = TDeviceType.dLolly4
        isSelected = false
        errFlag = Constants.PARSER_OK
    }

    val fileSelected = FileDetail("data_selected.csv").apply {
        niceName = "Měření_Louka_Vybráno"
        fileSize = 1024 * 1024 * 2L
        createdDt = LocalDateTime.of(2024, 1, 15, 9, 0)
        deviceType = TDeviceType.dTermoChron
        isSelected = true
        errFlag = Constants.PARSER_OK
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F0F0))
                .padding(8.dp)
        ) {
            FileRowItem(file = fileNormal, onToggleSelection = {})
            Spacer(modifier = Modifier.height(16.dp))
            FileRowItem(file = fileSelected, onToggleSelection = {})
        }
    }
}
