package com.tomst.lolly.ui

import androidx.compose.ui.platform.ComposeView
import androidx.navigation.NavController
import com.tomst.lolly.ui.theme.LollyTheme

object MainNavigationInterop {
    @JvmStatic
    fun setupBottomNav(composeView: ComposeView, navController: NavController) {
        composeView.setContent {
            LollyTheme {
                MainBottomNavigation(navController)
            }
        }
    }
}
