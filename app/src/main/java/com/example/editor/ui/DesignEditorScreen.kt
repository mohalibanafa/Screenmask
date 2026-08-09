package com.example.editor.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShapeObject
import com.example.data.model.ShapeType
import com.example.viewmodel.MainViewModel

enum class EditorTab {
    PROPERTIES, COLOR, LAYERS, GRID_SNAP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignEditorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val currentProject by viewModel.activeDesignProject.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedDesignProjects.collectAsStateWithLifecycle()
    val selectedShapeIds by viewModel.selectedShapeIds.collectAsStateWithLifecycle()
    val currentTool by viewModel.currentTool.collectAsStateWithLifecycle()
    val isPreviewMode by viewModel.isPreviewMode.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(EditorTab.PROPERTIES) }
    var showDesignManager by remember { mutableStateOf(false) }

    // Canvas zoom & pan state
    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Grid settings
    var showGrid by remember { mutableStateOf(true) }
    var gridSizePx by remember { mutableStateOf(40f) }
    var snapToGrid by remember { mutableStateOf(true) }
    var snapToEdges by remember { mutableStateOf(true) }
    var snapToCenter by remember { mutableStateOf(true) }

    val shapes = currentProject?.shapes ?: emptyList()
    val primarySelectedShape = shapes.lastOrNull { it.id in selectedShapeIds }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = currentProject?.name ?: "Design Editor",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Canvas: ${currentProject?.targetWidth ?: 1080}x${currentProject?.targetHeight ?: 2400}px • ${shapes.size} Objects",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Undo & Redo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier.testTag("btn_undo")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) MaterialTheme.colorScheme.onSurface else Color.Gray)
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier.testTag("btn_redo")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) MaterialTheme.colorScheme.onSurface else Color.Gray)
                    }

                    // Preview Toggle Button
                    IconButton(
                        onClick = { viewModel.togglePreviewMode() },
                        modifier = Modifier.testTag("btn_preview_toggle")
                    ) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Preview Mode",
                            tint = if (isPreviewMode) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Saved Designs Dialog Trigger
                    IconButton(onClick = { showDesignManager = true }) {
                        Icon(Icons.Default.Folder, contentDescription = "Saved Designs")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Apply / Run Overlays Button
                    Button(
                        onClick = {
                            viewModel.applyDesignToOverlays(context)
                            Toast.makeText(context, "Applying Design to Screen Overlays...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_apply_design")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply / Run", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Workspace Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Virtual Canvas
                VirtualCanvasView(
                    modifier = Modifier.fillMaxSize(),
                    shapes = shapes,
                    selectedShapeIds = selectedShapeIds,
                    currentTool = currentTool,
                    virtualWidth = (currentProject?.targetWidth ?: 1080).toFloat(),
                    virtualHeight = (currentProject?.targetHeight ?: 2400).toFloat(),
                    zoomScale = zoomScale,
                    panOffset = panOffset,
                    showGrid = showGrid,
                    gridSizePx = gridSizePx,
                    snapToGrid = snapToGrid,
                    snapToEdges = snapToEdges,
                    snapToCenter = snapToCenter,
                    isPreviewMode = isPreviewMode,
                    onSelectShape = { id, isMulti ->
                        viewModel.selectShape(id, isMulti)
                    },
                    onShapeTransformed = { updated ->
                        viewModel.updateShapeInCurrentDesign(updated)
                    },
                    onShapeTransformFinished = {
                        viewModel.commitCurrentDesignStateToHistory()
                    },
                    onNewShapeCreated = { newShape ->
                        viewModel.addShapeToCurrentDesign(newShape)
                    },
                    onZoomPanChanged = { newZoom, newPan ->
                        zoomScale = newZoom
                        panOffset = newPan
                    }
                )

                // Floating Drawing Toolbar (Top Center)
                if (!isPreviewMode) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolIconButton(
                                icon = Icons.Default.NearMe,
                                label = "Select",
                                isSelected = currentTool is EditorTool.Select,
                                onClick = { viewModel.setTool(EditorTool.Select) }
                            )

                            ToolIconButton(
                                icon = Icons.Default.CropSquare,
                                label = "Rect",
                                isSelected = (currentTool as? EditorTool.Draw)?.shapeType == ShapeType.RECTANGLE,
                                onClick = { viewModel.setTool(EditorTool.Draw(ShapeType.RECTANGLE)) }
                            )

                            ToolIconButton(
                                icon = Icons.Default.RoundedCorner,
                                label = "Rounded",
                                isSelected = (currentTool as? EditorTool.Draw)?.shapeType == ShapeType.ROUNDED_RECT,
                                onClick = { viewModel.setTool(EditorTool.Draw(ShapeType.ROUNDED_RECT)) }
                            )

                            ToolIconButton(
                                icon = Icons.Default.Circle,
                                label = "Circle",
                                isSelected = (currentTool as? EditorTool.Draw)?.shapeType == ShapeType.CIRCLE,
                                onClick = { viewModel.setTool(EditorTool.Draw(ShapeType.CIRCLE)) }
                            )

                            ToolIconButton(
                                icon = Icons.Default.ShowChart,
                                label = "Line",
                                isSelected = (currentTool as? EditorTool.Draw)?.shapeType == ShapeType.LINE,
                                onClick = { viewModel.setTool(EditorTool.Draw(ShapeType.LINE)) }
                            )

                            ToolIconButton(
                                icon = Icons.Default.Polyline,
                                label = "Polygon",
                                isSelected = (currentTool as? EditorTool.Draw)?.shapeType == ShapeType.POLYGON,
                                onClick = { viewModel.setTool(EditorTool.Draw(ShapeType.POLYGON)) }
                            )

                            ToolIconButton(
                                icon = Icons.Default.Draw,
                                label = "Freehand",
                                isSelected = (currentTool as? EditorTool.Draw)?.shapeType == ShapeType.FREEHAND_PATH,
                                onClick = { viewModel.setTool(EditorTool.Draw(ShapeType.FREEHAND_PATH)) }
                            )
                        }
                    }
                }

                // Floating Zoom & View Controls (Bottom Right)
                if (!isPreviewMode) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(5.0f) }) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.2f) }) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                zoomScale = 1.0f
                                panOffset = Offset.Zero
                            }) {
                                Icon(Icons.Default.FitScreen, contentDescription = "Fit to Screen", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { showGrid = !showGrid }) {
                                Icon(
                                    Icons.Default.GridOn,
                                    contentDescription = "Grid",
                                    tint = if (showGrid) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation & Collapsible Panels
            if (!isPreviewMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Bottom Control Tabs Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TabButton(
                                label = "Properties",
                                icon = Icons.Default.Tune,
                                isSelected = activeTab == EditorTab.PROPERTIES,
                                onClick = { activeTab = EditorTab.PROPERTIES }
                            )

                            TabButton(
                                label = "Color",
                                icon = Icons.Default.Palette,
                                isSelected = activeTab == EditorTab.COLOR,
                                onClick = { activeTab = EditorTab.COLOR }
                            )

                            TabButton(
                                label = "Layers",
                                icon = Icons.Default.Layers,
                                isSelected = activeTab == EditorTab.LAYERS,
                                onClick = { activeTab = EditorTab.LAYERS }
                            )

                            TabButton(
                                label = "Snap / Grid",
                                icon = Icons.Default.GridOn,
                                isSelected = activeTab == EditorTab.GRID_SNAP,
                                onClick = { activeTab = EditorTab.GRID_SNAP }
                            )
                        }

                        // Tab Content Body
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            when (activeTab) {
                                EditorTab.PROPERTIES -> {
                                    NumericalPropertiesPanel(
                                        shape = primarySelectedShape,
                                        canvasWidth = (currentProject?.targetWidth ?: 1080).toFloat(),
                                        canvasHeight = (currentProject?.targetHeight ?: 2400).toFloat(),
                                        onUpdateShape = { updated ->
                                            viewModel.updateShapeInCurrentDesign(updated)
                                            viewModel.commitCurrentDesignStateToHistory()
                                        }
                                    )
                                }
                                EditorTab.COLOR -> {
                                    ColorPickerPanel(
                                        selectedShape = primarySelectedShape,
                                        onColorChanged = { argb, alpha ->
                                            primarySelectedShape?.let { shape ->
                                                viewModel.updateShapeInCurrentDesign(shape.copy(colorArgb = argb, alpha = alpha))
                                                viewModel.commitCurrentDesignStateToHistory()
                                            }
                                        },
                                        onBlackoutModeToggle = {
                                            viewModel.enableBlackoutModeForCurrentDesign()
                                        }
                                    )
                                }
                                EditorTab.LAYERS -> {
                                    LayerPanelSheet(
                                        shapes = shapes,
                                        selectedShapeIds = selectedShapeIds,
                                        onSelectShape = { id, isMulti -> viewModel.selectShape(id, isMulti) },
                                        onUpdateShape = { updated ->
                                            viewModel.updateShapeInCurrentDesign(updated)
                                            viewModel.commitCurrentDesignStateToHistory()
                                        },
                                        onReorderShapes = { reordered ->
                                            viewModel.reorderShapesInCurrentDesign(reordered)
                                        },
                                        onDeleteShape = { id -> viewModel.deleteShapeFromCurrentDesign(id) }
                                    )
                                }
                                EditorTab.GRID_SNAP -> {
                                    GridSnapSettingsCard(
                                        showGrid = showGrid,
                                        onShowGridChange = { showGrid = it },
                                        gridSizePx = gridSizePx,
                                        onGridSizeChange = { gridSizePx = it },
                                        snapToGrid = snapToGrid,
                                        onSnapToGridChange = { snapToGrid = it },
                                        snapToEdges = snapToEdges,
                                        onSnapToEdgesChange = { snapToEdges = it },
                                        snapToCenter = snapToCenter,
                                        onSnapToCenterChange = { snapToCenter = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Saved Designs Manager Dialog
    if (showDesignManager) {
        DesignManagerDialog(
            projects = savedProjects,
            activeProjectId = currentProject?.id,
            onDismiss = { showDesignManager = false },
            onCreateNewProject = { name -> viewModel.createNewDesignProject(name) },
            onSelectProject = { id -> viewModel.setActiveDesignProject(id) },
            onDuplicateProject = { proj -> viewModel.duplicateDesignProject(proj) },
            onScaleProjectToCurrentDisplay = { proj -> viewModel.scaleDesignProjectToCurrentDisplay(proj, context) },
            onDeleteProject = { id -> viewModel.deleteDesignProject(id) }
        )
    }
}

@Composable
fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun TabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GridSnapSettingsCard(
    showGrid: Boolean,
    onShowGridChange: (Boolean) -> Unit,
    gridSizePx: Float,
    onGridSizeChange: (Float) -> Unit,
    snapToGrid: Boolean,
    onSnapToGridChange: (Boolean) -> Unit,
    snapToEdges: Boolean,
    onSnapToEdgesChange: (Boolean) -> Unit,
    snapToCenter: Boolean,
    onSnapToCenterChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Grid & Snap Alignment Controls", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Show Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Show Alignment Grid", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Switch(checked = showGrid, onCheckedChange = onShowGridChange)
            }

            // Grid Size
            Text(text = "Grid Size: ${gridSizePx.toInt()} px", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.Slider(
                value = gridSizePx,
                onValueChange = onGridSizeChange,
                valueRange = 10f..100f
            )

            // Snap to Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Snap Objects to Grid Lines", fontSize = 13.sp)
                Switch(checked = snapToGrid, onCheckedChange = onSnapToGridChange)
            }

            // Snap to Edges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Snap to Screen Boundaries/Edges", fontSize = 13.sp)
                Switch(checked = snapToEdges, onCheckedChange = onSnapToEdgesChange)
            }

            // Snap to Center
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Snap to Screen Center Lines (X/Y)", fontSize = 13.sp)
                Switch(checked = snapToCenter, onCheckedChange = onSnapToCenterChange)
            }
        }
    }
}
