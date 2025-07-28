package com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rtb.ai.projects.data.remote.AiRecipeResponse
import com.rtb.ai.projects.util.AppUtil.convertJsonToObject
import com.rtb.ai.projects.util.GeminiAiHelper
import com.rtb.ai.projects.util.constant.Constants.LOADING
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data class to hold the full recipe details
data class RecipeUiState(
    val recipeName: String = LOADING,
    val imageUrl: String? = null, // URL for the image
    val yield: String = LOADING,
    val prepTime: String = LOADING,
    val cookTime: String = LOADING,
    val ingredientsAndInstruction: String = LOADING,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// Data class for filter values
data class RecipeFilters(
    val region: String? = null,
    val ingredients: String? = null, // Comma-separated
    val otherConsiderations: String? = null
)

@HiltViewModel
class RandomRecipeViewModel @Inject constructor(
    private val geminiAiHelper: GeminiAiHelper
) : ViewModel() {

    // LiveData for the entire recipe UI state
    private val _recipeUiState =
        MutableLiveData(RecipeUiState(isLoading = false)) // Initial state can be loading
    val recipeUiState: LiveData<RecipeUiState> = _recipeUiState

    // LiveData for current filter values (to pre-fill the bottom sheet or re-apply)
    private val _currentFilters = MutableLiveData<RecipeFilters>(RecipeFilters())
    val currentFilters: LiveData<RecipeFilters> = _currentFilters

    init {
        // Optionally, load an initial random recipe when the ViewModel is created
        //fetchRandomRecipe()
    }

    fun resetRecipeUiState() {

        _recipeUiState.value = RecipeUiState(
            LOADING,
            null,
            LOADING,
            LOADING,
            LOADING,
            LOADING,
            false,
            null
        )
    }

    private fun fetchRandomRecipe() {
        viewModelScope.launch {
            _recipeUiState.value = RecipeUiState(isLoading = true) // Set loading state
            try {

                val prompt = constructPrompt(
                    region = currentFilters.value?.region ?: "",
                    ingredients = currentFilters.value?.ingredients ?: "",
                    otherConsideration = currentFilters.value?.otherConsiderations ?: ""
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

            _recipeUiState.value = RecipeUiState(
                recipeName = aiRecipeResponse.recipeTitle,
                imageUrl = null, // todo: add image
                yield = aiRecipeResponse.yield,
                prepTime = aiRecipeResponse.prepTime,
                cookTime = aiRecipeResponse.cookTime,
                ingredientsAndInstruction = instructionsAndIngredients,
                isLoading = false,
                errorMessage = null
            )
        }else {
            showError("Unable to get the recipe. PLease try again!!")
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
                markdownBuilder.append("${index + 1}. $instruction\n")
            }
        } else {
            markdownBuilder.append("No instructions provided.\n")
        }

        return markdownBuilder.toString().trimEnd() // Trim trailing newline if any
    }

    fun applyFilters(region: String?, ingredients: String?, otherConsiderations: String?) {
        _currentFilters.value = RecipeFilters(region, ingredients, otherConsiderations)
        fetchRandomRecipe()
    }

    fun refreshRecipe() {
        fetchRandomRecipe()
    }

    private fun showError(errorMessage: String) {

        _recipeUiState.value = RecipeUiState(
            isLoading = false,
            errorMessage = errorMessage
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
}
