package com.rtb.ai.projects.data.model

import java.io.Serializable

data class KeyElements(
    val protagonist: String? = null,
    val antagonist: String? = null,
    val setting: String? = null,
    val conflict: String? = null,
    val themes: List<String>? = null,
    val moodTone: String? = null
): Serializable

data class StoryInput(
    val genre: String? = null,
    val targetAudience: String? = null,
    val corePremise: String? = null,
    val keyElements: KeyElements? = null,
    val length: String? = null,
    val outputFormat: String? = null,
    val language: String? = null
): Serializable
