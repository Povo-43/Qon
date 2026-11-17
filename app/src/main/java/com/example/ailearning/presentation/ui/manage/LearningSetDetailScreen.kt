package com.example.ailearning.presentation.ui.manage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel

@Composable
fun LearningSetDetailScreen(
    viewModel: LearningSetViewModel,
    learningSetId: String,
    onBack: () -> Unit,
    onLearnClick: (String, String) -> Unit,
    onStatsClick: (String) -> Unit
) {
    val learningSets by viewModel.learningSets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val set = learningSets.firstOrNull { it.id == learningSetId }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = set?.name ?: "学習セット",
                subtitle = set?.subject?.takeIf { it.isNotBlank() },
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (set == null) {
                Text(
                    text = if (isLoading) "読み込み中…" else "学習セットが見つかりません",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = set.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoChip(text = set.subject)
                            InfoChip(text = set.category)
                        }
                        InfoChip(text = set.unit)
                        if (set.scope.isNotBlank()) {
                            Text(
                                text = "範囲: ${set.scope}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(text = "学習モード", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                ModeCard(
                    title = "連続学習",
                    description = "AIが新しい4択問題を生成して出題します",
                    onClick = { onLearnClick(learningSetId, "continuous") }
                )
                ModeCard(
                    title = "復習",
                    description = "間違えた問題を中心にもう一度解きます",
                    onClick = { onLearnClick(learningSetId, "review") }
                )
                ModeCard(
                    title = "ランダム",
                    description = "保存済みの問題からランダムに出題します",
                    onClick = { onLearnClick(learningSetId, "random") }
                )

                HorizontalDivider()

                OutlinedButton(
                    onClick = { onStatsClick(learningSetId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("統計を見る")
                }

                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("この学習セットを削除")
                }

                error?.let { message ->
                    Text(text = message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }

    if (showDeleteDialog && set != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("学習セットを削除") },
            text = { Text("「${set.name}」を削除します。この操作は取り消せません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteLearningSet(set.id) { onBack() }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    if (text.isBlank()) return
    SuggestionChip(
        onClick = {},
        label = { Text(text, fontSize = 12.sp) }
    )
}

@Composable
private fun ModeCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = "›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
