package com.example.app1.data.repository

import android.content.Context
import android.util.Log
import com.example.app1.data.local.JournalEntryDao
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.remote.JournalApiService
import com.example.app1.utils.NetworkUtils
import retrofit2.Response
import javax.inject.Inject

class JournalEntryRepository @Inject constructor(
    private val api: JournalApiService,
    private val journalDao: JournalEntryDao,

    private val context: Context
) {


    // Publicar una entrada de diario en la nube
    suspend fun publishJournalEntry(journalRequest: JournalApiService.JournalRequest): Response<JournalEntry> {
        val response = api.registerJournalEntry(journalRequest)
        if (response.isSuccessful) {
            val journalId = journalRequest.journalId


            journalDao.updateDraftStatus(journalId, isDraft = false)
        }
        return response
    }



    suspend fun updateJournalEntry(journalEntry: JournalEntry): Boolean {
        return try {
            journalDao.updateEntry(journalEntry)
            true
        } catch (e: Exception) {
            Log.e("JournalEntryRepository", "Error actualizando entrada", e)
            false
        }
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

    private fun JournalEntry.toJournalRequest(): JournalApiService.JournalRequest {
        return JournalApiService.JournalRequest(
            journalId = this.journalId,
            userId = this.userId,
            title = this.title,
            content = this.content,
            mood = this.mood,
            date = this.date,
            isEdited = this.isEdited,
            isDeleted = this.isDeleted
        )
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
            // 1. Subir journals locales marcados como isEdited, isDeleted o isDraft
            val entriesForSync = journalDao.getEntriesForSync(userId)
            for (entry in entriesForSync) {
                val journalRequest = entry.toJournalRequest() // Convertir JournalEntry a JournalRequest
                val response = when {
                    entry.isDeleted -> {
                        // Actualizar solo el flag isDeleted en la nube
                        api.updateJournalDeleteFlag(entry.journalId, isDeleted = true)
                    }
                    entry.isEdited -> {

                        api.updateJournalEntry(entry.journalId, journalRequest)
                    }
                    else -> {
                        // manejar como borrador

                        publishJournalEntry(journalRequest)
                    }
                }


                if (response.isSuccessful) {
                    when {
                        entry.isDraft -> {
                            // Si es borrador, marcarlo como no borrador
                            journalDao.updateDraftStatus(entry.journalId, isDraft = false)
                        }
                        entry.isEdited -> {
                            // Si está editado, marcarlo como no editado
                            journalDao.updateEditedStatus(entry.journalId, isEdited = false)
                        }
                    }
                }
            }

            // 2. Descargar journals de la nube a local
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



