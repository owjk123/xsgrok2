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
    onRewriteSelection: (String, String, (String) -> Unit) -> Unit,
    onApplyRewrite: (String, String) -> Unit,
    onNavigateToChapter: (Long) -> Unit,
    fontSize: Int = 16,
    nightMode: Boolean = false
) {
    val bgColor = if (nightMode) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.surface
    val contentColor = if (nightMode) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface
    var showRewriteDialog by remember { mutableStateOf(false) }
    var rewriteInstruction by remember { mutableStateOf("") }
    var selectedText by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf(uiState.editContent) }

    // Sync editContent when uiState changes
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
                BottomAppBar(
                    containerColor = if (nightMode) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.surface,
                ) {
                    IconButton(
                        onClick = { uiState.previousChapterId?.let { onNavigateToChapter(it) } },
                        enabled = uiState.previousChapterId != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一章", tint = if (uiState.previousChapterId != null) contentColor else contentColor.copy(alpha = 0.3f))
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    if (uiState.isEditing) {
                        TextButton(onClick = { onSaveEdit(editContent) }) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("保存", color = contentColor)
                        }
                        TextButton(onClick = onCancelEdit) {
                            Text("取消", color = contentColor)
                        }
                    } else {
                        TextButton(onClick = onStartEditing) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("编辑", color = contentColor)
                        }
                        TextButton(onClick = {
                            showRewriteDialog = true
                            selectedText = ""
                            rewriteInstruction = ""
                        }) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI改写", color = contentColor)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { uiState.nextChapterId?.let { onNavigateToChapter(it) } },
                        enabled = uiState.nextChapterId != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一章", tint = if (uiState.nextChapterId != null) contentColor else contentColor.copy(alpha = 0.3f))
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
                Text(
                    text = uiState.chapter.displayTitle(),
                    style = MaterialTheme.typography.headlineMedium.copy(color = contentColor),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val contentStyle = TextStyle(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.8f).sp,
                    color = contentColor,
                    letterSpacing = 0.5.sp
                )
                val paragraphs = uiState.chapter.content.split("\n").filter { it.isNotBlank() }
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
                    Text("选择改写方式：", style = MaterialTheme.typography.bodySmall)
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
                    OutlinedTextField(
                        value = selectedText,
                        onValueChange = { selectedText = it },
                        label = { Text("要改写的原文（可选）") },
                        placeholder = { Text("留空则改写整章，或粘贴想改写的片段") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chapter = uiState.chapter ?: return@TextButton
                        val textToRewrite = if (selectedText.isNotBlank()) selectedText else chapter.content
                        onRewriteSelection(textToRewrite, rewriteInstruction) { result ->
                            onApplyRewrite(textToRewrite, result)
                        }
                        showRewriteDialog = false
                    },
                    enabled = rewriteInstruction.isNotBlank() && !uiState.isRewriting
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
