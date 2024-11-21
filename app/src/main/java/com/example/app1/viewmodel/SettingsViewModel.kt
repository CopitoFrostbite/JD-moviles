package com.example.app1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.app1.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {


    fun getLocalSettingsByUserId(userId: String) = liveData(Dispatchers.IO) {
        val settings = settingsRepository.getSettingsByUserId(userId)
        emit(settings)
    }
}