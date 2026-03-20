package com.tomst.lolly.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomst.lolly.R
import com.tomst.lolly.ui.performLightTick
import com.tomst.lolly.ui.theme.LollyTheme

@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val appBackground = Color(0xFFF2F6F3)
    val surfaceColor = Color(0xFFFFFFFF)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF334155)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground),
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
            // Back Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = surfaceColor),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About Lolly",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Lolly for Android",
                        fontSize = 16.sp,
                        color = textSecondary,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Version 1.0",
                        fontSize = 14.sp,
                        color = textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tomst_logo),
                    contentDescription = "TOMST Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // No ripple on image looks cleaner here
                            onClick = {
                                haptic.performLightTick()
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tomst.com"))
                                context.startActivity(intent)
                            }
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "by TOMST",
                    fontSize = 16.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Added spacer to lift the logo a bit higher from the bottom
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    LollyTheme {
        AboutScreen(onBackClick = {})
    }
}
