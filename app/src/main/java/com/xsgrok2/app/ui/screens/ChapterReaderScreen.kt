package com.xsgrok2.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsgrok2.app.ui.viewmodel.ChapterReaderUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterReaderScreen(
    uiState: ChapterReaderUiState,
    onBack: () -> Unit,
    onStartEditing: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onRewriteSelection: (Int, Int, String) -> Unit,
    onRewriteFull: (String) -> Unit,
    onGenerateNextChapter: () -> Unit,
    onNavigateToChapter: (Long) -> Unit,
    fontSize: Int = 16,
    nightMode: Boolean = false
) {
    val bgColor = if (nightMode) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.surface
    val contentColor = if (nightMode) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface
    val barBgColor = if (nightMode) Color(0xFF252540) else MaterialTheme.colorScheme.surfaceVariant
    var showRewriteDialog by remember { mutableStateOf(false) }
    var rewriteInstruction by remember { mutableStateOf("") }
    var rewriteMode by remember { mutableStateOf("full") } // "full" or "selection"
    var selectionText by remember { mutableStateOf("") }
    var selectionStart by remember { mutableStateOf(-1) }
    var selectionEnd by remember { mutableStateOf(-1) }
    var editContent by remember { mutableStateOf(uiState.editContent) }

    LaunchedEffect(uiState.editContent) {
        editContent = uiState.editContent
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.chapter?.displayTitle() ?: "阅读") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (nightMode) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.surface,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor,
                    actionIconContentColor = contentColor
                )
            )
        },
        bottomBar = {
            if (uiState.chapter != null) {
                Column(modifier = Modifier.background(barBgColor)) {
                    // Quick action bar
                    if (!uiState.isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Previous
                            IconButton(
                                onClick = { uiState.previousChapterId?.let { onNavigateToChapter(it) } },
                                enabled = uiState.previousChapterId != null
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一章", tint = if (uiState.previousChapterId != null) contentColor else contentColor.copy(alpha = 0.3f))
                            }
                            // Edit
                            IconButton(onClick = onStartEditing) {
                                Icon(Icons.Default.Edit, "编辑", tint = contentColor)
                            }
                            // AI Rewrite
                            IconButton(onClick = {
                                rewriteMode = "full"
                                rewriteInstruction = ""
                                showRewriteDialog = true
                            }) {
                                Icon(Icons.Default.AutoFixHigh, "AI改写", tint = contentColor)
                            }
                            // Generate next
                            IconButton(
                                onClick = onGenerateNextChapter,
                                enabled = !uiState.isGeneratingNext
                            ) {
                                if (uiState.isGeneratingNext) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = contentColor)
                                } else {
                                    Icon(Icons.Default.Add, "生成下一章", tint = contentColor)
                                }
                            }
                            // Next
                            IconButton(
                                onClick = { uiState.nextChapterId?.let { onNavigateToChapter(it) } },
                                enabled = uiState.nextChapterId != null
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "下一章", tint = if (uiState.nextChapterId != null) contentColor else contentColor.copy(alpha = 0.3f))
                            }
                        }
                    } else {
                        // Edit mode actions
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = { onSaveEdit(editContent) }) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("保存", color = contentColor)
                            }
                            Spacer(modifier = Modifier.width(24.dp))
                            TextButton(onClick = onCancelEdit) {
                                Text("取消", color = contentColor)
                            }
                        }
                    }

                    // Error display
                    uiState.error?.let { error ->
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                    }
                }
            }
        },
        containerColor = bgColor
    ) { padding ->
        if (uiState.chapter == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator()
            }
        } else if (uiState.isEditing) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).background(bgColor)) {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    textStyle = TextStyle(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.6f).sp,
                        color = contentColor
                    ),
                    placeholder = { Text("编辑章节内容...", color = contentColor.copy(alpha = 0.5f)) }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(bgColor)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                val rawContent = uiState.chapter.content.trim()
                // Strip AI-generated title from content (already shown in TopAppBar)
                val contentWithoutTitle = rawContent.lines().dropWhile { line ->
                    val t = line.trim()
                    t.startsWith("#") || (t.startsWith("第") && t.contains("章") && t.length < 30)
                }.joinToString("\n")
                val paragraphs = contentWithoutTitle.split("\n").filter { it.isNotBlank() }

                val contentStyle = TextStyle(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.8f).sp,
                    color = contentColor,
                    letterSpacing = 0.5.sp
                )
                paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph.trim(),
                        style = contentStyle,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "—— ${uiState.chapter.displayTitle()} · 完 ——",
                    style = MaterialTheme.typography.bodySmall.copy(color = contentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }

    // AI Rewrite Dialog
    if (showRewriteDialog) {
        AlertDialog(
            onDismissRequest = { showRewriteDialog = false },
            title = { Text("AI改写") },
            text = {
                Column {
                    Text("改写方式：", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = rewriteInstruction == "润色文笔，让文字更优美",
                            onClick = { rewriteInstruction = "润色文笔，让文字更优美" },
                            label = { Text("润色") }
                        )
                        FilterChip(
                            selected = rewriteInstruction == "扩写细节，增加更多描写",
                            onClick = { rewriteInstruction = "扩写细节，增加更多描写" },
                            label = { Text("扩写") }
                        )
                        FilterChip(
                            selected = rewriteInstruction == "精简压缩，去掉冗余",
                            onClick = { rewriteInstruction = "精简压缩，去掉冗余" },
                            label = { Text("精简") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rewriteInstruction,
                        onValueChange = { rewriteInstruction = it },
                        label = { Text("改写指令") },
                        placeholder = { Text("描述你想怎么改...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = rewriteMode == "full",
                            onClick = { rewriteMode = "full" },
                            label = { Text("改写全文") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = rewriteMode == "selection",
                            onClick = { rewriteMode = "selection" },
                            label = { Text("改写片段") }
                        )
                    }
                    if (rewriteMode == "selection") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = selectionText,
                            onValueChange = {
                                selectionText = it
                                // Find the position of this text in the chapter content
                                val content = uiState.chapter?.content ?: ""
                                val idx = content.indexOf(it)
                                if (idx >= 0) {
                                    selectionStart = idx
                                    selectionEnd = idx + it.length
                                } else {
                                    selectionStart = -1
                                    selectionEnd = -1
                                }
                            },
                            label = { Text("粘贴要改写的原文片段") },
                            placeholder = { Text("从阅读中复制想改写的段落粘贴到这里") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (rewriteMode == "selection" && selectionStart >= 0 && selectionEnd > selectionStart) {
                            onRewriteSelection(selectionStart, selectionEnd, rewriteInstruction)
                        } else {
                            onRewriteFull(rewriteInstruction)
                        }
                        showRewriteDialog = false
                    },
                    enabled = rewriteInstruction.isNotBlank() && !uiState.isRewriting &&
                            (rewriteMode == "full" || (selectionStart >= 0 && selectionEnd > selectionStart))
                ) {
                    if (uiState.isRewriting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("改写")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRewriteDialog = false }) { Text("取消") }
            }
        )
    }
}
