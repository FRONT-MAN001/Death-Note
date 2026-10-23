package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BrushType
import com.example.model.Stroke
import com.example.model.StrokePoint
import com.example.viewmodel.DrawingViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("[Death Note]", appName)
  }

  @Test
  fun `test commit stroke updates project state`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DrawingViewModel(app)
    assertEquals(0, viewModel.project.value.strokes.size)

    val stroke = Stroke(
      id = "test-1",
      points = listOf(StrokePoint(50f, 50f), StrokePoint(100f, 100f)),
      brushType = BrushType.PEN,
      size = 8f,
      color = 0xFF0F172A.toLong(),
      opacity = 1f
    )
    viewModel.commitStroke(stroke)
    assertEquals(1, viewModel.project.value.strokes.size)
  }
}
