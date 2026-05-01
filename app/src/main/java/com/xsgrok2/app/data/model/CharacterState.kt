package com.xsgrok2.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 角色状态实体 - 用于追踪每个角色在故事中的当前状态
 * 包括情绪、知识、关系、位置等信息，确保长篇一致性
 */
@Entity(
    tableName = "character_states",
    foreignKeys = [
        ForeignKey(
            entity = Novel::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("novelId"), Index("characterName")]
)
data class CharacterState(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val novelId: Long = 0,
    val characterName: String = "",
    val currentMood: String = "",
    val knownInformation: String = "",
    val relationships: String = "",
    val currentLocation: String = "",
    val forbiddenActions: String = "",
    val arcProgress: String = "",
    val chapterNumber: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class StatePlan(
    val characterName: String,
    val moodStart: String,
    val moodEnd: String,
    val willLearn: List<String>,
    val relationshipShift: String,
    val addressing: Map<String, String>,
    val keyAction: String
)

data class MemoryStream(
    val recentChapterContent: String,
    val recentSummaries: List<ChapterSummary>,
    val characterStates: List<CharacterState>,
    val pendingForeshadowing: List<String>
)

data class ChapterSummary(
    val chapterNumber: Int,
    val summary: String,
    val keyEvents: List<String>,
    val characterDeltas: Map<String, CharacterDelta>,
    val qualityIssues: List<String>
)

data class CharacterDelta(
    val moodChange: String,
    val newKnowledge: List<String>,
    val relationshipChange: String,
    val locationChange: String? = null
)

data class CritiqueResult(
    val novelty: Int,
    val consistency: Int,
    val addressing: Int,
    val pacing: Int,
    val overallScore: Int,
    val issues: List<QualityIssue>,
    val revisedPassage: String
)

data class QualityIssue(
    val type: String,
    val location: String,
    val description: String,
    val suggestion: String
)
