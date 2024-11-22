package com.example.app1.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.app1.data.model.User
import com.example.app1.data.repository.UserRepository
import com.example.app1.utils.PreferencesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject


@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _user = MutableLiveData<User?>()
    val user: MutableLiveData<User?> get() = _user
    private val _operationStatus = MutableLiveData<String>()
    val operationStatus: LiveData<String> get() = _operationStatus

    fun createUser(
        username: RequestBody,
        name: RequestBody,
        lastname: RequestBody,
        email: RequestBody,
        password: RequestBody,
        avatarPart: MultipartBody.Part?
    ): LiveData<Response<User>> {
        val result = MutableLiveData<Response<User>>()



        // Llamar al método suspendido en el repositorio desde una corrutina
        viewModelScope.launch(Dispatchers.IO) {
            val response = userRepository.registerUser(username, name, lastname, email, password, avatarPart)
            result.postValue(response)
        }

        return result
    }

    fun updatePassword(userId: String, currentPassword: String, newPassword: String): LiveData<Response<Unit>> {
        val result = MutableLiveData<Response<Unit>>()
        viewModelScope.launch(Dispatchers.IO) {
            val response = userRepository.updatePassword(userId, currentPassword, newPassword)
            result.postValue(response)
        }
        return result
    }


    fun loginUser(email: String, password: String): MutableLiveData<Response<User>?> {
        val result = MutableLiveData<Response<User>?>()
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("UserViewModel", "Login started: email=$email") // Log at start
            try {
                val response = userRepository.loginUser(email, password)
                result.postValue(response)
                if (response.isSuccessful) {
                    Log.d("UserViewModel", "Login successful: user=${response.body()?.username}")
                } else {
                    Log.e("UserViewModel", "Login failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Exception during login", e)
                val errorResponse = Response.error<User>(500,
                    "Error logging in".toResponseBody("text/plain".toMediaTypeOrNull())
                )
                result.postValue(errorResponse)
            }
        }
        return result
    }
    fun updateUserData(updatedUser: User): LiveData<Response<User>> {
        val result = MutableLiveData<Response<User>>()
        viewModelScope.launch(Dispatchers.IO) {
            val response = userRepository.updateUserData(updatedUser)
            if (response.isSuccessful) {
                response.body()?.let { downloadAndSaveProfileImage(it) }
            }
            result.postValue(response)
        }
        return result
    }

    fun updateProfileImage(userId: String, avatarPart: MultipartBody.Part): LiveData<Response<User>> {
        val result = MutableLiveData<Response<User>>()
        viewModelScope.launch(Dispatchers.IO) {
            val response = userRepository.updateProfileImage(userId, avatarPart)
            if (response.isSuccessful) {
                response.body()?.let { user ->

                    downloadAndSaveProfileImage(user)
                }
            }
            result.postValue(response)
        }
        return result
    }
    fun setUser(user: User) {
        _user.value = user
    }

    fun getCurrentUser(): LiveData<User?> = liveData(Dispatchers.IO) {
        val userId = PreferencesHelper.getUserId(getApplication())
        val user = if (userId?.isNotEmpty() == true) {
            userRepository.getUserById(userId)?.also { downloadAndSaveProfileImage(it) }
        } else {
            null
        }
        _user.postValue(user)
        emit(user)
    }

    private suspend fun downloadAndSaveProfileImage(user: User) {
        user.profilePicture?.let { imageUrl ->
            val context = getApplication<Application>().applicationContext
            val localPath = userRepository.downloadAndSaveImage(context, imageUrl, user.userId)
            if (!localPath.isNullOrBlank()) {
                val updatedUser = user.copy(localProfilePicture = localPath)
                userRepository.saveUserToLocal(updatedUser) // Actualiza el usuario local
                _user.postValue(updatedUser) // Actualiza LiveData
            }
        }
    }


    fun createGuestUser() {
        val guestUser = User(
            userId = "guest",
            username = "Guest",
            name = "Guest",
            lastname = "User",
            email = "",
            password = "",
            profilePicture = null
        )
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.saveUserToLocal(guestUser)
            _user.postValue(guestUser)
        }
    }


}