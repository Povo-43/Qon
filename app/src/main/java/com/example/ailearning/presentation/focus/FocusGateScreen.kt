package com.example.ailearning.presentation.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 集中モードのゲート画面。
 * ブロック対象アプリの前面に重なり、1問正解すると閉じる。
 */
@Composable
fun FocusGateScreen(
    controller: FocusGateController,
    testMode: Boolean
) {
    val state by controller.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xE60B0F1E),
                        Color(0xF20D1226),
                        Color(0xE60B0F1E)
                    )
                )
            )
            .systemBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (testMode) {
            IconButton(
                onClick = controller::dismissTestGate,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "テストを閉じる", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Qon ロック",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "このアプリを使うには問題に正解してください",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                when (state.phase) {
                    FocusGateUiState.Phase.LOADING -> GateLoading(state)
                    FocusGateUiState.Phase.ANSWERING -> GateQuestion(state, controller)
                    FocusGateUiState.Phase.RESULT_CORRECT -> GateCorrect(state)
                    FocusGateUiState.Phase.RESULT_WRONG -> GateWrong(state, controller)
                    FocusGateUiState.Phase.UNLOCKED -> GateUnlocked(state)
                    FocusGateUiState.Phase.FAIL_OPEN -> GateFailOpen(state)
                }
            }

            Text(
                "正解すると次の画面ロックまでアプリが使えます",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun GateLoading(state: FocusGateUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
        Text("問題を準備中…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "少し待っても問題が出ない場合は自動的に解除されます",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GateQuestion(state: FocusGateUiState, controller: FocusGateController) {
    val question = state.question ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "問題 ${state.attempts + 1}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (state.requiredCorrect > 1) {
                Text(
                    "正解 ${state.correctCount} / ${state.requiredCorrect}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Text(
            question.questionText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(2.dp))

        val options = question.getOptions()
        options.forEachIndexed { index, option ->
            OptionRow(
                label = ('A' + index).toString(),
                text = option,
                onClick = { controller.selectAnswer(index) },
                highlight = OptionHighlight.NONE
            )
        }
    }
}

@Composable
private fun GateCorrect(state: FocusGateUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color(0xFF22C55E).copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(28.dp)
            )
        }
        Text("正解！", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "正解 ${state.correctCount} / ${state.requiredCorrect}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GateWrong(state: FocusGateUiState, controller: FocusGateController) {
    val question = state.question ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "不正解…",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        if (state.showCorrectAnswer) {
            val options = question.getOptions()
            options.forEachIndexed { index, option ->
                val highlight = when {
                    index == question.correctAnswer -> OptionHighlight.CORRECT
                    index == state.selectedAnswer -> OptionHighlight.WRONG
                    else -> OptionHighlight.NONE
                }
                OptionRow(
                    label = ('A' + index).toString(),
                    text = option,
                    onClick = {},
                    highlight = highlight,
                    enabled = false
                )
            }

            if (question.explanation.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        question.explanation,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            Text(
                "もう一度挑戦してください",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = controller::requestNextQuestion,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("次の問題へ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun GateUnlocked(state: FocusGateUiState) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(280),
        label = "unlockScale"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp, horizontal = 24.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF22C55E).copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(30.dp)
            )
        }
        Text("解除しました", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
            "次の画面ロックまでアプリを利用できます",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GateFailOpen(state: FocusGateUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            state.failOpenMessage ?: FocusGateSession.FAIL_OPEN_MESSAGE,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private enum class OptionHighlight { NONE, CORRECT, WRONG }

@Composable
private fun OptionRow(
    label: String,
    text: String,
    onClick: () -> Unit,
    highlight: OptionHighlight,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = when (highlight) {
            OptionHighlight.CORRECT -> Color(0xFF22C55E).copy(alpha = 0.16f)
            OptionHighlight.WRONG -> colors.error.copy(alpha = 0.12f)
            OptionHighlight.NONE -> colors.surfaceVariant.copy(alpha = 0.6f)
        },
        label = "optionContainer"
    )
    val border = when (highlight) {
        OptionHighlight.CORRECT -> Color(0xFF22C55E)
        OptionHighlight.WRONG -> colors.error
        OptionHighlight.NONE -> colors.outlineVariant.copy(alpha = 0.6f)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(colors.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
            Text(
                text,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
