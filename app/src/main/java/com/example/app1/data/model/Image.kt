package com.example.app1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "images",
    foreignKeys = [ForeignKey(entity = JournalEntry::class, parentColumns = ["journalId"], childColumns = ["journalId"], onDelete = ForeignKey.CASCADE)]
)
data class Image(
    @PrimaryKey val imageId: String,
    val journalId: String,
    val filePath: String,
    val description: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)