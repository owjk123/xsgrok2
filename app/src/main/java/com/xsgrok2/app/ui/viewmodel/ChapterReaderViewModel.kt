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
    val nextChapterId: Long? = null,
    val isGeneratingNext: Boolean = false
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

    /**
     * AI改写 - 使用位置索引精准替换
     * @param startIndex 选中文本在content中的起始位置
     * @param endIndex 选中文本在content中的结束位置
     */
    fun rewriteSelection(startIndex: Int, endIndex: Int, instruction: String) {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "请先在设置中配置API密钥") }
            return
        }
        val novel = _uiState.value.novel ?: return
        val content = _uiState.value.chapter?.content ?: return

        if (startIndex < 0 || endIndex > content.length || startIndex >= endIndex) {
            _uiState.update { it.copy(error = "选区无效") }
            return
        }

        val selectedText = content.substring(startIndex, endIndex)

        viewModelScope.launch {
            _uiState.update { it.copy(isRewriting = true, error = null) }
            val result = grokRepository.rewriteSelection(apiKey, preferences.model, novel, selectedText, instruction)
            result.fold(
                onSuccess = { rewritten ->
                    // Use position-based replace: only replace the specific range
                    val newContent = content.substring(0, startIndex) + rewritten + content.substring(endIndex)
                    val wordCount = newContent.filter { it.code > 127 }.length
                    val chapter = _uiState.value.chapter ?: return@fold
                    novelRepository.updateChapter(chapter.copy(
                        content = newContent,
                        wordCount = wordCount,
                        status = "edited"
                    ))
                    val updated = novelRepository.getChapterById(chapterId)
                    _uiState.update { it.copy(chapter = updated, isRewriting = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isRewriting = false, error = e.message ?: "改写失败") }
                }
            )
        }
    }

    /**
     * AI改写全文
     */
    fun rewriteFullContent(instruction: String) {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "请先在设置中配置API密钥") }
            return
        }
        val novel = _uiState.value.novel ?: return
        val content = _uiState.value.chapter?.content ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRewriting = true, error = null) }
            val result = grokRepository.rewriteSelection(apiKey, preferences.model, novel, content, instruction)
            result.fold(
                onSuccess = { rewritten ->
                    val wordCount = rewritten.filter { it.code > 127 }.length
                    val chapter = _uiState.value.chapter ?: return@fold
                    novelRepository.updateChapter(chapter.copy(
                        content = rewritten,
                        wordCount = wordCount,
                        status = "edited"
                    ))
                    val updated = novelRepository.getChapterById(chapterId)
                    _uiState.update { it.copy(chapter = updated, isRewriting = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isRewriting = false, error = e.message ?: "改写失败") }
                }
            )
        }
    }

    /**
     * 从阅读器直接生成下一章
     */
    fun generateNextChapterFromReader() {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "请先在设置中配置API密钥") }
            return
        }
        val novel = _uiState.value.novel ?: return
        val currentChapter = _uiState.value.chapter ?: return
        val novelId = currentChapter.novelId

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingNext = true, error = null) }
            
            val chapters = novelRepository.getChaptersByNovelId(novelId).first()
            val nextChapterNumber = (chapters.maxOfOrNull { it.chapterNumber } ?: 0) + 1
            val previousContent = currentChapter.content.takeLast(1500)
            val lorebookEntries = novelRepository.getEnabledLorebookEntries(novelId)

            val result = grokRepository.generateChapter(
                apiKey = apiKey,
                model = preferences.model,
                novel = novel,
                chapterNumber = nextChapterNumber,
                chapterTitle = "第${nextChapterNumber}章",
                previousChapterContent = previousContent,
                lorebookEntries = lorebookEntries
            )

            result.fold(
                onSuccess = { content ->
                    val wordCount = content.filter { it.code > 127 }.length
                    val chapter = Chapter(
                        novelId = novelId,
                        chapterNumber = nextChapterNumber,
                        title = "第${nextChapterNumber}章",
                        content = content,
                        isGenerated = true,
                        status = "generated",
                        generationMode = "new",
                        wordCount = wordCount
                    )
                    novelRepository.insertChapter(chapter)
                    // Update total word count
                    val updatedNovel = novel.copy(
                        totalWordCount = novel.totalWordCount + wordCount,
                        updatedAt = System.currentTimeMillis()
                    )
                    novelRepository.updateNovel(updatedNovel)
                    // Update next chapter id
                    val nextCh = novelRepository.getNextChapter(novelId, currentChapter.chapterNumber)
                    _uiState.update { it.copy(isGeneratingNext = false, nextChapterId = nextCh?.id) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isGeneratingNext = false, error = e.message ?: "生成下一章失败") }
                }
            )
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
