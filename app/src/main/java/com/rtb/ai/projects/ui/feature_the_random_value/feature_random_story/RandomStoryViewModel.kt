package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rtb.ai.projects.data.model.AIImage
import com.rtb.ai.projects.data.model.AIStory
import com.rtb.ai.projects.data.model.ContentItem
import com.rtb.ai.projects.data.model.ContentItemForDbSaving
import com.rtb.ai.projects.data.model.StoryInput
import com.rtb.ai.projects.data.repository.AIImageRepository
import com.rtb.ai.projects.data.repository.AIStoryRepository
import com.rtb.ai.projects.util.AppUtil
import com.rtb.ai.projects.util.AppUtil.retrieveImageAsByteArray
import com.rtb.ai.projects.util.AppUtil.saveByteArrayToInternalFile
import com.rtb.ai.projects.util.AppUtil.saveToCacheFile
import com.rtb.ai.projects.util.GeminiAiHelper
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story.util.StoryPdfGenerator
import com.rtb.ai.projects.util.constant.Constants
import com.rtb.ai.projects.util.constant.Constants.LOADING
import com.rtb.ai.projects.util.constant.Constants.TYPE_IMAGE
import com.rtb.ai.projects.util.constant.Constants.TYPE_TEXT
import com.rtb.ai.projects.util.constant.ImageCategoryTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    private val aiStoryRepository: AIStoryRepository,
    private val aiImageRepository: AIImageRepository,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        const val UI_STATE_KEY = "ui_state_key"
        const val IS_STORY_SAVED_TO_DB_KEY = "isStorySavedToDb"
        const val TAG = "RandomStoryViewModel"
    }

    val uiState = savedStateHandle.getStateFlow(UI_STATE_KEY, RandomStoryUIState(isLoading = true))
    val isStorySavedToDb: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(IS_STORY_SAVED_TO_DB_KEY, false)

    private var storyTitle: String? = ""
    private val imageGenerationInProgress = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            delay(1000)

            val lastSavedStory = aiStoryRepository.getLastSavedRepository().first()

            if (lastSavedStory != null) {
                updateUiBySavedStory(lastSavedStory)
            } else {
                generateRandomStory(StoryInput(length = "500"))
            }
        }
    }

    private fun updateUiBySavedStory(story: AIStory) {

        savedStateHandle[UI_STATE_KEY] = RandomStoryUIState(
            isLoading = false,
            storyTitle = story.storyTitle,
            storyContent = getStoryContentItemListFromStory(story)
        )
        savedStateHandle[IS_STORY_SAVED_TO_DB_KEY] = true
    }

    private fun getStoryContentItemListFromStory(story: AIStory): List<ContentItem> {

        val contentItems = mutableListOf<ContentItem>()

        story.storyContent?.sortedBy { it.position }?.forEach { contentItemForDbSaving ->

            when (contentItemForDbSaving.type) {
                TYPE_TEXT -> {
                    contentItems.add(
                        ContentItem.TextContent(
                            itemId = contentItemForDbSaving.itemId,
                            text = contentItemForDbSaving.text ?: ""
                        )
                    )
                }

                TYPE_IMAGE -> {
                    contentItems.add(
                        ContentItem.ImageContent(
                            itemId = contentItemForDbSaving.itemId,
                            imageFilePath = contentItemForDbSaving.imageFilePath,
                            imagePrompt = contentItemForDbSaving.imagePrompt
                        )
                    )
                }
            }
        }
        return contentItems
    }

    // ---------------------------------- DB -----------------------------------

    fun allStories(): StateFlow<List<AIStory>> = aiStoryRepository.getAllStories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Keep upstream flow active for 5s after last subscriber gone
            initialValue = emptyList()
        )

    fun saveOrDeleteStoryFromDbBasedOnBookmarkMenuPressed() {

        viewModelScope.launch {

            if (!isStorySavedToDb.value) {

                Log.d(
                    TAG,
                    "saveOrDeleteStoryFromDbBasedOnBookmarkMenuPressed: Saving story"
                )

                val storyUiStateValue = uiState.value

                if (!storyUiStateValue.storyTitle.isNullOrBlank() && storyUiStateValue.storyTitle != LOADING) {

                    val story = AIStory(
                        storyTitle = storyUiStateValue.storyTitle,
                        storyContent = null,
                        generatedAt = System.currentTimeMillis()
                    )

                    insertStoryAndItsImages(story, storyUiStateValue.storyContent)
                }
            } else {

                if (!uiState.value.storyTitle.isNullOrBlank() && uiState.value.storyTitle != LOADING) {

                    val storyToDelete =
                        aiStoryRepository.getStoryByTitle(uiState.value.storyTitle!!).first()

                    if (storyToDelete != null) {

                        // delete story images
                        deleteStoryImages(storyToDelete)
                        aiStoryRepository.deleteStory(storyToDelete)
                        savedStateHandle[IS_STORY_SAVED_TO_DB_KEY] = false
                    }
                }
            }
        }

    }

    fun getAndShowStoryById(storyId: Long) {
        viewModelScope.launch {
            val story = aiStoryRepository.getStoryById(storyId).first()
            if (story != null) {
                updateUiBySavedStory(story)
            }
        }
    }

    private suspend fun deleteStoryImages(storyToDelete: AIStory) {

        if (storyToDelete.storyContent != null) {

            storyToDelete.storyContent
                ?.filter { it.type == TYPE_IMAGE }
                ?.forEach { imageContent ->
                    if (!imageContent.imageFilePath.isNullOrBlank() && !imageContent.imagePrompt.isNullOrBlank()) {

                        val image =
                            aiImageRepository.getAIIMageByImagePrompt(imageContent.imagePrompt)
                                .first()

                        image?.let {
                            aiImageRepository.deleteImageAndFile(image)
                        }
                    }
                }
        }
    }

    private suspend fun insertStoryAndItsImages(
        story: AIStory,
        storyContent: List<ContentItem>
    ) {

        val context = getApplication<Application>().applicationContext

        val contentItemForDb: ArrayList<ContentItemForDbSaving> = arrayListOf()

        storyContent
            .forEachIndexed { i, content ->

                if (content is ContentItem.TextContent) {

                    val textContent: ContentItem.TextContent = content
                    contentItemForDb.add(
                        ContentItemForDbSaving(
                            itemId = textContent.itemId,
                            type = TYPE_TEXT,
                            position = i,
                            text = textContent.text
                        )
                    )
                }

                if (content is ContentItem.ImageContent && !content.imageFilePath.isNullOrBlank() && !content.imagePrompt.isNullOrBlank()) {

                    val byteArrayInCacheDir = retrieveImageAsByteArray(content.imageFilePath)

                    val savedImageFilePath =
                        byteArrayInCacheDir?.saveByteArrayToInternalFile(
                            context,
                            "story_images",
                            fileName = content.itemId.replace("-", "_")
                        )

                    if (savedImageFilePath != null) {

                        val aiImage = AIImage(
                            imagePrompt = content.imagePrompt,
                            imageFilePath = savedImageFilePath,
                            aiModel = Constants.GEMINI_IMAGE_MODEL,
                            tag = ImageCategoryTag.IMAGE_GENERATION_FOR_STORY.name,
                            generatedAt = System.currentTimeMillis()
                        )

                        aiImageRepository.insertImage(aiImage)

                        contentItemForDb.add(
                            ContentItemForDbSaving(
                                itemId = content.itemId,
                                type = TYPE_IMAGE,
                                position = i,
                                imageFilePath = savedImageFilePath,
                                imagePrompt = content.imagePrompt
                            )
                        )
                    }
                }
            }

        story.storyContent = contentItemForDb
        aiStoryRepository.insertStory(story)
        savedStateHandle[IS_STORY_SAVED_TO_DB_KEY] = true
    }

    // -------------------------------------------------------------------------

    fun generateRandomStory(storyInput: StoryInput? = null) {

        viewModelScope.launch {

            savedStateHandle[UI_STATE_KEY] = RandomStoryUIState(
                isLoading = true,
                errorMessage = null
            )
            savedStateHandle[IS_STORY_SAVED_TO_DB_KEY] = false

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
                            imageFilePath = null, // You can replace this with your image generation output
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

        if (imageGenerationInProgress.contains(itemId)) {
            Log.d(TAG, "Image generation already in progress for item $itemId. Skipping.")
            return
        }

        savedStateHandle[UI_STATE_KEY] = uiState.value.copy(isImageLoading = true)
        viewModelScope.launch {

            delay(800)

            Log.d(
                TAG,
                "generateImageForItem: Generating image for item $itemId with prompt: $prompt"
            )

            val aiImageFromDb = aiImageRepository.getAIIMageByImagePrompt(prompt).first()

            if (aiImageFromDb == null) {
                imageGenerationInProgress.add(itemId)
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
                            .also {
                                imageGenerationInProgress.remove(itemId)
                            }
                    }
                    .onFailure { error ->
                        Log.e(
                            TAG,
                            "generateImageForItem: Error for item $itemId: ${error.message}",
                            error
                        )
                        imageGenerationInProgress.remove(itemId)
                        savedStateHandle[UI_STATE_KEY] = uiState.value.copy(isImageLoading = false)
                    }
            } else {
                val imageBytes = retrieveImageAsByteArray(aiImageFromDb.imageFilePath)
                imageBytes?.let {
                    updateImageInStoryContent(itemId, it)
                }
            }
        }
    }

    private fun updateImageInStoryContent(itemId: String, imageBytes: ByteArray) {
        val currentState = uiState.value
        val updatedContent = currentState.storyContent.map { item ->
            if (item is ContentItem.ImageContent && item.itemId == itemId) {
                item.copy(
                    imageFilePath = imageBytes.saveToCacheFile(
                        getApplication<Application>().applicationContext,
                        "cached_story_images_$itemId",
                        ".png"
                    ), imageResId = null
                )
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

        //storyPrompt += "\nUser Inputs: $storyInput"

        storyInput?.let { input ->
            storyPrompt += "\n\n--- User Provided Inputs ---"
            input.genre?.let { storyPrompt += "\nGenre: $it" }
            input.targetAudience?.let { storyPrompt += "\nTarget Audience: $it" }
            input.corePremise?.let { storyPrompt += "\nCore Premise: $it" }
            input.keyElements?.let { elements ->
                storyPrompt += "\nKey Elements:"
                elements.protagonist?.let { storyPrompt += "\n  Protagonist: $it" }
                elements.antagonist?.let { storyPrompt += "\n  Antagonist: $it" }
                elements.setting?.let { storyPrompt += "\n  Setting: $it" }
                elements.conflict?.let { storyPrompt += "\n  Conflict: $it" }
                elements.themes?.let { if (it.isNotEmpty()) storyPrompt += "\n  Themes: ${it.joinToString()}" }
                elements.moodTone?.let { storyPrompt += "\n  Mood/Tone: $it" }
            }
            input.length?.let { storyPrompt += "\nLength: $it" }
            input.outputFormat?.let { storyPrompt += "\nOutput Format: $it" }
            input.language?.let { storyPrompt += "\nLanguage: $it" }
            storyPrompt += "\n---------------------------\n"
        } ?: run {
            storyPrompt += "\n\n--- No Specific User Inputs Provided ---"
            storyPrompt += "\nUsing default length: 500 words."
            storyPrompt += "\n-------------------------------------\n"
        }

        Log.d(TAG, "generatePrompt: prompt\n$storyPrompt")

        return storyPrompt
    }

    fun downloadCurrentStoryAsPdf() {

        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext

            StoryPdfGenerator.generatePdf(
                uiState.value.storyTitle,
                uiState.value.storyContent
            )
                ?.onSuccess { outputFilePath ->
                    Log.d(TAG, "downloadCurrentStoryAsPdf: Story downloaded")
                    Toast.makeText(
                        context,
                        "PDF generated successfully at $outputFilePath",
                        Toast.LENGTH_LONG
                    ).show()
                }
                ?.onFailure { e ->
                    Log.d(TAG, "downloadCurrentStoryAsPdf: Story download failed")
                    Toast.makeText(
                        context,
                        "Error writing PDF to file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    override fun onCleared() {
        super.onCleared()

        val isSuccess =
            AppUtil.clearApplicationCache(getApplication<Application>().applicationContext)

        if (isSuccess) {
            Log.d(TAG, "onCleared: Cleared all caches from cache directory")
        } else {
            Log.d(TAG, "onCleared: Unable to clear the caches from cache directory")
        }
    }
}