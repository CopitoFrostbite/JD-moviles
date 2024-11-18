package com.example.app1.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.app1.data.local.ImageDao
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.model.Image
import com.example.app1.utils.NetworkUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.File
import javax.inject.Inject


class ImageRepository @Inject constructor(
    private val api: JournalApiService,
    private val imageDao: ImageDao,
    private val context: Context
) {

    // Añadir una imagen a una entrada de journal
    suspend fun addImageToEntry(entryId: String, image: Image): Response<Image> {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = api.addImageToEntry(entryId, image)
                if (response.isSuccessful) {
                    response.body()?.let {
                        imageDao.insertImage(it.copy(isEdited = false)) // Guardar en Room sin marcar como editado
                    }
                }
                response
            } catch (e: Exception) {
                Log.e("ImageRepository", "Error al añadir imagen", e)
                Response.error(500, "Error al añadir imagen".toResponseBody())
            }
        } else {
            // Guardar localmente y marcar como editado
            val localImage = image.copy(isEdited = true)
            imageDao.insertImage(localImage)
            Response.success(localImage)
        }
    }

    // Obtener imágenes de una entrada de journal desde Room
    suspend fun addImages(images: List<Image>) {
        imageDao.insertImages(images)
    }

    suspend fun getImagesByJournalId(journalId: String): LiveData<List<Image>> {
        return imageDao.getImagesByJournalId(journalId)
    }

    // Obtener imágenes desde la API
    suspend fun getImagesByEntryIdFromApi(entryId: String): Response<List<Image>> {
        return try {
            val response = api.getImagesByEntryId(entryId)
            if (response.isSuccessful) {
                response.body()?.let { images ->
                    images.forEach { image ->
                        imageDao.insertImage(image.copy(isEdited = false)) // Guardar en Room
                    }
                }
            }
            response
        } catch (e: Exception) {
            Log.e("ImageRepository", "Error al obtener imágenes de la API", e)
            Response.error(500, "Error al obtener imágenes".toResponseBody())
        }
    }

    // Marcar imagen como eliminada (delete lógico)
    suspend fun markImageAsDeleted(imageId: String) {
        imageDao.markImageAsDeleted(imageId)
        Log.d("ImageRepository", "Imagen marcada como eliminada: $imageId")
    }

    // Obtener imágenes pendientes de sincronizar (editadas o eliminadas)
    private suspend fun getPendingSyncImages(): List<Image> {
        return imageDao.getPendingSyncImages()
    }

    // Sincronizar imágenes pendientes
    suspend fun syncImages(journalIds: List<String>? = null) {
        if (!NetworkUtils.isNetworkAvailable(context)) return

        val pendingImages = journalIds?.let { ids ->
            imageDao.getPendingSyncImages().filter { it.journalId in ids }
        } ?: imageDao.getPendingSyncImages()

        pendingImages.forEach { image ->
            try {
                if (image.isDeleted) {
                    val deleteResponse = api.deleteImage(image.imageId)
                    if (deleteResponse.isSuccessful) {
                        imageDao.deleteImageById(image.imageId)
                    }
                } else if (image.isEdited) {
                    val response = api.addImageToEntry(image.journalId, image)
                    if (response.isSuccessful) {
                        imageDao.insertImage(image.copy(isEdited = false))
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageRepository", "Error al sincronizar imagen: ${image.imageId}", e)
            }
        }
    }


}