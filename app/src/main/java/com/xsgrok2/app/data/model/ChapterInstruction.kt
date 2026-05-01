package com.xsgrok2.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "chapter_instructions",
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId"), Index("novelId")]
)
data class ChapterInstruction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterId: Long = 0,
    val novelId: Long = 0,
    val coreEvent: String = "",
    val characterChanges: String = "",
    val mood: String = "",
    val foreshadowing: String = "",
    val newThreads: String = "",
    val wordCountTarget: Int = 3000,
    val forbiddenElements: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
