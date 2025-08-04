package com.rtb.ai.projects.data.repository

import android.util.Log
import com.rtb.ai.projects.data.local.db.dao.AIImageDao
import com.rtb.ai.projects.data.model.AIImage
import com.rtb.ai.projects.util.constant.ImageCategoryTag
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Typically, repositories are singletons
class AIImageRepository @Inject constructor(private val aiImageDao: AIImageDao) {

    suspend fun insertImage(image: AIImage): Long {
        return aiImageDao.insertImage(image)
    }

    suspend fun updateImage(image: AIImage) {
        aiImageDao.updateImage(image)
    }

    suspend fun deleteImageAndFile(image: AIImage) {

        // 1. Delete the image file from internal storage (logic from previous examples)
        image.imageFilePath.let { filePath ->
            // ... file deletion logic using context (you'd need to pass context or handle it differently)
            try {
                val imageFile = File(filePath)
                if (imageFile.exists()) {
                    if (imageFile.delete()) {
                        Log.d("ImageDeletion", "Image file deleted: $filePath")
                    } else {
                        Log.w("ImageDeletion", "Failed to delete image file: $filePath")
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageDeletion", "Error deleting image file $filePath: ${e.message}", e)
            }
        }

        aiImageDao.deleteImage(image)
    }

    fun getLastSavedImage(tag: ImageCategoryTag) : Flow<AIImage?> {
        return aiImageDao.getLastSavedImage(tag.name)
    }

    suspend fun deleteImageById(imageId: Long) {
        aiImageDao.deleteImageById(imageId)
    }

    fun getImageById(imageId: Long): Flow<AIImage?> {
        return aiImageDao.getImageById(imageId)
    }

    fun getAllImages(): Flow<List<AIImage>> {
        return aiImageDao.getAllImages()
    }

    fun getAIIMageByImagePrompt(promptQuery: String) = aiImageDao.getAIIMageByImagePrompt(promptQuery)

    fun searchImagesByPrompt(promptQuery: String): Flow<List<AIImage>> {
        return aiImageDao.searchImagesByPrompt(promptQuery)
    }

    fun getImagesByTag(tag: ImageCategoryTag): Flow<List<AIImage>> {
        return aiImageDao.getImagesByTag(tag.name)
    }

    fun getImageByPath(imagePath: String): Flow<AIImage?> {
        return aiImageDao.getImageByPath(imagePath)
    }
}