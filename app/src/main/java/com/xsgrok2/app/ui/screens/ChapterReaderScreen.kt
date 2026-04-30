package com.xsgrok2.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xsgrok2.app.App
import com.xsgrok2.app.data.repository.NovelRepository
import com.xsgrok2.app.data.repository.GrokRepository
import com.xsgrok2.app.data.api.GrokApiService
import com.xsgrok2.app.ui.viewmodel.ChapterReaderViewModel
import com.xsgrok2.app.ui.viewmodel.ChapterReaderUiState
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterReaderScreen(
    novelId: Long,
    chapterId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val database = app.database
    val preferences = app.preferences

    val novelRepository = remember {
        NovelRepository(database.novelDao(), database.chapterDao())
    }
    val grokRepository = remember {
        val retrofit = Retrofit.Builder()
            .baseUrl(preferences.apiBaseUrl.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        GrokRepository(retrofit.create(GrokApiService::class.java))
    }

    val viewModel: ChapterReaderViewModel = viewModel(
        factory = ChapterReaderViewModel.Factory(novelId, chapterId, novelRepository, grokRepository, preferences)
    )

    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.chapter?.title ?: "Chapter",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.chapter != null) {
                        IconButton(
                            onClick = viewModel::regenerateChapter,
                            enabled = !uiState.isGenerating
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Regenerating chapter...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            val chapter = uiState.chapter
            if (chapter != null && chapter.content.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        chapter.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        chapter.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = androidx.compose.ui.unit.TextUnit(1.8f, androidx.compose.ui.unit.TextUnitType.Em)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No content yet. Tap refresh to regenerate.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
