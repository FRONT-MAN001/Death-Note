package com.example.storage

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.model.DrawProject
import com.example.model.ProjectMetadata
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Handles device internal/external storage project management for Death Note,
 * custom .idraw file persistence, PDF export, and crash recovery autosave.
 * Uses direct app storage without requiring problematic SAF folder permission trees.
 */
class SafStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("death_note_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        const val EXTENSION_IDRAW = ".idraw"
        const val MIME_IDRAW = "application/octet-stream"
        const val MIME_PDF = "application/pdf"
        private const val AUTOSAVE_FILE = "autosave_recovery.idraw"
    }

    /**
     * Get the internal project directory on device storage.
     * Uses getExternalFilesDir("Drawings") with fallback to filesDir/"Drawings".
     * Always accessible, private, permanent, and requires zero SAF prompts.
     */
    fun getInternalProjectsDir(): File {
        val externalDir = context.getExternalFilesDir("Drawings")
        val dir = if (externalDir != null && (externalDir.exists() || externalDir.mkdirs())) {
            externalDir
        } else {
            File(context.filesDir, "Drawings")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Returns a human-friendly name for the working storage location.
     */
    fun getActiveFolderName(): String {
        return "Internal Storage / Drawings"
    }

    /**
     * Legacy helper kept for backward compatibility with ViewModels.
     */
    fun getActiveFolderUri(): Uri? = null

    fun setActiveFolderUri(uri: Uri) {
        // No-op: Internal storage is used directly
    }

    /**
     * List all .idraw project files stored in device storage.
     */
    fun listProjectFiles(): List<ProjectMetadata> {
        val result = mutableListOf<ProjectMetadata>()
        val internalDir = getInternalProjectsDir()
        val files = internalDir.listFiles { f ->
            f.isFile && f.name.endsWith(EXTENSION_IDRAW, ignoreCase = true) && !f.name.startsWith(".")
        } ?: emptyArray()

        for (f in files) {
            val cleanName = f.name.removeSuffix(EXTENSION_IDRAW)
            var thumb: Bitmap? = null
            try {
                FileInputStream(f).use { stream ->
                    thumb = ProjectSerializer.extractThumbnail(stream)
                }
            } catch (e: Exception) {
                // Ignore corrupted thumbnails
            }
            result.add(
                ProjectMetadata(
                    id = f.absolutePath,
                    name = cleanName,
                    uriString = Uri.fromFile(f).toString(),
                    modifiedTime = f.lastModified(),
                    sizeBytes = f.length(),
                    thumbnailBitmap = thumb
                )
            )
        }

        return result.sortedByDescending { it.modifiedTime }
    }

    /**
     * Saves a project directly to device internal storage.
     */
    fun saveProject(
        project: DrawProject,
        existingUri: Uri? = null,
        fileName: String = project.name,
        thumbnailBitmap: Bitmap? = null
    ): Uri {
        val cleanName = if (fileName.endsWith(EXTENSION_IDRAW, ignoreCase = true)) {
            fileName
        } else {
            "$fileName$EXTENSION_IDRAW"
        }

        if (existingUri != null && existingUri.scheme == "content") {
            try {
                context.contentResolver.openOutputStream(existingUri, "wt")?.use { out ->
                    ProjectSerializer.serialize(project, out, thumbnailBitmap)
                }
                return existingUri
            } catch (e: Exception) {
                // Fall back to saving in internal storage below
            }
        }

        val dir = getInternalProjectsDir()
        val targetFile = File(dir, cleanName)
        FileOutputStream(targetFile).use { out ->
            ProjectSerializer.serialize(project, out, thumbnailBitmap)
        }
        return Uri.fromFile(targetFile)
    }

    /**
     * Load a project from URI (supports both file:// and content://).
     */
    fun loadProject(uri: Uri): DrawProject {
        val stream = if (uri.scheme == "file") {
            FileInputStream(File(uri.path!!))
        } else {
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream for $uri")
        }
        return stream.use { ProjectSerializer.deserialize(it) }
    }

    /**
     * Rename a project file.
     */
    fun renameProject(uri: Uri, newBaseName: String): Uri? {
        val cleanName = if (newBaseName.endsWith(EXTENSION_IDRAW, ignoreCase = true)) {
            newBaseName
        } else {
            "$newBaseName$EXTENSION_IDRAW"
        }

        return if (uri.scheme == "file") {
            val file = File(uri.path!!)
            val newFile = File(file.parentFile, cleanName)
            if (file.renameTo(newFile)) Uri.fromFile(newFile) else null
        } else {
            val doc = DocumentFile.fromSingleUri(context, uri)
            if (doc != null && doc.renameTo(cleanName)) {
                doc.uri
            } else null
        }
    }

    /**
     * Delete a project file.
     */
    fun deleteProject(uri: Uri): Boolean {
        return if (uri.scheme == "file") {
            File(uri.path!!).delete()
        } else {
            val doc = DocumentFile.fromSingleUri(context, uri)
            doc?.delete() ?: false
        }
    }

    /**
     * Export drawing to PDF file in internal device storage.
     */
    fun exportPdf(
        project: DrawProject,
        targetFileName: String,
        scope: PdfExporter.ExportScope,
        viewW: Float,
        viewH: Float
    ): Uri {
        val pdfName = if (targetFileName.endsWith(".pdf", ignoreCase = true)) {
            targetFileName
        } else {
            "$targetFileName.pdf"
        }

        val dir = getInternalProjectsDir()
        val file = File(dir, pdfName)
        FileOutputStream(file).use { out ->
            PdfExporter.exportToPdf(project, out, scope, viewW, viewH)
        }
        return Uri.fromFile(file)
    }

    /**
     * Prepares an Intent to share an .idraw project file via Android share sheet.
     */
    fun createShareProjectIntent(uri: Uri, project: DrawProject, thumb: Bitmap?): Intent {
        val shareFile = File(context.cacheDir, "${project.name}$EXTENSION_IDRAW")
        FileOutputStream(shareFile).use { out ->
            ProjectSerializer.serialize(project, out, thumb)
        }
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = MIME_IDRAW
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "${project.name}$EXTENSION_IDRAW")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Prepares an Intent to share an exported PDF file via Android share sheet.
     */
    fun createSharePdfIntent(project: DrawProject, viewW: Float, viewH: Float): Intent {
        val pdfFile = File(context.cacheDir, "${project.name}.pdf")
        FileOutputStream(pdfFile).use { out ->
            PdfExporter.exportToPdf(project, out, PdfExporter.ExportScope.ENTIRE_DRAWING, viewW, viewH)
        }
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = MIME_PDF
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "${project.name}.pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Crash recovery autosave.
     */
    fun saveAutosave(project: DrawProject) {
        try {
            val file = File(context.filesDir, AUTOSAVE_FILE)
            FileOutputStream(file).use { out ->
                ProjectSerializer.serialize(project, out, null)
            }
        } catch (e: Exception) {
            // Non-fatal
        }
    }

    fun hasAutosave(): Boolean {
        val file = File(context.filesDir, AUTOSAVE_FILE)
        return file.exists() && file.length() > 64
    }

    fun loadAutosave(): DrawProject? {
        return try {
            val file = File(context.filesDir, AUTOSAVE_FILE)
            if (!file.exists()) return null
            FileInputStream(file).use { ProjectSerializer.deserialize(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun discardAutosave() {
        val file = File(context.filesDir, AUTOSAVE_FILE)
        if (file.exists()) file.delete()
    }
}
