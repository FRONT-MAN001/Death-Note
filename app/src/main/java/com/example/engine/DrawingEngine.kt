package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.model.ActiveTool
import com.example.model.BrushType
import com.example.model.ImageObject
import com.example.model.ShapeObject
import com.example.model.Stroke
import com.example.model.StrokePoint
import java.util.UUID

/**
 * Undoable command interface.
 */
sealed interface DrawCommand {
    data class AddStroke(val stroke: Stroke) : DrawCommand
    data class AddShape(val shape: ShapeObject) : DrawCommand
    data class AddImage(val image: ImageObject, val imageBytes: ByteArray) : DrawCommand
    data class DeleteObjects(
        val strokes: List<Stroke>,
        val shapes: List<ShapeObject>,
        val images: List<ImageObject>
    ) : DrawCommand
    data class ModifyShape(val oldShape: ShapeObject, val newShape: ShapeObject) : DrawCommand
    data class ModifyImage(val oldImage: ImageObject, val newImage: ImageObject) : DrawCommand
    data class ClearCanvas(
        val previousStrokes: List<Stroke>,
        val previousShapes: List<ShapeObject>,
        val previousImages: List<ImageObject>
    ) : DrawCommand
}

/**
 * Drawing engine supporting smooth vector curves, spatial culling, selection hit testing,
 * and high-performance rendering.
 */
object DrawingEngine {

    /**
     * Builds a smooth Android/Compose Path from ordered stroke points using quadratic Bezier curve interpolation.
     */
    fun createSmoothPath(points: List<StrokePoint>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        if (points.size == 1) {
            val p = points[0]
            path.moveTo(p.x, p.y)
            path.lineTo(p.x + 0.1f, p.y + 0.1f)
            return path
        }

        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadraticTo(prev.x, prev.y, midX, midY)
        }
        val last = points.last()
        path.lineTo(last.x, last.y)
        return path
    }

    /**
     * Hit tests shapes in canvas coordinates (topmost first).
     */
    fun hitTestShape(shapes: List<ShapeObject>, canvasPoint: Offset): ShapeObject? {
        for (shape in shapes.reversed()) {
            val b = shape.bounds()
            if (canvasPoint.x in b.left..b.right && canvasPoint.y in b.top..b.bottom) {
                return shape
            }
        }
        return null
    }

    /**
     * Hit tests images in canvas coordinates (topmost first).
     */
    fun hitTestImage(images: List<ImageObject>, canvasPoint: Offset): ImageObject? {
        for (img in images.reversed()) {
            val b = img.bounds()
            if (canvasPoint.x in b.left..b.right && canvasPoint.y in b.top..b.bottom) {
                return img
            }
        }
        return null
    }

    /**
     * Hit tests strokes within a distance threshold.
     */
    fun hitTestStroke(strokes: List<Stroke>, canvasPoint: Offset): Stroke? {
        for (stroke in strokes.reversed()) {
            val b = stroke.bounds
            if (canvasPoint.x in b.left..b.right && canvasPoint.y in b.top..b.bottom) {
                // Approximate check against points
                val threshold = (stroke.size + 16f) * (stroke.size + 16f)
                for (p in stroke.points) {
                    val dx = p.x - canvasPoint.x
                    val dy = p.y - canvasPoint.y
                    if (dx * dx + dy * dy <= threshold) {
                        return stroke
                    }
                }
            }
        }
        return null
    }

    /**
     * Checks if an element's bounding box intersects the visible canvas bounds (for frustum culling).
     */
    fun intersectsViewport(elementBounds: Rect, viewport: Rect): Boolean {
        return elementBounds.left <= viewport.right &&
                elementBounds.right >= viewport.left &&
                elementBounds.top <= viewport.bottom &&
                elementBounds.bottom >= viewport.top
    }
}
