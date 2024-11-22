package com.example.app1.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.model.extensions.toRequest
import com.example.app1.data.repository.JournalEntryRepository
import com.example.app1.utils.PreferencesHelper
import com.example.app1.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class JournalEntryViewModel @Inject constructor(
    private val journalRepository: JournalEntryRepository,
    application: Application
) : AndroidViewModel(application) {

    // LiveData y MutableLiveData para manejar estados
    private val _syncStatus = MutableLiveData<Boolean>()
    val syncStatus: LiveData<Boolean> get() = _syncStatus

    private val _createJournalEntryLiveData = MutableLiveData<Response<JournalEntry>>()
    val createJournalEntryLiveData: LiveData<Response<JournalEntry>> get() = _createJournalEntryLiveData

    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus


    private val _saveJournalState = MutableLiveData<Boolean>()


    private val _updateJournalState = MutableLiveData<Boolean>()


    private val _journalList = MutableLiveData<List<JournalEntry>>()
    val journalList: LiveData<List<JournalEntry>> get() = _journalList



    // Obtener todas las entradas localmente
    fun getUserJournals(userId: String): LiveData<List<JournalEntry>> = liveData {
        emit(journalRepository.getAllJournalEntries(userId))
    }

    // Obtener una entrada específica localmente
    fun getJournalEntryById(entryId: String): LiveData<JournalEntry?> = liveData {
        emit(journalRepository.getJournalEntryById(entryId))
    }

    // Sincronizar todas las entradas con la nube
    suspend fun syncAllEntries(userId: String): UiState<Unit> {
        return try {
            val syncedJournals = journalRepository.syncAllEntries(userId) // Sincroniza y obtiene los journals
            if (syncedJournals.isNotEmpty()) {
                _journalList.postValue(syncedJournals) // Actualiza el LiveData
                UiState.Success(Unit)
            } else {
                UiState.Error("No se pudieron sincronizar las entradas.")
            }
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Error al sincronizar journals", e)
            UiState.Error("Error al sincronizar journals", e)
        }
    }



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

    fun refreshJournals(context: Context) {
        val userId = PreferencesHelper.getUserId(context) ?: return
        viewModelScope.launch {
            try {
                val updatedJournals = journalRepository.getAllJournalEntries(userId) // Recupera la lista actualizada
                _journalList.postValue(updatedJournals)
            } catch (e: Exception) {
                Log.e("ViewModel", "Error al actualizar journals: ${e.message}")
            }
        }
    }

    // Guardar un borrador de entrada de diario
    fun saveDraftJournalEntry(journalEntry: JournalEntry) {
        viewModelScope.launch {
            val isSuccessful = journalRepository.saveDraft(journalEntry)
            _saveJournalState.postValue(isSuccessful) // Actualizar el estado
        }
    }

    // Marcar una entrada como eliminada
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

    fun updateJournalEntry(journalEntry: JournalEntry) {
        viewModelScope.launch {
            try {
                val isSuccessful = journalRepository.updateJournalEntry(journalEntry)
                _updateJournalState.postValue(isSuccessful) // Publicar el resultado en el LiveData
            } catch (e: Exception) {
                Log.e("JournalEntryViewModel", "Error al actualizar la entrada", e)
                _updateJournalState.postValue(false) // Publicar error
            }
        }
    }


}