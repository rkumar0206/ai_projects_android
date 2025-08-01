package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rtb.ai.projects.data.model.AIImage
import com.rtb.ai.projects.data.repository.AIImageRepository
import com.rtb.ai.projects.util.AppUtil.downloadImage
import com.rtb.ai.projects.util.AppUtil.retrieveImageAsByteArray
import com.rtb.ai.projects.util.AppUtil.saveByteArrayToInternalFile
import com.rtb.ai.projects.util.GeminiAiHelper
import com.rtb.ai.projects.util.constant.Constants
import com.rtb.ai.projects.util.constant.Constants.IMAGE_GENERATION_RANDOM_IMAGE_USING_KEYWORD_TAG
import com.rtb.ai.projects.util.constant.Constants.LOADING
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.UUID
import javax.inject.Inject

data class RandomImageGenerationUIState(
    val isLoading: Boolean = false,
    val imagePrompt: String = "",
    val image: ByteArray? = null,
    val errorMessage: String? = null,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RandomImageGenerationUIState

        if (isLoading != other.isLoading) return false
        if (imagePrompt != other.imagePrompt) return false
        if (!image.contentEquals(other.image)) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + imagePrompt.hashCode()
        result = 31 * result + (image?.contentHashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}

@HiltViewModel
class RandomImageGenerationViewModel @Inject constructor(
    private val aiImageRepository: AIImageRepository,
    private val geminiAiHelper: GeminiAiHelper,
    private val savedStateHandle: SavedStateHandle,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        const val UI_STATE_KEY = "randomImageGenerationUiState"
        const val KEYWORDS_FILTER_KEY =
            "randomImageKeywordsFilter" // If you want to save keywords separately
        const val IS_IMAGE_SAVED_TO_DB_KEY = "isImageSavedToDb"

        // Or include it in UI_STATE_KEY as done in RandomImageGenerationUIState
        private const val TAG = "ImageGenViewModel"
    }

    val uiState: StateFlow<RandomImageGenerationUIState> =
        savedStateHandle.getStateFlow(UI_STATE_KEY, RandomImageGenerationUIState(isLoading = true))
    val keywordsFilter: StateFlow<String> = savedStateHandle.getStateFlow(KEYWORDS_FILTER_KEY, "")
    val isAIImageSavedToDb: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(IS_IMAGE_SAVED_TO_DB_KEY, false)

    init {
        showLastSavedImageOrFetchRandomImageFromAI()
    }

    // ------------------------------- DB methods ---------------------------------

    private fun showLastSavedImageOrFetchRandomImageFromAI() {

        viewModelScope.launch {

            val lastSavedImage = aiImageRepository.getLastSavedImage().first()

            if (lastSavedImage != null) {
                updateUIDataByImage(lastSavedImage)
            } else {
                generateImagePromptAndImage(null)
            }
        }
    }


    val allImages: StateFlow<List<AIImage>> = aiImageRepository.getAllImages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Keep upstream flow active for 5s after last subscriber gone
            initialValue = emptyList()
        )


    fun saveOrDeleteAIImageFromDbBasedOnBookmarkMenuPressed() {

        viewModelScope.launch {

            if (!isAIImageSavedToDb.value) {

                val imageUiStateValue = uiState.value
                if (imageUiStateValue.imagePrompt.isNotBlank() && imageUiStateValue.imagePrompt != LOADING) {

                    val context = getApplication<Application>().applicationContext

                    val filePath = imageUiStateValue.image?.saveByteArrayToInternalFile(
                        context,
                        "ai_generated_images",
                        "${imageUiStateValue.imagePrompt.substring(0, 5)}_${UUID.randomUUID()}.png"
                    )

                    val aiImage = AIImage(
                        imagePrompt = imageUiStateValue.imagePrompt,
                        imageFilePath = filePath ?: "",
                        aiModel = Constants.GEMINI_IMAGE_MODEL,
                        tag = IMAGE_GENERATION_RANDOM_IMAGE_USING_KEYWORD_TAG,
                        generatedAt = System.currentTimeMillis()
                    )

                    aiImageRepository.insertImage(aiImage)
                    savedStateHandle[IS_IMAGE_SAVED_TO_DB_KEY] = true
                }
            } else {

                val imageToDelete =
                    aiImageRepository.getAIIMageByImagePrompt(uiState.value.imagePrompt).first()

                if (imageToDelete != null) {

                    aiImageRepository.deleteImageAndFile(imageToDelete)
                    savedStateHandle[IS_IMAGE_SAVED_TO_DB_KEY] = false
                }
            }
        }

    }

    fun deleteAIImage(image: AIImage) {

        viewModelScope.launch {

            val argImageId = image.id
            val currImageShowing =
                aiImageRepository.getAIIMageByImagePrompt(uiState.value.imagePrompt).first()

            aiImageRepository.deleteImageAndFile(image)

            if (currImageShowing != null && currImageShowing.id == argImageId) {
                savedStateHandle[IS_IMAGE_SAVED_TO_DB_KEY] = false
            }
        }
    }

    fun getAndShowSelectedImage(imageId: Long) {
        viewModelScope.launch {
            val image = aiImageRepository.getImageById(imageId).first()
            if (image != null) {
                updateUIDataByImage(image)
            }
        }
    }

    private fun updateUIDataByImage(image: AIImage) {

        savedStateHandle[UI_STATE_KEY] = RandomImageGenerationUIState(
            isLoading = false,
            imagePrompt = image.imagePrompt,
            image = retrieveImageAsByteArray(image.imageFilePath)
        )
        savedStateHandle[IS_IMAGE_SAVED_TO_DB_KEY] = true
    }


    // ----------------------------------------------------------------------------


    fun generateImagePromptAndImage(keywords: String?) {

        viewModelScope.launch {
            savedStateHandle[UI_STATE_KEY] = RandomImageGenerationUIState(isLoading = true)

            geminiAiHelper.getPromptTextResult(generatePromptForRandomImagePrompt(keywords))
                .onSuccess { result ->

                    if (!result.isNullOrBlank()) {
                        generateImageFromPrompt(result)

                    } else {
                        showError("Error fetching image prompt")
                    }
                }
                .onFailure { error ->
                    showError(error.message)
                }
        }
    }

    private suspend fun generateImageFromPrompt(prompt: String) {

        geminiAiHelper.generateImageByPrompt(prompt)
            .onSuccess { imageResult ->

                if (imageResult != null && imageResult.isPresent) {
                    val image = imageResult.get()
                    savedStateHandle[UI_STATE_KEY] = RandomImageGenerationUIState(
                        isLoading = false,
                        imagePrompt = prompt,
                        image = image
                    )
                    savedStateHandle[IS_IMAGE_SAVED_TO_DB_KEY] = false
                } else {
                    showError("Unable to get the image. Please try again!!")
                }
            }
            .onFailure { error ->
                showError(error.message)
            }
    }


    /**
     * Saves the current generated image to the device's public "Downloads" directory.
     * The image is retrieved from uiState.imageBitmap.
     */
    fun downloadCurrentImage() {

        val context = getApplication<Application>().applicationContext
        if (uiState.value.image == null) {
            viewModelScope.launch {
                Toast.makeText(context, "No image to download", Toast.LENGTH_SHORT).show()
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) { // Perform file operations on IO dispatcher
            val imageByteArray = uiState.value.image
            val fileName =
                "${uiState.value.imagePrompt.substring(0, 5)}_${System.currentTimeMillis()}.png"

            val finalMessage: String = if (imageByteArray?.downloadImage(fileName, context.contentResolver) == true) {
                "Image downloaded successfully"
            } else {
                "Image download failed"
            }

            withContext(Dispatchers.Main) { // Show toast on Main thread
                Toast.makeText(context, finalMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError(errorMessage: String?) {
        savedStateHandle[UI_STATE_KEY] = RandomImageGenerationUIState(
            isLoading = false,
            errorMessage = errorMessage
        )
    }

    private fun generatePromptForRandomImagePrompt(keywords: String?): String {
        val prompt =
            "As an expert AI image prompt generator, your task is to create a single, highly descriptive, and imaginative text prompt suitable for advanced AI art generation models (e.g., Midjourney, DALL-E, Stable Diffusion).\n" +
                    "\n" +
                    "**Key Requirements:**\n" +
                    "1.  **Creativity & Specificity:** Generate a unique and vivid scene description. Include details such as the main subject(s), action, setting, time of day/lighting, mood/atmosphere, and an identifiable art style or aesthetic (e.g., 'cyberpunk', 'renaissance painting', 'photorealistic', 'fantasy art', 'abstract').\n" +
                    "2.  **Keyword Integration (if provided):**\n" +
                    "    *   If the user provides specific keywords, integrate them prominently and naturally into the core concept of the generated image prompt. These keywords should act as central elements or critical modifiers (e.g., a primary subject, a key object, or a dominant mood).\n" +
                    "    *   If no keywords are provided, generate a completely original and varied prompt, exploring diverse subjects, styles, and scenarios without external constraints.\n" +
                    "3.  **Format:** The output must be a single, continuous text string (e.g., a comma-separated list of descriptors or a coherent sentence/phrase), ready to be directly input into an image generator.\n" +
                    "Keywords: " + keywords

        return prompt
    }

}