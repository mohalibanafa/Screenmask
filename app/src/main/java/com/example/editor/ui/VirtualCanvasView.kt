package com.example.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.PointFData
import com.example.data.model.ShapeObject
import com.example.data.model.ShapeType
import com.example.editor.transform.SnapHelper
import com.example.editor.transform.SnapResult
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VirtualCanvasView(
    modifier: Modifier = Modifier,
    shapes: List<ShapeObject>,
    selectedShapeIds: Set<String>,
    currentTool: EditorTool,
    virtualWidth: Float = 1080f,
    virtualHeight: Float = 2400f,
    zoomScale: Float = 1.0f,
    panOffset: Offset = Offset.Zero,
    showGrid: Boolean = true,
    gridSizePx: Float = 40f,
    snapToGrid: Boolean = true,
    snapToEdges: Boolean = true,
    snapToCenter: Boolean = true,
    isPreviewMode: Boolean = false,
    onSelectShape: (String?, Boolean) -> Unit, // (id, isMultiSelect)
    onShapeTransformed: (ShapeObject) -> Unit,
    onShapeTransformFinished: () -> Unit,
    onNewShapeCreated: (ShapeObject) -> Unit,
    onZoomPanChanged: (Float, Offset) -> Unit
) {
    // Interactive drag state
    var activeHandle by remember { mutableStateOf(HandleType.NONE) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var shapeStartRect by remember { mutableStateOf<ShapeObject?>(null) }
    var tempDrawingShape by remember { mutableStateOf<ShapeObject?>(null) }
    var currentSnapResult by remember { mutableStateOf<SnapResult?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .pointerInput(currentTool, zoomScale, panOffset, isPreviewMode) {
                if (isPreviewMode) return@pointerInput

                detectTransformGestures { _, pan, zoom, _ ->
                    val newZoom = (zoomScale * zoom).coerceIn(0.2f, 5.0f)
                    val newPan = panOffset + pan
                    onZoomPanChanged(newZoom, newPan)
                }
            }
            .pointerInput(currentTool, shapes, selectedShapeIds, zoomScale, panOffset, isPreviewMode) {
                if (isPreviewMode) return@pointerInput

                detectTapGestures { tapOffset ->
                    val canvasCoord = toVirtualCanvasCoords(
                        tapOffset, size.width.toFloat(), size.height.toFloat(),
                        virtualWidth, virtualHeight, zoomScale, panOffset
                    )

                    if (currentTool is EditorTool.Draw) {
                        // Create shape on tap if not dragging
                        val newShape = ShapeObject(
                            type = currentTool.shapeType,
                            title = "${currentTool.shapeType.name.lowercase().replaceFirstChar { it.uppercase() }} ${shapes.size + 1}",
                            x = canvasCoord.x - 100f,
                            y = canvasCoord.y - 100f,
                            width = 200f,
                            height = 200f,
                            colorArgb = 0xFF000000L,
                            alpha = 1.0f,
                            zIndex = shapes.size
                        )
                        onNewShapeCreated(newShape)
                    } else {
                        // Selection hit testing
                        val hit = findTopShapeAt(shapes, canvasCoord)
                        val isMulti = currentTool is EditorTool.MultiSelect
                        onSelectShape(hit?.id, isMulti)
                    }
                }
            }
            .pointerInput(currentTool, shapes, selectedShapeIds, zoomScale, panOffset, isPreviewMode) {
                if (isPreviewMode) return@pointerInput

                detectDragGestures(
                    onDragStart = { startOffset ->
                        dragStartOffset = startOffset
                        val canvasCoord = toVirtualCanvasCoords(
                            startOffset, size.width.toFloat(), size.height.toFloat(),
                            virtualWidth, virtualHeight, zoomScale, panOffset
                        )

                        if (currentTool is EditorTool.Draw) {
                            tempDrawingShape = ShapeObject(
                                type = currentTool.shapeType,
                                title = "${currentTool.shapeType.name.lowercase().replaceFirstChar { it.uppercase() }} ${shapes.size + 1}",
                                x = canvasCoord.x,
                                y = canvasCoord.y,
                                width = 10f,
                                height = 10f,
                                colorArgb = 0xFF000000L,
                                alpha = 1.0f,
                                zIndex = shapes.size
                            )
                        } else {
                            val selectedShapes = shapes.filter { it.id in selectedShapeIds }
                            if (selectedShapes.isNotEmpty()) {
                                val primary = selectedShapes.last()
                                val handle = detectHandleHit(primary, canvasCoord, zoomScale)
                                activeHandle = handle
                                shapeStartRect = primary
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currCanvasCoord = toVirtualCanvasCoords(
                            change.position, size.width.toFloat(), size.height.toFloat(),
                            virtualWidth, virtualHeight, zoomScale, panOffset
                        )
                        val startCanvasCoord = toVirtualCanvasCoords(
                            dragStartOffset, size.width.toFloat(), size.height.toFloat(),
                            virtualWidth, virtualHeight, zoomScale, panOffset
                        )

                        if (currentTool is EditorTool.Draw && tempDrawingShape != null) {
                            val startX = startCanvasCoord.x
                            val startY = startCanvasCoord.y
                            val curX = currCanvasCoord.x
                            val curY = currCanvasCoord.y

                            val left = minOf(startX, curX)
                            val top = minOf(startY, curY)
                            val w = maxOf(30f, Math.abs(curX - startX))
                            val h = maxOf(30f, Math.abs(curY - startY))

                            tempDrawingShape = tempDrawingShape?.copy(
                                x = left,
                                y = top,
                                width = w,
                                height = h
                            )
                        } else if (shapeStartRect != null) {
                            val target = shapeStartRect!!
                            if (target.isLocked) return@detectDragGestures

                            var updated = target
                            val dx = currCanvasCoord.x - startCanvasCoord.x
                            val dy = currCanvasCoord.y - startCanvasCoord.y

                            when (activeHandle) {
                                HandleType.ROTATE -> {
                                    val centerX = target.x + target.width / 2f
                                    val centerY = target.y + target.height / 2f
                                    val angleRad = atan2((currCanvasCoord.y - centerY).toDouble(), (currCanvasCoord.x - centerX).toDouble())
                                    var degrees = Math.toDegrees(angleRad).toFloat() + 90f
                                    if (degrees < 0) degrees += 360f
                                    updated = target.copy(rotation = degrees % 360f)
                                }
                                HandleType.TOP_LEFT -> {
                                    val newW = maxOf(30f, target.width - dx)
                                    val newH = maxOf(30f, target.height - dy)
                                    val newX = target.x + (target.width - newW)
                                    val newY = target.y + (target.height - newH)
                                    updated = target.copy(x = newX, y = newY, width = newW, height = newH)
                                }
                                HandleType.TOP_RIGHT -> {
                                    val newW = maxOf(30f, target.width + dx)
                                    val newH = maxOf(30f, target.height - dy)
                                    val newY = target.y + (target.height - newH)
                                    updated = target.copy(y = newY, width = newW, height = newH)
                                }
                                HandleType.BOTTOM_LEFT -> {
                                    val newW = maxOf(30f, target.width - dx)
                                    val newH = maxOf(30f, target.height + dy)
                                    val newX = target.x + (target.width - newW)
                                    updated = target.copy(x = newX, width = newW, height = newH)
                                }
                                HandleType.BOTTOM_RIGHT -> {
                                    val newW = maxOf(30f, target.width + dx)
                                    val newH = maxOf(30f, target.height + dy)
                                    updated = target.copy(width = newW, height = newH)
                                }
                                else -> { // Drag move
                                    val rawX = target.x + dx
                                    val rawY = target.y + dy

                                    val snap = SnapHelper.calculateSnap(
                                        rawX, rawY, target.width, target.height,
                                        virtualWidth, virtualHeight, gridSizePx,
                                        snapToGrid, snapToEdges, snapToCenter
                                    )
                                    currentSnapResult = snap
                                    updated = target.copy(x = snap.snappedX, y = snap.snappedY)
                                }
                            }
                            onShapeTransformed(updated)
                        }
                    },
                    onDragEnd = {
                        if (tempDrawingShape != null) {
                            onNewShapeCreated(tempDrawingShape!!)
                            tempDrawingShape = null
                        }
                        activeHandle = HandleType.NONE
                        shapeStartRect = null
                        currentSnapResult = null
                        onShapeTransformFinished()
                    },
                    onDragCancel = {
                        tempDrawingShape = null
                        activeHandle = HandleType.NONE
                        shapeStartRect = null
                        currentSnapResult = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewWidth = size.width
            val viewHeight = size.height

            // Calculate virtual screen placement centered in viewport
            val scale = minOf(viewWidth / virtualWidth, viewHeight / virtualHeight) * zoomScale
            val canvasPxWidth = virtualWidth * scale
            val canvasPxHeight = virtualHeight * scale

            val leftPx = (viewWidth - canvasPxWidth) / 2f + panOffset.x
            val topPx = (viewHeight - canvasPxHeight) / 2f + panOffset.y

            withTransform({
                translate(leftPx, topPx)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                // 1. Draw Virtual Phone Screen Background
                drawRect(
                    color = Color(0xFF0F172A), // Dark canvas screen
                    size = Size(virtualWidth, virtualHeight)
                )

                // 2. Draw Grid Lines if enabled (Editor mode only)
                if (showGrid && !isPreviewMode) {
                    var x = 0f
                    while (x <= virtualWidth) {
                        drawLine(
                            color = Color(0x22FFFFFF),
                            start = Offset(x, 0f),
                            end = Offset(x, virtualHeight),
                            strokeWidth = 1f
                        )
                        x += gridSizePx
                    }
                    var y = 0f
                    while (y <= virtualHeight) {
                        drawLine(
                            color = Color(0x22FFFFFF),
                            start = Offset(0f, y),
                            end = Offset(virtualWidth, y),
                            strokeWidth = 1f
                        )
                        y += gridSizePx
                    }
                }

                // 3. Render Shapes
                val sortedShapes = shapes.sortedBy { it.zIndex }
                sortedShapes.forEach { shape ->
                    if (shape.isVisible) {
                        drawShapeObject(shape)
                    }
                }

                // Temporary drawing shape
                tempDrawingShape?.let { drawShapeObject(it) }

                // 4. Snap Guidelines
                currentSnapResult?.let { snap ->
                    if (snap.showVerticalGuideline && !isPreviewMode) {
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(snap.verticalGuidelineX, 0f),
                            end = Offset(snap.verticalGuidelineX, virtualHeight),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                    if (snap.showHorizontalGuideline && !isPreviewMode) {
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(0f, snap.horizontalGuidelineY),
                            end = Offset(virtualWidth, snap.horizontalGuidelineY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                }

                // 5. Bounding Box & Handles for Selected Shapes
                if (!isPreviewMode) {
                    sortedShapes.filter { it.id in selectedShapeIds }.forEach { selected ->
                        drawSelectionBoundingBox(selected, scale)
                    }
                }

                // Screen Border Outline
                drawRect(
                    color = Color(0xFF64748B),
                    size = Size(virtualWidth, virtualHeight),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

private fun DrawScope.drawShapeObject(shape: ShapeObject) {
    val color = Color(shape.colorArgb.toInt()).copy(alpha = shape.alpha)

    withTransform({
        val pivotX = shape.x + shape.width / 2f
        val pivotY = shape.y + shape.height / 2f
        rotate(degrees = shape.rotation, pivot = Offset(pivotX, pivotY))
    }) {
        when (shape.type) {
            ShapeType.RECTANGLE -> {
                drawRect(
                    color = color,
                    topLeft = Offset(shape.x, shape.y),
                    size = Size(shape.width, shape.height)
                )
            }
            ShapeType.ROUNDED_RECT -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(shape.x, shape.y),
                    size = Size(shape.width, shape.height),
                    cornerRadius = CornerRadius(shape.cornerRadius, shape.cornerRadius)
                )
            }
            ShapeType.CIRCLE -> {
                drawOval(
                    color = color,
                    topLeft = Offset(shape.x, shape.y),
                    size = Size(shape.width, shape.height)
                )
            }
            ShapeType.LINE -> {
                drawLine(
                    color = color,
                    start = Offset(shape.x, shape.y + shape.height / 2f),
                    end = Offset(shape.x + shape.width, shape.y + shape.height / 2f),
                    strokeWidth = maxOf(4f, shape.height)
                )
            }
            ShapeType.POLYGON -> {
                val path = Path()
                val points = if (shape.points.isNotEmpty()) shape.points else defaultPolygonPoints()
                points.forEachIndexed { i, pt ->
                    val px = shape.x + pt.x * shape.width
                    val py = shape.y + pt.y * shape.height
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path = path, color = color)
            }
            ShapeType.FREEHAND_PATH -> {
                val path = Path()
                if (shape.points.isNotEmpty()) {
                    shape.points.forEachIndexed { i, pt ->
                        val px = shape.x + pt.x * shape.width
                        val py = shape.y + pt.y * shape.height
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(path = path, color = color, style = Stroke(width = maxOf(6f, shape.cornerRadius)))
                } else {
                    drawRect(color = color, topLeft = Offset(shape.x, shape.y), size = Size(shape.width, shape.height))
                }
            }
        }
    }
}

private fun defaultPolygonPoints(): List<PointFData> {
    return listOf(
        PointFData(0.5f, 0f),
        PointFData(1f, 0.4f),
        PointFData(0.8f, 1f),
        PointFData(0.2f, 1f),
        PointFData(0f, 0.4f)
    )
}

private fun DrawScope.drawSelectionBoundingBox(shape: ShapeObject, zoomScale: Float) {
    val pivotX = shape.x + shape.width / 2f
    val pivotY = shape.y + shape.height / 2f

    withTransform({
        rotate(degrees = shape.rotation, pivot = Offset(pivotX, pivotY))
    }) {
        val outlineColor = if (shape.isLocked) Color(0xFFF59E0B) else Color(0xFFF43F5E)

        drawRect(
            color = outlineColor,
            topLeft = Offset(shape.x, shape.y),
            size = Size(shape.width, shape.height),
            style = Stroke(width = 3f)
        )

        if (!shape.isLocked) {
            val handleRadius = 14f
            val topRotationOffset = 40f

            // Corner Handles
            val corners = listOf(
                Offset(shape.x, shape.y),
                Offset(shape.x + shape.width, shape.y),
                Offset(shape.x, shape.y + shape.height),
                Offset(shape.x + shape.width, shape.y + shape.height)
            )

            corners.forEach { pt ->
                drawCircle(color = Color.White, radius = handleRadius, center = pt)
                drawCircle(color = outlineColor, radius = handleRadius, center = pt, style = Stroke(width = 3f))
            }

            // Top Rotation Handle
            val rotHandle = Offset(pivotX, shape.y - topRotationOffset)
            drawLine(
                color = outlineColor,
                start = Offset(pivotX, shape.y),
                end = rotHandle,
                strokeWidth = 2f
            )
            drawCircle(color = Color(0xFF38BDF8), radius = handleRadius + 2f, center = rotHandle)
            drawCircle(color = Color.White, radius = handleRadius + 2f, center = rotHandle, style = Stroke(width = 2f))
        }
    }
}

private fun toVirtualCanvasCoords(
    touch: Offset,
    viewWidth: Float,
    viewHeight: Float,
    virtualWidth: Float,
    virtualHeight: Float,
    zoomScale: Float,
    panOffset: Offset
): Offset {
    val scale = minOf(viewWidth / virtualWidth, viewHeight / virtualHeight) * zoomScale
    val canvasPxWidth = virtualWidth * scale
    val canvasPxHeight = virtualHeight * scale

    val leftPx = (viewWidth - canvasPxWidth) / 2f + panOffset.x
    val topPx = (viewHeight - canvasPxHeight) / 2f + panOffset.y

    val vx = (touch.x - leftPx) / scale
    val vy = (touch.y - topPx) / scale
    return Offset(vx, vy)
}

private fun findTopShapeAt(shapes: List<ShapeObject>, point: Offset): ShapeObject? {
    return shapes.sortedByDescending { it.zIndex }.firstOrNull { shape ->
        val right = shape.x + shape.width
        val bottom = shape.y + shape.height
        point.x >= shape.x && point.x <= right && point.y >= shape.y && point.y <= bottom
    }
}

enum class HandleType {
    NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, ROTATE
}

private fun detectHandleHit(shape: ShapeObject, point: Offset, zoomScale: Float): HandleType {
    val radius = 40f
    val pivotX = shape.x + shape.width / 2f
    val topRot = Offset(pivotX, shape.y - 40f)

    if (dist(point, topRot) <= radius) return HandleType.ROTATE
    if (dist(point, Offset(shape.x, shape.y)) <= radius) return HandleType.TOP_LEFT
    if (dist(point, Offset(shape.x + shape.width, shape.y)) <= radius) return HandleType.TOP_RIGHT
    if (dist(point, Offset(shape.x, shape.y + shape.height)) <= radius) return HandleType.BOTTOM_LEFT
    if (dist(point, Offset(shape.x + shape.width, shape.y + shape.height)) <= radius) return HandleType.BOTTOM_RIGHT

    if (point.x >= shape.x && point.x <= shape.x + shape.width && point.y >= shape.y && point.y <= shape.y + shape.height) {
        return HandleType.MOVE
    }
    return HandleType.NONE
}

private fun dist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
}
