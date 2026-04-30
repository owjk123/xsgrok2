package com.xsgrok2.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xsgrok2.app.App
import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.repository.NovelRepository
import com.xsgrok2.app.data.repository.GrokRepository
import com.xsgrok2.app.data.api.GrokApiService
import com.xsgrok2.app.ui.viewmodel.NovelDetailViewModel
import com.xsgrok2.app.ui.viewmodel.NovelDetailUiState
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novelId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToChapter: (Long, Long) -> Unit
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

    val viewModel: NovelDetailViewModel = viewModel(
        factory = NovelDetailViewModel.Factory(novelId, novelRepository, grokRepository, preferences)
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
                        uiState.novel?.title ?: "Novel",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val novel = uiState.novel
                if (novel != null) {
                    // Novel Info Card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(novel.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Genre: ${novel.genre}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (novel.worldSetting.isNotBlank()) {
                                Text("World Setting", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    novel.worldSetting,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (novel.keyCharacters.isNotBlank()) {
                                Text("Key Characters", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    novel.keyCharacters,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (novel.outline.isNotBlank()) {
                                Text("Outline", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    novel.outline,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Generate Next Chapter
                    Button(
                        onClick = viewModel::generateNextChapter,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isGenerating
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Chapter ${uiState.nextChapterNumber}...")
                        } else {
                            Icon(Icons.Default.AutoStories, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Chapter ${uiState.nextChapterNumber}")
                        }
                    }

                    // Chapter List
                    if (uiState.chapters.isNotEmpty()) {
                        Text("Chapters", style = MaterialTheme.typography.titleMedium)
                        uiState.chapters.forEach { chapter ->
                            ChapterItem(
                                chapter = chapter,
                                onClick = { onNavigateToChapter(novelId, chapter.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterItem(
    chapter: Chapter,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chapter.title,
                    style = MaterialTheme.typography.titleSmall
                )
                if (chapter.content.isNotBlank()) {
                    Text(
                        "${chapter.content.take(100)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (chapter.isGenerated) {
                Text(
                    "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
