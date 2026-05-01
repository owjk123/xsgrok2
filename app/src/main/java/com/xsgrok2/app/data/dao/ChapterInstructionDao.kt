package com.xsgrok2.app.data.dao

import androidx.room.*
import com.xsgrok2.app.data.model.ChapterInstruction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterInstructionDao {
    @Query("SELECT * FROM chapter_instructions WHERE chapterId = :chapterId LIMIT 1")
    suspend fun getInstructionByChapterId(chapterId: Long): ChapterInstruction?

    @Query("SELECT * FROM chapter_instructions WHERE novelId = :novelId")
    fun getInstructionsByNovelId(novelId: Long): Flow<List<ChapterInstruction>>

    @Insert
    suspend fun insertInstruction(instruction: ChapterInstruction): Long

    @Update
    suspend fun updateInstruction(instruction: ChapterInstruction)

    @Delete
    suspend fun deleteInstruction(instruction: ChapterInstruction)

    @Query("DELETE FROM chapter_instructions WHERE chapterId = :chapterId")
    suspend fun deleteByChapterId(chapterId: Long)
}
