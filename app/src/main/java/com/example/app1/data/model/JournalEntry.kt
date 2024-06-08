package com.example.app1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "journal_entries",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)]
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val entryId: Int = 0,
    val userId: String,
    val title: String,
    val content: String,
    val date: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false
)