package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.example.data.MaskEntity
import kotlin.math.max

@SuppressLint("ViewClickAndTouchUnspecified")
class OverlayMaskView(
    context: Context,
    var mask: MaskEntity,
    var isEditMode: Boolean,
    var isMaskSelected: Boolean,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val onUpdate: (MaskEntity) -> Unit,
    private val onDelete: (String) -> Unit,
    private val onSelect: (String) -> Unit
) : View(context) {

    private val maskPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * context.resources.displayMetrics.density
        color = Color.parseColor("#38BDF8") // Accent Light Blue
        isAntiAlias = true
    }

    private val selectedBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f * context.resources.displayMetrics.density
        color = Color.parseColor("#F43F5E") // Highlight Rose
        isAntiAlias = true
    }

    private val handlePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#38BDF8")
        isAntiAlias = true
    }

    private val handleBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * context.resources.displayMetrics.density
        color = Color.WHITE
        isAntiAlias = true
    }

    private val handleRadius = 14f * context.resources.displayMetrics.density

    // Touch dragging / resizing state
    private var lastX = 0f
    private var lastY = 0f
    private var activeHandle = Handle.NONE

    private enum class Handle {
        NONE, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    init {
        updatePaint()
    }

    fun updateData(newMask: MaskEntity, editMode: Boolean, selected: Boolean) {
        this.mask = newMask
        this.isEditMode = editMode
        this.isMaskSelected = selected
        updatePaint()
        invalidate()
    }

    private fun updatePaint() {
        val argb = mask.colorArgb.toInt()
        val alphaByte = if (mask.alpha >= 0.99f) 255 else (mask.alpha * 255).toInt().coerceIn(0, 255)
        val red = (argb shr 16) and 0xFF
        val green = (argb shr 8) and 0xFF
        val blue = argb and 0xFF

        maskPaint.reset()
        maskPaint.style = Paint.Style.FILL
        maskPaint.isAntiAlias = true
        maskPaint.color = Color.argb(alphaByte, red, green, blue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 0 || height <= 0) return

        // 1. Draw Mask Background
        canvas.drawRect(0f, 0f, width, height, maskPaint)

        // 2. In Edit Mode (if not locked), draw edit outline and corner handles
        if (isEditMode && !mask.isLocked) {
            val paintToUse = if (isMaskSelected) selectedBorderPaint else borderPaint
            canvas.drawRect(0f, 0f, width, height, paintToUse)

            // Corner Handles
            canvas.drawCircle(0f, 0f, handleRadius, handlePaint)
            canvas.drawCircle(0f, 0f, handleRadius, handleBorderPaint)

            canvas.drawCircle(width, 0f, handleRadius, handlePaint)
            canvas.drawCircle(width, 0f, handleRadius, handleBorderPaint)

            canvas.drawCircle(0f, height, handleRadius, handlePaint)
            canvas.drawCircle(0f, height, handleRadius, handleBorderPaint)

            canvas.drawCircle(width, height, handleRadius, handlePaint)
            canvas.drawCircle(width, height, handleRadius, handleBorderPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEditMode || mask.isLocked) {
            return false // Pass through touch events
        }

        val rawX = event.rawX
        val rawY = event.rawY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onSelect(mask.id)
                lastX = rawX
                lastY = rawY
                activeHandle = determineHandle(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - lastX
                val dy = rawY - lastY

                val currWidthPx = (mask.widthRatio * screenWidth)
                val currHeightPx = (mask.heightRatio * screenHeight)
                val currXPx = (mask.xRatio * screenWidth)
                val currYPx = (mask.yRatio * screenHeight)

                val minWidthPx = 1f
                val minHeightPx = 1f

                var newX = currXPx
                var newY = currYPx
                var newW = currWidthPx
                var newH = currHeightPx

                when (activeHandle) {
                    Handle.CENTER -> {
                        newX += dx
                        newY += dy
                    }
                    Handle.TOP_LEFT -> {
                        val potentialW = currWidthPx - dx
                        val potentialH = currHeightPx - dy
                        if (potentialW >= minWidthPx) {
                            newW = potentialW
                            newX = currXPx + dx
                        }
                        if (potentialH >= minHeightPx) {
                            newH = potentialH
                            newY = currYPx + dy
                        }
                    }
                    Handle.TOP_RIGHT -> {
                        val potentialW = currWidthPx + dx
                        val potentialH = currHeightPx - dy
                        if (potentialW >= minWidthPx) {
                            newW = potentialW
                        }
                        if (potentialH >= minHeightPx) {
                            newH = potentialH
                            newY = currYPx + dy
                        }
                    }
                    Handle.BOTTOM_LEFT -> {
                        val potentialW = currWidthPx - dx
                        val potentialH = currHeightPx + dy
                        if (potentialW >= minWidthPx) {
                            newW = potentialW
                            newX = currXPx + dx
                        }
                        if (potentialH >= minHeightPx) {
                            newH = potentialH
                        }
                    }
                    Handle.BOTTOM_RIGHT -> {
                        val potentialW = currWidthPx + dx
                        val potentialH = currHeightPx + dy
                        if (potentialW >= minWidthPx) {
                            newW = potentialW
                        }
                        if (potentialH >= minHeightPx) {
                            newH = potentialH
                        }
                    }
                    Handle.NONE -> {}
                }

                lastX = rawX
                lastY = rawY

                // Convert to screen percentages (unconstrained, down to 1px / 0.0001f)
                val minWRatio = 1f / max(screenWidth.toFloat(), 1f)
                val minHRatio = 1f / max(screenHeight.toFloat(), 1f)
                val newXRatio = (newX / screenWidth).coerceIn(-0.2f, 1.0f)
                val newYRatio = (newY / screenHeight).coerceIn(-0.2f, 1.0f)
                val newWRatio = (newW / screenWidth).coerceIn(minWRatio, 1.0f)
                val newHRatio = (newH / screenHeight).coerceIn(minHRatio, 1.0f)

                val updated = mask.copy(
                    xRatio = newXRatio,
                    yRatio = newYRatio,
                    widthRatio = newWRatio,
                    heightRatio = newHRatio,
                    updatedAt = System.currentTimeMillis()
                )
                mask = updated
                onUpdate(updated)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = Handle.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun determineHandle(x: Float, y: Float): Handle {
        val w = width.toFloat()
        val h = height.toFloat()
        val touchMargin = handleRadius * 2.5f

        return when {
            RectF(-touchMargin, -touchMargin, touchMargin, touchMargin).contains(x, y) -> Handle.TOP_LEFT
            RectF(w - touchMargin, -touchMargin, w + touchMargin, touchMargin).contains(x, y) -> Handle.TOP_RIGHT
            RectF(-touchMargin, h - touchMargin, touchMargin, h + touchMargin).contains(x, y) -> Handle.BOTTOM_LEFT
            RectF(w - touchMargin, h - touchMargin, w + touchMargin, h + touchMargin).contains(x, y) -> Handle.BOTTOM_RIGHT
            else -> Handle.CENTER
        }
    }
}
