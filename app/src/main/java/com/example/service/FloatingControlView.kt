package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

@SuppressLint("ViewClickAndTouchUnspecified")
class FloatingControlView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onAddMask: () -> Unit,
    private val onToggleEditMode: () -> Unit,
    private val onToggleLockAll: () -> Unit,
    private val onDeleteSelected: () -> Unit,
    private val onOpenApp: () -> Unit
) : LinearLayout(context) {

    private var isMinimized = false
    private var isEditMode = true

    private val containerLayout = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val p = (6 * context.resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)

        background = GradientDrawable().apply {
            setColor(Color.parseColor("#F20F172A")) // Deep Slate Dark 95% Opaque
            cornerRadius = 24f * context.resources.displayMetrics.density
            setStroke((1.5f * context.resources.displayMetrics.density).toInt(), Color.parseColor("#334155"))
        }
    }

    private val dragHandle = TextView(context).apply {
        text = " ≡ "
        textSize = 20f
        setTextColor(Color.parseColor("#94A3B8"))
        gravity = Gravity.CENTER
    }

    // Unified Mode Button: Switches between Edit (Yellow pencil) and Lock (Blue lock)
    private val btnMode = ImageButton(context).apply {
        setImageResource(android.R.drawable.ic_menu_edit)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(Color.parseColor("#10B981"))
        val size = (38 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            setMargins(4, 0, 4, 0)
        }
        contentDescription = "تغيير الوضع"
        setOnClickListener { onToggleEditMode() }
    }

    // Add Mask Button
    private val btnAdd = ImageButton(context).apply {
        setImageResource(android.R.drawable.ic_input_add)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(Color.parseColor("#38BDF8"))
        val size = (38 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            setMargins(4, 0, 4, 0)
        }
        contentDescription = "إضافة مربع"
        setOnClickListener { onAddMask() }
    }

    // Delete Button (only visible when a mask is actively selected)
    private val btnDelete = ImageButton(context).apply {
        setImageResource(android.R.drawable.ic_menu_delete)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(Color.parseColor("#F43F5E"))
        val size = (38 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            setMargins(4, 0, 4, 0)
        }
        contentDescription = "حذف المربع المحدد"
        visibility = View.GONE
        setOnClickListener { onDeleteSelected() }
    }

    // Open Main App Button
    private val btnApp = ImageButton(context).apply {
        setImageResource(android.R.drawable.ic_menu_preferences)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(Color.parseColor("#A855F7"))
        val size = (38 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            setMargins(4, 0, 4, 0)
        }
        contentDescription = "فتح التطبيق"
        setOnClickListener { onOpenApp() }
    }

    // Minimize / Collapse Bubble View
    private val minimizedBubble = TextView(context).apply {
        text = "⬛"
        textSize = 20f
        gravity = Gravity.CENTER
        val size = (42 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#F20F172A"))
            cornerRadius = 21f * context.resources.displayMetrics.density
            setStroke((2f * context.resources.displayMetrics.density).toInt(), Color.parseColor("#38BDF8"))
        }
        visibility = View.GONE
        setOnClickListener { toggleMinimize() }
    }

    // Minimize Button inside expanded bar
    private val btnMinimize = TextView(context).apply {
        text = " ✕ "
        textSize = 14f
        setTextColor(Color.parseColor("#64748B"))
        gravity = Gravity.CENTER
        setOnClickListener { toggleMinimize() }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        containerLayout.addView(dragHandle)
        containerLayout.addView(btnMode)
        containerLayout.addView(btnAdd)
        containerLayout.addView(btnDelete)
        containerLayout.addView(btnApp)
        containerLayout.addView(btnMinimize)

        addView(containerLayout)
        addView(minimizedBubble)

        setupDragToMove()
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        if (isMinimized) {
            containerLayout.visibility = View.GONE
            minimizedBubble.visibility = View.VISIBLE
        } else {
            containerLayout.visibility = View.VISIBLE
            minimizedBubble.visibility = View.GONE
        }
    }

    fun updateState(editMode: Boolean, hasSelected: Boolean) {
        this.isEditMode = editMode
        if (editMode) {
            btnMode.setImageResource(android.R.drawable.ic_menu_edit)
            btnMode.setColorFilter(Color.parseColor("#10B981")) // Green for Edit Mode
        } else {
            btnMode.setImageResource(android.R.drawable.ic_lock_lock)
            btnMode.setColorFilter(Color.parseColor("#F59E0B")) // Amber for Block/Lock Mode
        }
        btnDelete.visibility = if (hasSelected) View.VISIBLE else View.GONE
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private fun setupDragToMove() {
        val touchListener = OnTouchListener { _, event ->
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

        dragHandle.setOnTouchListener(touchListener)
        minimizedBubble.setOnTouchListener(touchListener)
    }
}

