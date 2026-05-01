package com.xsgrok2.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "lorebook_entries",
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
data class LorebookEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val novelId: Long = 0,
    val keyword: String = "",
    val content: String = "",
    val importance: Int = 3,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
