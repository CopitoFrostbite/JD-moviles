package com.example.app1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.app1.data.model.Reminder
import com.example.app1.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _reminders = MutableLiveData<List<Reminder>>()
    val reminders: LiveData<List<Reminder>> get() = _reminders

    private val _operationStatus = MutableLiveData<String>()
    val operationStatus: LiveData<String> get() = _operationStatus

    fun addReminder(
        userId: String,
        description: String,
        date: Long,
        time: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reminderRepository.addReminder(userId, description, date, time)
                loadReminders(userId) // Recarga los recordatorios después de añadir uno nuevo
                _operationStatus.postValue("Reminder added successfully")
            } catch (e: Exception) {
                _operationStatus.postValue("Error adding reminder: ${e.message}")
            }
        }
    }

    fun loadReminders(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reminderList = reminderRepository.getRemindersForUser(userId)
                _reminders.postValue(reminderList)
            } catch (e: Exception) {
                _operationStatus.postValue("Error loading reminders: ${e.message}")
            }
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reminderRepository.updateReminder(reminder)
                loadReminders(reminder.userId) // Recarga los recordatorios después de actualizar uno
                _operationStatus.postValue("Reminder updated successfully")
            } catch (e: Exception) {
                _operationStatus.postValue("Error updating reminder: ${e.message}")
            }
        }
    }

    fun deleteReminder(reminderId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reminderRepository.deleteReminder(reminderId)
                loadReminders(userId) // Recarga los recordatorios después de eliminar uno
                _operationStatus.postValue("Reminder deleted successfully")
            } catch (e: Exception) {
                _operationStatus.postValue("Error deleting reminder: ${e.message}")
            }
        }
    }

    fun markReminderAsDeleted(reminderId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reminderRepository.markReminderAsDeleted(reminderId)
                loadReminders(userId) // Recarga los recordatorios después de marcar uno como eliminado
                _operationStatus.postValue("Reminder marked as deleted")
            } catch (e: Exception) {
                _operationStatus.postValue("Error marking reminder as deleted: ${e.message}")
            }
        }
    }
}