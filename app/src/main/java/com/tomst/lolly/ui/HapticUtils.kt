package com.tomst.lolly.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay

// Reusable single premium tick
fun HapticFeedback.performLightTick() {
    this.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

// Reusable double-tick (Checkmark/Success)
suspend fun HapticFeedback.performSuccessTick() {
    this.performLightTick()
    delay(80L)
    this.performLightTick()
}

// Reusable alerting tick (Warning/Review needed)
suspend fun HapticFeedback.performWarningTick() {
    this.performLightTick()
    delay(60L)
    this.performLightTick()
    delay(60L)
    this.performLightTick()
}