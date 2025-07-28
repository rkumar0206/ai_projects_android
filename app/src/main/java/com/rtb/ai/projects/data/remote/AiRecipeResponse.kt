package com.rtb.ai.projects.data.remote

data class AiRecipeResponse(
    val recipeTitle: String,
    val description: String,
    val yield: String,
    val prepTime: String,
    val cookTime: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val imagePrompt: String
)
