package com.example.app1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "images",
    foreignKeys = [ForeignKey(entity = JournalEntry::class, parentColumns = ["entryId"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE)]
)
data class Image(
    @PrimaryKey(autoGenerate = true) val imageId: Int = 0,
    val entryId: Int,
    val filePath: String,
    val description: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)