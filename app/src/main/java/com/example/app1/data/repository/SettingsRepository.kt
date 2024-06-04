package com.example.app1.data.repository


import com.example.app1.data.local.SettingsDao
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.model.Settings
import retrofit2.Response
import javax.inject.Inject


class SettingsRepository @Inject constructor(
    private val api: JournalApiService,
    private val settingsDao: SettingsDao
) {
    suspend fun updateSettings(settings: Settings): Response<Settings> {
        return api.updateSettings(settings)
    }

    suspend fun getSettingsByUserId(userId: Int): Settings? {
        return settingsDao.getSettingsByUserId(userId)
    }

    suspend fun getSettingsByUserIdFromApi(userId: Int): Response<Settings> {
        return api.getSettingsByUserId(userId)
    }
}