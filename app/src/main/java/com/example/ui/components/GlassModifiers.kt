package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium frosted-glass modifier styling with translucent surface,
 * subtle white glossy border, and soft diffuse shadow.
 */
fun Modifier.glassCapsule(
    backgroundColor: Color = Color.White.copy(alpha = 0.82f),
    borderColor: Color = Color.White.copy(alpha = 0.65f),
    elevation: Dp = 10.dp,
    shape: Shape = CircleShape
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.08f),
        spotColor = Color.Black.copy(alpha = 0.14f)
    )
    .clip(shape)
    .background(backgroundColor)
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                borderColor,
                borderColor.copy(alpha = 0.25f)
            )
        ),
        shape = shape
    )

fun Modifier.glassCard(
    backgroundColor: Color = Color.White.copy(alpha = 0.90f),
    borderColor: Color = Color.White.copy(alpha = 0.70f),
    elevation: Dp = 12.dp,
    cornerRadius: Dp = 24.dp
): Modifier = this.glassCapsule(
    backgroundColor = backgroundColor,
    borderColor = borderColor,
    elevation = elevation,
    shape = RoundedCornerShape(cornerRadius)
)
