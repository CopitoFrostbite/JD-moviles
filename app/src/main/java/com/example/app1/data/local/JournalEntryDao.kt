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
    suspend fun insertEntry(entry: JournalEntry)

    @Query("SELECT * FROM journal_entries WHERE userId = :userId")
    fun getEntriesByUserId(userId: Int): LiveData<List<JournalEntry>>
}