package com.rtb.ai.projects.data.remote

data class AiRecipeResponse(
    val id: Long,
    val recipeTitle: String,
    val description: String,
    val yield: String,
    val prepTime: String,
    val cookTime: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val imagePrompt: String
)
