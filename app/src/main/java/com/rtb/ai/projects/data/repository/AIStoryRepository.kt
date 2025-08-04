package com.rtb.ai.projects.data.repository

import com.rtb.ai.projects.data.local.db.dao.AIStoryDao
import com.rtb.ai.projects.data.model.AIStory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // If using Hilt for dependency injection
class AIStoryRepository @Inject constructor( // If using Hilt
    private val aiStoryDao: AIStoryDao
) {

    suspend fun insertStory(story: AIStory): Long {

        return aiStoryDao.insertStory(story)
    }

    suspend fun updateStory(story: AIStory) {
        aiStoryDao.updateStory(story)
    }

    suspend fun deleteStory(story: AIStory) {
        aiStoryDao.deleteStory(story)
    }

    suspend fun deleteStoryById(storyId: Long) {
        aiStoryDao.deleteStoryById(storyId)
    }

    fun getStoryById(storyId: Long): Flow<AIStory?> {
        return aiStoryDao.getStoryById(storyId)
    }

    fun getStoryByTitle(title: String): Flow<AIStory?> {
        return aiStoryDao.getStoryByTitle(title)
    }

    fun getLastSavedRepository() = aiStoryDao.getLastSavedRepository()

    fun getAllStories(): Flow<List<AIStory>> {
        return aiStoryDao.getAllStories()
    }

    fun searchStories(query: String): Flow<List<AIStory>> {
        return aiStoryDao.searchStories(query)
    }

    suspend fun clearAllStories() {
        aiStoryDao.clearAllStories()
    }
}