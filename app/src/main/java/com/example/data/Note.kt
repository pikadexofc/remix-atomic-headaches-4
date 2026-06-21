package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val locationLabel: String,
    val mood: String,
    val preset: String,
    val deviceLocation: String,
    val audioPath: String? = null
)
