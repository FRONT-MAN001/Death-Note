package com.example.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.model.BrushType
import com.example.model.CanvasBackground
import com.example.model.CanvasTransform
import com.example.model.DrawProject
import com.example.model.ImageObject
import com.example.model.ShapeObject
import com.example.model.ShapeType
import com.example.model.Stroke
import com.example.model.StrokePoint
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Serializes and deserializes `.idraw` project packages.
 * An `.idraw` file is a self-contained ZIP archive containing metadata, vector strokes,
 * shapes, image placements, and embedded image assets.
 */
object ProjectSerializer {

    private const val ENTRY_MANIFEST = "manifest.json"
    private const val ENTRY_STROKES = "strokes.json"
    private const val ENTRY_SHAPES = "shapes.json"
    private const val ENTRY_IMAGES = "images.json"
    private const val ENTRY_THUMBNAIL = "thumbnail.png"
    private const val ASSETS_PREFIX = "assets/"

    /**
     * Serializes a DrawProject to an OutputStream (e.g., File or SAF DocumentOutputStream).
     */
    fun serialize(project: DrawProject, outputStream: OutputStream, thumbnailBitmap: Bitmap? = null) {
        ZipOutputStream(outputStream.buffered()).use { zip ->
            // 1. Manifest
            zip.putNextEntry(ZipEntry(ENTRY_MANIFEST))
            val manifestJson = JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("version", project.version)
                put("createdTime", project.createdTime)
                put("modifiedTime", System.currentTimeMillis())
                put("background", project.background.name)
                put("panX", project.transform.panX.toDouble())
                put("panY", project.transform.panY.toDouble())
                put("zoom", project.transform.zoom.toDouble())
            }
            zip.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. Strokes
            zip.putNextEntry(ZipEntry(ENTRY_STROKES))
            val strokesArray = JSONArray()
            for (stroke in project.strokes) {
                val strokeObj = JSONObject().apply {
                    put("id", stroke.id)
                    put("brushType", stroke.brushType.name)
                    put("size", stroke.size.toDouble())
                    put("color", stroke.color)
                    put("opacity", stroke.opacity.toDouble())
                    val pts = JSONArray()
                    for (p in stroke.points) {
                        val pt = JSONObject().apply {
                            put("x", p.x.toDouble())
                            put("y", p.y.toDouble())
                            put("p", p.pressure.toDouble())
                        }
                        pts.put(pt)
                    }
                    put("points", pts)
                }
                strokesArray.put(strokeObj)
            }
            zip.write(strokesArray.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 3. Shapes
            zip.putNextEntry(ZipEntry(ENTRY_SHAPES))
            val shapesArray = JSONArray()
            for (shape in project.shapes) {
                val shapeObj = JSONObject().apply {
                    put("id", shape.id)
                    put("type", shape.type.name)
                    put("startX", shape.startX.toDouble())
                    put("startY", shape.startY.toDouble())
                    put("endX", shape.endX.toDouble())
                    put("endY", shape.endY.toDouble())
                    put("strokeWidth", shape.strokeWidth.toDouble())
                    put("strokeColor", shape.strokeColor)
                    put("fillColor", shape.fillColor)
                    put("isFilled", shape.isFilled)
                    put("opacity", shape.opacity.toDouble())
                    put("rotation", shape.rotation.toDouble())
                }
                shapesArray.put(shapeObj)
            }
            zip.write(shapesArray.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 4. Images metadata
            zip.putNextEntry(ZipEntry(ENTRY_IMAGES))
            val imagesArray = JSONArray()
            for (image in project.images) {
                val imgObj = JSONObject().apply {
                    put("id", image.id)
                    put("fileName", image.fileName)
                    put("x", image.x.toDouble())
                    put("y", image.y.toDouble())
                    put("width", image.width.toDouble())
                    put("height", image.height.toDouble())
                    put("rotation", image.rotation.toDouble())
                    put("opacity", image.opacity.toDouble())
                }
                imagesArray.put(imgObj)
            }
            zip.write(imagesArray.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 5. Embedded Image Assets
            for ((fileName, bytes) in project.imageAssets) {
                zip.putNextEntry(ZipEntry("$ASSETS_PREFIX$fileName"))
                zip.write(bytes)
                zip.closeEntry()
            }

            // 6. Thumbnail
            if (thumbnailBitmap != null && !thumbnailBitmap.isRecycled) {
                zip.putNextEntry(ZipEntry(ENTRY_THUMBNAIL))
                val thumbBytes = ByteArrayOutputStream().use { baos ->
                    thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
                    baos.toByteArray()
                }
                zip.write(thumbBytes)
                zip.closeEntry()
            }
        }
    }

    /**
     * Deserializes a DrawProject from an InputStream.
     */
    fun deserialize(inputStream: InputStream): DrawProject {
        var manifestJson: JSONObject? = null
        var strokesJson: JSONArray? = null
        var shapesJson: JSONArray? = null
        var imagesJson: JSONArray? = null
        val imageAssets = mutableMapOf<String, ByteArray>()

        ZipInputStream(inputStream.buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == ENTRY_MANIFEST -> {
                        val text = zis.readBytes().toString(Charsets.UTF_8)
                        manifestJson = JSONObject(text)
                    }
                    name == ENTRY_STROKES -> {
                        val text = zis.readBytes().toString(Charsets.UTF_8)
                        strokesJson = JSONArray(text)
                    }
                    name == ENTRY_SHAPES -> {
                        val text = zis.readBytes().toString(Charsets.UTF_8)
                        shapesJson = JSONArray(text)
                    }
                    name == ENTRY_IMAGES -> {
                        val text = zis.readBytes().toString(Charsets.UTF_8)
                        imagesJson = JSONArray(text)
                    }
                    name.startsWith(ASSETS_PREFIX) -> {
                        val fileName = name.removePrefix(ASSETS_PREFIX)
                        if (fileName.isNotEmpty()) {
                            imageAssets[fileName] = zis.readBytes()
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val manifest = manifestJson ?: throw IllegalArgumentException("Invalid .idraw file: missing manifest")
        val id = manifest.optString("id", java.util.UUID.randomUUID().toString())
        val name = manifest.optString("name", "Drawing")
        val version = manifest.optInt("version", 1)
        val created = manifest.optLong("createdTime", System.currentTimeMillis())
        val modified = manifest.optLong("modifiedTime", System.currentTimeMillis())
        val bgName = manifest.optString("background", CanvasBackground.WHITE.name)
        val background = runCatching { CanvasBackground.valueOf(bgName) }.getOrDefault(CanvasBackground.WHITE)

        val panX = manifest.optDouble("panX", 0.0).toFloat()
        val panY = manifest.optDouble("panY", 0.0).toFloat()
        val zoom = manifest.optDouble("zoom", 1.0).toFloat()
        val transform = CanvasTransform(panX, panY, zoom)

        // Parse strokes
        val strokes = mutableListOf<Stroke>()
        if (strokesJson != null) {
            for (i in 0 until strokesJson.length()) {
                val sObj = strokesJson.getJSONObject(i)
                val sId = sObj.optString("id", java.util.UUID.randomUUID().toString())
                val brushTypeName = sObj.optString("brushType", BrushType.PEN.name)
                val brushType = runCatching { BrushType.valueOf(brushTypeName) }.getOrDefault(BrushType.PEN)
                val size = sObj.optDouble("size", 8.0).toFloat()
                val color = sObj.optLong("color", 0xFF000000.toLong())
                val opacity = sObj.optDouble("opacity", 1.0).toFloat()

                val ptsArray = sObj.optJSONArray("points")
                val pts = mutableListOf<StrokePoint>()
                if (ptsArray != null) {
                    for (j in 0 until ptsArray.length()) {
                        val p = ptsArray.getJSONObject(j)
                        pts.add(
                            StrokePoint(
                                x = p.optDouble("x", 0.0).toFloat(),
                                y = p.optDouble("y", 0.0).toFloat(),
                                pressure = p.optDouble("p", 1.0).toFloat()
                            )
                        )
                    }
                }
                strokes.add(
                    Stroke(
                        id = sId,
                        points = pts,
                        brushType = brushType,
                        size = size,
                        color = color,
                        opacity = opacity
                    )
                )
            }
        }

        // Parse shapes
        val shapes = mutableListOf<ShapeObject>()
        if (shapesJson != null) {
            for (i in 0 until shapesJson.length()) {
                val sh = shapesJson.getJSONObject(i)
                val shId = sh.optString("id", java.util.UUID.randomUUID().toString())
                val typeName = sh.optString("type", ShapeType.RECTANGLE.name)
                val type = runCatching { ShapeType.valueOf(typeName) }.getOrDefault(ShapeType.RECTANGLE)
                shapes.add(
                    ShapeObject(
                        id = shId,
                        type = type,
                        startX = sh.optDouble("startX", 0.0).toFloat(),
                        startY = sh.optDouble("startY", 0.0).toFloat(),
                        endX = sh.optDouble("endX", 0.0).toFloat(),
                        endY = sh.optDouble("endY", 0.0).toFloat(),
                        strokeWidth = sh.optDouble("strokeWidth", 4.0).toFloat(),
                        strokeColor = sh.optLong("strokeColor", 0xFF000000.toLong()),
                        fillColor = sh.optLong("fillColor", 0x00000000.toLong()),
                        isFilled = sh.optBoolean("isFilled", false),
                        opacity = sh.optDouble("opacity", 1.0).toFloat(),
                        rotation = sh.optDouble("rotation", 0.0).toFloat()
                    )
                )
            }
        }

        // Parse images
        val images = mutableListOf<ImageObject>()
        if (imagesJson != null) {
            for (i in 0 until imagesJson.length()) {
                val img = imagesJson.getJSONObject(i)
                val imgId = img.optString("id", java.util.UUID.randomUUID().toString())
                val fileName = img.optString("fileName", "")
                val imgBytes = imageAssets[fileName]
                val bmp = if (imgBytes != null) {
                    BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                } else null

                images.add(
                    ImageObject(
                        id = imgId,
                        fileName = fileName,
                        x = img.optDouble("x", 0.0).toFloat(),
                        y = img.optDouble("y", 0.0).toFloat(),
                        width = img.optDouble("width", 200.0).toFloat(),
                        height = img.optDouble("height", 200.0).toFloat(),
                        rotation = img.optDouble("rotation", 0.0).toFloat(),
                        opacity = img.optDouble("opacity", 1.0).toFloat(),
                        bitmap = bmp
                    )
                )
            }
        }

        return DrawProject(
            id = id,
            name = name,
            version = version,
            createdTime = created,
            modifiedTime = modified,
            background = background,
            transform = transform,
            strokes = strokes,
            shapes = shapes,
            images = images,
            imageAssets = imageAssets
        )
    }

    /**
     * Quickly extract thumbnail from .idraw ZIP stream without loading the full stroke data.
     */
    fun extractThumbnail(inputStream: InputStream): Bitmap? {
        return try {
            ZipInputStream(inputStream.buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == ENTRY_THUMBNAIL) {
                        val bytes = zis.readBytes()
                        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
