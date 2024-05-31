package com.example.app1.data.remote

import com.example.app1.data.model.JournalEntry
import retrofit2.http.GET

interface JournalApiService {
    @GET("entries")
    suspend fun getJournalEntries(): List<JournalEntry>
}