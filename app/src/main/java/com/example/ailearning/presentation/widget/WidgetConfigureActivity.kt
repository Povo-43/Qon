package com.example.ailearning.presentation.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.domain.model.WidgetConfig
import com.example.ailearning.domain.model.WidgetColors
import com.example.ailearning.di.AppContainer
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.ui.theme.AiLearningTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ウィジェット追加・再設定時に表示される設定画面。
 * 学習セット・配色・キャッシュ数・更新間隔などをカスタマイズできる。
 */
class WidgetConfigureActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setResultCancelledUntilSave()

        val activity = this
        setContent {
            AiLearningTheme {
                WidgetConfigureScreen(
                    widgetId = widgetId,
                    onClose = { finish() },
                    onSaved = {
                        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        activity.setResult(Activity.RESULT_OK, result)
                        activity.finish()
                    }
                )
            }
        }
    }

    /** 保存前に戻られた場合はウィジェット追加をキャンセル扱いにする。 */
    private fun setResultCancelledUntilSave() {
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
    }
}

@Composable
private fun WidgetConfigureScreen(
    widgetId: Int,
    onClose: () -> Unit,
    onSaved: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var learningSets by remember { mutableStateOf<List<com.example.ailearning.domain.model.LearningSet>>(emptyList()) }
    var existingConfig by remember { mutableStateOf<WidgetConfig?>(null) }
    var loaded by remember { mutableStateOf(false) }

    // 設定値
    var selectedSetId by remember { mutableStateOf<String?>(null) }
    var themeId by remember { mutableStateOf("indigo") }
    var customBackground by remember { mutableStateOf(0) }
    var customAccent by remember { mutableStateOf(0) }
    var questionCount by remember { mutableStateOf(10f) }
    var updateIntervalMinutes by remember { mutableStateOf(30) }
    var showExplanation by remember { mutableStateOf(true) }
    var autoNext by remember { mutableStateOf(false) }

    // 既存設定・学習セットを読み込む
    LaunchedEffect(widgetId) {
        val config = withContext(Dispatchers.IO) {
            AppContainer.widgetConfigRepository.getByWidgetId(widgetId).getOrNull()
        }
        val sets = withContext(Dispatchers.IO) {
            AppContainer.getLearningSetsUseCase().first()
        }
        existingConfig = config
        learningSets = sets
        if (config != null) {
            selectedSetId = config.learningSetId
            themeId = config.themeId
            customBackground = config.backgroundColor
            customAccent = config.accentColor
            questionCount = config.questionCount.toFloat()
            updateIntervalMinutes = config.updateIntervalMinutes
            showExplanation = config.showExplanation
            autoNext = config.autoNext
        }
        if (selectedSetId == null) {
            selectedSetId = sets.firstOrNull()?.id
        }
        loaded = true
    }

    val colors = remember(themeId, customBackground, customAccent) {
        WidgetColors.preset(themeId).withOverrides(customBackground, customAccent)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "ウィジェット設定",
                subtitle = "配色や表示内容をカスタマイズできます",
                onBack = onClose
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
                Button(
                    onClick = {
                        val setId = selectedSetId ?: return@Button
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val base = existingConfig
                                    ?: WidgetConfig(widgetId = widgetId, learningSetId = setId)
                                val newConfig = base.copy(
                                    learningSetId = setId,
                                    questionCount = questionCount.toInt(),
                                    updateIntervalMinutes = updateIntervalMinutes,
                                    showExplanation = showExplanation,
                                    autoNext = autoNext,
                                    themeId = themeId,
                                    backgroundColor = customBackground,
                                    accentColor = customAccent
                                )
                                AppContainer.widgetConfigRepository.insert(newConfig)
                                // キャッシュを作り直し、保存前に直接描画する。
                                // （構成アクティビティありのウィジェットは追加直後に onUpdate が
                                //   呼ばれないため、ここで最初の RemoteViews を用意しておく）
                                AppContainer.updateWidgetCacheUseCase(widgetId)
                                val config = AppContainer.widgetConfigRepository
                                    .getByWidgetId(widgetId).getOrNull() ?: newConfig
                                val question = AppContainer.getWidgetQuestionUseCase(widgetId).getOrNull()
                                WidgetRenderer.render(
                                    context,
                                    android.appwidget.AppWidgetManager.getInstance(context),
                                    widgetId,
                                    config,
                                    WidgetRenderer.WidgetState(
                                        question = question,
                                        questionNumber = if (question != null) config.currentIndex + 1 else 0
                                    )
                                )
                            }
                            WidgetScheduler.schedulePeriodicRefresh(
                                context, widgetId, updateIntervalMinutes, replace = true
                            )
                            WidgetScheduler.requestRefresh(context, widgetId)
                            onSaved()
                        }
                    },
                    enabled = loaded && selectedSetId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("保存", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { padding ->
        if (!loaded) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("読み込み中…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (learningSets.isEmpty()) {
                EmptySetsCard(onClose = onClose)
            } else {
                LearningSetSection(
                    learningSets = learningSets,
                    selectedSetId = selectedSetId,
                    onSelect = { selectedSetId = it }
                )
            }

            PreviewCard(colors = colors)

            ThemeSection(
                selectedThemeId = themeId,
                customBackground = customBackground,
                customAccent = customAccent,
                onThemeSelect = { id ->
                    themeId = id
                    // プリセット選択で個別色をリセット
                    customBackground = 0
                    customAccent = 0
                },
                onBackgroundSelect = { color -> customBackground = color },
                onAccentSelect = { color -> customAccent = color }
            )

            OptionsSection(
                questionCount = questionCount,
                onQuestionCountChange = { questionCount = it },
                updateIntervalMinutes = updateIntervalMinutes,
                onUpdateIntervalChange = { updateIntervalMinutes = it },
                showExplanation = showExplanation,
                onShowExplanationChange = { showExplanation = it },
                autoNext = autoNext,
                onAutoNextChange = { autoNext = it }
            )

            Text(
                text = "変更は保存後にウィジェットへ反映されます。ホーム画面でウィジェットを長押し → 「設定」からいつでも変更できます。",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ---------------------------------------------------------------- sections

@Composable
private fun LearningSetSection(
    learningSets: List<com.example.ailearning.domain.model.LearningSet>,
    selectedSetId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = learningSets.firstOrNull { it.id == selectedSetId }

    SettingsCard(title = "学習セット") {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected?.name ?: "選択してください",
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                learningSets.forEach { set ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(set.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (set.subject.isNotBlank()) {
                                    Text(
                                        set.subject,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelect(set.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(
    selectedThemeId: String,
    customBackground: Int,
    customAccent: Int,
    onThemeSelect: (String) -> Unit,
    onBackgroundSelect: (Int) -> Unit,
    onAccentSelect: (Int) -> Unit
) {
    SettingsCard(title = "テーマ") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WidgetColors.PRESETS.forEach { preset ->
                val isSelected = customBackground == 0 && customAccent == 0 && preset.id == selectedThemeId
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(preset.background))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape
                            )
                            .clickable { onThemeSelect(preset.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        // アクセントのドット
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(preset.accent))
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.001f))
                            )
                        }
                    }
                    Text(
                        text = preset.label,
                        fontSize = 9.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("背景色", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        SwatchRow(
            swatches = BACKGROUND_SWATCHES,
            selected = customBackground,
            onSelect = onBackgroundSelect
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("アクセントカラー", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        SwatchRow(
            swatches = ACCENT_SWATCHES,
            selected = customAccent,
            onSelect = onAccentSelect
        )

        if (customBackground != 0 || customAccent != 0) {
            TextButton(onClick = {
                onBackgroundSelect(0)
                onAccentSelect(0)
            }) {
                Text("カスタム色をリセット", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SwatchRow(
    swatches: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        swatches.forEach { colorInt ->
            val isSelected = colorInt == selected
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape
                    )
                    .clickable { onSelect(colorInt) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "選択中",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionsSection(
    questionCount: Float,
    onQuestionCountChange: (Float) -> Unit,
    updateIntervalMinutes: Int,
    onUpdateIntervalChange: (Int) -> Unit,
    showExplanation: Boolean,
    onShowExplanationChange: (Boolean) -> Unit,
    autoNext: Boolean,
    onAutoNextChange: (Boolean) -> Unit
) {
    var intervalExpanded by remember { mutableStateOf(false) }
    val intervalLabel = when (updateIntervalMinutes) {
        15 -> "15分"
        30 -> "30分"
        60 -> "1時間"
        180 -> "3時間"
        360 -> "6時間"
        720 -> "12時間"
        else -> "${updateIntervalMinutes}分"
    }

    SettingsCard(title = "動作") {
        Text(
            text = "キャッシュする問題数",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = questionCount,
                onValueChange = onQuestionCountChange,
                valueRange = 5f..20f,
                steps = 3,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    thumbColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "${questionCount.toInt()}問",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(52.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "更新間隔",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = intervalExpanded,
            onExpandedChange = { intervalExpanded = it }
        ) {
            OutlinedTextField(
                value = intervalLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = intervalExpanded,
                onDismissRequest = { intervalExpanded = false }
            ) {
                listOf(15, 30, 60, 180, 360, 720).forEach { minutes ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (minutes) {
                                    15 -> "15分"
                                    30 -> "30分"
                                    60 -> "1時間"
                                    180 -> "3時間"
                                    360 -> "6時間"
                                    else -> "12時間"
                                },
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            onUpdateIntervalChange(minutes)
                            intervalExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        ToggleRow(
            title = "解説を表示",
            subtitle = "回答後に解説を表示します",
            checked = showExplanation,
            onChange = onShowExplanationChange
        )
        ToggleRow(
            title = "自動で次の問題へ",
            subtitle = "回答後、数秒で次の問題に切り替えます",
            checked = autoNext,
            onChange = onAutoNextChange
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ---------------------------------------------------------------- preview

@Composable
private fun PreviewCard(colors: WidgetColors) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "プレビュー",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        WidgetPreview(colors = colors)
    }
}

@Composable
private fun WidgetPreview(colors: WidgetColors) {
    val bg = Brush.verticalGradient(
        listOf(Color(colors.background), Color(colors.backgroundGradientEnd))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Qon  ·  問題 1",
                    color = Color(colors.accent),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "更新 12:00",
                    color = Color(colors.secondaryText),
                    fontSize = 10.sp
                )
            }
            Text(
                text = "「アーキテクチャ」の意味として最も適切なものは？",
                color = Color(colors.text),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                modifier = Modifier.weight(1f, fill = false)
            )
            listOf("建物の構造や設計思想", "ソフトのバージョン", "通信の規格名", "データの単位").forEachIndexed { index, option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(colors.optionBackground))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 14.dp)
                            .background(Color(colors.accent))
                    )
                    Text(
                        text = "${('A' + index)}. $option",
                        color = Color(colors.text),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 9.dp)
                    )
                }
            }
            Text(
                "タップして回答",
                color = Color(colors.secondaryText),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EmptySetsCard(onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("学習セットがありません", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "先にアプリで学習セットを作成してください。\n作成後にウィジェットを追加し直すと選択できます。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp
            )
            TextButton(onClick = onClose) {
                Text("閉じる")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

private val BACKGROUND_SWATCHES = listOf(
    0xFF4F46E5.toInt(),
    0xFF0F172A.toInt(),
    0xFF0E7490.toInt(),
    0xFF166534.toInt(),
    0xFFC2410C.toInt(),
    0xFF9D174D.toInt(),
    0xFF6D28D9.toInt(),
    0xFF1F2937.toInt(),
    0xFFF6F7FB.toInt(),
    0xFFEDE9E3.toInt(),
    0xFF14301F.toInt(),
    0xFF3B1515.toInt()
)

private val ACCENT_SWATCHES = listOf(
    0xFFC7D2FE.toInt(),
    0xFF38BDF8.toInt(),
    0xFF67E8F9.toInt(),
    0xFF86EFAC.toInt(),
    0xFFFDBA74.toInt(),
    0xFFFDA4AF.toInt(),
    0xFFFDE68A.toInt(),
    0xFFA5B4FC.toInt(),
    0xFF4F46E5.toInt(),
    0xFF9A3412.toInt()
)
