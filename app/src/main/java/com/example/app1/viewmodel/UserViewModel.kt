package com.example.app1.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.app1.data.model.User
import com.example.app1.data.repository.UserRepository
import com.example.app1.workers.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    fun createUser(
        username: String,
        name: String,
        lastname: String,
        email: String,
        password: String,
        avatarUri: Uri?
    ): LiveData<Response<User>> {
        val result = MutableLiveData<Response<User>>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = userRepository.registerUser(username, name, lastname, email, password, avatarUri)
                result.postValue(response)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error viewmodel user", e)
                val errorResponse = Response.error<User>(500, ResponseBody.create("text/plain".toMediaTypeOrNull(), "Error registering user"))
                result.postValue(errorResponse)
            }
        }
        return result
    }

    fun loginUser(email: String, password: String) = liveData(Dispatchers.IO) {
        val response = userRepository.loginUser(email, password)
        emit(response)
    }

    fun getUserById(userId: Int) = liveData(Dispatchers.IO) {
        val user = userRepository.getUserById(userId)
        emit(user)
    }

    fun setUser(user: User) {
        _user.value = user
    }

    fun getCurrentUser(): LiveData<User?> {
        return userRepository.getCurrentUser()
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getApplication()).enqueue(syncRequest)
    }
}