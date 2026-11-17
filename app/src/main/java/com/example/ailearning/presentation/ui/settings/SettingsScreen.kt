package com.example.ailearning.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "設定",
                subtitle = "AIプロバイダの接続",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingKey = null
                    viewModel.clearError()
                    showEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("APIキー", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "端末内に安全に保存",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "APIキーはAndroid Keystoreで暗号化され、この端末の外には送信されません。",
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (apiKeys.isEmpty()) {
                item {
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
                                    imageVector = Icons.Rounded.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text("APIキー未登録", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "OpenAI・Claude・GeminiのいずれかのAPIキーを追加すると、AI問題生成が使えます。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    editingKey = null
                                    viewModel.clearError()
                                    showEditDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("APIキーを追加", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "登録済みのキー",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
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
                        fontSize = 12.sp
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
            shape = RoundedCornerShape(24.dp),
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
    val brand = providerColor(apiKey.provider)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(brand.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortProviderLabel(apiKey.provider).take(1),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = brand
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = apiKey.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${providerLabel(apiKey.provider)}  ·  ${apiKey.model}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(checked = apiKey.isEnabled, onCheckedChange = { onToggleEnabled() })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag(text = usageLabel(apiKey.usageType))
                Tag(text = "優先度 ${apiKey.priority}")
                if (!apiKey.isEnabled) {
                    Tag(text = "無効", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (isTesting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text("接続テスト中…", fontSize = 12.sp)
                }
            } else if (testResult != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (testResult) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                        contentDescription = null,
                        tint = if (testResult) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (testResult) "接続テスト成功" else "接続テスト失敗",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (testResult) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onTest, enabled = !isTesting) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("テスト", fontSize = 12.sp)
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("編集", fontSize = 12.sp)
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("削除", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = CircleShape
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
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
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                if (isNew) "APIキーを追加" else "APIキーを編集",
                fontWeight = FontWeight.Bold
            )
        },
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("プロバイダ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                            label = { Text(shortProviderLabel(p), fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("モデル名") },
                    placeholder = { Text(modelHint(provider)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rawKey,
                    onValueChange = { rawKey = it },
                    label = { Text(if (isNew) "APIキー" else "APIキー（変更する場合のみ）") },
                    placeholder = { Text(if (isNew) "sk-..." else "空欄なら現在のキーを使用") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("ベースURL（任意）") },
                    placeholder = { Text("カスタムAPIのURL") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priority,
                    onValueChange = { input -> priority = input.filter { it.isDigit() }.take(2) },
                    label = { Text("優先度（数字が大きいほど優先）") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("用途", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                            label = { Text(label, fontSize = 11.sp) }
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
                },
                shape = RoundedCornerShape(14.dp)
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
    ApiKey.PROVIDER_ANTHROPIC -> "Anthropic"
    ApiKey.PROVIDER_GOOGLE -> "Google"
    ApiKey.PROVIDER_CUSTOM -> "Custom"
    else -> provider
}

private fun providerColor(provider: String): Color = when (provider) {
    ApiKey.PROVIDER_OPENAI -> Color(0xFF10A37F)
    ApiKey.PROVIDER_ANTHROPIC -> Color(0xFFD97757)
    ApiKey.PROVIDER_GOOGLE -> Color(0xFF4285F4)
    else -> Color(0xFF6366F1)
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
