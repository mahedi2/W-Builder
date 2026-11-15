package com.example.ui.editor

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.GroupWork
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.LayerPayload
import com.example.domain.model.LayerType
import com.example.domain.model.WidgetDimension
import com.example.ui.ai.AiImageStudioSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCanvasEditorScreen(
    editorViewModel: EditorViewModel,
    onBack: () -> Unit
) {
    val uiState by editorViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showComponentPicker by remember { mutableStateOf(false) }
    var showInspectorSheet by remember { mutableStateOf(false) }
    var showAiStudioSheet by remember { mutableStateOf(false) }
    var showSaveComponentDialog by remember { mutableStateOf(false) }
    var showCustomDimensionDialog by remember { mutableStateOf(false) }
    var editingXmlLayer by remember { mutableStateOf<Pair<String, LayerPayload.CustomXmlSnippet>?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) editorViewModel.refreshLiveData()
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) editorViewModel.refreshLiveData()
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            editorViewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to dashboard"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.isComponentMode) {
                                "Custom Component · ${uiState.canvasWidthDp.toInt()}×${uiState.canvasHeightDp.toInt()}dp"
                            } else {
                                "${uiState.dimension.label} · ${uiState.layers.size} layers"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = editorViewModel::undo,
                        enabled = uiState.canUndo,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("editor_undo_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = editorViewModel::redo,
                        enabled = uiState.canRedo,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("editor_redo_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Redo")
                    }
                    Button(
                        onClick = { editorViewModel.saveAndApply(pinToHomeScreen = !uiState.isComponentMode) },
                        enabled = !uiState.isSaving,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("save_and_apply_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isComponentMode) Icons.Rounded.Save else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (uiState.isComponentMode) "Save" else "Save & Apply")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { showComponentPicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("editor_add_component_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showInspectorSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("editor_inspector_button")
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Style", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showAiStudioSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("editor_ai_art_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Art", maxLines = 1)
                    }

                    IconButton(
                        onClick = { showSaveComponentDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_as_custom_component_button")
                    ) {
                        Icon(
                            Icons.Rounded.LibraryAdd,
                            contentDescription = "Save as Custom Component",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Studio Toolbar: Dimension presets + Grid / Snap / Multi-select + Code XML quick action
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!uiState.isComponentMode) {
                    items(WidgetDimension.entries.filter { it != WidgetDimension.CUSTOM }) { dim ->
                        FilterChip(
                            selected = uiState.dimension == dim,
                            onClick = { editorViewModel.setDimension(dim) },
                            label = { Text(dim.label) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.dimension == WidgetDimension.CUSTOM,
                            onClick = { showCustomDimensionDialog = true },
                            label = {
                                Text(
                                    if (uiState.dimension == WidgetDimension.CUSTOM) {
                                        "Custom ${uiState.canvasWidthDp.toInt()}×${uiState.canvasHeightDp.toInt()}"
                                    } else {
                                        "Custom"
                                    }
                                )
                            },
                            modifier = Modifier.testTag("editor_custom_dimension_chip")
                        )
                    }
                }
                item {
                    FilterChip(
                        selected = uiState.showGridLines,
                        onClick = editorViewModel::toggleGridLines,
                        leadingIcon = { Icon(Icons.Rounded.GridOn, null, modifier = Modifier.size(16.dp)) },
                        label = { Text(if (uiState.snapEnabled) "Snap 8dp" else "Grid") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.isWindowTransparent,
                        onClick = editorViewModel::toggleTransparentWindow,
                        leadingIcon = { Icon(Icons.Rounded.Opacity, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Transparent") },
                        modifier = Modifier.testTag("editor_transparent_window_chip")
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.isMultiSelectMode,
                        onClick = editorViewModel::toggleMultiSelectMode,
                        leadingIcon = { Icon(Icons.Rounded.SelectAll, null, modifier = Modifier.size(16.dp)) },
                        label = {
                            Text(
                                if (uiState.isMultiSelectMode) {
                                    "Multi (${uiState.selectedLayerIds.size})"
                                } else {
                                    "Multi-Select"
                                }
                            )
                        },
                        modifier = Modifier.testTag("editor_multi_select_chip")
                    )
                }
                if (uiState.selectedLayerIds.size >= 2) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { editorViewModel.groupSelectedLayers(asStackGroup = false) },
                            leadingIcon = { Icon(Icons.Rounded.GroupWork, null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Overlap Group (${uiState.selectedLayerIds.size})") },
                            modifier = Modifier.testTag("editor_group_layers_button")
                        )
                    }
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { editorViewModel.groupSelectedLayers(asStackGroup = true) },
                            leadingIcon = { Icon(Icons.Rounded.ViewAgenda, null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Stack Group (${uiState.selectedLayerIds.size})") },
                            modifier = Modifier.testTag("editor_stack_group_layers_button")
                        )
                    }
                }
                val primaryGroup = uiState.primarySelectedLayer
                val hasEmbeddedChildren = when (val gp = primaryGroup?.payload) {
                    is LayerPayload.CustomComponentRef -> gp.embeddedLayers.isNotEmpty()
                    is LayerPayload.OverlapGroup -> gp.embeddedLayers.isNotEmpty()
                    is LayerPayload.StackGroup -> gp.embeddedLayers.isNotEmpty()
                    else -> false
                }
                if (primaryGroup != null && hasEmbeddedChildren) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { editorViewModel.ungroupSelectedLayer(primaryGroup.id) },
                            leadingIcon = { Icon(Icons.Rounded.LayersClear, null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Ungroup") },
                            modifier = Modifier.testTag("editor_ungroup_layer_button")
                        )
                    }
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val selected = uiState.primarySelectedLayer
                            if (selected != null && selected.payload is LayerPayload.CustomXmlSnippet) {
                                editingXmlLayer = selected.id to selected.payload
                            } else {
                                editorViewModel.addBuiltInLayer(LayerType.CUSTOM_XML_SNIPPET)
                            }
                        },
                        leadingIcon = { Icon(Icons.Rounded.Code, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Code / XML") },
                        modifier = Modifier.testTag("editor_code_xml_chip")
                    )
                }
            }

            // Interactive Drag-and-Drop Grid-Snapping Visual Canvas
            InteractiveCanvasSurface(
                uiState = uiState,
                onSelectLayer = editorViewModel::selectLayer,
                onDragStart = editorViewModel::beginDragOrResize,
                onMoveLayer = { id, dx, dy ->
                    editorViewModel.moveLayerBy(id, dx, dy, applySnap = uiState.snapEnabled)
                },
                onResizeLayer = { id, dw, dh ->
                    editorViewModel.resizeLayerBy(id, dw, dh, applySnap = uiState.snapEnabled)
                },
                onRotateLayer = { id, deltaDeg ->
                    editorViewModel.rotateLayerBy(id, deltaDeg, recordUndo = false)
                },
                onMediaAction = editorViewModel::dispatchMediaControl,
                onVolumeAction = editorViewModel::adjustVolumeControl,
                onQuickToggle = editorViewModel::executeTapAction,
                modifier = Modifier.weight(1f)
            )

            // Selected Layer Quick Actions + Quick Transform Bar + Layer Hierarchy Strip
            val selectedLayer = uiState.primarySelectedLayer
            if (selectedLayer != null) {
                QuickTransformBar(
                    selectedLayer = selectedLayer,
                    onRotateBy = { delta -> editorViewModel.rotateLayerBy(selectedLayer.id, delta, recordUndo = true) },
                    onSetRotation = { deg -> editorViewModel.setLayerRotation(selectedLayer.id, deg, recordUndo = false) },
                    onSetOpacity = { alpha -> editorViewModel.setLayerOpacity(selectedLayer.id, alpha, recordUndo = false) },
                    onSelectIconKey = { key ->
                        if (selectedLayer.payload is LayerPayload.VectorIcon) {
                            editorViewModel.updateLayer(
                                selectedLayer.copy(payload = selectedLayer.payload.copy(symbolKey = key))
                            )
                        }
                    },
                    onCenterH = { editorViewModel.centerLayerHorizontally(selectedLayer.id) },
                    onCenterV = { editorViewModel.centerLayerVertically(selectedLayer.id) },
                    onAlignEdge = { edge -> editorViewModel.alignLayerToEdge(selectedLayer.id, edge) },
                    onNudge = { dx, dy -> editorViewModel.nudgeLayer(selectedLayer.id, dx, dy) }
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedLayer.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.9f)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1.4f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { editorViewModel.sendLayerToBack(selectedLayer.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("send_to_back_button")
                            ) {
                                Icon(Icons.Rounded.VerticalAlignBottom, contentDescription = "Send to back", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { editorViewModel.sendLayerBackward(selectedLayer.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Send backward", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { editorViewModel.bringLayerForward(selectedLayer.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Bring forward", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { editorViewModel.bringLayerToFront(selectedLayer.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("bring_to_front_button")
                            ) {
                                Icon(Icons.Rounded.VerticalAlignTop, contentDescription = "Bring to front", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    editorViewModel.updateLayer(selectedLayer.copy(isLocked = !selectedLayer.isLocked))
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (selectedLayer.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                    contentDescription = "Toggle lock",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    editorViewModel.updateLayer(selectedLayer.copy(isVisible = !selectedLayer.isVisible))
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (selectedLayer.isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { editorViewModel.duplicateLayer(selectedLayer.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicate layer", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { editorViewModel.deleteLayer(selectedLayer.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete layer",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Layer Stack Strip
            if (uiState.layers.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.layers.sortedBy { it.style.zIndex }, key = { it.id }) { layer ->
                        val isSelected = layer.id in uiState.selectedLayerIds
                        Surface(
                            onClick = { editorViewModel.selectLayer(layer.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Layers,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = layer.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showComponentPicker) {
        ComponentPickerSheet(
            savedComponents = uiState.savedComponents,
            onPickBuiltIn = { type ->
                editorViewModel.addBuiltInLayer(type)
                when (type) {
                    LayerType.WEATHER_CARD,
                    LayerType.WEATHER_LOCATION,
                    LayerType.WEATHER_FORECAST,
                    LayerType.LOCATION_BADGE -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    LayerType.CALENDAR_MINI,
                    LayerType.CALENDAR_AGENDA -> calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    else -> Unit
                }
            },
            onPickCustomComponent = editorViewModel::insertCustomComponent,
            onDismiss = { showComponentPicker = false }
        )
    }

    if (showInspectorSheet) {
        PropertyInspectorSheet(
            uiState = uiState,
            onUpdateLayer = { editorViewModel.updateLayer(it, recordUndo = false) },
            onUpdateCanvasBackground = editorViewModel::updateCanvasBackground,
            onImportFontUri = editorViewModel::importCustomFontForSelectedLayer,
            onImportImageUri = editorViewModel::importImageForSelectedLayer,
            onSearchCities = editorViewModel::searchWeatherCities,
            onSelectCity = editorViewModel::applyCityToWeatherLayer,
            onOpenAiArtwork = { showAiStudioSheet = true },
            onOpenCustomXmlEditor = { snippet ->
                uiState.primarySelectedLayer?.id?.let { id ->
                    editingXmlLayer = id to snippet
                }
            },
            onConvertToCustomCode = { layerId ->
                editorViewModel.convertLayerToCustomCode(layerId)?.let { snippet ->
                    editingXmlLayer = layerId to snippet
                }
            },
            onAddChildToGroup = editorViewModel::addChildToGroup,
            onUngroupLayer = editorViewModel::ungroupSelectedLayer,
            onTestTapAction = editorViewModel::executeTapAction,
            onDismiss = { showInspectorSheet = false }
        )
    }

    if (showAiStudioSheet) {
        val selectedImgPath = (uiState.primarySelectedLayer?.payload as? LayerPayload.ImageMedia)?.assetPathOrUri
        AiImageStudioSheet(
            initialBitmapPath = selectedImgPath,
            aiState = uiState.aiImageState,
            onGenerateFromPrompt = editorViewModel::generateAiImage,
            onEditExistingImage = editorViewModel::editAiImage,
            onApplyBitmapToCanvas = { bmp, prompt ->
                editorViewModel.applyAiBitmapToCanvas(bmp, prompt)
                showAiStudioSheet = false
            },
            onDismiss = { showAiStudioSheet = false }
        )
    }

    editingXmlLayer?.let { (layerId, snippet) ->
        CustomXmlEditorDialog(
            initialMarkup = snippet.rawMarkup,
            initialIsJson = snippet.isJsonFormat,
            telemetry = uiState.telemetry,
            weather = uiState.weather,
            onApply = { markup, isJson ->
                val target = uiState.layers.firstOrNull { it.id == layerId }
                if (target != null) {
                    editorViewModel.updateLayer(
                        target.copy(payload = LayerPayload.CustomXmlSnippet(markup, isJson))
                    )
                }
                editingXmlLayer = null
            },
            onDismiss = { editingXmlLayer = null }
        )
    }

    if (showSaveComponentDialog) {
        SaveCustomComponentDialog(
            selectedCount = uiState.selectedLayerIds.size,
            onSave = { name, category ->
                editorViewModel.saveSelectedAsCustomComponent(name, category)
                showSaveComponentDialog = false
            },
            onDismiss = { showSaveComponentDialog = false }
        )
    }

    if (showCustomDimensionDialog) {
        CustomDimensionDialog(
            initialWidthDp = uiState.canvasWidthDp,
            initialHeightDp = uiState.canvasHeightDp,
            onApply = { w, h ->
                editorViewModel.setCustomDimension(w, h)
                showCustomDimensionDialog = false
            },
            onDismiss = { showCustomDimensionDialog = false }
        )
    }
}

@Composable
private fun CustomDimensionDialog(
    initialWidthDp: Float,
    initialHeightDp: Float,
    onApply: (Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var widthText by remember { mutableStateOf(initialWidthDp.toInt().toString()) }
    var heightText by remember { mutableStateOf(initialHeightDp.toInt().toString()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Widget Size") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pick a grid preset or enter exact dimensions (dp):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        "2×1" to (160f to 80f),
                        "3×2" to (240f to 160f),
                        "4×3" to (320f to 240f)
                    ).forEach { (lbl, dims) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                widthText = dims.first.toInt().toString()
                                heightText = dims.second.toInt().toString()
                            },
                            label = { Text(lbl) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = widthText,
                        onValueChange = { widthText = it },
                        label = { Text("Width (dp)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("Height (dp)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = widthText.toFloatOrNull() ?: initialWidthDp
                    val h = heightText.toFloatOrNull() ?: initialHeightDp
                    onApply(w, h)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
