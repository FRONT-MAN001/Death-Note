package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.foundation.layout.widthIn

@Composable
fun TopNavigationBar(
    projectName: String,
    folderName: String,
    hasUnsavedChanges: Boolean,
    zoomPercentage: Float,
    onMenuClick: () -> Unit,
    onQuickSaveClick: () -> Unit,
    onResetZoomClick: () -> Unit,
    onZoomToFitClick: () -> Unit,
    onRecenterClick: () -> Unit = onResetZoomClick,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Floating Capsule: Menu & Project Info
        Box(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .glassCapsule(
                    backgroundColor = Color.White.copy(alpha = 0.88f),
                    borderColor = Color.White.copy(alpha = 0.70f),
                    elevation = 10.dp
                )
                .clickable(onClick = onMenuClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("top_menu_capsule")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu Drawer",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(22.dp)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasUnsavedChanges) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF97316))
                            )
                        }
                    }

                    Text(
                        text = "📁 $folderName",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right Floating Capsule: Zoom, Recenter & Quick Save
        Box(
            modifier = Modifier
                .glassCapsule(
                    backgroundColor = Color.White.copy(alpha = 0.88f),
                    borderColor = Color.White.copy(alpha = 0.70f),
                    elevation = 10.dp
                )
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("top_actions_capsule")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Zoom Percentage Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onResetZoomClick)
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                        .testTag("zoom_indicator_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(zoomPercentage * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                // Recenter Canvas Origin (0,0) Button
                IconButton(
                    onClick = onRecenterClick,
                    modifier = Modifier.size(32.dp).testTag("recenter_origin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterCenterFocus,
                        contentDescription = "Center Canvas (0, 0)",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF475569)
                    )
                }

                // Zoom to Fit Button
                IconButton(
                    onClick = onZoomToFitClick,
                    modifier = Modifier.size(32.dp).testTag("zoom_to_fit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = "Zoom to Fit",
                        modifier = Modifier.size(17.dp),
                        tint = Color(0xFF475569)
                    )
                }

                // Quick Save
                IconButton(
                    onClick = onQuickSaveClick,
                    modifier = Modifier.size(32.dp).testTag("quick_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Drawing",
                        modifier = Modifier.size(18.dp),
                        tint = if (hasUnsavedChanges) Color(0xFF0284C7) else Color(0xFF475569)
                    )
                }
            }
        }
    }
}
