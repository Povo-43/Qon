package com.example.ailearning.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val apiKeys by viewModel.apiKeys.collectAsState()
    val error by viewModel.error.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<ApiKey?>(null) }
    var deletingKey by remember { mutableStateOf<ApiKey?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "APIキー設定",
                subtitle = "AIプロバイダの接続設定",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingKey = null
                    viewModel.clearError()
                    showEditDialog = true
                }
            ) {
                Text("＋ APIキー追加", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🔐 端末内に安全に保存", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "APIキーはAndroid Keystoreで暗号化され、この端末の外には送信されません。",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (apiKeys.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🔑", fontSize = 36.sp)
                            Text("APIキー未登録", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "AI問題生成を使うには、OpenAI・Claude・GeminiのいずれかのAPIキーを追加してください。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    editingKey = null
                                    viewModel.clearError()
                                    showEditDialog = true
                                },
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Text("APIキーを追加")
                            }
                        }
                    }
                }
            } else {
                items(apiKeys, key = { it.id }) { key ->
                    ApiKeyCard(
                        apiKey = key,
                        isTesting = isTesting.contains(key.id),
                        testResult = testResult[key.id],
                        onToggleEnabled = {
                            scope.launch { viewModel.toggleApiKeyEnabled(key) }
                        },
                        onTest = { viewModel.testApiKey(key.id) },
                        onEdit = {
                            editingKey = key
                            viewModel.clearError()
                            showEditDialog = true
                        },
                        onDelete = { deletingKey = key }
                    )
                }
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
    }

    if (showEditDialog) {
        ApiKeyEditorDialog(
            editingKey = editingKey,
            isSaving = isSaving,
            error = error,
            onDismiss = {
                if (!isSaving) {
                    showEditDialog = false
                    editingKey = null
                    viewModel.clearError()
                }
            },
            onSave = { name, provider, model, rawKey, baseUrl, priority, usageType ->
                if (isSaving) return@ApiKeyEditorDialog
                isSaving = true
                scope.launch {
                    val current = editingKey
                    val result = if (current == null) {
                        viewModel.addApiKey(name, provider, model, rawKey, baseUrl, priority, usageType)
                    } else {
                        viewModel.updateApiKey(
                            current.copy(
                                name = name,
                                provider = provider,
                                model = model,
                                baseUrl = baseUrl,
                                priority = priority,
                                usageType = usageType
                            ),
                            rawKey.takeIf { it.isNotBlank() }
                        )
                    }
                    isSaving = false
                    if (result.isSuccess) {
                        showEditDialog = false
                        editingKey = null
                    }
                }
            }
        )
    }

    deletingKey?.let { key ->
        AlertDialog(
            onDismissRequest = { deletingKey = null },
            title = { Text("APIキーを削除") },
            text = { Text("「${key.name}」を削除します。よろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingKey = null
                        scope.launch { viewModel.deleteApiKey(key.id) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingKey = null }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun ApiKeyCard(
    apiKey: ApiKey,
    isTesting: Boolean,
    testResult: Boolean?,
    onToggleEnabled: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = apiKey.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = providerLabel(apiKey.provider),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = apiKey.isEnabled, onCheckedChange = { onToggleEnabled() })
            }

            Text(
                text = "モデル: ${apiKey.model}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "用途: ${usageLabel(apiKey.usageType)}   優先度: ${apiKey.priority}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isTesting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text("接続テスト中…", fontSize = 12.sp)
                }
            } else if (testResult != null) {
                Text(
                    text = if (testResult) "✓ 接続テスト成功" else "✗ 接続テスト失敗",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (testResult) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onTest, enabled = !isTesting, modifier = Modifier.weight(1f)) {
                    Text("テスト", fontSize = 13.sp)
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("編集", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ApiKeyEditorDialog(
    editingKey: ApiKey?,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        provider: String,
        model: String,
        rawKey: String,
        baseUrl: String?,
        priority: Int,
        usageType: String
    ) -> Unit
) {
    var name by remember(editingKey) { mutableStateOf(editingKey?.name.orEmpty()) }
    var provider by remember(editingKey) { mutableStateOf(editingKey?.provider ?: ApiKey.PROVIDER_OPENAI) }
    var model by remember(editingKey) { mutableStateOf(editingKey?.model.orEmpty()) }
    var rawKey by remember(editingKey) { mutableStateOf("") }
    var baseUrl by remember(editingKey) { mutableStateOf(editingKey?.baseUrl.orEmpty()) }
    var priority by remember(editingKey) { mutableStateOf((editingKey?.priority ?: 1).toString()) }
    var usageType by remember(editingKey) { mutableStateOf(editingKey?.usageType ?: ApiKey.USAGE_GENERATION) }

    val isNew = editingKey == null
    val canSave = name.isNotBlank() && provider.isNotBlank() && model.isNotBlank() &&
        (!isNew || rawKey.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "APIキーを追加" else "APIキーを編集") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("表示名") },
                    placeholder = { Text("例: 個人用OpenAI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("プロバイダ", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ApiKey.PROVIDER_OPENAI,
                        ApiKey.PROVIDER_ANTHROPIC,
                        ApiKey.PROVIDER_GOOGLE
                    ).forEach { p ->
                        FilterChip(
                            selected = provider == p,
                            onClick = { provider = p },
                            label = { Text(shortProviderLabel(p), fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("モデル名") },
                    placeholder = { Text(modelHint(provider)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rawKey,
                    onValueChange = { rawKey = it },
                    label = { Text(if (isNew) "APIキー" else "APIキー（変更する場合のみ）") },
                    placeholder = { Text(if (isNew) "sk-..." else "空欄なら現在のキーを使用") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("ベースURL（任意）") },
                    placeholder = { Text("カスタムAPIのURL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = { input -> priority = input.filter { it.isDigit() }.take(2) },
                        label = { Text("優先度") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "数字が大きいほど優先",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.4f)
                    )
                }

                Text("用途", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ApiKey.USAGE_GENERATION to "問題生成",
                        ApiKey.USAGE_ANALYSIS to "分析",
                        ApiKey.USAGE_BOTH to "両方"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = usageType == value,
                            onClick = { usageType = value },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave && !isSaving,
                onClick = {
                    onSave(
                        name.trim(),
                        provider,
                        model.trim(),
                        rawKey.trim(),
                        baseUrl.trim().ifBlank { null },
                        priority.toIntOrNull() ?: 1,
                        usageType
                    )
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isNew) "追加" else "保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("キャンセル")
            }
        }
    )
}

private fun shortProviderLabel(provider: String): String = when (provider) {
    ApiKey.PROVIDER_OPENAI -> "OpenAI"
    ApiKey.PROVIDER_ANTHROPIC -> "Claude"
    ApiKey.PROVIDER_GOOGLE -> "Gemini"
    ApiKey.PROVIDER_CUSTOM -> "カスタム"
    else -> provider
}

private fun providerLabel(provider: String): String = when (provider) {
    ApiKey.PROVIDER_OPENAI -> "OpenAI"
    ApiKey.PROVIDER_ANTHROPIC -> "Anthropic Claude"
    ApiKey.PROVIDER_GOOGLE -> "Google Gemini"
    ApiKey.PROVIDER_CUSTOM -> "カスタムAPI"
    else -> provider
}

private fun usageLabel(usageType: String): String = when (usageType) {
    ApiKey.USAGE_GENERATION -> "問題生成"
    ApiKey.USAGE_ANALYSIS -> "分析"
    ApiKey.USAGE_BOTH -> "問題生成 + 分析"
    else -> usageType
}

private fun modelHint(provider: String): String = when (provider) {
    ApiKey.PROVIDER_OPENAI -> "例: gpt-4o-mini"
    ApiKey.PROVIDER_ANTHROPIC -> "例: claude-3-5-sonnet-20241022"
    ApiKey.PROVIDER_GOOGLE -> "例: gemini-1.5-flash"
    else -> "例: モデル名"
}
