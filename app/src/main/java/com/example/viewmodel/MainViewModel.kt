package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppSettingsRepository
import com.example.data.MaskEntity
import com.example.data.MaskRepository
import com.example.service.OverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val maskRepository: MaskRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val masks: StateFlow<List<MaskEntity>> = maskRepository.allMasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isServiceActive: StateFlow<Boolean> = settingsRepository.isServiceActive
    val isEditMode: StateFlow<Boolean> = settingsRepository.isEditMode
    val selectedMaskId: StateFlow<String?> = settingsRepository.selectedMaskId
    val startOnBoot: StateFlow<Boolean> = settingsRepository.startOnBoot

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    fun checkOverlayPermission(context: Context) {
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)
    }

    fun toggleService(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            _hasOverlayPermission.value = false
            return
        }

        val currentState = isServiceActive.value
        val newState = !currentState
        settingsRepository.setServiceActive(newState)

        val serviceIntent = Intent(context, OverlayService::class.java)
        if (newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            context.stopService(serviceIntent)
        }
    }

    fun toggleEditMode() {
        settingsRepository.setEditMode(!isEditMode.value)
    }

    fun addMask() {
        viewModelScope.launch {
            val count = masks.value.size + 1
            val newMask = MaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Mask $count",
                xRatio = 0.25f,
                yRatio = 0.30f,
                widthRatio = 0.50f,
                heightRatio = 0.20f,
                colorArgb = 0xFF000000L, // Pure RGB 0,0,0
                alpha = 1.0f,
                isLocked = false,
                isVisible = true
            )
            maskRepository.addMask(newMask)
            settingsRepository.setSelectedMaskId(newMask.id)
        }
    }

    fun updateMask(mask: MaskEntity) {
        viewModelScope.launch {
            maskRepository.updateMask(mask)
        }
    }

    fun deleteMask(id: String) {
        viewModelScope.launch {
            maskRepository.deleteMask(id)
            if (selectedMaskId.value == id) {
                settingsRepository.setSelectedMaskId(null)
            }
        }
    }

    fun deleteAllMasks() {
        viewModelScope.launch {
            maskRepository.deleteAllMasks()
            settingsRepository.setSelectedMaskId(null)
        }
    }

    fun toggleLockAll() {
        viewModelScope.launch {
            val currentMasks = masks.value
            val anyUnlocked = currentMasks.any { !it.isLocked }
            maskRepository.setAllLockState(anyUnlocked)
        }
    }

    fun toggleHideAll() {
        viewModelScope.launch {
            val currentMasks = masks.value
            val anyVisible = currentMasks.any { it.isVisible }
            maskRepository.setAllVisibilityState(!anyVisible)
        }
    }

    fun selectMask(id: String?) {
        settingsRepository.setSelectedMaskId(id)
    }

    fun setStartOnBoot(enabled: Boolean) {
        settingsRepository.setStartOnBoot(enabled)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            val maskRepo = MaskRepository.getInstance(db)
            val settingsRepo = AppSettingsRepository.getInstance(context)
            return MainViewModel(maskRepo, settingsRepo) as T
        }
    }
}
