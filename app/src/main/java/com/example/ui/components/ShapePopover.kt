package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShapeType
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShapePopover(
    selectedShapeType: ShapeType,
    strokeWidth: Float,
    isFilled: Boolean,
    strokeColor: Long,
    fillColor: Long,
    onShapeTypeSelected: (ShapeType) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onFillToggled: (Boolean) -> Unit,
    onStrokeColorSelected: (Long) -> Unit,
    onFillColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .glassCard(cornerRadius = 28.dp)
            .padding(20.dp)
            .testTag("shape_popover")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Geometric Shapes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shapes Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShapeType.values().forEach { type ->
                    val isSelected = type == selectedShapeType
                    val icon = when (type) {
                        ShapeType.LINE -> Icons.Default.LinearScale
                        ShapeType.ARROW -> Icons.AutoMirrored.Filled.ArrowForward
                        ShapeType.RECTANGLE -> Icons.Default.CropSquare
                        ShapeType.ROUNDED_RECTANGLE -> Icons.Default.CropLandscape
                        ShapeType.CIRCLE, ShapeType.ELLIPSE -> Icons.Default.Circle
                        ShapeType.TRIANGLE -> Icons.Default.ChangeHistory
                        ShapeType.POLYGON -> Icons.Default.Hexagon
                        ShapeType.STAR -> Icons.Default.Star
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                            .clickable { onShapeTypeSelected(type) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = type.displayName,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else Color(0xFF475569)
                        )
                        Text(
                            text = type.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF1E293B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stroke Width
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stroke Width",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF334155)
                )
                Text(
                    text = "${strokeWidth.roundToInt()} px",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0284C7)
                )
            }
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 1f..40f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF0284C7),
                    activeTrackColor = Color(0xFF38BDF8),
                    inactiveTrackColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fill Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fill Shape Interior",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = if (isFilled) "Shape is filled" else "Wireframe outline only",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                Switch(
                    checked = isFilled,
                    onCheckedChange = onFillToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Color swatches for shape
            Text(
                text = "Shape Palette",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(6.dp))

            val quickPalette = listOf(
                0xFF0F172A.toLong(),
                0xFF3B82F6.toLong(),
                0xFF10B981.toLong(),
                0xFFEF4444.toLong(),
                0xFFF59E0B.toLong(),
                0xFF8B5CF6.toLong(),
                0xFF06B6D4.toLong(),
                0xFFFFFFFF.toLong()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPalette.forEach { col ->
                    val isCurrent = col == strokeColor
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
                            .clickable {
                                onStrokeColorSelected(col)
                                onFillColorSelected(col and 0x55FFFFFFL)
                            }
                    )
                }
            }
        }
    }
}
