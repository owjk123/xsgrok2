package com.xsgrok2.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.Novel
import com.xsgrok2.app.ui.viewmodel.NovelDetailUiState
import com.xsgrok2.app.utils.ExportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    uiState: NovelDetailUiState,
    onBack: () -> Unit,
    onGenerateNextChapter: () -> Unit,
    onNavigateToChapter: (Long) -> Unit,
    onDeleteNovel: () -> Unit
) {
    val novel = uiState.novel
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(novel?.title ?: "小说详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                                if (dir != null && novel != null) {
                                    val file = ExportUtils.exportToTxt(novel, uiState.chapters, dir)
                                    scope.launch(Dispatchers.Main) {
                                        Toast.makeText(context, "已导出到: ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                scope.launch(Dispatchers.Main) {
                                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "导出")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (novel == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 小说信息
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(novel.title, style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${novel.genre} · ${novel.writingStyle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (novel.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(novel.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // 设定预览
                if (novel.worldSetting.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("世界设定", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(novel.worldSetting, style = MaterialTheme.typography.bodySmall, maxLines = 5)
                            }
                        }
                    }
                }

                if (novel.keyCharacters.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("核心角色", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(novel.keyCharacters, style = MaterialTheme.typography.bodySmall, maxLines = 5)
                            }
                        }
                    }
                }

                // 章节列表标题
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("章节 (${uiState.chapters.size})", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // 章节列表
                items(uiState.chapters) { chapter ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToChapter(chapter.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(chapter.title, style = MaterialTheme.typography.bodyLarge)
                                if (chapter.wordCount > 0) {
                                    Text("${chapter.wordCount}字", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // 生成按钮
                item {
                    Button(
                        onClick = onGenerateNextChapter,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isGenerating
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在生成第${uiState.nextChapterNumber}章...")
                        } else {
                            Text("生成第${uiState.nextChapterNumber}章")
                        }
                    }

                    uiState.error?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这本小说及所有章节吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteNovel()
                    showDeleteDialog = false
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}
