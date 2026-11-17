package com.eagleosint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.eagleosint.app.data.findModuleByRoute
import com.eagleosint.app.ui.screens.DashboardScreen
import com.eagleosint.app.ui.screens.ScanScreen
import com.eagleosint.app.ui.theme.EagleOSINTTheme
import com.eagleosint.app.viewmodel.OsintViewModel
import com.eagleosint.app.viewmodel.ScanUiState
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data class ScanRoute(val moduleRoute: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EagleOSINTTheme {
                val navController = rememberNavController()
                val viewModel: OsintViewModel = viewModel()
                val moduleStates by viewModel.moduleStates.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = DashboardRoute
                ) {
                    composable<DashboardRoute> {
                        DashboardScreen(
                            moduleStates = moduleStates,
                            onModuleClick = { module ->
                                navController.navigate(ScanRoute(module.route))
                            },
                            onClearAllHistory = {
                                viewModel.clearAllHistory()
                            }
                        )
                    }
                    composable<ScanRoute> { backStackEntry ->
                        val route: ScanRoute = backStackEntry.toRoute()
                        val module = findModuleByRoute(route.moduleRoute)
                        val uiState = moduleStates[module.route] ?: ScanUiState()
                        ScanScreen(
                            module = module,
                            uiState = uiState,
                            onStartScan = { query ->
                                viewModel.startScan(module.route, query)
                            },
                            onClearChat = {
                                viewModel.clearResults(module.route)
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
