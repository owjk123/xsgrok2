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
    val isEditing: Boolean = false,
    val editContent: String = "",
    val isRewriting: Boolean = false,
    val error: String? = null,
    val previousChapterId: Long? = null,
    val nextChapterId: Long? = null
)

class ChapterReaderViewModel(
    private val chapterId: Long,
    private val novelRepository: NovelRepository,
    private val grokRepository: GrokRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterReaderUiState())
    val uiState: StateFlow<ChapterReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapter(chapterId)
    }

    private fun loadChapter(chId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val chapter = novelRepository.getChapterById(chId)
            val novel = chapter?.novelId?.let { novelRepository.getNovelById(it) }
            val prevChapter = chapter?.let {
                novelRepository.getPreviousChapter(it.novelId, it.chapterNumber)
            }
            val nextChapter = chapter?.let {
                novelRepository.getNextChapter(it.novelId, it.chapterNumber)
            }
            _uiState.update {
                it.copy(
                    chapter = chapter,
                    novel = novel,
                    isLoading = false,
                    isEditing = false,
                    editContent = "",
                    previousChapterId = prevChapter?.id,
                    nextChapterId = nextChapter?.id
                )
            }
        }
    }

    fun startEditing() {
        val content = _uiState.value.chapter?.content ?: return
        _uiState.update { it.copy(isEditing = true, editContent = content) }
    }

    fun updateEditContent(content: String) {
        _uiState.update { it.copy(editContent = content) }
    }

    fun saveEdit() {
        viewModelScope.launch {
            val chapter = _uiState.value.chapter ?: return@launch
            val newContent = _uiState.value.editContent
            val wordCount = newContent.filter { it.code > 127 }.length
            novelRepository.updateChapter(chapter.copy(
                content = newContent,
                wordCount = wordCount,
                status = "edited"
            ))
            val updated = novelRepository.getChapterById(chapterId)
            _uiState.update { it.copy(isEditing = false, chapter = updated, editContent = "") }
            updated?.novelId?.let { nid ->
                val novel = novelRepository.getNovelById(nid)
                if (novel != null) {
                    val chapters = novelRepository.getChaptersByNovelId(nid).first()
                    val totalWords = chapters.sumOf { c -> c.wordCount }
                    novelRepository.updateNovel(novel.copy(totalWordCount = totalWords, updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(isEditing = false, editContent = "") }
    }

    fun rewriteSelection(selectedText: String, instruction: String, onResult: (String) -> Unit) {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "请先在设置中配置API密钥") }
            return
        }
        val novel = _uiState.value.novel ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRewriting = true, error = null) }
            val result = grokRepository.rewriteSelection(apiKey, preferences.model, novel, selectedText, instruction)
            result.fold(
                onSuccess = { rewritten ->
                    // Apply rewrite: replace selected text in content
                    val currentContent = _uiState.value.chapter?.content ?: return@fold
                    val newContent = currentContent.replace(selectedText, rewritten)
                    val wordCount = newContent.filter { it.code > 127 }.length
                    val chapter = _uiState.value.chapter ?: return@fold
                    novelRepository.updateChapter(chapter.copy(
                        content = newContent,
                        wordCount = wordCount,
                        status = "edited"
                    ))
                    val updated = novelRepository.getChapterById(chapterId)
                    _uiState.update { it.copy(chapter = updated, isRewriting = false) }
                    onResult(rewritten)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isRewriting = false, error = e.message ?: "改写失败") }
                }
            )
        }
    }

    fun applyRewriteResult(rewrittenText: String, originalText: String) {
        val currentContent = if (_uiState.value.isEditing) _uiState.value.editContent else _uiState.value.chapter?.content ?: return
        val newContent = currentContent.replace(originalText, rewrittenText)
        if (_uiState.value.isEditing) {
            _uiState.update { it.copy(editContent = newContent) }
        } else {
            _uiState.update { it.copy(isEditing = true, editContent = newContent) }
        }
    }

    fun navigateToChapter(newChapterId: Long) {
        loadChapter(newChapterId)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory(
        private val chapterId: Long,
        private val novelRepository: NovelRepository,
        private val grokRepository: GrokRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChapterReaderViewModel(chapterId, novelRepository, grokRepository, preferences) as T
        }
    }
}
