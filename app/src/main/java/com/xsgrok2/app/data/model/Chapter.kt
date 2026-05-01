package com.xsgrok2.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

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
    val content: String = "",
    val isGenerated: Boolean = false,
    val userNote: String = "",
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
