package com.example.app1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntry::class,
            parentColumns = ["journalId"],
            childColumns = ["journalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["journalId"])]
)
data class Image(
    @PrimaryKey val imageId: String,
    val journalId: String,
    val filePath: String,
    val cloudUrl: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val isEdited: Boolean = true,
    val isDeleted: Boolean = false,
    val syncDate: Long? = null
)