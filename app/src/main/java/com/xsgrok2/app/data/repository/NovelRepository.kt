package com.xsgrok2.app.data.repository

import com.xsgrok2.app.data.dao.ChapterDao
import com.xsgrok2.app.data.dao.ChapterInstructionDao
import com.xsgrok2.app.data.dao.CharacterStateDao
import com.xsgrok2.app.data.dao.LorebookEntryDao
import com.xsgrok2.app.data.dao.NovelDao
import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.ChapterInstruction
import com.xsgrok2.app.data.model.CharacterState
import com.xsgrok2.app.data.model.LorebookEntry
import com.xsgrok2.app.data.model.Novel
import kotlinx.coroutines.flow.Flow

class NovelRepository(
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao,
    private val lorebookEntryDao: LorebookEntryDao,
    private val chapterInstructionDao: ChapterInstructionDao,
    private val characterStateDao: CharacterStateDao
) {
    fun getAllNovels(): Flow<List<Novel>> = novelDao.getAllNovels()
    suspend fun getNovelById(id: Long): Novel? = novelDao.getNovelById(id)
    fun getNovelByIdFlow(id: Long): Flow<Novel?> = novelDao.getNovelByIdFlow(id)
    suspend fun insertNovel(novel: Novel): Long = novelDao.insertNovel(novel)
    suspend fun updateNovel(novel: Novel) = novelDao.updateNovel(novel)
    suspend fun deleteNovel(novel: Novel) = novelDao.deleteNovel(novel)
    suspend fun deleteNovelById(id: Long) = novelDao.deleteNovelById(id)

    fun getChaptersByNovelId(novelId: Long): Flow<List<Chapter>> = chapterDao.getChaptersByNovelId(novelId)
    suspend fun getChapterById(id: Long): Chapter? = chapterDao.getChapterById(id)
    suspend fun getChapterByNumber(novelId: Long, chapterNumber: Int): Chapter? = chapterDao.getChapterByNumber(novelId, chapterNumber)
    suspend fun getLatestChapter(novelId: Long): Chapter? = chapterDao.getLatestChapter(novelId)
    suspend fun getChapterCount(novelId: Long): Int = chapterDao.getChapterCount(novelId)
    suspend fun getPreviousChapter(novelId: Long, chapterNumber: Int): Chapter? = chapterDao.getPreviousChapter(novelId, chapterNumber)
    suspend fun getNextChapter(novelId: Long, chapterNumber: Int): Chapter? = chapterDao.getNextChapter(novelId, chapterNumber)
    suspend fun insertChapter(chapter: Chapter): Long = chapterDao.insertChapter(chapter)
    suspend fun insertChapters(chapters: List<Chapter>) = chapterDao.insertChapters(chapters)
    suspend fun updateChapter(chapter: Chapter) = chapterDao.updateChapter(chapter)
    suspend fun deleteChapter(chapter: Chapter) = chapterDao.deleteChapter(chapter)
    suspend fun deleteChapterById(id: Long) = chapterDao.deleteChapterById(id)
    suspend fun deleteChaptersByNovelId(novelId: Long) = chapterDao.deleteChaptersByNovelId(novelId)
    suspend fun shiftChaptersDown(novelId: Long, fromPosition: Int) = chapterDao.shiftChaptersDown(novelId, fromPosition)
    suspend fun shiftChaptersUp(novelId: Long, deletedPosition: Int) = chapterDao.shiftChaptersUp(novelId, deletedPosition)

    fun getLorebookEntries(novelId: Long): Flow<List<LorebookEntry>> = lorebookEntryDao.getEntriesByNovelId(novelId)
    suspend fun getEnabledLorebookEntries(novelId: Long): List<LorebookEntry> = lorebookEntryDao.getEnabledEntries(novelId)
    suspend fun getLorebookEntryById(id: Long): LorebookEntry? = lorebookEntryDao.getEntryById(id)
    suspend fun insertLorebookEntry(entry: LorebookEntry): Long = lorebookEntryDao.insertEntry(entry)
    suspend fun updateLorebookEntry(entry: LorebookEntry) = lorebookEntryDao.updateEntry(entry)
    suspend fun deleteLorebookEntry(entry: LorebookEntry) = lorebookEntryDao.deleteEntry(entry)
    suspend fun deleteLorebookEntriesByNovelId(novelId: Long) = lorebookEntryDao.deleteEntriesByNovelId(novelId)

    suspend fun getInstructionByChapterId(chapterId: Long): ChapterInstruction? = chapterInstructionDao.getInstructionByChapterId(chapterId)
    fun getInstructionsByNovelId(novelId: Long): Flow<List<ChapterInstruction>> = chapterInstructionDao.getInstructionsByNovelId(novelId)
    suspend fun insertInstruction(instruction: ChapterInstruction): Long = chapterInstructionDao.insertInstruction(instruction)
    suspend fun updateInstruction(instruction: ChapterInstruction) = chapterInstructionDao.updateInstruction(instruction)
    suspend fun deleteInstruction(instruction: ChapterInstruction) = chapterInstructionDao.deleteInstruction(instruction)
    suspend fun deleteInstructionByChapterId(chapterId: Long) = chapterInstructionDao.deleteByChapterId(chapterId)

    fun getCharacterStatesByNovelId(novelId: Long): Flow<List<CharacterState>> = characterStateDao.getCharacterStatesByNovelId(novelId)
    suspend fun getCharacterStatesByNovelIdSync(novelId: Long): List<CharacterState> = characterStateDao.getCharacterStatesByNovelIdSync(novelId)
    suspend fun getCharacterStateByName(novelId: Long, characterName: String): CharacterState? = characterStateDao.getCharacterStateByName(novelId, characterName)
    suspend fun getCharacterStatesByChapter(novelId: Long, chapterNumber: Int): List<CharacterState> = characterStateDao.getCharacterStatesByChapter(novelId, chapterNumber)
    suspend fun insertCharacterState(characterState: CharacterState): Long = characterStateDao.insertCharacterState(characterState)
    suspend fun insertCharacterStates(characterStates: List<CharacterState>) = characterStateDao.insertCharacterStates(characterStates)
    suspend fun updateCharacterState(characterState: CharacterState) = characterStateDao.updateCharacterState(characterState)
    suspend fun deleteCharacterState(characterState: CharacterState) = characterStateDao.deleteCharacterState(characterState)
    suspend fun deleteCharacterStatesByNovelId(novelId: Long) = characterStateDao.deleteCharacterStatesByNovelId(novelId)
}
