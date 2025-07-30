package com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rtb.ai.projects.data.model.Recipe
import com.rtb.ai.projects.data.remote.AiRecipeResponse
import com.rtb.ai.projects.data.repository.RecipeRepository
import com.rtb.ai.projects.util.AppUtil
import com.rtb.ai.projects.util.AppUtil.convertJsonToObject
import com.rtb.ai.projects.util.AppUtil.saveByteArrayToInternalFile
import com.rtb.ai.projects.util.GeminiAiHelper
import com.rtb.ai.projects.util.constant.Constants.LOADING
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.Serializable
import javax.inject.Inject

// Data class to hold the full recipe details
data class RecipeUiState(
    val recipeName: String = LOADING,
    val yield: String = LOADING,
    val prepTime: String = LOADING,
    val cookTime: String = LOADING,
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val ingredientsAndInstruction: String = LOADING,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val imagePrompt: String? = null
) : Serializable

// Data class for filter values
data class RecipeFilters(
    val region: String? = null,
    val ingredients: String? = null, // Comma-separated
    val otherConsiderations: String? = null
) : Serializable

data class ImageResult(
    val image: ByteArray? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageResult

        if (isLoading != other.isLoading) return false
        if (!image.contentEquals(other.image)) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + image.contentHashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}

@HiltViewModel
class RandomRecipeViewModel @Inject constructor(
    private val geminiAiHelper: GeminiAiHelper,
    private val recipeRepository: RecipeRepository,
    private val savedStateHandle: SavedStateHandle,
    application: Application
) : AndroidViewModel(application) {

    val recipeUiState: StateFlow<RecipeUiState> = savedStateHandle.getStateFlow(
        RECIPE_UI_STATE_KEY,
        resetRecipeUiState()
    )
    val currentFilters: StateFlow<RecipeFilters> =
        savedStateHandle.getStateFlow(RECIPE_FILTERS_KEY, RecipeFilters())
    val imageResult: StateFlow<ImageResult> = savedStateHandle.getStateFlow(
        IMAGE_RESULT_KEY,
        ImageResult(isLoading = true)
    )
    val isRecipeSavedToDb: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(IS_RECIPE_SAVED_TO_DB_KEY, false)

    init {
        Log.d(TAG, "init: init called")
        showLastSavedRecipeOrFetchRecipeFromAI()
    }

    fun resetRecipeUiState(): RecipeUiState {

        val recipeUiState = RecipeUiState(
            LOADING,
            LOADING,
            LOADING,
            LOADING,
            emptyList(),
            emptyList(),
            LOADING,
            true,
            null
        )

        updateSavedStateHandleValueForRecipeUIState(recipeUiState)
        return recipeUiState
    }

    // --------------------------- DB Methods --------------------------------------------------

    private fun showLastSavedRecipeOrFetchRecipeFromAI() {

        viewModelScope.launch {

            val lastSavedRecipe = recipeRepository.getLastSavedRecipe().first()

            if (lastSavedRecipe != null) {
                updateUIDataByRecipe(lastSavedRecipe)
            } else {
                fetchRandomRecipe()
            }
        }
    }

    val allRecipes: StateFlow<List<Recipe>> = recipeRepository.getAllRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Keep upstream flow active for 5s after last subscriber gone
            initialValue = emptyList()
        )

    fun saveOrDeleteRecipeFromDbBasedOnBookmarkMenuPressed() {

        viewModelScope.launch {

            Log.d(TAG, "saveOrDeleteRecipeFromDbBasedOnBookmarkMenuPressed: ")

            if (!isRecipeSavedToDb.value) {

                Log.d(TAG, "saveOrDeleteRecipeFromDbBasedOnBookmarkMenuPressed: Saving recipe")

                val recipeUiStateValue = recipeUiState.value
                val imageResultValue = imageResult.value
                val filtersValue = currentFilters.value

                if (recipeUiState.value.recipeName.isNotBlank() && recipeUiState.value.recipeName != LOADING) {

                    val recipe = Recipe(
                        recipeName = recipeUiStateValue.recipeName,
                        yield = recipeUiStateValue.yield,
                        prepTime = recipeUiStateValue.prepTime,
                        cookTime = recipeUiStateValue.cookTime,
                        ingredients = recipeUiStateValue.ingredients,
                        instructions = recipeUiStateValue.instructions,
                        imagePrompt = recipeUiStateValue.imagePrompt,
                        regionFilter = filtersValue.region,
                        ingredientsFilter = filtersValue.ingredients,
                        otherConsiderationsFilter = filtersValue.otherConsiderations,
                        imageFilePath = null,
                        generatedAt = System.currentTimeMillis()
                    )

                    insertRecipe(
                        recipe,
                        imageResultValue.image ?: ByteArray(0)
                    )
                }
            } else {

                val recipeToDelete =
                    recipeRepository.getRecipeByRecipeName(recipeUiState.value.recipeName).first()

                if (recipeToDelete != null) {

                    recipeRepository.deleteRecipeAndFile(recipeToDelete)
                    savedStateHandle[IS_RECIPE_SAVED_TO_DB_KEY] = false
                }
            }
        }

    }

    fun deleteRecipe(recipe: Recipe) {

        viewModelScope.launch {

            val argRecipeId = recipe.id
            val currRecipeShowing = recipeRepository.getRecipeByRecipeName(recipeUiState.value.recipeName).first()

            recipeRepository.deleteRecipeAndFile(recipe)

            if (currRecipeShowing != null && currRecipeShowing.id == argRecipeId) {
                savedStateHandle[IS_RECIPE_SAVED_TO_DB_KEY] = false
            }
        }
    }

    private suspend fun insertRecipe(recipe: Recipe, image: ByteArray) {
        val context = getApplication<Application>().applicationContext

        val filePath = image.saveByteArrayToInternalFile(
            context,
            "recipe_images",
            "${recipe.recipeName}.png"
        )

        recipe.imageFilePath = filePath

        val recipeId = recipeRepository.insertRecipe(recipe)
        Log.d(TAG, "insertRecipe: Saved recipe: $recipe")
        Log.d(TAG, "insertRecipe: Saved recipe id: $recipeId")
        savedStateHandle[IS_RECIPE_SAVED_TO_DB_KEY] = true
    }

    fun getAndShowSelectedRecipe(recipeId: Long) {
        viewModelScope.launch {
            val recipe = recipeRepository.getRecipeByIdFlow(recipeId).first()
            if (recipe != null) {
                updateUIDataByRecipe(recipe)
            }
        }
    }

    private fun updateUIDataByRecipe(recipe: Recipe) {

        Log.d(TAG, "updateUIDataByRecipe: ")

        val recipeUiState = RecipeUiState(
            recipeName = recipe.recipeName,
            yield = recipe.yield ?: LOADING,
            prepTime = recipe.prepTime ?: LOADING,
            cookTime = recipe.cookTime ?: LOADING,
            ingredientsAndInstruction = convertIngredientAndInstructionToMdString(
                recipe.ingredients ?: emptyList(), recipe.instructions ?: emptyList()
            ),
            isLoading = false,
            errorMessage = null
        )

        updateSavedStateHandleValueForRecipeUIState(recipeUiState)

        val imageResult = ImageResult(image = recipe.imageFilePath?.let { filePath ->
            AppUtil.retrieveImageAsByteArray(filePath)
        }, false, null)

        updateSavedStateHandleValueForImageUIState(imageResult)

        savedStateHandle[IS_RECIPE_SAVED_TO_DB_KEY] = true
    }

    // -----------------------------------------------------------------------------------------


    // -------------------- Fetching Random recipe through AI ------------------------------------
    private fun fetchRandomRecipe() {
        viewModelScope.launch {
            updateSavedStateHandleValueForRecipeUIState(resetRecipeUiState())
            updateSavedStateHandleValueForImageUIState(ImageResult(isLoading = true))
            savedStateHandle[IS_RECIPE_SAVED_TO_DB_KEY] = false
            try {

                val prompt = constructPrompt(
                    region = currentFilters.value.region ?: "",
                    ingredients = currentFilters.value.ingredients ?: "",
                    otherConsideration = currentFilters.value.otherConsiderations ?: ""
                )

                geminiAiHelper.getPromptTextResult(prompt)
                    .onSuccess { result ->

                        if (!result.isNullOrBlank()) {
                            extractRecipeResponseAndSetTheValues(result)
                        } else {
                            showError("Error fetching recipe")
                        }
                    }
                    .onFailure { error ->
                        showError("Error fetching recipe: ${error.message}")
                    }
            } catch (e: Exception) {
                showError("Error fetching recipe: ${e.message}")
            }
        }
    }

    private fun extractRecipeResponseAndSetTheValues(result: String) {

        val recipeJsonString = result.replace("```", "").replace("json", "").trim()

        val aiRecipeResponse =
            recipeJsonString.convertJsonToObject(AiRecipeResponse::class.java)

        if (aiRecipeResponse != null) {

            val instructionsAndIngredients =
                convertIngredientAndInstructionToMdString(
                    aiRecipeResponse.ingredients,
                    aiRecipeResponse.instructions
                )

            updateSavedStateHandleValueForRecipeUIState(
                RecipeUiState(
                    recipeName = aiRecipeResponse.recipeTitle,
                    yield = aiRecipeResponse.yield,
                    prepTime = aiRecipeResponse.prepTime,
                    cookTime = aiRecipeResponse.cookTime,
                    ingredients = aiRecipeResponse.ingredients,
                    instructions = aiRecipeResponse.instructions,
                    ingredientsAndInstruction = instructionsAndIngredients,
                    isLoading = false,
                    errorMessage = null,
                    imagePrompt = aiRecipeResponse.imagePrompt
                )
            )

            getRecipeImage(aiRecipeResponse.imagePrompt)

        } else {
            showError("Unable to get the recipe. PLease try again!!")
        }
    }

    private fun getRecipeImage(imagePrompt: String) {

        if (imagePrompt.isNotBlank()) {

            viewModelScope.launch {
                updateSavedStateHandleValueForImageUIState(ImageResult(isLoading = true))
                geminiAiHelper.generateImageByPrompt(imagePrompt)
                    .onSuccess { imageResult ->

                        if (imageResult != null && imageResult.isPresent) {
                            val image = imageResult.get()
                            updateSavedStateHandleValueForImageUIState(
                                ImageResult(
                                    image = image,
                                    isLoading = false
                                )
                            )
                        } else {
                            showError("Unable to get the recipe image. Please try again!!")
                            updateSavedStateHandleValueForImageUIState(ImageResult(isLoading = false))
                        }

                        Log.d(TAG, "getRecipeImage: imageResult: $imageResult")
                    }
                    .onFailure { error ->
                        Log.d(TAG, "getRecipeImage: Error: $error")
                        updateSavedStateHandleValueForImageUIState(ImageResult(errorMessage = error.message))
                    }
            }
        }
    }

    private fun convertIngredientAndInstructionToMdString(
        ingredients: List<String>,
        instructions: List<String>
    ): String {

        val markdownBuilder = StringBuilder()

        // Ingredients Section
        markdownBuilder.append("### <u>**Ingredients**</u>\n") // Bold heading
        if (ingredients.isNotEmpty()) {
            ingredients.forEachIndexed { index, ingredient ->
                markdownBuilder.append("- $ingredient\n")
            }
        } else {
            markdownBuilder.append("No ingredients listed.\n")
        }

        markdownBuilder.append("\n") // Add a newline for spacing between sections

        // Instructions Section
        markdownBuilder.append("### <u>**Instructions</u>**\n") // Bold heading
        if (instructions.isNotEmpty()) {
            instructions.forEachIndexed { index, instruction ->
                markdownBuilder.append("- $instruction\n")
            }
        } else {
            markdownBuilder.append("No instructions provided.\n")
        }

        return markdownBuilder.toString().trimEnd() // Trim trailing newline if any
    }

    fun applyFilters(region: String?, ingredients: String?, otherConsiderations: String?) {
        updateSavedStateHandleValueForRecipeFilter(
            RecipeFilters(
                region,
                ingredients,
                otherConsiderations
            )
        )
        fetchRandomRecipe()
    }

    private fun updateSavedStateHandleValueForImageUIState(imageResult: ImageResult) {

        savedStateHandle[IMAGE_RESULT_KEY] = imageResult
    }

    private fun updateSavedStateHandleValueForRecipeUIState(recipeUiState: RecipeUiState) {

        savedStateHandle[RECIPE_UI_STATE_KEY] = recipeUiState
    }

    private fun updateSavedStateHandleValueForRecipeFilter(recipeFilters: RecipeFilters) {
        savedStateHandle[RECIPE_FILTERS_KEY] = recipeFilters
    }

    fun refreshRecipeClicked() {
        fetchRandomRecipe()
    }

    private fun showError(errorMessage: String) {

        updateSavedStateHandleValueForRecipeUIState(
            RecipeUiState(
                isLoading = false,
                errorMessage = errorMessage
            )
        )
    }

    private fun constructPrompt(
        region: String,
        ingredients: String,
        otherConsideration: String
    ): String {

        return "You are a highly creative culinary expert and recipe generator. Your task is to provide a complete, practical, and edible food recipe based on user specifications.\n" +
                "\n" +
                "**Input Parameters:**\n" +
                "The user may provide the following optional parameters:\n" +
                "*   **`region`**: (Optional) A specific regional cuisine (e.g., \"Italian\", \"Mexican\", \"Thai\", \"Mediterranean\").\n" +
                "*   **`ingredients`**: (Optional) A comma-separated list of key ingredients to incorporate (e.g., \"chicken, rice, spinach\", \"tofu, noodles, peanut butter\").\n" +
                "\n" +
                "**Instructions:**\n" +
                "1.  **Recipe Selection:**\n" +
                "    *   If a `region` is provided, generate a *random* recipe characteristic of that cuisine.\n" +
                "    *   If `ingredients` are provided, generate a *random* recipe that prominently features those ingredients. You may suggest a few additional common pantry staples if necessary to complete the dish.\n" +
                "    *   If both `region` and `ingredients` are provided, generate a *random* recipe that integrates the specified ingredients within the given regional cuisine.\n" +
                "    *   If *neither* `region` nor `ingredients` are provided, generate a *truly random* recipe from any global cuisine. Ensure diversity in dish type, main course, and origin with each request (e.g., don't provide two pasta dishes in a row if no constraints are given).\n" +
                "    *   Also any other consideration provided by the user, keep that in mind as well.\n" +
                "2.  **Recipe Output Format:** Present the recipe in a clear, structured format, including:\n" +
                "{\n" +
                "  \"recipeTitle\": \"RECIPE NAME HERE\",\n" +
                "  \"description\": \"A short overview of the dish.\",\n" +
                "  \"yield\": \"Serves X\",\n" +
                "  \"prepTime\": \"XX minutes\",\n" +
                "  \"cookTime\": \"XX minutes\",\n" +
                "  \"ingredients\": [\n" +
                "    \"1 tbsp olive oil\",\n" +
                "    \"2 garlic cloves, minced\",\n" +
                "    \"1 medium onion, chopped\"\n" +
                "    // ... more ingredients\n" +
                "  ],\n" +
                "  \"instructions\": [\n" +
                "    \"Heat the olive oil in a large pan over medium heat.\",\n" +
                "    \"Add the garlic and onions, and sauté until translucent.\",\n" +
                "    \"Add remaining ingredients and cook as directed.\"\n" +
                "    // ... more steps\n" +
                "  ],\n" +
                "  \"imagePrompt\": \"A image prompt for generating image of the dish.\"\n" +
                "}\n" +
                "\n" +
                "**Constraint:** The recipe must be feasible for a home cook and use commonly available ingredients. Do not provide recipes that require highly specialized equipment or exotic ingredients unless explicitly requested." +
                "Region: " + region + "\n" +
                "Ingredients: " + ingredients + "\n" +
                "Other considerations: " + otherConsideration;
    }

    // --------------------------------------------------------------------------------------------


    companion object {

        const val RECIPE_UI_STATE_KEY = "recipeUiState"
        const val RECIPE_FILTERS_KEY = "recipeFilters"
        const val IMAGE_RESULT_KEY = "imageResult" // Unlikely to work directly due to ByteArray
        const val IS_RECIPE_SAVED_TO_DB_KEY = "isRecipeSavedToDb"
        const val TAG = "RandomRecipeViewModel"
    }
}
