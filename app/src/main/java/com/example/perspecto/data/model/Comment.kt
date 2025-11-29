package com.example.perspecto.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String,
    @SerialName("annotationId")
    val annotationId: String,
    val content: String,
    @SerialName("createdAt")
    val createdAt: String? = null
)
