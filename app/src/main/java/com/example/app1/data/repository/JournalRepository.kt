package com.example.app1.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.liveData
import com.example.app1.data.local.JournalEntryDao

import com.example.app1.data.model.JournalEntry
import com.example.app1.data.model.extensions.toRequest
import com.example.app1.data.remote.JournalApiService
import com.example.app1.utils.NetworkUtils
import com.example.app1.utils.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class JournalEntryRepository @Inject constructor(
    private val api: JournalApiService,
    private val journalDao: JournalEntryDao,
    private val context: Context
) {
    suspend fun registerJournalEntry(journalRequest: JournalApiService.JournalRequest): Response<JournalEntry> {
        val journalEntry = journalDao.getEntryById(journalRequest.journalId)

        // Si es un borrador, evitar enviar a la API
        if (journalEntry?.isDraft == true) {
            return Response.success(journalEntry)
        }

        return if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = api.registerJournalEntry(journalRequest)
                if (response.isSuccessful) {
                    journalEntry?.let {
                        it.isDraft = false
                        journalDao.updateEntry(it)
                    }
                }
                response
            } catch (e: Exception) {
                Response.error(500, "Error during journal entry registration".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } else {
            Response.error(503, "No network available".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }

    // Método para obtener todas las entradas de diario locales (sin verificar conexión)
    fun getLocalJournals(userId: String): LiveData<List<JournalEntry>> {
        return journalDao.getAllEntriesByUserIdSync(userId)
    }

    suspend fun saveDraft(journalEntry: JournalEntry): Boolean {
        return try {
            journalDao.insertEntry(journalEntry)

            // Verificar si el guardado funcionó recuperando el entry
            val savedEntry = journalDao.getEntryById(journalEntry.journalId)
            savedEntry != null  // Retorna 'true' si el draft fue guardado
        } catch (e: Exception) {
            Log.e("JournalRepository", "Error saving draft", e)
            false  // Retorna 'false' si hubo un error
        }
    }

    suspend fun getJournalDraftById(journalId: String): JournalEntry? {
        return journalDao.getJournalById(journalId)
    }

    // Método para actualizar el estado de `isDraft` de una entrada de diario en la base de datos local
    suspend fun updateJournalStatus(journalId: String, isDraft: Boolean) {
        val journalEntry = journalDao.getEntryById(journalId)
        journalEntry?.let {
            it.isDraft = isDraft
            journalDao.updateEntry(it)
        }
    }

    suspend fun getAllJournalEntries(userId: String): List<JournalEntry> {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = api.getAllJournalEntries(userId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        journalDao.insertAll(it)
                        return it
                    }
                }
                journalDao.getAllEntriesByUserId(userId)
            } catch (e: Exception) {
                journalDao.getAllEntriesByUserId(userId)
            }
        } else {
            journalDao.getAllEntriesByUserId(userId)
        }
    }

    suspend fun getJournalEntryById(entryId: String): JournalEntry? {
        return try {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val response = api.getJournalEntryById(entryId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        journalDao.insertEntry(it)
                        return it
                    }
                }
                journalDao.getEntryById(entryId)
            } else {
                journalDao.getEntryById(entryId)
            }
        } catch (e: Exception) {
            journalDao.getEntryById(entryId)
        }
    }

    // Sincronización automática tras guardar una nueva entrada
    suspend fun syncJournalEntry(journalEntry: JournalEntry): Response<JournalEntry>? {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            val cloudEntry = api.getJournalEntryById(journalEntry.journalId).body()

            val resolvedEntry = resolveConflict(journalEntry, cloudEntry)
            val response = resolvedEntry?.let { api.registerJournalEntry(it.toRequest()) }

            if (response != null) {
                if (response.isSuccessful) {
                    journalDao.updateEntry(resolvedEntry)
                }
            }
            response
        } else {
            // Si no hay conexión, guardar como borrador
            journalDao.insertEntry(journalEntry.apply { isDraft = true })
            Response.success(journalEntry)
        }
    }

    // Sincronización manual completa (Opción 4)
    suspend fun syncJournalEntriesWithCloud(userId: String) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val localEntries = journalDao.getAllEntriesByUserId(userId)
            val cloudEntriesResponse = api.getAllJournalEntries(userId)

            if (cloudEntriesResponse.isSuccessful) {
                val cloudEntries = cloudEntriesResponse.body() ?: emptyList()

                // Resolver conflictos y actualizar la base de datos
                cloudEntries.forEach { cloudEntry ->
                    val localEntry = localEntries.find { it.journalId == cloudEntry.journalId }
                    val resolvedEntry = resolveConflict(localEntry, cloudEntry)
                    if (resolvedEntry != null) {
                        journalDao.insertEntry(resolvedEntry)
                    } // Actualiza o inserta la entrada resuelta
                }
            }
        }
    }

    // Resolución de conflictos en base a la fecha de modificación
    private fun resolveConflict(localEntry: JournalEntry?, cloudEntry: JournalEntry?): JournalEntry? {
        return when {
            // Si la entrada local es más reciente, darle prioridad
            localEntry != null && cloudEntry != null && localEntry.date >= cloudEntry.date -> localEntry

            // Si la entrada en la nube es más reciente, actualizar la local
            localEntry != null && cloudEntry != null && localEntry.date < cloudEntry.date -> cloudEntry

            // Si solo existe la entrada local, retornar la entrada local
            localEntry != null -> localEntry

            // Si solo existe la entrada en la nube, retornar la entrada en la nube
            cloudEntry != null -> cloudEntry

            // Si no hay entradas, retornar null (opcional)
            else -> null
        }
    }

    // Método para actualizar todas las entradas locales desde la nube en segundo plano
    suspend fun syncEditedEntries() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val userId = PreferencesHelper.getUserId(context)
            userId?.let {
                val editedEntries = journalDao.getEditedEntries(it)
                for (entry in editedEntries) {
                    val response = api.updateJournalEntry(entry.journalId, entry.title, entry.content, entry.date, entry.isEdited)
                    if (response.isSuccessful) {
                        entry.isEdited = false // Marcar como sincronizado
                        journalDao.updateEntry(entry)
                    }
                }
            }
        }
    }

    fun getCurrentJournalEntries(): LiveData<List<JournalEntry>> = liveData(Dispatchers.IO) {
        val userId = PreferencesHelper.getUserId(context)
        val entries: LiveData<List<JournalEntry>> = if (userId?.isNotEmpty() == true) {
            journalDao.getAllEntriesByUserIdSync(userId)
        } else {
            MutableLiveData(emptyList())
        }
        emitSource(entries)
    }

    private fun uriToFile(uri: Uri): File {
        val contentResolver = context.contentResolver
        val fileName = getFileName(uri)
        val tempFile = File(context.cacheDir, fileName)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    }

    private fun getFileName(uri: Uri): String {
        var name = "temp_file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    // Método para sincronizar borradores cuando hay conexión
   // suspend fun syncDrafts() {
     //   if (NetworkUtils.isNetworkAvailable(context)) {
       //     val drafts = journalDao.getDraftEntriesByUserId()  // Método en `journalDao` que retorna los borradores

         //   drafts.forEach { draft ->
           //     try {
             //       val response = api.registerJournalEntry(draft.toRequest())
               //     if (response.isSuccessful) {
                 //       draft.isDraft = false
                   //     journalDao.updateEntry(draft)  // Marcar como publicado
                  //  }
               // } catch (e: Exception) {
                 //   Log.e("JournalEntryRepository", "Error al sincronizar borrador: ${draft.journalId}", e)
              //  }
          //  }
      //  }
    //}

    // Sincronizar borradores manualmente
    suspend fun syncDrafts(): Boolean {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            val drafts = journalDao.getDraftEntriesByUserId()
            var success = true
            drafts.forEach { draft ->
                try {
                    val response = api.registerJournalEntry(draft.toRequest())
                    if (response.isSuccessful) {
                        draft.isDraft = false
                        journalDao.updateEntry(draft)  // Marcar como sincronizado
                    } else {
                        success = false
                    }
                } catch (e: Exception) {
                    Log.e("JournalEntryRepository", "Error al sincronizar borrador: ${draft.journalId}", e)
                    success = false
                }
            }
            success
        } else {
            false
        }
    }

    // Método de sincronización general, que llama syncDrafts
    suspend fun syncData() {
        syncDrafts()
        // Agregar otras operaciones de sincronización si es necesario
    }

    suspend fun syncAllEntries(): Boolean {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            // Obtener todas las entradas de la nube
            val userId = PreferencesHelper.getUserId(context)
            if (userId != null) {
                try {
                    val cloudEntriesResponse = api.getAllJournalEntries(userId)
                    if (cloudEntriesResponse.isSuccessful) {
                        val cloudEntries = cloudEntriesResponse.body()
                        if (cloudEntries != null) {
                            cloudEntries.forEach { cloudEntry ->
                                val localEntry = journalDao.getEntryById(cloudEntry.journalId)
                                val resolvedEntry = resolveConflict(localEntry, cloudEntry)
                                if (resolvedEntry != null) {
                                    journalDao.insertEntry(resolvedEntry)
                                }  // Guardar la entrada resuelta en local
                            }
                            return true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("JournalEntryRepository", "Error al sincronizar las entradas", e)
                    return false
                }
            }
            false
        } else {
            false
        }
    }


}