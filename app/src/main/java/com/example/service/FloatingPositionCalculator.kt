package com.example.service

import android.view.Surface

/**
 * Utility to calculate physical screen coordinate transformations
 * when rotating the device across 0°, 90°, 180°, and 270° orientations.
 * Ensures floating controls and overlay icons maintain their exact physical
 * screen position regardless of screen aspect ratio and orientation changes.
 */
object FloatingPositionCalculator {

    data class Position(val x: Int, val y: Int)

    /**
     * Calculates the new top-left coordinates (x, y) for a floating view after a screen rotation,
     * maintaining the floating element above the same physical display pixels.
     *
     * @param oldX Current X position (top-left) in the old coordinate system
     * @param oldY Current Y position (top-left) in the old coordinate system
     * @param oldWidth Screen width before rotation
     * @param oldHeight Screen height before rotation
     * @param oldRotation Screen rotation before change (Surface.ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270)
     * @param newWidth Screen width after rotation
     * @param newHeight Screen height after rotation
     * @param newRotation Screen rotation after change (Surface.ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270)
     * @param iconWidth Width of the floating element
     * @param iconHeight Height of the floating element
     * @return New Position(x, y) clamped to the new screen boundaries
     */
    fun calculateRotatedPosition(
        oldX: Int,
        oldY: Int,
        oldWidth: Int,
        oldHeight: Int,
        oldRotation: Int,
        newWidth: Int,
        newHeight: Int,
        newRotation: Int,
        iconWidth: Int = 0,
        iconHeight: Int = 0
    ): Position {
        val deltaRotation = (newRotation - oldRotation + 4) % 4

        var newX = oldX
        var newY = oldY

        when (deltaRotation) {
            0 -> {
                // 0° - No rotation change
                newX = oldX
                newY = oldY
            }
            1 -> {
                // 90° Clockwise:
                // newX = oldHeight - oldY - iconHeight
                // newY = oldX
                newX = oldHeight - oldY - iconHeight
                newY = oldX
            }
            2 -> {
                // 180° Inverted:
                // newX = oldWidth - oldX - iconWidth
                // newY = oldHeight - oldY - iconHeight
                newX = oldWidth - oldX - iconWidth
                newY = oldHeight - oldY - iconHeight
            }
            3 -> {
                // 270° Clockwise / 90° Counter-Clockwise (Reverse):
                // newX = oldY
                // newY = oldWidth - oldX - iconWidth
                newX = oldY
                newY = oldWidth - oldX - iconWidth
            }
        }

        // Clamp to new screen bounds to avoid floating out of view
        val maxX = (newWidth - iconWidth).coerceAtLeast(0)
        val maxY = (newHeight - iconHeight).coerceAtLeast(0)

        val clampedX = newX.coerceIn(0, maxX)
        val clampedY = newY.coerceIn(0, maxY)

        return Position(clampedX, clampedY)
    }
}
