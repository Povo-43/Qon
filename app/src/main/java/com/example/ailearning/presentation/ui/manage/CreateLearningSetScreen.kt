package com.example.ailearning.presentation.ui.manage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ailearning.presentation.ui.components.CustomTopAppBar
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.viewmodel.LearningSetViewModel
import com.example.ailearning.R

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

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTopAppBar(
            title = "学習セット作成",
            navigationIcon = null,
            onNavigationClick = onCancel
        )

        if (isLoading) {
            LoadingOverlay(isVisible = true)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "学習セット名",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("学習セット名") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "科目",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("科目名") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "カテゴリ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("カテゴリ名") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "単元",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text("単元名") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "範囲",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = scope,
                onValueChange = { scope = it },
                label = { Text("学習範囲") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.createLearningSet(name, subject, category, unit, scope)
                    onSave()
                },
                enabled = name.isNotBlank() && subject.isNotBlank() && category.isNotBlank() && unit.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("作成")
            }
        }

        error?.let { msg ->
            Text(text = msg, color = Color.Red)
        }
    }
}
