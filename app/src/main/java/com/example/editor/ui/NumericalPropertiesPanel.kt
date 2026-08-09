package com.example.editor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShapeObject
import com.example.data.model.ShapeType

@Composable
fun NumericalPropertiesPanel(
    modifier: Modifier = Modifier,
    shape: ShapeObject?,
    canvasWidth: Float = 1080f,
    canvasHeight: Float = 2400f,
    onUpdateShape: (ShapeObject) -> Unit
) {
    if (shape == null) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No Shape Selected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap any shape on the virtual canvas or layer list to modify its numerical properties.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
            .testTag("panel_numerical_properties")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = shape.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Origin (0,0) Top-Left • Unit: Pixels",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Lock Ratio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = shape.aspectRatioLocked,
                        onCheckedChange = { onUpdateShape(shape.copy(aspectRatioLocked = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // X Position Controls
            NumericSettingRow(
                label = "X Position",
                unit = "px (${((shape.x / canvasWidth) * 100).toInt()}%)",
                value = shape.x,
                range = 0f..canvasWidth,
                onValueChange = { newX ->
                    onUpdateShape(shape.copy(x = newX))
                }
            )

            // Y Position Controls
            NumericSettingRow(
                label = "Y Position",
                unit = "px (${((shape.y / canvasHeight) * 100).toInt()}%)",
                value = shape.y,
                range = 0f..canvasHeight,
                onValueChange = { newY ->
                    onUpdateShape(shape.copy(y = newY))
                }
            )

            // Width Controls
            NumericSettingRow(
                label = "Width",
                unit = "px (${((shape.width / canvasWidth) * 100).toInt()}%)",
                value = shape.width,
                range = 20f..canvasWidth,
                onValueChange = { newW ->
                    if (shape.aspectRatioLocked && shape.width > 0f) {
                        val ratio = shape.height / shape.width
                        val newH = newW * ratio
                        onUpdateShape(shape.copy(width = newW, height = newH))
                    } else {
                        onUpdateShape(shape.copy(width = newW))
                    }
                }
            )

            // Height Controls
            NumericSettingRow(
                label = "Height",
                unit = "px (${((shape.height / canvasHeight) * 100).toInt()}%)",
                value = shape.height,
                range = 20f..canvasHeight,
                onValueChange = { newH ->
                    if (shape.aspectRatioLocked && shape.height > 0f) {
                        val ratio = shape.width / shape.height
                        val newW = newH * ratio
                        onUpdateShape(shape.copy(height = newH, width = newW))
                    } else {
                        onUpdateShape(shape.copy(height = newH))
                    }
                }
            )

            // Rotation Controls
            NumericSettingRow(
                label = "Rotation",
                unit = "° (0..360°)",
                value = shape.rotation,
                range = 0f..360f,
                onValueChange = { newRot ->
                    onUpdateShape(shape.copy(rotation = (newRot % 360f + 360f) % 360f))
                }
            )

            // Opacity / Alpha
            NumericSettingRow(
                label = "Opacity (Alpha)",
                unit = "% (${(shape.alpha * 100).toInt()}%)",
                value = shape.alpha * 100f,
                range = 0f..100f,
                onValueChange = { newAlpha ->
                    onUpdateShape(shape.copy(alpha = newAlpha / 100f))
                }
            )

            // Corner Radius (if rounded rect)
            if (shape.type == ShapeType.ROUNDED_RECT) {
                NumericSettingRow(
                    label = "Corner Radius",
                    unit = "px",
                    value = shape.cornerRadius,
                    range = 0f..200f,
                    onValueChange = { newR ->
                        onUpdateShape(shape.copy(cornerRadius = newR))
                    }
                )
            }
        }
    }
}

@Composable
fun NumericSettingRow(
    label: String,
    unit: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toInt().toString()) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: $unit",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                    input.toFloatOrNull()?.let { num ->
                        onValueChange(num.coerceIn(range.start, range.endInclusive))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .width(80.dp)
                    .height(42.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = {
                textValue = it.toInt().toString()
                onValueChange(it)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
