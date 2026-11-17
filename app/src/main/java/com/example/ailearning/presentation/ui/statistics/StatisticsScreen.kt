package com.example.ailearning.presentation.ui.statistics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.domain.model.StatisticsPeriod
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.ui.components.CategoryStatRow
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.ui.components.StatCard
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel
import com.example.ailearning.presentation.viewmodel.StatisticsViewModel
import kotlinx.serialization.json.Json
import com.example.ailearning.domain.model.AiAnalysisResult

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    learningSetViewModel: LearningSetViewModel,
    initialSetId: String? = null,
    onBack: (() -> Unit)? = null
) {
    val learningSets by learningSetViewModel.learningSets.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()
    val difficultyStats by viewModel.difficultyStats.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedSetId by remember { mutableStateOf<String?>(initialSetId) }
    var period by remember { mutableStateOf(StatisticsPeriod.ALL) }

    LaunchedEffect(learningSets, initialSetId) {
        if (selectedSetId == null || learningSets.none { it.id == selectedSetId }) {
            selectedSetId = learningSets.firstOrNull { it.id == initialSetId }?.id
                ?: learningSets.firstOrNull()?.id
        }
    }

    LaunchedEffect(selectedSetId, period) {
        selectedSetId?.let { viewModel.loadStatistics(it, period) }
    }

    val selectedSet = learningSets.firstOrNull { it.id == selectedSetId }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "統計・AI分析",
                subtitle = selectedSet?.name,
                onBack = onBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (learningSets.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📊", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("統計はまだありません", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "学習セットを作成して問題を解くと、ここに正答率や苦手分野が表示されます。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 学習セット選択
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        learningSets.forEach { set ->
                            FilterChip(
                                selected = set.id == selectedSetId,
                                onClick = { selectedSetId = set.id },
                                label = { Text(set.name, fontSize = 12.sp) }
                            )
                        }
                    }

                    // 期間選択
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeriodChip("今日", StatisticsPeriod.TODAY, period) { period = it }
                        PeriodChip("7日", StatisticsPeriod.WEEK, period) { period = it }
                        PeriodChip("30日", StatisticsPeriod.MONTH, period) { period = it }
                        PeriodChip("全期間", StatisticsPeriod.ALL, period) { period = it }
                    }

                    overallStats?.let { stats ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "解答数",
                                    value = stats.totalQuestions.toString(),
                                    subtitle = null,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "正答率",
                                    value = "${(stats.overallAccuracy * 100).toInt()}%",
                                    subtitle = null,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "学習時間",
                                    value = stats.getFormattedStudyTime(),
                                    subtitle = null,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "連続学習",
                                    value = "${stats.currentStreakDays}日",
                                    subtitle = "最長 ${stats.longestStreakDays}日",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } ?: Text(
                        text = "この学習セットにはまだ解答履歴がありません。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (categoryStats.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("分野別の正答率", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                categoryStats.forEach { stat ->
                                    CategoryStatRow(stat = stat)
                                }
                            }
                        }
                    }

                    if (difficultyStats.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("難易度別の正答率", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                difficultyStats.forEach { stat ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(stat.getDifficultyLabel(), fontSize = 13.sp)
                                        Text(
                                            "${stat.correct}/${stat.total}  ${(stat.accuracy * 100).toInt()}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // AI分析
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("AI分析レポート", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            if (isAnalyzing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text("AIが分析中…", fontSize = 13.sp)
                                }
                            } else {
                                val analysis = aiAnalysis
                                if (analysis != null) {
                                    AnalysisContent(content = analysis.content)
                                } else {
                                    Text(
                                        "5問以上解答すると、AIが苦手分野とおすすめの学習法を分析します。",
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = { viewModel.generateAnalysis("weakness") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (analysis == null) "AI分析を実行" else "再分析する")
                                }
                            }
                        }
                    }

                    error?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            LoadingOverlay(isVisible = isLoading)
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    value: StatisticsPeriod,
    current: StatisticsPeriod,
    onSelect: (StatisticsPeriod) -> Unit
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label, fontSize = 12.sp) }
    )
}

@Composable
private fun AnalysisContent(content: String) {
    val parsed = remember(content) {
        runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<AiAnalysisResult>(content)
        }.getOrNull()
    }
    if (parsed != null) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(parsed.summary, fontSize = 13.sp)
            if (parsed.recommendations.isNotEmpty()) {
                Text("おすすめ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                parsed.recommendations.forEach {
                    Text("・$it", fontSize = 12.sp)
                }
            }
        }
    } else {
        Text(content, fontSize = 12.sp)
    }
}
