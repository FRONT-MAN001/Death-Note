package com.example

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.example.engine.DrawingEngine
import com.example.model.BrushType
import com.example.model.Stroke
import com.example.model.StrokePoint
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testColorCreation() {
    val rawLong = 0xFF0F172A.toLong()
    val c = Color(rawLong)
    println("c.alpha = ${c.alpha}, c.red = ${c.red}, c.green = ${c.green}, c.blue = ${c.blue}")
    assertTrue("Alpha should be > 0.9, got ${c.alpha}", c.alpha > 0.9f)
  }

  @Test
  fun testIntersectsViewport() {
    val points = listOf(
      StrokePoint(100f, 100f),
      StrokePoint(120f, 120f),
      StrokePoint(150f, 150f)
    )
    val stroke = Stroke(
      id = "s1",
      points = points,
      brushType = BrushType.PEN,
      size = 8f,
      color = 0xFF0F172A.toLong()
    )
    val viewport = Rect(0f, 0f, 1080f, 1920f)
    println("stroke.bounds = ${stroke.bounds}, viewport = $viewport")
    val intersects = DrawingEngine.intersectsViewport(stroke.bounds, viewport)
    assertTrue("Stroke should intersect viewport", intersects)
  }
}
