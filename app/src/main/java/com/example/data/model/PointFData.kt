package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PointFData(
    val x: Float = 0f,
    val y: Float = 0f
)
