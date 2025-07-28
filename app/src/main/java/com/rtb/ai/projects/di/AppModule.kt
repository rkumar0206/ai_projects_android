package com.rtb.ai.projects.di

import com.google.genai.Client
import com.google.gson.Gson
import com.rtb.ai.projects.BuildConfig
import com.rtb.ai.projects.util.GeminiAiHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGenerativeAIClient(): Client {

        if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
            throw IllegalStateException("GEMINI_API_KEY not found in app's BuildConfig.")
        }

        return Client.builder()
            .apiKey(BuildConfig.GEMINI_API_KEY)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiAiHelper(
        client: Client
    ) : GeminiAiHelper {
        return GeminiAiHelper(client)
    }

}