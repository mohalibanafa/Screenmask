package com.example.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShapeObject

@Composable
fun LayerPanelSheet(
    modifier: Modifier = Modifier,
    shapes: List<ShapeObject>,
    selectedShapeIds: Set<String>,
    onSelectShape: (String, Boolean) -> Unit,
    onUpdateShape: (ShapeObject) -> Unit,
    onReorderShapes: (List<ShapeObject>) -> Unit,
    onDeleteShape: (String) -> Unit
) {
    // Highest zIndex at top
    val sortedLayers = remember(shapes) { shapes.sortedByDescending { it.zIndex } }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
            .testTag("panel_layers")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layers Panel (${shapes.size})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Top to Bottom Order",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sortedLayers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No shapes added yet. Choose a drawing tool from the toolbar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sortedLayers, key = { _, item -> item.id }) { index, shape ->
                        val isSelected = shape.id in selectedShapeIds

                        LayerItemRow(
                            shape = shape,
                            isSelected = isSelected,
                            canMoveUp = index > 0,
                            canMoveDown = index < sortedLayers.size - 1,
                            onSelect = { onSelectShape(shape.id, false) },
                            onToggleVisibility = { onUpdateShape(shape.copy(isVisible = !shape.isVisible)) },
                            onToggleLock = { onUpdateShape(shape.copy(isLocked = !shape.isLocked)) },
                            onMoveUp = { moveLayer(sortedLayers, index, index - 1, onReorderShapes) },
                            onMoveDown = { moveLayer(sortedLayers, index, index + 1, onReorderShapes) },
                            onBringToFront = { moveLayer(sortedLayers, index, 0, onReorderShapes) },
                            onSendToBack = { moveLayer(sortedLayers, index, sortedLayers.size - 1, onReorderShapes) },
                            onRename = { newName -> onUpdateShape(shape.copy(title = newName)) },
                            onDelete = { onDeleteShape(shape.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LayerItemRow(
    shape: ShapeObject,
    isSelected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var nameText by remember(shape.title) { mutableStateOf(shape.title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceContainerLowest
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color Badge
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(shape.colorArgb.toInt()))
                .border(1.dp, Color.Gray, CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Name or Edit TextField
        if (isEditingName) {
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    TextButton(onClick = {
                        onRename(nameText)
                        isEditingName = false
                    }) {
                        Text("OK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shape.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${shape.type.name} • (${shape.width.toInt()}x${shape.height.toInt()}px)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Actions: Rename, Visibility, Lock, Up, Down, Delete
        IconButton(onClick = { isEditingName = !isEditingName }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.Gray, modifier = Modifier.size(16.dp))
        }

        IconButton(onClick = onToggleVisibility, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = if (shape.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Visibility",
                tint = if (shape.isVisible) Color(0xFF10B981) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(onClick = onToggleLock, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = if (shape.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock",
                tint = if (shape.isLocked) Color(0xFFF59E0B) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(14.dp))
        }

        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(14.dp))
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E), modifier = Modifier.size(14.dp))
        }
    }
}

private fun moveLayer(
    sortedList: List<ShapeObject>,
    fromIndex: Int,
    toIndex: Int,
    onReorder: (List<ShapeObject>) -> Unit
) {
    if (fromIndex < 0 || fromIndex >= sortedList.size || toIndex < 0 || toIndex >= sortedList.size) return

    val mutable = sortedList.toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)

    // Re-assign zIndex values: top item gets highest zIndex
    val total = mutable.size
    val updated = mutable.mapIndexed { index, shape ->
        shape.copy(zIndex = total - index)
    }
    onReorder(updated)
}
