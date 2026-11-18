package com.example.ailearning.presentation.ui.statistics

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.domain.model.AiAnalysisResult
import com.example.ailearning.domain.model.DailyStatistics
import com.example.ailearning.domain.model.GlobalStatistics
import com.example.ailearning.domain.model.StatisticsPeriod
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.ui.components.CategoryStatRow
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.ui.components.StatCard
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel
import com.example.ailearning.presentation.viewmodel.StatisticsViewModel
import kotlinx.serialization.json.Json

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
    val trendData by viewModel.trendData.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val globalStats by viewModel.globalStats.collectAsState()
    val exportUri by viewModel.exportUri.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
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

    // 全セット横断統括統計を読み込む
    LaunchedEffect(Unit) {
        viewModel.loadGlobalStatistics()
    }

    val selectedSet = learningSets.firstOrNull { it.id == selectedSetId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("統計はまだありません", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "学習セットを作成して問題を解くと、ここに正答率や苦手分野が表示されます。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeriodChip("今日", StatisticsPeriod.TODAY, period) { period = it }
                        PeriodChip("7日", StatisticsPeriod.WEEK, period) { period = it }
                        PeriodChip("30日", StatisticsPeriod.MONTH, period) { period = it }
                        PeriodChip("全期間", StatisticsPeriod.ALL, period) { period = it }
                    }

                    overallStats?.let { stats ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "解答数",
                                    value = stats.totalQuestions.toString(),
                                    subtitle = null,
                                    icon = Icons.Rounded.Quiz,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "正答率",
                                    value = "${(stats.overallAccuracy * 100).toInt()}%",
                                    subtitle = null,
                                    icon = Icons.Rounded.CheckCircle,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    title = "学習時間",
                                    value = stats.getFormattedStudyTime(),
                                    subtitle = null,
                                    icon = Icons.Rounded.Timer,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "連続学習",
                                    value = "${stats.currentStreakDays}日",
                                    subtitle = "最長 ${stats.longestStreakDays}日",
                                    icon = Icons.Rounded.LocalFireDepartment,
                                    color = Color(0xFFF97316),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } ?: Text(
                        text = "この学習セットにはまだ解答履歴がありません。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    // ---- 全体統括統計（すべての学習セット横断）----
                    globalStats?.let { g ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    "全体統括統計（すべてのセット）",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatCard(
                                        title = "総解答数",
                                        value = g.totalQuestions.toString(),
                                        subtitle = "全セット合計",
                                        icon = Icons.Rounded.Quiz,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        title = "全体正答率",
                                        value = "${(g.overallAccuracy * 100).toInt()}%",
                                        subtitle = null,
                                        icon = Icons.Rounded.CheckCircle,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        title = "総学習時間",
                                        value = g.getFormattedStudyTime(),
                                        subtitle = null,
                                        icon = Icons.Rounded.Timer,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    "ソース別内訳",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                g.sourceBreakdown().forEach { s ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column {
                                            Text(s.label, fontSize = 13.sp)
                                            Text(
                                                "${s.correct}/${s.total} 問",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            "${(s.accuracy * 100).toInt()}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Divider()
                                }
                                // 集中モード専用
                                if (g.focusUnlockCount > 0 || g.focusWrongCount > 0) {
                                    Text(
                                        "集中モード（アプリロック）",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        StatCard(
                                            title = "解除回数",
                                            value = g.focusUnlockCount.toString(),
                                            subtitle = "問題に正解して解除",
                                            icon = Icons.Rounded.Quiz,
                                            color = Color(0xFFF97316),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatCard(
                                            title = "誤答回数",
                                            value = g.focusWrongCount.toString(),
                                            subtitle = "集中モードでの不正解",
                                            icon = Icons.Rounded.Timer,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (trendData.any { it.total > 0 }) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("学習の推移", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "日ごとの解答数",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                TrendBars(trendData.takeLast(14))
                            }
                        }
                    }

                    if (categoryStats.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("難易度別の正答率", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                difficultyStats.forEach { stat ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stat.getDifficultyLabel(), fontSize = 13.sp)
                                        Text(
                                            "${stat.correct}/${stat.total}  ${(stat.accuracy * 100).toInt()}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Psychology, contentDescription = null)
                                Text("AI分析レポート", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            if (isAnalyzing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("AIが分析中…", fontSize = 12.sp)
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
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(if (analysis == null) "AI分析を実行" else "再分析する")
                                }
                            }
                        }
                    }

                    // ---- 統計エクスポート----
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null)
                                Text("統計エクスポート", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "全解答履歴を CSV 形式でエクスポートし、PC やメールで保存・共有できます。",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                lineHeight = 17.sp
                            )
                            if (isExporting) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("エクスポート中…", fontSize = 12.sp)
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.exportStatistics() },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("CSV でエクスポート", fontWeight = FontWeight.Bold)
                                    }
                                    exportUri?.let { uri ->
                                        val context = LocalContext.current
                                        TextButton(
                                            onClick = {
                                                val intent = viewModel.shareExport(context)
                                                if (intent != null) {
                                                    context.startActivity(
                                                        Intent.createChooser(intent, "統計を共有")
                                                    )
                                                }
                                            },
                                            modifier = Modifier.height(46.dp)
                                        ) {
                                            Text("共有", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    error?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))
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
private fun TrendBars(data: List<DailyStatistics>) {
    val max = (data.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { day ->
            val fraction = if (day.total > 0) {
                (day.total.toFloat() / max).coerceAtLeast(0.06f)
            } else {
                0f
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            data.firstOrNull()?.date?.takeLast(5).orEmpty(),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            data.lastOrNull()?.date?.takeLast(5).orEmpty(),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
            Text(parsed.summary, fontSize = 13.sp, lineHeight = 20.sp)
            if (parsed.recommendations.isNotEmpty()) {
                Text("おすすめ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                parsed.recommendations.forEach {
                    Text("・$it", fontSize = 12.sp)
                }
            }
        }
    } else {
        Text(content, fontSize = 12.sp)
    }
}
