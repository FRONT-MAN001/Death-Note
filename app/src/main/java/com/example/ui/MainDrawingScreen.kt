package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.ThumbnailGenerator
import com.example.model.ActiveTool
import com.example.model.BrushType
import com.example.model.ImageObject
import com.example.model.ProjectMetadata
import com.example.storage.PdfExporter
import com.example.ui.canvas.InfiniteCanvas
import com.example.ui.components.BrushPopover
import com.example.ui.components.ColorPickerPopover
import com.example.ui.components.GlassToolbar
import com.example.ui.components.SelectionActionsBar
import com.example.ui.components.ShapePopover
import com.example.ui.components.TopNavigationBar
import com.example.ui.dialogs.AutosaveRecoveryDialog
import com.example.ui.dialogs.DeleteConfirmDialog
import com.example.ui.dialogs.FileInfoDialog
import com.example.ui.dialogs.HelpDialog
import com.example.ui.dialogs.HexColorInputDialog
import com.example.ui.dialogs.ImageManagerDialog
import com.example.ui.dialogs.PdfExportDialog
import com.example.ui.dialogs.RenameDialog
import com.example.ui.dialogs.SaveAsDialog
import com.example.ui.drawer.FileDrawer
import com.example.viewmodel.DrawingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

@Composable
fun MainDrawingScreen(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val project by viewModel.project.collectAsState()
    val transform by viewModel.transform.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val brushType by viewModel.brushType.collectAsState()
    val brushSize by viewModel.brushSize.collectAsState()
    val brushColor by viewModel.brushColor.collectAsState()
    val brushOpacity by viewModel.brushOpacity.collectAsState()
    val selectedShapeType by viewModel.selectedShapeType.collectAsState()
    val shapeStrokeWidth by viewModel.shapeStrokeWidth.collectAsState()
    val shapeStrokeColor by viewModel.shapeStrokeColor.collectAsState()
    val shapeFillColor by viewModel.shapeFillColor.collectAsState()
    val shapeIsFilled by viewModel.shapeIsFilled.collectAsState()
    val shapeOpacity by viewModel.shapeOpacity.collectAsState()
    val selectedShape by viewModel.selectedShape.collectAsState()
    val selectedImage by viewModel.selectedImage.collectAsState()
    val activeFolderName by viewModel.activeFolderName.collectAsState()
    val projectFiles by viewModel.projectFiles.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()

    // Popover visibilities
    var showBrushPopover by remember { mutableStateOf(false) }
    var showColorPopover by remember { mutableStateOf(false) }
    var showShapePopover by remember { mutableStateOf(false) }
    var showHexDialog by remember { mutableStateOf(false) }
    var showImageManagerDialog by remember { mutableStateOf(false) }

    // Dialog visibilities
    val showSaveAsDialog by viewModel.showSaveAsDialog.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsState()
    val showFileInfoDialog by viewModel.showFileInfoDialog.collectAsState()
    val showPdfExportDialog by viewModel.showPdfExportDialog.collectAsState()
    val showAutosaveRecoveryDialog by viewModel.showAutosaveRecoveryDialog.collectAsState()
    val showHelpDialog by viewModel.showHelpDialog.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.toastMessage.value = null
        }
    }

    // Android SAF Folder Picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectActiveFolder(uri)
        }
    }

    // Android SAF File Picker for .idraw
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.openProject(uri)
        }
    }

    // Image Picker for inserting onto canvas
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val baos = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.PNG, 90, baos)
                        val bytes = baos.toByteArray()
                        val fileName = "img_${System.currentTimeMillis()}.png"

                        // Place centered in current canvas view
                        val centerCanvas = transform.screenToCanvas(500f, 800f)
                        val targetW = bmp.width.toFloat().coerceAtMost(800f)
                        val targetH = (targetW / bmp.width) * bmp.height

                        val imgObj = ImageObject(
                            id = UUID.randomUUID().toString(),
                            fileName = fileName,
                            x = centerCanvas.x - targetW / 2f,
                            y = centerCanvas.y - targetH / 2f,
                            width = targetW,
                            height = targetH,
                            bitmap = bmp
                        )
                        withContext(Dispatchers.Main) {
                            viewModel.commitImage(imgObj, bytes)
                            viewModel.setActiveTool(ActiveTool.SELECT)
                            viewModel.selectObjectAt(centerCanvas)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        viewModel.toastMessage.value = "Failed to import image: ${e.message}"
                    }
                }
            }
        }
    }

    // Handle Back Press gracefully
    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            showBrushPopover -> showBrushPopover = false
            showColorPopover -> showColorPopover = false
            showShapePopover -> showShapePopover = false
            selectedShape != null || selectedImage != null -> viewModel.clearSelection()
            else -> {
                // Exit or minimize app
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FileDrawer(
                activeFolderName = activeFolderName,
                projectFiles = projectFiles,
                currentProjectName = project.name,
                currentBackground = project.background,
                onBrowseFolderClick = {
                    folderPickerLauncher.launch(null)
                },
                onNewDrawingClick = {
                    scope.launch { drawerState.close() }
                    viewModel.newDrawing()
                },
                onOpenFilePickerClick = {
                    scope.launch { drawerState.close() }
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                onSaveClick = {
                    val thumb = ThumbnailGenerator.generateThumbnail(project)
                    viewModel.saveCurrentProject(thumb)
                    scope.launch { drawerState.close() }
                },
                onSaveAsClick = {
                    viewModel.showSaveAsDialog.value = true
                    scope.launch { drawerState.close() }
                },
                onExportPdfClick = {
                    viewModel.showPdfExportDialog.value = true
                    scope.launch { drawerState.close() }
                },
                onOpenProject = { uri ->
                    viewModel.openProject(uri)
                    scope.launch { drawerState.close() }
                },
                onRenameClick = { meta ->
                    viewModel.showRenameDialog.value = meta
                },
                onDeleteClick = { meta ->
                    viewModel.showDeleteConfirmDialog.value = meta
                },
                onShareProjectClick = { meta ->
                    val thumb = ThumbnailGenerator.generateThumbnail(project)
                    val intent = viewModel.storageManager.createShareProjectIntent(
                        Uri.parse(meta.uriString),
                        project,
                        thumb
                    )
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Drawing Project"))
                },
                onSharePdfClick = {
                    val intent = viewModel.storageManager.createSharePdfIntent(project, 1080f, 1920f)
                    context.startActivity(android.content.Intent.createChooser(intent, "Share PDF Export"))
                },
                onFileInfoClick = { meta ->
                    viewModel.showFileInfoDialog.value = meta
                },
                onBackgroundSelected = { bg ->
                    viewModel.setCanvasBackground(bg)
                },
                onHelpClick = {
                    viewModel.showHelpDialog.value = true
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val screenW = maxWidth.value
            val screenH = maxHeight.value

            // 1. Infinite Canvas (Drawing Area)
            InfiniteCanvas(
                project = project,
                transform = transform,
                activeTool = activeTool,
                brushType = brushType,
                brushSize = brushSize,
                brushColor = brushColor,
                brushOpacity = brushOpacity,
                selectedShapeType = selectedShapeType,
                shapeStrokeWidth = shapeStrokeWidth,
                shapeStrokeColor = shapeStrokeColor,
                shapeFillColor = shapeFillColor,
                shapeIsFilled = shapeIsFilled,
                shapeOpacity = shapeOpacity,
                selectedShape = selectedShape,
                selectedImage = selectedImage,
                onTransformChange = { pX, pY, z ->
                    viewModel.setTransform(pX, pY, z)
                },
                onStrokeCommit = { stroke ->
                    viewModel.commitStroke(stroke)
                },
                onShapeCommit = { shape ->
                    viewModel.commitShape(shape)
                },
                onObjectSelect = { point ->
                    viewModel.selectObjectAt(point)
                },
                onSelectedMove = { dx, dy ->
                    viewModel.moveSelected(dx, dy)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dismiss popovers when touching outside
            if (showBrushPopover || showColorPopover || showShapePopover) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            showBrushPopover = false
                            showColorPopover = false
                            showShapePopover = false
                        }
                )
            }

            // 2. Floating Top Navigation Bar
            TopNavigationBar(
                projectName = project.name,
                folderName = activeFolderName,
                hasUnsavedChanges = hasUnsavedChanges,
                zoomPercentage = transform.zoom,
                onMenuClick = {
                    scope.launch { drawerState.open() }
                },
                onQuickSaveClick = {
                    val thumb = ThumbnailGenerator.generateThumbnail(project)
                    viewModel.saveCurrentProject(thumb)
                },
                onResetZoomClick = {
                    viewModel.resetZoom()
                },
                onZoomToFitClick = {
                    viewModel.zoomToFit(1080f, 1920f)
                },
                onRecenterClick = {
                    viewModel.resetZoom()
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 3. Selection Actions Floating Bar
            AnimatedVisibility(
                visible = selectedShape != null || selectedImage != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            ) {
                SelectionActionsBar(
                    onScaleUp = { viewModel.scaleSelected(1.15f) },
                    onScaleDown = { viewModel.scaleSelected(0.85f) },
                    onRotateCw = { viewModel.rotateSelected(15f) },
                    onRotateCcw = { viewModel.rotateSelected(-15f) },
                    onDuplicate = { viewModel.duplicateSelected() },
                    onDelete = { viewModel.deleteSelected() },
                    onDeselect = { viewModel.clearSelection() },
                    onMoveUp = { viewModel.moveSelected(0f, -25f) },
                    onMoveDown = { viewModel.moveSelected(0f, 25f) },
                    onMoveLeft = { viewModel.moveSelected(-25f, 0f) },
                    onMoveRight = { viewModel.moveSelected(25f, 0f) },
                    selectedImageIndex = project.images.indexOfFirst { it.id == selectedImage?.id },
                    totalImages = project.images.size,
                    onNextImage = { viewModel.selectNextImage() },
                    onPrevImage = { viewModel.selectPreviousImage() },
                    onOpenImageManager = { showImageManagerDialog = true }
                )
            }

            // 4. Popovers anchored above Bottom Toolbar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 84.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = showBrushPopover,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    BrushPopover(
                        selectedBrush = brushType,
                        brushSize = brushSize,
                        brushOpacity = brushOpacity,
                        brushColor = brushColor,
                        onBrushSelected = {
                            viewModel.setBrushType(it)
                            showBrushPopover = false
                        },
                        onSizeChange = { viewModel.setBrushSize(it) },
                        onOpacityChange = { viewModel.setBrushOpacity(it) },
                        onDismiss = { showBrushPopover = false }
                    )
                }

                AnimatedVisibility(
                    visible = showColorPopover,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    ColorPickerPopover(
                        currentColor = brushColor,
                        recentColors = recentColors,
                        onColorSelected = { viewModel.setBrushColor(it) },
                        onHexDialogRequested = { showHexDialog = true },
                        onDismiss = { showColorPopover = false }
                    )
                }

                AnimatedVisibility(
                    visible = showShapePopover,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    ShapePopover(
                        selectedShapeType = selectedShapeType,
                        strokeWidth = shapeStrokeWidth,
                        isFilled = shapeIsFilled,
                        strokeColor = shapeStrokeColor,
                        fillColor = shapeFillColor,
                        onShapeTypeSelected = {
                            viewModel.setSelectedShapeType(it)
                            showShapePopover = false
                        },
                        onStrokeWidthChange = { viewModel.setShapeStrokeWidth(it) },
                        onFillToggled = { viewModel.setShapeIsFilled(it) },
                        onStrokeColorSelected = { viewModel.setShapeStrokeColor(it) },
                        onFillColorSelected = { viewModel.setShapeFillColor(it) },
                        onDismiss = { showShapePopover = false }
                    )
                }
            }

            // 5. Floating Bottom Glass Toolbar
            GlassToolbar(
                activeTool = activeTool,
                brushType = brushType,
                brushColor = brushColor,
                brushSize = brushSize,
                canUndo = canUndo,
                canRedo = canRedo,
                onBrushClick = {
                    viewModel.setActiveTool(ActiveTool.BRUSH)
                    showBrushPopover = !showBrushPopover
                    showColorPopover = false
                    showShapePopover = false
                },
                onEraserClick = {
                    viewModel.setBrushType(BrushType.ERASER)
                    showBrushPopover = false
                    showColorPopover = false
                    showShapePopover = false
                },
                onPanClick = {
                    viewModel.setActiveTool(ActiveTool.PAN)
                    showBrushPopover = false
                    showColorPopover = false
                    showShapePopover = false
                },
                onColorClick = {
                    showColorPopover = !showColorPopover
                    showBrushPopover = false
                    showShapePopover = false
                },
                onShapeClick = {
                    viewModel.setActiveTool(ActiveTool.SHAPE)
                    showShapePopover = !showShapePopover
                    showBrushPopover = false
                    showColorPopover = false
                },
                onSelectClick = {
                    viewModel.setActiveTool(ActiveTool.SELECT)
                    showBrushPopover = false
                    showColorPopover = false
                    showShapePopover = false
                },
                onImageClick = {
                    showBrushPopover = false
                    showColorPopover = false
                    showShapePopover = false
                    if (project.images.isNotEmpty()) {
                        showImageManagerDialog = true
                    } else {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                onUndoClick = { viewModel.undo() },
                onRedoClick = { viewModel.redo() },
                onClearClick = { viewModel.clearCanvas() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }
    }

    // Interactive Dialogs (Only show soft keyboard here!)
    if (showSaveAsDialog) {
        SaveAsDialog(
            initialName = project.name,
            onConfirm = { name ->
                val thumb = ThumbnailGenerator.generateThumbnail(project)
                viewModel.saveProjectAs(name, thumb)
            },
            onDismiss = { viewModel.showSaveAsDialog.value = false }
        )
    }

    showRenameDialog?.let { meta ->
        RenameDialog(
            projectMeta = meta,
            onConfirm = { newName ->
                viewModel.renameProject(meta, newName)
            },
            onDismiss = { viewModel.showRenameDialog.value = null }
        )
    }

    showDeleteConfirmDialog?.let { meta ->
        DeleteConfirmDialog(
            projectMeta = meta,
            onConfirm = { viewModel.deleteProject(meta) },
            onDismiss = { viewModel.showDeleteConfirmDialog.value = null }
        )
    }

    showFileInfoDialog?.let { meta ->
        FileInfoDialog(
            meta = meta,
            project = project,
            onDismiss = { viewModel.showFileInfoDialog.value = null }
        )
    }

    if (showPdfExportDialog) {
        PdfExportDialog(
            projectName = project.name,
            onExportSave = { fileName, scope ->
                viewModel.exportPdf(fileName, scope, 1080f, 1920f)
            },
            onSharePdf = {
                val intent = viewModel.storageManager.createSharePdfIntent(project, 1080f, 1920f)
                context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
            },
            onDismiss = { viewModel.showPdfExportDialog.value = false }
        )
    }

    if (showHexDialog) {
        val hexStr = String.format("#%08X", brushColor)
        HexColorInputDialog(
            initialHex = hexStr,
            onApplyHex = {
                viewModel.setBrushColor(it)
                showHexDialog = false
            },
            onDismiss = { showHexDialog = false }
        )
    }

    if (showAutosaveRecoveryDialog) {
        AutosaveRecoveryDialog(
            onRestore = { viewModel.restoreAutosave() },
            onDiscard = { viewModel.discardAutosave() }
        )
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { viewModel.showHelpDialog.value = false })
    }

    if (showImageManagerDialog) {
        ImageManagerDialog(
            images = project.images,
            selectedImageId = selectedImage?.id,
            onSelectImage = { viewModel.focusOnImage(it) },
            onMoveLayerUp = { viewModel.moveImageLayerUp(it) },
            onMoveLayerDown = { viewModel.moveImageLayerDown(it) },
            onDeleteImage = { viewModel.deleteImage(it) },
            onImportNewImage = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showImageManagerDialog = false }
        )
    }
}
