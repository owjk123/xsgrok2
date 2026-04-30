package com.xsgrok2.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xsgrok2.app.App
import com.xsgrok2.app.data.repository.NovelRepository
import com.xsgrok2.app.data.repository.GrokRepository
import com.xsgrok2.app.data.api.GrokApiService
import com.xsgrok2.app.ui.viewmodel.CreateNovelViewModel
import com.xsgrok2.app.ui.viewmodel.CreateNovelUiState
import com.xsgrok2.app.ui.viewmodel.CreateStep
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNovelScreen(
    onNavigateBack: () -> Unit,
    onNovelCreated: (Long) -> Unit
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

    val viewModel: CreateNovelViewModel = viewModel(
        factory = CreateNovelViewModel.Factory(novelRepository, grokRepository, preferences)
    )

    val uiState by viewModel.uiState.collectAsState()

    // Navigate when novel is created
    LaunchedEffect(uiState.createdNovelId) {
        uiState.createdNovelId?.let { onNovelCreated(it) }
    }

    // Show error snackbar
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
                    Text(when (uiState.step) {
                        CreateStep.INPUT -> "New Novel"
                        CreateStep.GENERATING -> "Generating..."
                        CreateStep.REVIEW -> "Review Settings"
                        CreateStep.SAVING -> "Saving..."
                    })
                },
                navigationIcon = {
                    if (uiState.step == CreateStep.INPUT || uiState.step == CreateStep.REVIEW) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (uiState.step) {
            CreateStep.INPUT -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Create a New Novel",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Describe your novel idea and let AI generate the world setting, characters, and story outline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Genre suggestions
                    Text("Genre", style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val genres = listOf("Fantasy", "Romance", "Mystery", "Sci-Fi", "Comedy", "Daily")
                        genres.forEach { genre ->
                            FilterChip(
                                selected = uiState.genre.equals(genre, ignoreCase = true),
                                onClick = { viewModel.updateGenre(genre) },
                                label = { Text(genre) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.genre,
                        onValueChange = viewModel::updateGenre,
                        label = { Text("Genre (or type your own)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("Description") },
                        placeholder = { Text("Describe your novel idea in detail...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 8
                    )

                    Button(
                        onClick = viewModel::generateSettings,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.genre.isNotBlank() && uiState.description.isNotBlank()
                    ) {
                        Text("Generate Settings")
                    }
                }
            }
            CreateStep.GENERATING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI is generating novel settings...", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This may take a moment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            CreateStep.REVIEW -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Review Generated Settings", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "You can edit the generated settings before confirming.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = uiState.generatedSettings,
                        onValueChange = viewModel::updateGeneratedSettings,
                        label = { Text("Generated Settings") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        maxLines = 30
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::generateSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Regenerate")
                        }
                        Button(
                            onClick = viewModel::confirmAndCreateNovel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm & Create")
                        }
                    }
                }
            }
            CreateStep.SAVING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Saving novel...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
