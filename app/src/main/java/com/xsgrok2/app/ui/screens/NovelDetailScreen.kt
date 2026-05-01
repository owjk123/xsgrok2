package com.xsgrok2.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.ChapterInstruction
import com.xsgrok2.app.data.model.LorebookEntry
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
    onGenerateNextChapter: (String, ChapterInstruction?, String) -> Unit,
    onInsertChapterAt: (Int, String, ChapterInstruction?, String) -> Unit,
    onRegenerateChapter: (Long, String, ChapterInstruction?, String) -> Unit,
    onDeleteChapter: (Long) -> Unit,
    onUpdateChapterTitle: (Long, String) -> Unit,
    onNavigateToChapter: (Long) -> Unit,
    onDeleteNovel: () -> Unit,
    onUpdateNovelSetting: (String, String) -> Unit,
    onRegenerateSettings: () -> Unit,
    onAddLorebookEntry: (String, String, Int) -> Unit,
    onDeleteLorebookEntry: (LorebookEntry) -> Unit,
    onInitializeCharacterStates: () -> Unit = {}  // v3.2新增
) {
    val novel = uiState.novel
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLorebookDialog by remember { mutableStateOf(false) }
    var editingChapterId by remember { mutableStateOf<Long?>(null) }
    var editingChapterTitle by remember { mutableStateOf("") }

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
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设定管理")
                    }
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                if (novel != null) {
                                    val file = ExportUtils.exportToTxt(novel, uiState.chapters, context)
                                    scope.launch(Dispatchers.Main) {
                                        Toast.makeText(context, "已导出到Download: ${file.name}", Toast.LENGTH_LONG).show()
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
                            Text("${novel.genre} · ${novel.writingStyle} · ${novel.totalWordCount}字", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (novel.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(novel.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // 设定预览（可点击展开/编辑）
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showSettingsDialog = true }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("小说设定", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("点击编辑", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            if (novel.worldSetting.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(novel.worldSetting, style = MaterialTheme.typography.bodySmall, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Lorebook快捷入口
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showLorebookDialog = true }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("世界词条", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("${uiState.lorebookEntries.size}条", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Row {
                            OutlinedButton(
                                onClick = { showGenerateDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("添加章节")
                            }
                        }
                    }
                }

                // 章节列表
                items(uiState.chapters, key = { it.id }) { chapter ->
                    ChapterCard(
                        chapter = chapter,
                        onRead = { onNavigateToChapter(chapter.id) },
                        onRegenerate = { onRegenerateChapter(chapter.id, "rewrite", null, "") },
                        onDelete = { onDeleteChapter(chapter.id) },
                        onEditTitle = {
                            editingChapterId = chapter.id
                            editingChapterTitle = chapter.customTitle.ifBlank { chapter.title }
                        }
                    )
                }

                // 生成下一章按钮
                item {
                    Button(
                        onClick = { showGenerateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isGenerating
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.generationStage.isNotEmpty()) uiState.generationStage else "正在生成...")
                        } else {
                            Icon(Icons.Default.AutoStories, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
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

    // === Generate Chapter Dialog ===
    if (showGenerateDialog) {
        GenerateChapterDialog(
            nextChapterNumber = uiState.nextChapterNumber,
            existingChapterCount = uiState.chapters.size,
            onDismiss = { showGenerateDialog = false },
            onGenerateNext = { title, instruction, note ->
                onGenerateNextChapter(title, instruction, note)
                showGenerateDialog = false
            },
            onInsertAt = { position, title, instruction, note ->
                onInsertChapterAt(position, title, instruction, note)
                showGenerateDialog = false
            },
            isGenerating = uiState.isGenerating
        )
    }

    // === Settings Edit Dialog ===
    if (showSettingsDialog && novel != null) {
        NovelSettingsDialog(
            novel = novel,
            onDismiss = { showSettingsDialog = false },
            onUpdateSetting = onUpdateNovelSetting,
            onRegenerateSettings = onRegenerateSettings,
            isGenerating = uiState.isGenerating
        )
    }

    // === Lorebook Dialog ===
    if (showLorebookDialog) {
        LorebookDialog(
            entries = uiState.lorebookEntries,
            onDismiss = { showLorebookDialog = false },
            onAddEntry = onAddLorebookEntry,
            onDeleteEntry = onDeleteLorebookEntry
        )
    }

    // === Edit Chapter Title Dialog ===
    editingChapterId?.let { chId ->
        AlertDialog(
            onDismissRequest = { editingChapterId = null },
            title = { Text("修改章节标题") },
            text = {
                OutlinedTextField(
                    value = editingChapterTitle,
                    onValueChange = { editingChapterTitle = it },
                    label = { Text("章节标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateChapterTitle(chId, editingChapterTitle)
                    editingChapterId = null
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { editingChapterId = null }) { Text("取消") }
            }
        )
    }

    // === Delete Novel Dialog ===
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

@Composable
private fun ChapterCard(
    chapter: Chapter,
    onRead: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    onEditTitle: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    val statusColor = when (chapter.status) {
        "draft" -> MaterialTheme.colorScheme.tertiary
        "edited" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (chapter.status) {
        "draft" -> "草稿"
        "edited" -> "已编辑"
        else -> "已生成"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(chapter.displayTitle(), style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chapter.wordCount > 0) {
                        Text("${chapter.wordCount}字", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
                    // v3.2: 显示质量评分
                    chapter.qualityScore?.let { score ->
                        Spacer(modifier = Modifier.width(8.dp))
                        val scoreColor = when {
                            score >= 0.8f -> MaterialTheme.colorScheme.primary
                            score >= 0.6f -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                        Text("评分: ${(score * 100).toInt()}", style = MaterialTheme.typography.labelSmall, color = scoreColor)
                    }
                }
            }
            IconButton(onClick = onEditTitle) {
                Icon(Icons.Default.Edit, contentDescription = "改标题", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRead) {
                Icon(Icons.Default.AutoStories, contentDescription = "阅读", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showActions = !showActions }) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
        }
        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onRegenerate(); showActions = false }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重写")
                }
                TextButton(onClick = { onDelete(); showActions = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerateChapterDialog(
    nextChapterNumber: Int,
    existingChapterCount: Int,
    onDismiss: () -> Unit,
    onGenerateNext: (String, ChapterInstruction?, String) -> Unit,
    onInsertAt: (Int, String, ChapterInstruction?, String) -> Unit,
    isGenerating: Boolean
) {
    var mode by remember { mutableStateOf("next") } // "next" or "insert"
    var insertPosition by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    var coreEvent by remember { mutableStateOf("") }
    var characterChanges by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("") }
    var forbiddenElements by remember { mutableStateOf("") }
    var simpleNote by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("生成章节") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Mode selection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = mode == "next",
                        onClick = { mode = "next" },
                        label = { Text("续写下一章") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = mode == "insert",
                        onClick = { mode = "insert" },
                        label = { Text("插入章节") }
                    )
                }

                if (mode == "insert") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = insertPosition,
                        onValueChange = { insertPosition = it },
                        label = { Text("插入位置（第N章之后）") },
                        placeholder = { Text("如输入3，则在第3章后插入") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("章节标题（可选）") },
                    placeholder = { Text("留空则自动生成") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = simpleNote,
                    onValueChange = { simpleNote = it },
                    label = { Text("简单要求") },
                    placeholder = { Text("如：这一章要发生一场战斗...") },
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )

                // Advanced toggle
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "收起高级选项 ▲" else "展开高级选项 ▼")
                }

                if (showAdvanced) {
                    OutlinedTextField(
                        value = coreEvent,
                        onValueChange = { coreEvent = it },
                        label = { Text("核心事件") },
                        placeholder = { Text("这一章必须发生什么事") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = characterChanges,
                        onValueChange = { characterChanges = it },
                        label = { Text("人物变化") },
                        placeholder = { Text("角色状态/关系的改变") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = mood,
                        onValueChange = { mood = it },
                        label = { Text("情绪氛围") },
                        placeholder = { Text("如：紧张、温馨、悲伤...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = forbiddenElements,
                        onValueChange = { forbiddenElements = it },
                        label = { Text("禁止出现") },
                        placeholder = { Text("这一章绝对不能出现的内容") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val instruction = if (coreEvent.isNotBlank() || characterChanges.isNotBlank() || mood.isNotBlank() || forbiddenElements.isNotBlank()) {
                        ChapterInstruction(
                            coreEvent = coreEvent,
                            characterChanges = characterChanges,
                            mood = mood,
                            forbiddenElements = forbiddenElements
                        )
                    } else null

                    if (mode == "next") {
                        onGenerateNext(customTitle, instruction, simpleNote)
                    } else {
                        val pos = insertPosition.toIntOrNull() ?: (existingChapterCount)
                        onInsertAt(pos + 1, customTitle, instruction, simpleNote)
                    }
                },
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("生成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun NovelSettingsDialog(
    novel: Novel,
    onDismiss: () -> Unit,
    onUpdateSetting: (String, String) -> Unit,
    onRegenerateSettings: () -> Unit,
    isGenerating: Boolean
) {
    var worldSetting by remember(novel.worldSetting) { mutableStateOf(novel.worldSetting) }
    var keyCharacters by remember(novel.keyCharacters) { mutableStateOf(novel.keyCharacters) }
    var outline by remember(novel.outline) { mutableStateOf(novel.outline) }
    var currentTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("小说设定管理") },
        text = {
            Column {
                TabRow(selectedTabIndex = currentTab) {
                    Tab(selected = currentTab == 0, onClick = { currentTab = 0 }, text = { Text("世界设定") })
                    Tab(selected = currentTab == 1, onClick = { currentTab = 1 }, text = { Text("角色") })
                    Tab(selected = currentTab == 2, onClick = { currentTab = 2 }, text = { Text("大纲") })
                }
                Spacer(modifier = Modifier.height(8.dp))
                when (currentTab) {
                    0 -> OutlinedTextField(
                        value = worldSetting,
                        onValueChange = { worldSetting = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        label = { Text("世界设定") }
                    )
                    1 -> OutlinedTextField(
                        value = keyCharacters,
                        onValueChange = { keyCharacters = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        label = { Text("核心角色") }
                    )
                    2 -> OutlinedTextField(
                        value = outline,
                        onValueChange = { outline = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        label = { Text("故事大纲") }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onRegenerateSettings, enabled = !isGenerating) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重新生成全部设定")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdateSetting("worldSetting", worldSetting)
                onUpdateSetting("keyCharacters", keyCharacters)
                onUpdateSetting("outline", outline)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun LorebookDialog(
    entries: List<LorebookEntry>,
    onDismiss: () -> Unit,
    onAddEntry: (String, String, Int) -> Unit,
    onDeleteEntry: (LorebookEntry) -> Unit
) {
    var newKeyword by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var newImportance by remember { mutableStateOf(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("世界词条 (Lorebook)") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("添加世界设定词条，生成章节时会自动注入相关上下文", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Existing entries
                if (entries.isNotEmpty()) {
                    entries.forEach { entry ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.keyword, style = MaterialTheme.typography.labelLarge)
                                    Text(entry.content, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }
                                IconButton(onClick = { onDeleteEntry(entry) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add new entry
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("添加新词条", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newKeyword,
                    onValueChange = { newKeyword = it },
                    label = { Text("关键词") },
                    placeholder = { Text("如：灵气、暗影组织...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    label = { Text("词条内容") },
                    placeholder = { Text("详细描述这个设定的内容...") },
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("重要度", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    (1..5).forEach { i ->
                        FilterChip(
                            selected = newImportance == i,
                            onClick = { newImportance = i },
                            label = { Text("$i") }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (newKeyword.isNotBlank() && newContent.isNotBlank()) {
                    TextButton(onClick = {
                        onAddEntry(newKeyword, newContent, newImportance)
                        newKeyword = ""
                        newContent = ""
                        newImportance = 3
                    }) { Text("添加词条") }
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("完成") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
