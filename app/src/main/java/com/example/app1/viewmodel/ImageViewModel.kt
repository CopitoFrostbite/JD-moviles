package com.example.app1.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.app1.data.model.Image
import com.example.app1.data.repository.ImageRepository
import com.example.app1.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class ImageViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _imageUiState = MutableLiveData<UiState<List<Image>>>()
    val imageUiState: LiveData<UiState<List<Image>>> get() = _imageUiState

    private val _addImageUiState = MutableLiveData<UiState<Image>>()
    val addImageUiState: LiveData<UiState<Image>> get() = _addImageUiState

    private val _syncUiState = MutableLiveData<UiState<Unit>>()
    val syncUiState: LiveData<UiState<Unit>> get() = _syncUiState

    // Obtener imágenes asociadas a un journal
    fun getImagesByJournalId(journalId: String) {
        _imageUiState.postValue(UiState.Loading)
        viewModelScope.launch {
            try {
                val images = imageRepository.getImagesByJournalId(journalId)
                _imageUiState.postValue(UiState.Success(images.value ?: emptyList()))
            } catch (e: Exception) {
                _imageUiState.postValue(UiState.Error("Error al cargar imágenes", e))
            }
        }
    }

    // Añadir una imagen a un journal
    fun addImageToEntry(entryId: String, image: Image) {
        _addImageUiState.postValue(UiState.Loading)
        viewModelScope.launch {
            try {
                val response = imageRepository.addImageToEntry(entryId, image)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _addImageUiState.postValue(UiState.Success(it))
                    } ?: _addImageUiState.postValue(UiState.Error("Respuesta vacía del servidor"))
                } else {
                    _addImageUiState.postValue(UiState.Error("Error al añadir imagen: ${response.code()}"))
                }
            } catch (e: Exception) {
                _addImageUiState.postValue(UiState.Error("Error al añadir imagen", e))
            }
        }
    }

    // Sincronizar imágenes de journals específicos
    fun syncImagesWithJournals(journalIds: List<String>) {
        _syncUiState.postValue(UiState.Loading)
        viewModelScope.launch {
            try {
                imageRepository.syncImages(journalIds)
                _syncUiState.postValue(UiState.Success(Unit))
            } catch (e: Exception) {
                _syncUiState.postValue(UiState.Error("Error al sincronizar imágenes", e))
            }
        }
    }
}