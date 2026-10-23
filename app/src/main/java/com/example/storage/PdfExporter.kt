package com.example.storage

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.geometry.Rect
import com.example.model.BrushType
import com.example.model.CanvasBackground
import com.example.model.DrawProject
import com.example.model.ShapeType
import java.io.OutputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object PdfExporter {

    enum class ExportScope {
        ENTIRE_DRAWING,
        CURRENT_VIEW
    }

    /**
     * Exports a DrawProject to PDF format.
     */
    fun exportToPdf(
        project: DrawProject,
        outputStream: OutputStream,
        scope: ExportScope = ExportScope.ENTIRE_DRAWING,
        viewWidth: Float = 1080f,
        viewHeight: Float = 1920f
    ) {
        val pdfDocument = PdfDocument()

        val bounds: Rect = when (scope) {
            ExportScope.ENTIRE_DRAWING -> {
                val overall = project.computeOverallBounds()
                val pad = 40f
                Rect(overall.left - pad, overall.top - pad, overall.right + pad, overall.bottom + pad)
            }
            ExportScope.CURRENT_VIEW -> {
                val topLeft = project.transform.screenToCanvas(0f, 0f)
                val bottomRight = project.transform.screenToCanvas(viewWidth, viewHeight)
                Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
            }
        }

        val contentWidth = max(200f, bounds.width)
        val contentHeight = max(200f, bounds.height)

        // PDF coordinate system (points). Let's target a max page dimension of ~1200 points to keep files compact & crisp
        val maxDim = 1200f
        val scale = min(maxDim / contentWidth, maxDim / contentHeight).coerceIn(0.2f, 2.0f)
        val pageWidth = (contentWidth * scale).toInt().coerceAtLeast(100)
        val pageHeight = (contentHeight * scale).toInt().coerceAtLeast(100)

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // 1. Draw background
        val bgPaint = Paint().apply {
            color = when (project.background) {
                CanvasBackground.WHITE -> android.graphics.Color.WHITE
                CanvasBackground.DARK -> android.graphics.Color.rgb(20, 21, 27)
                CanvasBackground.WARM_PAPER -> android.graphics.Color.rgb(251, 248, 238)
                CanvasBackground.DARK_GRAPH -> android.graphics.Color.rgb(24, 27, 38)
                CanvasBackground.DOT_GRID, CanvasBackground.LINE_GRID -> android.graphics.Color.WHITE
            }
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Setup translation & scaling from canvas space to PDF page space
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(-bounds.left, -bounds.top)

        // 2. Draw Images
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        for (img in project.images) {
            val bmp = img.bitmap
            if (bmp != null && !bmp.isRecycled) {
                imagePaint.alpha = (img.opacity * 255).toInt().coerceIn(0, 255)
                canvas.save()
                canvas.translate(img.x + img.width / 2f, img.y + img.height / 2f)
                canvas.rotate(img.rotation)
                val srcRect = android.graphics.Rect(0, 0, bmp.width, bmp.height)
                val dstRect = android.graphics.RectF(-img.width / 2f, -img.height / 2f, img.width / 2f, img.height / 2f)
                canvas.drawBitmap(bmp, srcRect, dstRect, imagePaint)
                canvas.restore()
            }
        }

        // 3. Draw Shapes
        val shapeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val shapeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (shape in project.shapes) {
            val shapeOpacity = shape.opacity.coerceIn(0f, 1f)
            val strokeCol = shape.strokeColor.toInt()
            val fillCol = shape.fillColor.toInt()

            shapeStrokePaint.strokeWidth = shape.strokeWidth
            shapeStrokePaint.color = strokeCol
            shapeStrokePaint.alpha = ((android.graphics.Color.alpha(strokeCol) / 255f) * shapeOpacity * 255).toInt().coerceIn(0, 255)

            shapeFillPaint.color = fillCol
            shapeFillPaint.alpha = ((android.graphics.Color.alpha(fillCol) / 255f) * shapeOpacity * 255).toInt().coerceIn(0, 255)

            val left = shape.left
            val top = shape.top
            val right = shape.right
            val bottom = shape.bottom
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f

            canvas.save()
            if (shape.rotation != 0f) {
                canvas.rotate(shape.rotation, cx, cy)
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
                ShapeType.LINE -> {
                    canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, shapeStrokePaint)
                }
                ShapeType.ARROW -> {
                    canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, shapeStrokePaint)
                    // Draw arrowhead
                    val angle = Math.atan2((shape.endY - shape.startY).toDouble(), (shape.endX - shape.startX).toDouble())
                    val headLen = (shape.strokeWidth * 3.5f).coerceAtLeast(16f)
                    val arrowAngle = Math.PI / 6
                    val arrowPath = Path().apply {
                        moveTo(shape.endX, shape.endY)
                        lineTo(
                            (shape.endX - headLen * cos(angle - arrowAngle)).toFloat(),
                            (shape.endY - headLen * sin(angle - arrowAngle)).toFloat()
                        )
                        moveTo(shape.endX, shape.endY)
                        lineTo(
                            (shape.endX - headLen * cos(angle + arrowAngle)).toFloat(),
                            (shape.endY - headLen * sin(angle + arrowAngle)).toFloat()
                        )
                    }
                    canvas.drawPath(arrowPath, shapeStrokePaint)
                }
                ShapeType.TRIANGLE -> {
                    val triPath = Path().apply {
                        moveTo(cx, top)
                        lineTo(right, bottom)
                        lineTo(left, bottom)
                        close()
                    }
                    if (shape.isFilled) canvas.drawPath(triPath, shapeFillPaint)
                    canvas.drawPath(triPath, shapeStrokePaint)
                }
                ShapeType.POLYGON -> {
                    val polyPath = Path()
                    val radius = min(shape.width, shape.height) / 2f
                    for (i in 0 until 6) {
                        val a = (i * 60 - 30) * Math.PI / 180.0
                        val px = (cx + radius * cos(a)).toFloat()
                        val py = (cy + radius * sin(a)).toFloat()
                        if (i == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
                    }
                    polyPath.close()
                    if (shape.isFilled) canvas.drawPath(polyPath, shapeFillPaint)
                    canvas.drawPath(polyPath, shapeStrokePaint)
                }
                ShapeType.STAR -> {
                    val starPath = Path()
                    val outerR = min(shape.width, shape.height) / 2f
                    val innerR = outerR * 0.45f
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outerR else innerR
                        val a = (i * 36 - 90) * Math.PI / 180.0
                        val px = (cx + r * cos(a)).toFloat()
                        val py = (cy + r * sin(a)).toFloat()
                        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                    }
                    starPath.close()
                    if (shape.isFilled) canvas.drawPath(starPath, shapeFillPaint)
                    canvas.drawPath(starPath, shapeStrokePaint)
                }
            }
            canvas.restore()
        }

        // 4. Draw Strokes
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in project.strokes) {
            if (stroke.points.size < 2) continue
            val col = stroke.color.toInt()
            val strokeAlpha = stroke.opacity.coerceIn(0f, 1f)

            when (stroke.brushType) {
                BrushType.ERASER -> continue // Skip eraser in PDF export or draw background color
                BrushType.HIGHLIGHTER -> {
                    strokePaint.strokeCap = Paint.Cap.SQUARE
                    strokePaint.strokeWidth = stroke.size * 1.8f
                    strokePaint.color = col
                    strokePaint.alpha = ((strokeAlpha * 0.35f) * 255).toInt().coerceIn(0, 255)
                }
                BrushType.PENCIL -> {
                    strokePaint.strokeCap = Paint.Cap.ROUND
                    strokePaint.strokeWidth = stroke.size * 0.8f
                    strokePaint.color = col
                    strokePaint.alpha = ((strokeAlpha * 0.8f) * 255).toInt().coerceIn(0, 255)
                }
                BrushType.AIRBRUSH -> {
                    strokePaint.strokeCap = Paint.Cap.ROUND
                    strokePaint.strokeWidth = stroke.size * 1.5f
                    strokePaint.color = col
                    strokePaint.alpha = ((strokeAlpha * 0.25f) * 255).toInt().coerceIn(0, 255)
                }
                BrushType.MARKER -> {
                    strokePaint.strokeCap = Paint.Cap.SQUARE
                    strokePaint.strokeWidth = stroke.size * 1.3f
                    strokePaint.color = col
                    strokePaint.alpha = ((strokeAlpha * 0.9f) * 255).toInt().coerceIn(0, 255)
                }
                else -> {
                    strokePaint.strokeCap = Paint.Cap.ROUND
                    strokePaint.strokeWidth = stroke.size
                    strokePaint.color = col
                    strokePaint.alpha = ((android.graphics.Color.alpha(col) / 255f) * strokeAlpha * 255).toInt().coerceIn(0, 255)
                }
            }

            val strokePath = Path()
            val p0 = stroke.points[0]
            strokePath.moveTo(p0.x, p0.y)

            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                strokePath.quadTo(prev.x, prev.y, midX, midY)
            }
            val last = stroke.points.last()
            strokePath.lineTo(last.x, last.y)

            canvas.drawPath(strokePath, strokePaint)
        }

        canvas.restore()
        pdfDocument.finishPage(page)

        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
