package com.example.ailearning.presentation.ui.manage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel

@Composable
fun CreateLearningSetScreen(
    viewModel: LearningSetViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val error by viewModel.error.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "学習セット作成",
                onBack = onCancel
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "学習したい範囲を登録してください。AIがこの情報をもとに問題を生成します。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FormField(
                label = "学習セット名",
                value = name,
                onValueChange = { name = it },
                placeholder = "例: 高校数学 微分積分"
            )
            FormField(
                label = "科目",
                value = subject,
                onValueChange = { subject = it },
                placeholder = "例: 数学"
            )
            FormField(
                label = "カテゴリ",
                value = category,
                onValueChange = { category = it },
                placeholder = "例: 微分法"
            )
            FormField(
                label = "単元",
                value = unit,
                onValueChange = { unit = it },
                placeholder = "例: 接線と導関数"
            )
            FormField(
                label = "範囲（任意）",
                value = scope,
                onValueChange = { scope = it },
                placeholder = "例: 教科書 p.40〜p.58"
            )

            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    coroutineScope.launch {
                        val result = viewModel.createLearningSet(name, subject, category, unit, scope)
                        isSaving = false
                        if (result.isSuccess) {
                            onSave()
                        }
                    }
                },
                enabled = !isSaving &&
                    name.isNotBlank() && subject.isNotBlank() &&
                    category.isNotBlank() && unit.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("作成", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            error?.let { msg ->
                Text(text = msg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
    }
}
