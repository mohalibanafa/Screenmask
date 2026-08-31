package com.example.service

import android.view.Surface
import com.example.data.MaskEntity
import kotlin.math.max

/**
 * High-precision coordinate transformation system for physical display pixels.
 * Ensures shapes, masks, and quadrilateral geometry remain anchored to the exact
 * physical hardware glass pixels across device rotations (0°, 90°, 180°, 270°),
 * perfectly covering screen cracks, scratches, dead pixels, and hardware blemishes.
 */
object PhysicalPixelTransformer {

    data class Point2D(val x: Float, val y: Float)

    data class TransformedMaskLayout(
        val winX: Int,
        val winY: Int,
        val winWidth: Int,
        val winHeight: Int,
        val q1: Point2D,
        val q2: Point2D,
        val q3: Point2D,
        val q4: Point2D,
        val padX: Float = 0f,
        val padY: Float = 0f
    )

    /**
     * Obtains the display dimensions in natural orientation (ROTATION_0).
     */
    fun getNaturalDimensions(screenWidth: Int, screenHeight: Int, rotation: Int): Pair<Float, Float> {
        return if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            Pair(screenWidth.toFloat(), screenHeight.toFloat())
        } else {
            Pair(screenHeight.toFloat(), screenWidth.toFloat())
        }
    }

    /**
     * Converts a physical hardware screen point (X_phys, Y_phys) in natural coordinates (ROTATION_0)
     * to the current window coordinate system (x_win, y_win) in rotation [rotation].
     */
    fun physicalToWindow(xp: Float, yp: Float, w0: Float, h0: Float, rotation: Int): Point2D {
        return when (rotation) {
            Surface.ROTATION_0 -> Point2D(xp, yp)
            Surface.ROTATION_90 -> Point2D(yp, w0 - xp)
            Surface.ROTATION_180 -> Point2D(w0 - xp, h0 - yp)
            Surface.ROTATION_270 -> Point2D(h0 - yp, xp)
            else -> Point2D(xp, yp)
        }
    }

    /**
     * Converts a window coordinate (x_win, y_win) in rotation [rotation] to physical natural coordinate (X_phys, Y_phys).
     */
    fun windowToPhysical(xw: Float, yw: Float, w0: Float, h0: Float, rotation: Int): Point2D {
        return when (rotation) {
            Surface.ROTATION_0 -> Point2D(xw, yw)
            Surface.ROTATION_90 -> Point2D(w0 - yw, xw)
            Surface.ROTATION_180 -> Point2D(w0 - xw, h0 - yw)
            Surface.ROTATION_270 -> Point2D(yw, h0 - xw)
            else -> Point2D(xw, yw)
        }
    }

    /**
     * Converts touch movement deltas (dx, dy) from current screen window space to physical coordinate deltas.
     */
    fun windowDeltaToPhysical(dxw: Float, dyw: Float, rotation: Int): Point2D {
        return when (rotation) {
            Surface.ROTATION_0 -> Point2D(dxw, dyw)
            Surface.ROTATION_90 -> Point2D(-dyw, dxw)
            Surface.ROTATION_180 -> Point2D(-dxw, -dyw)
            Surface.ROTATION_270 -> Point2D(dyw, -dxw)
            else -> Point2D(dxw, dyw)
        }
    }

    /**
     * Calculates the WindowManager layout parameters (x, y, width, height) and the relative
     * corner points (q1, q2, q3, q4) for drawing inside the view.
     *
     * @param extraPadding Padding in pixels added around the bounding box (useful in Edit Mode
     *                     to ensure handles and touch zones are fully contained and never clipped).
     */
    fun calculateMaskLayout(
        mask: MaskEntity,
        screenWidth: Int,
        screenHeight: Int,
        rotation: Int,
        extraPadding: Float = 0f
    ): TransformedMaskLayout {
        if (!mask.isPhysicalLock) {
            // Logical screen relative mode
            val pxX = (mask.xRatio * screenWidth)
            val pxY = (mask.yRatio * screenHeight)
            val pxW = (mask.widthRatio * screenWidth).coerceAtLeast(1f)
            val pxH = (mask.heightRatio * screenHeight).coerceAtLeast(1f)

            val winX = (pxX - extraPadding).toInt()
            val winY = (pxY - extraPadding).toInt()
            val winW = max((pxW + 2 * extraPadding).toInt(), 1)
            val winH = max((pxH + 2 * extraPadding).toInt(), 1)

            val q1 = Point2D(extraPadding + mask.p1x * pxW, extraPadding + mask.p1y * pxH)
            val q2 = Point2D(extraPadding + mask.p2x * pxW, extraPadding + mask.p2y * pxH)
            val q3 = Point2D(extraPadding + mask.p3x * pxW, extraPadding + mask.p3y * pxH)
            val q4 = Point2D(extraPadding + mask.p4x * pxW, extraPadding + mask.p4y * pxH)

            return TransformedMaskLayout(winX, winY, winW, winH, q1, q2, q3, q4, extraPadding, extraPadding)
        }

        val (w0, h0) = getNaturalDimensions(screenWidth, screenHeight, rotation)

        val physX = mask.xRatio * w0
        val physY = mask.yRatio * h0
        val physW = (mask.widthRatio * w0).coerceAtLeast(1f)
        val physH = (mask.heightRatio * h0).coerceAtLeast(1f)

        // 4 corner points in physical space
        val p1x = physX + mask.p1x * physW
        val p1y = physY + mask.p1y * physH
        val p2x = physX + mask.p2x * physW
        val p2y = physY + mask.p2y * physH
        val p3x = physX + mask.p3x * physW
        val p3y = physY + mask.p3y * physH
        val p4x = physX + mask.p4x * physW
        val p4y = physY + mask.p4y * physH

        // Map physical corners to current window space
        val c1 = physicalToWindow(p1x, p1y, w0, h0, rotation)
        val c2 = physicalToWindow(p2x, p2y, w0, h0, rotation)
        val c3 = physicalToWindow(p3x, p3y, w0, h0, rotation)
        val c4 = physicalToWindow(p4x, p4y, w0, h0, rotation)

        val minWinX = minOf(c1.x, c2.x, c3.x, c4.x)
        val maxWinX = maxOf(c1.x, c2.x, c3.x, c4.x)
        val minWinY = minOf(c1.y, c2.y, c3.y, c4.y)
        val maxWinY = maxOf(c1.y, c2.y, c3.y, c4.y)

        val winX = (minWinX - extraPadding).toInt()
        val winY = (minWinY - extraPadding).toInt()
        val winW = max((maxWinX - minWinX + 2 * extraPadding).toInt(), 1)
        val winH = max((maxWinY - minWinY + 2 * extraPadding).toInt(), 1)

        // Points inside the view coordinate system
        val q1 = Point2D(c1.x - winX, c1.y - winY)
        val q2 = Point2D(c2.x - winX, c2.y - winY)
        val q3 = Point2D(c3.x - winX, c3.y - winY)
        val q4 = Point2D(c4.x - winX, c4.y - winY)

        return TransformedMaskLayout(winX, winY, winW, winH, q1, q2, q3, q4, extraPadding, extraPadding)
    }
}
