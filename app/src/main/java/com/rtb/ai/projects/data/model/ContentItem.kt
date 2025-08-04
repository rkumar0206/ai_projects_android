package com.rtb.ai.projects.data.model

import androidx.annotation.DrawableRes
import java.io.Serializable

// Sealed class to represent different types of content
sealed class ContentItem(val id: String) :
    Serializable { // Common ID for stable IDs in RecyclerView
    data class TextContent(
        val itemId: String,
        val text: String
    ) : Serializable, ContentItem(itemId)

    data class ImageContent(
        val itemId: String,
        val imageFilePath: String? = null, // For network images
        val imagePrompt: String? = null,
        @DrawableRes val imageResId: Int? = null // For local drawable resources
    ) : ContentItem(itemId), Serializable
}