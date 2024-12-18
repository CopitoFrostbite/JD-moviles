package com.example.app1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String ,
    val username: String,
    var name: String,
    var lastname: String,
    var email: String,
    var password: String? = null,
    var profilePicture: String? = null,
    var localProfilePicture: String? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)