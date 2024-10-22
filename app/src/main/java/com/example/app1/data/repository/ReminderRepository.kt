package com.example.app1.data.repository

import androidx.lifecycle.LiveData
import com.example.app1.data.local.ReminderDao
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.model.Reminder
import retrofit2.Response
import javax.inject.Inject


class ReminderRepository @Inject constructor(
    private val api: JournalApiService,
    private val reminderDao: ReminderDao
) {
    suspend fun createReminder(reminder: Reminder): Response<Reminder> {
        return api.createReminder(reminder)
    }

    fun getRemindersByUserId(userId: String): LiveData<List<Reminder>> {
        return reminderDao.getRemindersByUserId(userId)
    }

    suspend fun getRemindersByUserIdFromApi(userId: String): Response<List<Reminder>> {
        return api.getRemindersByUserId(userId)
    }
}