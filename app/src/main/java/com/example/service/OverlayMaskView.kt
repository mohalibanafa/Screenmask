package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.example.data.MaskEntity
import kotlin.math.max
import kotlin.math.min

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
        strokeWidth = 3f * context.resources.displayMetrics.density
        color = Color.parseColor("#38BDF8") // Cyan accent
        isAntiAlias = true
    }

    private val selectedBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f * context.resources.displayMetrics.density
        color = Color.parseColor("#F43F5E") // Rose red selected highlight
        isAntiAlias = true
    }

    private val handlePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#38BDF8")
        isAntiAlias = true
    }

    private val handleBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * context.resources.displayMetrics.density
        color = Color.WHITE
        isAntiAlias = true
    }

    private val handleRadius = 14f * context.resources.displayMetrics.density
    private val quadPath = Path()

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
        val alphaByte = (mask.alpha * 255).toInt().coerceIn(0, 255)
        val red = (argb shr 16) and 0xFF
        val green = (argb shr 8) and 0xFF
        val blue = argb and 0xFF

        val colorInt = Color.argb(alphaByte, red, green, blue)

        maskPaint.reset()
        maskPaint.style = Paint.Style.FILL
        maskPaint.isAntiAlias = true
        maskPaint.color = colorInt
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 0 || height <= 0) return

        val rect = RectF(0f, 0f, width, height)
        val cornerRadiusPx = mask.cornerRadiusDp * context.resources.displayMetrics.density

        // 1. Draw Mask Shape
        when (mask.shapeType) {
            MaskEntity.SHAPE_CIRCLE -> {
                canvas.drawOval(rect, maskPaint)
            }
            MaskEntity.SHAPE_ROUNDED -> {
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, maskPaint)
            }
            MaskEntity.SHAPE_FREE_QUAD -> {
                quadPath.reset()
                val x1 = mask.p1x * width
                val y1 = mask.p1y * height
                val x2 = mask.p2x * width
                val y2 = mask.p2y * height
                val x3 = mask.p3x * width
                val y3 = mask.p3y * height
                val x4 = mask.p4x * width
                val y4 = mask.p4y * height

                quadPath.moveTo(x1, y1)
                quadPath.lineTo(x2, y2)
                quadPath.lineTo(x3, y3)
                quadPath.lineTo(x4, y4)
                quadPath.close()

                canvas.drawPath(quadPath, maskPaint)
            }
            else -> {
                canvas.drawRect(rect, maskPaint)
            }
        }

        // 2. In Edit Mode (if not locked), draw edit outline and corner handles
        if (isEditMode && !mask.isLocked) {
            val paintToUse = if (isMaskSelected) selectedBorderPaint else borderPaint

            when (mask.shapeType) {
                MaskEntity.SHAPE_CIRCLE -> {
                    canvas.drawOval(rect, paintToUse)
                    drawCornerHandles(canvas, 0f, 0f, width, 0f, width, height, 0f, height)
                }
                MaskEntity.SHAPE_ROUNDED -> {
                    canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paintToUse)
                    drawCornerHandles(canvas, 0f, 0f, width, 0f, width, height, 0f, height)
                }
                MaskEntity.SHAPE_FREE_QUAD -> {
                    canvas.drawPath(quadPath, paintToUse)
                    val x1 = mask.p1x * width
                    val y1 = mask.p1y * height
                    val x2 = mask.p2x * width
                    val y2 = mask.p2y * height
                    val x3 = mask.p3x * width
                    val y3 = mask.p3y * height
                    val x4 = mask.p4x * width
                    val y4 = mask.p4y * height
                    drawCornerHandles(canvas, x1, y1, x2, y2, x3, y3, x4, y4)
                }
                else -> {
                    canvas.drawRect(rect, paintToUse)
                    drawCornerHandles(canvas, 0f, 0f, width, 0f, width, height, 0f, height)
                }
            }
        }
    }

    private fun drawCornerHandles(
        canvas: Canvas,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        x4: Float, y4: Float
    ) {
        // Top-Left (1)
        canvas.drawCircle(x1, y1, handleRadius, handlePaint)
        canvas.drawCircle(x1, y1, handleRadius, handleBorderPaint)

        // Top-Right (2)
        canvas.drawCircle(x2, y2, handleRadius, handlePaint)
        canvas.drawCircle(x2, y2, handleRadius, handleBorderPaint)

        // Bottom-Right (3)
        canvas.drawCircle(x3, y3, handleRadius, handlePaint)
        canvas.drawCircle(x3, y3, handleRadius, handleBorderPaint)

        // Bottom-Left (4)
        canvas.drawCircle(x4, y4, handleRadius, handlePaint)
        canvas.drawCircle(x4, y4, handleRadius, handleBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEditMode || mask.isLocked) {
            // When touch-through is disabled, consume touch events to block interactions with underlying apps/screen
            return !mask.isTouchThrough
        }

        val rawX = event.rawX
        val rawY = event.rawY
        val localX = event.x
        val localY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onSelect(mask.id)
                lastX = rawX
                lastY = rawY
                activeHandle = determineHandle(localX, localY)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - lastX
                val dy = rawY - lastY

                val currWidthPx = (mask.widthRatio * screenWidth)
                val currHeightPx = (mask.heightRatio * screenHeight)
                val currXPx = (mask.xRatio * screenWidth)
                val currYPx = (mask.yRatio * screenHeight)

                if (mask.shapeType == MaskEntity.SHAPE_FREE_QUAD) {
                    // Calculate current absolute screen points for all 4 corners
                    var x1 = currXPx + mask.p1x * currWidthPx
                    var y1 = currYPx + mask.p1y * currHeightPx
                    var x2 = currXPx + mask.p2x * currWidthPx
                    var y2 = currYPx + mask.p2y * currHeightPx
                    var x3 = currXPx + mask.p3x * currWidthPx
                    var y3 = currYPx + mask.p3y * currHeightPx
                    var x4 = currXPx + mask.p4x * currWidthPx
                    var y4 = currYPx + mask.p4y * currHeightPx

                    when (activeHandle) {
                        Handle.TOP_LEFT -> {
                            x1 += dx
                            y1 += dy
                        }
                        Handle.TOP_RIGHT -> {
                            x2 += dx
                            y2 += dy
                        }
                        Handle.BOTTOM_RIGHT -> {
                            x3 += dx
                            y3 += dy
                        }
                        Handle.BOTTOM_LEFT -> {
                            x4 += dx
                            y4 += dy
                        }
                        Handle.CENTER -> {
                            x1 += dx
                            y1 += dy
                            x2 += dx
                            y2 += dy
                            x3 += dx
                            y3 += dy
                            x4 += dx
                            y4 += dy
                        }
                        Handle.NONE -> {}
                    }

                    // Dynamically calculate bounding box enclosing all 4 points
                    val minX = minOf(x1, x2, x3, x4)
                    val maxX = maxOf(x1, x2, x3, x4)
                    val minY = minOf(y1, y2, y3, y4)
                    val maxY = maxOf(y1, y2, y3, y4)

                    val newWidthPx = max(maxX - minX, 15f)
                    val newHeightPx = max(maxY - minY, 15f)

                    val newXRatio = (minX / screenWidth).coerceIn(-0.2f, 1.2f)
                    val newYRatio = (minY / screenHeight).coerceIn(-0.2f, 1.2f)
                    val newWRatio = (newWidthPx / screenWidth).coerceIn(0.01f, 1.5f)
                    val newHRatio = (newHeightPx / screenHeight).coerceIn(0.005f, 1.5f)

                    val newP1x = ((x1 - minX) / newWidthPx).coerceIn(0f, 1f)
                    val newP1y = ((y1 - minY) / newHeightPx).coerceIn(0f, 1f)
                    val newP2x = ((x2 - minX) / newWidthPx).coerceIn(0f, 1f)
                    val newP2y = ((y2 - minY) / newHeightPx).coerceIn(0f, 1f)
                    val newP3x = ((x3 - minX) / newWidthPx).coerceIn(0f, 1f)
                    val newP3y = ((y3 - minY) / newHeightPx).coerceIn(0f, 1f)
                    val newP4x = ((x4 - minX) / newWidthPx).coerceIn(0f, 1f)
                    val newP4y = ((y4 - minY) / newHeightPx).coerceIn(0f, 1f)

                    val updated = mask.copy(
                        xRatio = newXRatio,
                        yRatio = newYRatio,
                        widthRatio = newWRatio,
                        heightRatio = newHRatio,
                        p1x = newP1x,
                        p1y = newP1y,
                        p2x = newP2x,
                        p2y = newP2y,
                        p3x = newP3x,
                        p3y = newP3y,
                        p4x = newP4x,
                        p4y = newP4y,
                        updatedAt = System.currentTimeMillis()
                    )

                    lastX = rawX
                    lastY = rawY
                    mask = updated
                    onUpdate(updated)
                    return true
                }

                // Standard resize / movement logic for rect, circle, rounded
                val minWidthPx = 10f
                val minHeightPx = 10f

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

                val minWRatio = 5f / max(screenWidth.toFloat(), 1f)
                val minHRatio = 5f / max(screenHeight.toFloat(), 1f)
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
        val touchMargin = handleRadius * 2.8f

        if (mask.shapeType == MaskEntity.SHAPE_FREE_QUAD) {
            val x1 = mask.p1x * w
            val y1 = mask.p1y * h
            val x2 = mask.p2x * w
            val y2 = mask.p2y * h
            val x3 = mask.p3x * w
            val y3 = mask.p3y * h
            val x4 = mask.p4x * w
            val y4 = mask.p4y * h

            return when {
                RectF(x1 - touchMargin, y1 - touchMargin, x1 + touchMargin, y1 + touchMargin).contains(x, y) -> Handle.TOP_LEFT
                RectF(x2 - touchMargin, y2 - touchMargin, x2 + touchMargin, y2 + touchMargin).contains(x, y) -> Handle.TOP_RIGHT
                RectF(x3 - touchMargin, y3 - touchMargin, x3 + touchMargin, y3 + touchMargin).contains(x, y) -> Handle.BOTTOM_RIGHT
                RectF(x4 - touchMargin, y4 - touchMargin, x4 + touchMargin, y4 + touchMargin).contains(x, y) -> Handle.BOTTOM_LEFT
                else -> Handle.CENTER
            }
        }

        return when {
            RectF(-touchMargin, -touchMargin, touchMargin, touchMargin).contains(x, y) -> Handle.TOP_LEFT
            RectF(w - touchMargin, -touchMargin, w + touchMargin, touchMargin).contains(x, y) -> Handle.TOP_RIGHT
            RectF(-touchMargin, h - touchMargin, touchMargin, h + touchMargin).contains(x, y) -> Handle.BOTTOM_LEFT
            RectF(w - touchMargin, h - touchMargin, w + touchMargin, h + touchMargin).contains(x, y) -> Handle.BOTTOM_RIGHT
            else -> Handle.CENTER
        }
    }
}
