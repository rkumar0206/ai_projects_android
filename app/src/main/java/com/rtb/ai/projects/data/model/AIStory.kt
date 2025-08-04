package com.rtb.ai.projects.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_stories",
    indices = [Index(value = ["storyTitle"], unique = true)]
)
data class AIStory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Auto-generated primary key
    val storyTitle: String,
    var storyContent: List<ContentItemForDbSaving>?,
    val generatedAt: Long = System.currentTimeMillis() // Timestamp of when it was saved
)
