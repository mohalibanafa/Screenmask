package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class DesignProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Mask Design",
    val targetWidth: Int = 1080,
    val targetHeight: Int = 2400,
    val targetDensity: Float = 2.75f,
    val shapes: List<ShapeObject> = emptyList(),
    val isBlackoutMode: Boolean = false,
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
