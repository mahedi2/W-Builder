package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.navigation.ComponentBuilderRoute
import com.example.navigation.DashboardRoute
import com.example.navigation.EditorRoute
import com.example.ui.backup.BackupRestoreViewModel
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.StudioDashboardScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.editor.WidgetCanvasEditorScreen
import com.example.ui.theme.WBuilderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before rendering UI or ViewModels.
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val container = remember { WBuilderAppContainer(applicationContext) }

            WBuilderTheme {
                val navController = rememberNavController()
                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.provideFactory(
                        widgetRepository = container.widgetRepository,
                        componentRepository = container.customComponentRepository
                    )
                )
                val backupViewModel: BackupRestoreViewModel = viewModel(
                    factory = BackupRestoreViewModel.provideFactory(
                        archiveManager = container.archiveManager
                    )
                )

                NavHost(
                    navController = navController,
                    startDestination = DashboardRoute
                ) {
                    composable<DashboardRoute> {
                        StudioDashboardScreen(
                            dashboardViewModel = dashboardViewModel,
                            backupViewModel = backupViewModel,
                            onOpenWidgetEditor = { projectId ->
                                navController.navigate(EditorRoute(projectId))
                            },
                            onOpenComponentBuilder = { componentId ->
                                navController.navigate(ComponentBuilderRoute(componentId))
                            }
                        )
                    }

                    composable<EditorRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<EditorRoute>()
                        val editorViewModel: EditorViewModel = viewModel(
                            key = "editor_${route.projectId}",
                            factory = EditorViewModel.provideFactory(
                                targetId = route.projectId,
                                isComponentMode = false,
                                widgetRepository = container.widgetRepository,
                                componentRepository = container.customComponentRepository,
                                assetStorageManager = container.assetStorageManager,
                                telemetryRepository = container.telemetryRepository,
                                weatherRepository = container.weatherRepository,
                                studioActionsRepository = container.studioActionsRepository,
                                aiImageRepository = container.aiImageRepository,
                                widgetApplyBridge = container.widgetApplyBridge
                            )
                        )
                        WidgetCanvasEditorScreen(
                            editorViewModel = editorViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<ComponentBuilderRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<ComponentBuilderRoute>()
                        val editorViewModel: EditorViewModel = viewModel(
                            key = "component_${route.componentId}",
                            factory = EditorViewModel.provideFactory(
                                targetId = route.componentId,
                                isComponentMode = true,
                                widgetRepository = container.widgetRepository,
                                componentRepository = container.customComponentRepository,
                                assetStorageManager = container.assetStorageManager,
                                telemetryRepository = container.telemetryRepository,
                                weatherRepository = container.weatherRepository,
                                studioActionsRepository = container.studioActionsRepository,
                                aiImageRepository = container.aiImageRepository,
                                widgetApplyBridge = container.widgetApplyBridge
                            )
                        )
                        WidgetCanvasEditorScreen(
                            editorViewModel = editorViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
