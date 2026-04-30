package com.xsgrok2.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xsgrok2.app.data.database.AppDatabase
import com.xsgrok2.app.data.model.Novel
import com.xsgrok2.app.data.preferences.AppPreferences
import com.xsgrok2.app.data.repository.GrokRepository
import com.xsgrok2.app.data.repository.NovelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CreateNovelUiState(
    val genre: String = "",
    val description: String = "",
    val generatedSettings: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val step: CreateStep = CreateStep.INPUT,
    val createdNovelId: Long? = null
)

enum class CreateStep {
    INPUT,
    GENERATING,
    REVIEW,
    SAVING
}

class CreateNovelViewModel(
    private val novelRepository: NovelRepository,
    private val grokRepository: GrokRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateNovelUiState())
    val uiState: StateFlow<CreateNovelUiState> = _uiState.asStateFlow()

    fun updateGenre(genre: String) {
        _uiState.update { it.copy(genre = genre) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateGeneratedSettings(settings: String) {
        _uiState.update { it.copy(generatedSettings = settings) }
    }

    fun generateSettings() {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "Please set your API key in Settings first") }
            return
        }
        val genre = _uiState.value.genre.trim()
        val description = _uiState.value.description.trim()
        if (genre.isEmpty() || description.isEmpty()) {
            _uiState.update { it.copy(error = "Please fill in both genre and description") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, step = CreateStep.GENERATING, error = null) }
            val result = grokRepository.generateNovelSettings(
                apiKey = apiKey,
                model = preferences.model,
                genre = genre,
                description = description
            )
            result.fold(
                onSuccess = { settings ->
                    _uiState.update {
                        it.copy(
                            generatedSettings = settings,
                            isLoading = false,
                            step = CreateStep.REVIEW
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = CreateStep.INPUT,
                            error = e.message ?: "Failed to generate settings"
                        )
                    }
                }
            )
        }
    }

    fun confirmAndCreateNovel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, step = CreateStep.SAVING) }
            val state = _uiState.value
            val novel = Novel(
                title = extractTitle(state.generatedSettings, state.genre),
                genre = state.genre,
                description = state.description,
                worldSetting = extractSection(state.generatedSettings, "World Setting"),
                keyCharacters = extractSection(state.generatedSettings, "Key Characters"),
                outline = extractSection(state.generatedSettings, "Story Outline"),
                model = preferences.model
            )
            val id = novelRepository.insertNovel(novel)
            _uiState.update { it.copy(isLoading = false, createdNovelId = id) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun extractTitle(settings: String, genre: String): String {
        val lines = settings.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.startsWith("Title")) {
                val title = trimmed.removePrefix("#").removePrefix("Title:").removePrefix("Title").trim()
                if (title.isNotEmpty()) return title
            }
        }
        return "$genre Novel"
    }

    private fun extractSection(text: String, sectionName: String): String {
        val lines = text.lines()
        val result = StringBuilder()
        var inSection = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains(sectionName, ignoreCase = true) &&
                (trimmed.startsWith("#") || trimmed.startsWith("**") || trimmed.startsWith("-") || trimmed.endsWith(":"))) {
                inSection = true
                continue
            }
            if (inSection) {
                if (trimmed.startsWith("#") || (trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4)) {
                    break
                }
                result.appendLine(line)
            }
        }
        return result.toString().trim()
    }

    class Factory(
        private val novelRepository: NovelRepository,
        private val grokRepository: GrokRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateNovelViewModel(novelRepository, grokRepository, preferences) as T
        }
    }
}
