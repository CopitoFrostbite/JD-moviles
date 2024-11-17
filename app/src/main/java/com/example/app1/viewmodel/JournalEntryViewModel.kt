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
import com.example.app1.data.model.Image
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.model.JournalWithImages
import com.example.app1.data.model.extensions.toRequest
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.repository.JournalEntryRepository
import com.example.app1.utils.UiState
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

    private val _journalUiState = MutableLiveData<UiState<List<JournalEntry>>>()
    val journalUiState: LiveData<UiState<List<JournalEntry>>> get() = _journalUiState

    private val _publishUiState = MutableLiveData<UiState<JournalEntry>>()
    val publishUiState: LiveData<UiState<JournalEntry>> get() = _publishUiState

    private val _deleteUiState = MutableLiveData<UiState<String>>()
    val deleteUiState: LiveData<UiState<String>> get() = _deleteUiState

    private val _draftUiState = MutableLiveData<UiState<Unit>>()
    val draftUiState: LiveData<UiState<Unit>> get() = _draftUiState

    private val _journalWithImages = MutableLiveData<UiState<JournalWithImages>>()
    val journalWithImages: LiveData<UiState<JournalWithImages>> get() = _journalWithImages

    private val _syncUiState = MutableLiveData<UiState<Unit>>()
    val syncUiState: LiveData<UiState<Unit>> get() = _syncUiState

    // Obtener todas las entradas localmente
    fun getUserJournals(userId: String) {
        _journalUiState.postValue(UiState.Loading)
        viewModelScope.launch {
            try {
                val localEntries = journalRepository.getAllJournalEntries(userId)
                _journalUiState.postValue(UiState.Success(localEntries))
            } catch (e: Exception) {
                _journalUiState.postValue(UiState.Error("Error al cargar las entradas", e))
            }
        }
    }

    // Obtener un journal con imágenes
    fun getJournalWithImages(journalId: String) {
        _journalWithImages.postValue(UiState.Loading)
        viewModelScope.launch {
            try {
                val result = journalRepository.getJournalWithImages(journalId).value
                result?.let {
                    _journalWithImages.postValue(UiState.Success(it))
                } ?: _journalWithImages.postValue(UiState.Error("Journal no encontrado"))
            } catch (e: Exception) {
                _journalWithImages.postValue(UiState.Error("Error al cargar el journal con imágenes", e))
            }
        }
    }

    // Publicar un journal con imágenes
    fun publishJournalWithImages(journal: JournalEntry, images: List<Image>) {
        _publishUiState.postValue(UiState.Loading)
        viewModelScope.launch {
            try {
                val success = journalRepository.publishJournalWithImages(journal, images)
                if (success) {
                    _publishUiState.postValue(UiState.Success(journal))
                } else {
                    _publishUiState.postValue(UiState.Error("Error al publicar el journal"))
                }
            } catch (e: Exception) {
                _publishUiState.postValue(UiState.Error("Error al publicar el journal", e))
            }
        }
    }

    // Guardar un journal como borrador
    fun saveDraftJournalEntry(journalEntry: JournalEntry) {
        _draftUiState.postValue(UiState.Loading)
        viewModelScope.launch {
            try {
                val success = journalRepository.saveDraft(journalEntry)
                if (success) {
                    _draftUiState.postValue(UiState.Success(Unit))
                } else {
                    _draftUiState.postValue(UiState.Error("Error al guardar el borrador"))
                }
            } catch (e: Exception) {
                _draftUiState.postValue(UiState.Error("Error al guardar el borrador", e))
            }
        }
    }

    // Sincronizar journals e imágenes
    fun syncAllEntries(userId: String) {
        _syncUiState.postValue(UiState.Loading)
        viewModelScope.launch {
            try {
                val success = journalRepository.syncAllEntries(userId)
                if (success) {
                    _syncUiState.postValue(UiState.Success(Unit))
                } else {
                    _syncUiState.postValue(UiState.Error("Error al sincronizar las entradas"))
                }
            } catch (e: Exception) {
                _syncUiState.postValue(UiState.Error("Error al sincronizar las entradas", e))
            }
        }
    }

    // Marcar como eliminado
    fun markAsDeleted(journalId: String) {
        _deleteUiState.postValue(UiState.Loading)
        viewModelScope.launch {
            try {
                journalRepository.markAsDeleted(journalId)
                _deleteUiState.postValue(UiState.Success(journalId))
            } catch (e: Exception) {
                _deleteUiState.postValue(UiState.Error("Error al eliminar entrada", e))
            }
        }
    }
}