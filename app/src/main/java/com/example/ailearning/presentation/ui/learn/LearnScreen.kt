package com.example.ailearning.presentation.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.LearningSession
import com.example.ailearning.domain.model.Question
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.ui.components.LinearProgressIndicator
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.ui.components.OptionState
import com.example.ailearning.presentation.ui.components.QuizOptionButton
import com.example.ailearning.presentation.viewmodel.LearningViewModel

@Composable
fun LearnScreen(
    viewModel: LearningViewModel,
    learningSetId: String,
    learningSetName: String,
    mode: String = "continuous",
    onBack: () -> Unit
) {
    val questions by viewModel.questions.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val session by viewModel.session.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val lastAnswer by viewModel.lastAnswer.collectAsState()
    val sessionComplete by viewModel.sessionComplete.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()

    var questionStartedAt by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(learningSetId, mode) {
        viewModel.reset()
        viewModel.startLearning(learningSetId, mode)
    }

    LaunchedEffect(currentQuestion?.id) {
        questionStartedAt = System.currentTimeMillis()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = learningSetName,
                subtitle = modeLabel(mode),
                onBack = {
                    if (!sessionComplete) viewModel.reset()
                    onBack()
                },
                actions = {
                    if (questions.isNotEmpty() && !sessionComplete) {
                        IconButton(
                            onClick = { viewModel.generateMoreQuestions(learningSetId) },
                            enabled = !isGenerating
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "問題を追加")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (questions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "問題 ${currentQuestionIndex + 1}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentQuestionIndex + 1} / ${questions.size}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = (currentQuestionIndex + 1).toFloat() / questions.size
                        )
                    }
                }

                if (questions.isEmpty() && !isLoading) {
                    EmptyQuestionsCard(
                        onGenerate = { viewModel.startLearning(learningSetId, mode) }
                    )
                }

                currentQuestion?.let { question ->
                    QuestionCard(
                        question = question,
                        showResult = showResult,
                        lastAnswer = lastAnswer,
                        onAnswerSelected = { answer, _ ->
                            viewModel.submitAnswer(
                                answer,
                                (System.currentTimeMillis() - questionStartedAt).coerceAtLeast(0)
                            )
                        }
                    )
                }

                if (showResult) {
                    val question = currentQuestion
                    val answer = lastAnswer
                    if (question != null && answer != null) {
                        ResultCard(
                            question = question,
                            answer = answer,
                            onNext = { viewModel.nextQuestion() }
                        )
                    }
                }

                if (sessionComplete && session != null) {
                    SessionCompleteCard(
                        session = session!!,
                        onFinish = {
                            viewModel.reset()
                            onBack()
                        }
                    )
                }

                error?.let { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.clearError() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text("閉じる", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.clearError()
                                        if (questions.isEmpty()) {
                                            viewModel.startLearning(learningSetId, mode)
                                        } else {
                                            viewModel.generateMoreQuestions(learningSetId)
                                        }
                                    },
                                    enabled = !isGenerating && !isLoading,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                        contentColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("再試行", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            LoadingOverlay(isVisible = isLoading || isGenerating)
        }
    }
}

private fun modeLabel(mode: String): String = when (mode) {
    "continuous" -> "連続学習"
    "review" -> "復習"
    "random" -> "ランダム"
    "unit" -> "単元学習"
    else -> "学習"
}

@Composable
private fun QuestionCard(
    question: Question,
    showResult: Boolean,
    lastAnswer: AnswerHistory?,
    onAnswerSelected: (Int, Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Text(
                        text = question.category.ifBlank { "問題" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape
                ) {
                    Text(
                        text = "難易度 ${question.difficulty}",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = question.questionText,
                fontSize = 18.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.getOptions().forEachIndexed { index, option ->
                    val isCorrect = index == question.correctAnswer
                    val state = when {
                        showResult && lastAnswer != null -> when {
                            lastAnswer.selectedAnswer == index && isCorrect -> OptionState.CORRECT
                            lastAnswer.selectedAnswer == index && !isCorrect -> OptionState.INCORRECT
                            isCorrect -> OptionState.CORRECT
                            else -> OptionState.DISABLED
                        }
                        else -> OptionState.DEFAULT
                    }

                    QuizOptionButton(
                        text = option,
                        label = ('A' + index),
                        state = state,
                        onClick = {
                            if (!showResult) {
                                onAnswerSelected(index, 0)
                            }
                        },
                        enabled = !showResult,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    question: Question,
    answer: AnswerHistory,
    onNext: () -> Unit
) {
    val isCorrect = answer.isCorrect
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val container = if (isCorrect) {
        if (dark) Color(0xFF14301F) else Color(0xFFE8F7EE)
    } else {
        if (dark) Color(0xFF3B1515) else Color(0xFFFDECEC)
    }
    val onContainer = if (isCorrect) {
        if (dark) Color(0xFF86EFAC) else Color(0xFF166534)
    } else {
        if (dark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCorrect) "正解！" else "不正解",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = Color.White.copy(alpha = 0.6f),
                    contentColor = onContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${String.format("%.1f", answer.responseTimeMs / 1000.0)}秒",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (!isCorrect) {
                Text(
                    text = "正解: ${question.getOptions().getOrElse(question.correctAnswer) { "" }}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = question.explanation.ifBlank { "解説はありません" },
                fontSize = 13.sp,
                lineHeight = 21.sp,
                color = onContainer.copy(alpha = 0.85f)
            )

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = onContainer,
                    contentColor = Color.White
                )
            ) {
                Text("次の問題へ", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SessionCompleteCard(
    session: LearningSession,
    onFinish: () -> Unit
) {
    val accuracy = if (session.totalQuestions > 0) {
        session.correctAnswers.toDouble() / session.totalQuestions
    } else {
        0.0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF4F46E5),
                            Color(0xFF7C3AED)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "セッション完了",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SessionStat("問題数", session.totalQuestions.toString(), Modifier.weight(1f))
                    SessionStat("正解", session.correctAnswers.toString(), Modifier.weight(1f))
                    SessionStat("正答率", "${(accuracy * 100).toInt()}%", Modifier.weight(1f))
                }
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF4F46E5)
                    )
                ) {
                    Text("完了", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SessionStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.16f),
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun EmptyQuestionsCard(onGenerate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text("問題がありません", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "設定でAPIキーを登録すると、AIがこの学習セットの問題を生成できます。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("問題を生成する", fontWeight = FontWeight.Bold)
            }
        }
    }
}
