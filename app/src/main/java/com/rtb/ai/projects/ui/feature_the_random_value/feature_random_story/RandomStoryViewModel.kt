package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtb.ai.projects.data.model.ContentItem
import com.rtb.ai.projects.data.model.StoryInput
import com.rtb.ai.projects.util.GeminiAiHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.UUID
import javax.inject.Inject

data class RandomStoryUIState(
    val isLoading: Boolean = false,
    val isImageLoading: Boolean = false,
    val storyTitle: String? = null,
    val storyContent: List<ContentItem> = emptyList(),
    val errorMessage: String? = null
) : Serializable

@HiltViewModel
class RandomStoryViewModel @Inject constructor(
    private val geminiAiHelper: GeminiAiHelper,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        const val UI_STATE_KEY = "ui_state_key"
        const val TAG = "RandomStoryViewModel"
    }

    val uiState = savedStateHandle.getStateFlow(UI_STATE_KEY, RandomStoryUIState(isLoading = false))

    private var storyTitle: String? = ""

//    init {
//        viewModelScope.launch {
//            delay(1000)
//            generateRandomStory(StoryInput(length = "short"))
//        }
//    }

    fun generateRandomStory(storyInput: StoryInput? = null) {

        viewModelScope.launch {

            savedStateHandle[UI_STATE_KEY] = RandomStoryUIState(
                isLoading = true,
                errorMessage = null
            )

            geminiAiHelper.getPromptTextResult(generatePrompt(storyInput))
                .onSuccess { result ->

                    if (!result.isNullOrBlank()) {

                        Log.d(TAG, "generateRandomStory: \n$result")

                        val storyOutput = parseStoryOutput(result)
                        savedStateHandle[UI_STATE_KEY] = RandomStoryUIState(
                            isLoading = false,
                            storyTitle = storyTitle,
                            storyContent = storyOutput
                        )
                    } else {
                        showError("Error fetching image prompt")
                    }
                }
                .onFailure { error ->
                    showError(error.message)
                }
        }
    }

    private fun showError(errorMessage: String?) {
        savedStateHandle[UI_STATE_KEY] = RandomStoryUIState(
            isLoading = false,
            errorMessage = errorMessage
        )
    }


    fun parseStoryOutput(story: String): List<ContentItem> {
        val contentItems = mutableListOf<ContentItem>()
        val lines = story.trim().lines()

        val buffer = StringBuilder()
        var currentType: String? = null

        for (line in lines) {
            when {

                line.startsWith("STORY_TITLE:") -> {
                    storyTitle = line.removePrefix("STORY_TITLE:").trim()
                }

                line.startsWith("TEXT_CONTENT:") -> {
                    // Flush previous content block
                    if (currentType == "TEXT" && buffer.isNotBlank()) {
                        contentItems.add(
                            ContentItem.TextContent(
                                itemId = UUID.randomUUID().toString(),
                                text = buffer.toString().trim()
                            )
                        )
                    }
                    buffer.clear()
                    buffer.appendLine(line.removePrefix("TEXT_CONTENT:").trim())
                    currentType = "TEXT"
                }

                line.startsWith("IMAGE_PROMPT:") -> {
                    // Flush previous TEXT
                    if (currentType == "TEXT" && buffer.isNotBlank()) {
                        contentItems.add(
                            ContentItem.TextContent(
                                itemId = UUID.randomUUID().toString(),
                                text = buffer.toString().trim()
                            )
                        )
                        buffer.clear()
                    }

                    // Create ImageContent with placeholder image byte array (to be replaced by real generation logic)
                    val imagePrompt = line.removePrefix("IMAGE_PROMPT:").trim()
                    contentItems.add(
                        ContentItem.ImageContent(
                            itemId = UUID.randomUUID().toString(),
                            image = null, // You can replace this with your image generation output
                            imagePrompt = imagePrompt,
                            imageResId = null
                        )
                    )
                    currentType = "IMAGE"
                }

                else -> {
                    // Append additional lines to current buffer
                    buffer.appendLine(line)
                }
            }
        }

        // Flush final block if needed
        if (currentType == "TEXT" && buffer.isNotBlank()) {
            contentItems.add(
                ContentItem.TextContent(
                    itemId = UUID.randomUUID().toString(),
                    text = buffer.toString().trim()
                )
            )
        }

        return contentItems
    }

    fun generateImageForItem(itemId: String, prompt: String) {
        savedStateHandle[UI_STATE_KEY] = uiState.value.copy(isImageLoading = true)
        viewModelScope.launch {
            Log.d(
                TAG,
                "generateImageForItem: Generating image for item $itemId with prompt: $prompt"
            )
            geminiAiHelper.generateImageByPrompt(prompt)
                .onSuccess { result ->
                    if (result != null && result.isPresent) {
                        updateImageInStoryContent(itemId, result.get())
                    } else {
                        Log.e(
                            TAG,
                            "generateImageForItem: Failed to generate image for item $itemId"
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(
                        TAG,
                        "generateImageForItem: Error for item $itemId: ${error.message}",
                        error
                    )
                }
        }
    }

    private fun updateImageInStoryContent(itemId: String, imageBytes: ByteArray) {
        val currentState = uiState.value
        val updatedContent = currentState.storyContent.map { item ->
            if (item is ContentItem.ImageContent && item.itemId == itemId) {
                item.copy(image = imageBytes, imageResId = null) // Clear placeholder
            } else {
                item
            }
        }
        savedStateHandle[UI_STATE_KEY] =
            currentState.copy(storyContent = updatedContent, isImageLoading = false)
        Log.d(TAG, "updateImageInStoryContent: Updated image for item $itemId")
    }


    private fun generatePrompt(storyInput: StoryInput?): String {

        Log.d(TAG, "generatePrompt: story input: $storyInput")

        var storyPrompt = """
    You are an expert Story Writer and Narrative Architect, known for your exceptional creativity, literary precision, and deep understanding of narrative structures across diverse genres. Your role is to craft original, vivid, and structured storytelling content optimized for programmatic rendering—particularly within dynamic user interfaces that utilize structured models like ContentItem (i.e., TextContent and ImageContent).

    Story Generation Guidelines:

    1. User Input Parameters:
    Leverage the following parameters (if provided) to guide the story. If any are omitted, make creative, coherent assumptions:
    - Genre (e.g., Fantasy, Sci-Fi, Mystery, Romance, Thriller, Horror, Historical Fiction, Slice-of-Life)
    - Target Audience (e.g., Children, Young Adult, Adult)
    - Core Premise: Concise plot idea or concept
    - Key Elements: protagonist, antagonist, setting/time/place, tone, themes, conflict
    - Length/Scope (e.g., short story, specific scene, synopsis, backstory, chapter outline)
    - Desired Output Format (e.g., narrative prose, bulleted summary, dialogue)
    - Language (e.g., English, Spanish, French, German, Italian, Japanese, Hindi)

    2. Creative Autonomy:
    If any of the above are missing, use your storytelling expertise to fill the gaps intelligently and consistently, ensuring the story remains coherent, engaging, and appropriate to the inferred genre and audience.

    Output Requirements:

    You must generate the narrative as a sequence of modular content blocks, suitable for conversion to the following structure:
    sealed class ContentItem {
        data class TextContent(val itemId: String, val text: String) : ContentItem()
        data class ImageContent(val itemId: String, val image: ByteArray? = null, @DrawableRes val imageResId: Int? = null) : ContentItem()
    }

    Output Format:
    1. Break the story into logical narrative segments (~2–3 paragraphs each).
    2. After three key narrative points, insert an AI-friendly image prompt as a visually descriptive block (meant to be mapped to ImageContent).
    3. Clearly label each content block like so:
       STORY_TITLE: [Story title here]
       TEXT_CONTENT: [Text here]
       IMAGE_PROMPT: [Image description here for AI model like Midjourney, DALL·E, or Stable Diffusion]
    4. The response should only contain the labels mentioned

    Example:
    TEXT_CONTENT: In a remote Martian outpost, biologist Elara scans the rust-colored horizon for signs of microbial life. The silence is deep, almost reverent—a fragile peace before the storm.
    IMAGE_PROMPT: A lone astronaut standing on a windswept Martian ridge, red dust swirling around, distant outpost buildings visible under a dusky alien sky.
    TEXT_CONTENT: Her discovery—a hidden cavern system teeming with bioluminescent fungi—could rewrite humanity’s understanding of life in the universe.
    TEXT_CONTENT: But not everyone wants the truth exposed. Shadows move in the dust, and her transmissions are being jammed.
    Image Prompt Criteria:
    Each image prompt should be highly descriptive, imaginative, and cinematic. Represent a key story moment, location, or character. Follow visual AI prompt standards: subject, environment, mood, and art style (e.g., gritty digital painting, whimsical watercolor).
""".trimIndent()

        storyPrompt += "\nUser Inputs: $storyInput"

        Log.d(TAG, "generatePrompt: prompt\n$storyPrompt")

        return storyPrompt
    }
}