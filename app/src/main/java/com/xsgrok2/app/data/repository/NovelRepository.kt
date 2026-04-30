package com.xsgrok2.app.data.repository

import com.xsgrok2.app.data.dao.ChapterDao
import com.xsgrok2.app.data.dao.NovelDao
import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.Novel
import kotlinx.coroutines.flow.Flow

class NovelRepository(
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao
) {
    // Novel operations
    fun getAllNovels(): Flow<List<Novel>> = novelDao.getAllNovels()

    suspend fun getNovelById(id: Long): Novel? = novelDao.getNovelById(id)

    fun getNovelByIdFlow(id: Long): Flow<Novel?> = novelDao.getNovelByIdFlow(id)

    suspend fun insertNovel(novel: Novel): Long = novelDao.insertNovel(novel)

    suspend fun updateNovel(novel: Novel) = novelDao.updateNovel(novel)

    suspend fun deleteNovel(novel: Novel) = novelDao.deleteNovel(novel)

    suspend fun deleteNovelById(id: Long) = novelDao.deleteNovelById(id)

    // Chapter operations
    fun getChaptersByNovelId(novelId: Long): Flow<List<Chapter>> = chapterDao.getChaptersByNovelId(novelId)

    suspend fun getChapterById(id: Long): Chapter? = chapterDao.getChapterById(id)

    suspend fun getLatestChapter(novelId: Long): Chapter? = chapterDao.getLatestChapter(novelId)

    suspend fun getChapterCount(novelId: Long): Int = chapterDao.getChapterCount(novelId)

    suspend fun insertChapter(chapter: Chapter): Long = chapterDao.insertChapter(chapter)

    suspend fun insertChapters(chapters: List<Chapter>) = chapterDao.insertChapters(chapters)

    suspend fun updateChapter(chapter: Chapter) = chapterDao.updateChapter(chapter)

    suspend fun deleteChapter(chapter: Chapter) = chapterDao.deleteChapter(chapter)

    suspend fun deleteChaptersByNovelId(novelId: Long) = chapterDao.deleteChaptersByNovelId(novelId)
}
