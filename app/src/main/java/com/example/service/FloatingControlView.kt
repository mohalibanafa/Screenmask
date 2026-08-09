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
import com.example.R

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
    private var isLocked = false

    private val containerLayout = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val p = (8 * context.resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)

        background = GradientDrawable().apply {
            setColor(Color.parseColor("#EE1E293B")) // Sleak Slate Dark 90% Opaque
            cornerRadius = 24f * context.resources.displayMetrics.density
            setStroke((1.5f * context.resources.displayMetrics.density).toInt(), Color.parseColor("#334155"))
        }
    }

    private val dragHandle = TextView(context).apply {
        text = " ≡ "
        textSize = 18f
        setTextColor(Color.parseColor("#94A3B8"))
        gravity = Gravity.CENTER
    }

    private val btnAdd = ImageButton(context).apply {
        setImageResource(android.R.drawable.ic_input_add)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(Color.parseColor("#38BDF8"))
        val size = (36 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            setMargins(4, 0, 4, 0)
        }
        contentDescription = "Add Mask"
        setOnClickListener { onAddMask() }
    }

    private val btnMode = ImageButton(context).apply {
        setImageResource(android.R.drawable.ic_menu_edit)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(Color.parseColor("#F59E0B"))
        val size = (36 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            setMargins(4, 0, 4, 0)
        }
        contentDescription = "Toggle Mode"
        setOnClickListener { onToggleEditMode() }
    }

    private val btnLock = ImageButton(context).apply {
        setImageResource(android.R.drawable.ic_lock_lock)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(Color.WHITE)
        val size = (36 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            setMargins(4, 0, 4, 0)
        }
        contentDescription = "Lock All"
        setOnClickListener { onToggleLockAll() }
    }

    private val btnDelete = ImageButton(context).apply {
        setImageResource(android.R.drawable.ic_menu_delete)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(Color.parseColor("#F43F5E"))
        val size = (36 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            setMargins(4, 0, 4, 0)
        }
        contentDescription = "Delete Mask"
        setOnClickListener { onDeleteSelected() }
    }

    private val btnApp = ImageButton(context).apply {
        setImageResource(android.R.drawable.ic_menu_preferences)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(Color.parseColor("#A855F7"))
        val size = (36 * context.resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(size, size).apply {
            setMargins(4, 0, 4, 0)
        }
        contentDescription = "Open App"
        setOnClickListener { onOpenApp() }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        containerLayout.addView(dragHandle)
        containerLayout.addView(btnAdd)
        containerLayout.addView(btnMode)
        containerLayout.addView(btnLock)
        containerLayout.addView(btnDelete)
        containerLayout.addView(btnApp)

        addView(containerLayout)

        setupDragToMove()
    }

    fun updateState(editMode: Boolean, hasSelected: Boolean) {
        this.isEditMode = editMode
        if (editMode) {
            btnMode.setColorFilter(Color.parseColor("#10B981")) // Green for Edit active
        } else {
            btnMode.setColorFilter(Color.parseColor("#F59E0B")) // Amber for Block active
        }
        btnDelete.visibility = if (hasSelected) View.VISIBLE else View.GONE
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

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
    }
}
