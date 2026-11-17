package com.example.ailearning.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel
import java.util.Calendar

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

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 5 -> "こんばんは"
            hour < 11 -> "おはようございます"
            hour < 18 -> "こんにちは"
            else -> "こんばんは"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("学習セット", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = greeting,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Qon",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = "設定")
                        }
                    }
                }

                item {
                    HeroCard(
                        setCount = learningSets.size,
                        onStatsClick = onStatsClick,
                        onStartClick = {
                            val first = learningSets.firstOrNull()
                            if (first != null) onLearningSetClick(first) else onCreateClick()
                        }
                    )
                }

                if (learningSets.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "学習セット",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${learningSets.size} 件",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(learningSets, key = { it.id }) { set ->
                        LearningSetCard(
                            set = set,
                            onClick = { onLearningSetClick(set) }
                        )
                    }
                } else if (!isLoading) {
                    item { EmptySetsCard(onCreateClick = onCreateClick) }
                }

                error?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            LoadingOverlay(isVisible = isLoading && learningSets.isEmpty())
        }
    }
}

@Composable
private fun HeroCard(
    setCount: Int,
    onStatsClick: () -> Unit,
    onStartClick: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F46E5),
            Color(0xFF7C3AED)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(gradient)
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Text(
                        text = "AI DRILL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = "今日も、\n少しずつ前へ。",
                    fontSize = 26.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "AIがあなた専用の4択問題を生成します",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStartClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF4F46E5)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("学習を始める", fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = onStatsClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("統計")
                    }
                }
                Text(
                    text = "登録済み $setCount セット",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private val accentPairs = listOf(
    Color(0xFF6366F1) to Color(0xFF8B5CF6),
    Color(0xFF0EA5E9) to Color(0xFF06B6D4),
    Color(0xFF10B981) to Color(0xFF14B8A6),
    Color(0xFFF59E0B) to Color(0xFFF97316),
    Color(0xFFEC4899) to Color(0xFFF43F5E)
)

@Composable
private fun LearningSetCard(set: LearningSet, onClick: () -> Unit) {
    val pair = accentPairs[set.name.hashCode().and(Int.MAX_VALUE) % accentPairs.size]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(pair.first, pair.second))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = set.name.take(1).ifEmpty { "Q" },
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = set.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(set.subject, set.category, set.unit)
                        .filter { it.isNotBlank() }
                        .joinToString("  ·  "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySetsCard(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = "最初の学習セットを作成",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "科目と単元を登録すると、AIが問題を作成します。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onCreateClick,
                modifier = Modifier.padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("作成する")
            }
        }
    }
}
