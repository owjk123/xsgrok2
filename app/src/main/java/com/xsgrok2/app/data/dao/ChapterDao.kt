package com.xsgrok2.app.data.dao

import androidx.room.*
import com.xsgrok2.app.data.model.Chapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    fun getChaptersByNovelId(novelId: Long): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): Chapter?

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber DESC LIMIT 1")
    suspend fun getLatestChapter(novelId: Long): Chapter?

    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId")
    suspend fun getChapterCount(novelId: Long): Int

    @Insert
    suspend fun insertChapter(chapter: Chapter): Long

    @Insert
    suspend fun insertChapters(chapters: List<Chapter>)

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Delete
    suspend fun deleteChapter(chapter: Chapter)

    @Query("DELETE FROM chapters WHERE novelId = :novelId")
    suspend fun deleteChaptersByNovelId(novelId: Long)
}
