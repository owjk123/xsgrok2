package com.xsgrok2.app.data.repository

import com.xsgrok2.app.data.api.GrokApiService
import com.xsgrok2.app.data.model.*

class GrokRepository(
    private val apiService: GrokApiService
) {
    suspend fun generateNovelSettings(
        apiKey: String,
        model: String,
        genre: String,
        description: String
    ): Result<String> {
        return try {
            val systemPrompt = """You are a creative novel writing assistant. Given a genre and description, generate:
1. World Setting: A detailed description of the world/universe where the story takes place
2. Key Characters: 3-5 main characters with names, personalities, and backgrounds
3. Story Outline: A detailed chapter-by-chapter outline (at least 10 chapters)

Format your response clearly with headers for each section. Be creative and detailed."""

            val request = ChatRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = "Genre: $genre\n\nDescription: $description\n\nPlease generate the world setting, key characters, and story outline for this novel.")
                ),
                temperature = 0.8,
                max_tokens = 4096
            )

            val response = apiService.chatCompletion("Bearer $apiKey", request)
            if (response.error != null) {
                Result.failure(Exception(response.error.message ?: "API error"))
            } else if (response.choices.isNullOrEmpty()) {
                Result.failure(Exception("Empty response from API"))
            } else {
                val content = response.choices[0].message?.content ?: ""
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateChapter(
        apiKey: String,
        model: String,
        novel: Novel,
        chapterNumber: Int,
        chapterTitle: String,
        previousChapterContent: String = ""
    ): Result<String> {
        return try {
            val systemPrompt = """You are a creative novel writer. Write a detailed chapter of a novel based on the provided information.
Write in an engaging, vivid style. The chapter should be substantial (at least 1000 words).
Do not include meta-commentary, just write the chapter content."""

            val userPrompt = buildString {
                append("Novel Title: ${novel.title}\n")
                append("Genre: ${novel.genre}\n")
                append("World Setting: ${novel.worldSetting}\n")
                append("Key Characters: ${novel.keyCharacters}\n")
                append("Story Outline: ${novel.outline}\n\n")
                append("Chapter $chapterNumber: $chapterTitle\n\n")
                if (previousChapterContent.isNotEmpty()) {
                    append("Previous chapter summary:\n")
                    append(previousChapterContent.take(500))
                    append("...\n\n")
                }
                append("Write the full content for Chapter $chapterNumber: $chapterTitle")
            }

            val request = ChatRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                temperature = 0.85,
                max_tokens = 4096
            )

            val response = apiService.chatCompletion("Bearer $apiKey", request)
            if (response.error != null) {
                Result.failure(Exception(response.error.message ?: "API error"))
            } else if (response.choices.isNullOrEmpty()) {
                Result.failure(Exception("Empty response from API"))
            } else {
                val content = response.choices[0].message?.content ?: ""
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
