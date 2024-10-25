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
        // Buscar la entrada de diario localmente usando el journalId del JournalRequest
        val journalEntry = journalDao.getEntryById(journalRequest.journalId)

        // Verifica si la entrada es un borrador
        if (journalEntry?.isDraft == true) {
            Log.d("JournalRepository", "Entry is a draft and won't be sent to the API")
            return Response.success(journalEntry)  // Retornamos la entrada localmente
        }

        // Verificación de red y envío solo si no es borrador
        return if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = api.registerJournalEntry(journalRequest)  // Usamos journalRequest directamente
                if (response.isSuccessful) {
                    // Actualiza la base de datos local para indicar que ya no es borrador
                    journalEntry?.isDraft = false
                    journalEntry?.let { journalDao.updateEntry(it) }  // Actualiza en la BD local
                }
                response
            } catch (e: Exception) {
                Log.e("JournalRepository", "Error registering journal", e)
                Response.error(
                    500,
                    "Error during journal entry registration".toResponseBody("text/plain".toMediaTypeOrNull())
                )
            }
        } else {
            Log.e("JournalRepository", "No network available for journal entry registration")
            Response.error(
                503,
                "No network available".toResponseBody("text/plain".toMediaTypeOrNull())
            )
        }
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

    suspend fun syncData() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val userId = PreferencesHelper.getUserId(context)
            if (userId != "") {
                userId?.let<String, List<JournalEntry>> { journalDao.getAllEntriesByUserId(it) }
                    ?.forEach {
                        //api.updateEntry(it.entryId, it)
                    }
            }
        }
    }

    suspend fun syncEditedEntries() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
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
            } catch (e: Exception) {
                Log.e("JournalRepository", "Error syncing edited journal entries", e)
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
}