package com.example.app1.data.repository

import com.example.app1.data.local.JournalEntryDao
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.model.JournalEntry

class JournalRepository(
    private val journalEntryDao: JournalEntryDao,
    private val journalApiService: JournalApiService
) {
    // Local data functions
    suspend fun getJournalEntries() = journalEntryDao.getAllEntries()
    suspend fun addJournalEntry(entry: JournalEntry) = journalEntryDao.insert(entry)

    // Remote data functions (if needed)
    // suspend fun fetchJournalEntriesFromApi() = journalApiService.getJournalEntries()
}