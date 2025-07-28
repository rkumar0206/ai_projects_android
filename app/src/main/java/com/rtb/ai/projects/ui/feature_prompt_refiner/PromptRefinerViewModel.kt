package com.rtb.ai.projects.ui.feature_prompt_refiner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtb.ai.projects.util.GeminiAiHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data class to hold the entire UI state
data class PromptRefinerUiState(
    val promptInput: String = "",
    val resultText: String = "",
    val isLoading: Boolean = false,
    val isSubmitButtonEnabled: Boolean = true,
    val isCopyButtonVisible: Boolean = false,
    val errorMessage: String? = null // For displaying errors
)

@HiltViewModel
class PromptRefinerViewModel @Inject constructor(
    private val geminiAPIHelper: GeminiAiHelper,
    private val savedStateHandle: SavedStateHandle // For saving/restoring state
) : ViewModel() {

    // --- StateFlow for UI State ---
    private val _uiState = MutableStateFlow(PromptRefinerUiState())
    val uiState: StateFlow<PromptRefinerUiState> = _uiState.asStateFlow()

    // --- Keys for SavedStateHandle ---
    companion object {
        private const val PROMPT_INPUT_KEY = "promptInput"
        private const val RESULT_TEXT_KEY = "resultText"
        private const val IS_LOADING_KEY = "isLoading"
        private const val IS_COPY_BUTTON_VISIBLE_KEY = "isCopyButtonVisible"
        // Submit button enabled state can often be derived, but can be saved if needed
    }

    init {
        // Restore state when ViewModel is created (e.g., after process death)
        val initialPrompt = savedStateHandle.get<String>(PROMPT_INPUT_KEY) ?: ""
        val initialResult = savedStateHandle.get<String>(RESULT_TEXT_KEY) ?: ""
        val initialLoading = savedStateHandle.get<Boolean>(IS_LOADING_KEY) ?: false
        val initialCopyVisible = savedStateHandle.get<Boolean>(IS_COPY_BUTTON_VISIBLE_KEY) ?: false

        _uiState.value = PromptRefinerUiState(
            promptInput = initialPrompt,
            resultText = initialResult,
            isLoading = initialLoading,
            isSubmitButtonEnabled = !initialLoading && initialPrompt.isNotBlank(), // Derive initial state
            isCopyButtonVisible = initialCopyVisible && initialResult.isNotBlank()
        )
    }


    // --- Event Handlers from UI ---

    fun onPromptChanged(newPrompt: String) {
        _uiState.value = _uiState.value.copy(
            promptInput = newPrompt,
            isSubmitButtonEnabled = newPrompt.isNotBlank() && !_uiState.value.isLoading
        )
        savedStateHandle[PROMPT_INPUT_KEY] = newPrompt
    }

    fun onSubmitClicked() {
        if (_uiState.value.promptInput.isBlank() || _uiState.value.isLoading) {
            return // Don't submit if prompt is empty or already loading
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isSubmitButtonEnabled = false,
            errorMessage = null // Clear previous errors
        )
        savedStateHandle[IS_LOADING_KEY] = true

        viewModelScope.launch {
            val prompt = _uiState.value.promptInput
            geminiAPIHelper.getPromptTextResult(generatePrompt(prompt))
                .onSuccess { result ->

                    var refinedPrompt = ""

                    if (!result.isNullOrBlank()) {
                        refinedPrompt = result.replace("Refined Prompt:", "").trim()
                    }

                    _uiState.value = _uiState.value.copy(
                        resultText = refinedPrompt,
                        isLoading = false,
                        isSubmitButtonEnabled = true,
                        isCopyButtonVisible = result?.isNotBlank() ?: false
                    )
                    savedStateHandle[RESULT_TEXT_KEY] = result
                    savedStateHandle[IS_COPY_BUTTON_VISIBLE_KEY] = result?.isNotBlank() ?: false
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        resultText = "", // Clear previous result on error
                        isLoading = false,
                        isSubmitButtonEnabled = true,
                        isCopyButtonVisible = false,
                        errorMessage = error.message ?: "An unknown error occurred"
                    )
                    savedStateHandle[RESULT_TEXT_KEY] = ""
                    savedStateHandle[IS_COPY_BUTTON_VISIBLE_KEY] = false
                }
            savedStateHandle[IS_LOADING_KEY] = false // Ensure loading is reset
        }
    }

    fun onErrorMessageShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun generatePrompt(rawText: String): String {

        return "You are PromptRefiner, an expert in prompt engineering for large language models (LLMs) like GPT and Gemini.\n" +
                "Your job is to improve raw, vague, or unclear prompts. Given a user’s input, you will:\n" +
                "1. Rewrite the prompt in a clear, structured format.\n" +
                "2. Make the intent explicit.\n" +
                "3. Suggest improvements or additional context if needed.\n" +
                "4. Optionally include an input/output example.\n" +
                "5. Use best practices from prompt engineering.\n\n" +
                "6. Only provide the refined prompt for the rawText and nothing else.\n" +
                "rawText: " + rawText + "\n" +
                "Your output must follow this format:\n" +
                "\n" +
                "Refined Prompt:\n\n" +
                "<your improved version>\n";
    }

}

