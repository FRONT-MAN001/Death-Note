package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Supported brush types for Infinite Draw.
 */
enum class BrushType(val displayName: String) {
    PENCIL("Pencil"),
    PEN("Pen"),
    MARKER("Marker"),
    HIGHLIGHTER("Highlighter"),
    SOFT_BRUSH("Soft Brush"),
    HARD_BRUSH("Hard Brush"),
    AIRBRUSH("Airbrush"),
    INK_BRUSH("Ink Brush"),
    PIXEL_BRUSH("Pixel Brush"),
    ERASER("Eraser")
}

/**
 * Point in infinite canvas space with optional stylus pressure.
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

/**
 * Vector drawing stroke consisting of ordered points, styling, and precalculated bounds.
 */
data class Stroke(
    val id: String,
    val points: List<StrokePoint>,
    val brushType: BrushType,
    val size: Float,
    val color: Long, // ARGB 32-bit packed
    val opacity: Float = 1.0f,
    val bounds: Rect = calculateBounds(points, size)
) {
    fun composeColor(): Color = Color(color).copy(alpha = opacity)

    companion object {
        fun calculateBounds(points: List<StrokePoint>, size: Float): Rect {
            if (points.isEmpty()) return Rect.Zero
            var minX = points[0].x
            var maxX = points[0].x
            var minY = points[0].y
            var maxY = points[0].y
            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
            val pad = size * 1.5f + 4f
            return Rect(minX - pad, minY - pad, maxX + pad, maxY + pad)
        }
    }
}

/**
 * Geometric shape types.
 */
enum class ShapeType(val displayName: String) {
    LINE("Line"),
    ARROW("Arrow"),
    RECTANGLE("Rectangle"),
    ROUNDED_RECTANGLE("Rounded Rect"),
    CIRCLE("Circle"),
    ELLIPSE("Ellipse"),
    TRIANGLE("Triangle"),
    POLYGON("Hexagon"),
    STAR("Star")
}

/**
 * Shape object positioned in the infinite canvas coordinate space.
 */
data class ShapeObject(
    val id: String,
    val type: ShapeType,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val strokeWidth: Float,
    val strokeColor: Long,
    val fillColor: Long,
    val isFilled: Boolean,
    val opacity: Float = 1.0f,
    val rotation: Float = 0f,
    val isSelected: Boolean = false
) {
    val left: Float get() = min(startX, endX)
    val right: Float get() = max(startX, endX)
    val top: Float get() = min(startY, endY)
    val bottom: Float get() = max(startY, endY)
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun bounds(): Rect {
        val pad = strokeWidth + 8f
        return Rect(left - pad, top - pad, right + pad, bottom + pad)
    }
}

/**
 * Image object placed on the infinite canvas.
 */
data class ImageObject(
    val id: String,
    val fileName: String, // stored inside the .idraw package
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val opacity: Float = 1.0f,
    val isSelected: Boolean = false,
    @Transient val bitmap: android.graphics.Bitmap? = null
) {
    fun bounds(): Rect {
        val pad = 8f
        return Rect(x - pad, y - pad, x + width + pad, y + height + pad)
    }
}

/**
 * Canvas background paper/grid styles.
 */
enum class CanvasBackground(val displayName: String, val bgColor: Long) {
    WHITE("Clean White", 0xFFFFFFFF),
    DARK("Studio Dark", 0xFF14151B),
    WARM_PAPER("Warm Paper", 0xFFFBF8EE),
    DOT_GRID("Dot Grid", 0xFFFFFFFF),
    LINE_GRID("Square Grid", 0xFFFFFFFF),
    DARK_GRAPH("Dark Blueprint", 0xFF181B26)
}

/**
 * Current camera view / pan and zoom state.
 */
data class CanvasTransform(
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoom: Float = 1.0f
) {
    fun screenToCanvas(screenX: Float, screenY: Float): Offset {
        return Offset(
            (screenX - panX) / zoom,
            (screenY - panY) / zoom
        )
    }

    fun canvasToScreen(canvasX: Float, canvasY: Float): Offset {
        return Offset(
            canvasX * zoom + panX,
            canvasY * zoom + panY
        )
    }
}

/**
 * Selection tool state.
 */
enum class ActiveTool {
    BRUSH,
    ERASER,
    SHAPE,
    SELECT,
    IMAGE,
    PAN
}

/**
 * Summary metadata for displaying in file manager.
 */
data class ProjectMetadata(
    val id: String,
    val name: String,
    val uriString: String,
    val modifiedTime: Long,
    val sizeBytes: Long,
    val thumbnailBitmap: android.graphics.Bitmap? = null
)
