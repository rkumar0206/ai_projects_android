package com.rtb.ai.projects.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "aiImages",
    indices = [Index(value = ["imagePrompt"], unique = true)]
)
data class AIImage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Auto-generated primary key
    val imagePrompt: String,
    val imageFilePath: String,
    val aiModel: String?,
    val tag: String?,
    val generatedAt: Long = System.currentTimeMillis() // Timestamp of when it was saved
)
