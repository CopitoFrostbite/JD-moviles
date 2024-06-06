package com.example.app1.data.repository

import android.content.Context
import com.example.app1.data.local.UserDao
import com.example.app1.data.model.User
import com.example.app1.data.remote.JournalApiService
import com.example.app1.utils.NetworkUtils
import com.example.app1.utils.PreferencesHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val api: JournalApiService,
    private val userDao: UserDao,
    private val context: Context
) {
    suspend fun registerUser(user: User): Response<User> {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            val response = api.registerUser(user)
            if (response.isSuccessful) {
                response.body()?.let {
                    userDao.clearUsers() // Clear existing user
                    userDao.insertUser(it)
                    PreferencesHelper.saveUserId(context, it.userId) // Save logged in user's ID
                }
            }
            response
        } else {
            Response.error(503, "No network available".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }

    suspend fun loginUser(email: String, password: String): Response<User> {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            val response = api.loginUser(mapOf("email" to email, "password" to password))
            if (response.isSuccessful) {
                response.body()?.let {
                    userDao.clearUsers() // Clear existing user
                    userDao.insertUser(it)
                    PreferencesHelper.saveUserId(context, it.userId) // Save logged in user's ID
                }
            }
            response
        } else {
            val user = userDao.authenticate(email, password)
            if (user != null) {
                PreferencesHelper.saveUserId(context, user.userId) // Save logged in user's ID
                Response.success(user)
            } else {
                Response.error(401, "Authentication failed".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        }
    }

    suspend fun getUserById(userId: Int): User? {
        return try {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val response = api.getUserById(userId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        userDao.clearUsers() // Clear existing user
                        userDao.insertUser(it)
                        it
                    }
                } else {
                    userDao.getUserById(userId)
                }
            } else {
                userDao.getUserById(userId)
            }
        } catch (e: Exception) {
            userDao.getUserById(userId)
        }
    }

    suspend fun syncData() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val userId = PreferencesHelper.getUserId(context)
            if (userId != -1) {
                val localUser = userDao.getUserById(userId)
                localUser?.let {
                    // Sync the local user with the server
                    api.updateUser(it.userId, it)
                }
            }
        }
    }
}