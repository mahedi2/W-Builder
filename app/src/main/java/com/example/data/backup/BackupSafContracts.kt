package com.example.data.backup

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Storage Access Framework & zero-permission Photo Picker Compose launcher helpers.
 */
@Composable
fun rememberWBuilderExportLauncher(
    onDestinationPicked: (Uri) -> Unit
): ManagedActivityResultLauncher<String, Uri?> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            onDestinationPicked(uri)
        }
    }
}

@Composable
fun rememberWBuilderImportLauncher(
    onArchivePicked: (Uri) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onArchivePicked(uri)
        }
    }
}

@Composable
fun rememberFontPickerLauncher(
    onFontPicked: (Uri) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onFontPicked(uri)
        }
    }
}

@Composable
fun rememberImagePhotoPickerLauncher(
    onImagePicked: (Uri) -> Unit
): ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImagePicked(uri)
        }
    }
}

object WBuilderMimeFilters {
    val archiveTypes = arrayOf(
        "application/octet-stream",
        "application/zip",
        "application/json",
        "text/plain",
        "*/*"
    )

    val fontTypes = arrayOf(
        "font/ttf",
        "font/otf",
        "application/x-font-ttf",
        "application/x-font-opentype",
        "application/octet-stream",
        "*/*"
    )
}
