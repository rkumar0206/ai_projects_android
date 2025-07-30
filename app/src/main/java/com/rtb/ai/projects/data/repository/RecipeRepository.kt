package com.rtb.ai.projects.data.repository

import android.util.Log
import com.rtb.ai.projects.data.local.db.dao.RecipeDao
import com.rtb.ai.projects.data.model.Recipe
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // This repository will also be a singleton
class RecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao
) {

    fun getAllRecipes(): Flow<List<Recipe>> {
        return recipeDao.getAllRecipesFlow()
    }

    suspend fun insertRecipe(recipe: Recipe) = recipeDao.insertRecipe(recipe)

    fun getLastSavedRecipe() = recipeDao.getLastSavedRecipe()

    fun getRecipeByIdFlow(id: Long) = recipeDao.getRecipeByIdFlow(id)

    fun getRecipeByRecipeName(recipeName: String) = recipeDao.getRecipeByRecipeNameFlow(recipeName)

    suspend fun deleteRecipeAndFile(recipe: Recipe) {
        // 1. Delete the image file from internal storage (logic from previous examples)
        recipe.imageFilePath?.let { filePath ->
            // ... file deletion logic using context (you'd need to pass context or handle it differently)
            try {
                val imageFile = File(filePath)
                if (imageFile.exists()) {
                    if (imageFile.delete()) {
                        Log.d("RecipeDeletion", "Image file deleted: $filePath")
                    } else {
                        Log.w("RecipeDeletion", "Failed to delete image file: $filePath")
                    }
                }
            } catch (e: Exception) {
                Log.e("RecipeDeletion", "Error deleting image file $filePath: ${e.message}", e)
            }
        }
        // 2. Delete from database
        recipeDao.deleteRecipe(recipe)
    }

}

