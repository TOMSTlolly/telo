package com.tomst.lolly.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.tomst.lolly.R

data class NavItem(val id: Int, val label: String, val iconRes: Int)

@Composable
fun MainBottomNavigation(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    var currentDestinationId by remember { mutableStateOf(navController.currentDestination?.id) }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentDestinationId = destination.id
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    NavigationBar(
        modifier = Modifier.height(64.dp),
        containerColor = Color.White,
        tonalElevation = 3.dp
    ) {
        val items = listOf(
            NavItem(R.id.navigation_home, "Home", R.drawable.ic_home_black_24dp),
            NavItem(R.id.navigation_graph, "Graph", R.drawable.baseline_bar_chart_24),
            NavItem(R.id.navigation_notifications, "File Viewer", R.drawable.baseline_insert_drive_file_24),
            NavItem(R.id.navigation_options, "Options", R.drawable.baseline_settings_24)
        )

        items.forEach { item ->
            val selected = currentDestinationId == item.id
            NavigationBarItem(
                selected = selected,
                alwaysShowLabel = false,
                onClick = {
                    haptic.performLightTick()
                    if (!selected) {
                        val navOptions = androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(navController.graph.startDestinationId, false, true)
                            .setLaunchSingleTop(true)
                            .setRestoreState(item.id != R.id.navigation_options) // Don't restore state for Options to ensure it resets
                            .build()
                        navController.navigate(item.id, null, navOptions)
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
