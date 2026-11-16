package com.example.ailearning.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val containerColor = when (state) {
        OptionState.DEFAULT -> MaterialTheme.colorScheme.surface
        OptionState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
        OptionState.CORRECT -> Color(0xFFC8E6C9)
        OptionState.INCORRECT -> Color(0xFFFFCDD2)
        OptionState.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (state) {
        OptionState.DEFAULT -> MaterialTheme.colorScheme.onSurface
        OptionState.SELECTED -> MaterialTheme.colorScheme.onPrimaryContainer
        OptionState.CORRECT -> Color(0xFF2E7D32)
        OptionState.INCORRECT -> Color(0xFFC62828)
        OptionState.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = text,
                fontSize = 16.sp,
                color = contentColor,
                modifier = Modifier.fillMaxWidth()
            )
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
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(trackColor, RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .width((progress.coerceIn(0f, 1f) * 300f).dp)
                .height(4.dp)
                .background(progressColor, RoundedCornerShape(2.dp))
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
                IconButton(onClick = { onNavigationClick?.invoke(); Unit }) {
                    Icon(imageVector = vector, contentDescription = null)
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun LoadingOverlay(isVisible: Boolean) {
    if (isVisible) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
            }
            Text(text = title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            subtitle?.let { Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null)
        }
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CategoryStatRow(
    stat: com.example.ailearning.domain.model.CategoryStatistics
) {
    val color = when {
        stat.accuracy >= 0.7 -> Color(0xFF2E7D32)
        stat.accuracy >= 0.5 -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stat.category, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${stat.correct}/${stat.total} 問  平均${stat.getFormattedAvgResponseTime()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${(stat.accuracy * 100).toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Box(
                modifier = Modifier.width(80.dp).height(6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier.width((stat.accuracy.toFloat() * 80f).dp).height(6.dp).background(color, RoundedCornerShape(3.dp))
                ) { }
            }
        }
    }
}
