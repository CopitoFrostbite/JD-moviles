package com.example.app1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "settings",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)]
)
data class Settings(
    @PrimaryKey(autoGenerate = true) val settingsId: Int = 0,
    val userId: String,
    val theme: String,
    val notificationsEnabled: Boolean
)