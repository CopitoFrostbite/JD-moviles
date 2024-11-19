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
    val userId: String, // Relación con el usuario
    var description: String,
    var date: Long,
    var time: String, // Cambiado a var para permitir edición
    var localNotificationId: String? = null, // Para manejar notificaciones locales
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis() // Consistencia con User
)