package com.example.ailearning.presentation.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.presentation.ui.components.CustomTopAppBar
import com.example.ailearning.presentation.ui.components.EmptyState
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel
import androidx.compose.ui.graphics.Color

@Composable
fun HomeScreen(
    viewModel: LearningSetViewModel,
    onLearningSetClick: (LearningSet) -> Unit,
    onCreateClick: () -> Unit
) {
    val learningSets by viewModel.learningSets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTopAppBar(
            title = "学習セット",
            navigationIcon = null,
            onNavigationClick = {}
        )

        if (isLoading) {
            LoadingOverlay(isVisible = true)
        }

        if (learningSets.isEmpty()) {
            EmptyState(
                icon = null,
                title = "学習セットがありません",
                description = "新しい学習セットを作成してください",
                modifier = Modifier.fillMaxSize().padding(32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(learningSets) { set ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onLearningSetClick(set) }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = set.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${set.subject} > ${set.category} > ${set.unit}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        error?.let { msg ->
            Text(text = msg, color = Color.Red)
        }
    }

    FloatingActionButton(
        onClick = onCreateClick,
    ) {
        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
