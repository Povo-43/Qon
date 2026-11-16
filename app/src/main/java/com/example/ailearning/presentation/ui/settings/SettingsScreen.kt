package com.example.ailearning.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.foundation.lazy.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.presentation.ui.components.CustomTopAppBar
import com.example.ailearning.presentation.ui.components.LoadingOverlay
import com.example.ailearning.presentation.viewmodel.SettingsViewModel
import com.example.ailearning.R
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val apiKeys by viewModel.apiKeys.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

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
            Text(text = "APIキーが登録されていません", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = key.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = key.provider, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = key.model, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        error?.let { msg ->
            Text(text = msg, color = Color.Red)
        }
    }
}
