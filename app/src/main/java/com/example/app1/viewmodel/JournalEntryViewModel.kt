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

    private val _syncStatus = MutableLiveData<Boolean>()
    private val _createJournalEntryLiveData = MutableLiveData<Response<JournalEntry>>()
    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus

    val createJournalEntryLiveData: LiveData<Response<JournalEntry>> get() = _createJournalEntryLiveData

    val syncStatus: LiveData<Boolean> get() = _syncStatus

    private val _publishStatus = MutableLiveData<Response<JournalEntry>>()
    val publishStatus: LiveData<Response<JournalEntry>> get() = _publishStatus

    // Obtener todas las entradas localmente
    fun getUserJournals(userId: String): LiveData<List<JournalEntry>> = liveData {
        emit(journalRepository.getAllJournalEntries(userId))
    }

    // Obtener una entrada específica localmente
    fun getJournalEntryById(entryId: String): LiveData<JournalEntry?> = liveData {
        emit(journalRepository.getJournalEntryById(entryId))
    }

    // Sincronizar todas las entradas con la nube
    fun syncAllEntries(userId: String) {
        viewModelScope.launch {
            val result = journalRepository.syncAllEntries(userId)
            _syncStatus.postValue(result)
        }
    }

    // Publicar una entrada en la nube
    // Publicar una entrada de diario
    fun publishJournalEntry(journalEntry: JournalEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = journalRepository.publishJournalEntry(journalEntry.toRequest())
                _createJournalEntryLiveData.postValue(response) // Publica el resultado en el LiveData
            } catch (e: Exception) {
                _createJournalEntryLiveData.postValue(
                    Response.error(500, "Error al publicar entrada".toResponseBody("text/plain".toMediaTypeOrNull()))
                )
            }
        }
    }

    fun markAsDeleted(journalId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                journalRepository.markAsDeleted(journalId)
                _deleteStatus.postValue(true) // Notifica éxito
            } catch (e: Exception) {
                Log.e("JournalEntryViewModel", "Error al marcar como eliminado", e)
                _deleteStatus.postValue(false) // Notifica error
            }
        }
    }



    fun saveDraftJournalEntry(journalEntry: JournalEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = journalRepository.saveDraft(journalEntry)
            _syncStatus.postValue(success)
        }
    }
}