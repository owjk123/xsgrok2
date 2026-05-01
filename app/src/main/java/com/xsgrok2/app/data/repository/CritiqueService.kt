package com.xsgrok2.app.data.repository

import com.google.gson.Gson
import com.xsgrok2.app.data.api.GrokApiService
import com.xsgrok2.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CritiqueService(private val apiService: GrokApiService) {
    private val gson = Gson()
    
    companion object {
        const val MAX_RETRIES = 2
        const val PASS_THRESHOLD = 80
    }
    
    suspend fun critiqueChapter(
        content: String,
        characterStates: String,
        memoryStream: String,
        apiKey: String,
        model: String
    ): Result<CritiqueResult> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = PromptTemplates.getCritiqueSystemPrompt()
            val userPrompt = PromptTemplates.getCritiqueUserPrompt(content, characterStates, memoryStream)
            val response = callApi(apiKey, model, systemPrompt, userPrompt, 0.6)
            response.map { jsonText ->
                val cleanJson = extractJson(jsonText)
                gson.fromJson(cleanJson, CritiqueResult::class.java)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun critiqueWithRetry(
        content: String,
        characterStates: String,
        memoryStream: String,
        apiKey: String,
        model: String,
        onProgressUpdate: ((String) -> Unit)? = null
    ): CritiqueResult = withContext(Dispatchers.IO) {
        var currentContent = content
        var currentRetry = 0
        var lastResult: CritiqueResult? = null
        
        while (currentRetry < MAX_RETRIES) {
            onProgressUpdate?.invoke("正在进行第${currentRetry + 1}轮质量审查...")
            val result = critiqueChapter(currentContent, characterStates, memoryStream, apiKey, model).getOrNull()
            
            if (result == null) {
                return@withContext CritiqueResult(
                    novelty = 5, consistency = 5, addressing = 5, pacing = 5, overallScore = 50,
                    issues = listOf(QualityIssue("logic", "未知", "质量审查API调用失败", "请人工检查章节质量")),
                    revisedPassage = ""
                )
            }
            
            lastResult = result
            if (result.overallScore >= PASS_THRESHOLD) {
                onProgressUpdate?.invoke("质量审查通过（评分：${result.overallScore}）")
                return@withContext result
            }
            
            if (result.revisedPassage.isNotEmpty()) {
                onProgressUpdate?.invoke("评分不达标（${result.overallScore}），正在应用修正...")
                currentContent = result.revisedPassage
                currentRetry++
            } else break
        }
        
        onProgressUpdate?.invoke("质量审查完成（最终评分：${lastResult?.overallScore ?: 0}）")
        lastResult ?: CritiqueResult(
            novelty = 5, consistency = 5, addressing = 5, pacing = 5, overallScore = 50,
            issues = emptyList(), revisedPassage = content
        )
    }
    
    fun getQualityLevel(score: Int): String = when {
        score >= 90 -> "优秀"
        score >= 80 -> "良好"
        score >= 70 -> "合格"
        score >= 60 -> "需改进"
        else -> "较差"
    }
    
    private fun extractJson(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```json")) cleaned = cleaned.substringAfter("```json").trim()
        else if (cleaned.startsWith("```")) cleaned = cleaned.substringAfter("```").trim()
        if (cleaned.endsWith("```")) cleaned = cleaned.substringBeforeLast("```").trim()
        return cleaned
    }
    
    private suspend fun callApi(apiKey: String, model: String, systemPrompt: String, userPrompt: String, temperature: Double): Result<String> {
        return try {
            val request = ChatRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
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
