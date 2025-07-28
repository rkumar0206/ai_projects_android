package com.rtb.ai.projects.util

import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.rtb.ai.projects.util.constant.Constants.GEMINI_MODEL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GeminiAiHelper @Inject constructor(
    private val client: Client
) {

    suspend fun getPromptTextResult(prompt: String): Result<String?> {

        return try {
            if (prompt.isBlank()) {
                Result.failure(IllegalArgumentException("Prompt cannot be empty"))
            } else {

                withContext(Dispatchers.IO) {

                    val response =
                        client.models.generateContent(
                            GEMINI_MODEL,
                            prompt,
                            GenerateContentConfig.builder()
                                .responseMimeType("text/plain")
                                .build()
                        )

                    Result.success(response.text())
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    }

}