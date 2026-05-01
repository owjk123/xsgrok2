package com.xsgrok2.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = Novel::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("novelId")]
)
data class Chapter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val novelId: Long = 0,
    val chapterNumber: Int = 0,
    val title: String = "",
    val customTitle: String = "",
    val content: String = "",
    val isGenerated: Boolean = false,
    val status: String = "generated",
    val generationMode: String = "new",
    val userNote: String = "",
    val wordCount: Int = 0,
    val summary: String = "",
    val keyEvents: String = "",
    val characterStateSnapshot: String = "",
    val qualityScore: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun displayTitle(): String = if (customTitle.isNotBlank()) customTitle else title
}
