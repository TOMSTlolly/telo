package com.tomst.lolly.ui

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomst.lolly.LollyActivity
import com.tomst.lolly.core.DmdViewModel
import com.tomst.lolly.ui.about.AboutScreen
import com.tomst.lolly.ui.graph.GraphScreen
import com.tomst.lolly.ui.graph.GraphViewModel
import com.tomst.lolly.ui.home.HomeScreen
import com.tomst.lolly.ui.home.HomeViewModel
import com.tomst.lolly.ui.options.OptionsScreen
import com.tomst.lolly.ui.options.OptionsViewModel
import com.tomst.lolly.ui.theme.LollyTheme
import com.tomst.lolly.ui.viewfile.FilesScreen
import com.tomst.lolly.ui.viewfile.ListViewModel

@Composable
fun LollyAppRouter() {
    LollyTheme {
        val navController = rememberNavController()
        val activity = LocalContext.current as LollyActivity
        
        // Shared ViewModels tied to the Activity lifecycle
        val dmdViewModel: DmdViewModel = viewModel(viewModelStoreOwner = activity)
        val homeViewModel: HomeViewModel = viewModel(viewModelStoreOwner = activity)
        val listViewModel: ListViewModel = viewModel(viewModelStoreOwner = activity)
        val graphViewModel: GraphViewModel = viewModel(viewModelStoreOwner = activity)
        val optionsViewModel: OptionsViewModel = viewModel(viewModelStoreOwner = activity)

        // Register router callback for Java Interop
        MainNavigationInterop.navigateToOptions = {
            navController.navigate("options")
        }

        // When USB hardware finishes downloading, switch to Graph and load it
        homeViewModel.onFinishedDataCallback = {
            val serial = homeViewModel.uiState.value.serialNumber
            if (serial.isNotEmpty()) {
                graphViewModel.processGraphMessage("TMD $serial", dmdViewModel)
                if (!optionsViewModel.uiState.value.disableAutoGraph) {
                    navController.navigate("graph") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }

        Scaffold(
            bottomBar = { MainBottomNavigation(navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {

                composable("home") {
                    // Scoped Hardware Service Binding
                    // This ensures the LollyService only runs when the Home tab is actively visible.
                    // It automatically unbinds to save battery when switching to Graph or Files.
                    DisposableEffect(Unit) {
                        homeViewModel.bindHardwareService()
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        onDispose {
                            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            homeViewModel.cleanup()
                        }
                    }

                    val uiState by homeViewModel.uiState.collectAsState()
                    HomeScreen(
                        state = uiState,
                        onDebugAction = { action -> homeViewModel.handleDebugAction(action) }
                    )
                }
                composable("graph") {
                    GraphScreen(
                        viewModel = graphViewModel,
                        dmdViewModel = dmdViewModel
                    )
                }
                composable("files") {
                    LaunchedEffect(Unit) {
                        listViewModel.loadFiles()
                    }
                    FilesScreen(
                        viewModel = listViewModel,
                        onGraphClick = { fileName ->
                            // Process file locally instead of routing through LiveData
                            graphViewModel.processGraphMessage("$fileName;", dmdViewModel)
                            navController.navigate("graph") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onZipLogsClick = { fileName ->
                            listViewModel.zipLogsDirectory(fileName, activity)
                        },
                        onZipAllClick = { note ->
                            listViewModel.shareExportedData(note, activity)
                        },
                        onSelectFolderClick = {
                            activity.openDirectoryPicker()
                        }
                    )
                }
                composable("options") {
                    LaunchedEffect(Unit) {
                        optionsViewModel.loadFromDevice(activity)
                    }
                    OptionsScreen(
                        viewModel = optionsViewModel,
                        onSaveClick = { optionsViewModel.saveToDevice(activity) },
                        onExportFolderClick = { activity.openDirectoryPicker() },
                        onPickDateClick = { /* Implement Date Picker */ },
                        onLoginClick = { /* Implement Login */ },
                        onLogoutClick = { /* Implement Logout */ },
                        onAboutClick = { navController.navigate("about") }
                    )
                }
                composable("about") {
                    AboutScreen(onBackClick = { navController.popBackStack() })
                }
            }
        }
    }
}
