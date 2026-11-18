package com.example.ailearning.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuizOptionButton(
    text: String,
    label: Char,
    state: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dark = isSystemInDarkTheme()
    val containerColor = when (state) {
        OptionState.DEFAULT -> MaterialTheme.colorScheme.surface
        OptionState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
        OptionState.CORRECT -> if (dark) Color(0xFF14301F) else Color(0xFFE8F7EE)
        OptionState.INCORRECT -> if (dark) Color(0xFF3B1515) else Color(0xFFFDECEC)
        OptionState.DISABLED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = when (state) {
        OptionState.DEFAULT -> MaterialTheme.colorScheme.onSurface
        OptionState.SELECTED -> MaterialTheme.colorScheme.onPrimaryContainer
        OptionState.CORRECT -> if (dark) Color(0xFF86EFAC) else Color(0xFF166534)
        OptionState.INCORRECT -> if (dark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
        OptionState.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val accent = when (state) {
        OptionState.DEFAULT -> MaterialTheme.colorScheme.outlineVariant
        OptionState.SELECTED -> MaterialTheme.colorScheme.primary
        OptionState.CORRECT -> Color(0xFF22C55E)
        OptionState.INCORRECT -> Color(0xFFEF4444)
        OptionState.DISABLED -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val badgeColor = when (state) {
        OptionState.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant
        OptionState.SELECTED -> MaterialTheme.colorScheme.primary
        OptionState.CORRECT -> Color(0xFF22C55E)
        OptionState.INCORRECT -> Color(0xFFEF4444)
        OptionState.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeContent = when (state) {
        OptionState.SELECTED, OptionState.CORRECT, OptionState.INCORRECT -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.5.dp, accent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeContent
                )
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            if (state == OptionState.CORRECT) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E)
                )
            } else if (state == OptionState.INCORRECT) {
                Icon(
                    imageVector = Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

enum class OptionState {
    DEFAULT, SELECTED, CORRECT, INCORRECT, DISABLED
}

@Composable
fun ProgressIndicator(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier
    )
}

@Composable
fun LinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun CustomTopAppBar(
    title: String,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = { }
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            navigationIcon?.let { vector ->
                IconButton(onClick = { onNavigationClick?.invoke() }) {
                    Icon(imageVector = vector, contentDescription = null)
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * アプリ共通のトップバー。角丸・フラットな背景でモダンに統一する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "戻る"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    title: String? = null,
    subtitle: String? = null,
    onCancel: (() -> Unit)? = null
) {
    if (!isVisible) return

    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(isVisible) {
        elapsedSeconds = 0
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(horizontal = 40.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title ?: "読み込み中…",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (elapsedSeconds >= 3) {
                    Text(
                        text = "${elapsedSeconds}秒経過",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onCancel != null) {
                    androidx.compose.material3.TextButton(onClick = onCancel) {
                        Text("キャンセル", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String?,
    icon: ImageVector? = null,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(17.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                    }
                }
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector? = null,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
            text = description,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryStatRow(
    stat: com.example.ailearning.domain.model.CategoryStatistics
) {
    val color = when {
        stat.accuracy >= 0.7 -> Color(0xFF16A34A)
        stat.accuracy >= 0.5 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }
    val progress by animateFloatAsState(
        targetValue = stat.accuracy.toFloat().coerceIn(0f, 1f),
        label = "categoryProgress"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stat.category, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${stat.correct}/${stat.total} 問 ・ 平均${stat.getFormattedAvgResponseTime()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${(stat.accuracy * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
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
    }
}
