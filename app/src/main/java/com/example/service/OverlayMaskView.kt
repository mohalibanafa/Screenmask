package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
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
    private val onSelect: (String) -> Unit,
    private val onLayoutUpdate: ((WindowManager.LayoutParams) -> Unit)? = null
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
    private val editPaddingPx get() = if (isEditMode && !mask.isLocked) 32f * context.resources.displayMetrics.density else 0f
    private val quadPath = Path()

    // Touch dragging / resizing state
    private var lastX = 0f
    private var lastY = 0f
    private var activeHandle = Handle.NONE

    // Cached state at the moment ACTION_DOWN occurs
    private var initialX1 = 0f
    private var initialY1 = 0f
    private var initialX2 = 0f
    private var initialY2 = 0f
    private var initialX3 = 0f
    private var initialY3 = 0f
    private var initialX4 = 0f
    private var initialY4 = 0f

    private var initialMinX = 0f
    private var initialMaxX = 0f
    private var initialMinY = 0f
    private var initialMaxY = 0f

    private enum class Handle {
        NONE, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT
    }

    private var currentLayout: PhysicalPixelTransformer.TransformedMaskLayout =
        PhysicalPixelTransformer.calculateMaskLayout(mask, screenWidth, screenHeight, screenRotation, editPaddingPx)

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
        this.currentLayout = PhysicalPixelTransformer.calculateMaskLayout(mask, screenWidth, screenHeight, screenRotation, editPaddingPx)
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
        val cornerRadiusPx = mask.cornerRadiusDp * context.resources.displayMetrics.density

        // Compute inner bounding box for circle / rounded shapes
        val minX = minOf(layout.q1.x, layout.q2.x, layout.q3.x, layout.q4.x)
        val maxX = maxOf(layout.q1.x, layout.q2.x, layout.q3.x, layout.q4.x)
        val minY = minOf(layout.q1.y, layout.q2.y, layout.q3.y, layout.q4.y)
        val maxY = maxOf(layout.q1.y, layout.q2.y, layout.q3.y, layout.q4.y)
        val shapeRect = RectF(minX, minY, maxX, maxY)

        // 1. Draw Mask Shape
        when (mask.shapeType) {
            MaskEntity.SHAPE_CIRCLE -> {
                canvas.drawOval(shapeRect, maskPaint)
            }
            MaskEntity.SHAPE_ROUNDED -> {
                canvas.drawRoundRect(shapeRect, cornerRadiusPx, cornerRadiusPx, maskPaint)
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
                canvas.drawRect(shapeRect, maskPaint)
            }
        }

        // 2. In Edit Mode (if not locked), draw edit outline and corner handles
        if (isEditMode && !mask.isLocked) {
            val paintToUse = if (isMaskSelected) selectedBorderPaint else borderPaint

            when (mask.shapeType) {
                MaskEntity.SHAPE_CIRCLE -> {
                    canvas.drawOval(shapeRect, paintToUse)
                    drawCornerHandles(canvas, layout.q1.x, layout.q1.y, layout.q2.x, layout.q2.y, layout.q3.x, layout.q3.y, layout.q4.x, layout.q4.y)
                }
                MaskEntity.SHAPE_ROUNDED -> {
                    canvas.drawRoundRect(shapeRect, cornerRadiusPx, cornerRadiusPx, paintToUse)
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
        // Corner 1 (Top-Left)
        canvas.drawCircle(x1, y1, handleRadius, handlePaint)
        canvas.drawCircle(x1, y1, handleRadius, handleBorderPaint)

        // Corner 2 (Top-Right)
        canvas.drawCircle(x2, y2, handleRadius, handlePaint)
        canvas.drawCircle(x2, y2, handleRadius, handleBorderPaint)

        // Corner 3 (Bottom-Right)
        canvas.drawCircle(x3, y3, handleRadius, handlePaint)
        canvas.drawCircle(x3, y3, handleRadius, handleBorderPaint)

        // Corner 4 (Bottom-Left)
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

                val (w0, h0) = PhysicalPixelTransformer.getNaturalDimensions(screenWidth, screenHeight, screenRotation)
                val currWidthPx = mask.widthRatio * w0
                val currHeightPx = mask.heightRatio * h0
                val currXPx = mask.xRatio * w0
                val currYPx = mask.yRatio * h0

                initialX1 = currXPx + mask.p1x * currWidthPx
                initialY1 = currYPx + mask.p1y * currHeightPx
                initialX2 = currXPx + mask.p2x * currWidthPx
                initialY2 = currYPx + mask.p2y * currHeightPx
                initialX3 = currXPx + mask.p3x * currWidthPx
                initialY3 = currYPx + mask.p3y * currHeightPx
                initialX4 = currXPx + mask.p4x * currWidthPx
                initialY4 = currYPx + mask.p4y * currHeightPx

                initialMinX = minOf(initialX1, initialX2, initialX3, initialX4)
                initialMaxX = maxOf(initialX1, initialX2, initialX3, initialX4)
                initialMinY = minOf(initialY1, initialY2, initialY3, initialY4)
                initialMaxY = maxOf(initialY1, initialY2, initialY3, initialY4)

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - lastX
                val dy = rawY - lastY

                val deltaPhys = PhysicalPixelTransformer.windowDeltaToPhysical(dx, dy, screenRotation)
                val dXp = deltaPhys.x
                val dYp = deltaPhys.y

                val (w0, h0) = PhysicalPixelTransformer.getNaturalDimensions(screenWidth, screenHeight, screenRotation)
                val isFreeQuad = mask.shapeType == MaskEntity.SHAPE_FREE_QUAD

                var newMinX = initialMinX
                var newMaxX = initialMaxX
                var newMinY = initialMinY
                var newMaxY = initialMaxY

                var x1 = initialX1
                var y1 = initialY1
                var x2 = initialX2
                var y2 = initialY2
                var x3 = initialX3
                var y3 = initialY3
                var x4 = initialX4
                var y4 = initialY4

                if (!isFreeQuad) {
                    // Standard Rect / Circle / Rounded: directly expand AND shrink borders
                    val minDimension = 15f * context.resources.displayMetrics.density
                    when (activeHandle) {
                        Handle.TOP_LEFT -> {
                            newMinX = (initialMinX + dXp).coerceAtMost(initialMaxX - minDimension)
                            newMinY = (initialMinY + dYp).coerceAtMost(initialMaxY - minDimension)
                        }
                        Handle.TOP_RIGHT -> {
                            newMaxX = (initialMaxX + dXp).coerceAtLeast(initialMinX + minDimension)
                            newMinY = (initialMinY + dYp).coerceAtMost(initialMaxY - minDimension)
                        }
                        Handle.BOTTOM_RIGHT -> {
                            newMaxX = (initialMaxX + dXp).coerceAtLeast(initialMinX + minDimension)
                            newMaxY = (initialMaxY + dYp).coerceAtLeast(initialMinY + minDimension)
                        }
                        Handle.BOTTOM_LEFT -> {
                            newMinX = (initialMinX + dXp).coerceAtMost(initialMaxX - minDimension)
                            newMaxY = (initialMaxY + dYp).coerceAtLeast(initialMinY + minDimension)
                        }
                        Handle.CENTER -> {
                            newMinX = initialMinX + dXp
                            newMaxX = initialMaxX + dXp
                            newMinY = initialMinY + dYp
                            newMaxY = initialMaxY + dYp
                        }
                        Handle.NONE -> {}
                    }
                } else {
                    // Free Quad: move individual corner points with full shrinkage & enlargement
                    when (activeHandle) {
                        Handle.TOP_LEFT -> {
                            x1 = initialX1 + dXp
                            y1 = initialY1 + dYp
                        }
                        Handle.TOP_RIGHT -> {
                            x2 = initialX2 + dXp
                            y2 = initialY2 + dYp
                        }
                        Handle.BOTTOM_RIGHT -> {
                            x3 = initialX3 + dXp
                            y3 = initialY3 + dYp
                        }
                        Handle.BOTTOM_LEFT -> {
                            x4 = initialX4 + dXp
                            y4 = initialY4 + dYp
                        }
                        Handle.CENTER -> {
                            x1 = initialX1 + dXp
                            y1 = initialY1 + dYp
                            x2 = initialX2 + dXp
                            y2 = initialY2 + dYp
                            x3 = initialX3 + dXp
                            y3 = initialY3 + dYp
                            x4 = initialX4 + dXp
                            y4 = initialY4 + dYp
                        }
                        Handle.NONE -> {}
                    }

                    newMinX = minOf(x1, x2, x3, x4)
                    newMaxX = maxOf(x1, x2, x3, x4)
                    newMinY = minOf(y1, y2, y3, y4)
                    newMaxY = maxOf(y1, y2, y3, y4)
                }

                val newWidthPx = max(newMaxX - newMinX, 10f)
                val newHeightPx = max(newMaxY - newMinY, 10f)

                val newXRatio = (newMinX / w0).coerceIn(-0.3f, 1.3f)
                val newYRatio = (newMinY / h0).coerceIn(-0.3f, 1.3f)
                val newWRatio = (newWidthPx / w0).coerceIn(0.001f, 1.5f)
                val newHRatio = (newHeightPx / h0).coerceIn(0.001f, 1.5f)

                val newP1x = if (isFreeQuad) ((x1 - newMinX) / newWidthPx).coerceIn(0f, 1f) else 0f
                val newP1y = if (isFreeQuad) ((y1 - newMinY) / newHeightPx).coerceIn(0f, 1f) else 0f
                val newP2x = if (isFreeQuad) ((x2 - newMinX) / newWidthPx).coerceIn(0f, 1f) else 1f
                val newP2y = if (isFreeQuad) ((y2 - newMinY) / newHeightPx).coerceIn(0f, 1f) else 0f
                val newP3x = if (isFreeQuad) ((x3 - newMinX) / newWidthPx).coerceIn(0f, 1f) else 1f
                val newP3y = if (isFreeQuad) ((y3 - newMinY) / newHeightPx).coerceIn(0f, 1f) else 1f
                val newP4x = if (isFreeQuad) ((x4 - newMinX) / newWidthPx).coerceIn(0f, 1f) else 0f
                val newP4y = if (isFreeQuad) ((y4 - newMinY) / newHeightPx).coerceIn(0f, 1f) else 1f

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

                mask = updated
                currentLayout = PhysicalPixelTransformer.calculateMaskLayout(mask, screenWidth, screenHeight, screenRotation, editPaddingPx)
                
                // Immediately update WindowManager layout params in real-time
                val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                val newParams = WindowManager.LayoutParams(
                    currentLayout.winWidth,
                    currentLayout.winHeight,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    flags,
                    android.graphics.PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = currentLayout.winX
                    y = currentLayout.winY
                }
                onLayoutUpdate?.invoke(newParams)
                invalidate()
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
        val layout = currentLayout
        val touchRadius = 36f * context.resources.displayMetrics.density
        val touchRadiusSq = touchRadius * touchRadius

        fun distSq(px: Float, py: Float): Float {
            val dx = x - px
            val dy = y - py
            return dx * dx + dy * dy
        }

        val d1 = distSq(layout.q1.x, layout.q1.y)
        val d2 = distSq(layout.q2.x, layout.q2.y)
        val d3 = distSq(layout.q3.x, layout.q3.y)
        val d4 = distSq(layout.q4.x, layout.q4.y)

        val minDist = minOf(d1, d2, d3, d4)
        if (minDist <= touchRadiusSq) {
            return when (minDist) {
                d1 -> Handle.TOP_LEFT
                d2 -> Handle.TOP_RIGHT
                d3 -> Handle.BOTTOM_RIGHT
                d4 -> Handle.BOTTOM_LEFT
                else -> Handle.CENTER
            }
        }

        return Handle.CENTER
    }
}
