package com.example.ui.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.backup.rememberImagePhotoPickerLauncher
import com.google.firebase.ai.type.AspectRatio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface AiImageUiState {
    data object Idle : AiImageUiState
    data object Loading : AiImageUiState
    data class Success(val bitmap: Bitmap, val prompt: String) : AiImageUiState
    data class Error(val message: String) : AiImageUiState
}

enum class AiAspectOption(val label: String, val ratio: AspectRatio) {
    SQUARE("1:1", AspectRatio.SQUARE_1x1),
    LANDSCAPE("16:9", AspectRatio.LANDSCAPE_16x9),
    CARD("4:3", AspectRatio.LANDSCAPE_4x3),
    PORTRAIT("3:4", AspectRatio.PORTRAIT_3x4)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiImageStudioSheet(
    initialBitmapPath: String?,
    aiState: AiImageUiState,
    onGenerateFromPrompt: (prompt: String, aspect: AspectRatio) -> Unit,
    onEditExistingImage: (sourceBitmap: Bitmap, instruction: String) -> Unit,
    onApplyBitmapToCanvas: (bitmap: Bitmap, prompt: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var prompt by remember { mutableStateOf("") }
    var selectedAspect by remember { mutableStateOf(AiAspectOption.SQUARE) }
    var referenceBitmap by remember(initialBitmapPath) {
        mutableStateOf(
            initialBitmapPath?.let { File(it) }?.takeIf { it.exists() }?.let {
                BitmapFactory.decodeFile(it.absolutePath)
            }
        )
    }

    val photoPicker = rememberImagePhotoPickerLauncher { uri ->
        scope.launch {
            val decoded = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
            if (decoded != null) {
                referenceBitmap = decoded
            }
        }
    }

    val quickPrompts = remember {
        listOf(
            "Frosted dark glass gradient background with subtle cyan aurora glow",
            "Minimalist isometric 3D sun and clouds weather icon on dark slate",
            "Retro-futuristic synthwave grid horizon widget backdrop",
            "Soft pastel botanical abstract wallpaper texture"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Artwork Studio",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Create or edit widget visuals with Gemini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close AI studio")
                }
            }

            // Optional reference image for Image-to-Image editing
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (referenceBitmap != null) {
                        Image(
                            bitmap = referenceBitmap!!.asImageBitmap(),
                            contentDescription = "Reference image for editing",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Edit Mode Active", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Your prompt will transform this image",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { referenceBitmap = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear reference image")
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Text-to-Image Mode", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Optionally pick a photo to edit with text",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("ai_pick_reference_image")
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pick Image")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = {
                    Text(
                        if (referenceBitmap != null) "Describe how to edit this image"
                        else "Describe widget artwork or background"
                    )
                },
                placeholder = { Text("e.g. Minimalist midnight aurora glassmorphism texture") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_image_prompt_input")
            )

            if (referenceBitmap == null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiAspectOption.entries.forEach { option ->
                        FilterChip(
                            selected = selectedAspect == option,
                            onClick = { selectedAspect = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            // Quick starter prompt chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPrompts.forEach { preset ->
                    FilterChip(
                        selected = prompt == preset,
                        onClick = { prompt = preset },
                        label = { Text(preset.take(28) + "…") }
                    )
                }
            }

            Button(
                onClick = {
                    val trimmed = prompt.trim()
                    if (trimmed.isNotEmpty()) {
                        val ref = referenceBitmap
                        if (ref != null) {
                            onEditExistingImage(ref, trimmed)
                        } else {
                            onGenerateFromPrompt(trimmed, selectedAspect.ratio)
                        }
                    }
                },
                enabled = prompt.isNotBlank() && aiState !is AiImageUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("ai_generate_button")
            ) {
                Icon(
                    imageVector = if (referenceBitmap != null) Icons.Rounded.Edit else Icons.Rounded.AutoAwesome,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (referenceBitmap != null) "Edit Image with AI" else "Generate Image with AI")
            }

            when (aiState) {
                is AiImageUiState.Idle -> Unit
                is AiImageUiState.Loading -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Synthesizing artwork (may take 30–60s)...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                is AiImageUiState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(
                            bitmap = aiState.bitmap.asImageBitmap(),
                            contentDescription = "AI generated widget artwork",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { referenceBitmap = aiState.bitmap },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Refine / Edit")
                            }
                            Button(
                                onClick = { onApplyBitmapToCanvas(aiState.bitmap, aiState.prompt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("ai_apply_to_canvas_button")
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Apply to Canvas")
                            }
                        }
                    }
                }
                is AiImageUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}
