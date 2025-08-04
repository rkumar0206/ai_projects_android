package com.rtb.ai.projects.data.local.db // Or your preferred package

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rtb.ai.projects.data.local.db.converter.ContentItemForDbListConverter
import com.rtb.ai.projects.data.local.db.converter.StringListConverter
import com.rtb.ai.projects.data.local.db.dao.AIImageDao
import com.rtb.ai.projects.data.local.db.dao.AIStoryDao
import com.rtb.ai.projects.data.local.db.dao.RecipeDao
import com.rtb.ai.projects.data.model.AIImage
import com.rtb.ai.projects.data.model.AIStory
import com.rtb.ai.projects.data.model.Recipe

@Database(
    entities = [Recipe::class, AIImage::class, AIStory::class], // List all your entities here
    version = 1,
    exportSchema = true
)
@TypeConverters(StringListConverter::class, ContentItemForDbListConverter::class) // Register your TypeConverters here
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao // Abstract method to get the DAO
    abstract fun aiImageDao(): AIImageDao
    abstract fun aiStoryDao(): AIStoryDao
}

