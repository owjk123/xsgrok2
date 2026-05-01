package com.xsgrok2.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.Novel
import com.xsgrok2.app.data.repository.NovelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChapterReaderUiState(
    val chapter: Chapter? = null,
    val novel: Novel? = null,
    val isLoading: Boolean = false
)

class ChapterReaderViewModel(
    private val chapterId: Long,
    private val novelRepository: NovelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterReaderUiState())
    val uiState: StateFlow<ChapterReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val chapter = novelRepository.getChapterById(chapterId)
            val novel = chapter?.novelId?.let { novelRepository.getNovelById(it) }
            _uiState.update {
                it.copy(
                    chapter = chapter,
                    novel = novel,
                    isLoading = false
                )
            }
        }
    }

    class Factory(
        private val chapterId: Long,
        private val novelRepository: NovelRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChapterReaderViewModel(chapterId, novelRepository) as T
        }
    }
}
