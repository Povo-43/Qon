package com.example.ailearning.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel

@Composable
fun HomeScreen(
    viewModel: LearningSetViewModel,
    onLearningSetClick: (LearningSet) -> Unit,
    onCreateClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    val learningSets by viewModel.learningSets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Qon",
                subtitle = "AI学習ノート",
                actions = {
                    TextButton(onClick = onSettingsClick) {
                        Text("設定", fontSize = 14.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("＋ 学習セット", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SummaryCard(
                        setCount = learningSets.size,
                        onStatsClick = onStatsClick
                    )
                }

                if (learningSets.isNotEmpty()) {
                    item {
                        Text(
                            text = "学習セット",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(learningSets, key = { it.id }) { set ->
                        LearningSetCard(set = set, onClick = { onLearningSetClick(set) })
                    }
                } else if (!isLoading) {
                    item { EmptySetsCard(onCreateClick = onCreateClick) }
                }

                error?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            LoadingOverlay(isVisible = isLoading && learningSets.isEmpty())
        }
    }
}

@Composable
private fun SummaryCard(setCount: Int, onStatsClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "今日も少しずつ学ぼう",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "AIがあなた専用の4択問題を生成します",
                fontSize = 13.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "登録セット", fontSize = 12.sp)
                    Text(text = "$setCount", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                FilledTonalButton(onClick = onStatsClick) {
                    Text("統計を見る")
                }
            }
        }
    }
}

@Composable
private fun LearningSetCard(set: LearningSet, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = set.name.take(1).ifEmpty { "?" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = set.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(set.subject, set.category, set.unit)
                        .filter { it.isNotBlank() }
                        .joinToString(" ・ "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "›",
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySetsCard(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "📚", fontSize = 40.sp)
            Text(
                text = "学習セットがありません",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "科目・単元を登録すると、AIが問題を作成できます",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onCreateClick, modifier = Modifier.padding(top = 8.dp)) {
                Text("学習セットを作成")
            }
        }
    }
}
