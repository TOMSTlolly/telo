package com.tomst.lolly.ui

import androidx.compose.ui.platform.ComposeView

object MainNavigationInterop {
    var navigateToOptions: (() -> Unit)? = null

    @JvmStatic
    fun setContent(composeView: ComposeView) {
        composeView.setContent {
            LollyAppRouter()
        }
    }
}
