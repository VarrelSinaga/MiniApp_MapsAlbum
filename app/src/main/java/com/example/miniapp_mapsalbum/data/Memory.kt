package com.example.miniapp_mapsalbum.data

// File: data/Memory.kt
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var title: String = "Kenangan Baru",
    val latitude: Double,
    val longitude: Double,
    var imagePath: String? = null
) : Parcelable