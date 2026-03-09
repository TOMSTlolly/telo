package com.tomst.lolly.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomst.lolly.R


// --- 🎨 FIELD-READY HIGH CONTRAST PALETTE ---
private val AppBackground = Color(0xFFF2F6F3)
private val SurfaceColor = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF334155)
private val DividerColor = Color(0xFFCBD5E1)
private val DangerColor = Color(0xFFDC2626)

// Progress Bar Colors (Lightened slightly so dark text always remains readable over them)
private val ProgressAdColor = Color(0xFFF59E0B)
private val ProgressMemColor = Color(0xFF94A3B8)

// Dynamic Download & Connection Colors (Optimized for Sunlight)
private val DwnlActiveColor = Color(0xFF7DD3FC)
private val DwnlDoneColor = Color(0xFF6EE7B7)
private val ConnectedColor = Color(0xFF15803D)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onDebugAction: (String) -> Unit
) {
    val showAdProgressBar = true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // =========================================================
            // 1. HEADER CARD (Serial & Mode)
            // =========================================================
            DashboardCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // LEFT SIDE: Serial Number
                    Column(modifier = Modifier.weight(1.4f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "SERIAL NO.", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)

                            // ROTATED DEVICE IMAGE
                            if (state.deviceImageRes != 0) {
                                Image(
                                    painter = painterResource(id = state.deviceImageRes),
                                    contentDescription = "Device Type",
                                    modifier = Modifier
                                        .width(56.dp)
                                        .height(18.dp)
                                        .rotateVertically(-90f),
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Strict pass-through of the UI State!
                        Text(
                            text = state.serialNumber,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // CENTER: Vertical Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(DividerColor)
                    )

                    // RIGHT SIDE: Mode
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Text(text = "MODE", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.mode,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // MASSIVE MODE ICON
                        if (state.modeImageRes != 0) {
                            Image(
                                painter = painterResource(id = state.modeImageRes),
                                contentDescription = "Mode Icon",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(start = 8.dp)
                                    .width(42.dp),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterEnd
                            )
                        }
                    }
                }
            }

            // =========================================================
            // 2. AD & MEMORY PROGRESS BARS
            // =========================================================
            DashboardCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val memProg = (state.memoryUsage / 100f).coerceIn(0f, 1f)
                    LabeledProgressBar("MEMORY", memProg, ProgressMemColor)

                    if (showAdProgressBar && state.humAD != "--" && state.humAD.isNotEmpty()) {
                        val adProg = (state.humProc / 100f).coerceIn(0f, 1f)
                        LabeledProgressBar("AD", adProg, ProgressAdColor)
                    }
                }
            }

            // =========================================================
            // 3. SENSORS GRID
            // =========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensorCard("HUM", state.humAD, Modifier.weight(1f))
                SensorCard("T1/°C", state.temp1, Modifier.weight(1f))
                SensorCard("T2/°C", state.temp2, Modifier.weight(1f))
                SensorCard("T3/°C", state.temp3, Modifier.weight(1f))
            }

            // =========================================================
            // 4. HIGHLY COMPACTED TIMES
            // =========================================================
            DashboardCard(padding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactTimeBlock(
                        label = "DEV TIME",
                        value = state.deviceTime,
                        modifier = Modifier.weight(1f)
                    )

                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().padding(vertical = 4.dp).background(DividerColor))

                    CompactTimeBlock(
                        label = "PHN TIME",
                        value = state.phoneTime,
                        modifier = Modifier.weight(1f)
                    )

                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().padding(vertical = 4.dp).background(DividerColor))

                    val isDiffOk = state.timeDiff == "OK" || state.timeDiff == "0" || state.timeDiff == "0.0" || state.timeDiff.startsWith("0.") || state.timeDiff.startsWith("-0.") || state.timeDiff == "---" || state.timeDiff.isEmpty()

                    CompactTimeBlock(
                        label = "DIFF",
                        value = state.timeDiff,
                        valueColor = if (isDiffOk) TextPrimary else DangerColor,
                        modifier = Modifier.weight(0.7f),
                        isDiffBlock = true
                    )
                }
            }

            // =========================================================
            // 5. HARDWARE CONNECTION & DOWNLOAD (MOVED TO BOTTOM)
            // =========================================================
            DashboardCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isConnected = state.isAdapterConnected

                    // Adapter Image Box
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.width(64.dp)
                    ) {
                        if (state.adapterImageRes != 0) {
                            Image(
                                painter = painterResource(id = state.adapterImageRes),
                                contentDescription = "Adapter Status",
                                modifier = Modifier.height(56.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.connectionStatus,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) ConnectedColor else DangerColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Download Progress Fill Bar
                        val actProg = state.downloadProgress
                        val maxProg = if (state.maxProgress > 0) state.maxProgress else 100
                        val dlProg = (actProg.toFloat() / maxProg).coerceIn(0f, 1f)
                        val dlPercent = (dlProg * 100).toInt()

                        val barColor = when {
                            dlPercent >= 100 -> DwnlDoneColor
                            dlPercent > 0 -> DwnlActiveColor
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            if (dlProg > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(dlProg)
                                        .background(barColor)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("DWNL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (state.remainDays.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(state.remainDays, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${dlPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Anti-Jitter Box for Heartbeat
                                    Box(modifier = Modifier.width(16.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = state.heartbeat,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 6. DEBUG BUTTONS
            // =========================================================
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DEBUG CONTROLS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DebugButton("t.Tup", Modifier.weight(1f)) { onDebugAction("t.Tup") }
                    DebugButton("t.Blowfish", Modifier.weight(1f)) { onDebugAction("t.Blowfish") }
                    DebugButton("t.Crash", Modifier.weight(1f)) { onDebugAction("t.Crash") }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ============================================================================
// 🧱 CUSTOM REUSABLE UI COMPONENTS
// ============================================================================

@Composable
private fun DashboardCard(
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
private fun LabeledProgressBar(
    label: String,
    progress: Float,
    fillColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F5F9))
    ) {
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(fillColor)
            )
        }
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = if (progress > 0.5f) Color.White else TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * 🧠 Smart Date/Time Parser
 * Intelligently separates dates and times, handling empty/default states gracefully.
 */
@Composable
private fun CompactTimeBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    isDiffBlock: Boolean = false
) {
    val cleanValue = value.trim()
    val displayTime: String
    val displayDate: String

    if (isDiffBlock) {
        displayTime = if (cleanValue.isEmpty()) "---" else cleanValue
        displayDate = ""
    } else {
        val parts = cleanValue.split(" ")
        if (parts.size >= 2) {
            displayDate = parts[0]
            displayTime = parts[1]
        } else if (parts.size == 1) {
            if (cleanValue.contains(":")) {
                displayTime = cleanValue
                displayDate = "--.--.----"
            } else {
                displayDate = cleanValue
                displayTime = "--:--:--"
            }
        } else {
            displayDate = "--.--.----"
            displayTime = "--:--:--"
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = displayTime,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isDiffBlock && displayTime != "---" && displayTime != "OK" && !displayTime.endsWith("s")) {
                Text(
                    text = "s",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )
            }
        }

        if (!isDiffBlock) {
            Text(
                text = displayDate,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SensorCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DebugButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
        border = BorderStroke(1.dp, DividerColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

fun Modifier.rotateVertically(rotation: Float = -90f): Modifier = this
    .layout { measurable, constraints ->
        val swappedConstraints = constraints.copy(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth
        )
        val placeable = measurable.measure(swappedConstraints)
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width - placeable.height) / 2,
                y = -(placeable.height - placeable.width) / 2
            )
        }
    }
    .graphicsLayer { rotationZ = rotation }

// ============================================================================
// 🔮 PREVIEW (Populated with mock data ONLY for Android Studio layout design!)
// ============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF2F6F3, widthDp = 400, heightDp = 850)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        // Here we pass fully populated dummy data SOLELY so the preview looks good.
        // This does NOT affect the actual phone app, which still uses the HomeUiState() defaults.
        HomeScreen(
            state = HomeUiState(
                serialNumber = "96386678",
                appVersion = "1.0",
                tmdVersion = "1.92",
                mode = "Smart",
                memoryUsage = 75,
                connectionStatus = "Downloading Data...",
                isAdapterConnected = true,
                downloadProgress = 45,
                maxProgress = 100,
                temp1 = "19.5", temp2 = "24.1", temp3 = "24.0", humAD = "500", humProc = 50,
                deviceTime = "06.03.2024 14:30:00",
                phoneTime = "06.03.2024 14:30:05",
                timeDiff = "5.0",
                deviceImageRes = R.drawable.dev_lolly,
                modeImageRes = R.drawable.home_smart,
                adapterImageRes = R.drawable.adapter_green,
                remainDays = "14d rem", heartbeat = "\\"
            ),
            onDebugAction = {}
        )
    }
}