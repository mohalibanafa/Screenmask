package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.R
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("ViewClickAndTouchUnspecified")
class AdvancedColorPickerView(
    context: Context,
    private val onColorChanged: (colorArgb: Long, alpha: Float) -> Unit
) : LinearLayout(context) {

    // HSV State
    private var hue: Float = 0f          // 0f .. 360f
    private var saturation: Float = 0f   // 0f .. 1f
    private var value: Float = 0f        // 0f .. 1f
    private var currentAlpha: Float = 1f // 0f .. 1f

    private var isUpdatingFromExternal = false

    // Header (Title & Hex Preview)
    private val headerRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val p = (2 * context.resources.displayMetrics.density).toInt()
        setPadding(0, p, 0, p)
    }

    private val titleText = TextView(context).apply {
        text = context.getString(R.string.color_picker_title)
        textSize = 11.5f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.parseColor("#E2E8F0"))
        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
    }

    // Live Color Preview Chip & Hex Badge
    private val previewBadgeRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private val previewChip = View(context).apply {
        val size = (22 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            val rightMargin = (6 * context.resources.displayMetrics.density).toInt()
            setMargins(0, 0, rightMargin, 0)
        }
        background = GradientDrawable().apply {
            cornerRadius = 11f * context.resources.displayMetrics.density
            setColor(Color.BLACK)
            setStroke(2, Color.parseColor("#94A3B8"))
        }
    }

    private val hexText = TextView(context).apply {
        text = "#000000"
        textSize = 10.5f
        typeface = Typeface.MONOSPACE
        setTextColor(Color.parseColor("#38BDF8"))
        val pHoriz = (5 * context.resources.displayMetrics.density).toInt()
        val pVert = (1 * context.resources.displayMetrics.density).toInt()
        setPadding(pHoriz, pVert, pHoriz, pVert)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#1E293B"))
            cornerRadius = 6f * context.resources.displayMetrics.density
            setStroke(1, Color.parseColor("#475569"))
        }
    }

    private val alphaBadge = TextView(context).apply {
        text = "100%"
        textSize = 10.5f
        setTextColor(Color.parseColor("#94A3B8"))
        val pHoriz = (4 * context.resources.displayMetrics.density).toInt()
        setPadding(pHoriz, 0, 0, 0)
    }

    // 2D Saturation / Value Canvas
    private val satValPicker = SatValView(context) { sat, v ->
        saturation = sat
        value = v
        updatePreviewAndNotify()
    }

    // Hue Rainbow Slider Canvas
    private val hueSlider = HueSliderView(context) { h ->
        hue = h
        satValPicker.setHue(h)
        updatePreviewAndNotify()
    }

    // Alpha / Transparency Slider Canvas
    private val alphaSlider = AlphaSliderView(context) { a ->
        currentAlpha = a
        updatePreviewAndNotify()
    }

    // Quick Swatches Palette Row
    private val swatchesRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val p = (2 * context.resources.displayMetrics.density).toInt()
        setPadding(0, p, 0, p)
    }

    private val swatchesScrollView = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
    }

    private val swatchColors = listOf(
        0xFF000000L, // Pure Pitch Black (OLED / Cracks)
        0xFF0F172AL, // Deep Navy Slate
        0xFF1E293BL, // Charcoal Dark
        0xFF475569L, // Medium Slate Gray
        0xFF94A3B8L, // Light Silver Gray
        0xFFFFFFFFL, // Pure White
        0xFFEF4444L, // Crimson Red
        0xFFF97316L, // Vivid Orange
        0xFFF59E0BL, // Amber Yellow
        0xFF10B981L, // Emerald Green
        0xFF0284C7L, // Bright Cyan / Sky Blue
        0xFF3B82F6L, // Royal Blue
        0xFF7C3AEDL, // Deep Violet Purple
        0xFFEC4899L  // Vibrant Pink
    )

    init {
        orientation = VERTICAL
        val pad = (6 * context.resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#241E293B"))
            cornerRadius = 12f * context.resources.displayMetrics.density
            setStroke(1, Color.parseColor("#334155"))
        }

        // 1. Header (Title + Preview + Hex + Alpha)
        previewBadgeRow.addView(previewChip)
        previewBadgeRow.addView(hexText)
        previewBadgeRow.addView(alphaBadge)

        headerRow.addView(titleText)
        headerRow.addView(previewBadgeRow)
        addView(headerRow)

        // 2. 2D Saturation / Value Box
        val satValLp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            (90 * context.resources.displayMetrics.density).toInt()
        ).apply {
            val topMargin = (6 * context.resources.displayMetrics.density).toInt()
            setMargins(0, topMargin, 0, 0)
        }
        satValPicker.layoutParams = satValLp
        addView(satValPicker)

        // 3. Hue Rainbow Slider
        val hueLp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            (26 * context.resources.displayMetrics.density).toInt()
        ).apply {
            val topMargin = (6 * context.resources.displayMetrics.density).toInt()
            setMargins(0, topMargin, 0, 0)
        }
        hueSlider.layoutParams = hueLp
        addView(hueSlider)

        // 4. Alpha / Opacity Slider
        val alphaLp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            (26 * context.resources.displayMetrics.density).toInt()
        ).apply {
            val topMargin = (5 * context.resources.displayMetrics.density).toInt()
            setMargins(0, topMargin, 0, 0)
        }
        alphaSlider.layoutParams = alphaLp
        addView(alphaSlider)

        // 5. Quick Swatches Row
        buildSwatches()
        swatchesScrollView.addView(swatchesRow)
        val swatchesLp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            val topMargin = (6 * context.resources.displayMetrics.density).toInt()
            setMargins(0, topMargin, 0, 0)
        }
        swatchesScrollView.layoutParams = swatchesLp
        addView(swatchesScrollView)
    }

    private fun buildSwatches() {
        swatchesRow.removeAllViews()
        val density = context.resources.displayMetrics.density
        val size = (22 * density).toInt()

        swatchColors.forEach { argbLong ->
            val colorInt = argbLong.toInt()
            val swatch = View(context).apply {
                layoutParams = LayoutParams(size, size).apply {
                    val m = (3 * density).toInt()
                    setMargins(m, (2 * density).toInt(), m, (2 * density).toInt())
                }
                background = GradientDrawable().apply {
                    setColor(colorInt)
                    cornerRadius = 11f * density
                    setStroke((1.5f * density).toInt(), Color.parseColor("#475569"))
                }
                setOnClickListener {
                    setColor(argbLong, currentAlpha)
                    notifyColorChanged()
                }
            }
            swatchesRow.addView(swatch)
        }
    }

    fun setColor(colorArgb: Long, alpha: Float) {
        isUpdatingFromExternal = true
        this.currentAlpha = alpha.coerceIn(0.05f, 1.0f)

        val colorInt = colorArgb.toInt()
        val hsv = FloatArray(3)
        Color.colorToHSV(colorInt, hsv)
        this.hue = hsv[0]
        this.saturation = hsv[1]
        this.value = hsv[2]

        satValPicker.setHue(hue)
        satValPicker.setSatVal(saturation, value)
        hueSlider.setHue(hue)
        alphaSlider.setColorAndAlpha(colorInt, currentAlpha)

        updatePreviewUI()
        isUpdatingFromExternal = false
    }

    private fun updatePreviewAndNotify() {
        if (isUpdatingFromExternal) return
        updatePreviewUI()
        notifyColorChanged()
    }

    private fun updatePreviewUI() {
        val hsv = floatArrayOf(hue, saturation, value)
        val solidColor = Color.HSVToColor(hsv)
        val r = Color.red(solidColor)
        val g = Color.green(solidColor)
        val b = Color.blue(solidColor)
        val alphaByte = (currentAlpha * 255).roundToInt().coerceIn(0, 255)
        val fullColor = Color.argb(alphaByte, r, g, b)

        // Update preview chip
        val density = context.resources.displayMetrics.density
        previewChip.background = GradientDrawable().apply {
            cornerRadius = 11f * density
            setColor(fullColor)
            setStroke(2, Color.WHITE)
        }

        // Update hex text
        hexText.text = String.format(Locale.US, "#%02X%02X%02X", r, g, b)
        alphaBadge.text = "${(currentAlpha * 100).roundToInt()}%"

        // Update alpha slider track color
        alphaSlider.setColorAndAlpha(solidColor, currentAlpha)
    }

    private fun notifyColorChanged() {
        val hsv = floatArrayOf(hue, saturation, value)
        val solidColor = Color.HSVToColor(hsv)
        val r = Color.red(solidColor)
        val g = Color.green(solidColor)
        val b = Color.blue(solidColor)
        val argbLong = (0xFF000000L or ((r and 0xFF).toLong() shl 16) or ((g and 0xFF).toLong() shl 8) or (b and 0xFF).toLong())

        onColorChanged(argbLong, currentAlpha)
    }

    // ==========================================
    // Inner View: 2D Saturation-Value Canvas
    // ==========================================
    private class SatValView(
        context: Context,
        private val onSatValChanged: (sat: Float, value: Float) -> Unit
    ) : View(context) {

        private var hue: Float = 0f
        private var sat: Float = 0f
        private var v: Float = 0f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * context.resources.displayMetrics.density
            color = Color.WHITE
        }
        private val targetFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        fun setHue(newHue: Float) {
            this.hue = newHue
            invalidate()
        }

        fun setSatVal(newSat: Float, newVal: Float) {
            this.sat = newSat
            this.v = newVal
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val radius = 8f * context.resources.displayMetrics.density
            val rect = RectF(0f, 0f, w, h)

            val valShader = LinearGradient(0f, 0f, 0f, h, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
            val hueColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            val satShader = LinearGradient(0f, 0f, w, 0f, Color.WHITE, hueColor, Shader.TileMode.CLAMP)
            val composeShader = ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY)

            paint.shader = composeShader
            canvas.drawRoundRect(rect, radius, radius, paint)

            // Draw Selector Crosshair Circle
            val targetX = sat * w
            val targetY = (1f - v) * h
            val targetRadius = 7f * context.resources.displayMetrics.density

            val currentColor = Color.HSVToColor(floatArrayOf(hue, sat, v))
            targetFillPaint.color = currentColor
            canvas.drawCircle(targetX, targetY, targetRadius, targetFillPaint)
            canvas.drawCircle(targetX, targetY, targetRadius, targetPaint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    val w = width.toFloat()
                    val h = height.toFloat()
                    if (w > 0 && h > 0) {
                        sat = (event.x / w).coerceIn(0f, 1f)
                        v = (1f - (event.y / h)).coerceIn(0f, 1f)
                        invalidate()
                        onSatValChanged(sat, v)
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    // ==========================================
    // Inner View: Hue Rainbow Slider Bar
    // ==========================================
    private class HueSliderView(
        context: Context,
        private val onHueChanged: (hue: Float) -> Unit
    ) : View(context) {

        private var hue: Float = 0f
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val thumbStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * context.resources.displayMetrics.density
            color = Color.WHITE
        }

        fun setHue(h: Float) {
            this.hue = h
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val radius = h / 2f
            val rect = RectF(0f, 0f, w, h)

            val hueColors = intArrayOf(
                Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
            )
            trackPaint.shader = LinearGradient(0f, 0f, w, 0f, hueColors, null, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)

            // Draggable Thumb
            val thumbX = (hue / 360f * w).coerceIn(radius, w - radius)
            val thumbColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            thumbPaint.color = thumbColor
            val thumbR = (h / 2f) - 1f
            canvas.drawCircle(thumbX, h / 2f, thumbR, thumbPaint)
            canvas.drawCircle(thumbX, h / 2f, thumbR, thumbStroke)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    val w = width.toFloat()
                    if (w > 0) {
                        hue = ((event.x / w).coerceIn(0f, 1f) * 360f).coerceIn(0f, 360f)
                        invalidate()
                        onHueChanged(hue)
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    // ==========================================
    // Inner View: Alpha / Opacity Slider Bar
    // ==========================================
    private class AlphaSliderView(
        context: Context,
        private val onAlphaChanged: (alpha: Float) -> Unit
    ) : View(context) {

        private var solidColor: Int = Color.BLACK
        private var currentAlpha: Float = 1f

        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val thumbStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * context.resources.displayMetrics.density
            color = Color.WHITE
        }

        fun setColorAndAlpha(solidColorInt: Int, alpha: Float) {
            this.solidColor = solidColorInt
            this.currentAlpha = alpha
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val radius = h / 2f
            val rect = RectF(0f, 0f, w, h)

            val r = Color.red(solidColor)
            val g = Color.green(solidColor)
            val b = Color.blue(solidColor)
            val startColor = Color.argb(25, r, g, b) // 10% min opacity
            val endColor = Color.argb(255, r, g, b)

            trackPaint.shader = LinearGradient(0f, 0f, w, 0f, startColor, endColor, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)

            // Draggable Thumb
            val thumbX = (currentAlpha * w).coerceIn(radius, w - radius)
            val alphaByte = (currentAlpha * 255).roundToInt().coerceIn(0, 255)
            thumbPaint.color = Color.argb(alphaByte, r, g, b)
            val thumbR = (h / 2f) - 1f
            canvas.drawCircle(thumbX, h / 2f, thumbR, thumbPaint)
            canvas.drawCircle(thumbX, h / 2f, thumbR, thumbStroke)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    val w = width.toFloat()
                    if (w > 0) {
                        currentAlpha = (event.x / w).coerceIn(0.1f, 1.0f)
                        invalidate()
                        onAlphaChanged(currentAlpha)
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }
}
