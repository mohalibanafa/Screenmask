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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MaskEntity
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenMaskApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkOverlayPermission(context)
    }

    val hasPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val masks by viewModel.masks.collectAsStateWithLifecycle()
    val selectedMaskId by viewModel.selectedMaskId.collectAsStateWithLifecycle()
    val startOnBoot by viewModel.startOnBoot.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "قناع الشاشة",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isServiceActive) "الخدمة شغالة • $masks.size مربع مغطي للشاشة" else "تغطية الكسور والبكسلات التالفة بسهولة",
                            fontSize = 12.sp,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isServiceActive) Color(0xFF10B981) else Color(0xFF64748B)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServiceActive) "مُفَعّل" else "متوقف",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isServiceActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Permission Card (if permission needed)
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

            // 2. Main Simplified Control Card
            item {
                SimplifiedMasterControlCard(
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

            // 3. Simple Visual 3-Step Guide
            item {
                QuickThreeStepGuideCard()
            }

            // 4. Active Masks List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المربعات المغطية للشاشة (${masks.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (masks.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.addMask() },
                            enabled = hasPermission,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("btn_add_mask_header")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ مربع جديد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Empty state if no masks
            if (masks.isEmpty()) {
                item {
                    EmptyMasksCard(onAddClicked = { viewModel.addMask() })
                }
            } else {
                items(masks, key = { it.id }) { mask ->
                    SimplifiedMaskItemCard(
                        mask = mask,
                        isSelected = mask.id == selectedMaskId,
                        onSelect = { viewModel.selectMask(mask.id) },
                        onUpdate = { viewModel.updateMask(it) },
                        onDelete = { viewModel.deleteMask(mask.id) }
                    )
                }
            }

            // 5. App Settings
            item {
                AppSettingsCard(
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
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_permission_warning")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "مطلوب إذن الظهور فوق الشاشة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "لتغطية الكسر أو العيوب على شاشة هاتفك، اضغط الزر أدناه لتفعيل إذن 'الظهور فوق التطبيقات'.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFDE68A)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onGrantClicked,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_grant_permission")
            ) {
                Text("منح الإذن الآن بضغطة واحدة", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SimplifiedMasterControlCard(
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(24.dp))
            .testTag("card_master_control")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Service Toggle Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تشغيل القناع على الشاشة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isServiceActive) "القناع يغطي الشاشة الآن" else "افتح المفتاح لتغطية الشاشة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Big Button: "+ إضافة مربع جديد"
            Button(
                onClick = onAddMask,
                enabled = hasPermission,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_add_box_primary")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+ إضافة مربع جديد للشاشة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Integrated 2-Way Mode Switcher: EDIT vs LOCK/BLOCK MODE
            Text(
                text = "اختر وضع التحكم:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Option 1: Edit Mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { if (!isEditMode) onToggleMode() }
                        .padding(vertical = 10.dp)
                        .testTag("btn_mode_edit"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✏️ وضع التعديل (تحريك)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Option 2: Lock / Block Mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!isEditMode) Color(0xFF10B981) else Color.Transparent)
                        .clickable { if (isEditMode) onToggleMode() }
                        .padding(vertical = 10.dp)
                        .testTag("btn_mode_block"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (!isEditMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🔒 وضع القفل (تغطية)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (!isEditMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Actions compact row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLockAll,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_lock_all")
                ) {
                    Text("🔒 قفل الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onHideAll,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_hide_all")
                ) {
                    Text("👁️ إخفاء/إظهار", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDeleteAll,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC5221F)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_delete_all")
                ) {
                    Text("🗑️ مسح الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SimplifiedMaskItemCard(
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(20.dp)
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
                            .background(Color(mask.colorArgb.toInt()).copy(alpha = mask.alpha))
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
                            text = if (mask.isLocked) "مثبّت ومقفول" else "قابل للتحريك",
                            fontSize = 11.sp,
                            color = if (mask.isLocked) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lock Toggle
                    IconButton(
                        onClick = { onUpdate(mask.copy(isLocked = !mask.isLocked)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (mask.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "قفل",
                            tint = if (mask.isLocked) Color(0xFFF59E0B) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Expand / Collapse Details Button
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "تفاصيل",
                            modifier = Modifier.size(22.dp)
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(14.dp)
                ) {
                    val isDefaultBlack = mask.colorArgb == 0xFF000000L
                    val isDefaultGray = mask.colorArgb == 0xFF1E293BL
                    val isDefaultRed = mask.colorArgb == 0xFF7F1D1DL
                    val isCustomColor = !isDefaultBlack && !isDefaultGray && !isDefaultRed

                    var showCustomPicker by remember { mutableStateOf(isCustomColor) }

                    Text(
                        text = "🎨 لون المربع:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ColorPresetChip(
                            color = Color.Black,
                            label = "أسود ناصع",
                            isSelected = !showCustomPicker && isDefaultBlack,
                            onClick = {
                                showCustomPicker = false
                                onUpdate(mask.copy(colorArgb = 0xFF000000L))
                            }
                        )

                        ColorPresetChip(
                            color = Color(0xFF1E293B),
                            label = "رمادي داكن",
                            isSelected = !showCustomPicker && isDefaultGray,
                            onClick = {
                                showCustomPicker = false
                                onUpdate(mask.copy(colorArgb = 0xFF1E293BL))
                            }
                        )

                        ColorPresetChip(
                            color = Color(0xFF7F1D1D),
                            label = "أحمر غامق",
                            isSelected = !showCustomPicker && isDefaultRed,
                            onClick = {
                                showCustomPicker = false
                                onUpdate(mask.copy(colorArgb = 0xFF7F1D1DL))
                            }
                        )

                        ColorPresetChip(
                            color = if (isCustomColor) Color(mask.colorArgb.toInt()) else Color(0xFF38BDF8),
                            label = "مخصص 🎨",
                            isSelected = showCustomPicker || isCustomColor,
                            onClick = {
                                showCustomPicker = !showCustomPicker
                            }
                        )
                    }

                    if (showCustomPicker || isCustomColor) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CustomColorPicker(
                            currentColorArgb = mask.colorArgb,
                            onColorSelected = { newColorArgb ->
                                onUpdate(mask.copy(colorArgb = newColorArgb))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transparency / Opacity Slider ("درجة العتمة والتغطية")
                    Text(
                        text = "⬛ درجة العتمة والتغطية (Opacity):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Opacity Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val opacityPresets = listOf(
                            1.0f to "100% معتم كلياً",
                            0.8f to "80% داكن",
                            0.5f to "50% نصف شفاف",
                            0.2f to "20% خفيف"
                        )
                        opacityPresets.forEach { (alphaVal, labelText) ->
                            val isSelectedAlpha = kotlin.math.abs(mask.alpha - alphaVal) < 0.05f
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelectedAlpha) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(
                                        width = if (isSelectedAlpha) 1.5.dp else 0.dp,
                                        color = if (isSelectedAlpha) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onUpdate(mask.copy(alpha = alphaVal)) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = labelText,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelectedAlpha) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelectedAlpha) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    SliderSettingRow(
                        label = "مستوى التغطية والعتمة (100% معتم كلياً يغطي الشاشة - 0% شفاف)",
                        value = mask.alpha,
                        range = 0.0f..1.0f,
                        step = 0.01f,
                        onValueChange = { newAlpha ->
                            onUpdate(mask.copy(alpha = newAlpha))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "📐 الأبعاد والموقع بالتفصيل:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset buttons for quick thin line / thickness choices
                    Text(
                        text = "اختيار سريع لسماكة الارتفاع:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PresetSizeChip(
                            label = "خط دقيق (1px)",
                            isSelected = mask.heightRatio < 0.002f,
                            onClick = { onUpdate(mask.copy(heightRatio = 0.0008f)) }
                        )
                        PresetSizeChip(
                            label = "رفيع جداً (0.5%)",
                            isSelected = mask.heightRatio in 0.002f..0.008f,
                            onClick = { onUpdate(mask.copy(heightRatio = 0.005f)) }
                        )
                        PresetSizeChip(
                            label = "شريط ناعم (2%)",
                            isSelected = mask.heightRatio in 0.008f..0.04f,
                            onClick = { onUpdate(mask.copy(heightRatio = 0.02f)) }
                        )
                        PresetSizeChip(
                            label = "مربع عادي (20%)",
                            isSelected = mask.heightRatio > 0.15f,
                            onClick = { onUpdate(mask.copy(heightRatio = 0.20f)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SliderSettingRow(
                        label = "الموقع أفقي (يمين/يسار)",
                        value = mask.xRatio,
                        range = 0.0f..1.0f,
                        step = 0.001f,
                        onValueChange = { onUpdate(mask.copy(xRatio = it)) }
                    )

                    SliderSettingRow(
                        label = "الموقع رأسي (أعلى/أسفل)",
                        value = mask.yRatio,
                        range = 0.0f..1.0f,
                        step = 0.001f,
                        onValueChange = { onUpdate(mask.copy(yRatio = it)) }
                    )

                    SliderSettingRow(
                        label = "عرض المربع (Width)",
                        value = mask.widthRatio,
                        range = 0.0005f..1.0f,
                        step = 0.001f,
                        onValueChange = { onUpdate(mask.copy(widthRatio = it)) }
                    )

                    SliderSettingRow(
                        label = "ارتفاع/سماكة المربع (Thickness/Height)",
                        value = mask.heightRatio,
                        range = 0.0005f..1.0f,
                        step = 0.001f,
                        onValueChange = { onUpdate(mask.copy(heightRatio = it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun PresetSizeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SliderSettingRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0.0005f..1.0f,
    step: Float = 0.001f,
    onValueChange: (Float) -> Unit
) {
    val displayValue = value.coerceIn(range.start, range.endInclusive)
    val percentText = when {
        displayValue < 0.01f -> java.lang.String.format(java.util.Locale.US, "%.2f%%", displayValue * 100)
        displayValue < 0.10f -> java.lang.String.format(java.util.Locale.US, "%.1f%%", displayValue * 100)
        else -> "${(displayValue * 100).toInt()}%"
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fine-tune decrease button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable {
                            val newVal = (displayValue - step).coerceIn(range.start, range.endInclusive)
                            onValueChange(newVal)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = percentText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Fine-tune increase button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable {
                            val newVal = (displayValue + step).coerceIn(range.start, range.endInclusive)
                            onValueChange(newVal)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Slider(
            value = displayValue,
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
                    .size(12.dp)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
            .testTag("card_empty_masks")
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CropSquare,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "لا يوجد أي مربع قناع حالياً",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "اضغط الزر أدناه لإضافة مربع أسود لتغطية البكسلات أو الكسر في شاشة هاتفك.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAddClicked,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("btn_empty_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ إضافة أول مربع الآن", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickThreeStepGuideCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💡 كيف تستخدم التطبيق بـ 3 خطوات بسيطة:",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            StepGuideRow(stepNum = "1", text = "اضغط '+ إضافة مربع جديد' لإدراج مربع أسود على الشاشة.")
            Spacer(modifier = Modifier.height(6.dp))
            StepGuideRow(stepNum = "2", text = "في وضع التعديل، اسحب المربع وغيّر حجمه لتغطية الجزء التالف بالضبط.")
            Spacer(modifier = Modifier.height(6.dp))
            StepGuideRow(stepNum = "3", text = "اختر 'وضع القفل' لتثبيت المربع واستخدام باقي تطبيقات الهاتف بحرية!")
        }
    }
}

@Composable
fun StepGuideRow(stepNum: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stepNum, color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun AppSettingsCard(
    startOnBoot: Boolean,
    onStartOnBootChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "التشغيل التلقائي عند تشغيل الهاتف", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = "استعادة المربعات السوداء تلقائياً بعد إعادة تشغيل الهاتف",
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
    }
}

@Composable
fun CustomColorPicker(
    currentColorArgb: Long,
    onColorSelected: (Long) -> Unit
) {
    val currentHsv = remember(currentColorArgb) { argbToHsv(currentColorArgb) }
    var hue by remember(currentColorArgb) { mutableStateOf(currentHsv[0]) }
    var saturation by remember(currentColorArgb) { mutableStateOf(currentHsv[1]) }
    var value by remember(currentColorArgb) { mutableStateOf(currentHsv[2]) }

    val updatedColorArgb = remember(hue, saturation, value) {
        hsvToArgb(hue, saturation, value)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
            .testTag("custom_color_picker")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎨 منتقي الألوان المخصص:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Live Preview Box with Hex code
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(updatedColorArgb.toInt()))
                        .border(1.dp, Color.Gray, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val hexString = String.format("#%06X", (0xFFFFFFL and updatedColorArgb))
                Text(
                    text = hexString,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Swatches Row
        Text(
            text = "درجات ألوان شائعة:",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val quickSwatches = listOf(
                0xFFFFFFFFL to "أبيض",
                0xFF38BDF8L to "سماوي",
                0xFF10B981L to "أخضر",
                0xFFF59E0BL to "أصفر",
                0xFFF97316L to "برتقالي",
                0xFFA855F7L to "بنفسجي",
                0xFFEC4899L to "وردي"
            )
            quickSwatches.forEach { (colorVal, _) ->
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(colorVal.toInt()))
                        .border(
                            width = if (currentColorArgb == colorVal) 2.dp else 1.dp,
                            color = if (currentColorArgb == colorVal) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorVal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hue Spectrum Bar & Slider
        Text(
            text = "تدرج اللون (Hue): ${(hue.toInt())}°",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        val rainbowGradient = Brush.horizontalGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(rainbowGradient)
        )

        Slider(
            value = hue,
            onValueChange = {
                hue = it
                onColorSelected(hsvToArgb(hue, saturation, value))
            },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = Color(updatedColorArgb.toInt()),
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .testTag("slider_hue")
        )

        // Saturation Slider
        SliderSettingRow(
            label = "تشبع اللون (Saturation)",
            value = saturation,
            range = 0f..1f,
            step = 0.02f,
            onValueChange = {
                saturation = it
                onColorSelected(hsvToArgb(hue, saturation, value))
            }
        )

        // Brightness Slider
        SliderSettingRow(
            label = "السطوع (Brightness)",
            value = value,
            range = 0f..1f,
            step = 0.02f,
            onValueChange = {
                value = it
                onColorSelected(hsvToArgb(hue, saturation, value))
            }
        )
    }
}

fun hsvToArgb(hue: Float, saturation: Float, value: Float): Long {
    val hsv = floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    val argbInt = android.graphics.Color.HSVToColor(hsv)
    return argbInt.toLong() and 0xFFFFFFFFL
}

fun argbToHsv(colorArgb: Long): FloatArray {
    val argbInt = colorArgb.toInt()
    val r = android.graphics.Color.red(argbInt)
    val g = android.graphics.Color.green(argbInt)
    val b = android.graphics.Color.blue(argbInt)
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(r, g, b, hsv)
    return hsv
}
