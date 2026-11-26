package com.example.ailearning.presentation.ui.manage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.domain.model.QuestionWithStats
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.ui.components.EmptyState
import com.example.ailearning.presentation.ui.components.StatCard
import com.example.ailearning.presentation.work.GenerationState
import com.example.ailearning.presentation.viewmodel.QuestionListViewModel

/**
 * 学習セットに作成された問題の一覧ページ。
 *
 * - 各問題の正答率を進捗バーで表示（未回答はグレー）
 * - 問題数を指定してバックグラウンドでAI生成を依頼できる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionListScreen(
    viewModel: QuestionListViewModel,
    learningSetId: String,
    learningSetName: String,
    onBack: () -> Unit
) {
    LaunchedEffect(learningSetId) {
        viewModel.load(learningSetId)
    }

    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val generationState by viewModel.observeGenerationState(learningSetId)
        .collectAsState(initial = GenerationState.IDLE)

    var selectedCount by remember { mutableStateOf(10) }

    val answeredCount = items.count { !it.isUnanswered }
    val accuracyList = items.mapNotNull { it.accuracy }
    val overallAccuracy = if (accuracyList.isEmpty()) null else accuracyList.average()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "問題一覧",
                subtitle = learningSetName.ifBlank { null },
                onBack = onBack
            )
        }
    ) { padding ->
        when {
            isLoading && items.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            items.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    EmptyState(
                        icon = Icons.Rounded.Quiz,
                        title = "問題がまだありません",
                        description = "下のボタンからAI問題を生成しましょう"
                    )
                    GenerateCard(
                        selectedCount = selectedCount,
                        onCountChange = { selectedCount = it },
                        isRunning = generationState == GenerationState.RUNNING,
                        onGenerate = { viewModel.requestGeneration(learningSetId, selectedCount) }
                    )
                    if (generationState == GenerationState.FAILED) {
                        Text(
                            text = "問題の生成に失敗しました。ネットワーク状況を確認してもう一度お試しください。",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "summary") {
                    SummaryRow(items.size, answeredCount, overallAccuracy)
                }
                item(key = "generate") {
                    GenerateCard(
                        selectedCount = selectedCount,
                        onCountChange = { selectedCount = it },
                        isRunning = generationState == GenerationState.RUNNING,
                        onGenerate = { viewModel.requestGeneration(learningSetId, selectedCount) }
                    )
                }
                if (generationState == GenerationState.RUNNING) {
                    item(key = "running") {
                        Text(
                            text = "AIがバックグラウンドで問題を生成中…\n完成すると自動でこの一覧に追加されます",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (generationState == GenerationState.FAILED) {
                    item(key = "failed") {
                        Text(
                            text = "問題の生成に失敗しました。ネットワーク状況を確認してもう一度お試しください。",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                items(items, key = { it.question.id }) { item ->
                    QuestionRow(item)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(total: Int, answered: Int, overallAccuracy: Double?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            title = "問題数",
            value = "$total",
            subtitle = "回答済み $answered",
            color = Color(0xFF4F46E5),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "平均正答率",
            value = overallAccuracy?.let { "${(it * 100).toInt()}%" } ?: "—",
            subtitle = "未回答 ${total - answered} 問",
            color = Color(0xFF16A34A),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GenerateCard(
    selectedCount: Int,
    onCountChange: (Int) -> Unit,
    isRunning: Boolean,
    onGenerate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text("新しい問題をAIで生成", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 50).forEach { count ->
                    FilterChip(
                        selected = selectedCount == count,
                        onClick = { onCountChange(count) },
                        label = { Text("${count}問") },
                        enabled = !isRunning
                    )
                }
            }

            Button(
                onClick = onGenerate,
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成中…", fontWeight = FontWeight.SemiBold)
                } else {
                    Text("${selectedCount}問をバックグラウンドで生成", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun QuestionRow(item: QuestionWithStats) {
    val q = item.question
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = q.questionText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val labels = listOf("A", "B", "C", "D")
                val options = q.getOptions()
                if (q.correctAnswer in 0..3) {
                    Text(
                        text = "正解 ${labels[q.correctAnswer]}. ${options[q.correctAnswer]}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (q.category.isNotBlank()) {
                    AssistChipMini(q.category)
                }
                AssistChipMini("難易度 ${q.difficulty}")
            }

            AccuracyBar(item)
        }
    }
}

@Composable
private fun AssistChipMini(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * 正答率の進捗バー。未回答はグレーの空バーで「未回答」と表示する。
 */
@Composable
private fun AccuracyBar(item: QuestionWithStats) {
    val accuracy = item.accuracy
    val progress by animateFloatAsState(
        targetValue = (accuracy ?: 0.0).toFloat().coerceIn(0f, 1f),
        label = "accuracyProgress"
    )
    val color = when {
        accuracy == null -> MaterialTheme.colorScheme.outlineVariant
        accuracy >= 0.7 -> Color(0xFF16A34A)
        accuracy >= 0.5 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }
    val label = if (item.isUnanswered) {
        "未回答"
    } else {
        "${item.correct}/${item.attempts}回中正解 ・ 正答率 ${(accuracy!! * 100).toInt()}%"
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (item.isUnanswered) MaterialTheme.colorScheme.onSurfaceVariant else color,
            fontWeight = if (item.isUnanswered) FontWeight.Normal else FontWeight.SemiBold
        )
    }
}
