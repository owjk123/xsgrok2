package com.xsgrok2.app.data.dao

import androidx.room.*
import com.xsgrok2.app.data.model.LorebookEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LorebookEntryDao {
    @Query("SELECT * FROM lorebook_entries WHERE novelId = :novelId ORDER BY importance DESC, keyword ASC")
    fun getEntriesByNovelId(novelId: Long): Flow<List<LorebookEntry>>

    @Query("SELECT * FROM lorebook_entries WHERE novelId = :novelId AND enabled = 1 ORDER BY importance DESC")
    suspend fun getEnabledEntries(novelId: Long): List<LorebookEntry>

    @Query("SELECT * FROM lorebook_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): LorebookEntry?

    @Insert
    suspend fun insertEntry(entry: LorebookEntry): Long

    @Update
    suspend fun updateEntry(entry: LorebookEntry)

    @Delete
    suspend fun deleteEntry(entry: LorebookEntry)

    @Query("DELETE FROM lorebook_entries WHERE novelId = :novelId")
    suspend fun deleteEntriesByNovelId(novelId: Long)
}
