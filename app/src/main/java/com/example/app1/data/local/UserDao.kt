package com.example.app1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.app1.data.model.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE email = :email AND password = :password")
    suspend fun authenticate(email: String, password: String): User?

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("UPDATE users SET isEdited = :isEdited WHERE userId = :userId")
    suspend fun markUserAsEdited(userId: String, isEdited: Boolean = true)

    @Query("UPDATE users SET isDeleted = :isDeleted WHERE userId = :userId")
    suspend fun markUserAsDeleted(userId: String, isDeleted: Boolean = true)

    @Query("SELECT * FROM users WHERE isEdited = 1 OR isDeleted = 1 ORDER BY updatedAt DESC")
    suspend fun getPendingSyncUsers(): List<User>

    @Query("SELECT * FROM users LIMIT 1")
    fun getSingleUserSync(): User?
}