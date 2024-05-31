package com.example.app1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.app1.data.model.JournalEntry

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries")
    suspend fun getAllEntries(): List<JournalEntry>

    @Insert
    suspend fun insert(entry: JournalEntry)
}