package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.local.converter.WBuilderJson
import com.example.data.repository.CustomComponentRepository
import com.example.data.repository.WidgetRepository
import com.example.domain.model.CustomComponent
import com.example.domain.model.LayerPayload
import com.example.domain.model.WidgetLayer
import com.example.domain.model.WidgetProject
import com.example.domain.model.WBuilderArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class ImportConflictStrategy {
    GENERATE_NEW_IDS,
    OVERWRITE_BY_ID
}

data class ExportSummary(
    val projectCount: Int,
    val componentCount: Int,
    val bundledAssetCount: Int,
    val suggestedFileName: String
)

data class ImportSummary(
    val importedProjects: List<WidgetProject>,
    val importedComponents: List<CustomComponent>,
    val restoredAssetCount: Int
)

/**
 * Self-contained `.wbuilder` backup/restore engine.
 *
 * Exports a lightweight ZIP archive (with `.wbuilder` extension) containing:
 * - `manifest.json`: human-readable [WBuilderArchive] JSON
 * - `assets/<key>`: any referenced local images, AI artwork, or custom `.ttf`/`.otf` fonts
 *
 * On import, auto-detects both ZIP-packaged `.wbuilder` archives and plain JSON `.wbuilder`/`.json`
 * files (including `embeddedAssetsBase64` payloads), restores bundled assets into app-private storage,
 * and rewrites asset paths before saving into Room.
 */
class WBuilderArchiveManager(
    private val context: Context,
    private val widgetRepository: WidgetRepository,
    private val componentRepository: CustomComponentRepository,
    private val assetStorage: AssetStorageManager
) {
    private val json = WBuilderJson.instance

    companion object {
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val ASSETS_DIR = "assets/"
        private const val MAX_ARCHIVE_ENTRY_BYTES = 20 * 1024 * 1024L // 20 MB per entry guard
    }

    suspend fun exportProjectToUri(
        project: WidgetProject,
        destinationUri: Uri,
        includeReferencedComponents: Boolean = true
    ): Result<ExportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val referencedComponentIds = project.layers
                .mapNotNull { (it.payload as? LayerPayload.CustomComponentRef)?.componentId }
                .toSet()

            val components = if (includeReferencedComponents && referencedComponentIds.isNotEmpty()) {
                componentRepository.getAllComponentsOnce().filter { it.id in referencedComponentIds }
            } else {
                emptyList()
            }

            writeArchiveToUri(
                destinationUri = destinationUri,
                projects = listOf(project),
                components = components,
                suggestedFileName = "${sanitizeFileName(project.name)}.wbuilder"
            )
        }
    }

    suspend fun exportFullStudioBackupToUri(
        destinationUri: Uri
    ): Result<ExportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val projects = widgetRepository.getAllProjectsOnce()
            val components = componentRepository.getAllComponentsOnce()
            writeArchiveToUri(
                destinationUri = destinationUri,
                projects = projects,
                components = components,
                suggestedFileName = "WBuilder_Studio_Backup.wbuilder"
            )
        }
    }

    suspend fun importFromUri(
        sourceUri: Uri,
        conflictStrategy: ImportConflictStrategy = ImportConflictStrategy.GENERATE_NEW_IDS
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.readBytes()
            } ?: error("Could not read selected .wbuilder file.")

            require(rawBytes.isNotEmpty()) { "Selected archive is empty." }

            val extractedAssets = mutableMapOf<String, ByteArray>()
            val manifestJsonString: String

            if (isZipStream(rawBytes)) {
                var manifestText: String? = null
                ZipInputStream(BufferedInputStream(rawBytes.inputStream())).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val entryName = entry.name
                            val entryBytes = readBoundedEntryBytes(zipIn)
                            when {
                                entryName == MANIFEST_ENTRY -> {
                                    manifestText = entryBytes.toString(Charsets.UTF_8)
                                }
                                entryName.startsWith(ASSETS_DIR) -> {
                                    val assetKey = entryName.removePrefix(ASSETS_DIR)
                                    if (assetKey.isNotBlank()) {
                                        extractedAssets[assetKey] = entryBytes
                                    }
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
                manifestJsonString = manifestText
                    ?: error("Invalid .wbuilder package: missing $MANIFEST_ENTRY.")
            } else {
                manifestJsonString = rawBytes.toString(Charsets.UTF_8)
            }

            val archive = json.decodeFromString(WBuilderArchive.serializer(), manifestJsonString)

            // Also materialize any base64-embedded assets from plain JSON archives
            archive.embeddedAssetsBase64.forEach { (key, b64) ->
                if (key !in extractedAssets) {
                    runCatching { Base64.decode(b64, Base64.DEFAULT) }
                        .getOrNull()
                        ?.let { extractedAssets[key] = it }
                }
            }

            // Restore extracted assets into app-private storage and map old keys -> new local paths
            val restoredAssetPathMap = mutableMapOf<String, String>()
            extractedAssets.forEach { (assetKey, bytes) ->
                val isFont = assetKey.endsWith(".ttf", ignoreCase = true) ||
                    assetKey.endsWith(".otf", ignoreCase = true)
                val localPath = assetStorage.writeImportedAssetBytes(assetKey, bytes, isFont = isFont)
                restoredAssetPathMap[assetKey] = localPath
            }

            // Remap component IDs if GENERATE_NEW_IDS
            val componentIdMap = mutableMapOf<String, String>()
            val now = System.currentTimeMillis()
            val restoredComponents = archive.customComponents.map { component ->
                val newCompId = if (conflictStrategy == ImportConflictStrategy.GENERATE_NEW_IDS) {
                    UUID.randomUUID().toString().also { componentIdMap[component.id] = it }
                } else {
                    component.id.also { componentIdMap[component.id] = it }
                }
                component.copy(
                    id = newCompId,
                    layers = component.layers.map { layer ->
                        rewriteLayerAssets(
                            layer = layer.copy(projectId = newCompId),
                            restoredAssetPathMap = restoredAssetPathMap,
                            componentIdMap = componentIdMap
                        )
                    },
                    updatedAt = now
                )
            }

            val restoredProjects = archive.projects.map { project ->
                val newProjectId = if (conflictStrategy == ImportConflictStrategy.GENERATE_NEW_IDS) {
                    UUID.randomUUID().toString()
                } else {
                    project.id
                }
                val newName = if (conflictStrategy == ImportConflictStrategy.GENERATE_NEW_IDS &&
                    widgetRepository.getProjectOnce(project.id) != null
                ) {
                    "${project.name} (Imported)"
                } else {
                    project.name
                }
                project.copy(
                    id = newProjectId,
                    name = newName,
                    previewAssetPath = project.previewAssetPath?.let { restoredAssetPathMap[it] ?: it },
                    layers = project.layers.map { layer ->
                        rewriteLayerAssets(
                            layer = layer.copy(
                                id = if (conflictStrategy == ImportConflictStrategy.GENERATE_NEW_IDS) {
                                    UUID.randomUUID().toString()
                                } else {
                                    layer.id
                                },
                                projectId = newProjectId
                            ),
                            restoredAssetPathMap = restoredAssetPathMap,
                            componentIdMap = componentIdMap
                        )
                    },
                    updatedAt = now
                )
            }

            if (restoredComponents.isNotEmpty()) {
                componentRepository.saveComponents(restoredComponents)
            }
            restoredProjects.forEach { project ->
                widgetRepository.saveProject(project)
            }

            ImportSummary(
                importedProjects = restoredProjects,
                importedComponents = restoredComponents,
                restoredAssetCount = restoredAssetPathMap.size
            )
        }
    }

    private fun writeArchiveToUri(
        destinationUri: Uri,
        projects: List<WidgetProject>,
        components: List<CustomComponent>,
        suggestedFileName: String
    ): ExportSummary {
        val assetKeyToFile = linkedMapOf<String, File>()

        val portableComponents = components.map { component ->
            component.copy(
                layers = component.layers.map { layer ->
                    collectAndRelativizeLayerAssets(layer, assetKeyToFile)
                }
            )
        }

        val portableProjects = projects.map { project ->
            val previewKey = project.previewAssetPath?.let { registerAssetFile(it, assetKeyToFile) }
            project.copy(
                previewAssetPath = previewKey ?: project.previewAssetPath,
                layers = project.layers.map { layer ->
                    collectAndRelativizeLayerAssets(layer, assetKeyToFile)
                }
            )
        }

        val archive = WBuilderArchive(
            schemaVersion = 1,
            exportedAt = System.currentTimeMillis(),
            projects = portableProjects,
            customComponents = portableComponents
        )
        val manifestBytes = json.encodeToString(WBuilderArchive.serializer(), archive)
            .toByteArray(Charsets.UTF_8)

        context.contentResolver.openOutputStream(destinationUri)?.use { rawOut ->
            ZipOutputStream(BufferedOutputStream(rawOut)).use { zipOut ->
                zipOut.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zipOut.write(manifestBytes)
                zipOut.closeEntry()

                assetKeyToFile.forEach { (assetKey, file) ->
                    if (file.exists() && file.isFile) {
                        zipOut.putNextEntry(ZipEntry("$ASSETS_DIR$assetKey"))
                        file.inputStream().use { input -> input.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }
            }
        } ?: error("Unable to open destination file for writing.")

        return ExportSummary(
            projectCount = portableProjects.size,
            componentCount = portableComponents.size,
            bundledAssetCount = assetKeyToFile.size,
            suggestedFileName = suggestedFileName
        )
    }

    private fun collectAndRelativizeLayerAssets(
        layer: WidgetLayer,
        assetKeyToFile: MutableMap<String, File>
    ): WidgetLayer {
        val fontKey = layer.style.fontAssetPath?.let { registerAssetFile(it, assetKeyToFile) }
        val updatedStyle = if (fontKey != null) {
            layer.style.copy(fontAssetPath = fontKey)
        } else {
            layer.style
        }

        val updatedPayload = when (val payload = layer.payload) {
            is LayerPayload.ImageMedia -> {
                val imgKey = payload.assetPathOrUri?.let { registerAssetFile(it, assetKeyToFile) }
                if (imgKey != null) payload.copy(assetPathOrUri = imgKey) else payload
            }
            is LayerPayload.CustomComponentRef -> {
                payload.copy(
                    embeddedLayers = payload.embeddedLayers.map {
                        collectAndRelativizeLayerAssets(it, assetKeyToFile)
                    }
                )
            }
            is LayerPayload.OverlapGroup -> {
                payload.copy(
                    embeddedLayers = payload.embeddedLayers.map {
                        collectAndRelativizeLayerAssets(it, assetKeyToFile)
                    }
                )
            }
            is LayerPayload.StackGroup -> {
                payload.copy(
                    embeddedLayers = payload.embeddedLayers.map {
                        collectAndRelativizeLayerAssets(it, assetKeyToFile)
                    }
                )
            }
            else -> payload
        }

        return layer.copy(style = updatedStyle, payload = updatedPayload)
    }

    private fun rewriteLayerAssets(
        layer: WidgetLayer,
        restoredAssetPathMap: Map<String, String>,
        componentIdMap: Map<String, String>
    ): WidgetLayer {
        val rewrittenFont = layer.style.fontAssetPath?.let { restoredAssetPathMap[it] ?: it }
        val updatedStyle = layer.style.copy(fontAssetPath = rewrittenFont)

        val updatedPayload = when (val payload = layer.payload) {
            is LayerPayload.ImageMedia -> {
                val rewrittenImage = payload.assetPathOrUri?.let { restoredAssetPathMap[it] ?: it }
                payload.copy(assetPathOrUri = rewrittenImage)
            }
            is LayerPayload.CustomComponentRef -> {
                payload.copy(
                    componentId = componentIdMap[payload.componentId] ?: payload.componentId,
                    embeddedLayers = payload.embeddedLayers.map {
                        rewriteLayerAssets(it, restoredAssetPathMap, componentIdMap)
                    }
                )
            }
            is LayerPayload.OverlapGroup -> {
                payload.copy(
                    embeddedLayers = payload.embeddedLayers.map {
                        rewriteLayerAssets(it, restoredAssetPathMap, componentIdMap)
                    }
                )
            }
            is LayerPayload.StackGroup -> {
                payload.copy(
                    embeddedLayers = payload.embeddedLayers.map {
                        rewriteLayerAssets(it, restoredAssetPathMap, componentIdMap)
                    }
                )
            }
            else -> payload
        }

        return layer.copy(style = updatedStyle, payload = updatedPayload)
    }

    private fun registerAssetFile(
        pathOrUri: String,
        assetKeyToFile: MutableMap<String, File>
    ): String? {
        val file = assetStorage.resolveExistingAssetFile(pathOrUri) ?: return null
        val existingKey = assetKeyToFile.entries.firstOrNull { it.value.absolutePath == file.absolutePath }?.key
        if (existingKey != null) return existingKey

        val key = "${assetKeyToFile.size}_${file.name}"
        assetKeyToFile[key] = file
        return key
    }

    private fun isZipStream(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() && // 'P'
            bytes[1] == 0x4B.toByte() && // 'K'
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()

    private fun readBoundedEntryBytes(zipIn: ZipInputStream): ByteArray {
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var total = 0L
        while (true) {
            val read = zipIn.read(buffer)
            if (read <= 0) break
            total += read
            require(total <= MAX_ARCHIVE_ENTRY_BYTES) {
                "Archive entry exceeds 20 MB safety limit."
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun sanitizeFileName(raw: String): String =
        raw.trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "WBuilder_Widget" }
}
