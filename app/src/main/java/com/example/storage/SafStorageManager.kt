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
 * Handles Android Storage Access Framework (SAF) folder-based project management,
 * custom .idraw file persistence, PDF export, and crash recovery autosave.
 */
class SafStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("infinite_draw_prefs", Context.MODE_PRIVATE)

    companion object {
        const val EXTENSION_IDRAW = ".idraw"
        const val MIME_IDRAW = "application/octet-stream"
        const val MIME_PDF = "application/pdf"
        private const val PREF_TREE_URI = "active_tree_uri"
        private const val PREF_LAST_PROJECT_NAME = "last_project_name"
        private const val AUTOSAVE_FILE = "autosave_recovery.idraw"
    }

    /**
     * Get the currently active tree folder URI, if any.
     */
    fun getActiveFolderUri(): Uri? {
        val uriStr = prefs.getString(PREF_TREE_URI, null) ?: return null
        return try {
            Uri.parse(uriStr)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save the active tree folder URI and persist permissions.
     */
    fun setActiveFolderUri(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            // Some providers or re-grants might throw, ignore safely
        }
        prefs.edit().putString(PREF_TREE_URI, uri.toString()).apply()
    }

    /**
     * Get a human-readable name for the active folder.
     */
    fun getActiveFolderName(): String {
        val treeUri = getActiveFolderUri()
        if (treeUri != null) {
            val doc = DocumentFile.fromTreeUri(context, treeUri)
            if (doc != null && doc.name != null) {
                return doc.name!!
            }
            return treeUri.lastPathSegment ?: "Selected Folder"
        }
        return "Internal Storage / Drawings"
    }

    /**
     * List only supported .idraw files in the active folder or local fallback.
     */
    fun listProjectFiles(): List<ProjectMetadata> {
        val result = mutableListOf<ProjectMetadata>()
        val treeUri = getActiveFolderUri()

        if (treeUri != null) {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null && rootDoc.isDirectory && rootDoc.canRead()) {
                val files = rootDoc.listFiles()
                for (file in files) {
                    val name = file.name ?: continue
                    if (name.endsWith(EXTENSION_IDRAW, ignoreCase = true) && !name.startsWith(".")) {
                        val cleanName = name.removeSuffix(EXTENSION_IDRAW)
                        var thumb: Bitmap? = null
                        try {
                            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                                thumb = ProjectSerializer.extractThumbnail(stream)
                            }
                        } catch (e: Exception) {
                            // Ignored thumbnail preview error
                        }
                        result.add(
                            ProjectMetadata(
                                id = file.uri.toString(),
                                name = cleanName,
                                uriString = file.uri.toString(),
                                modifiedTime = file.lastModified(),
                                sizeBytes = file.length(),
                                thumbnailBitmap = thumb
                            )
                        )
                    }
                }
            }
        } else {
            // Use internal app drawings directory
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
                    // Ignore
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
        }

        return result.sortedByDescending { it.modifiedTime }
    }

    /**
     * Saves a project directly to an existing URI or creates a new file in the active folder.
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

        if (existingUri != null) {
            // Overwrite existing file
            context.contentResolver.openOutputStream(existingUri, "wt")?.use { out ->
                ProjectSerializer.serialize(project, out, thumbnailBitmap)
            } ?: throw IllegalStateException("Could not open output stream for $existingUri")
            return existingUri
        }

        val treeUri = getActiveFolderUri()
        if (treeUri != null) {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IllegalStateException("Cannot access folder: $treeUri")
            val existing = rootDoc.findFile(cleanName)
            val docFile = existing ?: rootDoc.createFile(MIME_IDRAW, cleanName)
                ?: throw IllegalStateException("Failed to create file: $cleanName")

            context.contentResolver.openOutputStream(docFile.uri, "wt")?.use { out ->
                ProjectSerializer.serialize(project, out, thumbnailBitmap)
            } ?: throw IllegalStateException("Could not write to file: ${docFile.uri}")
            return docFile.uri
        } else {
            // Save to internal storage
            val dir = getInternalProjectsDir()
            val targetFile = File(dir, cleanName)
            FileOutputStream(targetFile).use { out ->
                ProjectSerializer.serialize(project, out, thumbnailBitmap)
            }
            return Uri.fromFile(targetFile)
        }
    }

    /**
     * Load a project from URI.
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
     * Export drawing to PDF file in active folder or internal documents.
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

        val treeUri = getActiveFolderUri()
        if (treeUri != null) {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            val docFile = rootDoc?.findFile(pdfName) ?: rootDoc?.createFile(MIME_PDF, pdfName)
                ?: throw IllegalStateException("Could not create PDF file in selected folder")

            context.contentResolver.openOutputStream(docFile.uri, "wt")?.use { out ->
                PdfExporter.exportToPdf(project, out, scope, viewW, viewH)
            } ?: throw IllegalStateException("Cannot write to PDF")
            return docFile.uri
        } else {
            val dir = getInternalProjectsDir()
            val file = File(dir, pdfName)
            FileOutputStream(file).use { out ->
                PdfExporter.exportToPdf(project, out, scope, viewW, viewH)
            }
            return Uri.fromFile(file)
        }
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

    private fun getInternalProjectsDir(): File {
        val dir = File(context.filesDir, "Drawings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
