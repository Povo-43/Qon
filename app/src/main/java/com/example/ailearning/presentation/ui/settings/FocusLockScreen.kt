package com.example.ailearning.presentation.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.viewmodel.FocusLockViewModel
import com.example.ailearning.presentation.viewmodel.InstalledAppEntry

/**
 * 集中モードの設定画面（v1.6.0）。
 * ロック解除のたびに1問正解するまで、指定したアプリを起動できなくする。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusLockScreen(
    viewModel: FocusLockViewModel,
    onBack: () -> Unit
) {
    val focusEnabled by viewModel.focusEnabled.collectAsState()
    val learningSetId by viewModel.learningSetId.collectAsState()
    val learningSets by viewModel.learningSets.collectAsState()
    val requiredCorrect by viewModel.requiredCorrect.collectAsState()
    val unlockDurationMinutes by viewModel.unlockDurationMinutes.collectAsState()
    val showCorrectAnswer by viewModel.showCorrectAnswer.collectAsState()
    val blockedApps by viewModel.blockedApps.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val unlockCount by viewModel.unlockCount.collectAsState()
    val wrongCount by viewModel.wrongCount.collectAsState()

    var showAppPicker by remember { mutableStateOf(false) }
    var setMenuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "集中モード",
                subtitle = "ロック解除のたびに1問正解",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // ------------------------------------------------ 有効化
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("集中モードを有効にする", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "画面ロックを解除するたびに、対象アプリは問題に正解するまで起動できません",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(checked = focusEnabled, onCheckedChange = viewModel::setEnabled)
                    }
                }
            }

            if (focusEnabled && !isServiceRunning) {
                item {
                    // ------------------------------------------------ 権限ガイド
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "アクセシビリティの許可が必要です",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Qon が前面アプリを検知するために「画面の内容の取得」権限を使います。\n画面の中身は読み取らず、アプリのパッケージ名のみを使用します。",
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                            Button(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(Icons.Rounded.Accessibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("アクセシビリティ設定を開く", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                // ------------------------------------------------ 問題セット
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("ゲートの問題元", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        ExposedDropdownMenuBox(
                            expanded = setMenuOpen,
                            onExpandedChange = { setMenuOpen = it }
                        ) {
                            val selectedLabel = if (learningSetId.isBlank()) {
                                "自動（問題があるセット）"
                            } else {
                                learningSets.firstOrNull { it.id == learningSetId }?.name ?: "自動（問題があるセット）"
                            }
                            OutlinedTextField(
                                value = selectedLabel,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = setMenuOpen) },
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = setMenuOpen,
                                onDismissRequest = { setMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("自動（問題があるセット）") },
                                    onClick = {
                                        viewModel.selectLearningSet("")
                                        setMenuOpen = false
                                    }
                                )
                                learningSets.forEach { set ->
                                    DropdownMenuItem(
                                        text = { Text(set.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        onClick = {
                                            viewModel.selectLearningSet(set.id)
                                            setMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            "選んだセットの問題が少ないと、裏で自動生成して待ち時間ゼロで出題できるようにします",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                // ------------------------------------------------ 解除条件
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("解除条件", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "必要な正解数",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..5).forEach { count ->
                                    FilterChip(
                                        selected = requiredCorrect == count,
                                        onClick = { viewModel.setRequiredCorrect(count) },
                                        label = { Text("${count}問", fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "正解後の有効期間",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0 to "次のロックまで", 10 to "10分", 30 to "30分", 60 to "1時間").forEach { (minutes, label) ->
                                    FilterChip(
                                        selected = unlockDurationMinutes == minutes,
                                        onClick = { viewModel.setUnlockDurationMinutes(minutes) },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                            Text(
                                if (unlockDurationMinutes == 0) {
                                    "画面ロックを解除するたびに再度問題が出ます"
                                } else {
                                    "正解してから ${unlockDurationMinutes} 分間は再度問われません"
                                },
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("誤答時に正解を表示", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "オフにすると正解が分からず、別の問題へ進みます",
                                    fontSize = 10.5.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = showCorrectAnswer, onCheckedChange = viewModel::setShowCorrectAnswer)
                        }
                    }
                }
            }

            item {
                // ------------------------------------------------ 対象アプリ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ロックするアプリ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${blockedApps.size} 個のアプリ",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.loadInstalledApps(context)
                                    showAppPicker = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("追加", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        if (blockedApps.isEmpty()) {
                            Text(
                                "まだアプリが追加されていません。\n追加したアプリがロック解除のたびに問題ゲートの対象になります。",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            blockedApps.forEach { app ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            app.label.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            app.label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (app.enabled) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            app.packageName,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        if (app.enabled) "有効" else "一時解除",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Switch(
                                        checked = app.enabled,
                                        onCheckedChange = { viewModel.setBlockedAppEnabled(app.packageName, it) }
                                    )
                                    IconButton(onClick = { viewModel.removeBlockedApp(app.packageName) }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "削除",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                // ------------------------------------------------ テスト
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("ゲートをテスト", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "実際のロック画面をシミュレーションします（記録されません）",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 16.sp
                            )
                        }
                        Button(
                            onClick = { viewModel.showTestGate() },
                            enabled = isServiceRunning,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("実行", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                // ------------------------------------------------ 統計 + 注意
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("これまでの記録", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("解除 ${unlockCount} 回", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("誤答 ${wrongCount} 回", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.size(2.dp))
                        Text(
                            "・Qon のアクセシビリティをオフにすると集中モードは無効化されます\n"
                                + "・設定・電話・Qon 自身はロック対象にできません\n"
                                + "・問題を用意できないときは、端末を使えなくならないよう自動的に解除されます\n"
                                + "・バッテリー最適化が強い端末（MIUI など）では「自動起動」の許可を推奨します",
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            entries = installedApps,
            addedPackages = blockedApps.map { it.packageName }.toSet(),
            onAdd = viewModel::addBlockedApp,
            onDismiss = { showAppPicker = false }
        )
    }
}

@Composable
private fun AppPickerDialog(
    entries: List<InstalledAppEntry>?,
    addedPackages: Set<String>,
    onAdd: (InstalledAppEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
        title = { Text("アプリを追加", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("アプリを検索") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                when {
                    entries == null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        }
                    }
                    entries.isEmpty() -> {
                        Text(
                            "アプリが見つかりません",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                    else -> {
                        val filtered = entries.filter {
                            it.label.contains(query, ignoreCase = true) ||
                                it.packageName.contains(query, ignoreCase = true)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filtered, key = { it.packageName }) { entry ->
                                val added = entry.packageName in addedPackages
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (added) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    entry.icon?.let {
                                        Image(
                                            bitmap = it,
                                            contentDescription = null,
                                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                                        )
                                    } ?: Box(
                                        modifier = Modifier.size(34.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            entry.label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            entry.packageName,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(
                                        onClick = { onAdd(entry) },
                                        enabled = !added
                                    ) {
                                        Text(if (added) "追加済み" else "追加")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
