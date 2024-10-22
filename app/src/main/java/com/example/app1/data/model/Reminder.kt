package com.example.app1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)]
)
data class Reminder(
    @PrimaryKey val reminderId: String,
    val userId: String,
    var description: String,
    var date: Long,
    val time: String
)