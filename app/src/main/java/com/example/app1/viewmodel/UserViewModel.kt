package com.example.app1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.app1.data.model.User
import com.example.app1.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

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
}