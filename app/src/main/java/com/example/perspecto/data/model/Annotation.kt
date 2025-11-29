package com.example.perspecto.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Annotation(
    val id: String,
    val videoId: String? = null,
    val userId: String,
    val content: String,
    val title: String,
    @SerialName("severity")
    val severity: String,
    val timestamp: Double,
    val startFrame: Int? = null,
    val endFrame: Int? = null,
    val frame: Int? = null,
    val projectId: String? = null,
    val duration: Double = 0.0,
    val durationFrames: Int = 0,
    val color: String = "#FFFFFF",
    val annotationType: String = "text",
    val comments: List<Comment> = emptyList()
)
