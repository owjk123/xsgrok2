package com.xsgrok2.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.Novel
import com.xsgrok2.app.data.preferences.AppPreferences
import com.xsgrok2.app.data.repository.GrokRepository
import com.xsgrok2.app.data.repository.NovelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChapterReaderUiState(
    val chapter: Chapter? = null,
    val novel: Novel? = null,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null
)

class ChapterReaderViewModel(
    private val novelId: Long,
    private val chapterId: Long,
    private val novelRepository: NovelRepository,
    private val grokRepository: GrokRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterReaderUiState())
    val uiState: StateFlow<ChapterReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val chapter = novelRepository.getChapterById(chapterId)
            val novel = novelRepository.getNovelById(novelId)
            _uiState.update {
                it.copy(
                    chapter = chapter,
                    novel = novel,
                    isLoading = false
                )
            }
        }
    }

    fun regenerateChapter() {
        val chapter = _uiState.value.chapter ?: return
        val novel = _uiState.value.novel ?: return
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "Please set your API key in Settings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            val result = grokRepository.generateChapter(
                apiKey = apiKey,
                model = preferences.model,
                novel = novel,
                chapterNumber = chapter.chapterNumber,
                chapterTitle = chapter.title
            )
            result.fold(
                onSuccess = { content ->
                    val updatedChapter = chapter.copy(content = content, isGenerated = true)
                    novelRepository.updateChapter(updatedChapter)
                    _uiState.update { it.copy(chapter = updatedChapter, isGenerating = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            error = e.message ?: "Failed to regenerate"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory(
        private val novelId: Long,
        private val chapterId: Long,
        private val novelRepository: NovelRepository,
        private val grokRepository: GrokRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChapterReaderViewModel(novelId, chapterId, novelRepository, grokRepository, preferences) as T
        }
    }
}
