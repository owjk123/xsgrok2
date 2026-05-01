package com.xsgrok2.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsgrok2.app.ui.viewmodel.ChapterReaderUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterReaderScreen(
    uiState: ChapterReaderUiState,
    onBack: () -> Unit,
    fontSize: Int = 16,
    nightMode: Boolean = false
) {
    val bgColor = if (nightMode) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.surface
    val contentColor = if (nightMode) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.chapter?.title ?: "阅读") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (nightMode) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.surface,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        if (uiState.chapter == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator()
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
                // 章节标题
                Text(
                    text = uiState.chapter.title,
                    style = MaterialTheme.typography.headlineMedium.copy(color = contentColor),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 章节内容
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
                    "—— ${uiState.chapter.title} · 完 ——",
                    style = MaterialTheme.typography.bodySmall.copy(color = contentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }
}
