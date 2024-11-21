package com.example.app1.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.app1.data.local.ImageDao
import com.example.app1.data.model.Image
import com.example.app1.data.remote.JournalApiService
import com.example.app1.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject


class ImageRepository @Inject constructor(
    private val api: JournalApiService,
    private val imageDao: ImageDao,
    private val context: Context
) {

    // Añadir una imagen a una entrada de journal
    suspend fun addImageToEntry(entryId: String, image: Image): Response<Image> {
        return try {
            // Guardar la imagen localmente marcándola como editada
            val localImage = image.copy(
                journalId = entryId,
                isEdited = true,
                syncDate = null
            )
            imageDao.insertImage(localImage) // Guardar en la base de datos local
            Log.d("ImageRepository", "Imagen guardada localmente como editada")

            Response.success(localImage) // Retornar la imagen local como respuesta
        } catch (e: Exception) {
            Log.e("ImageRepository", "Error al guardar imagen localmente", e)
            Response.error(500, "Error al guardar imagen localmente".toResponseBody())
        }
    }

    suspend fun publishImageToCloud(entryId: String, image: Image): Response<Void> {
        return try {
            // Convertir la imagen a MultipartBody.Part
            val imageFile = File(image.filePath)
            val imagePart = MultipartBody.Part.createFormData(
                "image",
                imageFile.name,
                imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )

            // Convertir los demás datos de la imagen a RequestBody
            val journalIdPart = entryId.toRequestBody("text/plain".toMediaTypeOrNull())
            val imageIdPart = image.imageId.toRequestBody("text/plain".toMediaTypeOrNull())
            val filePathPart = image.filePath.toRequestBody("text/plain".toMediaTypeOrNull())
            val dateAddedPart = image.dateAdded.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val syncDatePart = (image.syncDate?.toString() ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

            // Llamar al método de la API para publicar la imagen
            val response = api.addImageToEntry(
                entryId = journalIdPart,
                imagePart = imagePart,
                imageId = imageIdPart,
                filePath = filePathPart,
                dateAdded = dateAddedPart,
                syncDate = syncDatePart
            )

            // Verificar si la publicación fue exitosa
            if (response.isSuccessful) {
                Log.d("ImageRepository", "Imagen publicada con éxito en la nube")
            } else {
                Log.e("ImageRepository", "Error al publicar imagen en la nube: ${response.message()}")
            }
            response
        } catch (e: Exception) {
            Log.e("ImageRepository", "Error al publicar imagen en la nube", e)
            Response.error(500, "Error al publicar imagen en la nube".toResponseBody())
        }
    }

    // Obtener imágenes de una entrada de journal desde Room
    suspend fun addImages(images: List<Image>) {
        imageDao.insertImages(images)
    }

    fun getImagesByJournalId(journalId: String): LiveData<List<Image>> {
        return imageDao.getImagesByJournalId(journalId)
    }

    suspend fun deleteImageById(imageId: String) {
        try {
            imageDao.deleteImageById(imageId)
            Log.d("ImageRepository", "Imagen eliminada físicamente: $imageId")
        } catch (e: Exception) {
            Log.e("ImageRepository", "Error al eliminar imagen: $imageId", e)
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

    suspend fun downloadAndSaveImageLocally(context: Context, image: Image): String? {
        return try {
            val imageUrl = image.cloudUrl ?: return null
            val url = URL(imageUrl)
            val connection = withContext(Dispatchers.IO) {
                url.openConnection()
            }
            withContext(Dispatchers.IO) {
                connection.connect()
            }
            val inputStream = withContext(Dispatchers.IO) {
                connection.getInputStream()
            }

            val fileName = "${image.imageId}.jpg"
            val file = File(context.filesDir, fileName)
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Actualiza la base de datos local con la ruta del archivo
            imageDao.updateFilePath(image.imageId, file.absolutePath)
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ImageRepository", "Error al guardar la imagen localmente", e)
            null
        }
    }

    // Sincronizar imágenes pendientes
    suspend fun syncImages(userId: String, journalIds: List<String>? = null) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.e("ImageRepository", "Sin conexión a Internet. Sincronización cancelada.")
            return
        }

        val pendingImages = withContext(Dispatchers.IO) {
            userId.let { ids ->
                imageDao.getPendingSyncImages().filter { it.journalId in ids }
            }
        }

        if (pendingImages.isEmpty()) {
            Log.d("ImageRepository", "No hay imágenes pendientes de sincronización.")
        } else {
            val successfullySyncedImages = mutableListOf<String>()
            val successfullyDeletedImages = mutableListOf<String>()

            withContext(Dispatchers.IO) {
                pendingImages.map { image ->
                    async {
                        retryWithBackoff {
                            if (image.isDeleted) {
                                val deleteResponse = api.markImageAsDeleted(image.imageId)
                                if (deleteResponse.isSuccessful) {
                                    successfullyDeletedImages.add(image.imageId)
                                    Log.d("ImageRepository", "Imagen eliminada del servidor: ${image.imageId}")
                                } else {
                                    Log.e("ImageRepository", "Error al eliminar imagen: ${deleteResponse.message()}")
                                }
                            } else if (image.isEdited) {
                                val imageFile = File(image.filePath)
                                if (imageFile.exists()) {
                                    val imagePart = MultipartBody.Part.createFormData(
                                        "image",
                                        imageFile.name,
                                        imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                    )
                                    val journalIdPart = image.journalId.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val imageIdPart = image.imageId.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val filePathPart = image.filePath.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val dateAddedPart = image.dateAdded.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                    val syncDatePart = (image.syncDate?.toString() ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

                                    val response = api.addImageToEntry(
                                        journalIdPart,
                                        imagePart,
                                        imageIdPart,
                                        filePathPart,
                                        dateAddedPart,
                                        syncDatePart
                                    )
                                    if (response.isSuccessful) {
                                        successfullySyncedImages.add(image.imageId)
                                        Log.d("ImageRepository", "Imagen sincronizada con el servidor: ${image.imageId}")
                                    } else {
                                        Log.e("ImageRepository", "Error sincronizando imagen: ${response.message()}")
                                    }
                                } else {
                                    Log.e("ImageRepository", "Archivo no encontrado: ${image.filePath}")
                                }
                            }
                        }
                    }
                }.awaitAll()

                // Actualizar solo las banderas en la base de datos local
                if (successfullySyncedImages.isNotEmpty()) {
                    imageDao.markImagesAsSynced(successfullySyncedImages)
                }
                if (successfullyDeletedImages.isNotEmpty()) {
                    imageDao.unmarkImagesAsDeleted(successfullyDeletedImages)
                }
            }
        }



        if (journalIds != null) {
            withContext(Dispatchers.IO) {
                try {
                    journalIds.forEach { journalId ->
                        val response = api.getImagesByUserId(journalId)


                        Log.d("ImageRepository", "Journal ID: $journalId")
                        Log.d("ImageRepository", "Response Code: ${response.code()}")
                        Log.d("ImageRepository", "Response Message: ${response.message()}")
                        Log.d("ImageRepository", "Response Body: ${response.body()}")

                        if (response.isSuccessful) {
                            response.body()?.let { downloadedImages ->
                                imageDao.insertImages(downloadedImages.map { it.copy(isEdited = false, isDeleted = false) })
                                Log.d("ImageRepository", "Imágenes descargadas y guardadas localmente para el journal: $journalId")
                            }
                        } else {
                            Log.e("ImageRepository", "Error al descargar imágenes para el journalId: $journalId, Message: ${response.message()}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ImageRepository", "Error al descargar imágenes del usuario: $userId", e)
                }
            }
        }
    }
    // Función para manejar reintentos
    private suspend fun retryWithBackoff(retries: Int = 3, block: suspend () -> Unit) {
        var attempt = 0
        var delayTime = 1000L // 1 segundo

        while (attempt < retries) {
            try {
                block()
                return
            } catch (e: Exception) {
                attempt++
                if (attempt >= retries) throw e
                kotlinx.coroutines.delay(delayTime)
                delayTime *= 2
            }
        }
    }


}