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

data class NovelDetailUiState(
    val novel: Novel? = null,
    val chapters: List<Chapter> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val nextChapterNumber: Int = 1
)

class NovelDetailViewModel(
    private val novelId: Long,
    private val novelRepository: NovelRepository,
    private val grokRepository: GrokRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(NovelDetailUiState())
    val uiState: StateFlow<NovelDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            novelRepository.getNovelByIdFlow(novelId).collect { novel ->
                _uiState.update { it.copy(novel = novel, isLoading = false) }
            }
        }
        viewModelScope.launch {
            novelRepository.getChaptersByNovelId(novelId).collect { chapters ->
                _uiState.update {
                    it.copy(
                        chapters = chapters,
                        nextChapterNumber = chapters.size + 1
                    )
                }
            }
        }
    }

    fun generateNextChapter() {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "Please set your API key in Settings first") }
            return
        }
        val novel = _uiState.value.novel ?: return
        val chapterNumber = _uiState.value.nextChapterNumber
        val chapterTitle = "Chapter $chapterNumber"

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            val previousContent = _uiState.value.chapters.lastOrNull()?.content ?: ""
            val result = grokRepository.generateChapter(
                apiKey = apiKey,
                model = preferences.model,
                novel = novel,
                chapterNumber = chapterNumber,
                chapterTitle = chapterTitle,
                previousChapterContent = previousContent
            )
            result.fold(
                onSuccess = { content ->
                    val chapter = Chapter(
                        novelId = novelId,
                        chapterNumber = chapterNumber,
                        title = chapterTitle,
                        content = content,
                        isGenerated = true
                    )
                    novelRepository.insertChapter(chapter)
                    _uiState.update { it.copy(isGenerating = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            error = e.message ?: "Failed to generate chapter"
                        )
                    }
                }
            )
        }
    }

    fun deleteNovel() {
        viewModelScope.launch {
            val novel = _uiState.value.novel ?: return@launch
            novelRepository.deleteChaptersByNovelId(novelId)
            novelRepository.deleteNovel(novel)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory(
        private val novelId: Long,
        private val novelRepository: NovelRepository,
        private val grokRepository: GrokRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NovelDetailViewModel(novelId, novelRepository, grokRepository, preferences) as T
        }
    }
}
