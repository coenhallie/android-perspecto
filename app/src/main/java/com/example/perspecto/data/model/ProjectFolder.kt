package com.example.perspecto.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectFolder(
    @SerialName("project_id") val projectId: String,
    @SerialName("folder_id") val folderId: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)
