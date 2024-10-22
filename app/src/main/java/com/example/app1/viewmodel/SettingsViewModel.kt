package com.example.app1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.app1.data.model.Settings
import com.example.app1.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun updateSettings(settings: Settings) = liveData(Dispatchers.IO) {
        val response = settingsRepository.updateSettings(settings)
        emit(response)
    }

    fun getSettingsByUserId(userId: String) = liveData(Dispatchers.IO) {
        val settings = settingsRepository.getSettingsByUserIdFromApi(userId)
        emit(settings)
    }

    fun getLocalSettingsByUserId(userId: String) = liveData(Dispatchers.IO) {
        val settings = settingsRepository.getSettingsByUserId(userId)
        emit(settings)
    }
}