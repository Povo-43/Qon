package com.example.ailearning.presentation.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val showBottomBar = currentRoute == null ||
                        currentRoute.startsWith("home") ||
                        currentRoute.startsWith("stats") ||
                        currentRoute.startsWith("settings")

                    Scaffold(
                        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (showBottomBar) {
                                BottomNavigationBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = learningSetViewModel,
                                    onLearningSetClick = { set ->
                                        navController.navigate("detail/${Uri.encode(set.id)}")
                                    },
                                    onCreateClick = { navController.navigate("create") },
                                    onSettingsClick = { navController.navigate("settings") },
                                    onStatsClick = { navController.navigate("stats") }
                                )
                            }

                            composable("create") {
                                CreateLearningSetScreen(
                                    viewModel = learningSetViewModel,
                                    onSave = { navController.popBackStack() },
                                    onCancel = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "detail/{setId}",
                                arguments = listOf(navArgument("setId") { type = NavType.StringType })
                            ) { entry ->
                                val setId = entry.arguments?.getString("setId").orEmpty()
                                LearningSetDetailScreen(
                                    viewModel = learningSetViewModel,
                                    learningSetId = setId,
                                    onBack = { navController.popBackStack() },
                                    onLearnClick = { id, mode ->
                                        navController.navigate("learn/${Uri.encode(id)}?mode=$mode")
                                    },
                                    onStatsClick = { id ->
                                        navController.navigate("stats?setId=${Uri.encode(id)}")
                                    }
                                )
                            }

                            composable(
                                route = "learn/{setId}?mode={mode}",
                                arguments = listOf(
                                    navArgument("setId") { type = NavType.StringType },
                                    navArgument("mode") {
                                        type = NavType.StringType
                                        defaultValue = "continuous"
                                    }
                                )
                            ) { entry ->
                                val setId = entry.arguments?.getString("setId").orEmpty()
                                val mode = entry.arguments?.getString("mode") ?: "continuous"
                                val sets by learningSetViewModel.learningSets.collectAsState()
                                val setName = sets.firstOrNull { it.id == setId }?.name ?: "学習"
                                LearnScreen(
                                    viewModel = learningViewModel,
                                    learningSetId = setId,
                                    learningSetName = setName,
                                    mode = mode,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "stats?setId={setId}",
                                arguments = listOf(
                                    navArgument("setId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { entry ->
                                StatisticsScreen(
                                    viewModel = statisticsViewModel,
                                    learningSetViewModel = learningSetViewModel,
                                    initialSetId = entry.arguments?.getString("setId")
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = {
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        BottomNavItem(
            selected = currentRoute == null || currentRoute.startsWith("home"),
            glyph = "⌂",
            label = "ホーム",
            onClick = { onNavigate("home") }
        )
        BottomNavItem(
            selected = currentRoute?.startsWith("stats") == true,
            glyph = "▤",
            label = "統計",
            onClick = { onNavigate("stats") }
        )
        BottomNavItem(
            selected = currentRoute?.startsWith("settings") == true,
            glyph = "⚙",
            label = "設定",
            onClick = { onNavigate("settings") }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomNavItem(
    selected: Boolean,
    glyph: String,
    label: String,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Text(
                text = glyph,
                fontSize = 20.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        label = { Text(label, fontSize = 11.sp) }
    )
}
