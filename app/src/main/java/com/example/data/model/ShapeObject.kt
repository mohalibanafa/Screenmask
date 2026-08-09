package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class ShapeObject(
    val id: String = UUID.randomUUID().toString(),
    val type: ShapeType = ShapeType.RECTANGLE,
    val title: String = "Shape",
    val x: Float = 100f,
    val y: Float = 200f,
    val width: Float = 300f,
    val height: Float = 200f,
    val rotation: Float = 0f,
    val colorArgb: Long = 0xFF000000L, // Pure Black default
    val alpha: Float = 1.0f,           // 100% Opacity default
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val zIndex: Int = 0,
    val cornerRadius: Float = 24f,     // For ROUNDED_RECT
    val aspectRatioLocked: Boolean = false,
    val points: List<PointFData> = emptyList() // For POLYGON or FREEHAND_PATH
)
