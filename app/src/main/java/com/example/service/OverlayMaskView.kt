package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import com.example.data.MaskEntity
import kotlin.math.max

@SuppressLint("ViewClickAndTouchUnspecified")
class OverlayMaskView(
    context: Context,
    var mask: MaskEntity,
    var isEditMode: Boolean,
    var isMaskSelected: Boolean,
    private var screenWidth: Int,
    private var screenHeight: Int,
    private var screenRotation: Int = Surface.ROTATION_0,
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

    private var currentLayout: PhysicalPixelTransformer.TransformedMaskLayout =
        PhysicalPixelTransformer.calculateMaskLayout(mask, screenWidth, screenHeight, screenRotation)

    init {
        updatePaint()
    }

    fun updateData(
        newMask: MaskEntity,
        editMode: Boolean,
        selected: Boolean,
        screenW: Int = screenWidth,
        screenH: Int = screenHeight,
        rotation: Int = screenRotation
    ) {
        this.mask = newMask
        this.isEditMode = editMode
        this.isMaskSelected = selected
        this.screenWidth = screenW
        this.screenHeight = screenH
        this.screenRotation = rotation
        this.currentLayout = PhysicalPixelTransformer.calculateMaskLayout(mask, screenWidth, screenHeight, screenRotation)
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

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val layout = currentLayout
        val rect = RectF(0f, 0f, w, h)
        val cornerRadiusPx = mask.cornerRadiusDp * context.resources.displayMetrics.density

        // 1. Draw Mask Shape
        when (mask.shapeType) {
            MaskEntity.SHAPE_CIRCLE -> {
                canvas.drawOval(rect, maskPaint)
            }
            MaskEntity.SHAPE_ROUNDED -> {
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, maskPaint)
            }
            MaskEntity.SHAPE_FREE_QUAD, MaskEntity.SHAPE_RECT -> {
                quadPath.reset()
                quadPath.moveTo(layout.q1.x, layout.q1.y)
                quadPath.lineTo(layout.q2.x, layout.q2.y)
                quadPath.lineTo(layout.q3.x, layout.q3.y)
                quadPath.lineTo(layout.q4.x, layout.q4.y)
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
                    drawCornerHandles(canvas, layout.q1.x, layout.q1.y, layout.q2.x, layout.q2.y, layout.q3.x, layout.q3.y, layout.q4.x, layout.q4.y)
                }
                MaskEntity.SHAPE_ROUNDED -> {
                    canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paintToUse)
                    drawCornerHandles(canvas, layout.q1.x, layout.q1.y, layout.q2.x, layout.q2.y, layout.q3.x, layout.q3.y, layout.q4.x, layout.q4.y)
                }
                else -> {
                    canvas.drawPath(quadPath, paintToUse)
                    drawCornerHandles(canvas, layout.q1.x, layout.q1.y, layout.q2.x, layout.q2.y, layout.q3.x, layout.q3.y, layout.q4.x, layout.q4.y)
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

                val deltaPhys = PhysicalPixelTransformer.windowDeltaToPhysical(dx, dy, screenRotation)
                val dXp = deltaPhys.x
                val dYp = deltaPhys.y

                val (w0, h0) = PhysicalPixelTransformer.getNaturalDimensions(screenWidth, screenHeight, screenRotation)
                val currWidthPx = mask.widthRatio * w0
                val currHeightPx = mask.heightRatio * h0
                val currXPx = mask.xRatio * w0
                val currYPx = mask.yRatio * h0

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
                        x1 += dXp
                        y1 += dYp
                    }
                    Handle.TOP_RIGHT -> {
                        x2 += dXp
                        y2 += dYp
                    }
                    Handle.BOTTOM_RIGHT -> {
                        x3 += dXp
                        y3 += dYp
                    }
                    Handle.BOTTOM_LEFT -> {
                        x4 += dXp
                        y4 += dYp
                    }
                    Handle.CENTER -> {
                        x1 += dXp
                        y1 += dYp
                        x2 += dXp
                        y2 += dYp
                        x3 += dXp
                        y3 += dYp
                        x4 += dXp
                        y4 += dYp
                    }
                    Handle.NONE -> {}
                }

                // Recalculate bounding box in physical space
                val minX = minOf(x1, x2, x3, x4)
                val maxX = maxOf(x1, x2, x3, x4)
                val minY = minOf(y1, y2, y3, y4)
                val maxY = maxOf(y1, y2, y3, y4)

                val newWidthPx = max(maxX - minX, 10f)
                val newHeightPx = max(maxY - minY, 10f)

                val newXRatio = (minX / w0).coerceIn(-0.2f, 1.2f)
                val newYRatio = (minY / h0).coerceIn(-0.2f, 1.2f)
                val newWRatio = (newWidthPx / w0).coerceIn(0.001f, 1.5f)
                val newHRatio = (newHeightPx / h0).coerceIn(0.001f, 1.5f)

                val isFreeQuad = mask.shapeType == MaskEntity.SHAPE_FREE_QUAD
                val newP1x = if (isFreeQuad) ((x1 - minX) / newWidthPx).coerceIn(0f, 1f) else 0f
                val newP1y = if (isFreeQuad) ((y1 - minY) / newHeightPx).coerceIn(0f, 1f) else 0f
                val newP2x = if (isFreeQuad) ((x2 - minX) / newWidthPx).coerceIn(0f, 1f) else 1f
                val newP2y = if (isFreeQuad) ((y2 - minY) / newHeightPx).coerceIn(0f, 1f) else 0f
                val newP3x = if (isFreeQuad) ((x3 - minX) / newWidthPx).coerceIn(0f, 1f) else 1f
                val newP3y = if (isFreeQuad) ((y3 - minY) / newHeightPx).coerceIn(0f, 1f) else 1f
                val newP4x = if (isFreeQuad) ((x4 - minX) / newWidthPx).coerceIn(0f, 1f) else 0f
                val newP4y = if (isFreeQuad) ((y4 - minY) / newHeightPx).coerceIn(0f, 1f) else 1f

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
                currentLayout = PhysicalPixelTransformer.calculateMaskLayout(mask, screenWidth, screenHeight, screenRotation)
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
        val touchMargin = handleRadius * 2.8f
        val layout = currentLayout
        return when {
            RectF(layout.q1.x - touchMargin, layout.q1.y - touchMargin, layout.q1.x + touchMargin, layout.q1.y + touchMargin).contains(x, y) -> Handle.TOP_LEFT
            RectF(layout.q2.x - touchMargin, layout.q2.y - touchMargin, layout.q2.x + touchMargin, layout.q2.y + touchMargin).contains(x, y) -> Handle.TOP_RIGHT
            RectF(layout.q3.x - touchMargin, layout.q3.y - touchMargin, layout.q3.x + touchMargin, layout.q3.y + touchMargin).contains(x, y) -> Handle.BOTTOM_RIGHT
            RectF(layout.q4.x - touchMargin, layout.q4.y - touchMargin, layout.q4.x + touchMargin, layout.q4.y + touchMargin).contains(x, y) -> Handle.BOTTOM_LEFT
            else -> Handle.CENTER
        }
    }
}
