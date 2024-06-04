package com.example.app1.data.repository

import androidx.lifecycle.LiveData
import com.example.app1.data.local.JournalEntryDao
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.model.JournalEntry
import retrofit2.Response
import javax.inject.Inject

class JournalEntryRepository @Inject constructor(
    private val api: JournalApiService,
    private val journalEntryDao: JournalEntryDao
) {
    suspend fun createEntry(entry: JournalEntry): Response<JournalEntry> {
        return api.createEntry(entry)
    }

    fun getEntriesByUserId(userId: Int): LiveData<List<JournalEntry>> {
        return journalEntryDao.getEntriesByUserId(userId)
    }

    suspend fun getEntriesByUserIdFromApi(userId: Int): Response<List<JournalEntry>> {
        return api.getEntriesByUserId(userId)
    }
}