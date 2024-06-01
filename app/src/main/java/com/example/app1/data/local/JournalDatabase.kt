package com.example.app1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.model.User
import com.example.app1.data.model.Image
import com.example.app1.data.model.Reminder
import com.example.app1.data.model.Settings

@Database(entities = [User::class, JournalEntry::class, Image::class, Reminder::class, Settings::class], version = 1)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun imageDao(): ImageDao
    abstract fun reminderDao(): ReminderDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var instance: JournalDatabase? = null

        fun getDatabase(context: Context): JournalDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                JournalDatabase::class.java, "journal_app.db")
                .build()
    }
}