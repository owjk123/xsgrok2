package com.xsgrok2.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xsgrok2.app.data.api.GrokApiService
import com.xsgrok2.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemoryService(private val apiService: GrokApiService) {
    private val gson = Gson()
    
    suspend fun generateChapterSummary(
        chapter: Chapter,
        novel: Novel,
        apiKey: String,
        model: String
    ): Result<ChapterSummary> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = PromptTemplates.getSummaryGeneratorSystemPrompt()
            val userPrompt = PromptTemplates.getSummaryGeneratorUserPrompt(
                title = novel.title,
                genre = novel.genre,
                chapterNumber = chapter.chapterNumber,
                chapterContent = chapter.content
            )
            val response = callApi(apiKey, model, systemPrompt, userPrompt, 0.5)
            response.map { jsonText ->
                val cleanJson = extractJson(jsonText)
                gson.fromJson(cleanJson, ChapterSummary::class.java)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun buildMemoryStream(
        novelId: Long,
        currentChapterNumber: Int,
        novelRepository: NovelRepository
    ): MemoryStream = withContext(Dispatchers.IO) {
        val latestChapter = novelRepository.getLatestChapter(novelId)
        val recentChapterContent = latestChapter?.content?.takeLast(1800) ?: ""
        val characterStates = novelRepository.getCharacterStatesByNovelIdSync(novelId)
        val pendingForeshadowing = getPendingForeshadowing(novelRepository, novelId, currentChapterNumber)
        
        MemoryStream(
            recentChapterContent = recentChapterContent,
            recentSummaries = emptyList(),
            characterStates = characterStates,
            pendingForeshadowing = pendingForeshadowing
        )
    }
    
    fun memoryStreamToString(memoryStream: MemoryStream): String {
        val sb = StringBuilder()
        if (memoryStream.recentChapterContent.isNotEmpty()) {
            sb.appendLine("【最新章节内容（末尾片段）】")
            sb.appendLine(memoryStream.recentChapterContent)
            sb.appendLine()
        }
        if (memoryStream.pendingForeshadowing.isNotEmpty()) {
            sb.appendLine("【待回收伏笔】")
            memoryStream.pendingForeshadowing.forEach { shadow ->
                sb.appendLine("- $shadow")
            }
            sb.appendLine()
        }
        return sb.toString()
    }
    
    fun characterStatesToJson(characterStates: List<CharacterState>): String = gson.toJson(characterStates)
    
    fun characterStatesToSummary(characterStates: List<CharacterState>): String {
        if (characterStates.isEmpty()) return "（暂无角色状态记录）"
        val sb = StringBuilder()
        characterStates.forEach { state ->
            sb.appendLine("【${state.characterName}】")
            sb.appendLine("- 当前情绪：${state.currentMood}")
            sb.appendLine("- 当前位置：${state.currentLocation}")
            sb.appendLine()
        }
        return sb.toString()
    }
    
    suspend fun initializeCharacterStates(novel: Novel, novelRepository: NovelRepository) = withContext(Dispatchers.IO) {
        val characterNames = extractCharacterNames(novel.keyCharacters)
        characterNames.forEach { name ->
            val initialState = CharacterState(
                novelId = novel.id,
                characterName = name,
                currentMood = "平静",
                knownInformation = "[]",
                relationships = "{}",
                currentLocation = "未知",
                forbiddenActions = "[]",
                arcProgress = "角色弧线未开始",
                chapterNumber = 0
            )
            novelRepository.insertCharacterState(initialState)
        }
    }
    
    private fun extractCharacterNames(keyCharacters: String): List<String> {
        val names = mutableListOf<String>()
        val patterns = listOf(
            Regex("姓名[：:][\\s]*(.{2,4})[\\s,，]"),
            Regex("^([\\u4e00-\\u9fa5]{2,4})[（(]"),
            Regex("名叫[\\s]*([\\u4e00-\\u9fa5]{2,4})")
        )
        patterns.forEach { pattern ->
            pattern.findAll(keyCharacters).forEach { match ->
                val name = match.groupValues[1].trim()
                if (name.length in 2..4 && !names.contains(name)) names.add(name)
            }
        }
        return names
    }
    
    suspend fun updateCharacterStatesFromSummary(
        novelId: Long,
        chapterNumber: Int,
        summary: ChapterSummary,
        existingStates: List<CharacterState>,
        novelRepository: NovelRepository
    ) = withContext(Dispatchers.IO) {
        summary.characterDeltas.forEach { (charName, delta) ->
            val existingState = existingStates.find { it.characterName == charName }
            if (existingState != null) {
                val updatedKnownInfo = updateKnownInformation(existingState.knownInformation, delta.newKnowledge)
                val updatedMood = delta.moodChange.ifEmpty { existingState.currentMood }
                novelRepository.updateCharacterState(
                    existingState.copy(
                        chapterNumber = chapterNumber,
                        currentMood = updatedMood,
                        knownInformation = updatedKnownInfo,
                        arcProgress = "进度更新：${delta.moodChange}"
                    )
                )
            }
        }
    }
    
    private fun updateKnownInformation(existingJson: String, newKnowledge: List<String>): String {
        val existingList = try {
            parseStringList(existingJson).toMutableList()
        } catch (e: Exception) { mutableListOf() }
        existingList.addAll(newKnowledge)
        return gson.toJson(existingList.takeLast(20))
    }
    
    private suspend fun getPendingForeshadowing(
        novelRepository: NovelRepository,
        novelId: Long,
        currentChapter: Int
    ): List<String> = withContext(Dispatchers.IO) {
        val foreshadowingList = mutableListOf<String>()
        for (i in 1..3) {
            val chapterNum = currentChapter - i
            if (chapterNum > 0) {
                val chapter = novelRepository.getChapterByNumber(novelId, chapterNum)
                chapter?.let {
                    val instruction = novelRepository.getInstructionByChapterId(it.id)
                    if (instruction?.newThreads?.isNotEmpty() == true) {
                        foreshadowingList.addAll(instruction.newThreads.split("\n").filter { line -> line.trim().isNotEmpty() })
                    }
                }
            }
        }
        foreshadowingList.take(10)
    }
    
    private fun parseStringList(json: String): List<String> = try {
        if (json.isBlank() || json == "[]") emptyList()
        else gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
    } catch (e: Exception) { emptyList() }
    
    private fun extractJson(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```json")) cleaned = cleaned.substringAfter("```json").trim()
        else if (cleaned.startsWith("```")) cleaned = cleaned.substringAfter("```").trim()
        if (cleaned.endsWith("```")) cleaned = cleaned.substringBeforeLast("```").trim()
        return cleaned
    }
    
    private suspend fun callApi(apiKey: String, model: String, systemPrompt: String, userPrompt: String, temperature: Double): Result<String> {
        return try {
            val request = com.xsgrok2.app.data.model.ChatRequest(
                model = model,
                messages = listOf(
                    com.xsgrok2.app.data.model.Message(role = "system", content = systemPrompt),
                    com.xsgrok2.app.data.model.Message(role = "user", content = userPrompt)
                ),
                temperature = temperature,
                max_tokens = 4096
            )
            val response = apiService.chatCompletion("Bearer $apiKey", request)
            if (response.error != null) Result.failure(Exception(response.error.message ?: "API错误"))
            else if (response.choices.isNullOrEmpty()) Result.failure(Exception("API返回为空"))
            else Result.success(response.choices[0].message?.content ?: "")
        } catch (e: Exception) { Result.failure(e) }
    }
}
