package com.example.app1.data.repository


import com.example.app1.data.local.UserDao
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.model.User
import retrofit2.Response
import javax.inject.Inject


class UserRepository @Inject constructor(
    private val api: JournalApiService,
    private val userDao: UserDao
) {
    suspend fun registerUser(user: User): Response<User> {
        return api.registerUser(user)
    }

    suspend fun loginUser(email: String, password: String): Response<User> {
        return api.loginUser(mapOf("email" to email, "password" to password))
    }

    suspend fun getUserById(userId: Int): User? {
        return userDao.getUserById(userId)
    }
}