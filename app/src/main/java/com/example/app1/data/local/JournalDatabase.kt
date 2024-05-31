package com.example.app1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.app1.data.model.JournalEntry

@Database(entities = [JournalEntry::class], version = 1)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        @Volatile private var instance: JournalDatabase? = null

        fun getDatabase(context: Context): JournalDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, JournalDatabase::class.java, "journal_database")
                .build()
    }
}