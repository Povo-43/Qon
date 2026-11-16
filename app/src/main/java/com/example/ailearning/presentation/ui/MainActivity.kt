package com.example.ailearning.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ailearning.AiLearningApplication
import com.example.ailearning.presentation.ui.home.HomeScreen
import com.example.ailearning.presentation.ui.learn.LearnScreen
import com.example.ailearning.presentation.ui.manage.CreateLearningSetScreen
import com.example.ailearning.presentation.ui.manage.LearningSetDetailScreen
import com.example.ailearning.presentation.ui.settings.SettingsScreen
import com.example.ailearning.presentation.ui.statistics.StatisticsScreen
import com.example.ailearning.presentation.ui.theme.AiLearningTheme
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel
import com.example.ailearning.presentation.viewmodel.LearningViewModel
import com.example.ailearning.presentation.viewmodel.SettingsViewModel
import com.example.ailearning.presentation.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val learningSetViewModel: LearningSetViewModel = ViewModelProvider(this)[LearningSetViewModel::class.java]
        val learningViewModel: LearningViewModel = ViewModelProvider(this)[LearningViewModel::class.java]
        val settingsViewModel: SettingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        val statisticsViewModel: StatisticsViewModel = ViewModelProvider(this)[StatisticsViewModel::class.java]

        setContent {
            AiLearningTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    NavHost(navController, "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = learningSetViewModel,
                                onLearningSetClick = { set ->
                                    navController.navigate("detail")
                                },
                                onCreateClick = {
                                    navController.navigate("create")
                                }
                            )
                        }
                        composable("create") {
                            CreateLearningSetScreen(
                                viewModel = learningSetViewModel,
                                onSave = { navController.popBackStack() },
                                onCancel = { navController.popBackStack() }
                            )
                        }
                        composable("detail") {
                            LearningSetDetailScreen(
                                viewModel = learningSetViewModel,
                                learningSetId = "default",
                                onBack = { navController.popBackStack() },
                                onLearnClick = { setId, name ->
                                    navController.navigate("learn/$setId/$name")
                                },
                                onStatsClick = { setId, name ->
                                    navController.navigate("stats/$setId/$name")
                                }
                            )
                        }
                        composable(
                            route = "learn/{learningSetId}/{learningSetName}",
                            arguments = listOf(
                                androidx.navigation.navArgument("learningSetId") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("learningSetName") { type = androidx.navigation.NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val learningSetId = backStackEntry.arguments?.getString("learningSetId") ?: "default"
                            val learningSetName = backStackEntry.arguments?.getString("learningSetName") ?: "default"
                            LearnScreen(
                                viewModel = learningViewModel,
                                learningSetId = learningSetId,
                                learningSetName = learningSetName,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "stats/{learningSetId}/{learningSetName}",
                            arguments = listOf(
                                androidx.navigation.navArgument("learningSetId") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("learningSetName") { type = androidx.navigation.NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val learningSetId = backStackEntry.arguments?.getString("learningSetId") ?: "default"
                            val learningSetName = backStackEntry.arguments?.getString("learningSetName") ?: "default"
                            StatisticsScreen(
                                viewModel = statisticsViewModel,
                                learningSetId = learningSetId,
                                learningSetName = learningSetName,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
