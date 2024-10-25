package com.example.app1.data.model.extensions
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.remote.JournalApiService

fun JournalEntry.toRequest(): JournalApiService.JournalRequest {
    return JournalApiService.JournalRequest(
        journalId = this.journalId,
        userId = this.userId,
        title = this.title,
        content = this.content,
        mood = this.mood,
        date = this.date,
        isEdited = this.isEdited
    )
}