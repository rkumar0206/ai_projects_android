package com.rtb.ai.projects.data.model

data class ContentItemForDbSaving(
    val itemId: String,
    val type: String,
    val position: Int,
    val text: String? = null,
    val imageFilePath: String? = null,
    val imagePrompt: String? = null,
)