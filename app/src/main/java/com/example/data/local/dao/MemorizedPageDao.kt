package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MemorizedPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemorizedPageDao {
    @Query("SELECT * FROM memorized_pages ORDER BY pageNumber ASC")
    fun getAllMemorizedPages(): Flow<List<MemorizedPageEntity>>

    @Query("SELECT * FROM memorized_pages WHERE pageNumber = :pageNumber LIMIT 1")
    suspend fun getMemorizedPage(pageNumber: Int): MemorizedPageEntity?

    @Query("SELECT COUNT(*) FROM memorized_pages WHERE isMemorized = 1")
    fun getMemorizedPageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemorizedPage(page: MemorizedPageEntity)
    
    @Query("DELETE FROM memorized_pages WHERE pageNumber = :pageNumber")
    suspend fun deleteMemorizedPage(pageNumber: Int)
}
