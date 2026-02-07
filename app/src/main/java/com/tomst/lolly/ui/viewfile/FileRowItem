package com.tomst.lolly.ui.viewfile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.tomst.lolly.R
import com.tomst.lolly.core.Constants
import com.tomst.lolly.fileview.FileDetail

@Composable
fun FileRowItem(
    file: FileDetail,
    onClick: () -> Unit,
    onToggleSelection: (Boolean) -> Unit
) {
    // Pozadí vybraného řádku (jako v adapteru LTGRAY vs TRANSPARENT)
    val rowBackgroundColor = if (file.isSelected) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent

    // Barva pozadí ikony (Červená pro chybu, Zelená pro OK)
    val iconBackgroundColor = if (file.errFlag == Constants.PARSER_OK) Color(0xFF4CAF50) else Color.Red

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickable { onClick() }, // Klik na kartu
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFAF1)) // colorCardBackground
    ) {
        Row(
            modifier = Modifier
                .background(rowBackgroundColor)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 1. LEVÝ SLOUPEC: Checkbox a Ikona ---
            Column(
                modifier = Modifier.weight(1.5f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Checkbox
                Checkbox(
                    checked = file.isSelected,
                    onCheckedChange = { isChecked -> onToggleSelection(isChecked) }
                )

                // Ikona s barevným pozadím
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(iconBackgroundColor), // Color.GREEN or Color.RED
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_insert_drive_file_24), // Tvoje ikona
                        contentDescription = "File Icon",
                        tint = Color.White, // Ikona bílá na barevném pozadí
                        modifier = Modifier.padding(8.dp).fillMaxSize()
                    )
                }
            }

            // --- 2. PRAVÝ SLOUPEC: Texty a Data ---
            Column(modifier = Modifier.weight(8f).padding(start = 8.dp)) {

                // HEADER: Jméno souboru + Typ Zařízení
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.niceName ?: file.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037), // textColorCardTitle
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Device Type (vpravo nahoře)
                    Text(
                        text = file.getDeviceTypeLabel(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                    )
                }

                // ANNOTATION (Datum vytvoření nebo Chyba)
                val annotationText = when (file.errFlag) {
                    Constants.PARSER_OK -> file.getFormattedCreated()
                    Constants.PARSER_ERROR -> "Parser Error"
                    Constants.PARSER_HOLE_ERR -> "Hole in data"
                    Constants.PARSER_FILE_EMPTY -> "Empty---------"
                    else -> "Unknown"
                }

                val annotationColor = if (file.errFlag == Constants.PARSER_OK)
                    Color.Black // default_text_color
                else
                    Color(0xFFFF4081) // color_accent (red/pinkish)

                Text(
                    text = annotationText,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = annotationColor,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // DATA GRID (Zobrazit jen pokud OK)
                if (file.errFlag == Constants.PARSER_OK) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFEBE9)) // colorCardBackground_Data
                            .padding(4.dp)
                    ) {
                        // Sloupec 1 (From, Events, Min Tx, Min Hum)
                        Column(modifier = Modifier.weight(1f)) {
                            DataLabelValue("From:", file.getFormattedFrom())
                            DataLabelValue("Events:", file.iCount.toString())
                            DataLabelValue("Min Tx:", file.getDisplayMinTx())
                            DataLabelValue("Min Hum:", String.format("%.0f", file.minHum))
                        }

                        // Sloupec 2 (Into, Size, Max Tx, Max Hum)
                        Column(modifier = Modifier.weight(1f)) {
                            DataLabelValue("Into:", file.getFormattedInto())
                            DataLabelValue("Size:", file.getFormattedSize())
                            DataLabelValue("Max Tx:", file.getDisplayMaxTx())
                            DataLabelValue("Max Hum:", String.format("%.0f", file.maxHum))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DataLabelValue(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
