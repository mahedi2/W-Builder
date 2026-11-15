package com.example.data.backup

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AssetStorageManager(private val context: Context) {

    private val assetsRoot: File
        get() = File(context.filesDir, "widget_assets").apply { mkdirs() }

    private val imagesDir: File
        get() = File(assetsRoot, "images").apply { mkdirs() }

    private val fontsDir: File
        get() = File(assetsRoot, "fonts").apply { mkdirs() }

    suspend fun copyImageFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val extension = resolveExtension(uri, defaultExt = "png")
            val destFile = File(imagesDir, "img_${UUID.randomUUID()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open selected image stream.")
            destFile.absolutePath
        }
    }

    suspend fun saveBitmapAsAsset(bitmap: Bitmap, prefix: String = "ai_art"): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val destFile = File(imagesDir, "${prefix}_${UUID.randomUUID()}.png")
                FileOutputStream(destFile).use { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        "Failed to compress bitmap to PNG."
                    }
                }
                destFile.absolutePath
            }
        }

    suspend fun copyFontFromUri(uri: Uri): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val displayName = resolveDisplayName(uri) ?: "CustomFont.ttf"
            val ext = displayName.substringAfterLast('.', "ttf").lowercase()
            val safeExt = if (ext in setOf("ttf", "otf")) ext else "ttf"
            val cleanBaseName = displayName.substringBeforeLast('.')
                .replace(Regex("[^A-Za-z0-9_ -]"), "")
                .ifBlank { "CustomFont" }
            val destFile = File(fontsDir, "font_${UUID.randomUUID()}.$safeExt")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open selected font file.")
            cleanBaseName to destFile.absolutePath
        }
    }

    fun resolveExistingAssetFile(pathOrUri: String?): File? {
        if (pathOrUri.isNullOrBlank()) return null
        val file = File(pathOrUri)
        return if (file.exists() && file.isFile) file else null
    }

    fun writeImportedAssetBytes(archiveRelativeName: String, bytes: ByteArray, isFont: Boolean): String {
        val safeName = File(archiveRelativeName).name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val targetDir = if (isFont) fontsDir else imagesDir
        val destFile = File(targetDir, "imp_${UUID.randomUUID()}_$safeName")
        val canonicalRoot = targetDir.canonicalPath + File.separator
        require(destFile.canonicalPath.startsWith(canonicalRoot)) {
            "Unsafe asset path rejected: $archiveRelativeName"
        }
        destFile.writeBytes(bytes)
        return destFile.absolutePath
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
    }

    private fun resolveExtension(uri: Uri, defaultExt: String): String {
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
        return when {
            "png" in mime -> "png"
            "webp" in mime -> "webp"
            "jpeg" in mime || "jpg" in mime -> "jpg"
            else -> defaultExt
        }
    }
}
