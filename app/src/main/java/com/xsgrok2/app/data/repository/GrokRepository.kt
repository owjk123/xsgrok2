package com.xsgrok2.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xsgrok2.app.data.api.GrokApiService
import com.xsgrok2.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GrokRepository(private val apiService: GrokApiService) {
    private val gson = Gson()

    suspend fun generateNovelSettings(apiKey: String, model: String, genre: String, description: String): Result<String> {
        val systemPrompt = PromptTemplates.getNovelSettingsSystemPrompt(genre)
        val userPrompt = "我想写一部${genre}类型的小说，核心构思是：$description"
        return callApi(apiKey, model, systemPrompt, userPrompt, 0.8, 4096)
    }

    suspend fun generateChapter(
        apiKey: String, model: String, novel: Novel, chapterNumber: Int, chapterTitle: String,
        previousChapterContent: String = "", userNote: String = "", instruction: ChapterInstruction? = null,
        lorebookEntries: List<LorebookEntry> = emptyList(), memoryStream: MemoryStream? = null,
        characterStates: List<CharacterState> = emptyList(),
        onStageUpdate: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val instructionBlock = buildInstructionBlock(instruction, userNote)
            val lorebookContext = buildLorebookContext(lorebookEntries)
            val memoryStreamStr = memoryStream?.let { buildDefaultMemoryStream(previousChapterContent) } ?: buildDefaultMemoryStream(previousChapterContent)
            val characterStatesJson = gson.toJson(characterStates)
            
            onStageUpdate?.invoke("正在规划角色状态变化...")
            val statePlanResult = generateStatePlan(apiKey, model, novel.title, novel.genre, memoryStreamStr, characterStatesJson, instructionBlock)
            if (statePlanResult.isFailure) return@withContext Result.failure(statePlanResult.exceptionOrNull() ?: Exception("State Planner失败"))
            val statePlanJson = statePlanResult.getOrNull() ?: "{}"
            
            val outlineSection = extractChapterOutline(novel.outline, chapterNumber)
            val addressMap = extractAddressMap(characterStates)
            val characterStateSummary = buildCharacterStateSummary(characterStates)
            
            onStageUpdate?.invoke("正在撰写章节正文...")
            val writerResult = generateChapterWithPlan(apiKey, model, memoryStreamStr, characterStatesJson, outlineSection, lorebookContext, chapterNumber, chapterTitle, statePlanJson, instructionBlock, instruction?.wordCountTarget ?: novel.targetWordCount, addressMap, characterStateSummary)
            
            onStageUpdate?.invoke("章节生成完成")
            writerResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateStatePlan(apiKey: String, model: String, title: String, genre: String, memoryStream: String, characterStatesJson: String, instruction: String): Result<String> {
        val systemPrompt = PromptTemplates.getStatePlannerSystemPrompt()
        val userPrompt = PromptTemplates.getStatePlannerUserPrompt(title, genre, memoryStream, characterStatesJson, instruction)
        val response = callApi(apiKey, model, systemPrompt, userPrompt, 0.7, 2048)
        return response.map { extractJson(it) }
    }

    private suspend fun generateChapterWithPlan(apiKey: String, model: String, memoryStream: String, characterStatesJson: String, outlineSection: String, lorebookContext: String, chapterNumber: Int, chapterTitle: String, statePlanJson: String, instruction: String, wordCountTarget: Int, addressMap: Map<String, String>, characterStateSummary: String): Result<String> {
        val systemPrompt = PromptTemplates.getChapterWriterSystemPrompt(addressMap, characterStateSummary)
        val userPrompt = PromptTemplates.getChapterWriterUserPrompt(memoryStream, characterStatesJson, outlineSection, lorebookContext, chapterNumber, chapterTitle, statePlanJson, instruction, wordCountTarget)
        return callApi(apiKey, model, systemPrompt, userPrompt, 0.85, 8192)
    }

    suspend fun regenerateChapter(
        apiKey: String, model: String, novel: Novel, chapterNumber: Int, chapterTitle: String,
        previousChapterContent: String = "", currentContent: String = "", mode: String = "rewrite",
        instruction: ChapterInstruction? = null, userNote: String = "", lorebookEntries: List<LorebookEntry> = emptyList(),
        memoryStream: MemoryStream? = null, characterStates: List<CharacterState> = emptyList(),
        onStageUpdate: ((String) -> Unit)? = null
    ): Result<String> {
        return generateChapter(apiKey, model, novel, chapterNumber, chapterTitle, previousChapterContent, userNote, instruction, lorebookEntries, memoryStream, characterStates, onStageUpdate)
    }

    suspend fun rewriteSelection(apiKey: String, model: String, novel: Novel, selectedText: String, instruction: String): Result<String> {
        val systemPrompt = PromptTemplates.getRewriteSystemPrompt()
        val userPrompt = PromptTemplates.getRewriteUserPrompt(novel.genre, novel.writingStyle, selectedText, instruction)
        return callApi(apiKey, model, systemPrompt, userPrompt, 0.7, 2048)
    }

    private fun buildInstructionBlock(instruction: ChapterInstruction?, userNote: String): String {
        return if (instruction != null) {
            buildString {
                if (instruction.coreEvent.isNotBlank()) appendLine("- 核心事件：${instruction.coreEvent}")
                if (instruction.characterChanges.isNotBlank()) appendLine("- 人物变化：${instruction.characterChanges}")
                if (instruction.mood.isNotBlank()) appendLine("- 情绪氛围：${instruction.mood}")
                if (instruction.foreshadowing.isNotBlank()) appendLine("- 伏笔回收：${instruction.foreshadowing}")
                if (instruction.newThreads.isNotBlank()) appendLine("- 新线埋设：${instruction.newThreads}")
                if (instruction.forbiddenElements.isNotBlank()) appendLine("- 禁止出现：${instruction.forbiddenElements}")
                if (instruction.wordCountTarget > 0) appendLine("- 目标字数：${instruction.wordCountTarget}字")
            }
        } else if (userNote.isNotBlank()) "必须在剧情中落实此要求：$userNote"
        else "（无特殊创作指令）"
    }

    private fun buildLorebookContext(lorebookEntries: List<LorebookEntry>): String = if (lorebookEntries.isNotEmpty()) "\n【世界词条】\n" + lorebookEntries.joinToString("\n") { "- ${it.keyword}：${it.content}" } else ""

    private fun buildDefaultMemoryStream(previousChapterContent: String): String = if (previousChapterContent.isNotEmpty()) "【前一章结尾片段】\n${previousChapterContent.takeLast(1500)}" else "（这是故事的开篇，无前文记忆）"

    private fun extractChapterOutline(outline: String, chapterNumber: Int): String {
        val lines = outline.lines()
        val sb = StringBuilder()
        var collecting = false
        for (line in lines) {
            val chapterMatch = Regex("第(\\d+)章").find(line)
            if (chapterMatch != null) {
                val num = chapterMatch.groupValues[1].toIntOrNull() ?: 0
                if (num == chapterNumber) { collecting = true; sb.appendLine(line) }
                else if (collecting) break
            } else if (collecting) sb.appendLine(line)
        }
        return if (sb.isNotEmpty()) sb.toString() else outline.take(500)
    }

    private fun extractAddressMap(characterStates: List<CharacterState>): Map<String, String> = emptyMap()

    private fun buildCharacterStateSummary(characterStates: List<CharacterState>): String {
        if (characterStates.isEmpty()) return "（暂无角色状态记录）"
        val sb = StringBuilder()
        characterStates.forEach { state ->
            sb.appendLine("【${state.characterName}】")
            sb.appendLine("- 当前情绪：${state.currentMood}")
            sb.appendLine("- 当前位置：${state.currentLocation}")
        }
        return sb.toString()
    }

    private fun extractJson(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```json")) cleaned = cleaned.substringAfter("```json").trim()
        else if (cleaned.startsWith("```")) cleaned = cleaned.substringAfter("```").trim()
        if (cleaned.endsWith("```")) cleaned = cleaned.substringBeforeLast("```").trim()
        return cleaned
    }

    private suspend fun callApi(apiKey: String, model: String, systemPrompt: String, userPrompt: String, temperature: Double = 0.8, maxTokens: Int = 4096): Result<String> {
        return try {
            val request = ChatRequest(model = model, messages = listOf(Message(role = "system", content = systemPrompt), Message(role = "user", content = userPrompt)), temperature = temperature, max_tokens = maxTokens)
            val response = apiService.chatCompletion("Bearer $apiKey", request)
            if (response.error != null) Result.failure(Exception(response.error.message ?: "API错误"))
            else if (response.choices.isNullOrEmpty()) Result.failure(Exception("API返回为空"))
            else Result.success(response.choices[0].message?.content ?: "")
        } catch (e: Exception) { Result.failure(e) }
    }
}
