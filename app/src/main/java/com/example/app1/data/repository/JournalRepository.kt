package com.example.app1.data.repository

import android.content.Context

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.app1.data.local.ImageDao

import com.example.app1.data.local.JournalEntryDao
import com.example.app1.data.model.Image

import com.example.app1.data.model.JournalEntry
import com.example.app1.data.model.JournalWithImages
import com.example.app1.data.model.extensions.toRequest
import com.example.app1.data.remote.JournalApiService
import com.example.app1.utils.NetworkUtils
import com.example.app1.utils.PreferencesHelper

import okhttp3.MediaType.Companion.toMediaTypeOrNull

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

import javax.inject.Inject

class JournalEntryRepository @Inject constructor(
    private val api: JournalApiService,
    private val imageRepository: ImageRepository,
    private val journalDao: JournalEntryDao,
    private val imageDao: ImageDao,
    private val context: Context
) {

    private suspend fun registerJournalEntry(journalRequest: JournalApiService.JournalRequest): Response<JournalEntry> {
        val journalEntry = journalDao.getEntryById(journalRequest.journalId)
        if (journalEntry?.isDraft == true || journalEntry?.isDeleted == true) {
            return Response.success(journalEntry)
        }

        return safeApiCall {
            val response = api.registerJournalEntry(journalRequest)
            if (response.isSuccessful) {
                journalDao.updateJournalDeletionStatus(journalRequest.journalId, isDeleted = false)
            }
            response
        }
    }

    suspend fun registerJournalEntryWithImages(
        journalRequest: JournalApiService.JournalRequest,
        images: List<Image>
    ): Boolean {
        val journalResponse = registerJournalEntry(journalRequest)
        if (journalResponse.isSuccessful) {
            images.forEach { image ->
                imageRepository.addImageToEntry(journalRequest.journalId, image)
            }
            return true
        }
        return false
    }

    fun getJournalWithImages(journalId: String): LiveData<JournalWithImages> {
        return journalDao.getJournalWithImages(journalId)
    }

    // Publicar una entrada de diario en la nube
    suspend fun publishJournalEntry(journalRequest: JournalApiService.JournalRequest): Response<JournalEntry> {
        val response = api.registerJournalEntry(journalRequest)
        if (response.isSuccessful) {
            val journalId = journalRequest.journalId
            // Marcar como publicado (isDraft = false)
            val journalEntry = journalDao.getEntryById(journalId)
            journalEntry?.let {
                it.isDraft = false
                journalDao.insertEntry(it) // Actualizar entrada local
            }
        }
        return response
    }

    suspend fun saveDraft(journalEntry: JournalEntry): Boolean {
        return try {
            // Asegurar que el borrador esté marcado como draft
            journalEntry.isDraft = true
            journalDao.insertEntry(journalEntry)
            true
        } catch (e: Exception) {
            Log.e("JournalEntryRepository", "Error al guardar borrador", e)
            false
        }
    }

    suspend fun updateLocalJournalDeletionStatus(journalId: String, isDeleted: Boolean) {
        journalDao.updateJournalDeletionStatus(journalId, isDeleted)
    }

    // Obtener todas las entradas de diario localmente
    suspend fun getAllJournalEntries(userId: String): List<JournalEntry> {
        return journalDao.getAllEntriesByUserId(userId).filter { !it.isDeleted }
    }



    // Obtener una entrada de diario específica localmente
    suspend fun getJournalEntryById(journalId: String): JournalEntry? {
        return journalDao.getEntryById(journalId)?.takeIf { !it.isDeleted }
    }



    // Sincronizar todas las entradas con la nube
    suspend fun syncAllEntries(userId: String): Boolean {
        if (!NetworkUtils.isNetworkAvailable(context)) return false

        return try {
            val response = api.getAllJournalEntries(userId)
            if (response.isSuccessful) {
                val cloudEntries = response.body()?.filter { !it.isDeleted } ?: emptyList()
                journalDao.insertAll(cloudEntries)

                // Sincronizar imágenes asociadas usando ImageRepository
                cloudEntries.forEach { journal ->
                    imageRepository.syncImages(listOf(journal.journalId))
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("JournalEntryRepository", "Error sincronizando entradas", e)
            false
        }
    }

    suspend fun publishJournalWithImages(journal: JournalEntry, images: List<Image>): Boolean {
        val journalRequest = journal.toRequest()
        val journalResponse = registerJournalEntry(journalRequest)
        if (journalResponse.isSuccessful) {
            images.forEach { image ->
                imageRepository.addImageToEntry(journal.journalId, image)
            }
            return true
        }
        return false
    }

    suspend fun markAsDeleted(journalId: String) {
        journalDao.updateJournalDeletionStatus(journalId, isDeleted = true)
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): T {
        return try {
            apiCall()
        } catch (e: Exception) {
            Log.e("Repository", "Error during API call", e)
            throw e
        }
    }


}