package com.example.perspecto.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Video(
    val id: String = "", // Default value for backward compatibility if needed, but should be populated
    val videoId: String, // This maps to the 'videoId' column in Supabase which seems to be the ID used for annotations
    val title: String,
    val url: String,
    val name: String = "", // Keeping name for now as it was used before, might be redundant with title
    val annotationCount: Int = 0, // Count of annotations for this video
    val fileSize: Long? = null,
    val updatedAt: String? = null,
    val isPublic: Boolean = false,
    val allowAnnotations: Boolean = false
)
