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
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("ViewClickAndTouchUnspecified")
class FloatingControlView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onAddMask: (shapeType: String) -> Unit,
    private val onExitEditMode: () -> Unit,
    private val onUpdateMask: (MaskEntity) -> Unit,
    private val onDeleteSelected: () -> Unit,
    private val onDuplicateSelected: () -> Unit,
    private val onExitApp: () -> Unit,
    private val onTogglePreviewMode: (Boolean) -> Unit = {}
) : FrameLayout(context) {

    fun getLayoutParamsTyped(): WindowManager.LayoutParams = layoutParams

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

    // Tier 2: Settings + Delete + Exit Lock (Checkmark) + Preview (Eye) + Exit App (Door)
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

    // Button: Exit Edit Mode / Lock & Done (Checkmark icon) (Tier 2)
    private val btnExit = createIconButton(R.drawable.ic_check_done, context.getString(R.string.btn_lock_done)) {
        onExitEditMode()
    }

    // Button: Preview Mode (Eye Icon) (Tier 2)
    private val btnMinimize = createIconButton(R.drawable.ic_preview_eye, context.getString(R.string.floating_btn_minimize_desc)) {
        toggleMinimize()
    }

    // Button: Exit App (Door with arrow icon) (Tier 2) - Directly kills the app
    private val btnExitApp = createIconButton(R.drawable.ic_exit_door, context.getString(R.string.floating_btn_exit_app_desc)) {
        onExitApp()
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
            text = context.getString(R.string.minimized_preview_label)
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

    // High Precision Sliders (1000 steps + micro steppers)
    private data class SliderRowComponents(
        val container: LinearLayout,
        val seekBar: SeekBar,
        val valueTextView: TextView,
        val setProgressInternal: (Int) -> Unit
    )

    private lateinit var seekWidthComponents: SliderRowComponents
    private lateinit var seekHeightComponents: SliderRowComponents
    private lateinit var seekPosXComponents: SliderRowComponents
    private lateinit var seekPosYComponents: SliderRowComponents

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

    // Physical Pixel Lock Switch & Explanation (Screen Crack Fixed-Location)
    private val physicalLockContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val p = (6 * context.resources.displayMetrics.density).toInt()
        val topM = (6 * context.resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, topM, 0, 0)
        }
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#3310B981")) // Subtle Emerald Green Accent
            cornerRadius = 10f * context.resources.displayMetrics.density
            setStroke(1, Color.parseColor("#10B981"))
        }
    }

    private val switchPhysicalLock = Switch(context).apply {
        text = context.getString(R.string.physical_lock_title)
        setTextColor(Color.WHITE)
        textSize = 12f
        isChecked = true
        setOnCheckedChangeListener { _, isChecked ->
            currentSelectedMask?.let { mask ->
                onUpdateMask(mask.copy(isPhysicalLock = isChecked))
            }
        }
    }

    private val txtPhysicalLockExplanation = TextView(context).apply {
        text = context.getString(R.string.physical_lock_explanation)
        setTextColor(Color.parseColor("#A7F3D0"))
        textSize = 10.5f
        setPadding(0, 2, 0, 0)
    }

    init {
        // Build Tier 1 (Shapes & Duplicate)
        rowTier1.addView(dragHandle)
        rowTier1.addView(btnAddRect)
        rowTier1.addView(btnAddCircle)
        rowTier1.addView(btnAddParallelogram)
        rowTier1.addView(btnDuplicate)

        // Build Tier 2 (Settings, Delete, Lock/Done [Checkmark], Preview [Eye], Exit App [Door])
        rowTier2.addView(btnSettings)
        rowTier2.addView(btnDelete)
        rowTier2.addView(btnExit)
        rowTier2.addView(btnMinimize)
        rowTier2.addView(btnExitApp)

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

        // High Precision Sliders (1000 sub-steps for exact pixel level precision)
        seekWidthComponents = createSliderRow(context.getString(R.string.slider_width), 1000) { ratio ->
            val wRatio = ratio.coerceIn(0.001f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(widthRatio = wRatio)) }
        }
        settingsPanel.addView(seekWidthComponents.container)

        seekHeightComponents = createSliderRow(context.getString(R.string.slider_height), 1000) { ratio ->
            val hRatio = ratio.coerceIn(0.001f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(heightRatio = hRatio)) }
        }
        settingsPanel.addView(seekHeightComponents.container)

        seekPosXComponents = createSliderRow(context.getString(R.string.slider_pos_x), 1000) { ratio ->
            val xRatio = ratio.coerceIn(0.0f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(xRatio = xRatio)) }
        }
        settingsPanel.addView(seekPosXComponents.container)

        seekPosYComponents = createSliderRow(context.getString(R.string.slider_pos_y), 1000) { ratio ->
            val yRatio = ratio.coerceIn(0.0f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(yRatio = yRatio)) }
        }
        settingsPanel.addView(seekPosYComponents.container)

        // Advanced Color Picker Component
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

        // Physical Pixel Lock Section
        physicalLockContainer.addView(switchPhysicalLock)
        physicalLockContainer.addView(txtPhysicalLockExplanation)
        settingsPanel.addView(physicalLockContainer)
    }

    private fun createSliderRow(
        title: String,
        maxVal: Int = 1000,
        onProgress: (Float) -> Unit
    ): SliderRowComponents {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = (2 * context.resources.displayMetrics.density).toInt()
            setPadding(0, p, 0, p)
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val lbl = TextView(context).apply {
            text = title
            textSize = 10.5f
            setTextColor(Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valTxt = TextView(context).apply {
            text = "50.0%"
            textSize = 10.5f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(Color.parseColor("#38BDF8"))
        }
        header.addView(lbl)
        header.addView(valTxt)
        row.addView(header)

        // Control row with [-] SeekBar [+] for micro-precision adjustment
        val sliderControlRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val topM = (1 * context.resources.displayMetrics.density).toInt()
            setPadding(0, topM, 0, 0)
        }

        var isUpdating = false
        lateinit var seekBar: SeekBar

        val btnMinus = TextView(context).apply {
            text = "−"
            textSize = 14f
            setTextColor(Color.parseColor("#E2E8F0"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val s = (22 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 6f * context.resources.displayMetrics.density
            }
            setOnClickListener {
                val nextProg = (seekBar.progress - 1).coerceAtLeast(0)
                seekBar.progress = nextProg
            }
        }

        val btnPlus = TextView(context).apply {
            text = "+"
            textSize = 14f
            setTextColor(Color.parseColor("#E2E8F0"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val s = (22 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 6f * context.resources.displayMetrics.density
            }
            setOnClickListener {
                val nextProg = (seekBar.progress + 1).coerceAtMost(maxVal)
                seekBar.progress = nextProg
            }
        }

        seekBar = SeekBar(context).apply {
            max = maxVal
            progress = 500
            progressTintList = ColorStateList.valueOf(Color.parseColor("#38BDF8"))
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val ratio = progress.toFloat() / maxVal.toFloat()
                    valTxt.text = String.format(Locale.US, "%.1f%%", ratio * 100f)
                    if (fromUser || !isUpdating) {
                        onProgress(ratio)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        sliderControlRow.addView(btnMinus)
        sliderControlRow.addView(seekBar)
        sliderControlRow.addView(btnPlus)
        row.addView(sliderControlRow)

        val setProgressFunc: (Int) -> Unit = { prog ->
            isUpdating = true
            seekBar.progress = prog.coerceIn(0, maxVal)
            val ratio = seekBar.progress.toFloat() / maxVal.toFloat()
            valTxt.text = String.format(Locale.US, "%.1f%%", ratio * 100f)
            isUpdating = false
        }

        return SliderRowComponents(row, seekBar, valTxt, setProgressFunc)
    }

    fun updateState(selectedMask: MaskEntity?) {
        this.currentSelectedMask = selectedMask

        if (selectedMask != null) {
            txtPanelTitle.text = selectedMask.title

            val wProg = (selectedMask.widthRatio * 1000).roundToInt()
            seekWidthComponents.setProgressInternal(wProg)

            val hProg = (selectedMask.heightRatio * 1000).roundToInt()
            seekHeightComponents.setProgressInternal(hProg)

            val xProg = (selectedMask.xRatio * 1000).roundToInt()
            seekPosXComponents.setProgressInternal(xProg)

            val yProg = (selectedMask.yRatio * 1000).roundToInt()
            seekPosYComponents.setProgressInternal(yProg)

            colorPicker.setColor(selectedMask.colorArgb, selectedMask.alpha)

            switchTouchThrough.setOnCheckedChangeListener(null)
            switchTouchThrough.isChecked = selectedMask.isTouchThrough
            updateTouchExplanation(selectedMask.isTouchThrough)
            switchTouchThrough.setOnCheckedChangeListener { _, isChecked ->
                currentSelectedMask?.let { mask ->
                    if (mask.isTouchThrough != isChecked) {
                        updateTouchExplanation(isChecked)
                        onUpdateMask(mask.copy(isTouchThrough = isChecked))
                    }
                }
            }

            switchPhysicalLock.setOnCheckedChangeListener(null)
            switchPhysicalLock.isChecked = selectedMask.isPhysicalLock
            switchPhysicalLock.setOnCheckedChangeListener { _, isChecked ->
                currentSelectedMask?.let { mask ->
                    if (mask.isPhysicalLock != isChecked) {
                        onUpdateMask(mask.copy(isPhysicalLock = isChecked))
                    }
                }
            }
        }
    }

    private fun updateTouchExplanation(isTouchThrough: Boolean) {
        if (isTouchThrough) {
            txtTouchExplanation.text = context.getString(R.string.touch_through_on)
            txtTouchExplanation.setTextColor(Color.parseColor("#BAE6FD"))
        } else {
            txtTouchExplanation.text = context.getString(R.string.touch_through_off)
            txtTouchExplanation.setTextColor(Color.parseColor("#FCA5A5"))
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
        onTogglePreviewMode(isMinimized)
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

