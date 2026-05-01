package com.xsgrok2.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xsgrok2.app.data.model.Novel
import com.xsgrok2.app.data.preferences.AppPreferences
import com.xsgrok2.app.data.repository.GrokRepository
import com.xsgrok2.app.data.repository.NovelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CreateStep { INPUT, GENERATING, REVIEW, SAVING }

data class CreateNovelUiState(
    val genre: String = "",
    val description: String = "",
    val writingStyle: String = "细腻生动",
    val generatedSettings: String = "",
    val targetWordCount: Int = 3000,
    val temperature: Float = 0.85f,
    val isLoading: Boolean = false,
    val step: CreateStep = CreateStep.INPUT,
    val error: String? = null,
    val createdNovelId: Long? = null
)

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

    fun updateWritingStyle(style: String) {
        _uiState.update { it.copy(writingStyle = style) }
    }

    fun updateGeneratedSettings(settings: String) {
        _uiState.update { it.copy(generatedSettings = settings) }
    }

    fun updateTargetWordCount(count: Int) {
        _uiState.update { it.copy(targetWordCount = count) }
    }

    fun updateTemperature(temp: Float) {
        _uiState.update { it.copy(temperature = temp) }
    }

    fun generateSettings() {
        val apiKey = preferences.apiKey
        if (apiKey.isEmpty()) {
            _uiState.update { it.copy(error = "请先在设置中配置API密钥") }
            return
        }
        val genre = _uiState.value.genre.trim()
        val description = _uiState.value.description.trim()
        if (genre.isEmpty() || description.isEmpty()) {
            _uiState.update { it.copy(error = "请填写小说类型和核心构思") }
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
                            error = e.message ?: "生成设定失败"
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
                writingStyle = state.writingStyle,
                worldSetting = extractSection(state.generatedSettings, listOf("一、", "世界设定", "时代背景", "背景与环境")),
                keyCharacters = extractSection(state.generatedSettings, listOf("二、", "核心角色", "角色")),
                outline = extractSection(state.generatedSettings, listOf("三、", "故事大纲", "大纲")),
                model = preferences.model,
                targetWordCount = state.targetWordCount,
                temperature = state.temperature
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
            if (trimmed.startsWith("《") && trimmed.contains("》")) {
                return trimmed.substringAfter("《").substringBefore("》").trim()
            }
            if (trimmed.startsWith("#")) {
                val title = trimmed.removePrefix("#").removePrefix("#").trim()
                if (title.isNotEmpty() && title.length < 30 && !title.contains("设定") && !title.contains("角色") && !title.contains("大纲")) {
                    return title
                }
            }
        }
        return "${genre}小说"
    }

    private fun extractSection(text: String, sectionHeaders: List<String>): String {
        val lines = text.lines()
        val result = StringBuilder()
        var inSection = false
        var headerDepth = 0  // Track markdown header depth

        for (line in lines) {
            val trimmed = line.trim()
            
            // Only detect TOP-LEVEL section headers (the 三大板块分割线)
            // Top-level: starts with ### or fewer #, or matches 一二三、 pattern, or **bold** title
            // Sub-level: starts with #### or more #, or "- " list items, or "第X章" patterns
            val hashCount = trimmed.takeWhile { it == '#' }.length
            
            val isTopLevelHeader = when {
                // Markdown headers: only ## or fewer (### and #### are sub-headers within a section)
                hashCount in 1..2 -> true
                // Chinese numbered headers: 一、二、三、etc (top-level section markers)
                trimmed.matches(Regex("^[一二三四五六七八九十]+[、．.].*")) -> true
                // Bold text as section header
                trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4 -> true
                else -> false
            }

            if (isTopLevelHeader) {
                if (inSection) break  // End of current section
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
