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
    suspend fun syncImages(journalIds: List<String>? = null) {
        // Verificar la conexión antes de proceder
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.e("ImageRepository", "Sin conexión a Internet. Sincronización cancelada.")
            return
        }

        val pendingImages = withContext(Dispatchers.IO) {
            journalIds?.let { ids ->
                imageDao.getPendingSyncImages().filter { it.journalId in ids }
            } ?: imageDao.getPendingSyncImages()
        }

        // Procesar imágenes en paralelo
        withContext(Dispatchers.IO) {
            pendingImages.map { image ->
                async {
                    retryWithBackoff {
                        try {
                            if (image.isDeleted) {
                                // Lógica para imágenes marcadas como eliminadas
                                val deleteResponse = api.deleteImage(image.imageId)
                                if (deleteResponse.isSuccessful) {
                                    imageDao.deleteImageById(image.imageId) // Borra la imagen de la base de datos local
                                    Log.d("ImageRepository", "Imagen eliminada en el servidor: ${image.imageId}")
                                } else {
                                    Log.e("ImageRepository", "Error al eliminar imagen en el servidor: ${deleteResponse.message()}")
                                }
                            } else if (image.isEdited) {
                                // Lógica para imágenes editadas: convertir a MultipartBody.Part
                                val imageFile = File(image.filePath)
                                if (imageFile.exists()) {
                                    val imagePart = MultipartBody.Part.createFormData(
                                        "image",
                                        imageFile.name,
                                        imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                    )

                                    // Convertir datos adicionales de Image a RequestBody
                                    val journalIdPart = image.journalId.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val imageIdPart = image.imageId.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val filePathPart = image.filePath.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val dateAddedPart = image.dateAdded.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                    val syncDatePart = (image.syncDate?.toString() ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

                                    // Subir imagen al servidor con datos adicionales
                                    val response = api.addImageToEntry(
                                        journalIdPart,
                                        imagePart,
                                        imageIdPart,
                                        filePathPart,
                                        dateAddedPart,
                                        syncDatePart
                                    )
                                    if (response.isSuccessful) {
                                        imageDao.insertImage(image.copy(isEdited = false, syncDate = System.currentTimeMillis()))
                                        Log.d("ImageRepository", "Imagen sincronizada con el servidor: ${image.imageId}")
                                    } else {
                                        Log.e("ImageRepository", "Error al sincronizar imagen con el servidor: ${response.message()}")
                                    }
                                } else {
                                    Log.e("ImageRepository", "Archivo de imagen no encontrado: ${image.filePath}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ImageRepository", "Error al sincronizar imagen: ${image.imageId}", e)
                        }
                    }
                }
            }.awaitAll() // Esperar a que todos los procesos concurrentes finalicen
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