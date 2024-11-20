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
     fun getImagesByJournalId(journalId: String): LiveData<List<Image>> {
        return imageRepository.getImagesByJournalId(journalId)
    }

    fun deleteImageById(imageId: String) {
        viewModelScope.launch {
            try {
                imageRepository.deleteImageById(imageId)
                Log.d("ImageViewModel", "Imagen eliminada: $imageId")
            } catch (e: Exception) {
                Log.e("ImageViewModel", "Error al eliminar imagen: $imageId", e)
            }
        }
    }




    // Añadir una imagen a un journal
    fun addImageToEntry(entryId: String, image: Image) {
        viewModelScope.launch {
            try {
                val response = imageRepository.addImageToEntry(entryId, image)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("ImageViewModel", "Imagen añadida: ${it.imageId}")
                    }
                } else {
                    Log.e("ImageViewModel", "Error al añadir imagen: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("ImageViewModel", "Error al añadir imagen", e)
            }
        }
    }

    fun addImageToCloud(entryId: String, image: Image) {
        viewModelScope.launch {
            try {
                val response = imageRepository.publishImageToCloud(entryId, image)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("ImageViewModel", "Imagen publicada en la nube: ${response.message()}")
                    }
                } else {
                    Log.e("ImageViewModel", "Error al publicar imagen en la nube: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("ImageViewModel", "Error al publicar imagen en la nube", e)
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
                Log.e("SyncViewModel", "Error al sincronizar imágenes: ${e.localizedMessage}", e)
                _syncUiState.postValue(UiState.Error("Error al sincronizar imágenes", e))
            }
        }
    }

    fun syncImages(journalIds: List<String>) {
        viewModelScope.launch {
            try {
                imageRepository.syncImages(journalIds)
                Log.d("ImageViewModel", "Imágenes sincronizadas con journals: $journalIds")
            } catch (e: Exception) {
                Log.e("ImageViewModel", "Error al sincronizar imágenes", e)
            }
        }
    }

    // Descargar y guardar imágenes localmente
    fun downloadImage(image: Image) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val filePath = imageRepository.downloadAndSaveImageLocally(context, image)
                if (filePath != null) {
                    Log.d("ImageViewModel", "Imagen guardada localmente: $filePath")
                }
            } catch (e: Exception) {
                Log.e("ImageViewModel", "Error al descargar la imagen", e)
            }
        }
    }
}