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

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterNumber = :chapterNumber LIMIT 1")
    suspend fun getChapterByNumber(novelId: Long, chapterNumber: Int): Chapter?

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber DESC LIMIT 1")
    suspend fun getLatestChapter(novelId: Long): Chapter?

    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId")
    suspend fun getChapterCount(novelId: Long): Int

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterNumber < :chapterNumber ORDER BY chapterNumber DESC LIMIT 1")
    suspend fun getPreviousChapter(novelId: Long, chapterNumber: Int): Chapter?

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterNumber > :chapterNumber ORDER BY chapterNumber ASC LIMIT 1")
    suspend fun getNextChapter(novelId: Long, chapterNumber: Int): Chapter?

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

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: Long)

    @Query("UPDATE chapters SET chapterNumber = chapterNumber + 1 WHERE novelId = :novelId AND chapterNumber >= :fromPosition")
    suspend fun shiftChaptersDown(novelId: Long, fromPosition: Int)

    @Query("UPDATE chapters SET chapterNumber = chapterNumber - 1 WHERE novelId = :novelId AND chapterNumber > :deletedPosition")
    suspend fun shiftChaptersUp(novelId: Long, deletedPosition: Int)
}
