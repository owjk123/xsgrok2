package com.xsgrok2.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.ChapterInstruction
import com.xsgrok2.app.data.model.LorebookEntry
import com.xsgrok2.app.data.model.Novel
import com.xsgrok2.app.data.preferences.AppPreferences
import com.xsgrok2.app.data.repository.GrokRepository
import com.xsgrok2.app.data.repository.NovelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NovelDetailUiState(
    val novel: Novel? = null,
    val chapters: List<Chapter> = emptyList(),
    val lorebookEntries: List<LorebookEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val nextChapterNumber: Int = 1,
    val isEditingSettings: Boolean = false,
    val editingField: String = "",
    val editingContent: String = ""
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
                        nextChapterNumber = (chapters.maxOfOrNull { c -> c.chapterNumber } ?: 0) + 1
                    )
                }
            }
        }
        viewModelScope.launch {
            novelRepository.getLorebookEntries(novelId).collect { entries ->
                _uiState.update { it.copy(lorebookEntries = entries) }
            }
        }
    }

    // === Generate new chapter at next position ===
    fun generateNextChapter(
        customTitle: String = "",
        instruction: ChapterInstruction? = null,
        userNote: String = ""
    ) {
        val chapterNumber = _uiState.value.nextChapterNumber
        generateChapterAt(chapterNumber, customTitle, instruction, userNote, "new")
    }

    // === Generate chapter at specific position (insert) ===
    fun generateChapterAt(
        chapterNumber: Int,
        customTitle: String = "",
        instruction: ChapterInstruction? = null,
        userNote: String = "",
        mode: String = "new"
    ) {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "请先在设置中配置API密钥") }
            return
        }
        val novel = _uiState.value.novel ?: return
        val title = if (customTitle.isNotBlank()) customTitle else "第${chapterNumber}章"

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            val previousContent = _uiState.value.chapters
                .filter { it.chapterNumber < chapterNumber }
                .maxByOrNull { it.chapterNumber }
                ?.content?.takeLast(1500) ?: ""

            val lorebookEntries = novelRepository.getEnabledLorebookEntries(novelId)

            val result = grokRepository.generateChapter(
                apiKey = apiKey,
                model = preferences.model,
                novel = novel,
                chapterNumber = chapterNumber,
                chapterTitle = title,
                previousChapterContent = previousContent,
                userNote = userNote,
                instruction = instruction,
                lorebookEntries = lorebookEntries
            )
            result.fold(
                onSuccess = { generatedContent ->
                    val wordCount = generatedContent.filter { it.code > 127 }.length
                    val chapter = Chapter(
                        novelId = novelId,
                        chapterNumber = chapterNumber,
                        title = "第${chapterNumber}章",
                        customTitle = customTitle,
                        content = generatedContent,
                        isGenerated = true,
                        status = "generated",
                        generationMode = mode,
                        userNote = userNote,
                        wordCount = wordCount
                    )
                    val savedChapterId = novelRepository.insertChapter(chapter)
                    // Save instruction if provided - use the returned ID directly
                    if (instruction != null) {
                        novelRepository.insertInstruction(instruction.copy(chapterId = savedChapterId, novelId = novelId))
                    }
                    updateNovelWordCount(novel)
                    _uiState.update { it.copy(isGenerating = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isGenerating = false, error = e.message ?: "生成章节失败")
                    }
                }
            )
        }
    }

    // === Insert chapter at position (shift existing chapters down) ===
    fun insertChapterAt(
        position: Int,
        customTitle: String = "",
        instruction: ChapterInstruction? = null,
        userNote: String = ""
    ) {
        viewModelScope.launch {
            novelRepository.shiftChaptersDown(novelId, position)
            generateChapterAt(position, customTitle, instruction, userNote, "insert")
        }
    }

    // === Regenerate existing chapter ===
    fun regenerateChapter(
        chapterId: Long,
        mode: String = "rewrite",
        instruction: ChapterInstruction? = null,
        userNote: String = ""
    ) {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "请先在设置中配置API密钥") }
            return
        }
        val novel = _uiState.value.novel ?: return

        viewModelScope.launch {
            val chapter = novelRepository.getChapterById(chapterId) ?: return@launch
            _uiState.update { it.copy(isGenerating = true, error = null) }

            val previousContent = novelRepository.getPreviousChapter(novelId, chapter.chapterNumber)
                ?.content?.takeLast(1500) ?: ""

            val lorebookEntries = novelRepository.getEnabledLorebookEntries(novelId)

            val result = grokRepository.regenerateChapter(
                apiKey = apiKey,
                model = preferences.model,
                novel = novel,
                chapterNumber = chapter.chapterNumber,
                chapterTitle = chapter.displayTitle(),
                previousChapterContent = previousContent,
                currentContent = chapter.content,
                mode = mode,
                instruction = instruction,
                userNote = userNote,
                lorebookEntries = lorebookEntries
            )
            result.fold(
                onSuccess = { content ->
                    val wordCount = content.filter { it.code > 127 }.length
                    novelRepository.updateChapter(chapter.copy(
                        content = content,
                        wordCount = wordCount,
                        status = if (mode == "improve") "edited" else "generated",
                        generationMode = mode,
                        userNote = userNote
                    ))
                    if (instruction != null) {
                        val existing = novelRepository.getInstructionByChapterId(chapterId)
                        if (existing != null) {
                            novelRepository.updateInstruction(instruction.copy(id = existing.id, chapterId = chapterId, novelId = novelId))
                        } else {
                            novelRepository.insertInstruction(instruction.copy(chapterId = chapterId, novelId = novelId))
                        }
                    }
                    updateNovelWordCount(novel)
                    _uiState.update { it.copy(isGenerating = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isGenerating = false, error = e.message ?: "重新生成失败")
                    }
                }
            )
        }
    }

    // === Delete single chapter ===
    fun deleteChapter(chapterId: Long) {
        viewModelScope.launch {
            val chapter = novelRepository.getChapterById(chapterId) ?: return@launch
            novelRepository.deleteChapterById(chapterId)
            novelRepository.shiftChaptersUp(novelId, chapter.chapterNumber)
            novelRepository.deleteInstructionByChapterId(chapterId)
            _uiState.value.novel?.let { updateNovelWordCount(it) }
        }
    }

    // === Update chapter title ===
    fun updateChapterTitle(chapterId: Long, customTitle: String) {
        viewModelScope.launch {
            val chapter = novelRepository.getChapterById(chapterId) ?: return@launch
            novelRepository.updateChapter(chapter.copy(customTitle = customTitle))
        }
    }

    // === Update chapter content (manual edit) ===
    fun updateChapterContent(chapterId: Long, newContent: String) {
        viewModelScope.launch {
            val chapter = novelRepository.getChapterById(chapterId) ?: return@launch
            val wordCount = newContent.filter { it.code > 127 }.length
            novelRepository.updateChapter(chapter.copy(
                content = newContent,
                wordCount = wordCount,
                status = "edited"
            ))
            _uiState.value.novel?.let { updateNovelWordCount(it) }
        }
    }

    // === AI rewrite selected text ===
    fun rewriteSelection(chapterId: Long, selectedText: String, instruction: String, onResult: (String) -> Unit) {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "请先在设置中配置API密钥") }
            return
        }
        val novel = _uiState.value.novel ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            val result = grokRepository.rewriteSelection(apiKey, preferences.model, novel, selectedText, instruction)
            result.fold(
                onSuccess = { rewritten ->
                    onResult(rewritten)
                    _uiState.update { it.copy(isGenerating = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isGenerating = false, error = e.message ?: "改写失败") }
                }
            )
        }
    }

    // === Edit novel settings ===
    fun updateNovelSetting(field: String, content: String) {
        viewModelScope.launch {
            val novel = _uiState.value.novel ?: return@launch
            val updated = when (field) {
                "worldSetting" -> novel.copy(worldSetting = content, lastSettingVersion = novel.lastSettingVersion + 1)
                "keyCharacters" -> novel.copy(keyCharacters = content, lastSettingVersion = novel.lastSettingVersion + 1)
                "outline" -> novel.copy(outline = content, lastSettingVersion = novel.lastSettingVersion + 1)
                "description" -> novel.copy(description = content)
                "title" -> novel.copy(title = content)
                else -> novel
            }
            novelRepository.updateNovel(updated.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    // === Regenerate settings ===
    fun regenerateSettings() {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "请先在设置中配置API密钥") }
            return
        }
        val novel = _uiState.value.novel ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            val result = grokRepository.generateNovelSettings(
                apiKey = apiKey,
                model = preferences.model,
                genre = novel.genre,
                description = novel.description
            )
            result.fold(
                onSuccess = { settings ->
                    val worldSetting = extractSection(settings, listOf("一、", "世界设定", "时代背景", "背景与环境"))
                    val keyCharacters = extractSection(settings, listOf("二、", "核心角色", "角色"))
                    val outline = extractSection(settings, listOf("三、", "故事大纲", "大纲"))
                    novelRepository.updateNovel(novel.copy(
                        worldSetting = worldSetting,
                        keyCharacters = keyCharacters,
                        outline = outline,
                        lastSettingVersion = novel.lastSettingVersion + 1,
                        updatedAt = System.currentTimeMillis()
                    ))
                    _uiState.update { it.copy(isGenerating = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isGenerating = false, error = e.message ?: "重新生成设定失败") }
                }
            )
        }
    }

    // === Lorebook CRUD ===
    fun addLorebookEntry(keyword: String, content: String, importance: Int = 3) {
        viewModelScope.launch {
            novelRepository.insertLorebookEntry(LorebookEntry(
                novelId = novelId, keyword = keyword, content = content, importance = importance
            ))
        }
    }

    fun updateLorebookEntry(entry: LorebookEntry) {
        viewModelScope.launch { novelRepository.updateLorebookEntry(entry) }
    }

    fun deleteLorebookEntry(entry: LorebookEntry) {
        viewModelScope.launch { novelRepository.deleteLorebookEntry(entry) }
    }

    fun deleteNovel() {
        viewModelScope.launch {
            val novel = _uiState.value.novel ?: return@launch
            novelRepository.deleteChaptersByNovelId(novelId)
            novelRepository.deleteLorebookEntriesByNovelId(novelId)
            novelRepository.deleteNovel(novel)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun updateNovelWordCount(novel: Novel) {
        val chapters = novelRepository.getChaptersByNovelId(novelId).first()
        val totalWords = chapters.sumOf { it.wordCount }
        novelRepository.updateNovel(novel.copy(totalWordCount = totalWords, updatedAt = System.currentTimeMillis()))
    }

    private fun extractSection(text: String, sectionHeaders: List<String>): String {
        val lines = text.lines()
        val result = StringBuilder()
        var inSection = false

        for (line in lines) {
            val trimmed = line.trim()
            val hashCount = trimmed.takeWhile { it == '#' }.length

            val isTopLevelHeader = when {
                hashCount in 1..2 -> true
                trimmed.matches(Regex("^[一二三四五六七八九十]+[、．.].*")) -> true
                trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4 -> true
                else -> false
            }

            if (isTopLevelHeader) {
                if (inSection) break
                if (sectionHeaders.any { header -> trimmed.contains(header) }) {
                    inSection = true
                    continue
                }
            } else if (inSection) {
                result.appendLine(line)
            }
        }
        return result.toString().trim()
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
