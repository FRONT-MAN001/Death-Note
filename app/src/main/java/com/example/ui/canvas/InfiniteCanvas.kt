package com.example.ui.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as ComposeStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.engine.DrawingEngine
import com.example.model.ActiveTool
import com.example.model.BrushType
import com.example.model.CanvasBackground
import com.example.model.CanvasTransform
import com.example.model.DrawProject
import com.example.model.ImageObject
import com.example.model.ShapeObject
import com.example.model.ShapeType
import com.example.model.Stroke
import com.example.model.StrokePoint
import java.util.UUID
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun InfiniteCanvas(
    project: DrawProject,
    transform: CanvasTransform,
    activeTool: ActiveTool,
    brushType: BrushType,
    brushSize: Float,
    brushColor: Long,
    brushOpacity: Float,
    selectedShapeType: ShapeType,
    shapeStrokeWidth: Float,
    shapeStrokeColor: Long,
    shapeFillColor: Long,
    shapeIsFilled: Boolean,
    shapeOpacity: Float,
    selectedShape: ShapeObject?,
    selectedImage: ImageObject?,
    onTransformChange: (panX: Float, panY: Float, zoom: Float) -> Unit,
    onStrokeCommit: (Stroke) -> Unit,
    onShapeCommit: (ShapeObject) -> Unit,
    onObjectSelect: (Offset) -> Unit,
    onSelectedMove: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep updated references so gesture events inside pointerInput always use the latest state
    val currentProject by rememberUpdatedState(project)
    val currentTransform by rememberUpdatedState(transform)
    val currentActiveTool by rememberUpdatedState(activeTool)
    val currentBrushType by rememberUpdatedState(brushType)
    val currentBrushSize by rememberUpdatedState(brushSize)
    val currentBrushColor by rememberUpdatedState(brushColor)
    val currentBrushOpacity by rememberUpdatedState(brushOpacity)
    val currentSelectedShapeType by rememberUpdatedState(selectedShapeType)
    val currentShapeStrokeWidth by rememberUpdatedState(shapeStrokeWidth)
    val currentShapeStrokeColor by rememberUpdatedState(shapeStrokeColor)
    val currentShapeFillColor by rememberUpdatedState(shapeFillColor)
    val currentShapeIsFilled by rememberUpdatedState(shapeIsFilled)
    val currentShapeOpacity by rememberUpdatedState(shapeOpacity)
    val currentSelectedShape by rememberUpdatedState(selectedShape)
    val currentSelectedImage by rememberUpdatedState(selectedImage)
    val currentOnTransformChange by rememberUpdatedState(onTransformChange)
    val currentOnStrokeCommit by rememberUpdatedState(onStrokeCommit)
    val currentOnShapeCommit by rememberUpdatedState(onShapeCommit)
    val currentOnObjectSelect by rememberUpdatedState(onObjectSelect)
    val currentOnSelectedMove by rememberUpdatedState(onSelectedMove)

    // Active stroke in progress
    val activePoints = remember { mutableStateListOf<StrokePoint>() }
    // Active shape in progress
    var activeShapeStart by remember { mutableStateOf<Offset?>(null) }
    var activeShapeEnd by remember { mutableStateOf<Offset?>(null) }

    // Multi-touch tracking
    var isMultiTouching by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().testTag("infinite_canvas_box")) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("drawing_canvas")
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointers = event.changes

                            // Reset multi-touch if all fingers lifted
                            if (pointers.isEmpty() || pointers.all { !it.pressed }) {
                                isMultiTouching = false
                            }

                            if (pointers.size >= 2) {
                                // Multi-touch: 2 fingers -> Pinch zoom and Pan
                                isMultiTouching = true
                                activePoints.clear()
                                activeShapeStart = null
                                activeShapeEnd = null

                                val p0 = pointers[0]
                                val p1 = pointers[1]

                                val prevDist = (p0.previousPosition - p1.previousPosition).getDistance()
                                val currDist = (p0.position - p1.position).getDistance()

                                val prevCenter = (p0.previousPosition + p1.previousPosition) / 2f
                                val currCenter = (p0.position + p1.position) / 2f

                                val panDelta = currCenter - prevCenter
                                val zoomRatio = if (prevDist > 10f && currDist > 10f) {
                                    currDist / prevDist
                                } else 1.0f

                                val t = currentTransform
                                val newZoom = (t.zoom * zoomRatio).coerceIn(0.05f, 40.0f)
                                val zoomFactor = newZoom / t.zoom

                                val newPanX = currCenter.x - (currCenter.x - (t.panX + panDelta.x)) * zoomFactor
                                val newPanY = currCenter.y - (currCenter.y - (t.panY + panDelta.y)) * zoomFactor

                                currentOnTransformChange(newPanX, newPanY, newZoom)

                                p0.consume()
                                p1.consume()
                            } else if (pointers.size == 1) {
                                val pointer = pointers[0]

                                // If new touch down starts, reset multi-touch state immediately
                                if (pointer.changedToDown()) {
                                    isMultiTouching = false
                                }

                                if (isMultiTouching) {
                                    if (pointer.changedToUp() || !pointer.pressed) {
                                        isMultiTouching = false
                                    }
                                    pointer.consume()
                                    continue
                                }

                                val screenPos = pointer.position
                                val canvasPos = currentTransform.screenToCanvas(screenPos.x, screenPos.y)
                                val pressure = if (pointer.type == PointerType.Stylus) pointer.pressure else 1.0f

                                if (pointer.changedToDown() || (pointer.pressed && activePoints.isEmpty() && activeShapeStart == null)) {
                                    // Touch Down
                                    when (currentActiveTool) {
                                        ActiveTool.BRUSH, ActiveTool.ERASER -> {
                                            activePoints.clear()
                                            activePoints.add(StrokePoint(canvasPos.x, canvasPos.y, pressure))
                                        }
                                        ActiveTool.SHAPE -> {
                                            activeShapeStart = canvasPos
                                            activeShapeEnd = canvasPos
                                        }
                                        ActiveTool.SELECT -> {
                                            val hitCurImg = currentSelectedImage?.let { img ->
                                                val b = img.bounds()
                                                canvasPos.x in b.left..b.right && canvasPos.y in b.top..b.bottom
                                            } ?: false
                                            val hitCurShape = currentSelectedShape?.let { shp ->
                                                val b = shp.bounds()
                                                canvasPos.x in b.left..b.right && canvasPos.y in b.top..b.bottom
                                            } ?: false

                                            if (!hitCurImg && !hitCurShape) {
                                                currentOnObjectSelect(canvasPos)
                                            }
                                        }
                                        ActiveTool.IMAGE -> {}
                                        ActiveTool.PAN -> {}
                                    }
                                    pointer.consume()
                                } else if (pointer.pressed) {
                                    // Touch Drag / Move
                                    when (currentActiveTool) {
                                        ActiveTool.BRUSH, ActiveTool.ERASER -> {
                                            val last = activePoints.lastOrNull()
                                            if (last == null || (last.x - canvasPos.x) * (last.x - canvasPos.x) + (last.y - canvasPos.y) * (last.y - canvasPos.y) >= 1f) {
                                                activePoints.add(StrokePoint(canvasPos.x, canvasPos.y, pressure))
                                            }
                                        }
                                        ActiveTool.SHAPE -> {
                                            activeShapeEnd = canvasPos
                                        }
                                        ActiveTool.SELECT -> {
                                            if (currentSelectedShape != null || currentSelectedImage != null) {
                                                val prevScreen = pointer.previousPosition
                                                val prevCanvas = currentTransform.screenToCanvas(prevScreen.x, prevScreen.y)
                                                val dx = canvasPos.x - prevCanvas.x
                                                val dy = canvasPos.y - prevCanvas.y
                                                currentOnSelectedMove(dx, dy)
                                            }
                                        }
                                        ActiveTool.PAN -> {
                                            val panDelta = pointer.position - pointer.previousPosition
                                            val t = currentTransform
                                            currentOnTransformChange(t.panX + panDelta.x, t.panY + panDelta.y, t.zoom)
                                        }
                                        ActiveTool.IMAGE -> {}
                                    }
                                    pointer.consume()
                                } else if (pointer.changedToUp() || !pointer.pressed) {
                                    // Touch Up -> Commit Stroke or Shape!
                                    when (currentActiveTool) {
                                        ActiveTool.BRUSH, ActiveTool.ERASER -> {
                                            if (activePoints.isNotEmpty()) {
                                                val bgCol = currentProject.background.bgColor
                                                val finalColor = if (currentActiveTool == ActiveTool.ERASER) bgCol else currentBrushColor
                                                val stroke = Stroke(
                                                    id = UUID.randomUUID().toString(),
                                                    points = activePoints.toList(),
                                                    brushType = if (currentActiveTool == ActiveTool.ERASER) BrushType.ERASER else currentBrushType,
                                                    size = currentBrushSize,
                                                    color = finalColor,
                                                    opacity = currentBrushOpacity
                                                )
                                                currentOnStrokeCommit(stroke)
                                                activePoints.clear()
                                            }
                                        }
                                        ActiveTool.SHAPE -> {
                                            val start = activeShapeStart
                                            val end = activeShapeEnd
                                            if (start != null && end != null) {
                                                val dist = (end - start).getDistance()
                                                if (dist > 4f) {
                                                    val shape = ShapeObject(
                                                        id = UUID.randomUUID().toString(),
                                                        type = currentSelectedShapeType,
                                                        startX = start.x,
                                                        startY = start.y,
                                                        endX = end.x,
                                                        endY = end.y,
                                                        strokeWidth = currentShapeStrokeWidth,
                                                        strokeColor = currentShapeStrokeColor,
                                                        fillColor = currentShapeFillColor,
                                                        isFilled = currentShapeIsFilled,
                                                        opacity = currentShapeOpacity
                                                    )
                                                    currentOnShapeCommit(shape)
                                                }
                                            }
                                            activeShapeStart = null
                                            activeShapeEnd = null
                                        }
                                        ActiveTool.SELECT -> {}
                                        ActiveTool.IMAGE -> {}
                                        ActiveTool.PAN -> {}
                                    }
                                    isMultiTouching = false
                                    pointer.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            val viewW = size.width
            val viewH = size.height

            // 1. Draw Canvas Background
            drawCanvasBackground(currentProject.background, currentTransform, viewW, viewH)

            // 2. Transform canvas context for content rendering
            translate(currentTransform.panX, currentTransform.panY) {
                scale(currentTransform.zoom, currentTransform.zoom, pivot = Offset.Zero) {
                    // Draw Images
                    for (img in currentProject.images) {
                        drawImageObject(img)
                    }

                    // Draw Shapes
                    for (shape in currentProject.shapes) {
                        drawShapeObject(shape)
                    }

                    // Draw All Committed Strokes
                    for (stroke in currentProject.strokes) {
                        drawStrokeObject(stroke, currentProject.background)
                    }

                    // Draw Active Stroke in progress
                    if (activePoints.isNotEmpty()) {
                        val bgCol = currentProject.background.bgColor
                        val finalColor = if (currentActiveTool == ActiveTool.ERASER) bgCol else currentBrushColor
                        val activeStroke = Stroke(
                            id = "active",
                            points = activePoints.toList(),
                            brushType = if (currentActiveTool == ActiveTool.ERASER) BrushType.ERASER else currentBrushType,
                            size = currentBrushSize,
                            color = finalColor,
                            opacity = currentBrushOpacity
                        )
                        drawStrokeObject(activeStroke, currentProject.background)
                    }

                    // Draw Active Shape in progress
                    val aStart = activeShapeStart
                    val aEnd = activeShapeEnd
                    if (aStart != null && aEnd != null) {
                        val activeShape = ShapeObject(
                            id = "active_shape",
                            type = currentSelectedShapeType,
                            startX = aStart.x,
                            startY = aStart.y,
                            endX = aEnd.x,
                            endY = aEnd.y,
                            strokeWidth = currentShapeStrokeWidth,
                            strokeColor = currentShapeStrokeColor,
                            fillColor = currentShapeFillColor,
                            isFilled = currentShapeIsFilled,
                            opacity = currentShapeOpacity
                        )
                        drawShapeObject(activeShape)
                    }

                    // Draw Selection Highlight Overlay
                    if (currentSelectedShape != null) {
                        drawSelectionBounds(currentSelectedShape!!.bounds())
                    }
                    if (currentSelectedImage != null) {
                        drawSelectionBounds(currentSelectedImage!!.bounds())
                    }
                }
            }
        }
    }
}

/**
 * Draws infinite canvas background patterns.
 */
private fun DrawScope.drawCanvasBackground(
    bg: CanvasBackground,
    transform: CanvasTransform,
    width: Float,
    height: Float
) {
    val bgColor = Color(bg.bgColor)
    drawRect(color = bgColor, size = Size(width, height))

    when (bg) {
        CanvasBackground.WHITE, CanvasBackground.DARK, CanvasBackground.WARM_PAPER -> {
            // Clean solid background
        }
        CanvasBackground.DOT_GRID -> {
            val baseSpacing = 32f
            val spacing = baseSpacing * transform.zoom
            if (spacing > 12f) {
                val startX = (transform.panX % spacing + spacing) % spacing
                val startY = (transform.panY % spacing + spacing) % spacing
                val dotColor = Color(0xFFCBD5E1).copy(alpha = 0.7f)
                val dotRadius = (1.5f * (transform.zoom.coerceIn(0.5f, 2.0f))).coerceAtLeast(1.0f)

                var x = startX
                while (x < width) {
                    var y = startY
                    while (y < height) {
                        drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                        y += spacing
                    }
                    x += spacing
                }
            }
        }
        CanvasBackground.LINE_GRID -> {
            val baseSpacing = 40f
            val spacing = baseSpacing * transform.zoom
            if (spacing > 10f) {
                val startX = (transform.panX % spacing + spacing) % spacing
                val startY = (transform.panY % spacing + spacing) % spacing
                val gridColor = Color(0xFFE2E8F0).copy(alpha = 0.8f)

                var x = startX
                while (x < width) {
                    drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
                    x += spacing
                }
                var y = startY
                while (y < height) {
                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                    y += spacing
                }
            }
        }
        CanvasBackground.DARK_GRAPH -> {
            val baseSpacing = 40f
            val spacing = baseSpacing * transform.zoom
            if (spacing > 10f) {
                val startX = (transform.panX % spacing + spacing) % spacing
                val startY = (transform.panY % spacing + spacing) % spacing
                val gridColor = Color(0xFF2E384D).copy(alpha = 0.6f)

                var x = startX
                while (x < width) {
                    drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
                    x += spacing
                }
                var y = startY
                while (y < height) {
                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                    y += spacing
                }
            }
        }
    }
}

/**
 * Draws image object with rotation and opacity.
 */
private fun DrawScope.drawImageObject(img: ImageObject) {
    val bmp = img.bitmap ?: return
    if (bmp.isRecycled) return

    val imageBitmap = bmp.asImageBitmap()
    rotate(degrees = img.rotation, pivot = Offset(img.x + img.width / 2f, img.y + img.height / 2f)) {
        drawImage(
            image = imageBitmap,
            dstOffset = androidx.compose.ui.unit.IntOffset(img.x.toInt(), img.y.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(img.width.toInt(), img.height.toInt()),
            alpha = img.opacity
        )
    }
}

/**
 * Draws geometric shapes.
 */
private fun DrawScope.drawShapeObject(shape: ShapeObject) {
    val strokeColor = Color(shape.strokeColor).copy(alpha = shape.opacity)
    val fillColor = Color(shape.fillColor).copy(alpha = shape.opacity)

    val left = shape.left
    val top = shape.top
    val right = shape.right
    val bottom = shape.bottom
    val width = shape.width
    val height = shape.height
    val center = Offset((left + right) / 2f, (top + bottom) / 2f)

    rotate(degrees = shape.rotation, pivot = center) {
        when (shape.type) {
            ShapeType.RECTANGLE -> {
                if (shape.isFilled) {
                    drawRect(color = fillColor, topLeft = Offset(left, top), size = Size(width, height))
                }
                drawRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = ComposeStroke(width = shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            ShapeType.ROUNDED_RECTANGLE -> {
                val cr = min(width, height) * 0.18f
                if (shape.isFilled) {
                    drawRoundRect(color = fillColor, topLeft = Offset(left, top), size = Size(width, height), cornerRadius = CornerRadius(cr, cr))
                }
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(cr, cr),
                    style = ComposeStroke(width = shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            ShapeType.CIRCLE, ShapeType.ELLIPSE -> {
                if (shape.isFilled) {
                    drawOval(color = fillColor, topLeft = Offset(left, top), size = Size(width, height))
                }
                drawOval(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = ComposeStroke(width = shape.strokeWidth)
                )
            }
            ShapeType.LINE -> {
                drawLine(
                    color = strokeColor,
                    start = Offset(shape.startX, shape.startY),
                    end = Offset(shape.endX, shape.endY),
                    strokeWidth = shape.strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            ShapeType.ARROW -> {
                val start = Offset(shape.startX, shape.startY)
                val end = Offset(shape.endX, shape.endY)
                drawLine(
                    color = strokeColor,
                    start = start,
                    end = end,
                    strokeWidth = shape.strokeWidth,
                    cap = StrokeCap.Round
                )
                val angle = Math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
                val headLen = (shape.strokeWidth * 3.5f).coerceAtLeast(16f)
                val arrowAngle = Math.PI / 6
                val p1 = Offset(
                    (end.x - headLen * cos(angle - arrowAngle)).toFloat(),
                    (end.y - headLen * sin(angle - arrowAngle)).toFloat()
                )
                val p2 = Offset(
                    (end.x - headLen * cos(angle + arrowAngle)).toFloat(),
                    (end.y - headLen * sin(angle + arrowAngle)).toFloat()
                )
                val arrowPath = Path().apply {
                    moveTo(end.x, end.y)
                    lineTo(p1.x, p1.y)
                    moveTo(end.x, end.y)
                    lineTo(p2.x, p2.y)
                }
                drawPath(path = arrowPath, color = strokeColor, style = ComposeStroke(width = shape.strokeWidth, cap = StrokeCap.Round))
            }
            ShapeType.TRIANGLE -> {
                val tri = Path().apply {
                    moveTo(center.x, top)
                    lineTo(right, bottom)
                    lineTo(left, bottom)
                    close()
                }
                if (shape.isFilled) drawPath(path = tri, color = fillColor)
                drawPath(path = tri, color = strokeColor, style = ComposeStroke(width = shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ShapeType.POLYGON -> {
                val poly = Path()
                val radius = min(width, height) / 2f
                for (i in 0 until 6) {
                    val a = (i * 60 - 30) * Math.PI / 180.0
                    val px = (center.x + radius * cos(a)).toFloat()
                    val py = (cy(center.y, radius, a)).toFloat()
                    if (i == 0) poly.moveTo(px, py) else poly.lineTo(px, py)
                }
                poly.close()
                if (shape.isFilled) drawPath(path = poly, color = fillColor)
                drawPath(path = poly, color = strokeColor, style = ComposeStroke(width = shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ShapeType.STAR -> {
                val star = Path()
                val outerR = min(width, height) / 2f
                val innerR = outerR * 0.45f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val a = (i * 36 - 90) * Math.PI / 180.0
                    val px = (center.x + r * cos(a)).toFloat()
                    val py = (center.y + r * sin(a)).toFloat()
                    if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
                }
                star.close()
                if (shape.isFilled) drawPath(path = star, color = fillColor)
                drawPath(path = star, color = strokeColor, style = ComposeStroke(width = shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

private fun cy(centerY: Float, radius: Float, angleRad: Double): Double {
    return centerY + radius * sin(angleRad)
}

/**
 * Draws vector stroke using smooth Bezier path with brush style.
 */
private fun DrawScope.drawStrokeObject(stroke: Stroke, background: CanvasBackground) {
    if (stroke.points.isEmpty()) return

    val baseColor = stroke.composeColor()

    // Single point / dot fallback
    if (stroke.points.size == 1) {
        val p = stroke.points[0]
        val drawCol = if (stroke.brushType == BrushType.ERASER) Color(background.bgColor) else baseColor
        drawCircle(
            color = drawCol,
            radius = (stroke.size / 2f).coerceAtLeast(1f),
            center = Offset(p.x, p.y)
        )
        return
    }

    val path = DrawingEngine.createSmoothPath(stroke.points)

    when (stroke.brushType) {
        BrushType.ERASER -> {
            // Eraser cleanly matches canvas background color
            drawPath(
                path = path,
                color = Color(background.bgColor),
                style = ComposeStroke(
                    width = stroke.size,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        BrushType.HIGHLIGHTER -> {
            drawPath(
                path = path,
                color = baseColor.copy(alpha = (stroke.opacity * 0.35f).coerceIn(0.05f, 0.5f)),
                style = ComposeStroke(
                    width = stroke.size * 1.8f,
                    cap = StrokeCap.Square,
                    join = StrokeJoin.Bevel
                )
            )
        }
        BrushType.PENCIL -> {
            drawPath(
                path = path,
                color = baseColor.copy(alpha = stroke.opacity * 0.85f),
                style = ComposeStroke(
                    width = stroke.size * 0.8f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        BrushType.AIRBRUSH -> {
            // Layered soft drawing
            drawPath(
                path = path,
                color = baseColor.copy(alpha = stroke.opacity * 0.2f),
                style = ComposeStroke(
                    width = stroke.size * 1.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawPath(
                path = path,
                color = baseColor.copy(alpha = stroke.opacity * 0.4f),
                style = ComposeStroke(
                    width = stroke.size * 0.7f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        BrushType.MARKER -> {
            drawPath(
                path = path,
                color = baseColor.copy(alpha = stroke.opacity * 0.9f),
                style = ComposeStroke(
                    width = stroke.size * 1.3f,
                    cap = StrokeCap.Square,
                    join = StrokeJoin.Round
                )
            )
        }
        BrushType.INK_BRUSH -> {
            drawPath(
                path = path,
                color = baseColor,
                style = ComposeStroke(
                    width = stroke.size * 1.1f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        BrushType.PIXEL_BRUSH -> {
            drawPath(
                path = path,
                color = baseColor,
                style = ComposeStroke(
                    width = stroke.size,
                    cap = StrokeCap.Square,
                    join = StrokeJoin.Miter
                )
            )
        }
        else -> {
            // Standard Pen / Soft / Hard Brush
            drawPath(
                path = path,
                color = baseColor,
                style = ComposeStroke(
                    width = stroke.size,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Draws dashed bounding box around selected object.
 */
private fun DrawScope.drawSelectionBounds(bounds: Rect) {
    val highlightColor = Color(0xFF38BDF8)
    drawRoundRect(
        color = highlightColor,
        topLeft = Offset(bounds.left, bounds.top),
        size = Size(bounds.width, bounds.height),
        cornerRadius = CornerRadius(8f, 8f),
        style = ComposeStroke(
            width = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
        )
    )

    // Corner handle circles
    val cornerRadius = 6f
    drawCircle(color = highlightColor, radius = cornerRadius, center = Offset(bounds.left, bounds.top))
    drawCircle(color = highlightColor, radius = cornerRadius, center = Offset(bounds.right, bounds.top))
    drawCircle(color = highlightColor, radius = cornerRadius, center = Offset(bounds.left, bounds.bottom))
    drawCircle(color = highlightColor, radius = cornerRadius, center = Offset(bounds.right, bounds.bottom))
}
