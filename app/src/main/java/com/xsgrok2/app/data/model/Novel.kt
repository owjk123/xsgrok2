package com.xsgrok2.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class Novel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val genre: String = "",
    val description: String = "",
    val writingStyle: String = "细腻生动",
    val worldSetting: String = "",
    val keyCharacters: String = "",
    val outline: String = "",
    val model: String = "grok-4.20-beta",
    val totalWordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
