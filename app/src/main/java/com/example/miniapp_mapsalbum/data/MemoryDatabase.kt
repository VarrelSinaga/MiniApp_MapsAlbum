package com.example.miniapp_mapsalbum.data

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context // <--- TAMBAHKAN INI
import androidx.room.Room      // <--- TAMBAHKAN INI

@Database(entities = [Memory::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getDatabase(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "memory_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}