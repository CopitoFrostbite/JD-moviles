package com.example.app1.data.repository


import com.example.app1.data.local.SettingsDao
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.model.Settings
import javax.inject.Inject


class SettingsRepository @Inject constructor(
    private val api: JournalApiService,
    private val settingsDao: SettingsDao
) {


    suspend fun getSettingsByUserId(userId: String): Settings? {
        return settingsDao.getSettingsByUserId(userId)
    }

}