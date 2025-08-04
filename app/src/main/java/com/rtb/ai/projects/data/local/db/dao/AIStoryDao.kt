package com.rtb.ai.projects.data.local.db.dao

import androidx.room.*
import com.rtb.ai.projects.data.model.AIStory
import kotlinx.coroutines.flow.Flow

@Dao
interface AIStoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: AIStory): Long // Returns the new rowId

    @Update
    suspend fun updateStory(story: AIStory)

    @Delete
    suspend fun deleteStory(story: AIStory)

    @Query("DELETE FROM ai_stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: Long)

    @Query("SELECT * FROM ai_stories WHERE id = :storyId")
    fun getStoryById(storyId: Long): Flow<AIStory?>

    @Query("SELECT * FROM ai_stories WHERE storyTitle = :storyTitle")
    fun getStoryByTitle(storyTitle: String): Flow<AIStory?>

    @Query("SELECT * FROM ai_stories ORDER BY id DESC") // Or any other order you prefer
    fun getAllStories(): Flow<List<AIStory>>

    @Query("SELECT * FROM ai_stories ORDER BY generatedAt DESC LIMIT 1")
    fun getLastSavedRepository(): Flow<AIStory?>

    @Query("SELECT * FROM ai_stories WHERE storyTitle LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchStories(query: String): Flow<List<AIStory>>

    @Query("DELETE FROM ai_stories")
    suspend fun clearAllStories()
}
