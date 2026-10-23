package com.example

import com.example.engine.DrawingEngine
import com.example.model.BrushType
import com.example.model.CanvasBackground
import com.example.model.CanvasTransform
import com.example.model.DrawProject
import com.example.model.ShapeObject
import com.example.model.ShapeType
import com.example.model.Stroke
import com.example.model.StrokePoint
import com.example.storage.ProjectSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DrawingEngineAndStorageTest {

    @Test
    fun testCanvasTransformCoordinates() {
        val transform = CanvasTransform(panX = 100f, panY = 50f, zoom = 2.0f)
        val canvasPt = transform.screenToCanvas(200f, 150f)
        assertEquals(50f, canvasPt.x, 0.001f)
        assertEquals(50f, canvasPt.y, 0.001f)

        val screenPt = transform.canvasToScreen(50f, 50f)
        assertEquals(200f, screenPt.x, 0.001f)
        assertEquals(150f, screenPt.y, 0.001f)
    }

    @Test
    fun testProjectSerializationRoundtrip() {
        val stroke = Stroke(
            id = "stroke-1",
            points = listOf(
                StrokePoint(10f, 20f, 0.8f),
                StrokePoint(30f, 40f, 1.0f)
            ),
            brushType = BrushType.PEN,
            size = 12f,
            color = 0xFF123456.toLong(),
            opacity = 0.9f
        )

        val shape = ShapeObject(
            id = "shape-1",
            type = ShapeType.ROUNDED_RECTANGLE,
            startX = 100f,
            startY = 150f,
            endX = 300f,
            endY = 400f,
            strokeWidth = 4f,
            strokeColor = 0xFF000000.toLong(),
            fillColor = 0x88FF0000.toLong(),
            isFilled = true,
            opacity = 1.0f
        )

        val project = DrawProject(
            id = "proj-123",
            name = "Test Sketch",
            version = 1,
            background = CanvasBackground.WARM_PAPER,
            transform = CanvasTransform(panX = 50f, panY = -30f, zoom = 1.5f),
            strokes = listOf(stroke),
            shapes = listOf(shape)
        )

        val baos = ByteArrayOutputStream()
        ProjectSerializer.serialize(project, baos, null)
        val bytes = baos.toByteArray()
        assertTrue("Serialized .idraw must not be empty", bytes.isNotEmpty())

        val bais = ByteArrayInputStream(bytes)
        val restored = ProjectSerializer.deserialize(bais)

        assertEquals(project.id, restored.id)
        assertEquals(project.name, restored.name)
        assertEquals(project.background, restored.background)
        assertEquals(project.transform.panX, restored.transform.panX, 0.001f)
        assertEquals(project.transform.panY, restored.transform.panY, 0.001f)
        assertEquals(project.transform.zoom, restored.transform.zoom, 0.001f)

        assertEquals(1, restored.strokes.size)
        val restoredStroke = restored.strokes[0]
        assertEquals(stroke.id, restoredStroke.id)
        assertEquals(stroke.brushType, restoredStroke.brushType)
        assertEquals(stroke.size, restoredStroke.size, 0.001f)
        assertEquals(stroke.color, restoredStroke.color)
        assertEquals(stroke.points.size, restoredStroke.points.size)

        assertEquals(1, restored.shapes.size)
        val restoredShape = restored.shapes[0]
        assertEquals(shape.id, restoredShape.id)
        assertEquals(shape.type, restoredShape.type)
        assertEquals(shape.isFilled, restoredShape.isFilled)
        assertEquals(shape.strokeColor, restoredShape.strokeColor)
    }

    @Test
    fun testOverallBoundsCalculation() {
        val stroke = Stroke(
            id = "s1",
            points = listOf(StrokePoint(0f, 0f), StrokePoint(100f, 200f)),
            brushType = BrushType.PEN,
            size = 10f,
            color = 0xFF000000.toLong()
        )
        val project = DrawProject(strokes = listOf(stroke))
        val bounds = project.computeOverallBounds()
        assertTrue(bounds.left <= 0f)
        assertTrue(bounds.top <= 0f)
        assertTrue(bounds.right >= 100f)
        assertTrue(bounds.bottom >= 200f)
    }

    @Test
    fun testInfinitePanCoordinates() {
        // Test massive negative and positive coordinates on X and Y
        val farTransform = CanvasTransform(panX = -50000f, panY = 75000f, zoom = 0.5f)
        val screenPos = farTransform.canvasToScreen(10000f, -20000f)
        val backToCanvas = farTransform.screenToCanvas(screenPos.x, screenPos.y)

        assertEquals(10000f, backToCanvas.x, 0.01f)
        assertEquals(-20000f, backToCanvas.y, 0.01f)

        // Reset zoom / origin
        val origin = CanvasTransform(0f, 0f, 1.0f)
        val originPt = origin.screenToCanvas(0f, 0f)
        assertEquals(0f, originPt.x, 0.001f)
        assertEquals(0f, originPt.y, 0.001f)
    }

    @Test
    fun testPanToolAvailability() {
        val panTool = com.example.model.ActiveTool.PAN
        assertNotNull(panTool)
        assertEquals("PAN", panTool.name)
    }
}

