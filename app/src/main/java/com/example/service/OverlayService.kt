package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.Gravity
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
import kotlinx.coroutines.withContext
import java.util.UUID

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var maskRepository: MaskRepository
    private lateinit var settingsRepository: AppSettingsRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val activeMaskViews = mutableMapOf<String, Pair<OverlayMaskView, WindowManager.LayoutParams>>()
    private var floatingControlView: FloatingControlView? = null
    private var floatingControlParams: WindowManager.LayoutParams? = null

    private var currentMaskList = listOf<MaskEntity>()
    private var isEditMode = true
    private var selectedMaskId: String? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val db = AppDatabase.getInstance(this)
        maskRepository = MaskRepository.getInstance(db)
        settingsRepository = AppSettingsRepository.getInstance(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        observeState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                settingsRepository.setServiceActive(false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MODE -> {
                val nextMode = !settingsRepository.isEditMode.value
                settingsRepository.setEditMode(nextMode)
            }
            ACTION_ADD_MASK -> {
                addNewDefaultMask()
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
                refreshAllMaskViews()
            }
        }
    }

    private fun refreshAllMaskViews() {
        val (screenWidth, screenHeight) = getScreenDimensions()

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

            val isSelected = mask.id == selectedMaskId
            val params = createMaskLayoutParams(mask, isEditMode, screenWidth, screenHeight)

            if (activeMaskViews.containsKey(mask.id)) {
                val (view, _) = activeMaskViews[mask.id]!!
                view.updateData(mask, isEditMode, isSelected)
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                activeMaskViews[mask.id] = Pair(view, params)
            } else {
                val maskView = OverlayMaskView(
                    context = this,
                    mask = mask,
                    isEditMode = isEditMode,
                    isMaskSelected = isSelected,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    onUpdate = { updatedMask ->
                        serviceScope.launch(Dispatchers.IO) {
                            maskRepository.updateMask(updatedMask)
                        }
                    },
                    onDelete = { maskId ->
                        serviceScope.launch(Dispatchers.IO) {
                            maskRepository.deleteMask(maskId)
                        }
                    },
                    onSelect = { maskId ->
                        settingsRepository.setSelectedMaskId(maskId)
                    }
                )

                try {
                    windowManager.addView(maskView, params)
                    activeMaskViews[mask.id] = Pair(maskView, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. Update or attach Floating Control Bar in Edit Mode
        if (isEditMode) {
            showFloatingController()
        } else {
            hideFloatingController()
        }
    }

    private fun createMaskLayoutParams(
        mask: MaskEntity,
        editMode: Boolean,
        screenWidth: Int,
        screenHeight: Int
    ): WindowManager.LayoutParams {
        val flags = if (editMode && !mask.isLocked) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            // Block mode or locked mask: pass touch through completely!
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }

        val pxX = (mask.xRatio * screenWidth).toInt()
        val pxY = (mask.yRatio * screenHeight).toInt()
        val pxWidth = (mask.widthRatio * screenWidth).toInt().coerceAtLeast(1)
        val pxHeight = (mask.heightRatio * screenHeight).toInt().coerceAtLeast(1)

        return WindowManager.LayoutParams(
            pxWidth,
            pxHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = pxX
            y = pxY
        }
    }

    private fun showFloatingController() {
        if (floatingControlView == null) {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 80
            }

            val controlView = FloatingControlView(
                context = this,
                windowManager = windowManager,
                layoutParams = params,
                onAddMask = { addNewDefaultMask() },
                onToggleEditMode = {
                    settingsRepository.setEditMode(!isEditMode)
                },
                onToggleLockAll = {
                    serviceScope.launch(Dispatchers.IO) {
                        val allLocked = currentMaskList.all { it.isLocked }
                        maskRepository.setAllLockState(!allLocked)
                    }
                },
                onDeleteSelected = {
                    selectedMaskId?.let { id ->
                        serviceScope.launch(Dispatchers.IO) {
                            maskRepository.deleteMask(id)
                            settingsRepository.setSelectedMaskId(null)
                        }
                    }
                },
                onOpenApp = {
                    val appIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(appIntent)
                }
            )

            try {
                windowManager.addView(controlView, params)
                floatingControlView = controlView
                floatingControlParams = params
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        floatingControlView?.updateState(isEditMode, selectedMaskId != null)
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

    private fun addNewDefaultMask() {
        serviceScope.launch(Dispatchers.IO) {
            val maskCount = currentMaskList.size + 1
            val newMask = MaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Mask $maskCount",
                xRatio = 0.30f,
                yRatio = 0.35f,
                widthRatio = 0.40f,
                heightRatio = 0.20f,
                colorArgb = 0xFF000000L,
                alpha = 1.0f,
                isLocked = false,
                isVisible = true
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Mask Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active Screen Overlay Mask Service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_TOGGLE_MODE
        }
        val pendingToggle = PendingIntent.getService(
            this, 2, toggleIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val addIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_ADD_MASK
        }
        val pendingAdd = PendingIntent.getService(
            this, 3, addIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val modeText = if (isEditMode) "وضع التحرير (Edit Mode)" else "وضع الحجب (Block Mode)"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Mask Active")
            .setContentText("الحالة الحالية: $modeText | Mask Count: ${currentMaskList.size}")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_input_add, "+ Mask", pendingAdd)
            .addAction(android.R.drawable.ic_menu_edit, if (isEditMode) "Block" else "Edit", pendingToggle)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
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
        const val CHANNEL_ID = "screen_mask_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_SERVICE"
        const val ACTION_TOGGLE_MODE = "com.example.ACTION_TOGGLE_MODE"
        const val ACTION_ADD_MASK = "com.example.ACTION_ADD_MASK"
    }
}
