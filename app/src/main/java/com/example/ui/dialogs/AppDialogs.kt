package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawProject
import com.example.model.ImageObject
import com.example.model.ProjectMetadata
import com.example.storage.PdfExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SaveAsDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialName.removeSuffix(".idraw")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF0284C7))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Drawing Project",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter a name for your drawing. It will be saved as an editable .idraw file.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    trailingIcon = { Text(".idraw", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth().testTag("save_as_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.testTag("confirm_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameDialog(
    projectMeta: ProjectMetadata,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(projectMeta.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename Project",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Choose a new name for '${projectMeta.name}.idraw':",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    trailingIcon = { Text(".idraw", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    projectMeta: ProjectMetadata,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete Project?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to permanently delete '${projectMeta.name}.idraw'? This action cannot be undone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FileInfoDialog(
    meta: ProjectMetadata,
    project: DrawProject?,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy 'at' HH:mm:ss", Locale.getDefault()) }
    val dateStr = if (meta.modifiedTime > 0) dateFormat.format(Date(meta.modifiedTime)) else "Unknown"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Project Information",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow(label = "Filename", value = "${meta.name}.idraw")
                InfoRow(label = "File Format", value = "Infinite Draw Package (.idraw)")
                InfoRow(label = "File Size", value = "${(meta.sizeBytes / 1024).coerceAtLeast(1)} KB")
                InfoRow(label = "Last Modified", value = dateStr)
                if (project != null && meta.name == project.name) {
                    InfoRow(label = "Strokes", value = "${project.strokes.size}")
                    InfoRow(label = "Shapes", value = "${project.shapes.size}")
                    InfoRow(label = "Embedded Images", value = "${project.images.size}")
                    InfoRow(label = "Canvas Style", value = project.background.displayName)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PdfExportDialog(
    projectName: String,
    onExportSave: (fileName: String, scope: PdfExporter.ExportScope) -> Unit,
    onSharePdf: () -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf(projectName.removeSuffix(".idraw")) }
    var selectedScope by remember { mutableStateOf(PdfExporter.ExportScope.ENTIRE_DRAWING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export to PDF",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Export your infinite canvas drawing to a high-resolution vector PDF document.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("PDF Filename") },
                    singleLine = true,
                    trailingIcon = { Text(".pdf", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth().testTag("pdf_filename_input")
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Export Region",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedScope = PdfExporter.ExportScope.ENTIRE_DRAWING
                    }
                ) {
                    RadioButton(
                        selected = selectedScope == PdfExporter.ExportScope.ENTIRE_DRAWING,
                        onClick = { selectedScope = PdfExporter.ExportScope.ENTIRE_DRAWING },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                    )
                    Text(
                        text = "Entire Drawing (Auto-fit all artwork)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedScope = PdfExporter.ExportScope.CURRENT_VIEW
                    }
                ) {
                    RadioButton(
                        selected = selectedScope == PdfExporter.ExportScope.CURRENT_VIEW,
                        onClick = { selectedScope = PdfExporter.ExportScope.CURRENT_VIEW },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                    )
                    Text(
                        text = "Current Canvas Viewport",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSharePdf,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share PDF via System Sheet")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (fileName.isNotBlank()) onExportSave(fileName.trim(), selectedScope) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.testTag("confirm_pdf_export_button")
            ) {
                Text("Export & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HexColorInputDialog(
    initialHex: String,
    onApplyHex: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var hexText by remember { mutableStateOf(initialHex.removePrefix("#")) }
    var parsedColor by remember {
        mutableStateOf<Long?>(
            try {
                android.graphics.Color.parseColor("#$hexText").toLong() and 0xFFFFFFFFL
            } catch (e: Exception) {
                null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Custom Hex Color",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a 6-digit (#RRGGBB) or 8-digit (#AARRGGBB) hex code:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (parsedColor != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(parsedColor!!))
                                .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = {
                            hexText = it.removePrefix("#")
                            parsedColor = try {
                                val s = if (hexText.length == 6) "#FF$hexText" else "#$hexText"
                                android.graphics.Color.parseColor(s).toLong() and 0xFFFFFFFFL
                            } catch (e: Exception) {
                                null
                            }
                        },
                        label = { Text("Hex Code") },
                        prefix = { Text("#") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedColor?.let { onApplyHex(it) }
                },
                enabled = parsedColor != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AutosaveRecoveryDialog(
    onRestore: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDiscard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF0284C7))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Restore Unsaved Session?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = "Infinite Draw detected an unsaved drawing session from your last launch. Would you like to restore your progress?",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.testTag("restore_autosave_button")
            ) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
        }
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Infinite Draw Guide",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HelpPoint(
                    title = "Infinite 2D Workspace",
                    desc = "Draw without artificial page boundaries. Pan and zoom across miles of canvas space."
                )
                HelpPoint(
                    title = "1 Finger = Draw / Manipulate",
                    desc = "Touch and drag to draw vector strokes, create geometric shapes, or move selected objects."
                )
                HelpPoint(
                    title = "2 Fingers = Pan & Pinch Zoom",
                    desc = "Pinch with two fingers to zoom smoothly into details (centered on your fingers) or pan simultaneously without accidental marks."
                )
                HelpPoint(
                    title = "Directional Nudge & Moving",
                    desc = "Use the Left, Up, Down, and Right buttons on the selection bar to nudge images and shapes with pixel accuracy."
                )
                HelpPoint(
                    title = "Multiple Image Management",
                    desc = "Tap on images to select and cycle between overlapping pictures. Use the Images manager to view all pictures, focus, reorder layers, or delete."
                )
                HelpPoint(
                    title = "Storage & Custom .idraw Files",
                    desc = "Projects are stored locally using Android Storage Access Framework (SAF). Full vector stroke data, images, and settings are preserved."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun HelpPoint(title: String, desc: String) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = desc,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}

/**
 * Dialog to manage and select multiple images placed on the infinite canvas.
 */
@Composable
fun ImageManagerDialog(
    images: List<ImageObject>,
    selectedImageId: String?,
    onSelectImage: (ImageObject) -> Unit,
    onMoveLayerUp: (ImageObject) -> Unit,
    onMoveLayerDown: (ImageObject) -> Unit,
    onDeleteImage: (ImageObject) -> Unit,
    onImportNewImage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Canvas Images (${images.size})",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Quick Import New Image button
                Button(
                    onClick = onImportNewImage,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_import_new_image_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Another Image")
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No images currently placed on this canvas.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Text(
                        text = "Tap an image to select and center on it, or reorder layers:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(images) { index, img ->
                            val isSelected = img.id == selectedImageId
                            ImageItemRow(
                                index = index,
                                totalCount = images.size,
                                image = img,
                                isSelected = isSelected,
                                onSelect = {
                                    onSelectImage(img)
                                    onDismiss()
                                },
                                onMoveUp = { onMoveLayerUp(img) },
                                onMoveDown = { onMoveLayerDown(img) },
                                onDelete = { onDeleteImage(img) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ImageItemRow(
    index: Int,
    totalCount: Int,
    image: ImageObject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF0284C7).copy(alpha = 0.20f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Thumbnail or placeholder
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            if (image.bitmap != null) {
                Image(
                    bitmap = image.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Image #${index + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0284C7))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Text(
                text = "${image.width.toInt()} × ${image.height.toInt()} px • Pos: (${image.x.toInt()}, ${image.y.toInt()})",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Buttons: Focus, Move Layer, Delete
        IconButton(
            onClick = onSelect,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.CenterFocusStrong,
                contentDescription = "Select & Center",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onMoveUp,
            enabled = index < totalCount - 1,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.ArrowUpward,
                contentDescription = "Bring Forward",
                tint = if (index < totalCount - 1) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onMoveDown,
            enabled = index > 0,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.ArrowDownward,
                contentDescription = "Send Backward",
                tint = if (index > 0) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Image",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
