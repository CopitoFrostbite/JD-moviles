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
import com.example.app1.data.remote.JournalApiService
import com.example.app1.utils.NetworkUtils
import com.example.app1.utils.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

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
    suspend fun registerJournalEntry(
        userId: String,
        title: String,
        content: String,
        date: Long,
        isEdited: Boolean
    ): Response<JournalEntry> {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val userIdPart = userId.toRequestBody("text/plain".toMediaTypeOrNull())
                val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val contentPart = content.toRequestBody("text/plain".toMediaTypeOrNull())
                val datePart = date.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val isEditedPart = isEdited.toString().toRequestBody("text/plain".toMediaTypeOrNull())



                val response = api.registerJournalEntry( userIdPart, titlePart, contentPart, datePart, isEditedPart)
                if (response.isSuccessful) {
                    response.body()?.let {
                        journalDao.insertEntry(it)
                    }
                }
                response
            } catch (e: Exception) {
                Log.e("JournalRepository", "Error registering journal entry", e)
                Response.error(500, "Error during journal entry registration".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } else {
            Response.error(503, "No network available".toResponseBody("text/plain".toMediaTypeOrNull()))
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

    suspend fun getJournalEntryById(entryId: Int): JournalEntry? {
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
                        val response = api.updateJournalEntry(entry.entryId, entry.title, entry.content, entry.date, entry.isEdited)
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