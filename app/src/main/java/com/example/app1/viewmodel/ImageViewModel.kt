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

    // LiveData para monitorear el estado de sincronización
    private val _syncStatus = MutableLiveData<Boolean>()
    val syncStatus: LiveData<Boolean> get() = _syncStatus

    // LiveData para el estado de creación/añadido de imágenes
    private val _addImageLiveData = MutableLiveData<Response<Image>>()
    val addImageLiveData: LiveData<Response<Image>> get() = _addImageLiveData

    // LiveData para el estado de eliminación
    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus

    // Obtener imágenes asociadas a una entrada de journal desde Room
    fun getImagesByEntryId(entryId: String): LiveData<List<Image>> {
        return imageRepository.getImagesByEntryId(entryId)
    }

    // Añadir una imagen a una entrada de journal
    fun addImageToEntry(entryId: String, image: Image) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = imageRepository.addImageToEntry(entryId, image)
                _addImageLiveData.postValue(response) // Publicar el resultado en el LiveData
            } catch (e: Exception) {
                _addImageLiveData.postValue(
                    Response.error(500, "Error al añadir imagen".toResponseBody("text/plain".toMediaTypeOrNull()))
                )
                Log.e("ImageViewModel", "Error al añadir imagen", e)
            }
        }
    }

    // Marcar una imagen como eliminada (delete lógico)
    fun markImageAsDeleted(imageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                imageRepository.markImageAsDeleted(imageId)
                _deleteStatus.postValue(true) // Notifica éxito
            } catch (e: Exception) {
                Log.e("ImageViewModel", "Error al marcar imagen como eliminada", e)
                _deleteStatus.postValue(false) // Notifica error
            }
        }
    }

    // Sincronizar imágenes pendientes con la nube
    fun syncImages() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                imageRepository.syncImages()
                _syncStatus.postValue(true) // Sincronización exitosa
            } catch (e: Exception) {
                Log.e("ImageViewModel", "Error al sincronizar imágenes", e)
                _syncStatus.postValue(false) // Sincronización fallida
            }
        }
    }

    // Obtener imágenes desde la nube
    fun fetchImagesFromApi(entryId: String): LiveData<Response<List<Image>>> = liveData(Dispatchers.IO) {
        try {
            val response = imageRepository.getImagesByEntryIdFromApi(entryId)
            emit(response)
        } catch (e: Exception) {
            Log.e("ImageViewModel", "Error al obtener imágenes desde la API", e)
            emit(
                Response.error(500, "Error al obtener imágenes desde la API".toResponseBody("text/plain".toMediaTypeOrNull()))
            )
        }
    }
}