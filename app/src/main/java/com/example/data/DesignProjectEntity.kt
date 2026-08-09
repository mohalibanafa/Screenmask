package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "design_projects")
data class DesignProjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Mask Design",
    val targetWidth: Int = 1080,
    val targetHeight: Int = 2400,
    val targetDensity: Float = 2.75f,
    val shapesJson: String = "[]",
    val isBlackoutMode: Boolean = false,
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
