package com.example.ailearning.presentation.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.example.ailearning.presentation.ui.manage.QuestionListScreen
import com.example.ailearning.presentation.ui.settings.FocusLockScreen
import com.example.ailearning.presentation.ui.settings.SettingsScreen
import com.example.ailearning.presentation.ui.statistics.StatisticsScreen
import com.example.ailearning.presentation.ui.theme.AiLearningTheme
import com.example.ailearning.presentation.work.QuestionPrefetchScheduler
import com.example.ailearning.presentation.viewmodel.FocusLockViewModel
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel
import com.example.ailearning.presentation.viewmodel.LearningViewModel
import com.example.ailearning.presentation.viewmodel.QuestionListViewModel
import com.example.ailearning.presentation.viewmodel.SettingsViewModel
import com.example.ailearning.presentation.viewmodel.StatisticsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 問題が少ない学習セットを裏で補充する（起動時 + 定期）
        QuestionPrefetchScheduler.ensurePeriodicTopUp(this)
        QuestionPrefetchScheduler.requestPrefetchAll(this)

        val learningSetViewModel: LearningSetViewModel = ViewModelProvider(this)[LearningSetViewModel::class.java]
        val learningViewModel: LearningViewModel = ViewModelProvider(this)[LearningViewModel::class.java]
        val questionListViewModel: QuestionListViewModel = ViewModelProvider(this)[QuestionListViewModel::class.java]
        val settingsViewModel: SettingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        val statisticsViewModel: StatisticsViewModel = ViewModelProvider(this)[StatisticsViewModel::class.java]
        val focusLockViewModel: FocusLockViewModel = ViewModelProvider(this)[FocusLockViewModel::class.java]

        setContent {
            AiLearningTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val showBottomBar = currentRoute == null ||
                        currentRoute.startsWith("home") ||
                        currentRoute.startsWith("stats") ||
                        currentRoute.startsWith("settings")

                    Scaffold(
                        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                        containerColor = MaterialTheme.colorScheme.background,
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
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = {
                                fadeIn(tween(220)) + slideInHorizontally(tween(280)) { it / 14 }
                            },
                            exitTransition = { fadeOut(tween(160)) },
                            popEnterTransition = { fadeIn(tween(220)) },
                            popExitTransition = {
                                fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { it / 14 }
                            }
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
                                    onQuestionsClick = { id ->
                                        navController.navigate("questions/${Uri.encode(id)}")
                                    },
                                    onStatsClick = { id ->
                                        navController.navigate("stats?setId=${Uri.encode(id)}")
                                    },
                                    onEditClick = { id ->
                                        navController.navigate("edit/${Uri.encode(id)}")
                                    }
                                )
                            }

                            composable(
                                route = "edit/{setId}",
                                arguments = listOf(navArgument("setId") { type = NavType.StringType })
                            ) { entry ->
                                val setId = entry.arguments?.getString("setId").orEmpty()
                                val set = learningSetViewModel.findById(setId)
                                CreateLearningSetScreen(
                                    viewModel = learningSetViewModel,
                                    learningSet = set,
                                    onSave = { navController.popBackStack() },
                                    onCancel = { navController.popBackStack() }
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
                                route = "questions/{setId}",
                                arguments = listOf(navArgument("setId") { type = NavType.StringType })
                            ) { entry ->
                                val setId = entry.arguments?.getString("setId").orEmpty()
                                val sets by learningSetViewModel.learningSets.collectAsState()
                                val setName = sets.firstOrNull { it.id == setId }?.name ?: ""
                                QuestionListScreen(
                                    viewModel = questionListViewModel,
                                    learningSetId = setId,
                                    learningSetName = setName,
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
                                    initialSetId = entry.arguments?.getString("setId"),
                                    onCreateSetClick = { navController.navigate("create") }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onFocusLockClick = { navController.navigate("focus") },
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

                            composable("focus") {
                                FocusLockScreen(
                                    viewModel = focusLockViewModel,
                                    onBack = { navController.popBackStack() }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 18.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            BottomNavItem(
                selected = currentRoute == null || currentRoute.startsWith("home"),
                icon = Icons.Rounded.Home,
                label = "ホーム",
                onClick = { onNavigate("home") }
            )
            BottomNavItem(
                selected = currentRoute?.startsWith("stats") == true,
                icon = Icons.Rounded.BarChart,
                label = "統計",
                onClick = { onNavigate("stats") }
            )
            BottomNavItem(
                selected = currentRoute?.startsWith("settings") == true,
                icon = Icons.Rounded.Settings,
                label = "設定",
                onClick = { onNavigate("settings") }
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomNavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = { androidx.compose.material3.Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
