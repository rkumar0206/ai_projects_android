package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rtb.ai.projects.util.GeminiAiHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.Serializable
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
    private val geminiAiHelper: GeminiAiHelper,
    private val savedStateHandle: SavedStateHandle,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        const val UI_STATE_KEY = "randomImageGenerationUiState"
        const val KEYWORDS_FILTER_KEY =
            "randomImageKeywordsFilter" // If you want to save keywords separately

        // Or include it in UI_STATE_KEY as done in RandomImageGenerationUIState
        private const val TAG = "ImageGenViewModel"

        // Default prompt prefix or structure, can be configured
        private const val DEFAULT_PROMPT_PREFIX = "Generate an image of: "
    }


    val uiState: StateFlow<RandomImageGenerationUIState> =
        savedStateHandle.getStateFlow(UI_STATE_KEY, RandomImageGenerationUIState())
    val keywordsFilter: StateFlow<String> = savedStateHandle.getStateFlow(KEYWORDS_FILTER_KEY, "")

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
                } else {
                    showError("Unable to get the recipe image. Please try again!!")
                }
            }
            .onFailure { error ->
                showError(error.message)
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