package com.example.ailearning.presentation.ui.manage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.presentation.ui.components.AppTopBar
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel

@Composable
fun CreateLearningSetScreen(
    viewModel: LearningSetViewModel,
    learningSet: LearningSet? = null,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val isEdit = learningSet != null
    val initialName = if (isEdit) learningSet!!.name else ""
    val initialSubject = if (isEdit) learningSet!!.subject else ""
    val initialCategory = if (isEdit) learningSet!!.category else ""
    val initialUnit = if (isEdit) learningSet!!.unit else ""
    val initialScope = if (isEdit) learningSet!!.scope else ""
    var name by remember { mutableStateOf(initialName) }
    var subject by remember { mutableStateOf(initialSubject) }
    var category by remember { mutableStateOf(initialCategory) }
    var unit by remember { mutableStateOf(initialUnit) }
    var scope by remember { mutableStateOf(initialScope) }
    var isSaving by remember { mutableStateOf(false) }

    val error by viewModel.error.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val canSave = !isSaving && name.isNotBlank() && subject.isNotBlank() &&
        category.isNotBlank() && unit.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = if (isEdit) "学習セットを編集" else "新しい学習セット",
                onBack = onCancel
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp
            ) {
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        coroutineScope.launch {
                            if (isEdit) {
                                val set = learningSet ?: return@launch
                                val updated = set.copy(
                                    name = name,
                                    subject = subject,
                                    category = category,
                                    unit = unit,
                                    scope = scope
                                )
                                viewModel.updateLearningSet(updated)
                            } else {
                                val result = viewModel.createLearningSet(name, subject, category, unit, scope)
                                if (result.isSuccess) onSave()
                            }
                            isSaving = false
                            if (!isEdit) onSave()
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            if (isEdit) "内容を更新" else "学習セットを作成",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "AIが問題をつくります",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "学習したい範囲を入力してください。科目・単元の情報をもとに出題します。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Text(text = "基本情報", fontSize = 15.sp, fontWeight = FontWeight.Bold)

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

            error?.let { msg ->
                Text(text = msg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )
}
