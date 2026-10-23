package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawProject
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
                Text("Save Drawing Project")
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter a name for your drawing. It will be saved as an editable .idraw file.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
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
        title = { Text("Rename Project") },
        text = {
            Column {
                Text(
                    text = "Choose a new name for '${projectMeta.name}.idraw':",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
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
                Text("Delete Project?")
            }
        },
        text = {
            Text("Are you sure you want to permanently delete '${projectMeta.name}.idraw'? This action cannot be undone.")
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
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF0284C7))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Project Information")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
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
                Text("Export to PDF")
            }
        },
        text = {
            Column {
                Text(
                    text = "Export your infinite canvas drawing to a high-resolution vector PDF document.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
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
                Text(text = "Export Region", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedScope == PdfExporter.ExportScope.ENTIRE_DRAWING,
                        onClick = { selectedScope = PdfExporter.ExportScope.ENTIRE_DRAWING },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                    )
                    Text("Entire Drawing (Auto-fit all artwork)", fontSize = 13.sp, color = Color(0xFF1E293B))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedScope == PdfExporter.ExportScope.CURRENT_VIEW,
                        onClick = { selectedScope = PdfExporter.ExportScope.CURRENT_VIEW },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                    )
                    Text("Current Canvas Viewport", fontSize = 13.sp, color = Color(0xFF1E293B))
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
        title = { Text("Custom Hex Color") },
        text = {
            Column {
                Text("Enter a 6-digit (#RRGGBB) or 8-digit (#AARRGGBB) hex code:", fontSize = 13.sp, color = Color(0xFF64748B))
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
                Text("Restore Unsaved Session?")
            }
        },
        text = {
            Text(
                "Infinite Draw detected an unsaved drawing session from your last launch. Would you like to restore your progress?",
                fontSize = 13.sp,
                color = Color(0xFF475569)
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
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF0284C7))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Infinite Draw Guide")
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
                    title = "Stylus Hardware",
                    desc = "Hardware stylus pressure sensitivity is automatically supported on compatible Android devices."
                )
                HelpPoint(
                    title = "Selection & Transforms",
                    desc = "Use the Select tool to tap any shape or image to scale, rotate, duplicate, or delete."
                )
                HelpPoint(
                    title = "Storage & Custom .idraw Files",
                    desc = "Projects are stored locally using Android Storage Access Framework (SAF). Full vector stroke data, images, and settings are preserved."
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun HelpPoint(title: String, desc: String) {
    Column {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Text(text = desc, fontSize = 12.sp, color = Color(0xFF64748B), lineHeight = 16.sp)
    }
}
