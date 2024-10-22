package com.example.app1.data.repository

import androidx.lifecycle.LiveData
import com.example.app1.data.local.ImageDao
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.model.Image
import retrofit2.Response
import javax.inject.Inject


class ImageRepository @Inject constructor(
    private val api: JournalApiService,
    private val imageDao: ImageDao
) {
    suspend fun addImageToEntry(entryId: String, image: Image): Response<Image> {
        return api.addImageToEntry(entryId, image)
    }

    fun getImagesByEntryId(entryId: String): LiveData<List<Image>> {
        return imageDao.getImagesByEntryId(entryId)
    }

    suspend fun getImagesByEntryIdFromApi(entryId: String): Response<List<Image>> {
        return api.getImagesByEntryId(entryId)
    }
}