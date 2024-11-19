package com.example.app1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.app1.data.model.Reminder

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE reminderId = :reminderId")
    suspend fun getReminderById(reminderId: String): Reminder?

    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY date ASC, time ASC")
    suspend fun getRemindersByUser(userId: String): List<Reminder>

    @Query("DELETE FROM reminders WHERE reminderId = :reminderId")
    suspend fun deleteReminderById(reminderId: String)

    @Query("UPDATE reminders SET isEdited = :isEdited WHERE reminderId = :reminderId")
    suspend fun markReminderAsEdited(reminderId: String, isEdited: Boolean = true)

    @Query("UPDATE reminders SET isDeleted = :isDeleted WHERE reminderId = :reminderId")
    suspend fun markReminderAsDeleted(reminderId: String, isDeleted: Boolean = true)

    @Query("SELECT * FROM reminders WHERE isEdited = 1 OR isDeleted = 1 ORDER BY updatedAt DESC")
    suspend fun getPendingSyncReminders(): List<Reminder>

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun clearRemindersForUser(userId: String)
}