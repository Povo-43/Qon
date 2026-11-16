package com.example.ailearning.presentation.ui.manage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.presentation.ui.components.CustomTopAppBar
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel
import com.example.ailearning.R
import androidx.compose.ui.graphics.Color

@Composable
fun LearningSetDetailScreen(
    viewModel: LearningSetViewModel,
    learningSetId: String,
    onBack: () -> Unit,
    onLearnClick: (String, String) -> Unit,
    onStatsClick: (String, String) -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTopAppBar(
            title = "学習セット詳細",
            navigationIcon = null,
            onNavigationClick = onBack
        )

        if (isLoading) {
            LoadingOverlay(isVisible = true)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "学習セットID: $learningSetId", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = "学習セットを選択して学習を開始できます", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = { onLearnClick(learningSetId, "学習セット") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("学習を開始")
            }

            Button(
                onClick = { onStatsClick(learningSetId, "学習セット") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("統計を表示")
            }
        }

        error?.let { msg ->
            Text(text = msg, color = Color.Red)
        }
    }
}
