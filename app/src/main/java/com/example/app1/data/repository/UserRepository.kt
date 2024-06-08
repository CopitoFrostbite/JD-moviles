package com.example.app1.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.app1.data.local.UserDao
import com.example.app1.data.model.User
import com.example.app1.data.remote.JournalApiService
import com.example.app1.utils.NetworkUtils
import com.example.app1.utils.PreferencesHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val api: JournalApiService,
    private val userDao: UserDao,
    private val context: Context
) {
    suspend fun registerUser(
        username: String,
        name: String,
        lastname: String,
        email: String,
        password: String,
        avatarUri: Uri?
    ): Response<User> {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val usernamePart = RequestBody.create("text/plain".toMediaTypeOrNull(), username)
                val namePart = RequestBody.create("text/plain".toMediaTypeOrNull(), name)
                val lastnamePart = RequestBody.create("text/plain".toMediaTypeOrNull(), lastname)
                val emailPart = RequestBody.create("text/plain".toMediaTypeOrNull(), email)
                val passwordPart = RequestBody.create("text/plain".toMediaTypeOrNull(), password)

                val avatarPart = avatarUri?.let {
                    val file = uriToFile(it)
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("avatar", file.name, requestFile)
                }

                val response = api.registerUser(avatarPart, usernamePart, namePart, lastnamePart, emailPart, passwordPart)
                response
            } catch (e: Exception) {
                Log.e("UserRepository", "Error de registro", e)
                Response.error(500, "Error during registration".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } else {
            Response.error(503, "No network available".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
    }

    suspend fun loginUser(email: String, password: String): Response<User> {
        return if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = api.loginUser(mapOf("email" to email, "password" to password))
                if (response.isSuccessful) {
                    response.body()?.let {
                        userDao.clearUsers() // Clear existing users
                        userDao.insertUser(it)
                        PreferencesHelper.saveUserId(context, it.userId) // Save logged in user's ID
                    }
                }
                response
            } catch (e: Exception) {
                Response.error(500, "Error during login".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
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
                        userDao.clearUsers() // Clear existing users
                        userDao.insertUser(it)
                        return it
                    }
                }
                userDao.getUserById(userId)
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

    fun getCurrentUser(): LiveData<User?> {
        val result = MutableLiveData<User?>()
        val userId = PreferencesHelper.getUserId(context)
        if (userId != -1) {
            result.postValue(userDao.getUserByIdSync(userId))
        } else {
            result.postValue(null)
        }
        return result
    }

    private fun uriToFile(uri: Uri): File {
        val contentResolver = context.contentResolver
        val fileName = getFileName(uri)
        val tempFile = File(context.cacheDir, fileName)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    }

    private fun getFileName(uri: Uri): String {
        var name = "temp_file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
