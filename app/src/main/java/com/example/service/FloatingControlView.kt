package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
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
    private val onAddCustomLine: (isVertical: Boolean) -> Unit,
    private val onSelectMask: (String) -> Unit,
    private val onDuplicateMask: (MaskEntity) -> Unit,
    private val onDeleteMask: (String) -> Unit,
    private val onUpdateMask: (MaskEntity) -> Unit,
    private val onEnterPreviewMode: () -> Unit,
    private val onEnterDisplayMode: () -> Unit,
    private val onExitPreviewMode: () -> Unit,
    private val onExitApp: () -> Unit
) : FrameLayout(context) {

    fun getLayoutParamsTyped(): WindowManager.LayoutParams = layoutParams

    private var isMinimized = false
    private var isSettingsPanelExpanded = false
    private var isMasksListExpanded = false

    private var currentSelectedMask: MaskEntity? = null
    private var currentMaskList: List<MaskEntity> = emptyList()

    private val density = context.resources.displayMetrics.density

    // Main Floating Bar Container (2-tier compact layout + expandable panels)
    private val mainToolbar = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val p = (5 * density).toInt()
        setPadding(p, p, p, p)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#EE0F172A")) // Modern Deep Slate 93%
            cornerRadius = 18f * density
            setStroke((1.5f * density).toInt(), Color.parseColor("#334155"))
        }
    }

    // Tier 1: Drag handle + Shapes + Masks List (Hamburger)
    private val rowTier1 = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    // Tier 2: Settings + Duplicate + Delete + Preview (Eye) + Display (Checkmark) + Exit App (Door)
    private val rowTier2 = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val topMargin = (3 * density).toInt()
        setPadding(0, topMargin, 0, 0)
    }

    // Drag Handle (Tier 1)
    private val dragHandle = TextView(context).apply {
        text = "⠿"
        textSize = 17f
        setTextColor(Color.parseColor("#94A3B8"))
        gravity = Gravity.CENTER
        val pHoriz = (5 * density).toInt()
        val pVert = (2 * density).toInt()
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

    // Button: Masks List (Hamburger Menu) - Toggles dropdown of all masks
    private val btnMaskList = createIconButton(R.drawable.ic_menu_burger, context.getString(R.string.floating_btn_list_desc)) {
        toggleMasksListPanel()
    }

    // Button: Settings Panel Expansion Toggle (Tier 2)
    private val btnSettings = createIconButton(R.drawable.ic_settings_gear, context.getString(R.string.floating_btn_settings_desc)) {
        toggleSettingsPanel()
    }

    // Button: Duplicate Selected Shape
    private val btnDuplicate = createIconButton(R.drawable.ic_duplicate, context.getString(R.string.btn_duplicate_shape)) {
        currentSelectedMask?.let { onDuplicateMask(it) }
    }

    // Button: Delete Selected Shape directly from toolbar (Tier 2)
    private val btnDelete = createIconButton(R.drawable.ic_delete_trash, context.getString(R.string.btn_delete_shape)) {
        currentSelectedMask?.id?.let { onDeleteMask(it) }
    }

    // Button: Preview Mode (Eye Icon) (Tier 2) - Minimizes toolbar and applies settings with touch pass-through
    private val btnPreview = createIconButton(R.drawable.ic_preview_eye, context.getString(R.string.floating_btn_preview_desc)) {
        onEnterPreviewMode()
    }

    // Button: Display Mode (Checkmark icon) (Tier 2) - Applies settings and hides toolbar completely
    private val btnDisplay = createIconButton(R.drawable.ic_check_done, context.getString(R.string.floating_btn_display_desc)) {
        onEnterDisplayMode()
    }

    // Button: Exit App (Door with arrow icon) (Tier 2) - Cleanly stops overlays and terminates app
    private val btnExitApp = createIconButton(R.drawable.ic_exit_door, context.getString(R.string.floating_btn_exit_app_desc)) {
        onExitApp()
    }

    // Minimized Floating Pill Bubble (Preview Mode)
    private val minimizedBubble = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pHoriz = (12 * density).toInt()
        val pVert = (7 * density).toInt()
        setPadding(pHoriz, pVert, pHoriz, pVert)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#F20F172A"))
            cornerRadius = 24f * density
            setStroke((2f * density).toInt(), Color.parseColor("#38BDF8"))
        }

        val eyeIcon = ImageView(context).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_preview_eye))
            val iconSize = (18 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                val m = (5 * density).toInt()
                setMargins(0, 0, m, 0)
            }
        }

        val label = TextView(context).apply {
            text = context.getString(R.string.minimized_preview_label)
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        addView(eyeIcon)
        addView(label)
        visibility = View.GONE
    }

    // ----------------------------------------------------
    // MASKS LIST DROPDOWN PANEL
    // ----------------------------------------------------
    private val masksListContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val p = (8 * density).toInt()
        setPadding(p, p, p, p)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#E60F172A"))
            cornerRadius = 14f * density
            setStroke(1, Color.parseColor("#334155"))
        }
    }

    private val masksListScrollView = ScrollView(context).apply {
        val maxH = (300 * density).toInt()
        val w = (270 * density).toInt()
        layoutParams = LinearLayout.LayoutParams(w, maxH).apply {
            val topMargin = (6 * density).toInt()
            setMargins(0, topMargin, 0, 0)
        }
        visibility = View.GONE
    }

    private val masksItemsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    // ----------------------------------------------------
    // EXPANDABLE SETTINGS & SLIDERS PANEL CONTAINER
    // ----------------------------------------------------
    private val settingsPanel = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val p = (8 * density).toInt()
        setPadding(p, p, p, p)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#E61E293B"))
            cornerRadius = 14f * density
            setStroke(1, Color.parseColor("#475569"))
        }
    }

    private val settingsScrollView = ScrollView(context).apply {
        val maxH = (360 * density).toInt()
        val w = (270 * density).toInt()
        layoutParams = LinearLayout.LayoutParams(w, maxH).apply {
            val topMargin = (6 * density).toInt()
            setMargins(0, topMargin, 0, 0)
        }
        visibility = View.GONE
    }

    private val txtPanelTitle = TextView(context).apply {
        text = context.getString(R.string.panel_title_edit)
        textSize = 13f
        setTextColor(Color.WHITE)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, (4 * density).toInt())
    }

    // High Precision Sliders
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
        val p = (6 * density).toInt()
        val topM = (6 * density).toInt()
        setPadding(p, p, p, p)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, topM, 0, 0)
        }
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#330284C7"))
            cornerRadius = 10f * density
            setStroke(1, Color.parseColor("#0284C7"))
        }
    }

    private val switchTouchThrough = Switch(context).apply {
        text = context.getString(R.string.touch_through_title)
        setTextColor(Color.WHITE)
        textSize = 12f
        isChecked = true
        setOnCheckedChangeListener { _, isChecked ->
            currentSelectedMask?.let { mask ->
                onUpdateMask(mask.copy(isTouchThrough = isChecked))
            }
        }
    }

    private val txtTouchThroughExplanation = TextView(context).apply {
        text = context.getString(R.string.touch_through_explanation)
        setTextColor(Color.parseColor("#BAE6FD"))
        textSize = 10.5f
        setPadding(0, 2, 0, 0)
    }

    // Physical Pixel Lock Switch
    private val physicalLockContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val p = (6 * density).toInt()
        val topM = (6 * density).toInt()
        setPadding(p, p, p, p)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, topM, 0, 0)
        }
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#3310B981"))
            cornerRadius = 10f * density
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
        // Build Tier 1 (Shapes, List Hamburger)
        rowTier1.addView(dragHandle)
        rowTier1.addView(btnAddRect)
        rowTier1.addView(btnAddCircle)
        rowTier1.addView(btnAddParallelogram)
        rowTier1.addView(btnMaskList)

        // Build Tier 2 (Settings, Duplicate, Delete, Preview [Eye], Display [Checkmark], Exit App [Door])
        rowTier2.addView(btnSettings)
        rowTier2.addView(btnDuplicate)
        rowTier2.addView(btnDelete)
        rowTier2.addView(btnPreview)
        rowTier2.addView(btnDisplay)
        rowTier2.addView(btnExitApp)

        mainToolbar.addView(rowTier1)
        mainToolbar.addView(rowTier2)

        // Build Masks List Dropdown
        buildMasksListContent()
        masksListScrollView.addView(masksListContainer)
        mainToolbar.addView(masksListScrollView)

        // Build Settings & Precision Controls Panel
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
            val size = (32 * density).toInt()
            val pad = (4 * density).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(2, 0, 2, 0)
            }
            contentDescription = desc
            setOnClickListener { onClick() }
        }
    }

    // ----------------------------------------------------
    // BUILD MASKS LIST DROPDOWN
    // ----------------------------------------------------
    private fun buildMasksListContent() {
        // Header
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bPad = (4 * density).toInt()
            setPadding(0, 0, 0, bPad)
        }
        val title = TextView(context).apply {
            text = context.getString(R.string.masks_list_title)
            textSize = 12.5f
            setTextColor(Color.parseColor("#38BDF8"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(title)
        masksListContainer.addView(headerRow)

        // Quick line add shortcuts (+ H-Line 1px / + V-Line 1px)
        val quickLinesRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bPad = (6 * density).toInt()
            setPadding(0, 0, 0, bPad)
        }
        val btnHLine = createSmallButton(context.getString(R.string.btn_add_h_line)) {
            onAddCustomLine(false)
        }
        val btnVLine = createSmallButton(context.getString(R.string.btn_add_v_line)) {
            onAddCustomLine(true)
        }
        btnHLine.layoutParams = LinearLayout.LayoutParams(0, (28 * density).toInt(), 1f).apply {
            val r = (3 * density).toInt()
            setMargins(0, 0, r, 0)
        }
        btnVLine.layoutParams = LinearLayout.LayoutParams(0, (28 * density).toInt(), 1f).apply {
            val l = (3 * density).toInt()
            setMargins(l, 0, 0, 0)
        }
        quickLinesRow.addView(btnHLine)
        quickLinesRow.addView(btnVLine)
        masksListContainer.addView(quickLinesRow)

        // Items container
        masksListContainer.addView(masksItemsContainer)
    }

    private fun rebuildMasksItemsList() {
        masksItemsContainer.removeAllViews()

        if (currentMaskList.isEmpty()) {
            val emptyTxt = TextView(context).apply {
                text = context.getString(R.string.masks_list_empty)
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
                gravity = Gravity.CENTER
                val p = (12 * density).toInt()
                setPadding(p, p, p, p)
            }
            masksItemsContainer.addView(emptyTxt)
            return
        }

        currentMaskList.forEachIndexed { index, mask ->
            val isSelected = (mask.id == currentSelectedMask?.id)
            val itemRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val pV = (5 * density).toInt()
                val pH = (6 * density).toInt()
                setPadding(pH, pV, pH, pV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    val m = (2 * density).toInt()
                    setMargins(0, m, 0, m)
                }
                background = GradientDrawable().apply {
                    val bgCol = if (isSelected) Color.parseColor("#3338BDF8") else Color.parseColor("#22334155")
                    setColor(bgCol)
                    cornerRadius = 8f * density
                    val strokeCol = if (isSelected) Color.parseColor("#38BDF8") else Color.TRANSPARENT
                    setStroke(if (isSelected) (1.5f * density).toInt() else 0, strokeCol)
                }
                setOnClickListener {
                    onSelectMask(mask.id)
                }
            }

            // Color Swatch
            val swatch = View(context).apply {
                val s = (12 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(s, s).apply {
                    val r = (6 * density).toInt()
                    setMargins(0, 0, r, 0)
                }
                background = GradientDrawable().apply {
                    setColor(mask.colorArgb.toInt())
                    cornerRadius = 3f * density
                    setStroke(1, Color.WHITE)
                }
            }
            itemRow.addView(swatch)

            // Info Column (Title + Size/Type)
            val infoCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val titleTxt = TextView(context).apply {
                text = mask.title.ifEmpty { context.getString(R.string.mask_title_format, index + 1) }
                textSize = 11.5f
                setTextColor(if (isSelected) Color.parseColor("#38BDF8") else Color.WHITE)
                typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            }
            val subTxt = TextView(context).apply {
                val shapeDesc = when {
                    mask.heightRatio <= 0.005f -> context.getString(R.string.mask_item_h_line)
                    mask.widthRatio <= 0.005f -> context.getString(R.string.mask_item_v_line)
                    mask.shapeType == MaskEntity.SHAPE_CIRCLE -> context.getString(R.string.shape_circle)
                    mask.shapeType == MaskEntity.SHAPE_FREE_QUAD -> context.getString(R.string.shape_parallelogram)
                    else -> context.getString(R.string.shape_rect)
                }
                val statusDesc = if (mask.isLocked) " • ${context.getString(R.string.mask_item_locked)}" else ""
                text = "$shapeDesc$statusDesc"
                textSize = 9.5f
                setTextColor(Color.parseColor("#94A3B8"))
            }
            infoCol.addView(titleTxt)
            infoCol.addView(subTxt)
            itemRow.addView(infoCol)

            // Visibility Toggle Button (Eye)
            val btnVis = createSmallIconButton(
                if (mask.isVisible) R.drawable.ic_preview_eye else R.drawable.ic_visibility_off
            ) {
                onUpdateMask(mask.copy(isVisible = !mask.isVisible))
            }
            itemRow.addView(btnVis)

            // Lock Toggle Button (Lock)
            val btnLock = createSmallIconButton(
                if (mask.isLocked) R.drawable.ic_lock_closed else R.drawable.ic_lock_open
            ) {
                onUpdateMask(mask.copy(isLocked = !mask.isLocked))
            }
            itemRow.addView(btnLock)

            // Duplicate Button
            val btnDup = createSmallIconButton(R.drawable.ic_duplicate) {
                onDuplicateMask(mask)
            }
            itemRow.addView(btnDup)

            // Delete Button
            val btnDel = createSmallIconButton(R.drawable.ic_delete_trash) {
                onDeleteMask(mask.id)
            }
            itemRow.addView(btnDel)

            masksItemsContainer.addView(itemRow)
        }
    }

    private fun createSmallIconButton(drawableRes: Int, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            setImageDrawable(ContextCompat.getDrawable(context, drawableRes))
            setBackgroundColor(Color.TRANSPARENT)
            val s = (26 * density).toInt()
            val pad = (3 * density).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                val m = (1 * density).toInt()
                setMargins(m, 0, m, 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createSmallButton(text: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 10.5f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2238BDF8"))
                cornerRadius = 6f * density
                setStroke((1 * density).toInt(), Color.parseColor("#38BDF8"))
            }
            setOnClickListener { onClick() }
        }
    }

    // ----------------------------------------------------
    // BUILD SETTINGS & PRECISION CONTROLS PANEL
    // ----------------------------------------------------
    private fun buildSettingsPanelContent() {
        settingsPanel.addView(txtPanelTitle)

        // 1. Pixel-Precision D-Pad Directional Controller
        buildDPadSection()

        // 2. High Precision Sliders & Steppers
        val stepperHeader = TextView(context).apply {
            text = context.getString(R.string.precision_controls_title)
            textSize = 11.5f
            setTextColor(Color.parseColor("#38BDF8"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val topP = (8 * density).toInt()
            setPadding(0, topP, 0, (2 * density).toInt())
        }
        settingsPanel.addView(stepperHeader)

        seekWidthComponents = createSliderRow(
            title = context.getString(R.string.slider_width),
            maxVal = 1000,
            onStepDelta = { deltaPx ->
                currentSelectedMask?.let { mask ->
                    val newW = (mask.widthRatio + deltaPx * 0.001f).coerceIn(0.0005f, 1.0f)
                    onUpdateMask(mask.copy(widthRatio = newW))
                }
            },
            onQuickPreset = { preset ->
                currentSelectedMask?.let { mask ->
                    val newW = when (preset) {
                        "full" -> 1.0f
                        "1px" -> 0.001f
                        "2px" -> 0.002f
                        "5px" -> 0.005f
                        else -> mask.widthRatio
                    }
                    onUpdateMask(mask.copy(widthRatio = newW))
                }
            }
        ) { ratio ->
            val wRatio = ratio.coerceIn(0.0005f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(widthRatio = wRatio)) }
        }
        settingsPanel.addView(seekWidthComponents.container)

        seekHeightComponents = createSliderRow(
            title = context.getString(R.string.slider_height),
            maxVal = 1000,
            onStepDelta = { deltaPx ->
                currentSelectedMask?.let { mask ->
                    val newH = (mask.heightRatio + deltaPx * 0.001f).coerceIn(0.0005f, 1.0f)
                    onUpdateMask(mask.copy(heightRatio = newH))
                }
            },
            onQuickPreset = { preset ->
                currentSelectedMask?.let { mask ->
                    val newH = when (preset) {
                        "full" -> 1.0f
                        "1px" -> 0.001f
                        "2px" -> 0.002f
                        "5px" -> 0.005f
                        else -> mask.heightRatio
                    }
                    onUpdateMask(mask.copy(heightRatio = newH))
                }
            }
        ) { ratio ->
            val hRatio = ratio.coerceIn(0.0005f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(heightRatio = hRatio)) }
        }
        settingsPanel.addView(seekHeightComponents.container)

        seekPosXComponents = createSliderRow(
            title = context.getString(R.string.slider_pos_x),
            maxVal = 1000,
            onStepDelta = { deltaPx ->
                currentSelectedMask?.let { mask ->
                    val newX = (mask.xRatio + deltaPx * 0.001f).coerceIn(-0.2f, 1.2f)
                    onUpdateMask(mask.copy(xRatio = newX))
                }
            },
            onQuickPreset = null
        ) { ratio ->
            val xRatio = ratio.coerceIn(0.0f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(xRatio = xRatio)) }
        }
        settingsPanel.addView(seekPosXComponents.container)

        seekPosYComponents = createSliderRow(
            title = context.getString(R.string.slider_pos_y),
            maxVal = 1000,
            onStepDelta = { deltaPx ->
                currentSelectedMask?.let { mask ->
                    val newY = (mask.yRatio + deltaPx * 0.001f).coerceIn(-0.2f, 1.2f)
                    onUpdateMask(mask.copy(yRatio = newY))
                }
            },
            onQuickPreset = null
        ) { ratio ->
            val yRatio = ratio.coerceIn(0.0f, 1.0f)
            currentSelectedMask?.let { onUpdateMask(it.copy(yRatio = yRatio)) }
        }
        settingsPanel.addView(seekPosYComponents.container)

        // 3. Advanced Color Picker Component
        colorPicker = AdvancedColorPickerView(context) { colorArgb, alpha ->
            currentSelectedMask?.let { mask ->
                onUpdateMask(mask.copy(colorArgb = colorArgb, alpha = alpha))
            }
        }
        settingsPanel.addView(colorPicker)

        // 4. Touch Through Section
        touchThroughContainer.addView(switchTouchThrough)
        touchThroughContainer.addView(txtTouchThroughExplanation)
        settingsPanel.addView(touchThroughContainer)

        // 5. Physical Pixel Lock Section
        physicalLockContainer.addView(switchPhysicalLock)
        physicalLockContainer.addView(txtPhysicalLockExplanation)
        settingsPanel.addView(physicalLockContainer)
    }

    // ----------------------------------------------------
    // PIXEL-PRECISION D-PAD CONTROLLER
    // ----------------------------------------------------
    private fun buildDPadSection() {
        val dpadCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val p = (6 * density).toInt()
            val topM = (4 * density).toInt()
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, topM, 0, 0)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#220F172A"))
                cornerRadius = 10f * density
                setStroke(1, Color.parseColor("#334155"))
            }
        }

        val dpadLabel = TextView(context).apply {
            text = context.getString(R.string.dpad_title)
            textSize = 10.5f
            setTextColor(Color.parseColor("#94A3B8"))
            val b = (4 * density).toInt()
            setPadding(0, 0, 0, b)
        }
        dpadCard.addView(dpadLabel)

        // D-Pad Grid (3 rows: [  ⬆  ] / [ ⬅  •  ➡ ] / [  ⬇  ])
        val rowUp = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val btnUp = createDPadButton("▲") {
            // Move up 1 step (Long press repeating)
            currentSelectedMask?.let { mask ->
                val step = 0.001f // ~1-2 pixels
                onUpdateMask(mask.copy(yRatio = (mask.yRatio - step).coerceIn(-0.2f, 1.2f)))
            }
        }
        rowUp.addView(btnUp)
        dpadCard.addView(rowUp)

        val rowMid = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val vM = (2 * density).toInt()
            setPadding(0, vM, 0, vM)
        }
        val btnLeft = createDPadButton("◀") {
            // Move left 1 step (Long press repeating)
            currentSelectedMask?.let { mask ->
                val step = 0.001f
                onUpdateMask(mask.copy(xRatio = (mask.xRatio - step).coerceIn(-0.2f, 1.2f)))
            }
        }
        val centerDot = TextView(context).apply {
            text = "•"
            textSize = 14f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            val s = (28 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s)
        }
        val btnRight = createDPadButton("▶") {
            // Move right 1 step (Long press repeating)
            currentSelectedMask?.let { mask ->
                val step = 0.001f
                onUpdateMask(mask.copy(xRatio = (mask.xRatio + step).coerceIn(-0.2f, 1.2f)))
            }
        }
        rowMid.addView(btnLeft)
        rowMid.addView(centerDot)
        rowMid.addView(btnRight)
        dpadCard.addView(rowMid)

        val rowDown = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val btnDown = createDPadButton("▼") {
            // Move down 1 step (Long press repeating)
            currentSelectedMask?.let { mask ->
                val step = 0.001f
                onUpdateMask(mask.copy(yRatio = (mask.yRatio + step).coerceIn(-0.2f, 1.2f)))
            }
        }
        rowDown.addView(btnDown)
        dpadCard.addView(rowDown)

        settingsPanel.addView(dpadCard)
    }

    private fun createDPadButton(arrow: String, onStep: () -> Unit): TextView {
        return TextView(context).apply {
            text = arrow
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val s = (28 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 6f * density
                setStroke(1, Color.parseColor("#475569"))
            }
            setupRepeatingTouchAction(this, onStep)
        }
    }

    // ----------------------------------------------------
    // REPEATING LONG PRESS TOUCH HANDLER
    // ----------------------------------------------------
    private fun setupRepeatingTouchAction(view: View, action: () -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                action()
                handler.postDelayed(this, 40) // Rapid repeat every 40ms
            }
        }
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    action() // First instant step
                    handler.postDelayed(runnable, 300) // Start rapid repeat after 300ms hold
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    handler.removeCallbacks(runnable)
                    true
                }
                else -> false
            }
        }
    }

    private fun createSliderRow(
        title: String,
        maxVal: Int = 1000,
        onStepDelta: ((Float) -> Unit)? = null,
        onQuickPreset: ((String) -> Unit)? = null,
        onProgress: (Float) -> Unit
    ): SliderRowComponents {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = (2 * density).toInt()
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

        // Preset buttons row (if available)
        if (onQuickPreset != null) {
            val presetRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val topP = (1 * density).toInt()
                setPadding(0, topP, 0, 0)
            }
            val presets = listOf("1px", "2px", "5px", "full")
            val presetLabels = listOf(
                context.getString(R.string.btn_1px),
                context.getString(R.string.btn_2px),
                context.getString(R.string.btn_5px),
                if (title == context.getString(R.string.slider_width)) context.getString(R.string.btn_full_width) else context.getString(R.string.btn_full_height)
            )
            presets.forEachIndexed { i, pKey ->
                val pBtn = TextView(context).apply {
                    text = presetLabels[i]
                    textSize = 9f
                    setTextColor(Color.parseColor("#BAE6FD"))
                    gravity = Gravity.CENTER
                    val h = (20 * density).toInt()
                    layoutParams = LinearLayout.LayoutParams(0, h, 1f).apply {
                        val m = (1 * density).toInt()
                        setMargins(m, 0, m, 0)
                    }
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#2238BDF8"))
                        cornerRadius = 4f * density
                    }
                    setOnClickListener {
                        onQuickPreset(pKey)
                    }
                }
                presetRow.addView(pBtn)
            }
            row.addView(presetRow)
        }

        // Control row with [-] SeekBar [+] supporting LONG PRESS for rapid adjustment
        val sliderControlRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val topM = (1 * density).toInt()
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
            val s = (22 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 6f * density
            }
            setupRepeatingTouchAction(this) {
                if (onStepDelta != null) {
                    onStepDelta(-1f)
                } else {
                    val nextProg = (seekBar.progress - 1).coerceAtLeast(0)
                    seekBar.progress = nextProg
                }
            }
        }

        val btnPlus = TextView(context).apply {
            text = "+"
            textSize = 14f
            setTextColor(Color.parseColor("#E2E8F0"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val s = (22 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 6f * density
            }
            setupRepeatingTouchAction(this) {
                if (onStepDelta != null) {
                    onStepDelta(1f)
                } else {
                    val nextProg = (seekBar.progress + 1).coerceAtMost(maxVal)
                    seekBar.progress = nextProg
                }
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

    fun updateState(selectedMask: MaskEntity?, allMasks: List<MaskEntity> = emptyList()) {
        this.currentSelectedMask = selectedMask
        this.currentMaskList = allMasks

        rebuildMasksItemsList()

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
            switchTouchThrough.setOnCheckedChangeListener { _, isChecked ->
                currentSelectedMask?.let { mask ->
                    if (mask.isTouchThrough != isChecked) {
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

    fun showPreviewMode() {
        isMinimized = true
        mainToolbar.visibility = View.GONE
        minimizedBubble.visibility = View.VISIBLE
    }

    fun showEditMode() {
        isMinimized = false
        mainToolbar.visibility = View.VISIBLE
        minimizedBubble.visibility = View.GONE
    }

    private fun toggleMasksListPanel() {
        isMasksListExpanded = !isMasksListExpanded
        masksListScrollView.visibility = if (isMasksListExpanded) View.VISIBLE else View.GONE
        if (isMasksListExpanded) {
            // Close settings if masks list opens
            isSettingsPanelExpanded = false
            settingsScrollView.visibility = View.GONE
        }
    }

    private fun toggleSettingsPanel() {
        isSettingsPanelExpanded = !isSettingsPanelExpanded
        settingsScrollView.visibility = if (isSettingsPanelExpanded) View.VISIBLE else View.GONE
        if (isSettingsPanelExpanded) {
            // Close masks list if settings opens
            isMasksListExpanded = false
            masksListScrollView.visibility = View.GONE
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
                        onExitPreviewMode()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
