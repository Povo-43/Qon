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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ailearning.domain.model.AiApiStyle
import com.example.ailearning.domain.model.AiProvider
import com.example.ailearning.domain.model.AiProviderCatalog
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onFocusLockClick: () -> Unit = {},
    onBack: () -> Unit
) {
    val apiKeys by viewModel.apiKeys.collectAsState()
    val error by viewModel.error.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val testMessage by viewModel.testMessage.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()
    val modelFetchError by viewModel.modelFetchError.collectAsState()

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
                    viewModel.clearModelFetchState()
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
                                "OpenAI・Claude・Gemini・NVIDIA NIM・Kilo など、お好みのプロバイダを登録すると AI 問題生成が使えます。OpenAI 互換の独自エンドポイントも指定できます。",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    editingKey = null
                                    viewModel.clearError()
                                    viewModel.clearModelFetchState()
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
                        testMessage = testMessage[key.id],
                        onToggleEnabled = {
                            scope.launch { viewModel.toggleApiKeyEnabled(key) }
                        },
                        onTest = { viewModel.testApiKey(key.id) },
                        onEdit = {
                            editingKey = key
                            viewModel.clearError()
                            viewModel.clearModelFetchState()
                            showEditDialog = true
                        },
                        onDelete = { deletingKey = key }
                    )
                }
            }

            // 集中モード（v1.6.0）
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onFocusLockClick,
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                "集中モード",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "ロック解除のたびに1問正解するまで、指定アプリを起動できなくする",
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            isFetchingModels = isFetchingModels,
            availableModels = availableModels,
            modelFetchError = modelFetchError,
            onFetchModels = { provider, baseUrl, rawKey ->
                viewModel.fetchModels(
                    provider = provider,
                    baseUrl = baseUrl,
                    rawKey = rawKey,
                    existingKeyId = editingKey?.id,
                    model = editingKey?.model.orEmpty()
                )
            },
            onClearModels = { viewModel.clearModelFetchState() },
            onDismiss = {
                if (!isSaving) {
                    showEditDialog = false
                    editingKey = null
                    viewModel.clearError()
                    viewModel.clearModelFetchState()
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
                        viewModel.clearModelFetchState()
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
    testMessage: String?,
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
                if (!testResult && !testMessage.isNullOrBlank()) {
                    SelectionContainer {
                        Text(
                            text = testMessage,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp)
                        )
                    }
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
    isFetchingModels: Boolean,
    availableModels: List<String>,
    modelFetchError: String?,
    onFetchModels: (provider: String, baseUrl: String, rawKey: String) -> Unit,
    onClearModels: () -> Unit,
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
    val initialProvider = remember(editingKey) {
        AiProviderCatalog.resolve(editingKey?.provider ?: ApiKey.PROVIDER_OPENAI)
    }
    var name by remember(editingKey) { mutableStateOf(editingKey?.name.orEmpty()) }
    var provider by remember(editingKey) { mutableStateOf(initialProvider.id) }
    var model by remember(editingKey) { mutableStateOf(editingKey?.model.orEmpty()) }
    var rawKey by remember(editingKey) { mutableStateOf("") }
    var baseUrl by remember(editingKey) {
        mutableStateOf(editingKey?.baseUrl ?: initialProvider.baseUrl)
    }
    var priority by remember(editingKey) { mutableStateOf((editingKey?.priority ?: 1).toString()) }
    var usageType by remember(editingKey) { mutableStateOf(editingKey?.usageType ?: ApiKey.USAGE_GENERATION) }

    val providerInfo = AiProviderCatalog.resolve(provider)
    val isNew = editingKey == null
    // ローカル推論（Ollama / LM Studio）は API キー不要
    val keyRequired = isNew && !providerInfo.isLocal
    val canSave = name.isNotBlank() && model.isNotBlank() &&
        (!providerInfo.requiresBaseUrl || baseUrl.isNotBlank()) &&
        (!keyRequired || rawKey.isNotBlank())

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
                var providerMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = providerMenuExpanded,
                    onExpandedChange = { providerMenuExpanded = !providerMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = providerInfo.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("選択中") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = providerMenuExpanded,
                        onDismissRequest = { providerMenuExpanded = false }
                    ) {
                        AiProviderCatalog.providers.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = option.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (option.description.isNotBlank()) {
                                            Text(
                                                text = option.description,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    val previous = AiProviderCatalog.resolve(provider)
                                    if (baseUrl.isBlank() || baseUrl == previous.baseUrl) {
                                        baseUrl = option.baseUrl
                                    }
                                    if (model.isBlank() || model == previous.exampleModel) {
                                        model = option.exampleModel
                                    }
                                    provider = option.id
                                    onClearModels()
                                    providerMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lan,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = buildString {
                            append(styleLabel(providerInfo.apiStyle))
                            if (providerInfo.isLocal) append(" ・ ローカル")
                            if (providerInfo.isLocal) {
                                append(" ・ PC のサーバーを起動してください")
                            }
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("モデル名") },
                    placeholder = {
                        Text(
                            if (providerInfo.exampleModel.isNotBlank()) {
                                "例: ${providerInfo.exampleModel}"
                            } else {
                                "例: モデル名"
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onFetchModels(provider, baseUrl, rawKey) },
                        enabled = !isFetchingModels &&
                            (!keyRequired || rawKey.isNotBlank()) &&
                            (!providerInfo.requiresBaseUrl || baseUrl.isNotBlank()),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isFetchingModels) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text("取得中…", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text("モデル一覧取得", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (availableModels.isNotEmpty()) {
                    var modelMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = modelMenuExpanded,
                        onExpandedChange = { modelMenuExpanded = !modelMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("取得したモデル（${availableModels.size}件）") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            availableModels.forEach { id ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = id,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        model = id
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (modelFetchError != null) {
                    Text(
                        text = modelFetchError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                OutlinedTextField(
                    value = rawKey,
                    onValueChange = { rawKey = it },
                    label = {
                        Text(
                            when {
                                !isNew -> "APIキー（変更する場合のみ）"
                                providerInfo.isLocal -> "APIキー（任意）"
                                else -> "APIキー"
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            if (!isNew) "空欄なら現在のキーを使用"
                            else if (providerInfo.isLocal) "空欄でOK"
                            else providerInfo.keyHint
                        )
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = {
                        Text(
                            if (providerInfo.requiresBaseUrl) "ベースURL（必須）" else "ベースURL"
                        )
                    },
                    placeholder = { Text("https://example.com/v1/") },
                    supportingText = {
                        Text(
                            text = if (providerInfo.isLocal) {
                                "エミュレータから PC へは 10.0.2.2、実機は PC の IP を指定"
                            } else {
                                "…/chat/completions まで貼り付けても自動で補正します"
                            },
                            fontSize = 10.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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

private fun shortProviderLabel(provider: String): String =
    AiProviderCatalog.shortLabel(provider)

private fun providerLabel(provider: String): String =
    AiProviderCatalog.label(provider)

private fun providerColor(provider: String): Color =
    Color(AiProviderCatalog.brandColor(provider))

private fun usageLabel(usageType: String): String = when (usageType) {
    ApiKey.USAGE_GENERATION -> "問題生成"
    ApiKey.USAGE_ANALYSIS -> "分析"
    ApiKey.USAGE_BOTH -> "問題生成 + 分析"
    else -> usageType
}

private fun styleLabel(style: AiApiStyle): String = when (style) {
    AiApiStyle.OPENAI -> "OpenAI 互換 API"
    AiApiStyle.ANTHROPIC -> "Anthropic Messages API"
    AiApiStyle.GEMINI -> "Google Gemini API"
}
