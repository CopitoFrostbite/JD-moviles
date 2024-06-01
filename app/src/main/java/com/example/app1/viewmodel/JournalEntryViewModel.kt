package com.example.app1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.repository.JournalEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class JournalEntryViewModel @Inject constructor(
    private val journalEntryRepository: JournalEntryRepository
) : ViewModel() {

    fun createEntry(entry: JournalEntry) = liveData(Dispatchers.IO) {
        val response = journalEntryRepository.createEntry(entry)
        emit(response)
    }

    fun getEntriesByUserId(userId: Int) = liveData(Dispatchers.IO) {
        val entries = journalEntryRepository.getEntriesByUserIdFromApi(userId)
        emit(entries)
    }

    fun getLocalEntriesByUserId(userId: Int) = journalEntryRepository.getEntriesByUserId(userId)
}