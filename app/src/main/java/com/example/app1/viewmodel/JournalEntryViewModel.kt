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
import com.example.app1.data.model.extensions.toRequest
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.repository.JournalEntryRepository
import com.example.app1.workers.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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
    private val _syncStatus = MutableLiveData<Boolean>()
    val syncStatus: LiveData<Boolean> get() = _syncStatus
    val createJournalEntryLiveData: LiveData<Response<JournalEntry>> get() = _createJournalEntryLiveData
    // LiveData para listar los journals desde la base de datos local

    fun createJournalEntry(journalRequest: JournalApiService.JournalRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = journalRepository.registerJournalEntry(journalRequest)
                _createJournalEntryLiveData.postValue(response)
            } catch (e: Exception) {
                Log.e("JournalEntryViewModel", "Error creating journal entry", e)
                val errorResponse = Response.error<JournalEntry>(
                    500,
                    "Error creating journal entry".toResponseBody("text/plain".toMediaTypeOrNull())
                )
                _createJournalEntryLiveData.postValue(errorResponse)
            }
        }
    }

    fun syncDrafts() {
        viewModelScope.launch {
            _syncStatus.value = journalRepository.syncDrafts()
        }
    }

    fun saveDraftJournalEntry(journalEntry: JournalEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            journalRepository.saveDraft(journalEntry)  // Guardar en la base de datos local
        }
    }

    fun publishJournalEntry(journalEntry: JournalEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            // Convierte el JournalEntry a JournalRequest para la API
            val response = journalRepository.registerJournalEntry(journalEntry.toRequest())

            if (response.isSuccessful) {
                // Actualizar en la BD local para marcar que ya no es borrador
                journalRepository.updateJournalStatus(journalEntry.journalId, isDraft = false)
                _createJournalEntryLiveData.postValue(response)  // Notificar éxito
            } else {
                _createJournalEntryLiveData.postValue(response)  // Notificar error
            }
        }
    }



    fun syncAllEntries() {
        viewModelScope.launch {
            val result = journalRepository.syncAllEntries()
            _syncStatus.value = result
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

    fun getJournalEntryById(entryId: String) = liveData(Dispatchers.IO) {
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



    // Sincronización manual desde el fragmento
    fun syncAllJournalEntries(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            journalRepository.syncJournalEntriesWithCloud(userId)
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