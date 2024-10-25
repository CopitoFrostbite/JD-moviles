package com.example.app1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.app1.data.model.Image

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: Image)

    @Query("SELECT * FROM images WHERE journalId = :journalId")
    fun getImagesByEntryId(journalId: String): LiveData<List<Image>>
}