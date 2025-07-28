package com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
)

// Data class for filter values
data class RecipeFilters(
    val region: String? = null,
    val ingredients: String? = null, // Comma-separated
    val otherConsiderations: String? = null
)

data class ImageResult(
    val image: ByteArray? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
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
    application: Application
) : AndroidViewModel(application) {

    // LiveData for the entire recipe UI state
    private val _recipeUiState =
        MutableLiveData(RecipeUiState(isLoading = false)) // Initial state can be loading
    val recipeUiState: LiveData<RecipeUiState> = _recipeUiState

    // LiveData for current filter values (to pre-fill the bottom sheet or re-apply)
    private val _currentFilters = MutableLiveData<RecipeFilters>(RecipeFilters())
    val currentFilters: LiveData<RecipeFilters> = _currentFilters

    private val _imageResult = MutableLiveData(ImageResult(isLoading = false))
    val imageResult: LiveData<ImageResult> = _imageResult

    init {
        // Optionally, load an initial random recipe when the ViewModel is created
        //fetchRandomRecipe()
    }

    fun resetRecipeUiState() {

        _recipeUiState.value = RecipeUiState(
            LOADING,
            LOADING,
            LOADING,
            LOADING,
            emptyList(),
            emptyList(),
            LOADING,
            false,
            null
        )
    }

    // --------------------------- DB Methods --------------------------------------------------

    val allRecipes: StateFlow<List<Recipe>> = recipeRepository.getAllRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Keep upstream flow active for 5s after last subscriber gone
            initialValue = emptyList()
        )

    fun saveRecipeToDb() {

        val recipeUiStateValue = _recipeUiState.value
        val imageResultValue = _imageResult.value
        val filtersValue = _currentFilters.value

        if (recipeUiStateValue != null) {

            val recipe = Recipe(
                recipeName = recipeUiStateValue.recipeName,
                yield = recipeUiStateValue.yield,
                prepTime = recipeUiStateValue.prepTime,
                cookTime = recipeUiStateValue.cookTime,
                ingredients = recipeUiStateValue.ingredients,
                instructions = recipeUiStateValue.instructions,
                imagePrompt = recipeUiStateValue.imagePrompt,
                regionFilter = filtersValue?.region,
                ingredientsFilter = filtersValue?.ingredients,
                otherConsiderationsFilter = filtersValue?.otherConsiderations,
                imageFilePath = null,
                generatedAt = System.currentTimeMillis()
            )

            insertRecipe(
                recipe,
                imageResultValue?.image ?: ByteArray(0)
            )

        }
    }

    fun insertRecipe(recipe: Recipe, image: ByteArray) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext

            val filePath = image.saveByteArrayToInternalFile(
                context,
                "recipe_images",
                "${recipe.recipeName}.png"
            )

            recipe.imageFilePath = filePath

            recipeRepository.insertRecipe(recipe)
        }
    }

    fun getAndShowSelectedRecipe(recipeId: Int) {

        val recipe = recipeRepository.getRecipeById(recipeId)

        if (recipe != null) {

            setRecipeUiState(recipe)
        }
    }

    private fun setRecipeUiState(recipe: Recipe) {

        _recipeUiState.value = RecipeUiState(
            recipeName = recipe.recipeName,
            yield = recipe.yield ?: LOADING,
            prepTime = recipe.prepTime ?: LOADING,
            cookTime = recipe.cookTime ?: LOADING,
            ingredientsAndInstruction = convertIngredientAndInstructionToMdString(recipe.ingredients ?: emptyList(), recipe.instructions ?: emptyList()),
            isLoading = false,
            errorMessage = null
        )

        _imageResult.value = ImageResult(image = recipe.imageFilePath?.let { filePath ->
            AppUtil.retrieveImageAsByteArray(filePath)
        }, false, null)
    }

    // -----------------------------------------------------------------------------------------


    // -------------------- Fetching Random recipe through AI ------------------------------------
    private fun fetchRandomRecipe() {
        viewModelScope.launch {
            _recipeUiState.value = RecipeUiState(isLoading = true) // Set loading state
            _imageResult.value = ImageResult(isLoading = true)
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

            getRecipeImage(aiRecipeResponse.imagePrompt)

        } else {
            showError("Unable to get the recipe. PLease try again!!")
        }
    }

    private fun getRecipeImage(imagePrompt: String) {

        if (imagePrompt.isNotBlank()) {

            viewModelScope.launch {
                _imageResult.value = ImageResult(isLoading = true)
                geminiAiHelper.generateImageByPrompt(imagePrompt)
                    .onSuccess { imageResult ->

                        if (imageResult != null && imageResult.isPresent) {
                            val image = imageResult.get()
                            _imageResult.value = ImageResult(image = image, isLoading = false)
                        } else {
                            showError("Unable to get the recipe image. PLease try again!!")
                            _imageResult.value = ImageResult(isLoading = false)
                        }
                    }
                    .onFailure { error ->
                        _imageResult.value = ImageResult(errorMessage = error.message)
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

    // --------------------------------------------------------------------------------------------
}
