package com.example.app1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.app1.data.model.JournalEntry

@Dao
interface JournalEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(journalEntry: JournalEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(journalEntries: List<JournalEntry>)

    @Query("SELECT * FROM journal_entries WHERE entryId = :entryId")
    suspend fun getEntryById(entryId: String): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE userId = :userId")
    suspend fun getAllEntriesByUserId(userId: String): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId")
    fun getAllEntriesByUserIdSync(userId: String): LiveData<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND isEdited = 1")
    suspend fun getEditedEntries(userId: String): List<JournalEntry>

    @Update
    suspend fun updateEntry(journalEntry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE entryId = :entryId")
    suspend fun deleteEntry(entryId: String)

    @Query("DELETE FROM journal_entries WHERE userId = :userId")
    suspend fun deleteAllEntriesByUserId(userId: String)
}