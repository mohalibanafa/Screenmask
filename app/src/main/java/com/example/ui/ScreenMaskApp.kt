package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MaskEntity
import com.example.editor.ui.DesignEditorScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenMaskApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkOverlayPermission(context)
    }

    var isDesignEditorOpen by remember { mutableStateOf(false) }

    if (isDesignEditorOpen) {
        DesignEditorScreen(
            viewModel = viewModel,
            onBack = { isDesignEditorOpen = false }
        )
        return
    }

    val hasPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val masks by viewModel.masks.collectAsStateWithLifecycle()
    val selectedMaskId by viewModel.selectedMaskId.collectAsStateWithLifecycle()
    val startOnBoot by viewModel.startOnBoot.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeDesignProject.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Screen Masker Studio",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isServiceActive) "Service Active • ${masks.size} Overlay(s)" else "Vector Mask Design Studio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isServiceActive) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isServiceActive) "ACTIVE" else "STOPPED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isServiceActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Permission Card if needed
            if (!hasPermission) {
                item {
                    PermissionWarningCard(
                        onGrantClicked = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // 2. Open Full Screen Design Editor Banner (PROMINENT CALL TO ACTION)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDesignEditorOpen = true }
                        .testTag("banner_open_design_editor")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Design Studio (محرر التصميم)",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Active Canvas: ${activeProject?.name ?: "Default"} • Vector shapes, zoom & pan, snapping, numerical controls",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }

                        Button(
                            onClick = { isDesignEditorOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Open Studio", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 3. Master Control Panel Card
            item {
                MasterControlCard(
                    isServiceActive = isServiceActive,
                    isEditMode = isEditMode,
                    hasPermission = hasPermission,
                    maskCount = masks.size,
                    onToggleService = { viewModel.toggleService(context) },
                    onToggleMode = { viewModel.toggleEditMode() },
                    onAddMask = { viewModel.addMask() },
                    onLockAll = { viewModel.toggleLockAll() },
                    onHideAll = { viewModel.toggleHideAll() },
                    onDeleteAll = { viewModel.deleteAllMasks() }
                )
            }

            // 4. Active Masks Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Screen Overlays",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = { isDesignEditorOpen = true },
                        enabled = hasPermission,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("btn_add_mask_header")
                    ) {
                        Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Canvas Editor", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Empty state if no masks
            if (masks.isEmpty()) {
                item {
                    EmptyMasksCard(onAddClicked = { isDesignEditorOpen = true })
                }
            } else {
                items(masks, key = { it.id }) { mask ->
                    MaskItemCard(
                        mask = mask,
                        isSelected = mask.id == selectedMaskId,
                        onSelect = { viewModel.selectMask(mask.id) },
                        onUpdate = { viewModel.updateMask(it) },
                        onDelete = { viewModel.deleteMask(mask.id) }
                    )
                }
            }

            // 5. Application Settings & Usage Guide
            item {
                SettingsAndGuideCard(
                    startOnBoot = startOnBoot,
                    onStartOnBootChange = { viewModel.setStartOnBoot(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun PermissionWarningCard(onGrantClicked: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF451A03)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("card_permission_warning")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To draw black masks over broken or bright screen areas without root, please grant 'Display over other apps' permission.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFDE68A)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onGrantClicked,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                modifier = Modifier.align(Alignment.End).testTag("btn_grant_permission")
            ) {
                Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MasterControlCard(
    isServiceActive: Boolean,
    isEditMode: Boolean,
    hasPermission: Boolean,
    maskCount: Int,
    onToggleService: () -> Unit,
    onToggleMode: () -> Unit,
    onAddMask: () -> Unit,
    onLockAll: () -> Unit,
    onHideAll: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(28.dp))
            .testTag("card_master_control")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time Screen Overlays",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isServiceActive) "Overlays actively drawing on screen" else "Service currently stopped",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Switch(
                    checked = isServiceActive,
                    onCheckedChange = { onToggleService() },
                    enabled = hasPermission,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("switch_master_service")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Mode Selector: EDIT MODE vs BLOCK MODE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isEditMode) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                        .clickable { if (!isEditMode) onToggleMode() }
                        .padding(vertical = 12.dp)
                        .testTag("btn_mode_edit"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EDIT MODE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isEditMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { if (isEditMode) onToggleMode() }
                        .padding(vertical = 12.dp)
                        .testTag("btn_mode_block"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BLOCK MODE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = if (!isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLockAll,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f).testTag("btn_lock_all")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lock All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onHideAll,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f).testTag("btn_hide_all")
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hide/Show", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDeleteAll,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC5221F)),
                    modifier = Modifier.weight(1f).testTag("btn_delete_all")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MaskItemCard(
    mask: MaskEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdate: (MaskEntity) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(isSelected) }

    LaunchedEffect(isSelected) {
        if (isSelected) expanded = true
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onSelect() }
            .testTag("card_mask_item_${mask.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(mask.colorArgb.toInt()))
                            .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = mask.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Pos (${(mask.xRatio * 100).toInt()}%, ${(mask.yRatio * 100).toInt()}%) • Size (${(mask.widthRatio * 100).toInt()}% x ${(mask.heightRatio * 100).toInt()}%)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = { onUpdate(mask.copy(isLocked = !mask.isLocked)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (mask.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (mask.isLocked) Color(0xFFF59E0B) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onUpdate(mask.copy(isVisible = !mask.isVisible)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (mask.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Visibility",
                            tint = if (mask.isVisible) Color(0xFF10B981) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Digital Fine-Tuning",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SliderSettingRow(
                        label = "X Position (${(mask.xRatio * 100).toInt()}%)",
                        value = mask.xRatio,
                        onValueChange = { onUpdate(mask.copy(xRatio = it)) }
                    )

                    SliderSettingRow(
                        label = "Y Position (${(mask.yRatio * 100).toInt()}%)",
                        value = mask.yRatio,
                        onValueChange = { onUpdate(mask.copy(yRatio = it)) }
                    )

                    SliderSettingRow(
                        label = "Width (${(mask.widthRatio * 100).toInt()}%)",
                        value = mask.widthRatio,
                        range = 0.05f..1.0f,
                        onValueChange = { onUpdate(mask.copy(widthRatio = it)) }
                    )

                    SliderSettingRow(
                        label = "Height (${(mask.heightRatio * 100).toInt()}%)",
                        value = mask.heightRatio,
                        range = 0.02f..1.0f,
                        onValueChange = { onUpdate(mask.copy(heightRatio = it)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Mask Color & Opacity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorPresetChip(
                            color = Color.Black,
                            label = "Pure Black (0,0,0)",
                            isSelected = mask.colorArgb == 0xFF000000L,
                            onClick = { onUpdate(mask.copy(colorArgb = 0xFF000000L, alpha = 1.0f)) }
                        )

                        ColorPresetChip(
                            color = Color(0xFF1E293B),
                            label = "Dark Slate",
                            isSelected = mask.colorArgb == 0xFF1E293BL,
                            onClick = { onUpdate(mask.copy(colorArgb = 0xFF1E293BL)) }
                        )

                        ColorPresetChip(
                            color = Color(0xFF7F1D1D),
                            label = "Red Tint",
                            isSelected = mask.colorArgb == 0xFF7F1D1DL,
                            onClick = { onUpdate(mask.copy(colorArgb = 0xFF7F1D1DL)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SliderSettingRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0.0f..1.0f,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
fun ColorPresetChip(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun EmptyMasksCard(onAddClicked: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(24.dp))
            .testTag("card_empty_masks")
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No Active Screen Masks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Open the Vector Design Studio to draw, position, and customize custom shapes and masks for your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClicked,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("btn_empty_add")
            ) {
                Text("Open Design Studio", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsAndGuideCard(
    startOnBoot: Boolean,
    onStartOnBootChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Settings & Quick Instructions",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Start Service on Phone Boot", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Automatically restore black screen masks after restarting device",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = startOnBoot,
                    onCheckedChange = onStartOnBootChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Text(text = "💡 How it works:", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "1. Design Studio: Full-screen virtual canvas simulating physical screen. Place rectangles, rounded notches, circles, lines, polygons, and freehand shapes.", fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "2. Numerical Precision: Edit exact pixel coordinates (X, Y, W, H, Rotation, Opacity) or use touch handles with auto-snapping.", fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "3. Apply / Run: Renders pure black overlays over dead pixels while allowing touch pass-through in Block Mode.", fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}
