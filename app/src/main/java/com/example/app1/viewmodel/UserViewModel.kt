package com.example.app1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.work.*
import com.example.app1.data.model.User
import com.example.app1.data.repository.UserRepository
import com.example.app1.workers.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    fun registerUser(user: User) = liveData(Dispatchers.IO) {
        val response = userRepository.registerUser(user)
        emit(response)
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

