package com.rtb.ai.projects.data.model // Or your preferred package for data models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rtb.ai.projects.data.local.db.converter.StringListConverter // Assuming you create this

@Entity(tableName = "recipes",
    indices = [Index(value = ["recipeName"], unique = true)])
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Auto-generated primary key

    val recipeName: String,
    var imageFilePath: String?, // e.g., "/data/user/0/com.rtb.ai.projects/files/images/recipe_123.jpg"
    var imagePrompt: String?,

    val yield: String?, // e.g., "4 servings"
    val prepTime: String?, // e.g., "15 minutes"
    val cookTime: String?, // e.g., "30 minutes"

    // Storing lists as a single string (e.g., JSON or delimited)
    // Alternatively, use a TypeConverter for List<String>
    @TypeConverters(StringListConverter::class)
    val ingredients: List<String>?,

    @TypeConverters(StringListConverter::class)
    val instructions: List<String>?,

    val regionFilter: String?, // Store the filter used to generate this, if applicable
    val ingredientsFilter: String?, // Store the filter used, if applicable
    val otherConsiderationsFilter: String?, // Store the filter used, if applicable

    val generatedAt: Long = System.currentTimeMillis() // Timestamp of when it was saved
)


