package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppSettingsRepository
import com.example.data.DesignRepository
import com.example.data.MaskEntity
import com.example.data.MaskRepository
import com.example.data.model.DesignProject
import com.example.data.model.ShapeObject
import com.example.editor.history.EditorCommand
import com.example.editor.history.HistoryManager
import com.example.editor.ui.EditorTool
import com.example.service.OverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val maskRepository: MaskRepository,
    private val designRepository: DesignRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    // Overlays State
    val masks: StateFlow<List<MaskEntity>> = maskRepository.allMasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isServiceActive: StateFlow<Boolean> = settingsRepository.isServiceActive
    val isEditMode: StateFlow<Boolean> = settingsRepository.isEditMode
    val selectedMaskId: StateFlow<String?> = settingsRepository.selectedMaskId
    val startOnBoot: StateFlow<Boolean> = settingsRepository.startOnBoot

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    // Design Projects State
    val savedDesignProjects: StateFlow<List<DesignProject>> = designRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeDesignProject = MutableStateFlow<DesignProject?>(null)
    val activeDesignProject: StateFlow<DesignProject?> = _activeDesignProject.asStateFlow()

    // Editor Tools & Selection State
    private val _selectedShapeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedShapeIds: StateFlow<Set<String>> = _selectedShapeIds.asStateFlow()

    private val _currentTool = MutableStateFlow<EditorTool>(EditorTool.Select)
    val currentTool: StateFlow<EditorTool> = _currentTool.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    // History Manager for Undo/Redo
    private val historyManager = HistoryManager()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    init {
        observeActiveProject()
    }

    private fun observeActiveProject() {
        viewModelScope.launch {
            designRepository.activeProject.collectLatest { project ->
                if (project == null) {
                    // Create default initial project if empty
                    createInitialDefaultProject()
                } else {
                    _activeDesignProject.value = project
                }
            }
        }
    }

    private suspend fun createInitialDefaultProject() {
        val defaultProj = DesignProject(
            id = UUID.randomUUID().toString(),
            name = "Default Mask Design",
            targetWidth = 1080,
            targetHeight = 2400,
            shapes = listOf(
                ShapeObject(
                    id = UUID.randomUUID().toString(),
                    title = "Rectangle 1",
                    x = 200f,
                    y = 300f,
                    width = 680f,
                    height = 300f,
                    colorArgb = 0xFF000000L,
                    alpha = 1.0f,
                    zIndex = 1
                )
            ),
            isActive = true
        )
        designRepository.saveProject(defaultProj)
        designRepository.setActiveProject(defaultProj.id)
    }

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
                colorArgb = 0xFF000000L,
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

    // --- DESIGN EDITOR METHODS ---

    fun createNewDesignProject(name: String) {
        viewModelScope.launch {
            val newProj = DesignProject(
                id = UUID.randomUUID().toString(),
                name = name,
                targetWidth = 1080,
                targetHeight = 2400,
                shapes = emptyList(),
                isActive = true
            )
            designRepository.saveProject(newProj)
            designRepository.setActiveProject(newProj.id)
            _selectedShapeIds.value = emptySet()
            historyManager.clear()
            updateHistoryState()
        }
    }

    fun setActiveDesignProject(id: String) {
        viewModelScope.launch {
            designRepository.setActiveProject(id)
            _selectedShapeIds.value = emptySet()
            historyManager.clear()
            updateHistoryState()
        }
    }

    fun duplicateDesignProject(project: DesignProject) {
        viewModelScope.launch {
            val dup = project.copy(
                id = UUID.randomUUID().toString(),
                name = "${project.name} (Copy)",
                isActive = false,
                updatedAt = System.currentTimeMillis()
            )
            designRepository.saveProject(dup)
        }
    }

    fun scaleDesignProjectToCurrentDisplay(project: DesignProject, context: Context) {
        viewModelScope.launch {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)

            val currentWidth = metrics.widthPixels
            val currentHeight = metrics.heightPixels

            if (project.targetWidth == currentWidth && project.targetHeight == currentHeight) return@launch

            val scaleX = currentWidth.toFloat() / project.targetWidth.toFloat()
            val scaleY = currentHeight.toFloat() / project.targetHeight.toFloat()

            val scaledShapes = project.shapes.map { shape ->
                shape.copy(
                    x = shape.x * scaleX,
                    y = shape.y * scaleY,
                    width = shape.width * scaleX,
                    height = shape.height * scaleY
                )
            }

            val scaledProj = project.copy(
                targetWidth = currentWidth,
                targetHeight = currentHeight,
                targetDensity = metrics.density,
                shapes = scaledShapes,
                updatedAt = System.currentTimeMillis()
            )
            designRepository.saveProject(scaledProj)
            _activeDesignProject.value = scaledProj
        }
    }

    fun deleteDesignProject(id: String) {
        viewModelScope.launch {
            designRepository.deleteProject(id)
        }
    }

    fun setTool(tool: EditorTool) {
        _currentTool.value = tool
    }

    fun togglePreviewMode() {
        _isPreviewMode.value = !_isPreviewMode.value
    }

    fun selectShape(id: String?, isMultiSelect: Boolean) {
        if (id == null) {
            _selectedShapeIds.value = emptySet()
            return
        }
        if (isMultiSelect) {
            val current = _selectedShapeIds.value.toMutableSet()
            if (current.contains(id)) current.remove(id) else current.add(id)
            _selectedShapeIds.value = current
        } else {
            _selectedShapeIds.value = setOf(id)
        }
    }

    fun addShapeToCurrentDesign(shape: ShapeObject) {
        val proj = _activeDesignProject.value ?: return
        val newShapes = (proj.shapes + shape).sortedBy { it.zIndex }
        val updatedProj = proj.copy(shapes = newShapes)
        _activeDesignProject.value = updatedProj

        historyManager.pushCommand(EditorCommand.AddShape(shape))
        _selectedShapeIds.value = setOf(shape.id)
        updateHistoryState()
        autoSaveDesign(updatedProj)
    }

    fun updateShapeInCurrentDesign(updatedShape: ShapeObject) {
        val proj = _activeDesignProject.value ?: return
        val newShapes = proj.shapes.map { if (it.id == updatedShape.id) updatedShape else it }
        val updatedProj = proj.copy(shapes = newShapes)
        _activeDesignProject.value = updatedProj
        autoSaveDesign(updatedProj)
    }

    fun commitCurrentDesignStateToHistory() {
        val proj = _activeDesignProject.value ?: return
        historyManager.pushCommand(EditorCommand.ReplaceAllShapes(proj.shapes, proj.shapes))
        updateHistoryState()
    }

    fun reorderShapesInCurrentDesign(reorderedShapes: List<ShapeObject>) {
        val proj = _activeDesignProject.value ?: return
        val oldShapes = proj.shapes
        val updatedProj = proj.copy(shapes = reorderedShapes)
        _activeDesignProject.value = updatedProj

        historyManager.pushCommand(EditorCommand.ReorderShapes(oldShapes, reorderedShapes))
        updateHistoryState()
        autoSaveDesign(updatedProj)
    }

    fun deleteShapeFromCurrentDesign(id: String) {
        val proj = _activeDesignProject.value ?: return
        val toDelete = proj.shapes.filter { it.id == id }
        val newShapes = proj.shapes.filterNot { it.id == id }
        val updatedProj = proj.copy(shapes = newShapes)
        _activeDesignProject.value = updatedProj

        historyManager.pushCommand(EditorCommand.DeleteShapes(toDelete))
        _selectedShapeIds.value = _selectedShapeIds.value - id
        updateHistoryState()
        autoSaveDesign(updatedProj)
    }

    fun enableBlackoutModeForCurrentDesign() {
        val proj = _activeDesignProject.value ?: return
        val blackoutShapes = proj.shapes.map {
            it.copy(colorArgb = 0xFF000000L, alpha = 1.0f)
        }
        val updatedProj = proj.copy(shapes = blackoutShapes, isBlackoutMode = true)
        _activeDesignProject.value = updatedProj

        historyManager.pushCommand(EditorCommand.ReplaceAllShapes(proj.shapes, blackoutShapes))
        updateHistoryState()
        autoSaveDesign(updatedProj)
    }

    fun undo() {
        val proj = _activeDesignProject.value ?: return
        val restored = historyManager.undo(proj.shapes) ?: return
        val updatedProj = proj.copy(shapes = restored)
        _activeDesignProject.value = updatedProj
        updateHistoryState()
        autoSaveDesign(updatedProj)
    }

    fun redo() {
        val proj = _activeDesignProject.value ?: return
        val restored = historyManager.redo(proj.shapes) ?: return
        val updatedProj = proj.copy(shapes = restored)
        _activeDesignProject.value = updatedProj
        updateHistoryState()
        autoSaveDesign(updatedProj)
    }

    private fun updateHistoryState() {
        _canUndo.value = historyManager.canUndo()
        _canRedo.value = historyManager.canRedo()
    }

    private fun autoSaveDesign(project: DesignProject) {
        viewModelScope.launch {
            designRepository.saveProject(project)
        }
    }

    fun applyDesignToOverlays(context: Context) {
        viewModelScope.launch {
            val proj = _activeDesignProject.value ?: return@launch
            val targetW = proj.targetWidth.toFloat()
            val targetH = proj.targetHeight.toFloat()

            // Convert shapes to active MaskEntity items
            val newMaskEntities = proj.shapes.filter { it.isVisible }.map { shape ->
                MaskEntity(
                    id = shape.id,
                    title = shape.title,
                    xRatio = (shape.x / targetW).coerceIn(0f, 0.98f),
                    yRatio = (shape.y / targetH).coerceIn(0f, 0.98f),
                    widthRatio = (shape.width / targetW).coerceIn(0.02f, 1.0f),
                    heightRatio = (shape.height / targetH).coerceIn(0.02f, 1.0f),
                    colorArgb = shape.colorArgb,
                    alpha = shape.alpha,
                    isLocked = shape.isLocked,
                    isVisible = shape.isVisible,
                    updatedAt = System.currentTimeMillis()
                )
            }

            maskRepository.deleteAllMasks()
            maskRepository.addMasks(newMaskEntities)

            // Set to Block / Runtime mode
            settingsRepository.setEditMode(false)
            settingsRepository.setServiceActive(true)

            // Start/restart OverlayService
            if (Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            val maskRepo = MaskRepository.getInstance(db)
            val designRepo = DesignRepository.getInstance(db)
            val settingsRepo = AppSettingsRepository.getInstance(context)
            return MainViewModel(maskRepo, designRepo, settingsRepo) as T
        }
    }
}
