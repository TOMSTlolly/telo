package com.tomst.lolly.ui.viewfile

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomst.lolly.core.Constants
import com.tomst.lolly.fileview.FileDetail
import java.time.LocalDateTime
import com.tomst.lolly.core.TDeviceType

// --- HLAVNÍ KOMPONENTA ŘÁDKU ---
@Composable
fun FileRowItem(
    file: FileDetail,
    onToggleSelection: (Boolean) -> Unit,
    onClick: () -> Unit = {}
) {
    // State pro otevření detailu (ikona "i") zůstává, to je správně
    var showDetail by remember { mutableStateOf(false) }

    // ZMĚNA: Úplně jsme odstranili lokální `isHighlighted`
    // Barva pozadí se teď řídí VÝHRADNĚ stavem `file.isSelected` z ViewModelu
    val backgroundColor = if (file.isSelected) Color(0xFFE3F2FD) else Color.White

    // Definice úzkého fontu pro tento řádek
    val condensedFontFamily = FontFamily(
        Typeface.create("sans-serif-condensed", Typeface.BOLD)
    )

    // 1. KARTA ŘÁDKU
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A) CHECKBOX (Slouží pro Multi-Select)
            Checkbox(
                checked = file.isSelected,
                onCheckedChange = { isChecked ->
                    onToggleSelection(isChecked)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // B) TEXT (Slouží pro Single-Select, exkluzivní výběr)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        // ZMĚNA: Odstraněno `isHighlighted = !isHighlighted`
                        // Nyní jen pošleme signál nahoru, ať to vyřeší ViewModel
                        onClick()
                    }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = file.niceName ?: file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = condensedFontFamily,
                        fontSize = 15.sp,
                        letterSpacing = (-0.5).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${file.getDeviceTypeLabel()} | ${file.getFormattedSize()} | ${file.getFormattedCreated()}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.3).sp,
                        fontSize = 11.sp
                    ),
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // C) IKONA "i" (Spouští dialog)
            IconButton(
                onClick = { showDetail = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Detail souboru",
                    tint = Color.LightGray
                )
            }
        }
    }

    // 2. LOGIKA ZOBRAZENÍ DIALOGU
    if (showDetail) {
        FileDetailDialog(
            file = file,
            onDismiss = { showDetail = false }
        )
    }
}

// --- PREVIEW ---
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