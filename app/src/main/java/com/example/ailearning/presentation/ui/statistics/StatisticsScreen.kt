package com.example.ailearning.presentation.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ailearning.presentation.ui.components.CustomTopAppBar
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.viewmodel.StatisticsViewModel
import com.example.ailearning.R
import androidx.compose.ui.graphics.Color

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    learningSetId: String,
    learningSetName: String,
    onBack: () -> Unit
) {
    val overallStats by viewModel.overallStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTopAppBar(
            title = "$learningSetName - 統計",
            navigationIcon = null,
            onNavigationClick = onBack
        )

        if (isLoading) {
            LoadingOverlay(isVisible = true)
        }

        overallStats?.let { stats ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "総合統計", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "総問題数: ${stats.totalQuestions}", fontSize = 16.sp)
                Text(text = "正答率: ${(stats.overallAccuracy * 100).toInt()}%", fontSize = 16.sp)
                Text(text = "学習時間: ${stats.getFormattedStudyTime()}", fontSize = 16.sp)
            }
        }

        error?.let { msg ->
            Text(text = msg, color = Color.Red)
        }
    }
}
