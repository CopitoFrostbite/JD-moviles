package com.example.app1.data.repository

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.app1.data.local.ReminderDao
import com.example.app1.data.model.Reminder
import com.example.app1.ui.AlarmReceiver
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject


class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val context: Context
) {

    suspend fun addReminder(
        userId: String,
        description: String,
        date: Long,
        time: String
    ) {
        val reminder = Reminder(
            reminderId = UUID.randomUUID().toString(),
            userId = userId,
            description = description,
            date = date,
            time = time
        )
        reminderDao.insertReminder(reminder)
        setLocalAlarm(reminder) // Configurar alarma local
    }

    suspend fun getRemindersForUser(userId: String): List<Reminder> {
        return reminderDao.getRemindersByUser(userId)
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
        setLocalAlarm(reminder) // Actualizar alarma local
    }

    suspend fun deleteReminder(reminderId: String) {
        reminderDao.deleteReminderById(reminderId)
        cancelLocalAlarm(reminderId) // Cancelar alarma local
    }

    suspend fun markReminderAsDeleted(reminderId: String) {
        reminderDao.markReminderAsDeleted(reminderId, true)
        cancelLocalAlarm(reminderId) // Cancelar alarma local
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setLocalAlarm(reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("reminderId", reminder.reminderId)
            putExtra("description", reminder.description)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            timeInMillis = reminder.date
        }
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelLocalAlarm(reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }
}