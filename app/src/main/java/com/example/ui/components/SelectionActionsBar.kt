package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
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

@Composable
fun SelectionActionsBar(
    onScaleUp: () -> Unit,
    onScaleDown: () -> Unit,
    onRotateCw: () -> Unit,
    onRotateCcw: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onDeselect: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onMoveLeft: () -> Unit = {},
    onMoveRight: () -> Unit = {},
    selectedImageIndex: Int = -1,
    totalImages: Int = 0,
    onNextImage: (() -> Unit)? = null,
    onPrevImage: (() -> Unit)? = null,
    onOpenImageManager: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassCapsule(
                backgroundColor = Color(0xFF0F172A).copy(alpha = 0.95f),
                borderColor = Color.White.copy(alpha = 0.35f),
                elevation = 14.dp,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("selection_actions_bar")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: Directional Nudge Buttons (Up, Down, Left, Right) + Image Switcher / Layers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Move:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )

                NudgeArrowButton(
                    icon = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Move Left",
                    onClick = onMoveLeft,
                    testTag = "nudge_left_button"
                )
                NudgeArrowButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    onClick = onMoveUp,
                    testTag = "nudge_up_button"
                )
                NudgeArrowButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    onClick = onMoveDown,
                    testTag = "nudge_down_button"
                )
                NudgeArrowButton(
                    icon = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Move Right",
                    onClick = onMoveRight,
                    testTag = "nudge_right_button"
                )

                // If images exist and an image is selected, show image switcher
                if (selectedImageIndex >= 0 && totalImages > 1 && onNextImage != null && onPrevImage != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 1.dp, height = 18.dp)
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.35f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onPrevImage)
                                .testTag("prev_image_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous Image",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "Img ${selectedImageIndex + 1}/$totalImages",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onNextImage)
                                .testTag("next_image_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next Image",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (totalImages > 0 && onOpenImageManager != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable(onClick = onOpenImageManager)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("manage_images_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "All Images",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Images ($totalImages)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Row 2: Transform actions (Scale +, Scale -, Rotate, Duplicate, Delete, Close)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                SelectionButton(icon = Icons.Default.ZoomIn, label = "Scale +", onClick = onScaleUp)
                SelectionButton(icon = Icons.Default.ZoomOut, label = "Scale -", onClick = onScaleDown)
                SelectionButton(icon = Icons.Default.RotateRight, label = "Rotate", onClick = onRotateCw)
                SelectionButton(icon = Icons.Default.ContentCopy, label = "Duplicate", onClick = onDuplicate)
                SelectionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    tint = Color(0xFFFCA5A5),
                    backgroundColor = Color(0xFFDC2626).copy(alpha = 0.35f),
                    onClick = onDelete
                )

                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = onDeselect)
                        .testTag("deselect_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Deselect",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NudgeArrowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SelectionButton(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    backgroundColor: Color = Color.White.copy(alpha = 0.12f),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp),
                tint = tint
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
