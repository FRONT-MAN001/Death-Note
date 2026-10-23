package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Rect
import com.example.model.BrushType
import com.example.model.CanvasBackground
import com.example.model.DrawProject
import com.example.model.ShapeType
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object ThumbnailGenerator {

    /**
     * Generates a 512x512 preview thumbnail bitmap of the project for saving into the .idraw bundle.
     */
    fun generateThumbnail(project: DrawProject, width: Int = 512, height: Int = 512): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background
        val bgColor = when (project.background) {
            CanvasBackground.WHITE -> android.graphics.Color.WHITE
            CanvasBackground.DARK -> android.graphics.Color.rgb(20, 21, 27)
            CanvasBackground.WARM_PAPER -> android.graphics.Color.rgb(251, 248, 238)
            CanvasBackground.DARK_GRAPH -> android.graphics.Color.rgb(24, 27, 38)
            CanvasBackground.DOT_GRID, CanvasBackground.LINE_GRID -> android.graphics.Color.WHITE
        }
        canvas.drawColor(bgColor)

        val bounds = project.computeOverallBounds()
        val contentW = max(bounds.width, 100f)
        val contentH = max(bounds.height, 100f)

        val pad = 40f
        val paddedW = contentW + pad * 2f
        val paddedH = contentH + pad * 2f

        val scale = min(width / paddedW, height / paddedH)
        val cx = bounds.left + bounds.width / 2f
        val cy = bounds.top + bounds.height / 2f

        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(scale, scale)
        canvas.translate(-cx, -cy)

        // 2. Images
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        for (img in project.images) {
            val bmp = img.bitmap
            if (bmp != null && !bmp.isRecycled) {
                imagePaint.alpha = (img.opacity * 255).toInt().coerceIn(0, 255)
                canvas.save()
                canvas.translate(img.x + img.width / 2f, img.y + img.height / 2f)
                canvas.rotate(img.rotation)
                val src = android.graphics.Rect(0, 0, bmp.width, bmp.height)
                val dst = android.graphics.RectF(-img.width / 2f, -img.height / 2f, img.width / 2f, img.height / 2f)
                canvas.drawBitmap(bmp, src, dst, imagePaint)
                canvas.restore()
            }
        }

        // 3. Shapes
        val shapeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val shapeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (shape in project.shapes) {
            val left = shape.left
            val top = shape.top
            val right = shape.right
            val bottom = shape.bottom
            val sc = shape.strokeColor.toInt()
            val fc = shape.fillColor.toInt()

            shapeStrokePaint.strokeWidth = shape.strokeWidth
            shapeStrokePaint.color = sc
            shapeFillPaint.color = fc

            canvas.save()
            if (shape.rotation != 0f) {
                canvas.rotate(shape.rotation, (left + right) / 2f, (top + bottom) / 2f)
            }
            when (shape.type) {
                ShapeType.RECTANGLE -> {
                    if (shape.isFilled) canvas.drawRect(left, top, right, bottom, shapeFillPaint)
                    canvas.drawRect(left, top, right, bottom, shapeStrokePaint)
                }
                ShapeType.ROUNDED_RECTANGLE -> {
                    val cr = min(shape.width, shape.height) * 0.15f
                    if (shape.isFilled) canvas.drawRoundRect(left, top, right, bottom, cr, cr, shapeFillPaint)
                    canvas.drawRoundRect(left, top, right, bottom, cr, cr, shapeStrokePaint)
                }
                ShapeType.CIRCLE, ShapeType.ELLIPSE -> {
                    if (shape.isFilled) canvas.drawOval(left, top, right, bottom, shapeFillPaint)
                    canvas.drawOval(left, top, right, bottom, shapeStrokePaint)
                }
                ShapeType.LINE, ShapeType.ARROW -> {
                    canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, shapeStrokePaint)
                }
                ShapeType.TRIANGLE -> {
                    val p = Path().apply {
                        moveTo((left + right) / 2f, top)
                        lineTo(right, bottom)
                        lineTo(left, bottom)
                        close()
                    }
                    if (shape.isFilled) canvas.drawPath(p, shapeFillPaint)
                    canvas.drawPath(p, shapeStrokePaint)
                }
                ShapeType.POLYGON, ShapeType.STAR -> {
                    val p = Path().apply {
                        val r = min(shape.width, shape.height) / 2f
                        val mX = (left + right) / 2f
                        val mY = (top + bottom) / 2f
                        moveTo(mX, mY - r)
                        lineTo(mX + r, mY)
                        lineTo(mX, mY + r)
                        lineTo(mX - r, mY)
                        close()
                    }
                    if (shape.isFilled) canvas.drawPath(p, shapeFillPaint)
                    canvas.drawPath(p, shapeStrokePaint)
                }
            }
            canvas.restore()
        }

        // 4. Strokes
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in project.strokes) {
            if (stroke.points.size < 2) continue
            if (stroke.brushType == BrushType.ERASER) continue

            strokePaint.strokeWidth = stroke.size
            strokePaint.color = stroke.color.toInt()

            val path = Path()
            val p0 = stroke.points[0]
            path.moveTo(p0.x, p0.y)
            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                path.quadTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
            }
            path.lineTo(stroke.points.last().x, stroke.points.last().y)
            canvas.drawPath(path, strokePaint)
        }

        canvas.restore()
        return bitmap
    }
}
