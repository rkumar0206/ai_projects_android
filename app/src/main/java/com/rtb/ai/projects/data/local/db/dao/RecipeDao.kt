package com.rtb.ai.projects.data.local.db.dao // Or your preferred package

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rtb.ai.projects.data.model.Recipe
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long // Returns the new rowId

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecipes(recipes: List<Recipe>)

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Long)

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()

    @Query("SELECT * FROM recipes ORDER BY generatedAt DESC LIMIT 1")
    fun getLastSavedRecipe(): Flow<Recipe?>

    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    fun getRecipeByIdFlow(recipeId: Long): Flow<Recipe?> // Flow for observable read

    @Query("SELECT * FROM recipes WHERE recipeName = :recipeName")
    fun getRecipeByRecipeNameFlow(recipeName: String): Flow<Recipe?>

    @Query("SELECT * FROM recipes ORDER BY generatedAt DESC")
    fun getAllRecipesFlow(): Flow<List<Recipe>> // Observable list of all recipes

    @Query("SELECT * FROM recipes ORDER BY recipeName ASC")
    fun getAllRecipesSortedByNameFlow(): Flow<List<Recipe>>

    // Example: Find recipes by a part of their name (case-insensitive)
    @Query("SELECT * FROM recipes WHERE LOWER(recipeName) LIKE '%' || LOWER(:query) || '%' ORDER BY recipeName ASC")
    fun searchRecipesByNameFlow(query: String): Flow<List<Recipe>>

    // You can add more specific queries as needed, e.g., based on filters
    @Query("SELECT * FROM recipes WHERE LOWER(regionFilter) = LOWER(:region)")
    fun getRecipesByRegionFlow(region: String): Flow<List<Recipe>>
}
