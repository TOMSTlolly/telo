package com.tomst.lolly.ui.viewfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

// 1. Obalovací Dialog
@Composable
fun FileDetailDialog(
    file: FileDetail,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít")
            }
        },
        title = {
            Text(
                text = file.niceName ?: file.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            // Zde voláme funkci pro obsah (tabulku)
            FileDetailContent(file)
        }
    )
}

// 2. Obsah Dialogu (Tabulka)
@Composable
fun FileDetailContent(file: FileDetail) {
    if (file.errFlag == Constants.PARSER_OK) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Levý sloupec
                Column(modifier = Modifier.weight(1f)) {
                    DataLabelValue("Od:", file.getFormattedFrom())
                    DataLabelValue("Záznamů:", file.iCount.toString())
                    DataLabelValue("Min Tx:", file.getDisplayMinTx())
                    DataLabelValue("Min Hum:", file.getDisplayMinHum())
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Pravý sloupec
                Column(modifier = Modifier.weight(1f)) {
                    DataLabelValue("Do:", file.getFormattedInto())
                    DataLabelValue("Velikost:", file.getFormattedSize())
                    DataLabelValue("Max Tx:", file.getDisplayMaxTx())
                    DataLabelValue("Max Hum:", file.getDisplayMaxHum())
                }
            }
        }
    } else {
        Text(
            text = "Chyba souboru (kód: ${file.errFlag})",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

// 3. Pomocná řádka tabulky
@Composable
fun DataLabelValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp
            ),
            color = Color.Gray,
            modifier = Modifier.width(55.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.3).sp
            ),
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, name = "Pouze Obsah (Tabulka)")
@Composable
fun FileDetailContentPreview() {
    val sampleFile = FileDetail("data.csv").apply {
        niceName = "Test"
        iCount = 100
        minT1 = 10.0
        maxT1 = 20.0
        errFlag = Constants.PARSER_OK
    }

    MaterialTheme {
        Column(modifier = Modifier.padding(10.dp)) {
            FileDetailContent(file = sampleFile)
        }
    }
}

@Preview(showBackground = true, name = "2. Chyba (Error Flag > 0)")
@Composable
fun FileDetailErrorPreview() {
    val errorFile = FileDetail("corrupted_data.csv").apply {
        niceName = "Poškozený_Soubor_2024"
        // Nastavíme chybový flag (jakékoli číslo různé od PARSER_OK)
        // Předpokládám, že PARSER_ERROR je třeba -1 nebo 1
        errFlag = 1
    }

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Jak to vypadá při chybě:")
            Spacer(modifier = Modifier.height(8.dp))
            FileDetailContent(file = errorFile)
        }
    }
}