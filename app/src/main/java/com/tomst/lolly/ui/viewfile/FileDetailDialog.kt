package com.tomst.lolly.ui.viewfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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

@Composable
fun FileDetailDialog(
    file: FileDetail,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column {
                Text(
                    text = "File Details",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = file.niceName ?: file.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            FileDetailContent(file)
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
fun FileDetailContent(file: FileDetail) {
    if (file.errFlag == Constants.PARSER_OK) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        DataLabelValue("From:", file.getFormattedFrom())
                        DataLabelValue("Records:", file.iCount.toString())
                        DataLabelValue("Min Tx:", file.getDisplayMinTx())
                        DataLabelValue("Min Hum:", file.getDisplayMinHum())
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        DataLabelValue("To:", file.getFormattedInto())
                        DataLabelValue("Size:", file.getFormattedSize())
                        DataLabelValue("Max Tx:", file.getDisplayMaxTx())
                        DataLabelValue("Max Hum:", file.getDisplayMaxHum())
                    }
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "File Error (code: ${file.errFlag})",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun DataLabelValue(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.3).sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, name = "Content Table")
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

@Preview(showBackground = true, name = "Error")
@Composable
fun FileDetailErrorPreview() {
    val errorFile = FileDetail("corrupted_data.csv").apply {
        niceName = "Poškozený_Soubor_2024"
        errFlag = 1
    }

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FileDetailContent(file = errorFile)
        }
    }
}
