package com.rtb.ai.projects.data.model

import androidx.annotation.DrawableRes

// Sealed class to represent different types of content
sealed class ContentItem(val id: String) { // Common ID for stable IDs in RecyclerView
    data class TextContent(
        val itemId: String,
        val text: String
    ) : ContentItem(itemId)

    data class ImageContent(
        val itemId: String,
        val image: ByteArray? = null, // For network images
        val imagePrompt: String? = null,
        @DrawableRes val imageResId: Int? = null // For local drawable resources
    ) : ContentItem(itemId) {
//        init {
//            require(image != null || imageResId != null) {
//                "Either image or imageResId must be provided for ImageContent"
//            }
//        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ImageContent

            if (imageResId != other.imageResId) return false
            if (itemId != other.itemId) return false
            if (!image.contentEquals(other.image)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = imageResId ?: 0
            result = 31 * result + itemId.hashCode()
            result = 31 * result + (image?.contentHashCode() ?: 0)
            return result
        }
    }
    // You could add more types here like VideoContent, QuoteContent, etc.
}