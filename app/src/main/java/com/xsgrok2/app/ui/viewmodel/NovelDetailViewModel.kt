package com.xsgrok2.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.xsgrok2.app.data.model.*
import com.xsgrok2.app.data.preferences.AppPreferences
import com.xsgrok2.app.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NovelDetailUiState(
    val novel: Novel? = null,
    val chapters: List<Chapter> = emptyList(),
    val lorebookEntries: List<LorebookEntry> = emptyList(),
    val characterStates: List<CharacterState> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val generationStage: String = "",
    val error: String? = null,
    val nextChapterNumber: Int = 1,
    val isEditingSettings: Boolean = false,
    val editingField: String = "",
    val editingContent: String = "",
    val lastQualityScore: Float? = null
)

class NovelDetailViewModel(
    private val novelId: Long,
    private val novelRepository: NovelRepository,
    private val grokRepository: GrokRepository,
    private val memoryService: MemoryService,
    private val critiqueService: CritiqueService,
    private val preferences: AppPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(NovelDetailUiState())
    val uiState: StateFlow<NovelDetailUiState> = _uiState.asStateFlow()
    private val gson = Gson()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            novelRepository.getNovelByIdFlow(novelId).collect { novel ->
                _uiState.update { it.copy(novel = novel, isLoading = false) }
            }
        }
        viewModelScope.launch {
            novelRepository.getChaptersByNovelId(novelId).collect { chapters ->
                _uiState.update { it.copy(chapters = chapters, nextChapterNumber = (chapters.maxOfOrNull { c -> c.chapterNumber } ?: 0) + 1) }
            }
        }
        viewModelScope.launch {
            novelRepository.getLorebookEntries(novelId).collect { entries ->
                _uiState.update { it.copy(lorebookEntries = entries) }
            }
        }
        viewModelScope.launch {
            novelRepository.getCharacterStatesByNovelId(novelId).collect { states ->
                _uiState.update { it.copy(characterStates = states) }
            }
        }
    }

    fun generateNextChapter(customTitle: String = "", instruction: ChapterInstruction? = null, userNote: String = "") {
        val chapterNumber = _uiState.value.nextChapterNumber
        generateChapterAt(chapterNumber, customTitle, instruction, userNote, "new")
    }

    fun generateChapterAt(chapterNumber: Int, customTitle: String = "", instruction: ChapterInstruction? = null, userNote: String = "", mode: String = "new") {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) { _uiState.update { it.copy(error = "请先在设置中配置API密钥") }; return }
        val novel = _uiState.value.novel ?: return
        val title = if (customTitle.isNotBlank()) customTitle else "第${chapterNumber}章"

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, generationStage = "准备中...") }
            try {
                val memoryStream = memoryService.buildMemoryStream(novelId, chapterNumber, novelRepository)
                val characterStates = novelRepository.getCharacterStatesByNovelIdSync(novelId)
                val previousContent = novelRepository.getPreviousChapter(novelId, chapterNumber)?.content ?: ""
                val lorebookEntries = novelRepository.getEnabledLorebookEntries(novelId)

                val result = grokRepository.generateChapter(apiKey, preferences.model, novel, chapterNumber, title, previousContent, userNote, instruction, lorebookEntries, memoryStream, characterStates) { stage -> _uiState.update { it.copy(generationStage = stage) } }

                result.fold(
                    onSuccess = { generatedContent ->
                        _uiState.update { it.copy(generationStage = "正在进行质量审查...") }
                        val memoryStreamStr = memoryService.memoryStreamToString(memoryStream)
                        val characterStatesJson = memoryService.characterStatesToJson(characterStates)
                        val critiqueResult = critiqueService.critiqueWithRetry(generatedContent, characterStatesJson, memoryStreamStr, apiKey, preferences.model) { stage -> _uiState.update { it.copy(generationStage = stage) } }
                        val finalContent = critiqueResult.revisedPassage.ifEmpty { generatedContent }

                        val summaryResult = memoryService.generateChapterSummary(Chapter(novelId = novelId, chapterNumber = chapterNumber, title = title, content = finalContent), novel, apiKey, preferences.model)

                        val wordCount = finalContent.filter { it.code > 127 }.length
                        val chapter = Chapter(novelId = novelId, chapterNumber = chapterNumber, title = "第${chapterNumber}章", customTitle = customTitle, content = finalContent, isGenerated = true, status = "generated", generationMode = mode, userNote = userNote, wordCount = wordCount, summary = summaryResult.getOrNull()?.summary ?: "", keyEvents = gson.toJson(summaryResult.getOrNull()?.keyEvents ?: emptyList<String>()), qualityScore = critiqueResult.overallScore.toFloat() / 100f)

                        val savedChapterId = novelRepository.insertChapter(chapter)
                        if (instruction != null) novelRepository.insertInstruction(instruction.copy(chapterId = savedChapterId, novelId = novelId))

                        summaryResult.getOrNull()?.let { summary ->
                            memoryService.updateCharacterStatesFromSummary(novelId, chapterNumber, summary, characterStates, novelRepository)
                        }
                        updateNovelWordCount(novel)
                        _uiState.update { it.copy(isGenerating = false, generationStage = "", lastQualityScore = critiqueResult.overallScore.toFloat() / 100f) }
                    },
                    onFailure = { e -> _uiState.update { it.copy(isGenerating = false, generationStage = "", error = e.message ?: "生成章节失败") } }
                )
            } catch (e: Exception) { _uiState.update { it.copy(isGenerating = false, generationStage = "", error = e.message ?: "生成章节失败") } }
        }
    }

    fun insertChapterAt(position: Int, customTitle: String = "", instruction: ChapterInstruction? = null, userNote: String = "") {
        viewModelScope.launch {
            novelRepository.shiftChaptersDown(novelId, position)
            generateChapterAt(position, customTitle, instruction, userNote, "insert")
        }
    }

    fun regenerateChapter(chapterId: Long, mode: String = "rewrite", instruction: ChapterInstruction? = null, userNote: String = "") {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) { _uiState.update { it.copy(error = "请先在设置中配置API密钥") }; return }
        val novel = _uiState.value.novel ?: return

        viewModelScope.launch {
            val chapter = novelRepository.getChapterById(chapterId) ?: return@launch
            _uiState.update { it.copy(isGenerating = true, error = null, generationStage = "准备中...") }
            try {
                val memoryStream = memoryService.buildMemoryStream(novelId, chapter.chapterNumber, novelRepository)
                val characterStates = novelRepository.getCharacterStatesByNovelIdSync(novelId)
                val previousContent = novelRepository.getPreviousChapter(novelId, chapter.chapterNumber)?.content ?: ""
                val lorebookEntries = novelRepository.getEnabledLorebookEntries(novelId)

                val result = grokRepository.regenerateChapter(apiKey, preferences.model, novel, chapter.chapterNumber, chapter.displayTitle(), previousContent, chapter.content, mode, instruction, userNote, lorebookEntries, memoryStream, characterStates) { stage -> _uiState.update { it.copy(generationStage = stage) } }

                result.fold(
                    onSuccess = { content ->
                        _uiState.update { it.copy(generationStage = "正在进行质量审查...") }
                        val memoryStreamStr = memoryService.memoryStreamToString(memoryStream)
                        val characterStatesJson = memoryService.characterStatesToJson(characterStates)
                        val critiqueResult = critiqueService.critiqueWithRetry(content, characterStatesJson, memoryStreamStr, apiKey, preferences.model) { stage -> _uiState.update { it.copy(generationStage = stage) } }
                        val finalContent = critiqueResult.revisedPassage.ifEmpty { content }

                        val wordCount = finalContent.filter { it.code > 127 }.length
                        novelRepository.updateChapter(chapter.copy(content = finalContent, wordCount = wordCount, status = if (mode == "improve") "edited" else "generated", generationMode = mode, userNote = userNote, qualityScore = critiqueResult.overallScore.toFloat() / 100f))
                        if (instruction != null) {
                            val existing = novelRepository.getInstructionByChapterId(chapterId)
                            if (existing != null) novelRepository.updateInstruction(instruction.copy(id = existing.id, chapterId = chapterId, novelId = novelId))
                            else novelRepository.insertInstruction(instruction.copy(chapterId = chapterId, novelId = novelId))
                        }
                        updateNovelWordCount(novel)
                        _uiState.update { it.copy(isGenerating = false, generationStage = "", lastQualityScore = critiqueResult.overallScore.toFloat() / 100f) }
                    },
                    onFailure = { e -> _uiState.update { it.copy(isGenerating = false, generationStage = "", error = e.message ?: "重新生成失败") } }
                )
            } catch (e: Exception) { _uiState.update { it.copy(isGenerating = false, generationStage = "", error = e.message ?: "重新生成失败") } }
        }
    }

    fun deleteChapter(chapterId: Long) {
        viewModelScope.launch {
            val chapter = novelRepository.getChapterById(chapterId) ?: return@launch
            novelRepository.deleteChapterById(chapterId)
            novelRepository.shiftChaptersUp(novelId, chapter.chapterNumber)
            novelRepository.deleteInstructionByChapterId(chapterId)
            _uiState.value.novel?.let { updateNovelWordCount(it) }
        }
    }

    fun updateChapterTitle(chapterId: Long, customTitle: String) {
        viewModelScope.launch {
            val chapter = novelRepository.getChapterById(chapterId) ?: return@launch
            novelRepository.updateChapter(chapter.copy(customTitle = customTitle))
        }
    }

    fun updateChapterContent(chapterId: Long, newContent: String) {
        viewModelScope.launch {
            val chapter = novelRepository.getChapterById(chapterId) ?: return@launch
            val wordCount = newContent.filter { it.code > 127 }.length
            novelRepository.updateChapter(chapter.copy(content = newContent, wordCount = wordCount, status = "edited"))
            _uiState.value.novel?.let { updateNovelWordCount(it) }
        }
    }

    fun rewriteSelection(chapterId: Long, selectedText: String, instruction: String, onResult: (String) -> Unit) {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) { _uiState.update { it.copy(error = "请先在设置中配置API密钥") }; return }
        val novel = _uiState.value.novel ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, generationStage = "正在改写...") }
            val result = grokRepository.rewriteSelection(apiKey, preferences.model, novel, selectedText, instruction)
            result.fold(
                onSuccess = { rewritten -> onResult(rewritten); _uiState.update { it.copy(isGenerating = false, generationStage = "") } },
                onFailure = { e -> _uiState.update { it.copy(isGenerating = false, generationStage = "", error = e.message ?: "改写失败") } }
            )
        }
    }

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

    fun regenerateSettings() {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) { _uiState.update { it.copy(error = "请先在设置中配置API密钥") }; return }
        val novel = _uiState.value.novel ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, generationStage = "正在重新生成设定...") }
            val result = grokRepository.generateNovelSettings(apiKey, preferences.model, novel.genre, novel.description)
            result.fold(
                onSuccess = { settings ->
                    val worldSetting = extractSection(settings, listOf("一、", "世界设定", "时代背景", "背景与环境"))
                    val keyCharacters = extractSection(settings, listOf("二、", "核心角色", "角色"))
                    val outline = extractSection(settings, listOf("三、", "故事大纲", "大纲"))
                    novelRepository.updateNovel(novel.copy(worldSetting = worldSetting, keyCharacters = keyCharacters, outline = outline, lastSettingVersion = novel.lastSettingVersion + 1, updatedAt = System.currentTimeMillis()))
                    _uiState.update { it.copy(isGenerating = false, generationStage = "") }
                },
                onFailure = { e -> _uiState.update { it.copy(isGenerating = false, generationStage = "", error = e.message ?: "重新生成设定失败") } }
            )
        }
    }

    fun addLorebookEntry(keyword: String, content: String, importance: Int = 3) {
        viewModelScope.launch { novelRepository.insertLorebookEntry(LorebookEntry(novelId = novelId, keyword = keyword, content = content, importance = importance)) }
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
            novelRepository.deleteCharacterStatesByNovelId(novelId)
            novelRepository.deleteNovel(novel)
        }
    }

    fun initializeCharacterStates() {
        viewModelScope.launch {
            val novel = _uiState.value.novel ?: return@launch
            memoryService.initializeCharacterStates(novel, novelRepository)
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

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
                if (sectionHeaders.any { header -> trimmed.contains(header) }) { inSection = true; continue }
            } else if (inSection) result.appendLine(line)
        }
        return result.toString().trim()
    }

    class Factory(private val novelId: Long, private val novelRepository: NovelRepository, private val grokRepository: GrokRepository, private val memoryService: MemoryService, private val critiqueService: CritiqueService, private val preferences: AppPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = NovelDetailViewModel(novelId, novelRepository, grokRepository, memoryService, critiqueService, preferences) as T
    }
}
