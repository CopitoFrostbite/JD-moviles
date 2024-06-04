package com.example.app1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.app1.data.model.Reminder
import com.example.app1.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    fun createReminder(reminder: Reminder) = liveData(Dispatchers.IO) {
        val response = reminderRepository.createReminder(reminder)
        emit(response)
    }

    fun getRemindersByUserId(userId: Int) = liveData(Dispatchers.IO) {
        val reminders = reminderRepository.getRemindersByUserIdFromApi(userId)
        emit(reminders)
    }

    fun getLocalRemindersByUserId(userId: Int) = reminderRepository.getRemindersByUserId(userId)
}