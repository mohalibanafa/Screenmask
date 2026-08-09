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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShapeObject

@Composable
fun ColorPickerPanel(
    modifier: Modifier = Modifier,
    selectedShape: ShapeObject?,
    onColorChanged: (Long, Float) -> Unit,
    onBlackoutModeToggle: () -> Unit
) {
    var hexInput by remember(selectedShape?.colorArgb) {
        mutableStateOf(String.format("#%06X", (0xFFFFFFL and (selectedShape?.colorArgb ?: 0xFF000000L))))
    }
    var alphaValue by remember(selectedShape?.alpha) {
        mutableStateOf(selectedShape?.alpha ?: 1.0f)
    }

    val presetColors = listOf(
        Pair(0xFF000000L, "Pure Black"),
        Pair(0xFF1E293BL, "Dark Slate"),
        Pair(0xFF7F1D1DL, "Crimson Red"),
        Pair(0xFF1E3A8AL, "Deep Blue"),
        Pair(0xFF064E3BL, "Emerald Green"),
        Pair(0xFF78350FL, "Amber Gold"),
        Pair(0xFFFFFFFFL, "Pure White")
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
            .testTag("panel_color_picker")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Color & Opacity Picker",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )

                Button(
                    onClick = onBlackoutModeToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Blackout Mode (#0000)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Color Presets
            Text(text = "Quick Presets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(presetColors) { (argb, label) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = if (selectedShape?.colorArgb == argb) 2.dp else 1.dp,
                                color = if (selectedShape?.colorArgb == argb) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                hexInput = String.format("#%06X", (0xFFFFFFL and argb))
                                onColorChanged(argb, alphaValue)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb.toInt()))
                                    .border(1.dp, Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual HEX Color Entry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { text ->
                        hexInput = text
                        try {
                            val clean = text.replace("#", "").trim()
                            if (clean.length == 6) {
                                val parsed = clean.toLong(16) or 0xFF000000L
                                onColorChanged(parsed, alphaValue)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    label = { Text("Manual HEX Code") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Preview Color Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            try {
                                val clean = hexInput.replace("#", "").trim()
                                val parsed = clean.toLong(16) or 0xFF000000L
                                Color(parsed.toInt()).copy(alpha = alphaValue)
                            } catch (e: Exception) {
                                Color.Black
                            }
                        )
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Opacity Alpha Slider
            Text(
                text = "Opacity / Alpha: ${(alphaValue * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = alphaValue,
                onValueChange = {
                    alphaValue = it
                    try {
                        val clean = hexInput.replace("#", "").trim()
                        val parsed = if (clean.length == 6) clean.toLong(16) or 0xFF000000L else 0xFF000000L
                        onColorChanged(parsed, alphaValue)
                    } catch (e: Exception) {
                        onColorChanged(0xFF000000L, alphaValue)
                    }
                },
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
