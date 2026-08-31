package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.AppSettingsRepository
import com.example.data.MaskEntity
import com.example.data.MaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var maskRepository: MaskRepository
    private lateinit var settingsRepository: AppSettingsRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val activeMaskViews = mutableMapOf<String, Pair<OverlayMaskView, WindowManager.LayoutParams>>()
    private var floatingControlView: FloatingControlView? = null

    private var currentMaskList = listOf<MaskEntity>()
    private var isEditMode = true
    private var isPreviewMode = false
    private var selectedMaskId: String? = null

    // Screen dimensions and rotation tracking to preserve exact physical position
    private var oldScreenWidth: Int = 0
    private var oldScreenHeight: Int = 0
    private var oldRotation: Int = Surface.ROTATION_0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val db = AppDatabase.getInstance(this)
        maskRepository = MaskRepository.getInstance(db)
        settingsRepository = AppSettingsRepository.getInstance(this)

        val (initialW, initialH) = getScreenDimensions()
        oldScreenWidth = initialW
        oldScreenHeight = initialH
        oldRotation = getScreenRotation()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        observeState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                killAndExitApp()
                return START_NOT_STICKY
            }
            ACTION_START_EDIT_MODE -> {
                settingsRepository.setServiceActive(true)
                settingsRepository.setEditMode(true)
            }
            ACTION_TOGGLE_MODE -> {
                val nextMode = !settingsRepository.isEditMode.value
                settingsRepository.setEditMode(nextMode)
            }
            ACTION_ADD_MASK -> {
                val shape = intent.getStringExtra(EXTRA_SHAPE_TYPE) ?: MaskEntity.SHAPE_RECT
                addNewMask(shape)
            }
            else -> {
                // Default launch opens directly in Edit Mode
                settingsRepository.setServiceActive(true)
                settingsRepository.setEditMode(true)
            }
        }
        return START_STICKY
    }

    private fun observeState() {
        serviceScope.launch {
            settingsRepository.isEditMode.collectLatest { editMode ->
                isEditMode = editMode
                updateNotification()
                refreshAllMaskViews()
            }
        }

        serviceScope.launch {
            settingsRepository.selectedMaskId.collectLatest { selectedId ->
                selectedMaskId = selectedId
                refreshAllMaskViews()
            }
        }

        serviceScope.launch {
            maskRepository.allMasks.collectLatest { masks ->
                currentMaskList = masks
                // Only select a mask if masks exist, or set to null when empty
                if (masks.isEmpty()) {
                    settingsRepository.setSelectedMaskId(null)
                } else if (selectedMaskId == null || masks.none { it.id == selectedMaskId }) {
                    settingsRepository.setSelectedMaskId(masks.firstOrNull()?.id)
                }
                refreshAllMaskViews()
            }
        }
    }

    private fun refreshAllMaskViews() {
        val (screenWidth, screenHeight) = getScreenDimensions()
        val screenRotation = getScreenRotation()

        // 1. Remove views that no longer exist in DB
        val existingIds = currentMaskList.map { it.id }.toSet()
        val toRemove = activeMaskViews.keys.filter { it !in existingIds }
        toRemove.forEach { id ->
            activeMaskViews[id]?.first?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            activeMaskViews.remove(id)
        }

        var newMaskAdded = false

        // 2. Add or update active visible masks
        currentMaskList.forEach { mask ->
            if (!mask.isVisible) {
                activeMaskViews[mask.id]?.first?.let { view ->
                    try {
                        windowManager.removeView(view)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                activeMaskViews.remove(mask.id)
                return@forEach
            }

            val inActualEditMode = isEditMode && !isPreviewMode && !mask.isLocked
            val isSelected = mask.id == selectedMaskId && inActualEditMode
            val params = createMaskLayoutParams(mask, isEditMode, screenWidth, screenHeight, screenRotation)

            if (activeMaskViews.containsKey(mask.id)) {
                val (view, _) = activeMaskViews[mask.id]!!
                view.updateData(mask, inActualEditMode, isSelected, screenWidth, screenHeight, screenRotation)
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                activeMaskViews[mask.id] = Pair(view, params)
            } else {
                lateinit var maskViewRef: OverlayMaskView
                val maskView = OverlayMaskView(
                    context = this,
                    mask = mask,
                    isEditMode = inActualEditMode,
                    isMaskSelected = isSelected,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    screenRotation = screenRotation,
                    onUpdate = { updatedMask ->
                        serviceScope.launch(Dispatchers.IO) {
                            maskRepository.updateMask(updatedMask)
                        }
                    },
                    onDelete = { maskId ->
                        serviceScope.launch(Dispatchers.IO) {
                            maskRepository.deleteMask(maskId)
                            if (selectedMaskId == maskId) {
                                settingsRepository.setSelectedMaskId(null)
                            }
                        }
                    },
                    onSelect = { maskId ->
                        settingsRepository.setSelectedMaskId(maskId)
                    },
                    onLayoutUpdate = { newParams ->
                        try {
                            windowManager.updateViewLayout(maskViewRef, newParams)
                            activeMaskViews[mask.id] = Pair(maskViewRef, newParams)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
                maskViewRef = maskView

                try {
                    windowManager.addView(maskView, params)
                    activeMaskViews[mask.id] = Pair(maskView, params)
                    newMaskAdded = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. Update or attach Floating Control Bar in Edit Mode (ensure it stays on top of all mask shapes)
        if (isEditMode) {
            showFloatingController(forceReattach = newMaskAdded)
        } else {
            hideFloatingController()
        }
    }

    private fun createMaskLayoutParams(
        mask: MaskEntity,
        editMode: Boolean,
        screenWidth: Int,
        screenHeight: Int,
        rotation: Int
    ): WindowManager.LayoutParams {
        val inActualEditMode = editMode && !isPreviewMode && !mask.isLocked
        val flags = if (inActualEditMode) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else if (mask.isTouchThrough) {
            // Touch passes through completely in Lock/Preview/Display Mode when touch-through is enabled
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            // Touch is blocked: touches on the mask will be intercepted and not passed to underlying apps
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }

        val extraPadding = if (inActualEditMode) 32f * resources.displayMetrics.density else 0f
        val layout = PhysicalPixelTransformer.calculateMaskLayout(mask, screenWidth, screenHeight, rotation, extraPadding)

        return WindowManager.LayoutParams(
            layout.winWidth,
            layout.winHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = layout.winX
            y = layout.winY
        }
    }

    private fun showFloatingController(forceReattach: Boolean = false) {
        val selectedMask = currentMaskList.find { it.id == selectedMaskId } ?: currentMaskList.firstOrNull()

        if (floatingControlView != null && forceReattach) {
            try {
                val params = floatingControlView?.getLayoutParamsTyped()
                if (params != null) {
                    windowManager.removeView(floatingControlView)
                    windowManager.addView(floatingControlView, params)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (floatingControlView == null) {
            val (screenWidth, _) = getScreenDimensions()
            val initialWidth = (280 * resources.displayMetrics.density).toInt()
            val initialX = ((screenWidth - initialWidth) / 2).coerceAtLeast(0)
            val initialY = (80 * resources.displayMetrics.density).toInt()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = initialX
                y = initialY
            }

            val controlView = FloatingControlView(
                context = this,
                windowManager = windowManager,
                layoutParams = params,
                onAddMask = { shapeType -> addNewMask(shapeType) },
                onExitEditMode = {
                    settingsRepository.setEditMode(false)
                },
                onUpdateMask = { updated ->
                    serviceScope.launch(Dispatchers.IO) {
                        maskRepository.updateMask(updated)
                    }
                },
                onDeleteSelected = {
                    val idToDelete = selectedMaskId ?: currentMaskList.lastOrNull()?.id
                    idToDelete?.let { id ->
                        serviceScope.launch(Dispatchers.IO) {
                            maskRepository.deleteMask(id)
                            if (selectedMaskId == id) {
                                settingsRepository.setSelectedMaskId(null)
                            }
                        }
                    }
                },
                onDuplicateSelected = {
                    selectedMask?.let { mask ->
                        serviceScope.launch(Dispatchers.IO) {
                            val duplicated = mask.copy(
                                id = UUID.randomUUID().toString(),
                                title = getString(R.string.mask_title_format, currentMaskList.size + 1),
                                xRatio = (mask.xRatio + 0.05f).coerceIn(0f, 0.9f),
                                yRatio = (mask.yRatio + 0.05f).coerceIn(0f, 0.9f),
                                updatedAt = System.currentTimeMillis()
                            )
                            maskRepository.addMask(duplicated)
                            settingsRepository.setSelectedMaskId(duplicated.id)
                        }
                    }
                },
                onExitApp = {
                    killAndExitApp()
                },
                onTogglePreviewMode = { previewing ->
                    isPreviewMode = previewing
                    refreshAllMaskViews()
                }
            )

            try {
                windowManager.addView(controlView, params)
                floatingControlView = controlView
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        floatingControlView?.updateState(selectedMask)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleOrientationChange()
    }

    /**
     * Handles screen rotation changes, recalculates floating icon position across 0°, 90°, 180°, 270°,
     * and refreshes overlay masks to conform to new dimensions.
     */
    private fun handleOrientationChange() {
        val (newWidth, newHeight) = getScreenDimensions()
        val newRotation = getScreenRotation()

        if (oldScreenWidth > 0 && oldScreenHeight > 0) {
            updateFloatingPositionOnRotation(
                oldWidth = oldScreenWidth,
                oldHeight = oldScreenHeight,
                oldRotation = oldRotation,
                newWidth = newWidth,
                newHeight = newHeight,
                newRotation = newRotation
            )
        }

        oldScreenWidth = newWidth
        oldScreenHeight = newHeight
        oldRotation = newRotation

        refreshAllMaskViews()
    }

    /**
     * Preserves the floating icon / control bar's physical screen position across screen rotations
     * (0°, 90°, 180°, 270°) by converting old coordinates into new coordinates and calling updateViewLayout.
     */
    fun updateFloatingPositionOnRotation(
        oldWidth: Int,
        oldHeight: Int,
        oldRotation: Int,
        newWidth: Int,
        newHeight: Int,
        newRotation: Int
    ) {
        val controlView = floatingControlView ?: return
        val params = controlView.getLayoutParamsTyped()

        val iconWidth = if (controlView.width > 0) controlView.width else params.width.coerceAtLeast(1)
        val iconHeight = if (controlView.height > 0) controlView.height else params.height.coerceAtLeast(1)

        val newPos = FloatingPositionCalculator.calculateRotatedPosition(
            oldX = params.x,
            oldY = params.y,
            oldWidth = oldWidth,
            oldHeight = oldHeight,
            oldRotation = oldRotation,
            newWidth = newWidth,
            newHeight = newHeight,
            newRotation = newRotation,
            iconWidth = iconWidth,
            iconHeight = iconHeight
        )

        params.x = newPos.x
        params.y = newPos.y

        try {
            windowManager.updateViewLayout(controlView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideFloatingController() {
        floatingControlView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingControlView = null
        }
    }

    /**
     * Completely cleans up all overlay views, stops foreground service,
     * and terminates the application process cleanly.
     */
    private fun killAndExitApp() {
        try {
            settingsRepository.setServiceActive(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        activeMaskViews.values.forEach { (view, _) ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        activeMaskViews.clear()

        hideFloatingController()

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopSelf()

        android.os.Process.killProcess(android.os.Process.myPid())
        kotlin.system.exitProcess(0)
    }

    private fun addNewMask(shapeType: String = MaskEntity.SHAPE_RECT) {
        serviceScope.launch(Dispatchers.IO) {
            val maskCount = currentMaskList.size + 1
            val isFreeQuad = shapeType == MaskEntity.SHAPE_FREE_QUAD
            val p1x = if (isFreeQuad) 0.20f else 0f
            val p1y = 0f
            val p2x = 1.0f
            val p2y = 0f
            val p3x = if (isFreeQuad) 0.80f else 1.0f
            val p3y = 1.0f
            val p4x = 0f
            val p4y = 1.0f

            val newMask = MaskEntity(
                id = UUID.randomUUID().toString(),
                title = getString(R.string.mask_title_format, maskCount),
                shapeType = shapeType,
                xRatio = 0.25f,
                yRatio = 0.35f,
                widthRatio = 0.50f,
                heightRatio = 0.22f,
                colorArgb = 0xFF000000L,
                alpha = 1.0f,
                isTouchThrough = true,
                isLocked = false,
                isVisible = true,
                p1x = p1x,
                p1y = p1y,
                p2x = p2x,
                p2y = p2y,
                p3x = p3x,
                p3y = p3y,
                p4x = p4x,
                p4y = p4y
            )
            maskRepository.addMask(newMask)
            settingsRepository.setSelectedMaskId(newMask.id)
        }
    }

    private fun getScreenDimensions(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            Pair(metrics.bounds.width(), metrics.bounds.height())
        } else {
            val point = Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(point)
            Pair(point.x, point.y)
        }
    }

    private fun getScreenRotation(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                display?.rotation ?: windowManager.defaultDisplay.rotation
            } catch (e: Exception) {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_title)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val editIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_START_EDIT_MODE
        }
        val pendingEdit = PendingIntent.getService(
            this, 0, editIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val modeText = if (isEditMode) getString(R.string.service_mode_edit) else getString(R.string.service_mode_locked)
        val notifTitle = getString(R.string.service_notification_title)
        val notifContent = getString(R.string.service_notification_text, modeText, currentMaskList.size)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notifTitle)
            .setContentText(notifContent)
            .setSmallIcon(R.drawable.ic_shape_square)
            .setContentIntent(pendingEdit)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_edit, getString(R.string.service_action_edit), pendingEdit)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.service_action_stop), pendingStop)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        activeMaskViews.values.forEach { (view, _) ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        activeMaskViews.clear()

        hideFloatingController()

        settingsRepository.setServiceActive(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "screen_mask_floating_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_SERVICE"
        const val ACTION_START_EDIT_MODE = "com.example.ACTION_START_EDIT_MODE"
        const val ACTION_TOGGLE_MODE = "com.example.ACTION_TOGGLE_MODE"
        const val ACTION_ADD_MASK = "com.example.ACTION_ADD_MASK"

        const val EXTRA_SHAPE_TYPE = "extra_shape_type"
    }
}
