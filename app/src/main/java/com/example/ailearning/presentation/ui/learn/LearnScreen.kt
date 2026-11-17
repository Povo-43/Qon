package com.example.ailearning.presentation.ui.learn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.LearningSession
import com.example.ailearning.domain.model.Question
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.ui.components.CustomTopAppBar
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.ui.components.OptionState
import com.example.ailearning.presentation.ui.components.ProgressIndicator
import com.example.ailearning.presentation.ui.components.QuizOptionButton
import com.example.ailearning.presentation.ui.components.StatCard
import com.example.ailearning.presentation.viewmodel.LearningViewModel
import com.example.ailearning.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

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

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
    AppTopBar(
        title = learningSetName,
        subtitle = modeLabel(mode),
        onBack = {
            if (!sessionComplete) {
                viewModel.reset()
            }
            onBack()
        },
        actions = {
            if (questions.isNotEmpty() && !sessionComplete) {
                TextButton(onClick = { viewModel.generateMoreQuestions(learningSetId) }, enabled = !isGenerating) {
                    Text("＋問題", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (questions.isNotEmpty()) {
                ProgressIndicator(
                    current = currentQuestionIndex + 1,
                    total = questions.size
                )
                Text(
                    text = "${currentQuestionIndex + 1} / ${questions.size} 問",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (questions.isEmpty() && !isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("問題がありません", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "設定画面でAPIキーを登録すると、AIがこの学習セットの問題を生成できます。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.startLearning(learningSetId, mode) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("問題を生成する")
                        }
                    }
                }
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
fun QuestionCard(
    question: Question,
    showResult: Boolean,
    lastAnswer: AnswerHistory?,
    onAnswerSelected: (Int, Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = question.questionText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                    val enabled = !showResult

                    QuizOptionButton(
                        text = option,
                        label = ('A' + index).toChar(),
                        state = state,
                        onClick = {
                            if (enabled) {
                                val responseTime = System.currentTimeMillis() - System.currentTimeMillis()
                                onAnswerSelected(index, responseTime)
                            }
                        },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ResultCard(
    question: Question,
    answer: AnswerHistory,
    onNext: () -> Unit
) {
    val isCorrect = answer.isCorrect

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCorrect) "正解！" else "不正解…",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "あなたの回答: ${('A' + answer.selectedAnswer).toChar()}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isCorrect) {
                Text(
                    text = "正解: ${question.getOptions()[question.correctAnswer]}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "解説: ${question.explanation}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Text(
                text = "回答時間: ${String.format("%.1f", answer.responseTimeMs / 1000.0)}秒",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("次の問題へ", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun SessionCompleteCard(
    session: LearningSession,
    onFinish: () -> Unit
) {
    val accuracy = if (session.totalQuestions > 0) session.correctAnswers.toDouble() / session.totalQuestions else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "学習セッション完了！",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatCard(
                    title = "問題数",
                    value = session.totalQuestions.toString(),
                    subtitle = null,
                    icon = null,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.width(0.dp).weight(1f)
                )
                StatCard(
                    title = "正解数",
                    value = session.correctAnswers.toString(),
                    subtitle = null,
                    icon = null,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.width(0.dp).weight(1f)
                )
                StatCard(
                    title = "正答率",
                    value = "${(accuracy * 100).toInt()}%",
                    subtitle = null,
                    icon = null,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.width(0.dp).weight(1f)
                )
            }

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("完了", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
