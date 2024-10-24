package com.example.app1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "journal_entries",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)]
)
data class JournalEntry(
    @PrimaryKey val journalId: String = UUID.randomUUID().toString(),  // ID local
    val userId: String,
    var title: String,
    var content: String,
    var mood: Int,
    var date: Long = System.currentTimeMillis(),
    var isEdited: Boolean = false,
    var isDraft: Boolean = true
)