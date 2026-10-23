package com.example.model

/**
 * Complete editable project representation for Infinite Draw.
 */
data class DrawProject(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Untitled Drawing",
    val version: Int = 1,
    val createdTime: Long = System.currentTimeMillis(),
    val modifiedTime: Long = System.currentTimeMillis(),
    val background: CanvasBackground = CanvasBackground.WHITE,
    val transform: CanvasTransform = CanvasTransform(),
    val strokes: List<Stroke> = emptyList(),
    val shapes: List<ShapeObject> = emptyList(),
    val images: List<ImageObject> = emptyList(),
    val imageAssets: Map<String, ByteArray> = emptyMap()
) {
    /**
     * Compute the aggregate bounding box of all strokes, shapes, and images.
     */
    fun computeOverallBounds(): androidx.compose.ui.geometry.Rect {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var hasContent = false

        for (stroke in strokes) {
            val b = stroke.bounds
            if (b.left < minX) minX = b.left
            if (b.top < minY) minY = b.top
            if (b.right > maxX) maxX = b.right
            if (b.bottom > maxY) maxY = b.bottom
            hasContent = true
        }

        for (shape in shapes) {
            val b = shape.bounds()
            if (b.left < minX) minX = b.left
            if (b.top < minY) minY = b.top
            if (b.right > maxX) maxX = b.right
            if (b.bottom > maxY) maxY = b.bottom
            hasContent = true
        }

        for (image in images) {
            val b = image.bounds()
            if (b.left < minX) minX = b.left
            if (b.top < minY) minY = b.top
            if (b.right > maxX) maxX = b.right
            if (b.bottom > maxY) maxY = b.bottom
            hasContent = true
        }

        if (!hasContent) {
            return androidx.compose.ui.geometry.Rect(-500f, -500f, 500f, 500f)
        }
        return androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY)
    }
}
