package com.example.app1.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.repository.JournalEntryRepository
import com.example.app1.workers.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class JournalEntryViewModel @Inject constructor(
    private val journalRepository: JournalEntryRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _createJournalEntryLiveData = MutableLiveData<Response<JournalEntry>>()
    val createJournalEntryLiveData: LiveData<Response<JournalEntry>> get() = _createJournalEntryLiveData

    fun createJournalEntry(
        userId: String,
        title: String,
        content: String,
        date: Long,
        isEdited: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = journalRepository.registerJournalEntry(userId, title, content, date, isEdited)
                _createJournalEntryLiveData.postValue(response)
            } catch (e: Exception) {
                Log.e("JournalEntryViewModel", "Error creating journal entry", e)
                val errorResponse = Response.error<JournalEntry>(500, "Error creating journal entry".toResponseBody("text/plain".toMediaTypeOrNull()))
                _createJournalEntryLiveData.postValue(errorResponse)
            }
        }
    }

    fun updateJournalEntry(
        entryId: Int,
        title: String,
        content: String,
        date: Long,
        isEdited: Boolean,
        imageUri: Uri?
    ): LiveData<Response<JournalEntry>> {
        val result = MutableLiveData<Response<JournalEntry>>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
               // val response = journalRepository.updateJournalEntry(entryId, title, content, date, isEdited, imageUri)
               // result.postValue(response)
            } catch (e: Exception) {
                Log.e("JournalEntryViewModel", "Error updating journal entry", e)
                val errorResponse = Response.error<JournalEntry>(500, ResponseBody.create("text/plain".toMediaTypeOrNull(), "Error updating journal entry"))
                result.postValue(errorResponse)
            }
        }
        return result
    }

    fun getJournalEntryById(entryId: Int) = liveData(Dispatchers.IO) {
        val entry = journalRepository.getJournalEntryById(entryId)
        emit(entry)
    }

    fun setJournalEntry(entry: JournalEntry) {
        //_journalEntry.value = entry
    }

    fun getCurrentJournalEntries(): LiveData<List<JournalEntry>> {
        return journalRepository.getCurrentJournalEntries()
    }

    fun getUserJournals(userId: String): LiveData<List<JournalEntry>> {
        return liveData {
            val journals = journalRepository.getAllJournalEntries(userId)
            emit(journals)
        }
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getApplication()).enqueue(syncRequest)
    }
}