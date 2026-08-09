package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("screen_mask_prefs", Context.MODE_PRIVATE)

    private val _isServiceActive = MutableStateFlow(prefs.getBoolean(KEY_SERVICE_ACTIVE, false))
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _isEditMode = MutableStateFlow(prefs.getBoolean(KEY_EDIT_MODE, true))
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _selectedMaskId = MutableStateFlow<String?>(prefs.getString(KEY_SELECTED_MASK, null))
    val selectedMaskId: StateFlow<String?> = _selectedMaskId.asStateFlow()

    private val _startOnBoot = MutableStateFlow(prefs.getBoolean(KEY_START_ON_BOOT, false))
    val startOnBoot: StateFlow<Boolean> = _startOnBoot.asStateFlow()

    fun setServiceActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ACTIVE, active).apply()
        _isServiceActive.value = active
    }

    fun setEditMode(editMode: Boolean) {
        prefs.edit().putBoolean(KEY_EDIT_MODE, editMode).apply()
        _isEditMode.value = editMode
    }

    fun setSelectedMaskId(id: String?) {
        prefs.edit().putString(KEY_SELECTED_MASK, id).apply()
        _selectedMaskId.value = id
    }

    fun setStartOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_START_ON_BOOT, enabled).apply()
        _startOnBoot.value = enabled
    }

    companion object {
        private const val KEY_SERVICE_ACTIVE = "key_service_active"
        private const val KEY_EDIT_MODE = "key_edit_mode"
        private const val KEY_SELECTED_MASK = "key_selected_mask"
        private const val KEY_START_ON_BOOT = "key_start_on_boot"

        @Volatile
        private var INSTANCE: AppSettingsRepository? = null

        fun getInstance(context: Context): AppSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AppSettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
