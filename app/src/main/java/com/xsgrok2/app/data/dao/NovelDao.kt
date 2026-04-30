package com.xsgrok2.app.data.dao

import androidx.room.*
import com.xsgrok2.app.data.model.Novel
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    @Query("SELECT * FROM novels ORDER BY updatedAt DESC")
    fun getAllNovels(): Flow<List<Novel>>

    @Query("SELECT * FROM novels WHERE id = :id")
    suspend fun getNovelById(id: Long): Novel?

    @Query("SELECT * FROM novels WHERE id = :id")
    fun getNovelByIdFlow(id: Long): Flow<Novel?>

    @Insert
    suspend fun insertNovel(novel: Novel): Long

    @Update
    suspend fun updateNovel(novel: Novel)

    @Delete
    suspend fun deleteNovel(novel: Novel)

    @Query("DELETE FROM novels WHERE id = :id")
    suspend fun deleteNovelById(id: Long)
}
