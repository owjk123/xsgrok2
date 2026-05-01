package com.xsgrok2.app.data.dao

import androidx.room.*
import com.xsgrok2.app.data.model.CharacterState
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterStateDao {
    @Query("SELECT * FROM character_states WHERE novelId = :novelId ORDER BY characterName ASC")
    fun getCharacterStatesByNovelId(novelId: Long): Flow<List<CharacterState>>

    @Query("SELECT * FROM character_states WHERE novelId = :novelId ORDER BY characterName ASC")
    suspend fun getCharacterStatesByNovelIdSync(novelId: Long): List<CharacterState>

    @Query("SELECT * FROM character_states WHERE novelId = :novelId AND characterName = :characterName LIMIT 1")
    suspend fun getCharacterStateByName(novelId: Long, characterName: String): CharacterState?

    @Query("SELECT * FROM character_states WHERE novelId = :novelId AND chapterNumber = :chapterNumber ORDER BY characterName ASC")
    suspend fun getCharacterStatesByChapter(novelId: Long, chapterNumber: Int): List<CharacterState>

    @Query("SELECT * FROM character_states WHERE novelId = :novelId ORDER BY chapterNumber DESC LIMIT :limit")
    suspend fun getRecentCharacterStates(novelId: Long, limit: Int = 10): List<CharacterState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacterState(characterState: CharacterState): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacterStates(characterStates: List<CharacterState>)

    @Update
    suspend fun updateCharacterState(characterState: CharacterState)

    @Delete
    suspend fun deleteCharacterState(characterState: CharacterState)

    @Query("DELETE FROM character_states WHERE novelId = :novelId")
    suspend fun deleteCharacterStatesByNovelId(novelId: Long)

    @Query("DELETE FROM character_states WHERE novelId = :novelId AND chapterNumber > :chapterNumber")
    suspend fun deleteCharacterStatesAfterChapter(novelId: Long, chapterNumber: Int)

    @Query("UPDATE character_states SET chapterNumber = :chapterNumber, currentMood = :mood, knownInformation = :knownInfo, relationships = :relationships, currentLocation = :location WHERE novelId = :novelId AND characterName = :characterName")
    suspend fun updateCharacterStateSync(
        novelId: Long,
        characterName: String,
        chapterNumber: Int,
        mood: String,
        knownInfo: String,
        relationships: String,
        location: String
    )
}
