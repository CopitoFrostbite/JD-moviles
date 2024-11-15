package com.example.app1.data.repository

import android.content.Context

import android.util.Log
import androidx.lifecycle.LiveData

import com.example.app1.data.local.JournalEntryDao

import com.example.app1.data.model.JournalEntry
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
    private val journalDao: JournalEntryDao,
    private val context: Context
) {

    suspend fun registerJournalEntry(journalRequest: JournalApiService.JournalRequest): Response<JournalEntry> {
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

    /*suspend fun getAllJournalEntries(userId: String): List<JournalEntry> {
        return try {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val response = api.getAllJournalEntries(userId)
                if (response.isSuccessful) {
                    val cloudEntries = response.body()?.filter { !it.isDeleted } ?: emptyList()
                    journalDao.insertAll(cloudEntries) // Sincroniza los datos en la base de datos local
                    cloudEntries
                } else {
                    journalDao.getAllEntriesByUserId(userId).filter { !it.isDeleted }
                }
            } else {
                journalDao.getAllEntriesByUserId(userId).filter { !it.isDeleted }
            }
        } catch (e: Exception) {
            Log.e("JournalEntryRepository", "Error al obtener entradas de diario", e)
            journalDao.getAllEntriesByUserId(userId).filter { !it.isDeleted }
        }
    }*/

    // Obtener una entrada de diario específica localmente
    suspend fun getJournalEntryById(journalId: String): JournalEntry? {
        return journalDao.getEntryById(journalId)?.takeIf { !it.isDeleted }
    }

    /*suspend fun getJournalEntryById(journalId: String): JournalEntry? {
        return try {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val response = api.getJournalEntryById(journalId)
                if (response.isSuccessful) {
                    val journalEntry = response.body()
                    if (journalEntry != null) {
                        journalDao.insertEntry(journalEntry) // Actualiza la base de datos local
                    }
                    journalEntry
                } else {
                    journalDao.getEntryById(journalId)
                }
            } else {
                journalDao.getEntryById(journalId)
            }
        } catch (e: Exception) {
            Log.e("JournalEntryRepository", "Error al obtener entrada de diario por ID", e)
            journalDao.getEntryById(journalId)
        }
    }*/

    // Sincronizar todas las entradas con la nube
    suspend fun syncAllEntries(userId: String): Boolean {
        if (!NetworkUtils.isNetworkAvailable(context)) return false

        return try {
            val response = api.getAllJournalEntries(userId)
            if (response.isSuccessful) {
                val cloudEntries = response.body()?.filter { !it.isDeleted } ?: emptyList()
                journalDao.insertAll(cloudEntries) // Sobrescribir datos locales con los de la nube
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("JournalEntryRepository", "Error sincronizando entradas", e)
            false
        }
    }

    suspend fun markAsDeleted(journalId: String) {
        journalDao.updateJournalDeletionStatus(journalId, isDeleted = true)
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): T {
        return try {
            apiCall()
        } catch (e: Exception) {
            Log.e("Repository", "Error during API call", e)
            throw e // Relanza la excepción para que sea manejada en otro nivel si es necesario
        }
    }
}