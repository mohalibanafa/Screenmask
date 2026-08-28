package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.MaskEntity

@SuppressLint("ViewClickAndTouchUnspecified")
class FloatingControlView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onAddMask: (shapeType: String) -> Unit,
    private val onExitEditMode: () -> Unit,
    private val onUpdateMask: (MaskEntity) -> Unit,
    private val onDeleteSelected: () -> Unit,
    private val onDuplicateSelected: () -> Unit
) : FrameLayout(context) {

    private var isMinimized = false
    private var isSettingsPanelExpanded = false
    private var currentSelectedMask: MaskEntity? = null

    // Main Floating Bar Container (2-tier compact layout + expandable settings)
    private val mainToolbar = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val p = (5 * context.resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#EE0F172A")) // Modern Deep Slate 93%
            cornerRadius = 18f * context.resources.displayMetrics.density
            setStroke((1.5f * context.resources.displayMetrics.density).toInt(), Color.parseColor("#334155"))
        }
    }

    // Tier 1: Drag handle + Shapes + Duplicate
    private val rowTier1 = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    // Tier 2: Settings + Delete + Exit Lock + Minimize
    private val rowTier2 = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val topMargin = (3 * context.resources.displayMetrics.density).toInt()
        setPadding(0, topMargin, 0, 0)
    }

    // Drag Handle (Tier 1)
    private val dragHandle = TextView(context).apply {
        text = "⠿"
        textSize = 17f
        setTextColor(Color.parseColor("#94A3B8"))
        gravity = Gravity.CENTER
        val pHoriz = (5 * context.resources.displayMetrics.density).toInt()
        val pVert = (2 * context.resources.displayMetrics.density).toInt()
        setPadding(pHoriz, pVert, pHoriz, pVert)
    }

    // Button: Add Square / Rectangle
    private val btnAddRect = createIconButton(R.drawable.ic_shape_square, context.getString(R.string.floating_btn_add_rect)) {
        onAddMask(MaskEntity.SHAPE_RECT)
    }

    // Button: Add Circle
    private val btnAddCircle = createIconButton(R.drawable.ic_shape_circle, context.getString(R.string.floating_btn_add_circle)) {
        onAddMask(MaskEntity.SHAPE_CIRCLE)
    }

    // Button: Add Parallelogram (Free Corners)
    private val btnAddParallelogram = createIconButton(R.drawable.ic_shape_parallelogram, context.getString(R.string.floating_btn_add_parallelogram)) {
        onAddMask(MaskEntity.SHAPE_FREE_QUAD)
    }

    // Button: Duplicate Selected Shape
    private val btnDuplicate = createIconButton(R.drawable.ic_duplicate, context.getString(R.string.btn_duplicate_shape)) {
        onDuplicateSelected()
    }

    // Button: Settings Panel Expansion Toggle (Tier 2)
    private val btnSettings = createIconButton(R.drawable.ic_settings_gear, context.getString(R.string.floating_btn_settings_desc)) {
        toggleSettingsPanel()
    }

    // Button: Delete Selected Shape directly from toolbar (Tier 2)
    private val btnDelete = createIconButton(R.drawable.ic_delete_trash, context.getString(R.string.btn_delete_shape)) {
        onDeleteSelected()
    }

    // Button: Exit Edit Mode / Lock shapes (Tier 2)
    private val btnExit = createIconButton(R.drawable.ic_exit_lock, context.getString(R.string.btn_lock_done)) {
        onExitEditMode()
    }

    // Button: Minimize Toolbar (Tier 2)
    private val btnMinimize = TextView(context).apply {
        text = "─"
        textSize = 17f
        setTextColor(Color.parseColor("#94A3B8"))
        gravity = Gravity.CENTER
        val size = (32 * context.resources.displayMetrics.density).toInt()
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
            setMargins(2, 0, 2, 0)
        }
        setOnClickListener { toggleMinimize() }
    }

    // Minimized Floating Pill Bubble
    private val minimizedBubble = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pHoriz = (12 * context.resources.displayMetrics.density).toInt()
        val pVert = (7 * context.resources.displayMetrics.density).toInt()
        setPadding(pHoriz, pVert, pHoriz, pVert)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#F20F172A"))
            cornerRadius = 24f * context.resources.displayMetrics.density
            setStroke((2f * context.resources.displayMetrics.density).toInt(), Color.parseColor("#38BDF8"))
        }

        // Small eye icon for preview mode
        val eyeIcon = ImageView(context).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_preview_eye))
            val iconSize = (18 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                val m = (4 * context.resources.displayMetrics.density).toInt()
                setMargins(0, 0, m, 0)
            }
        }

        val label = TextView(context).apply {
            text = "${context.getString(R.string.minimized_preview_label)} ⤢"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        addView(eyeIcon)
        addView(label)
        visibility = View.GONE
    }

    // Expandable Settings & Sliders Panel Container
    private val settingsPanel = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val p = (8 * context.resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#E61E293B"))
            cornerRadius = 14f * context.resources.displayMetrics.density
            setStroke(1, Color.parseColor("#475569"))
        }
    }

    private val settingsScrollView = ScrollView(context).apply {
        val maxH = (340 * context.resources.displayMetrics.density).toInt()
        val w = (260 * context.resources.displayMetrics.density).toInt()
        layoutParams = LinearLayout.LayoutParams(w, maxH).apply {
            val topMargin = (6 * context.resources.displayMetrics.density).toInt()
            setMargins(0, topMargin, 0, 0)
        }
        visibility = View.GONE
    }

    // UI elements inside settings panel
    private val txtPanelTitle = TextView(context).apply {
        text = context.getString(R.string.panel_title_edit)
        textSize = 13f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, 4)
    }

// Sliders
    private lateinit var seekWidth: SeekBar
    private lateinit var txtWidthVal: TextView
    private lateinit var seekHeight: SeekBar
    private lateinit var txtHeightVal: TextView
    private lateinit var seekPosX: SeekBar
    private lateinit var txtPosXVal: TextView
    private lateinit var seekPosY: SeekBar
    private lateinit var txtPosYVal: TextView

    // Advanced Color & Alpha Picker
    private lateinit var colorPicker: AdvancedColorPickerView

    // Touch-Through Switch & Explanation
    private val touchThroughContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val p = (6 * context.resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#3338BDF8"))
            cornerRadius = 10f * context.resources.displayMetrics.density
            setStroke(1, Color.parseColor("#38BDF8"))
        }
    }

    private val switchTouchThrough = Switch(context).apply {
        text = context.getString(R.string.touch_through_title)
        setTextColor(Color.WHITE)
        textSize = 12f
        setOnCheckedChangeListener { _, isChecked ->
            currentSelectedMask?.let { mask ->
                onUpdateMask(mask.copy(isTouchThrough = isChecked))
            }
        }
    }

    private val txtTouchExplanation = TextView(context).apply {
        text = context.getString(R.string.touch_through_explanation)
        setTextColor(Color.parseColor("#BAE6FD"))
        textSize = 10.5f
        setPadding(0, 2, 0, 0)
    }

    // Color Palette Container
    private val colorPaletteRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val p = (2 * context.resources.displayMetrics.density).toInt()
        setPadding(0, p, 0, p)
    }

    init {
        // Build Tier 1 (Shapes & Duplicate)
        rowTier1.addView(dragHandle)
        rowTier1.addView(btnAddRect)
        rowTier1.addView(btnAddCircle)
        rowTier1.addView(btnAddParallelogram)
        rowTier1.addView(btnDuplicate)

        // Build Tier 2 (Settings, Delete, Exit Lock, Minimize)
        rowTier2.addView(btnSettings)
        rowTier2.addView(btnDelete)
        rowTier2.addView(btnExit)
        rowTier2.addView(btnMinimize)

        mainToolbar.addView(rowTier1)
        mainToolbar.addView(rowTier2)

        buildSettingsPanelContent()
        settingsScrollView.addView(settingsPanel)
        mainToolbar.addView(settingsScrollView)

        addView(mainToolbar)
        addView(minimizedBubble)

        setupDragToMove()
    }

    private fun createIconButton(drawableRes: Int, desc: String, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            setImageDrawable(ContextCompat.getDrawable(context, drawableRes))
            setBackgroundColor(Color.TRANSPARENT)
            val size = (32 * context.resources.displayMetrics.density).toInt()
            val pad = (4 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(2, 0, 2, 0)
            }
            contentDescription = desc
            setOnClickListener { onClick() }
        }
    }

    private fun buildSettingsPanelContent() {
        settingsPanel.addView(txtPanelTitle)

        // Sliders
        val (wRow, wSeek, wTxt) = createSliderRow(context.getString(R.string.slider_width), 100) { progress ->
            val wRatio = (progress / 100f).coerceIn(0.01f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(widthRatio = wRatio)) }
        }
        seekWidth = wSeek
        txtWidthVal = wTxt
        settingsPanel.addView(wRow)

        val (hRow, hSeek, hTxt) = createSliderRow(context.getString(R.string.slider_height), 100) { progress ->
            val hRatio = (progress / 100f).coerceIn(0.005f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(heightRatio = hRatio)) }
        }
        seekHeight = hSeek
        txtHeightVal = hTxt
        settingsPanel.addView(hRow)

        val (xRow, xSeek, xTxt) = createSliderRow(context.getString(R.string.slider_pos_x), 100) { progress ->
            val xRatio = (progress / 100f).coerceIn(0.0f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(xRatio = xRatio)) }
        }
        seekPosX = xSeek
        txtPosXVal = xTxt
        settingsPanel.addView(xRow)

        val (yRow, ySeek, yTxt) = createSliderRow(context.getString(R.string.slider_pos_y), 100) { progress ->
            val yRatio = (progress / 100f).coerceIn(0.0f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(yRatio = yRatio)) }
        }
        seekPosY = ySeek
        txtPosYVal = yTxt
        settingsPanel.addView(yRow)

        // Advanced Color Picker Component (replaces flat palette & separate opacity slider)
        colorPicker = AdvancedColorPickerView(context) { colorArgb, alpha ->
            currentSelectedMask?.let { mask ->
                onUpdateMask(mask.copy(colorArgb = colorArgb, alpha = alpha))
            }
        }
        settingsPanel.addView(colorPicker)

        // Touch Through Section
        touchThroughContainer.addView(switchTouchThrough)
        touchThroughContainer.addView(txtTouchExplanation)
        settingsPanel.addView(touchThroughContainer)
    }

    private fun createSliderRow(
        title: String,
        maxVal: Int,
        onProgress: (Int) -> Unit
    ): Triple<LinearLayout, SeekBar, TextView> {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 1, 0, 1)
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val lbl = TextView(context).apply {
            text = title
            textSize = 10.5f
            setTextColor(Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valTxt = TextView(context).apply {
            text = "50%"
            textSize = 10.5f
            setTextColor(Color.parseColor("#38BDF8"))
        }
        header.addView(lbl)
        header.addView(valTxt)
        row.addView(header)

        val seekBar = SeekBar(context).apply {
            max = maxVal
            progress = 50
            progressTintList = ColorStateList.valueOf(Color.parseColor("#38BDF8"))
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        valTxt.text = "$progress%"
                        onProgress(progress)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        row.addView(seekBar)

        return Triple(row, seekBar, valTxt)
    }

    fun updateState(selectedMask: MaskEntity?) {
        this.currentSelectedMask = selectedMask

        if (selectedMask != null) {
            txtPanelTitle.text = selectedMask.title
            seekWidth.progress = (selectedMask.widthRatio * 100).toInt()
            txtWidthVal.text = "${(selectedMask.widthRatio * 100).toInt()}%"

            seekHeight.progress = (selectedMask.heightRatio * 100).toInt()
            txtHeightVal.text = "${(selectedMask.heightRatio * 100).toInt()}%"

            seekPosX.progress = (selectedMask.xRatio * 100).toInt()
            txtPosXVal.text = "${(selectedMask.xRatio * 100).toInt()}%"

            seekPosY.progress = (selectedMask.yRatio * 100).toInt()
            txtPosYVal.text = "${(selectedMask.yRatio * 100).toInt()}%"

            colorPicker.setColor(selectedMask.colorArgb, selectedMask.alpha)

            switchTouchThrough.setOnCheckedChangeListener(null)
            switchTouchThrough.isChecked = selectedMask.isTouchThrough
            switchTouchThrough.setOnCheckedChangeListener { _, isChecked ->
                currentSelectedMask?.let { mask ->
                    if (mask.isTouchThrough != isChecked) {
                        onUpdateMask(mask.copy(isTouchThrough = isChecked))
                    }
                }
            }
        }
    }

    private fun toggleSettingsPanel() {
        isSettingsPanelExpanded = !isSettingsPanelExpanded
        settingsScrollView.visibility = if (isSettingsPanelExpanded) View.VISIBLE else View.GONE
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        if (isMinimized) {
            mainToolbar.visibility = View.GONE
            minimizedBubble.visibility = View.VISIBLE
        } else {
            mainToolbar.visibility = View.VISIBLE
            minimizedBubble.visibility = View.GONE
        }
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private fun setupDragToMove() {
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(this, layoutParams)
                    true
                }
                else -> false
            }
        }

        minimizedBubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = Math.abs(event.rawX - initialTouchX)
                    val dy = Math.abs(event.rawY - initialTouchY)
                    if (dx > 10 || dy > 10) {
                        isDragging = true
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(this, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleMinimize()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
