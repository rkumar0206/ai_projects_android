package com.rtb.ai.projects.di // Or your preferred package for DI modules

import android.content.Context
import androidx.room.Room
import com.rtb.ai.projects.data.local.db.AppDatabase
import com.rtb.ai.projects.data.local.db.dao.AIImageDao
import com.rtb.ai.projects.data.local.db.dao.RecipeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Provides instances that live as long as the application
object DatabaseModule {

    @Provides
    @Singleton // Ensures only one instance of AppDatabase is created
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_db" // Same database name as in your AppDatabase companion object
        )
            .build()
    }

    @Provides
    @Singleton // Often DAOs are also singletons, tied to the single AppDatabase instance
    fun provideRecipeDao(appDatabase: AppDatabase): RecipeDao {
        return appDatabase.recipeDao()
    }

    @Provides
    fun provideAIImageDao(appDatabase: AppDatabase): AIImageDao {
        return appDatabase.aiImageDao()
    }
}

