package com.example.app1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.app1.data.model.Image

@Dao
interface ImageDao {

    // Inserción de una sola imagen
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: Image)

    // Inserción masiva de imágenes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<Image>)


    // Actualización de una imagen específica
    @Update
    suspend fun updateImage(image: Image)

    // Obtener todas las imágenes de un journal
    @Query("SELECT * FROM images WHERE journalId = :journalId AND isDeleted = 0")
    fun getImagesByJournalId(journalId: String): LiveData<List<Image>>

    @Query("UPDATE images SET filePath = :filePath WHERE imageId = :imageId")
    suspend fun updateFilePath(imageId: String, filePath: String)

    // Marcar una imagen como editada
    @Query("UPDATE images SET isEdited = :isEdited WHERE imageId = :imageId")
    suspend fun markImageAsEdited(imageId: String, isEdited: Boolean = true)

    // Eliminar una imagen específica
    @Query("DELETE FROM images WHERE imageId = :imageId")
    suspend fun deleteImageById(imageId: String)

    // Obtener imágenes con cambios pendientes
    @Query("SELECT * FROM images WHERE isEdited = 1 OR isDeleted = 1")
    suspend fun getPendingSyncImages(): List<Image>

    @Query("UPDATE images SET isDeleted = 1 WHERE imageId = :imageId")
    suspend fun markImageAsDeleted(imageId: String)
}