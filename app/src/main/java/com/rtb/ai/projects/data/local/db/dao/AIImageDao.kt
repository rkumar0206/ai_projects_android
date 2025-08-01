package com.rtb.ai.projects.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rtb.ai.projects.data.model.AIImage // Your AIImage entity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: AIImage): Long

    @Update
    suspend fun updateImage(image: AIImage)

    @Delete
    suspend fun deleteImage(image: AIImage)

    @Query("DELETE FROM aiImages WHERE id = :imageId")
    suspend fun deleteImageById(imageId: Long)

    @Query("SELECT * FROM aiImages WHERE id = :imageId")
    fun getImageById(imageId: Long): Flow<AIImage?> // Use Flow for observable queries

    @Query("SELECT * FROM aiImages ORDER BY generatedAt DESC")
    fun getAllImages(): Flow<List<AIImage>>

    @Query("SELECT * FROM aiImages WHERE imagePrompt = :promptQuery LIMIT 1")
    fun getAIIMageByImagePrompt(promptQuery: String): Flow<AIImage?>

    @Query("SELECT * FROM aiImages WHERE imagePrompt LIKE '%' || :promptQuery || '%' ORDER BY generatedAt DESC")
    fun searchImagesByPrompt(promptQuery: String): Flow<List<AIImage>>

    @Query("SELECT * FROM aiImages WHERE tag = :tag ORDER BY generatedAt DESC")
    fun getImagesByTag(tag: String): Flow<List<AIImage>>

    @Query("SELECT * FROM aiImages WHERE imageFilePath = :imagePath LIMIT 1")
    fun getImageByPath(imagePath: String): Flow<AIImage?>

    @Query("SELECT * FROM aiImages ORDER BY generatedAt DESC LIMIT 1")
    fun getLastSavedImage(): Flow<AIImage?>
}