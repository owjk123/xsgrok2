@file:OptIn(ExperimentalLayoutApi::class)
package com.xsgrok2.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xsgrok2.app.ui.viewmodel.CreateStep
import com.xsgrok2.app.ui.viewmodel.CreateNovelUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNovelScreen(
    uiState: CreateNovelUiState,
    onBack: () -> Unit,
    onUpdateGenre: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateWritingStyle: (String) -> Unit,
    onUpdateGeneratedSettings: (String) -> Unit,
    onGenerate: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val writingStyles = listOf("细腻生动", "简洁有力", "幽默风趣", "冷峻写实", "诗意唯美")
    val genreSuggestions = listOf("玄幻", "言情", "都市", "悬疑", "科幻", "日常", "历史", "古风", "恐怖", "冒险")

    val createdNovelId = uiState.createdNovelId
    LaunchedEffect(createdNovelId) {
        if (createdNovelId != null) {
            onNavigateToDetail(createdNovelId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (uiState.step) {
                        CreateStep.INPUT -> "新建小说"
                        CreateStep.GENERATING -> "生成设定中..."
                        CreateStep.REVIEW -> "审阅设定"
                        CreateStep.SAVING -> "保存中..."
                    })
                },
                navigationIcon = {
                    if (uiState.step == CreateStep.INPUT || uiState.step == CreateStep.REVIEW) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (uiState.step) {
            CreateStep.INPUT -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("小说类型", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        genreSuggestions.forEach { g ->
                            FilterChip(
                                selected = uiState.genre == g,
                                onClick = { onUpdateGenre(g) },
                                label = { Text(g) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = if (uiState.genre in genreSuggestions) "" else uiState.genre,
                        onValueChange = { onUpdateGenre(it) },
                        label = { Text("自定义类型") },
                        placeholder = { Text("如：末世生存、校园灵异...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = onUpdateDescription,
                        label = { Text("核心构思") },
                        placeholder = { Text("描述你想写的故事核心，越具体越好...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )

                    Text("写作风格", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        writingStyles.forEach { style ->
                            FilterChip(
                                selected = uiState.writingStyle == style,
                                onClick = { onUpdateWritingStyle(style) },
                                label = { Text(style) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onGenerate,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.genre.isNotBlank() && uiState.description.isNotBlank()
                    ) {
                        Text("生成小说设定")
                    }

                    uiState.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            CreateStep.GENERATING -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI正在构思小说设定...", style = MaterialTheme.typography.bodyLarge)
                        Text("这可能需要几十秒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            CreateStep.REVIEW -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("AI生成的小说设定", style = MaterialTheme.typography.titleMedium)
                    Text("你可以直接编辑下面的内容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = uiState.generatedSettings,
                        onValueChange = onUpdateGeneratedSettings,
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        label = { Text("小说设定（可编辑）") }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onGenerate,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重新生成")
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("确认并创建")
                        }
                    }

                    uiState.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            CreateStep.SAVING -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
