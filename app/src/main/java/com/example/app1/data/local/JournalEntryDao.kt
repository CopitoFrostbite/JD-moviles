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

    @Query("SELECT * FROM journal_entries WHERE journalId = :journalId")
    suspend fun getEntryById(journalId: String): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE userId = :userId")
    suspend fun getAllEntriesByUserId(userId: String): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId")
    fun getAllEntriesByUserIdSync(userId: String): LiveData<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND isEdited = 1")
    suspend fun getEditedEntries(userId: String): List<JournalEntry>

    @Update
    suspend fun updateEntry(journalEntry: JournalEntry)

    @Query("SELECT * FROM journal_entries WHERE journalId = :journalId AND isDraft = 1 LIMIT 1")
    suspend fun getJournalById(journalId: String): JournalEntry?

    @Query("DELETE FROM journal_entries WHERE journalId = :journalId")
    suspend fun deleteEntry(journalId: String)

    @Query("DELETE FROM journal_entries WHERE userId = :userId")
    suspend fun deleteAllEntriesByUserId(userId: String)

    // Nuevos métodos
    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND isDraft = 1")
    suspend fun getDraftEntriesByUserId(userId: String): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND isDraft = 0")
    suspend fun getPublishedEntriesByUserId(userId: String): List<JournalEntry>

    @Query("UPDATE journal_entries SET isDraft = 0 WHERE journalId = :journalId")
    suspend fun markAsPublished(journalId: String)

    @Query("SELECT * FROM journal_entries WHERE isDraft = 0 AND isEdited = 1")
    suspend fun getEntriesNeedingSync(): List<JournalEntry>

    // Nuevo método para actualizar solo el estado de borrador
    @Query("UPDATE journal_entries SET isDraft = :isDraft WHERE journalId = :journalId")
    suspend fun updateDraftStatus(journalId: String, isDraft: Boolean)


}