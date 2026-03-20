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

data class NavItem(val route: String, val label: String, val iconRes: Int)

@Composable
fun MainBottomNavigation(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    var currentRoute by remember { mutableStateOf(navController.currentDestination?.route) }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentRoute = destination.route
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
            NavItem("home", "Home", R.drawable.ic_home_black_24dp),
            NavItem("graph", "Graph", R.drawable.baseline_bar_chart_24),
            NavItem("files", "File Viewer", R.drawable.baseline_insert_drive_file_24),
            NavItem("options", "Options", R.drawable.baseline_settings_24)
        )

        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                alwaysShowLabel = false,
                onClick = {
                    haptic.performLightTick()
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = (item.route != "options") // Don't restore state for Options to ensure it resets
                        }
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
