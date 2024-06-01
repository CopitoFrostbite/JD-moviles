package com.example.app1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.app1.data.model.Image
import com.example.app1.data.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class ImageViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {

    fun addImageToEntry(entryId: Int, image: Image) = liveData(Dispatchers.IO) {
        val response = imageRepository.addImageToEntry(entryId, image)
        emit(response)
    }

    fun getImagesByEntryId(entryId: Int) = liveData(Dispatchers.IO) {
        val images = imageRepository.getImagesByEntryIdFromApi(entryId)
        emit(images)
    }

    fun getLocalImagesByEntryId(entryId: Int) = imageRepository.getImagesByEntryId(entryId)
}