package com.example.app1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.model.JournalWithImages

@Dao
interface JournalEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(journalEntry: JournalEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(journalEntries: List<JournalEntry>)

    @Query("SELECT * FROM journal_entries WHERE journalId = :journalId AND (:isDraft IS NULL OR isDraft = :isDraft) AND isDeleted = 0")
    suspend fun getEntryById(journalId: String, isDraft: Boolean? = null): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND isDeleted = 0")
    suspend fun getAllEntriesByUserId(userId: String): List<JournalEntry>
    @Transaction
    @Query("SELECT * FROM journal_entries WHERE journalId = :journalId")
    fun getJournalWithImages(journalId: String): LiveData<JournalWithImages>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND isDeleted = 0")
    fun getAllEntriesByUserIdSync(userId: String): LiveData<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE (isEdited = 1 OR isDeleted = 1 OR isDraft = 1) AND userId = :userId")
    suspend fun getEntriesForSync(userId: String): List<JournalEntry>

    @Query("UPDATE journal_entries SET isDeleted = :isDeleted WHERE journalId = :journalId")
    suspend fun updateJournalDeletionStatus(journalId: String, isDeleted: Boolean)

    @Query("UPDATE journal_entries SET isDraft = :isDraft WHERE journalId = :journalId")
    suspend fun updateDraftStatus(journalId: String, isDraft: Boolean)

    @Query("UPDATE journal_entries SET isEdited = :isEdited WHERE journalId = :journalId")
    suspend fun updateEditedStatus(journalId: String, isEdited: Boolean)

    @Update
    suspend fun updateEntry(journalEntry: JournalEntry)


}