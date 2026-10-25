package com.example.storage

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.model.DrawProject
import com.example.model.ProjectMetadata
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Handles device storage project management for Death Note,
 * custom .idraw file persistence, PDF export, and crash recovery autosave.
 * Supports user-selected internal storage locations (Downloads, Documents, etc.)
 * via Storage Access Framework with persistable permissions.
 */
class SafStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("death_note_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        const val EXTENSION_IDRAW = ".idraw"
        const val MIME_IDRAW = "application/octet-stream"
        const val MIME_PDF = "application/pdf"
        private const val AUTOSAVE_FILE = "autosave_recovery.idraw"
        private const val PREF_TRACKED_URIS = "tracked_project_uris"
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
        return "Internal Storage"
    }

    /**
     * Legacy helper kept for backward compatibility with ViewModels.
     */
    fun getActiveFolderUri(): Uri? = null

    fun setActiveFolderUri(uri: Uri) {
        addTrackedUri(uri)
    }

    /**
     * Resolves the human-readable display name of a file from its Uri.
     */
    fun getUriDisplayName(uri: Uri): String {
        if (uri.scheme == "file") {
            return File(uri.path ?: "").name
        }
        var name: String? = null
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        name = cursor.getString(idx)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cursor exceptions
        }
        return name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Drawing$EXTENSION_IDRAW"
    }

    fun addTrackedUri(uri: Uri) {
        val current = getTrackedUris().toMutableSet()
        current.add(uri.toString())
        prefs.edit().putStringSet(PREF_TRACKED_URIS, current).apply()
    }

    fun removeTrackedUri(uri: Uri) {
        val current = getTrackedUris().toMutableSet()
        current.remove(uri.toString())
        prefs.edit().putStringSet(PREF_TRACKED_URIS, current).apply()
    }

    fun getTrackedUris(): Set<String> {
        return prefs.getStringSet(PREF_TRACKED_URIS, emptySet()) ?: emptySet()
    }

    /**
     * List all .idraw project files stored in device storage or chosen via SAF.
     */
    fun listProjectFiles(): List<ProjectMetadata> {
        val result = mutableListOf<ProjectMetadata>()
        val seenUris = mutableSetOf<String>()

        // 1. Check all user-chosen project URIs on internal storage
        for (uriStr in getTrackedUris()) {
            try {
                val uri = Uri.parse(uriStr)
                val doc = DocumentFile.fromSingleUri(context, uri)
                if (doc != null && !doc.exists()) {
                    removeTrackedUri(uri)
                    continue
                }

                val displayName = getUriDisplayName(uri)
                val cleanName = displayName.removeSuffix(EXTENSION_IDRAW)
                var size = if (doc != null && doc.length() > 0) doc.length() else 0L
                var modified = if (doc != null && doc.lastModified() > 0) doc.lastModified() else System.currentTimeMillis()

                var thumb: Bitmap? = null
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        thumb = ProjectSerializer.extractThumbnail(stream)
                    }
                } catch (e: Exception) {
                    // Ignore corrupted thumbnails
                }

                result.add(
                    ProjectMetadata(
                        id = uriStr,
                        name = cleanName,
                        uriString = uriStr,
                        modifiedTime = modified,
                        sizeBytes = size,
                        thumbnailBitmap = thumb
                    )
                )
                seenUris.add(uriStr)
            } catch (e: Exception) {
                // Ignore invalid URIs
            }
        }

        // 2. Also check fallback internal folder files
        val internalDir = getInternalProjectsDir()
        val files = internalDir.listFiles { f ->
            f.isFile && f.name.endsWith(EXTENSION_IDRAW, ignoreCase = true) && !f.name.startsWith(".")
        } ?: emptyArray()

        for (f in files) {
            val fUri = Uri.fromFile(f).toString()
            if (!seenUris.contains(fUri)) {
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
                        uriString = fUri,
                        modifiedTime = f.lastModified(),
                        sizeBytes = f.length(),
                        thumbnailBitmap = thumb
                    )
                )
            }
        }

        return result.sortedByDescending { it.modifiedTime }
    }

    /**
     * Saves a project directly to a user-chosen URI via SAF.
     */
    fun saveProjectToUri(
        project: DrawProject,
        uri: Uri,
        thumbnailBitmap: Bitmap? = null
    ): Boolean {
        return try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Not all providers support persistable permissions; write may still succeed
            }

            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return false
            stream.use { out ->
                ProjectSerializer.serialize(project, out, thumbnailBitmap)
            }
            addTrackedUri(uri)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves a project directly to device internal storage (fallback).
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
            val ok = saveProjectToUri(project, existingUri, thumbnailBitmap)
            if (ok) return existingUri
        }

        val dir = getInternalProjectsDir()
        val targetFile = File(dir, cleanName)
        FileOutputStream(targetFile).use { out ->
            ProjectSerializer.serialize(project, out, thumbnailBitmap)
        }
        val fileUri = Uri.fromFile(targetFile)
        addTrackedUri(fileUri)
        return fileUri
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
        addTrackedUri(uri)
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
            if (file.renameTo(newFile)) {
                removeTrackedUri(uri)
                val newUri = Uri.fromFile(newFile)
                addTrackedUri(newUri)
                newUri
            } else null
        } else {
            val doc = DocumentFile.fromSingleUri(context, uri)
            if (doc != null && doc.renameTo(cleanName)) {
                removeTrackedUri(uri)
                addTrackedUri(doc.uri)
                doc.uri
            } else null
        }
    }

    /**
     * Delete a project file.
     */
    fun deleteProject(uri: Uri): Boolean {
        removeTrackedUri(uri)
        return if (uri.scheme == "file") {
            File(uri.path!!).delete()
        } else {
            val doc = DocumentFile.fromSingleUri(context, uri)
            doc?.delete() ?: false
        }
    }

    /**
     * Export drawing to PDF directly to a user-chosen destination Uri.
     */
    fun exportPdfToUri(
        project: DrawProject,
        uri: Uri,
        scope: PdfExporter.ExportScope,
        viewW: Float,
        viewH: Float
    ): Boolean {
        return try {
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return false
            stream.use { out ->
                PdfExporter.exportToPdf(project, out, scope, viewW, viewH)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
