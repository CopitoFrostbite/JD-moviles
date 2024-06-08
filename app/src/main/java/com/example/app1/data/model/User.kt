package com.example.app1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String,
    val username: String,
    val name: String,
    val lastname: String,
    val email: String,
    val password: String,
    val profilePicture: String? = null
)