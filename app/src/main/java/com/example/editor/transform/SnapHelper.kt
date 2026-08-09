package com.example.editor.transform

import kotlin.math.abs

data class SnapResult(
    val snappedX: Float,
    val snappedY: Float,
    val showVerticalGuideline: Boolean = false,
    val verticalGuidelineX: Float = 0f,
    val showHorizontalGuideline: Boolean = false,
    val horizontalGuidelineY: Float = 0f
)

object SnapHelper {

    private const val SNAP_THRESHOLD_PX = 18f

    fun calculateSnap(
        rawX: Float,
        rawY: Float,
        width: Float,
        height: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        gridSizePx: Float,
        snapToGrid: Boolean,
        snapToEdges: Boolean,
        snapToCenter: Boolean
    ): SnapResult {
        var newX = rawX
        var newY = rawY
        var showVGuideline = false
        var vGuidelineX = 0f
        var showHGuideline = false
        var hGuidelineY = 0f

        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        // 1. Center & Edge Snapping
        if (snapToCenter) {
            val shapeCenterX = rawX + width / 2f
            if (abs(shapeCenterX - centerX) < SNAP_THRESHOLD_PX) {
                newX = centerX - width / 2f
                showVGuideline = true
                vGuidelineX = centerX
            }

            val shapeCenterY = rawY + height / 2f
            if (abs(shapeCenterY - centerY) < SNAP_THRESHOLD_PX) {
                newY = centerY - height / 2f
                showHGuideline = true
                hGuidelineY = centerY
            }
        }

        if (snapToEdges) {
            // Left edge
            if (abs(rawX) < SNAP_THRESHOLD_PX) {
                newX = 0f
                showVGuideline = true
                vGuidelineX = 0f
            }
            // Right edge
            if (abs((rawX + width) - canvasWidth) < SNAP_THRESHOLD_PX) {
                newX = canvasWidth - width
                showVGuideline = true
                vGuidelineX = canvasWidth
            }

            // Top edge
            if (abs(rawY) < SNAP_THRESHOLD_PX) {
                newY = 0f
                showHGuideline = true
                hGuidelineY = 0f
            }
            // Bottom edge
            if (abs((rawY + height) - canvasHeight) < SNAP_THRESHOLD_PX) {
                newY = canvasHeight - height
                showHGuideline = true
                hGuidelineY = canvasHeight
            }
        }

        // 2. Grid Snapping
        if (snapToGrid && gridSizePx > 0f) {
            if (!showVGuideline) {
                val gridX = (newX / gridSizePx).let { Math.round(it) * gridSizePx }
                if (abs(newX - gridX) < SNAP_THRESHOLD_PX) {
                    newX = gridX
                }
            }
            if (!showHGuideline) {
                val gridY = (newY / gridSizePx).let { Math.round(it) * gridSizePx }
                if (abs(newY - gridY) < SNAP_THRESHOLD_PX) {
                    newY = gridY
                }
            }
        }

        return SnapResult(
            snappedX = newX,
            snappedY = newY,
            showVerticalGuideline = showVGuideline,
            verticalGuidelineX = vGuidelineX,
            showHorizontalGuideline = showHGuideline,
            horizontalGuidelineY = hGuidelineY
        )
    }
}
