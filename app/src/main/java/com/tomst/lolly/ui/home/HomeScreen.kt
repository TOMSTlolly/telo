package com.tomst.lolly.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings // Přidal jsem pro Device
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tomst.lolly.R

@Composable
fun HomeScreen(
    state: HomeUiState,
    onDebugAction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F5F1))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // Vzdušnější mezera mezi kartami
    ) {

        // --- 1. KARTA: DEVICE INFO ---
        HomeCard(
            title = "Device Info",
            iconVector = Icons.Default.Settings // Nebo ikonka čipu/zařízení
        ) {
            Column {
                // 1. ŘÁDEK: SN + OBRÁZEK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SN: ${state.serialNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (state.deviceImageRes != 0) {
                        Icon(
                            painter = painterResource(id = state.deviceImageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .width(120.dp)
                                .height(30.dp),
                            tint = Color.Unspecified
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. ŘÁDEK: VERZE A MÓD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "FW: ${state.appVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Text(
                        text = "TMD: ${state.tmdVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    // Mode Chip
                    Surface(
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.mode,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            if (state.modeImageRes != 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(id = state.modeImageRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. KARTA: CONNECTION ---
        HomeCard(
            title = "Connection",
            // Použijeme ikonku adaptéru přímo v hlavičce, aby to ladilo, nebo obecnou ikonu
            iconPainter = painterResource(id = state.adapterImageRes)
        ) {
            Column {
                // Stav textem
                Text(
                    text = state.connectionStatus,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                var maxProgress by remember { mutableIntStateOf(100) }
                if (state.downloadProgress<0)
                {
                    maxProgress = -state.downloadProgress
                }
                else  {
                    LinearProgressIndicator(
                        //progress = { state.downloadProgress / 100f },
                        progress = { state.downloadProgress.toFloat() / maxProgress.toFloat() },
                        modifier = Modifier
                            .height(12.dp) // Decentnější výška
                            .fillMaxWidth(),
                        trackColor = Color(0xFFE0E0E0),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Detaily pod statusem/progressem
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    var prog = state.downloadProgress.toFloat() / maxProgress.toFloat() * 100

                    Text("${prog}%", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (state.remainDays.isNotEmpty()) {
                        Text(state.remainDays, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    // Heartbeat
                    Text("Activity: ${state.heartbeat}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        // --- 3. KARTA: DIAGNOSTICS ---
        HomeCard(
            title = "Diagnostics",
            iconVector = Icons.Default.Info
        ) {
            Column {
                // Řádek Paměť a Vlhkost
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Paměť
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Memory Usage", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { state.memoryUsage / 100f },
                            modifier = Modifier
                                .height(8.dp)
                                .fillMaxWidth(),
                            color = Color(0xFF6200EE),
                            trackColor = Color(0xFFE0E0E0)
                        )
                        Text("${state.memoryUsage}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // AD / Hum
                    Column(modifier = Modifier.weight(0.6f)) {
                        Text("AD/Hum", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(
                            text = state.humAD,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Řádek Teploty - Použijeme hezčí rozložení
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TempItem(label = "T1 (CPU)", value = state.temp1)
                    TempItem(label = "T2 (Board)", value = state.temp2)
                    TempItem(label = "T3 (Probe)", value = state.temp3)
                }
            }
        }

        // --- 4. KARTA: TIME SYNC ---
        HomeCard(
            title = "Time Sync",
            iconVector = Icons.Default.DateRange
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TimeItem(label = "Device Time", value = state.deviceTime, modifier = Modifier.weight(1f))
                    // Oddělovač nebo jen mezera
                    Spacer(modifier = Modifier.width(8.dp))
                    TimeItem(label = "Phone Time", value = state.phoneTime, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Difference se zobrazí jako alert nebo potvrzení
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Diff: ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(
                        text = state.timeDiff,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (state.timeDiff == "OK" || state.timeDiff == "0" || state.timeDiff.startsWith("0.")) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }
        }

        // --- 5. DEBUG BUTTONS ---
        // Ty necháme mimo karty, jako volné prvky dole
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = { onDebugAction("t.Tup") }) { Text("Log") }
            OutlinedButton(onClick = { onDebugAction("t.Blowfish") }) { Text("Flash") }
            OutlinedButton(onClick = { onDebugAction("t.Crash") }) { Text("Crash") }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// --- POMOCNÉ KOMPONENTY ---

// Upravená definice HomeCard s hlavičkou
@Composable
fun HomeCard(
    title: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Hlavička
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                if (iconVector != null) {
                    Icon(imageVector = iconVector, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                } else if (iconPainter != null) {
                    Icon(painter = iconPainter, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
            }

            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(12.dp))

            // Obsah
            content()
        }
    }
}

@Composable
fun TempItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimeItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val mockState = HomeUiState(
        serialNumber = "98765432",
        appVersion = "1.1",
        tmdVersion = "1.92",
        mode = "Work",
        memoryUsage = 75,
        connectionStatus = "Connected",
        downloadProgress = 45,
        temp1 = "24", temp2 = "25", temp3 = "19",
        humAD = "15"
    )
    MaterialTheme {
        HomeScreen(state = mockState, onDebugAction = {})
    }
}
