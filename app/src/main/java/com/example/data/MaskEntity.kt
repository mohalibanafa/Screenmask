package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "masks")
data class MaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Mask",
    val shapeType: String = SHAPE_RECT, // SHAPE_RECT, SHAPE_ROUNDED, SHAPE_CIRCLE, SHAPE_FREE_QUAD
    val cornerRadiusDp: Float = 12f,
    val xRatio: Float = 0.25f,      // 0.0 .. 1.0 relative to screen width
    val yRatio: Float = 0.25f,      // 0.0 .. 1.0 relative to screen height
    val widthRatio: Float = 0.45f,  // 0.0005 .. 1.0 relative to screen width
    val heightRatio: Float = 0.20f, // 0.0005 .. 1.0 relative to screen height
    val colorArgb: Long = 0xFF000000L, // Pure RGB 0,0,0
    val alpha: Float = 1.0f,        // 0.1 .. 1.0
    val isTouchThrough: Boolean = true, // When active/locked, allows touch pass-through
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,
    val isPhysicalLock: Boolean = true, // Anchors the shape to physical screen hardware pixels across rotation
    // Free Quad 4 Corner Ratios (0.0 .. 1.0 within the bounding box)
    val p1x: Float = 0f, // Top-Left X
    val p1y: Float = 0f, // Top-Left Y
    val p2x: Float = 1f, // Top-Right X
    val p2y: Float = 0f, // Top-Right Y
    val p3x: Float = 1f, // Bottom-Right X
    val p3y: Float = 1f, // Bottom-Right Y
    val p4x: Float = 0f, // Bottom-Left X
    val p4y: Float = 1f, // Bottom-Left Y
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SHAPE_RECT = "RECT"
        const val SHAPE_ROUNDED = "ROUNDED"
        const val SHAPE_CIRCLE = "CIRCLE"
        const val SHAPE_FREE_QUAD = "FREE_QUAD" // Free-corner quadrilateral / parallelogram
    }
}
