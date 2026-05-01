package com.xsgrok2.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xsgrok2.app.ui.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onUpdateModel: (String) -> Unit,
    onUpdateApiBaseUrl: (String) -> Unit,
    onUpdateWritingStyle: (String) -> Unit,
    onUpdateFontSize: (Int) -> Unit,
    onUpdateNightMode: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    var apiKeyInput by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
    var baseUrlInput by remember(uiState.apiBaseUrl) { mutableStateOf(uiState.apiBaseUrl) }

    val models = listOf(
        "grok-4.20-beta" to "Grok 4.20 基础版",
        "grok-4.20-beta-0309-reasoning" to "Grok 4.20 推理版"
    )
    val writingStyles = listOf("细腻生动", "简洁有力", "幽默风趣", "冷峻写实", "诗意唯美")
    val fontSizes = listOf(14 to "小", 16 to "中", 18 to "大", 20 to "特大")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API设置
            Text("API 配置", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    onUpdateApiKey(it)
                },
                label = { Text("API 密钥") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = {
                    baseUrlInput = it
                    onUpdateApiBaseUrl(it)
                },
                label = { Text("API 地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("模型选择", style = MaterialTheme.typography.bodyMedium)
            models.forEach { (value, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = uiState.model == value,
                        onClick = { onUpdateModel(value) }
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }

            HorizontalDivider()

            // 写作风格
            Text("默认写作风格", style = MaterialTheme.typography.titleMedium)
            writingStyles.forEach { style ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = uiState.writingStyle == style,
                        onClick = { onUpdateWritingStyle(style) }
                    )
                    Text(style, modifier = Modifier.padding(start = 8.dp))
                }
            }

            HorizontalDivider()

            // 阅读设置
            Text("阅读设置", style = MaterialTheme.typography.titleMedium)

            Text("字体大小", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fontSizes.forEach { (size, label) ->
                    FilterChip(
                        selected = uiState.fontSize == size,
                        onClick = { onUpdateFontSize(size) },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("夜间模式", modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.nightMode,
                    onCheckedChange = onUpdateNightMode
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存设置")
            }

            if (uiState.isSaved) {
                Text(
                    "设置已保存 ✓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
