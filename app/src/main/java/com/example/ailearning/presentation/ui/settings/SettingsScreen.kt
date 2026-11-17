package com.example.ailearning.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.presentation.ui.components.CustomTopAppBar
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.viewmodel.SettingsViewModel
import com.example.ailearning.R
import kotlin.Result
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val apiKeys by viewModel.apiKeys.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    val scope = rememberCoroutineScope()

    // APIキー追加・編集ダイアログの状態
    var showAddDialog by remember { mutableStateOf(false) }
    var editKey by remember { mutableStateOf<ApiKey?>(null) }
    var dialogName by remember { mutableStateOf("") }
    var dialogProvider by remember { mutableStateOf("") }
    var dialogModel by remember { mutableStateOf("") }
    var dialogApiKey by remember { mutableStateOf("") }
    var dialogBaseUrl by remember { mutableStateOf("") }
    var dialogPriority by remember { mutableStateOf(1) }
    var dialogUsageType by remember { mutableStateOf("generation") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTopAppBar(
            title = "APIキー設定",
            navigationIcon = null,
            onNavigationClick = onBack
        )

        if (isLoading) {
            LoadingOverlay(isVisible = true)
        }

        if (apiKeys.isEmpty()) {
            Text(
                text = "APIキーが登録されていません",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 48.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apiKeys) { key ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = key.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (key.isEnabled) {
                                    Text(
                                        text = "有効",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "無効",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "${key.provider} / ${key.model}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        editKey = key
                                        dialogName = key.name
                                        dialogProvider = key.provider
                                        dialogModel = key.model
                                        dialogApiKey = key.encryptedKey
                                        dialogBaseUrl = key.baseUrl.orEmpty()
                                        dialogPriority = key.priority
                                        dialogUsageType = key.usageType
                                        showAddDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Text("編集")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.testApiKey(key.id)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                ) {
                                    Text("テスト")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.deleteApiKey(key.id)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text("削除")
                                }
                            }
                        }
                    }
                }
            }
        }

        error?.let { msg ->
            Text(text = msg, color = Color.Red, modifier = Modifier.padding(horizontal = 16.dp))
        }

        testResult.entries.forEach { (keyId, success) ->
            if (success) {
                Text(
                    text = "接続テスト成功",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else if (testResult.containsKey(keyId)) {
                Text(
                    text = "接続テスト失敗",
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // APIキー追加・編集ダイアログ
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editKey = null
                dialogName = ""
                dialogProvider = ""
                dialogModel = ""
                dialogApiKey = ""
                dialogBaseUrl = ""
                dialogPriority = 1
                dialogUsageType = "generation"
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = if (editKey == null) {
                                viewModel.addApiKey(
                                    name = dialogName,
                                    provider = dialogProvider,
                                    model = dialogModel,
                                    apiKey = dialogApiKey,
                                    baseUrl = dialogBaseUrl.takeIf { it.isNotEmpty() },
                                    priority = dialogPriority,
                                    usageType = dialogUsageType
                                )
                            } else {
                                val ek = editKey!!
                                val updatedKey = ek.copy(
                                    name = dialogName,
                                    provider = dialogProvider,
                                    model = dialogModel,
                                    encryptedKey = dialogApiKey,
                                    baseUrl = dialogBaseUrl.takeIf { it.isNotEmpty() },
                                    priority = dialogPriority,
                                    usageType = dialogUsageType
                                )
                                viewModel.updateApiKey(updatedKey)
                            }
                            if (result.isSuccess) {
                                showAddDialog = false
                                editKey = null
                                dialogName = ""
                                dialogProvider = ""
                                dialogModel = ""
                                dialogApiKey = ""
                                dialogBaseUrl = ""
                                dialogPriority = 1
                                dialogUsageType = "generation"
                            }
                        }
                    },
                    enabled = dialogName.isNotBlank() && dialogProvider.isNotBlank() && dialogModel.isNotBlank() && dialogApiKey.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (editKey == null) "追加" else "更新")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showAddDialog = false
                        editKey = null
                        dialogName = ""
                        dialogProvider = ""
                        dialogModel = ""
                        dialogApiKey = ""
                        dialogBaseUrl = ""
                        dialogPriority = 1
                        dialogUsageType = "generation"
                    }
                ) {
                    Text("キャンセル")
                }
            },
            title = {
                Text(if (editKey == null) "APIキー追加" else "APIキー編集")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dialogName,
                        onValueChange = { dialogName = it },
                        label = { Text("名前") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogProvider,
                        onValueChange = { dialogProvider = it },
                        label = { Text("プロバイダ (openai / anthropic / google)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogModel,
                        onValueChange = { dialogModel = it },
                        label = { Text("モデル名") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogApiKey,
                        onValueChange = { dialogApiKey = it },
                        label = { Text("APIキー") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogBaseUrl,
                        onValueChange = { dialogBaseUrl = it },
                        label = { Text("ベースURL (省略可)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogPriority.toString(),
                        onValueChange = { dialogPriority = it.toIntOrNull() ?: 1 },
                        label = { Text("優先度 (数字)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogUsageType,
                        onValueChange = { dialogUsageType = it },
                        label = { Text("用途タイプ (generation / analysis)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = {
                editKey = null
                dialogName = ""
                dialogProvider = ""
                dialogModel = ""
                dialogApiKey = ""
                dialogBaseUrl = ""
                dialogPriority = 1
                dialogUsageType = "generation"
                showAddDialog = true
            }
        ) {
            Text("＋", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
