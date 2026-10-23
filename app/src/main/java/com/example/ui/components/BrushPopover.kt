package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrushType
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrushPopover(
    selectedBrush: BrushType,
    brushSize: Float,
    brushOpacity: Float,
    brushColor: Long,
    onBrushSelected: (BrushType) -> Unit,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .glassCard(cornerRadius = 28.dp)
            .padding(20.dp)
            .testTag("brush_popover")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Live Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Brushes & Pens",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // Live stroke preview dot
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewRadius = (brushSize / 2f).coerceIn(2f, 16f)
                        Box(
                            modifier = Modifier
                                .size((previewRadius * 2).dp)
                                .clip(CircleShape)
                                .background(Color(brushColor).copy(alpha = brushOpacity))
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("close_brush_popover")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Brush Selection Chips (Grid/Flow)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrushType.values().forEach { brush ->
                    val isSelected = brush == selectedBrush
                    BrushChip(
                        brush = brush,
                        isSelected = isSelected,
                        onClick = { onBrushSelected(brush) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Size Control Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stroke Size",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF334155)
                )
                Text(
                    text = "${brushSize.roundToInt()} px",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0284C7)
                )
            }

            Slider(
                value = brushSize,
                onValueChange = onSizeChange,
                valueRange = 1f..120f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF0284C7),
                    activeTrackColor = Color(0xFF38BDF8),
                    inactiveTrackColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth().testTag("brush_size_slider")
            )

            // Quick Preset Sizes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val presets = listOf(2f, 6f, 12f, 24f, 48f, 80f)
                presets.forEach { preset ->
                    val isPreset = (brushSize - preset).let { it >= -1f && it <= 1f }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPreset) Color(0xFF0284C7) else Color(0xFFF1F5F9))
                            .clickable { onSizeChange(preset) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${preset.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPreset) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Opacity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Opacity",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF334155)
                )
                Text(
                    text = "${(brushOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0284C7)
                )
            }

            Slider(
                value = brushOpacity,
                onValueChange = onOpacityChange,
                valueRange = 0.05f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF0284C7),
                    activeTrackColor = Color(0xFF38BDF8),
                    inactiveTrackColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth().testTag("brush_opacity_slider")
            )
        }
    }
}

@Composable
private fun BrushChip(
    brush: BrushType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (brush) {
        BrushType.PENCIL -> Icons.Default.Edit
        BrushType.PEN -> Icons.Default.Create
        BrushType.MARKER -> Icons.Default.FormatPaint
        BrushType.HIGHLIGHTER -> Icons.Default.Highlight
        BrushType.SOFT_BRUSH -> Icons.Default.Brush
        BrushType.HARD_BRUSH -> Icons.Default.ModeEdit
        BrushType.AIRBRUSH -> Icons.Default.AutoFixHigh
        BrushType.INK_BRUSH -> Icons.Default.Brush
        BrushType.PIXEL_BRUSH -> Icons.Default.Grain
        BrushType.ERASER -> Icons.Default.Close
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = brush.displayName,
            modifier = Modifier.size(16.dp),
            tint = if (isSelected) Color.White else Color(0xFF475569)
        )
        Text(
            text = brush.displayName,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF1E293B)
        )
    }
}
