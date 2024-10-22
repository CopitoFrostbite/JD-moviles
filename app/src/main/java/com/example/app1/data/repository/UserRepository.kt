package com.example.app1.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.example.app1.data.local.UserDao
import com.example.app1.data.model.User
import com.example.app1.data.remote.JournalApiService
import com.example.app1.utils.NetworkUtils
import com.example.app1.utils.PreferencesHelper
import kotlinx.coroutines.Dispatchers
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
        Log.d("UserRepository", "Attempting login for email: $email")
        return if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = api.loginUser(mapOf("email" to email, "password" to password))
                Log.d("UserRepository", "API response received: $response")

                if (response.isSuccessful) {
                    val userResponse = response.body()
                    Log.d("UserRepository", "Response body: $userResponse")

                    if (userResponse != null) {
                        Log.d("UserRepository", "Login successful: username=${userResponse.username}, email=${userResponse.email}")

                        // Construir el objeto User con los datos relevantes de la respuesta
                        val user = User(
                            userId = userResponse.userId, // Asegúrate de manejar el ID adecuadamente
                            username = userResponse.username,
                            name = userResponse.name,
                            lastname = userResponse.lastname,
                            email = userResponse.email,
                            password = "",  // No guardes la contraseña en texto plano en una app real
                            profilePicture = userResponse.profilePicture
                        )

                        userDao.clearUsers() // Clear existing users
                        userDao.insertUser(user)
                        PreferencesHelper.saveUserId(context, user.userId) // Save logged in user's ID

                        return Response.success(user)
                    } else {
                        Log.e("UserRepository", "Login failed: Response body is null")
                        return Response.error(500, "Login failed: Response body is null".toResponseBody("text/plain".toMediaTypeOrNull()))
                    }
                } else {
                    Log.e("UserRepository", "Login failed: ${response.errorBody()?.string()}")
                    return response
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Exception during login request", e)
                return Response.error(500, "Error during login".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        } else {
            Log.w("UserRepository", "No network available, attempting local authentication")
            val user = userDao.authenticate(email, password)
            return if (user != null) {
                Log.d("UserRepository", "Local authentication successful: userId=${user.userId}")
                PreferencesHelper.saveUserId(context, user.userId) // Save logged in user's ID
                Response.success(user)
            } else {
                Log.e("UserRepository", "Local authentication failed for email: $email")
                Response.error(401, "Authentication failed".toResponseBody("text/plain".toMediaTypeOrNull()))
            }
        }
    }

    suspend fun getUserById(userId: String): User? {
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
            if (userId != "") {
                val localUser = userId?.let { userDao.getUserById(it) }
                localUser?.let {
                    // Sync the local user with the server
                    api.updateUser(it.userId, it)
                }
            }
        }
    }

    fun getCurrentUser(): LiveData<User?> = liveData(Dispatchers.IO) {
        val userId = PreferencesHelper.getUserId(context)
        val user = if (userId?.isNotEmpty() == true) {
            userDao.getUserByIdSync(userId)
        } else {
            null
        }
        emit(user)
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
