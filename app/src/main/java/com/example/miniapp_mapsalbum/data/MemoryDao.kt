package com.example.miniapp_mapsalbum.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY id DESC") // Tambahkan ORDER BY untuk konsistensi
    fun getAllMemories(): Flow<List<Memory>>

    // BARU: Tambahkan fungsi ini
    @Query("SELECT * FROM memories ORDER BY id DESC")
    suspend fun getAllMemoriesList(): List<Memory>

    @Insert
    suspend fun insertMemory(memory: Memory): Long // Mengembalikan ID yang di-generate

    @Update
    suspend fun updateMemory(memory: Memory)

    @Delete
    suspend fun deleteMemory(memory: Memory)

    @Query("SELECT * FROM memories WHERE id = :memoryId")
    suspend fun getMemoryById(memoryId: Long): Memory?
}