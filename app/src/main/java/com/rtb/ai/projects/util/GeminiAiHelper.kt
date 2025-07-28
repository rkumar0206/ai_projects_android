package com.rtb.ai.projects.util

import com.google.genai.Client
import com.google.genai.types.Blob
import com.google.genai.types.Candidate
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.rtb.ai.projects.util.constant.Constants.GEMINI_IMAGE_MODEL
import com.rtb.ai.projects.util.constant.Constants.GEMINI_MODEL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Optional
import java.util.function.Function
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

    suspend fun generateImageByPrompt(prompt: String): Result<Optional<ByteArray?>?> {

        return try {
            if (prompt.isBlank()) {
                Result.failure(IllegalArgumentException("Prompt cannot be empty"))
            } else {

                withContext(Dispatchers.IO) {

                    val config = GenerateContentConfig.builder()
                        .responseModalities(mutableListOf<String?>("TEXT", "IMAGE"))
                        .build()

                    val content = Content.fromParts(Part.fromText(prompt))

                    val response = client.models.generateContent(
                        GEMINI_IMAGE_MODEL,
                        content,
                        config
                    )

                    val candidateOpt: Optional<Candidate?> = response.candidates()
                        .flatMap<Candidate?>(Function { candidates: MutableList<Candidate?>? ->
                            candidates!!.stream().findFirst()
                        })

                    if (candidateOpt.isEmpty) {
                        return@withContext Result.failure(RuntimeException("No candidate found"))
                    }

                    val candidate: Candidate = candidateOpt.get()
                    val contentOpt: Optional<Content?> = candidate.content()

                    if (contentOpt.isEmpty) {
                        println("No content found.")
                        return@withContext Result.failure(RuntimeException("No content found"))
                    }

                    val parts: MutableList<Part> =
                        contentOpt.get().parts().orElse(emptyList<Part>())

                    for (part in parts) {
                        if (part.inlineData().isPresent) {
                            val blob: Blob = part.inlineData().get()
                            return@withContext Result.success(blob.data())
                        }
                    }

                    return@withContext Result.failure(RuntimeException("No image found"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}