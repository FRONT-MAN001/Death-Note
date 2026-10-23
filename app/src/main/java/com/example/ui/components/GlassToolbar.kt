package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveTool
import com.example.model.BrushType

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn

@Composable
fun GlassToolbar(
    activeTool: ActiveTool,
    brushType: BrushType,
    brushColor: Long,
    brushSize: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onBrushClick: () -> Unit,
    onEraserClick: () -> Unit,
    onPanClick: () -> Unit,
    onColorClick: () -> Unit,
    onShapeClick: () -> Unit,
    onSelectClick: () -> Unit,
    onImageClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .widthIn(max = 560.dp)
            .glassCapsule(
                backgroundColor = Color.White.copy(alpha = 0.90f),
                borderColor = Color.White.copy(alpha = 0.75f),
                elevation = 14.dp,
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("glass_bottom_toolbar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            // 1. Pan / Infinite Hand Tool
            ToolbarIconButton(
                icon = Icons.Default.PanTool,
                label = "Pan Canvas",
                isActive = activeTool == ActiveTool.PAN,
                onClick = onPanClick,
                testTag = "toolbar_pan_button"
            )

            // Divider
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // 2. Brush Tool (Active / Popover)
            ToolbarIconButton(
                icon = Icons.Default.Brush,
                label = "Brush",
                isActive = activeTool == ActiveTool.BRUSH,
                badgeColor = Color(brushColor),
                onClick = onBrushClick,
                testTag = "toolbar_brush_button"
            )

            // 2. Eraser Tool
            ToolbarIconButton(
                icon = Icons.Default.AutoFixNormal,
                label = "Eraser",
                isActive = activeTool == ActiveTool.ERASER,
                onClick = onEraserClick,
                testTag = "toolbar_eraser_button"
            )

            // 3. Color Picker Button (With vibrant color preview circle)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onColorClick)
                    .testTag("toolbar_color_button"),
                contentAlignment = Alignment.Center
            ) {
                // Rainbow rim around selected color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
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
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(brushColor))
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // 4. Shape Tool
            ToolbarIconButton(
                icon = Icons.Default.Category,
                label = "Shapes",
                isActive = activeTool == ActiveTool.SHAPE,
                onClick = onShapeClick,
                testTag = "toolbar_shape_button"
            )

            // 5. Select / Transform Tool
            ToolbarIconButton(
                icon = Icons.Default.NearMe,
                label = "Select",
                isActive = activeTool == ActiveTool.SELECT,
                onClick = onSelectClick,
                testTag = "toolbar_select_button"
            )

            // 6. Image Tool
            ToolbarIconButton(
                icon = Icons.Default.AddPhotoAlternate,
                label = "Image",
                isActive = activeTool == ActiveTool.IMAGE,
                onClick = onImageClick,
                testTag = "toolbar_image_button"
            )

            // Divider
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // 7. Undo
            IconButton(
                onClick = onUndoClick,
                enabled = canUndo,
                modifier = Modifier.size(38.dp).testTag("toolbar_undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size(20.dp),
                    tint = if (canUndo) Color(0xFF1E293B) else Color(0xFFCBD5E1)
                )
            }

            // 8. Redo
            IconButton(
                onClick = onRedoClick,
                enabled = canRedo,
                modifier = Modifier.size(38.dp).testTag("toolbar_redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    modifier = Modifier.size(20.dp),
                    tint = if (canRedo) Color(0xFF1E293B) else Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    badgeColor: Color? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isActive) Color(0xFF0F172A) else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = if (isActive) Color.White else Color(0xFF475569)
        )

        if (badgeColor != null && !isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}
