package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "masks")
data class MaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Mask",
    val xRatio: Float = 0.25f,      // 0.0 .. 1.0 relative to screen width
    val yRatio: Float = 0.25f,      // 0.0 .. 1.0 relative to screen height
    val widthRatio: Float = 0.50f,  // 0.05 .. 1.0 relative to screen width
    val heightRatio: Float = 0.25f, // 0.02 .. 1.0 relative to screen height
    val colorArgb: Long = 0xFF000000L, // Pure RGB 0,0,0
    val alpha: Float = 1.0f,        // 100% Alpha default
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
