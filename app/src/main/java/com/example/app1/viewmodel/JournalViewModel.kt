package com.example.app1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: JournalRepository
) : ViewModel() {

    private val _journalEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journalEntries: StateFlow<List<JournalEntry>> get() = _journalEntries

    fun getJournalEntries() {
        viewModelScope.launch {
            _journalEntries.value = repository.getJournalEntries()
        }
    }

    fun addJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.addJournalEntry(entry)
            getJournalEntries()
        }
    }
}