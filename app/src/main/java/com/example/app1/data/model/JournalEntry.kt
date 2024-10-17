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
    var title: String,
    var content: String,
    val date: Long = System.currentTimeMillis(),
    var isEdited: Boolean = false
)