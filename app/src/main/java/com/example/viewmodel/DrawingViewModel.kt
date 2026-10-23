package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.DrawCommand
import com.example.engine.DrawingEngine
import com.example.model.ActiveTool
import com.example.model.BrushType
import com.example.model.CanvasBackground
import com.example.model.CanvasTransform
import com.example.model.DrawProject
import com.example.model.ImageObject
import com.example.model.ProjectMetadata
import com.example.model.ShapeObject
import com.example.model.ShapeType
import com.example.model.Stroke
import com.example.storage.PdfExporter
import com.example.storage.SafStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class DrawingViewModel(application: Application) : AndroidViewModel(application) {

    val storageManager = SafStorageManager(application)

    private val _project = MutableStateFlow(DrawProject())
    val project: StateFlow<DrawProject> = _project.asStateFlow()

    private val _currentFileUri = MutableStateFlow<Uri?>(null)
    val currentFileUri: StateFlow<Uri?> = _currentFileUri.asStateFlow()

    private val _activeTool = MutableStateFlow(ActiveTool.BRUSH)
    val activeTool: StateFlow<ActiveTool> = _activeTool.asStateFlow()

    private val _brushType = MutableStateFlow(BrushType.PEN)
    val brushType: StateFlow<BrushType> = _brushType.asStateFlow()

    private val _brushSize = MutableStateFlow(8f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    private val _brushColor = MutableStateFlow(0xFF0F172A.toLong()) // Deep slate/black
    val brushColor: StateFlow<Long> = _brushColor.asStateFlow()

    private val _brushOpacity = MutableStateFlow(1.0f)
    val brushOpacity: StateFlow<Float> = _brushOpacity.asStateFlow()

    // Shapes
    private val _selectedShapeType = MutableStateFlow(ShapeType.RECTANGLE)
    val selectedShapeType: StateFlow<ShapeType> = _selectedShapeType.asStateFlow()

    private val _shapeStrokeWidth = MutableStateFlow(6f)
    val shapeStrokeWidth: StateFlow<Float> = _shapeStrokeWidth.asStateFlow()

    private val _shapeStrokeColor = MutableStateFlow(0xFF0F172A.toLong())
    val shapeStrokeColor: StateFlow<Long> = _shapeStrokeColor.asStateFlow()

    private val _shapeFillColor = MutableStateFlow(0x4438BDF8.toLong())
    val shapeFillColor: StateFlow<Long> = _shapeFillColor.asStateFlow()

    private val _shapeIsFilled = MutableStateFlow(false)
    val shapeIsFilled: StateFlow<Boolean> = _shapeIsFilled.asStateFlow()

    private val _shapeOpacity = MutableStateFlow(1.0f)
    val shapeOpacity: StateFlow<Float> = _shapeOpacity.asStateFlow()

    // Transform / Viewport
    private val _transform = MutableStateFlow(CanvasTransform())
    val transform: StateFlow<CanvasTransform> = _transform.asStateFlow()

    // Selection
    private val _selectedShape = MutableStateFlow<ShapeObject?>(null)
    val selectedShape: StateFlow<ShapeObject?> = _selectedShape.asStateFlow()

    private val _selectedImage = MutableStateFlow<ImageObject?>(null)
    val selectedImage: StateFlow<ImageObject?> = _selectedImage.asStateFlow()

    // Undo / Redo
    private val undoStack = mutableListOf<DrawCommand>()
    private val redoStack = mutableListOf<DrawCommand>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    // Project File Management
    private val _activeFolderName = MutableStateFlow(storageManager.getActiveFolderName())
    val activeFolderName: StateFlow<String> = _activeFolderName.asStateFlow()

    private val _projectFiles = MutableStateFlow<List<ProjectMetadata>>(emptyList())
    val projectFiles: StateFlow<List<ProjectMetadata>> = _projectFiles.asStateFlow()

    private val _recentColors = MutableStateFlow(
        listOf(
            0xFF0F172A.toLong(), // Slate Dark
            0xFFEF4444.toLong(), // Red
            0xFFF97316.toLong(), // Orange
            0xFFEAB308.toLong(), // Yellow
            0xFF10B981.toLong(), // Emerald
            0xFF06B6D4.toLong(), // Cyan
            0xFF3B82F6.toLong(), // Blue
            0xFF8B5CF6.toLong(), // Violet
            0xFFEC4899.toLong(), // Pink
            0xFFFFFFFF.toLong()  // White
        )
    )
    val recentColors: StateFlow<List<Long>> = _recentColors.asStateFlow()

    // UI Dialog States
    val showFileDrawer = MutableStateFlow(false)
    val showBrushPalette = MutableStateFlow(false)
    val showColorPicker = MutableStateFlow(false)
    val showShapePalette = MutableStateFlow(false)
    val showSaveAsDialog = MutableStateFlow(false)
    val showRenameDialog = MutableStateFlow<ProjectMetadata?>(null)
    val showDeleteConfirmDialog = MutableStateFlow<ProjectMetadata?>(null)
    val showFileInfoDialog = MutableStateFlow<ProjectMetadata?>(null)
    val showPdfExportDialog = MutableStateFlow(false)
    val showAutosaveRecoveryDialog = MutableStateFlow(false)
    val showHelpDialog = MutableStateFlow(false)
    val toastMessage = MutableStateFlow<String?>(null)

    init {
        refreshFileList()
        checkForAutosaveRecovery()
    }

    private fun checkForAutosaveRecovery() {
        if (storageManager.hasAutosave()) {
            showAutosaveRecoveryDialog.value = true
        }
    }

    fun restoreAutosave() {
        viewModelScope.launch(Dispatchers.IO) {
            val recovered = storageManager.loadAutosave()
            if (recovered != null) {
                withContext(Dispatchers.Main) {
                    _project.value = recovered
                    _transform.value = recovered.transform
                    undoStack.clear()
                    redoStack.clear()
                    updateUndoRedoStates()
                    _hasUnsavedChanges.value = true
                    toastMessage.value = "Restored unsaved drawing session"
                }
            }
            showAutosaveRecoveryDialog.value = false
        }
    }

    fun discardAutosave() {
        storageManager.discardAutosave()
        showAutosaveRecoveryDialog.value = false
    }

    fun refreshFileList() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = storageManager.listProjectFiles()
            val folderName = storageManager.getActiveFolderName()
            withContext(Dispatchers.Main) {
                _projectFiles.value = files
                _activeFolderName.value = folderName
            }
        }
    }

    fun selectActiveFolder(treeUri: Uri) {
        storageManager.setActiveFolderUri(treeUri)
        refreshFileList()
        toastMessage.value = "Folder selected: ${storageManager.getActiveFolderName()}"
    }

    // Tools & Styling
    fun setActiveTool(tool: ActiveTool) {
        _activeTool.value = tool
        if (tool != ActiveTool.SELECT) {
            clearSelection()
        }
    }

    fun setBrushType(type: BrushType) {
        _brushType.value = type
        if (type == BrushType.ERASER) {
            _activeTool.value = ActiveTool.ERASER
        } else {
            _activeTool.value = ActiveTool.BRUSH
        }
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size.coerceIn(1f, 160f)
    }

    fun setBrushOpacity(opacity: Float) {
        _brushOpacity.value = opacity.coerceIn(0.01f, 1.0f)
    }

    fun setBrushColor(color: Long) {
        _brushColor.value = color
        // Add to recent colors
        val current = _recentColors.value.toMutableList()
        current.remove(color)
        current.add(0, color)
        if (current.size > 14) current.removeAt(current.size - 1)
        _recentColors.value = current
    }

    fun setCanvasBackground(bg: CanvasBackground) {
        _project.value = _project.value.copy(background = bg)
        _hasUnsavedChanges.value = true
    }

    fun setSelectedShapeType(type: ShapeType) {
        _selectedShapeType.value = type
        _activeTool.value = ActiveTool.SHAPE
    }

    fun setShapeStrokeWidth(w: Float) {
        _shapeStrokeWidth.value = w.coerceIn(1f, 60f)
    }

    fun setShapeStrokeColor(color: Long) {
        _shapeStrokeColor.value = color
    }

    fun setShapeFillColor(color: Long) {
        _shapeFillColor.value = color
    }

    fun setShapeIsFilled(filled: Boolean) {
        _shapeIsFilled.value = filled
    }

    // Zoom & Pan
    fun updatePan(dx: Float, dy: Float) {
        val current = _transform.value
        _transform.value = current.copy(
            panX = current.panX + dx,
            panY = current.panY + dy
        )
    }

    fun setTransform(panX: Float, panY: Float, zoom: Float) {
        _transform.value = CanvasTransform(panX, panY, zoom.coerceIn(0.05f, 40.0f))
    }

    fun updateZoom(zoomFactor: Float, pivotX: Float, pivotY: Float) {
        val current = _transform.value
        val newZoom = (current.zoom * zoomFactor).coerceIn(0.05f, 40.0f)
        val factor = newZoom / current.zoom

        // Zoom toward the finger pivot
        val newPanX = pivotX - (pivotX - current.panX) * factor
        val newPanY = pivotY - (pivotY - current.panY) * factor

        _transform.value = current.copy(
            zoom = newZoom,
            panX = newPanX,
            panY = newPanY
        )
    }

    fun resetZoom() {
        _transform.value = CanvasTransform(0f, 0f, 1.0f)
    }

    fun zoomToFit(viewWidth: Float, viewHeight: Float) {
        val bounds = _project.value.computeOverallBounds()
        val contentW = bounds.width.coerceAtLeast(100f)
        val contentH = bounds.height.coerceAtLeast(100f)

        val zoomX = (viewWidth * 0.85f) / contentW
        val zoomY = (viewHeight * 0.85f) / contentH
        val fitZoom = minOf(zoomX, zoomY).coerceIn(0.1f, 10.0f)

        val cx = bounds.left + bounds.width / 2f
        val cy = bounds.top + bounds.height / 2f

        val newPanX = (viewWidth / 2f) - cx * fitZoom
        val newPanY = (viewHeight / 2f) - cy * fitZoom

        _transform.value = CanvasTransform(panX = newPanX, panY = newPanY, zoom = fitZoom)
    }

    // Drawing Actions
    fun commitStroke(stroke: Stroke) {
        val newStrokes = _project.value.strokes + stroke
        _project.value = _project.value.copy(strokes = newStrokes)
        undoStack.add(DrawCommand.AddStroke(stroke))
        redoStack.clear()
        updateUndoRedoStates()
        markChanged()
    }

    fun commitShape(shape: ShapeObject) {
        val newShapes = _project.value.shapes + shape
        _project.value = _project.value.copy(shapes = newShapes)
        undoStack.add(DrawCommand.AddShape(shape))
        redoStack.clear()
        updateUndoRedoStates()
        markChanged()
    }

    fun commitImage(image: ImageObject, imageBytes: ByteArray) {
        val newImages = _project.value.images + image
        val newAssets = _project.value.imageAssets + (image.fileName to imageBytes)
        _project.value = _project.value.copy(images = newImages, imageAssets = newAssets)
        undoStack.add(DrawCommand.AddImage(image, imageBytes))
        redoStack.clear()
        updateUndoRedoStates()
        markChanged()
    }

    // Selection
    fun selectObjectAt(canvasPoint: Offset) {
        // Hit test shapes first
        val hitShape = DrawingEngine.hitTestShape(_project.value.shapes, canvasPoint)
        if (hitShape != null) {
            _selectedShape.value = hitShape
            _selectedImage.value = null
            return
        }

        // Hit test images next
        val hitImg = DrawingEngine.hitTestImage(_project.value.images, canvasPoint)
        if (hitImg != null) {
            _selectedImage.value = hitImg
            _selectedShape.value = null
            return
        }

        // None hit
        clearSelection()
    }

    fun clearSelection() {
        _selectedShape.value = null
        _selectedImage.value = null
    }

    fun moveSelected(dx: Float, dy: Float) {
        val shape = _selectedShape.value
        if (shape != null) {
            val moved = shape.copy(
                startX = shape.startX + dx,
                startY = shape.startY + dy,
                endX = shape.endX + dx,
                endY = shape.endY + dy
            )
            updateShape(shape, moved)
            _selectedShape.value = moved
            return
        }

        val img = _selectedImage.value
        if (img != null) {
            val moved = img.copy(
                x = img.x + dx,
                y = img.y + dy
            )
            updateImage(img, moved)
            _selectedImage.value = moved
        }
    }

    fun scaleSelected(scaleFactor: Float) {
        val shape = _selectedShape.value
        if (shape != null) {
            val cx = (shape.startX + shape.endX) / 2f
            val cy = (shape.startY + shape.endY) / 2f
            val halfW = (shape.width * scaleFactor) / 2f
            val halfH = (shape.height * scaleFactor) / 2f
            val scaled = shape.copy(
                startX = cx - halfW,
                startY = cy - halfH,
                endX = cx + halfW,
                endY = cy + halfH
            )
            updateShape(shape, scaled)
            _selectedShape.value = scaled
            return
        }

        val img = _selectedImage.value
        if (img != null) {
            val newW = img.width * scaleFactor
            val newH = img.height * scaleFactor
            val scaled = img.copy(width = newW, height = newH)
            updateImage(img, scaled)
            _selectedImage.value = scaled
        }
    }

    fun rotateSelected(angleDelta: Float) {
        val shape = _selectedShape.value
        if (shape != null) {
            val rotated = shape.copy(rotation = (shape.rotation + angleDelta) % 360f)
            updateShape(shape, rotated)
            _selectedShape.value = rotated
            return
        }

        val img = _selectedImage.value
        if (img != null) {
            val rotated = img.copy(rotation = (img.rotation + angleDelta) % 360f)
            updateImage(img, rotated)
            _selectedImage.value = rotated
        }
    }

    fun duplicateSelected() {
        val shape = _selectedShape.value
        if (shape != null) {
            val dup = shape.copy(
                id = UUID.randomUUID().toString(),
                startX = shape.startX + 40f,
                startY = shape.startY + 40f,
                endX = shape.endX + 40f,
                endY = shape.endY + 40f
            )
            commitShape(dup)
            _selectedShape.value = dup
            toastMessage.value = "Shape duplicated"
            return
        }

        val img = _selectedImage.value
        if (img != null) {
            val dup = img.copy(
                id = UUID.randomUUID().toString(),
                x = img.x + 40f,
                y = img.y + 40f
            )
            val bytes = _project.value.imageAssets[img.fileName] ?: ByteArray(0)
            commitImage(dup, bytes)
            _selectedImage.value = dup
            toastMessage.value = "Image duplicated"
        }
    }

    fun deleteSelected() {
        val shape = _selectedShape.value
        if (shape != null) {
            val newShapes = _project.value.shapes.filterNot { it.id == shape.id }
            _project.value = _project.value.copy(shapes = newShapes)
            undoStack.add(DrawCommand.DeleteObjects(emptyList(), listOf(shape), emptyList()))
            redoStack.clear()
            updateUndoRedoStates()
            clearSelection()
            markChanged()
            toastMessage.value = "Deleted selected shape"
            return
        }

        val img = _selectedImage.value
        if (img != null) {
            val newImages = _project.value.images.filterNot { it.id == img.id }
            _project.value = _project.value.copy(images = newImages)
            undoStack.add(DrawCommand.DeleteObjects(emptyList(), emptyList(), listOf(img)))
            redoStack.clear()
            updateUndoRedoStates()
            clearSelection()
            markChanged()
            toastMessage.value = "Deleted selected image"
        }
    }

    private fun updateShape(old: ShapeObject, new: ShapeObject) {
        val list = _project.value.shapes.map { if (it.id == old.id) new else it }
        _project.value = _project.value.copy(shapes = list)
        undoStack.add(DrawCommand.ModifyShape(old, new))
        redoStack.clear()
        updateUndoRedoStates()
        markChanged()
    }

    private fun updateImage(old: ImageObject, new: ImageObject) {
        val list = _project.value.images.map { if (it.id == old.id) new else it }
        _project.value = _project.value.copy(images = list)
        undoStack.add(DrawCommand.ModifyImage(old, new))
        redoStack.clear()
        updateUndoRedoStates()
        markChanged()
    }

    fun clearCanvas() {
        val p = _project.value
        if (p.strokes.isEmpty() && p.shapes.isEmpty() && p.images.isEmpty()) return

        undoStack.add(DrawCommand.ClearCanvas(p.strokes, p.shapes, p.images))
        redoStack.clear()
        _project.value = p.copy(strokes = emptyList(), shapes = emptyList(), images = emptyList())
        updateUndoRedoStates()
        clearSelection()
        markChanged()
        toastMessage.value = "Canvas cleared"
    }

    // Undo / Redo
    fun undo() {
        if (undoStack.isEmpty()) return
        val cmd = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(cmd)

        val p = _project.value
        when (cmd) {
            is DrawCommand.AddStroke -> {
                _project.value = p.copy(strokes = p.strokes.filterNot { it.id == cmd.stroke.id })
            }
            is DrawCommand.AddShape -> {
                _project.value = p.copy(shapes = p.shapes.filterNot { it.id == cmd.shape.id })
            }
            is DrawCommand.AddImage -> {
                _project.value = p.copy(images = p.images.filterNot { it.id == cmd.image.id })
            }
            is DrawCommand.DeleteObjects -> {
                _project.value = p.copy(
                    strokes = p.strokes + cmd.strokes,
                    shapes = p.shapes + cmd.shapes,
                    images = p.images + cmd.images
                )
            }
            is DrawCommand.ModifyShape -> {
                _project.value = p.copy(shapes = p.shapes.map { if (it.id == cmd.newShape.id) cmd.oldShape else it })
            }
            is DrawCommand.ModifyImage -> {
                _project.value = p.copy(images = p.images.map { if (it.id == cmd.newImage.id) cmd.oldImage else it })
            }
            is DrawCommand.ClearCanvas -> {
                _project.value = p.copy(
                    strokes = cmd.previousStrokes,
                    shapes = cmd.previousShapes,
                    images = cmd.previousImages
                )
            }
        }
        updateUndoRedoStates()
        markChanged()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val cmd = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(cmd)

        val p = _project.value
        when (cmd) {
            is DrawCommand.AddStroke -> {
                _project.value = p.copy(strokes = p.strokes + cmd.stroke)
            }
            is DrawCommand.AddShape -> {
                _project.value = p.copy(shapes = p.shapes + cmd.shape)
            }
            is DrawCommand.AddImage -> {
                _project.value = p.copy(images = p.images + cmd.image)
            }
            is DrawCommand.DeleteObjects -> {
                val strokeIds = cmd.strokes.map { it.id }.toSet()
                val shapeIds = cmd.shapes.map { it.id }.toSet()
                val imageIds = cmd.images.map { it.id }.toSet()
                _project.value = p.copy(
                    strokes = p.strokes.filterNot { it.id in strokeIds },
                    shapes = p.shapes.filterNot { it.id in shapeIds },
                    images = p.images.filterNot { it.id in imageIds }
                )
            }
            is DrawCommand.ModifyShape -> {
                _project.value = p.copy(shapes = p.shapes.map { if (it.id == cmd.oldShape.id) cmd.newShape else it })
            }
            is DrawCommand.ModifyImage -> {
                _project.value = p.copy(images = p.images.map { if (it.id == cmd.oldImage.id) cmd.newImage else it })
            }
            is DrawCommand.ClearCanvas -> {
                _project.value = p.copy(strokes = emptyList(), shapes = emptyList(), images = emptyList())
            }
        }
        updateUndoRedoStates()
        markChanged()
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun markChanged() {
        _hasUnsavedChanges.value = true
        // Trigger background autosave safely
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.saveAutosave(_project.value.copy(transform = _transform.value))
        }
    }

    // Project File Management
    fun newDrawing() {
        _project.value = DrawProject(name = "Untitled Drawing")
        _currentFileUri.value = null
        _transform.value = CanvasTransform()
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoStates()
        _hasUnsavedChanges.value = false
        clearSelection()
        storageManager.discardAutosave()
        toastMessage.value = "Created new drawing"
    }

    fun saveCurrentProject(thumbnail: Bitmap?) {
        val existing = _currentFileUri.value
        if (existing == null) {
            // New project requires filename
            showSaveAsDialog.value = true
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val savedProject = _project.value.copy(
                        transform = _transform.value,
                        modifiedTime = System.currentTimeMillis()
                    )
                    storageManager.saveProject(savedProject, existing, savedProject.name, thumbnail)
                    withContext(Dispatchers.Main) {
                        _project.value = savedProject
                        _hasUnsavedChanges.value = false
                        storageManager.discardAutosave()
                        toastMessage.value = "Saved: ${savedProject.name}.idraw"
                        refreshFileList()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        toastMessage.value = "Failed to save: ${e.message}"
                    }
                }
            }
        }
    }

    fun saveProjectAs(name: String, thumbnail: Bitmap?) {
        val cleanName = name.trim().removeSuffix(SafStorageManager.EXTENSION_IDRAW)
        if (cleanName.isBlank()) {
            toastMessage.value = "Please enter a valid filename"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = _project.value.copy(
                    name = cleanName,
                    transform = _transform.value,
                    modifiedTime = System.currentTimeMillis()
                )
                val newUri = storageManager.saveProject(updated, null, cleanName, thumbnail)
                withContext(Dispatchers.Main) {
                    _project.value = updated
                    _currentFileUri.value = newUri
                    _hasUnsavedChanges.value = false
                    storageManager.discardAutosave()
                    showSaveAsDialog.value = false
                    toastMessage.value = "Saved project: $cleanName.idraw"
                    refreshFileList()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toastMessage.value = "Failed to save: ${e.message}"
                }
            }
        }
    }

    fun openProject(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loaded = storageManager.loadProject(uri)
                withContext(Dispatchers.Main) {
                    _project.value = loaded
                    _transform.value = loaded.transform
                    _currentFileUri.value = uri
                    undoStack.clear()
                    redoStack.clear()
                    updateUndoRedoStates()
                    _hasUnsavedChanges.value = false
                    clearSelection()
                    storageManager.discardAutosave()
                    showFileDrawer.value = false
                    toastMessage.value = "Opened ${loaded.name}.idraw"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toastMessage.value = "Could not open file: ${e.message}"
                }
            }
        }
    }

    fun renameProject(meta: ProjectMetadata, newName: String) {
        val clean = newName.trim().removeSuffix(SafStorageManager.EXTENSION_IDRAW)
        if (clean.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(meta.uriString)
                val newUri = storageManager.renameProject(uri, clean)
                withContext(Dispatchers.Main) {
                    if (newUri != null) {
                        if (_currentFileUri.value == uri) {
                            _currentFileUri.value = newUri
                            _project.value = _project.value.copy(name = clean)
                        }
                        toastMessage.value = "Renamed to $clean.idraw"
                    } else {
                        toastMessage.value = "Could not rename project"
                    }
                    showRenameDialog.value = null
                    refreshFileList()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toastMessage.value = "Rename error: ${e.message}"
                }
            }
        }
    }

    fun deleteProject(meta: ProjectMetadata) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(meta.uriString)
                val deleted = storageManager.deleteProject(uri)
                withContext(Dispatchers.Main) {
                    if (deleted) {
                        if (_currentFileUri.value == uri) {
                            _currentFileUri.value = null
                        }
                        toastMessage.value = "Deleted ${meta.name}.idraw"
                    } else {
                        toastMessage.value = "Could not delete file"
                    }
                    showDeleteConfirmDialog.value = null
                    refreshFileList()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toastMessage.value = "Delete error: ${e.message}"
                }
            }
        }
    }

    fun exportPdf(name: String, scope: PdfExporter.ExportScope, viewW: Float, viewH: Float) {
        val cleanName = name.trim().removeSuffix(".pdf")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = storageManager.exportPdf(_project.value, cleanName, scope, viewW, viewH)
                withContext(Dispatchers.Main) {
                    showPdfExportDialog.value = false
                    toastMessage.value = "PDF exported: $cleanName.pdf"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toastMessage.value = "Failed to export PDF: ${e.message}"
                }
            }
        }
    }
}
