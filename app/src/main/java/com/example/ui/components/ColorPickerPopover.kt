package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerPopover(
    currentColor: Long,
    recentColors: List<Long>,
    onColorSelected: (Long) -> Unit,
    onHexDialogRequested: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Convert current ARGB to HSV
    val c = Color(currentColor)
    val hsv = remember(currentColor) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.argb(
                (c.alpha * 255).toInt(),
                (c.red * 255).toInt(),
                (c.green * 255).toInt(),
                (c.blue * 255).toInt()
            ),
            arr
        )
        arr
    }

    var hue by remember(currentColor) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(currentColor) { mutableFloatStateOf(hsv[1]) }
    var value by remember(currentColor) { mutableFloatStateOf(hsv[2]) }
    var alpha by remember(currentColor) { mutableFloatStateOf(c.alpha) }

    fun updateColor(h: Float, s: Float, v: Float, a: Float) {
        val colorInt = android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))
        val longColor = colorInt.toLong() and 0xFFFFFFFFL
        onColorSelected(longColor)
    }

    Box(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .glassCard(cornerRadius = 28.dp)
            .padding(20.dp)
            .testTag("color_picker_popover")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Preview and Hex tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(currentColor))
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Color Palette",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F172A)
                        )
                        val hexStr = String.format("#%08X", currentColor)
                        Text(
                            text = hexStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Custom Hex button (opens dialog without keyboard on canvas)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable(onClick = onHexDialogRequested)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tag, contentDescription = "Hex", modifier = Modifier.size(14.dp), tint = Color(0xFF475569))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hex", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Saturation / Value Gradient Area
            val huePureColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.White, huePureColor)
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black)
                        )
                    )
                    .pointerInput(hue) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val s = (offset.x / size.width).coerceIn(0f, 1f)
                                val v = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                saturation = s
                                value = v
                                updateColor(hue, s, v, alpha)
                            },
                            onDrag = { change, _ ->
                                val s = (change.position.x / size.width).coerceIn(0f, 1f)
                                val v = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                saturation = s
                                value = v
                                updateColor(hue, s, v, alpha)
                            }
                        )
                    }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hue Rainbow Slider
            Text(
                text = "Hue Spectrum",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF475569)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Red,
                                Color.Yellow,
                                Color.Green,
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta,
                                Color.Red
                            )
                        )
                    )
            ) {
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        updateColor(it, saturation, value, alpha)
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hue_slider")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alpha Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alpha / Opacity",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF475569)
                )
                Text(
                    text = "${(alpha * 100).roundToInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
            }
            Slider(
                value = alpha,
                onValueChange = {
                    alpha = it
                    updateColor(hue, saturation, value, it)
                },
                valueRange = 0.05f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF0284C7),
                    activeTrackColor = Color(0xFF38BDF8),
                    inactiveTrackColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Recent and Favorite Colors
            Text(
                text = "Palette & Recents",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentColors.forEach { col ->
                    val isCurrent = col == currentColor
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(col))
                            .border(
                                width = if (isCurrent) 2.5.dp else 1.dp,
                                color = if (isCurrent) Color(0xFF0284C7) else Color(0x33000000),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(col) }
                    )
                }
            }
        }
    }
}
