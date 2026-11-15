package com.example.ui.editor

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.backup.WBuilderMimeFilters
import com.example.data.backup.rememberFontPickerLauncher
import com.example.data.backup.rememberImagePhotoPickerLauncher
import com.example.data.remote.GeocodingPlaceDto
import com.example.data.repository.ExportedActivityInfo
import com.example.data.repository.LaunchableAppInfo
import com.example.data.repository.ShortcutPresetInfo
import com.example.domain.model.CalendarMode
import com.example.domain.model.FontWeightOption
import com.example.domain.model.GaugeMetric
import com.example.domain.model.GradientConfig
import com.example.domain.model.GradientType
import com.example.domain.model.LayerPayload
import com.example.domain.model.LayerType
import com.example.domain.model.ProgressBarStyle
import com.example.domain.model.ShapeKind
import com.example.domain.model.StackOrientation
import com.example.domain.model.StudioActionMode
import com.example.domain.model.TapAction
import com.example.domain.model.TextAlignOption
import com.example.domain.model.ToggleButtonKind
import com.example.domain.model.VolumeStreamType
import com.example.domain.model.WidgetLayer
import com.example.ui.components.CustomShapePathParser
import com.example.ui.components.parseHexColorSafe
import java.util.Locale
import kotlin.math.roundToInt

private val StudioSwatchPalette = listOf(
    "#FFFFFFFF",
    "#FF000000",
    "#00000000",
    "#FF181C2E",
    "#FF5CE1E6",
    "#FFFF8A7A",
    "#FF8B5CF6",
    "#FF38BDF8",
    "#FF34D399",
    "#FFFBBF24",
    "#33FFFFFF",
    "#220F172A"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyInspectorSheet(
    uiState: EditorUiState,
    onUpdateLayer: (WidgetLayer) -> Unit,
    onUpdateCanvasBackground: (String, GradientConfig, Float) -> Unit,
    onImportFontUri: (android.net.Uri) -> Unit,
    onImportImageUri: (android.net.Uri) -> Unit,
    onSearchCities: (String) -> Unit,
    onSelectCity: (layerId: String, GeocodingPlaceDto) -> Unit,
    onOpenAiArtwork: () -> Unit,
    onOpenCustomXmlEditor: (LayerPayload.CustomXmlSnippet) -> Unit,
    onConvertToCustomCode: (String) -> Unit,
    onAddChildToGroup: (groupId: String, childType: LayerType) -> Unit = { _, _ -> },
    onUngroupLayer: (String) -> Unit = {},
    onTestTapAction: (TapAction) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedLayer = uiState.primarySelectedLayer

    val fontPicker = rememberFontPickerLauncher(onFontPicked = onImportFontUri)
    val photoPicker = rememberImagePhotoPickerLauncher(onImagePicked = onImportImageUri)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        if (selectedLayer == null) {
            CanvasBackgroundInspector(
                colorHex = uiState.backgroundColorHex,
                gradient = uiState.backgroundGradient,
                cornerRadiusDp = uiState.backgroundCornerRadiusDp,
                onApply = onUpdateCanvasBackground
            )
        } else {
            LayerInspectorContent(
                layer = selectedLayer,
                installedApps = uiState.installedApps,
                exportedActivities = uiState.exportedActivities,
                shortcutPresets = uiState.shortcutPresets,
                globalSwitches = uiState.globalSwitches,
                citySearchResults = uiState.citySearchResults,
                isSearchingCities = uiState.isSearchingCities,
                onUpdate = onUpdateLayer,
                onTestTapAction = onTestTapAction,
                onPickFont = { fontPicker.launch(WBuilderMimeFilters.fontTypes) },
                onPickImage = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSearchCities = onSearchCities,
                onSelectCity = { place -> onSelectCity(selectedLayer.id, place) },
                onOpenAiArtwork = {
                    onDismiss()
                    onOpenAiArtwork()
                },
                onOpenXmlEditor = { snippet ->
                    onDismiss()
                    onOpenCustomXmlEditor(snippet)
                },
                onConvertToCustomCode = {
                    onDismiss()
                    onConvertToCustomCode(selectedLayer.id)
                },
                onAddChildToGroup = { childType ->
                    onAddChildToGroup(selectedLayer.id, childType)
                },
                onUngroupLayer = {
                    onUngroupLayer(selectedLayer.id)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun CanvasBackgroundInspector(
    colorHex: String,
    gradient: GradientConfig,
    cornerRadiusDp: Float,
    onApply: (String, GradientConfig, Float) -> Unit
) {
    var solidHex by remember(colorHex) { mutableStateOf(colorHex) }
    var gradEnabled by remember(gradient) { mutableStateOf(gradient.enabled) }
    var gradStart by remember(gradient) { mutableStateOf(gradient.startColorHex) }
    var gradEnd by remember(gradient) { mutableStateOf(gradient.endColorHex) }
    var gradType by remember(gradient) { mutableStateOf(gradient.type) }
    var gradAngle by remember(gradient) { mutableStateOf(gradient.angleDegrees) }
    var corner by remember(cornerRadiusDp) { mutableStateOf(cornerRadiusDp) }

    val currentAlpha = remember(solidHex, gradEnabled) {
        if (!gradEnabled) extractAlphaFraction(solidHex) else 1f
    }
    val isFullyTransparent = !gradEnabled && currentAlpha <= 0.01f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Widget Canvas Background", style = MaterialTheme.typography.titleLarge)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Transparent Window (Show Wallpaper)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "0% lets your home screen wallpaper show directly behind your components.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isFullyTransparent,
                        onCheckedChange = { makeTransparent ->
                            if (makeTransparent) {
                                solidHex = "#00000000"
                                gradEnabled = false
                            } else {
                                solidHex = "#FF181C2E"
                            }
                            onApply(
                                solidHex,
                                gradient.copy(enabled = gradEnabled, type = gradType, startColorHex = gradStart, endColorHex = gradEnd, angleDegrees = gradAngle),
                                corner
                            )
                        },
                        modifier = Modifier.testTag("inspector_transparent_window_switch")
                    )
                }

                SliderPropertyRow(
                    label = "Window Background Opacity",
                    value = currentAlpha * 100f,
                    range = 0f..100f,
                    unit = "%"
                ) { percent ->
                    val updatedHex = applyAlphaToHex(
                        baseHex = if (solidHex.equals("#00000000", ignoreCase = true)) "#FF181C2E" else solidHex,
                        alphaFraction = percent / 100f
                    )
                    solidHex = updatedHex
                    if (percent < 99.5f) {
                        gradEnabled = false
                    }
                    onApply(
                        updatedHex,
                        gradient.copy(enabled = gradEnabled, type = gradType, startColorHex = gradStart, endColorHex = gradEnd, angleDegrees = gradAngle),
                        corner
                    )
                }
            }
        }

        ColorHexPickerRow(
            label = "Background Color (Hex / ARGB)",
            hexValue = solidHex,
            onHexChange = {
                solidHex = it
                onApply(it, gradient.copy(enabled = gradEnabled, type = gradType, startColorHex = gradStart, endColorHex = gradEnd, angleDegrees = gradAngle), corner)
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Enable Gradient Backdrop", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = gradEnabled,
                onCheckedChange = {
                    gradEnabled = it
                    onApply(solidHex, gradient.copy(enabled = it, type = gradType, startColorHex = gradStart, endColorHex = gradEnd, angleDegrees = gradAngle), corner)
                }
            )
        }

        if (gradEnabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientType.entries.forEach { t ->
                    FilterChip(
                        selected = gradType == t,
                        onClick = {
                            gradType = t
                            onApply(solidHex, gradient.copy(enabled = true, type = t, startColorHex = gradStart, endColorHex = gradEnd, angleDegrees = gradAngle), corner)
                        },
                        label = { Text(if (t == GradientType.LINEAR) "Linear Gradient" else "Radial Gradient") }
                    )
                }
            }
            ColorHexPickerRow(
                label = "Gradient Start",
                hexValue = gradStart,
                onHexChange = {
                    gradStart = it
                    onApply(solidHex, gradient.copy(enabled = true, type = gradType, startColorHex = it, endColorHex = gradEnd, angleDegrees = gradAngle), corner)
                }
            )
            ColorHexPickerRow(
                label = "Gradient End",
                hexValue = gradEnd,
                onHexChange = {
                    gradEnd = it
                    onApply(solidHex, gradient.copy(enabled = true, type = gradType, startColorHex = gradStart, endColorHex = it, angleDegrees = gradAngle), corner)
                }
            )
            if (gradType == GradientType.LINEAR) {
                SliderPropertyRow("Gradient Angle", gradAngle, 0f..360f, "°") { ang ->
                    gradAngle = ang
                    onApply(solidHex, gradient.copy(enabled = true, type = gradType, startColorHex = gradStart, endColorHex = gradEnd, angleDegrees = ang), corner)
                }
            }
        }

        Text("Corner Radius: ${corner.toInt()}dp", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = corner,
            onValueChange = {
                corner = it
                onApply(solidHex, gradient.copy(enabled = gradEnabled, startColorHex = gradStart, endColorHex = gradEnd), it)
            },
            valueRange = 0f..40f
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerInspectorContent(
    layer: WidgetLayer,
    installedApps: List<LaunchableAppInfo>,
    exportedActivities: List<ExportedActivityInfo>,
    shortcutPresets: List<ShortcutPresetInfo>,
    globalSwitches: Map<String, Boolean>,
    citySearchResults: List<GeocodingPlaceDto>,
    isSearchingCities: Boolean,
    onUpdate: (WidgetLayer) -> Unit,
    onTestTapAction: (TapAction) -> Unit,
    onPickFont: () -> Unit,
    onPickImage: () -> Unit,
    onSearchCities: (String) -> Unit,
    onSelectCity: (GeocodingPlaceDto) -> Unit,
    onOpenAiArtwork: () -> Unit,
    onOpenXmlEditor: (LayerPayload.CustomXmlSnippet) -> Unit,
    onConvertToCustomCode: () -> Unit,
    onAddChildToGroup: (LayerType) -> Unit,
    onUngroupLayer: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Style", "Content & Font", "Tap Action")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = layer.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        TabRow(selectedTabIndex = activeTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("inspector_tab_$index")
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                0 -> StyleTransformTab(layer = layer, onUpdate = onUpdate)
                1 -> ContentAndFontTab(
                    layer = layer,
                    citySearchResults = citySearchResults,
                    isSearchingCities = isSearchingCities,
                    onUpdate = onUpdate,
                    onPickFont = onPickFont,
                    onPickImage = onPickImage,
                    onSearchCities = onSearchCities,
                    onSelectCity = onSelectCity,
                    onOpenAiArtwork = onOpenAiArtwork,
                    onOpenXmlEditor = onOpenXmlEditor,
                    onConvertToCustomCode = onConvertToCustomCode,
                    onAddChildToGroup = onAddChildToGroup,
                    onUngroupLayer = onUngroupLayer
                )
                2 -> TapActionTab(
                    layer = layer,
                    installedApps = installedApps,
                    exportedActivities = exportedActivities,
                    shortcutPresets = shortcutPresets,
                    globalSwitches = globalSwitches,
                    onUpdate = onUpdate,
                    onTestTapAction = onTestTapAction
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleTransformTab(
    layer: WidgetLayer,
    onUpdate: (WidgetLayer) -> Unit
) {
    val style = layer.style

    Text("Position & Size", style = MaterialTheme.typography.titleMedium)

    SliderPropertyRow("X Coordinate", style.xDp, 0f..300f, "dp") {
        onUpdate(layer.copy(style = style.copy(xDp = it)))
    }
    SliderPropertyRow("Y Coordinate", style.yDp, 0f..300f, "dp") {
        onUpdate(layer.copy(style = style.copy(yDp = it)))
    }
    SliderPropertyRow("Width", style.widthDp, 24f..340f, "dp") {
        onUpdate(layer.copy(style = style.copy(widthDp = it)))
    }
    SliderPropertyRow("Height", style.heightDp, 20f..340f, "dp") {
        onUpdate(layer.copy(style = style.copy(heightDp = it)))
    }
    SliderPropertyRow("Rotation", style.rotationDegrees, -180f..180f, "°") {
        onUpdate(layer.copy(style = style.copy(rotationDegrees = it)))
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(-90f to "-90°", -45f to "-45°", 0f to "0°", 45f to "45°", 90f to "90°").forEach { (deg, lbl) ->
            FilterChip(
                selected = kotlin.math.abs(style.rotationDegrees - deg) < 0.5f,
                onClick = { onUpdate(layer.copy(style = style.copy(rotationDegrees = deg))) },
                label = { Text(lbl) }
            )
        }
    }
    SliderPropertyRow("Opacity (Transparency)", style.opacity * 100f, 5f..100f, "%") {
        onUpdate(layer.copy(style = style.copy(opacity = it / 100f)))
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0.25f to "25%", 0.5f to "50%", 0.75f to "75%", 1f to "100%").forEach { (op, lbl) ->
            FilterChip(
                selected = kotlin.math.abs(style.opacity - op) < 0.03f,
                onClick = { onUpdate(layer.copy(style = style.copy(opacity = op))) },
                label = { Text(lbl) }
            )
        }
    }

    Text("Spacing, Borders & Corners", style = MaterialTheme.typography.titleMedium)

    SliderPropertyRow("Internal Padding", style.paddingDp, 0f..24f, "dp") {
        onUpdate(layer.copy(style = style.copy(paddingDp = it)))
    }
    SliderPropertyRow("Outer Margin", style.marginDp, 0f..20f, "dp") {
        onUpdate(layer.copy(style = style.copy(marginDp = it)))
    }
    SliderPropertyRow("Corner Radius", style.cornerRadiusDp, 0f..36f, "dp") {
        onUpdate(layer.copy(style = style.copy(cornerRadiusDp = it)))
    }
    SliderPropertyRow("Border Width", style.borderWidthDp, 0f..8f, "dp") {
        onUpdate(layer.copy(style = style.copy(borderWidthDp = it)))
    }
    if (style.borderWidthDp > 0f) {
        ColorHexPickerRow(
            label = "Border Color (Hex / ARGB)",
            hexValue = style.borderColorHex,
            onHexChange = { onUpdate(layer.copy(style = style.copy(borderColorHex = it))) }
        )
    }

    Text("Effects: Glass, Shadow & Glow", style = MaterialTheme.typography.titleMedium)

    SwitchOptionRow("Glassmorphism Tint", style.glassBlurEnabled) {
        onUpdate(layer.copy(style = style.copy(glassBlurEnabled = it)))
    }
    SliderPropertyRow("Drop Shadow", style.shadowElevationDp, 0f..24f, "dp") {
        onUpdate(layer.copy(style = style.copy(shadowElevationDp = it)))
    }
    if (style.shadowElevationDp > 0f) {
        ColorHexPickerRow(
            label = "Shadow Color",
            hexValue = style.shadowColorHex,
            onHexChange = { onUpdate(layer.copy(style = style.copy(shadowColorHex = it))) }
        )
    }
    SliderPropertyRow("Outer Glow", style.glowRadiusDp, 0f..24f, "dp") {
        onUpdate(layer.copy(style = style.copy(glowRadiusDp = it)))
    }
    if (style.glowRadiusDp > 0f) {
        ColorHexPickerRow(
            label = "Glow Color",
            hexValue = style.glowColorHex,
            onHexChange = { onUpdate(layer.copy(style = style.copy(glowColorHex = it))) }
        )
    }

    Text("Color & Gradients", style = MaterialTheme.typography.titleMedium)

    ColorHexPickerRow(
        label = "Foreground / Text Color",
        hexValue = style.foregroundColorHex,
        onHexChange = { onUpdate(layer.copy(style = style.copy(foregroundColorHex = it))) }
    )
    ColorHexPickerRow(
        label = "Accent Color",
        hexValue = style.accentColorHex,
        onHexChange = { onUpdate(layer.copy(style = style.copy(accentColorHex = it))) }
    )
    val hasLayerBg = parseHexColorSafe(style.backgroundColorHex, Color.Transparent).alpha > 0.01f ||
        style.gradient.enabled || style.glassBlurEnabled || style.borderWidthDp > 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Layer Background", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = {
                onUpdate(
                    layer.copy(
                        style = style.copy(
                            backgroundColorHex = "#00000000",
                            gradient = style.gradient.copy(enabled = false),
                            glassBlurEnabled = false,
                            borderWidthDp = 0f
                        )
                    )
                )
            },
            enabled = hasLayerBg,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("style_remove_background_button")
        ) {
            Icon(Icons.Rounded.Opacity, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (hasLayerBg) "Remove Background" else "Transparent")
        }
    }
    ColorHexPickerRow(
        label = "Layer Background Color",
        hexValue = style.backgroundColorHex,
        onHexChange = { onUpdate(layer.copy(style = style.copy(backgroundColorHex = it))) }
    )
    SwitchOptionRow("Enable Layer Gradient", style.gradient.enabled) { enabled ->
        onUpdate(layer.copy(style = style.copy(gradient = style.gradient.copy(enabled = enabled))))
    }
    if (style.gradient.enabled) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradientType.entries.forEach { t ->
                FilterChip(
                    selected = style.gradient.type == t,
                    onClick = { onUpdate(layer.copy(style = style.copy(gradient = style.gradient.copy(type = t)))) },
                    label = { Text(if (t == GradientType.LINEAR) "Linear" else "Radial") }
                )
            }
        }
        ColorHexPickerRow(
            label = "Gradient Start Color",
            hexValue = style.gradient.startColorHex,
            onHexChange = { onUpdate(layer.copy(style = style.copy(gradient = style.gradient.copy(startColorHex = it)))) }
        )
        ColorHexPickerRow(
            label = "Gradient End Color",
            hexValue = style.gradient.endColorHex,
            onHexChange = { onUpdate(layer.copy(style = style.copy(gradient = style.gradient.copy(endColorHex = it)))) }
        )
        if (style.gradient.type == GradientType.LINEAR) {
            SliderPropertyRow("Gradient Angle", style.gradient.angleDegrees, 0f..360f, "°") { ang ->
                onUpdate(layer.copy(style = style.copy(gradient = style.gradient.copy(angleDegrees = ang))))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContentAndFontTab(
    layer: WidgetLayer,
    citySearchResults: List<GeocodingPlaceDto>,
    isSearchingCities: Boolean,
    onUpdate: (WidgetLayer) -> Unit,
    onPickFont: () -> Unit,
    onPickImage: () -> Unit,
    onSearchCities: (String) -> Unit,
    onSelectCity: (GeocodingPlaceDto) -> Unit,
    onOpenAiArtwork: () -> Unit,
    onOpenXmlEditor: (LayerPayload.CustomXmlSnippet) -> Unit,
    onConvertToCustomCode: () -> Unit,
    onAddChildToGroup: (LayerType) -> Unit,
    onUngroupLayer: () -> Unit
) {
    val style = layer.style
    val builtInFonts = remember {
        listOf("Default", "Space Grotesk", "Plus Jakarta Sans", "JetBrains Mono", "Serif")
    }

    Text("Typography", style = MaterialTheme.typography.titleMedium)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        builtInFonts.forEach { font ->
            FilterChip(
                selected = style.fontFamilyName == font && style.fontAssetPath == null,
                onClick = {
                    onUpdate(layer.copy(style = style.copy(fontFamilyName = font, fontAssetPath = null)))
                },
                label = { Text(font) }
            )
        }
        OutlinedButton(
            onClick = onPickFont,
            modifier = Modifier.testTag("import_ttf_font_button")
        ) {
            Icon(Icons.Rounded.FontDownload, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (style.fontAssetPath != null) "Imported: ${style.fontFamilyName}" else "Import .ttf/.otf")
        }
    }

    SliderPropertyRow("Font Size", style.fontSizeSp, 10f..48f, "sp") {
        onUpdate(layer.copy(style = style.copy(fontSizeSp = it)))
    }
    SliderDecimalRow("Line Height", style.lineHeightMultiplier, 0.8f..2.2f, "x") {
        onUpdate(layer.copy(style = style.copy(lineHeightMultiplier = it)))
    }
    SliderDecimalRow("Letter Spacing", style.letterSpacingSp, -2f..8f, "sp") {
        onUpdate(layer.copy(style = style.copy(letterSpacingSp = it)))
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FontWeightOption.entries.forEach { w ->
            FilterChip(
                selected = style.fontWeight == w,
                onClick = { onUpdate(layer.copy(style = style.copy(fontWeight = w))) },
                label = { Text(w.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextAlignOption.entries.forEach { align ->
            FilterChip(
                selected = style.textAlign == align,
                onClick = { onUpdate(layer.copy(style = style.copy(textAlign = align))) },
                label = { Text("Align: ${align.name}") }
            )
        }
    }

    Spacer(Modifier.height(4.dp))
    Text("Component Settings", style = MaterialTheme.typography.titleMedium)

    when (val payload = layer.payload) {
        is LayerPayload.DigitalClock -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("24-Hour Format")
                Switch(
                    checked = payload.is24Hour,
                    onCheckedChange = { onUpdate(layer.copy(payload = payload.copy(is24Hour = it))) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Seconds")
                Switch(
                    checked = payload.showSeconds,
                    onCheckedChange = { onUpdate(layer.copy(payload = payload.copy(showSeconds = it))) }
                )
            }
        }
        is LayerPayload.CustomText -> {
            OutlinedTextField(
                value = payload.text,
                onValueChange = { onUpdate(layer.copy(payload = payload.copy(text = it))) },
                label = { Text("Text or {{tokens}}") },
                supportingText = { Text("Supports {{time}}, {{battery}}, {{weather_temp}}, {{network}}") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        is LayerPayload.DateText -> {
            OutlinedTextField(
                value = payload.pattern,
                onValueChange = { onUpdate(layer.copy(payload = payload.copy(pattern = it))) },
                label = { Text("Date Format Pattern") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        is LayerPayload.WeatherCard -> {
            WeatherLocationConfigSection(
                useDeviceLocation = payload.useDeviceLocation,
                currentCity = payload.cityName,
                unitCelsius = payload.unitCelsius,
                citySearchResults = citySearchResults,
                isSearchingCities = isSearchingCities,
                onToggleDeviceLocation = {
                    onUpdate(layer.copy(payload = payload.copy(useDeviceLocation = it)))
                },
                onManualCityChange = {
                    onUpdate(layer.copy(payload = payload.copy(cityName = it)))
                },
                onToggleUnit = {
                    onUpdate(layer.copy(payload = payload.copy(unitCelsius = it)))
                },
                onSearchCities = onSearchCities,
                onSelectCity = onSelectCity
            )
            SwitchOptionRow("Show High / Low", payload.showMinMax) {
                onUpdate(layer.copy(payload = payload.copy(showMinMax = it)))
            }
            SwitchOptionRow("Show Condition Icon", payload.showConditionIcon) {
                onUpdate(layer.copy(payload = payload.copy(showConditionIcon = it)))
            }
        }
        is LayerPayload.WeatherLocation -> {
            WeatherLocationConfigSection(
                useDeviceLocation = payload.useDeviceLocation,
                currentCity = payload.cityName,
                unitCelsius = payload.unitCelsius,
                citySearchResults = citySearchResults,
                isSearchingCities = isSearchingCities,
                onToggleDeviceLocation = {
                    onUpdate(layer.copy(payload = payload.copy(useDeviceLocation = it)))
                },
                onManualCityChange = {
                    onUpdate(layer.copy(payload = payload.copy(cityName = it)))
                },
                onToggleUnit = {
                    onUpdate(layer.copy(payload = payload.copy(unitCelsius = it)))
                },
                onSearchCities = onSearchCities,
                onSelectCity = onSelectCity
            )
            SwitchOptionRow("Show Feels-Like Temp", payload.showFeelsLike) {
                onUpdate(layer.copy(payload = payload.copy(showFeelsLike = it)))
            }
            SwitchOptionRow("Show Humidity & Wind", payload.showHumidityWind) {
                onUpdate(layer.copy(payload = payload.copy(showHumidityWind = it)))
            }
        }
        is LayerPayload.WeatherForecast -> {
            WeatherLocationConfigSection(
                useDeviceLocation = payload.useDeviceLocation,
                currentCity = payload.cityName,
                unitCelsius = payload.unitCelsius,
                citySearchResults = citySearchResults,
                isSearchingCities = isSearchingCities,
                onToggleDeviceLocation = {
                    onUpdate(layer.copy(payload = payload.copy(useDeviceLocation = it)))
                },
                onManualCityChange = {
                    onUpdate(layer.copy(payload = payload.copy(cityName = it)))
                },
                onToggleUnit = {
                    onUpdate(layer.copy(payload = payload.copy(unitCelsius = it)))
                },
                onSearchCities = onSearchCities,
                onSelectCity = onSelectCity
            )
            Text("Forecast Days", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3).forEach { count ->
                    FilterChip(
                        selected = payload.daysCount == count,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(daysCount = count))) },
                        label = { Text("$count Day${if (count > 1) "s" else ""}") }
                    )
                }
            }
        }
        is LayerPayload.LocationBadge -> {
            OutlinedTextField(
                value = payload.fallbackCity,
                onValueChange = { onUpdate(layer.copy(payload = payload.copy(fallbackCity = it))) },
                label = { Text("Fallback City Label") },
                modifier = Modifier.fillMaxWidth()
            )
            SwitchOptionRow("Show Pin Icon", payload.showPinIcon) {
                onUpdate(layer.copy(payload = payload.copy(showPinIcon = it)))
            }
            SwitchOptionRow("Show GPS Coordinates", payload.showCoordinates) {
                onUpdate(layer.copy(payload = payload.copy(showCoordinates = it)))
            }
        }
        is LayerPayload.CountdownTimer -> {
            OutlinedTextField(
                value = payload.title,
                onValueChange = { onUpdate(layer.copy(payload = payload.copy(title = it))) },
                label = { Text("Countdown Event Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Quick Target Offset", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1L to "+1 Day", 7L to "+7 Days", 30L to "+30 Days", 90L to "+90 Days").forEach { (days, label) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val target = System.currentTimeMillis() + days * 86_400_000L
                            onUpdate(layer.copy(payload = payload.copy(targetEpochMillis = target)))
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
        is LayerPayload.QuickToggleBar -> {
            Text(
                text = "Choose which interactive toggle buttons appear on this bar:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SwitchOptionRow("Flashlight Torch", payload.showFlashlight) {
                onUpdate(layer.copy(payload = payload.copy(showFlashlight = it)))
            }
            SwitchOptionRow("Mute / Unmute Media", payload.showMuteToggle) {
                onUpdate(layer.copy(payload = payload.copy(showMuteToggle = it)))
            }
            SwitchOptionRow("Play / Pause Music", payload.showPlayPause) {
                onUpdate(layer.copy(payload = payload.copy(showPlayPause = it)))
            }
            SwitchOptionRow("Wi-Fi Quick Panel", payload.showWifiSettings) {
                onUpdate(layer.copy(payload = payload.copy(showWifiSettings = it)))
            }
            SwitchOptionRow("Bluetooth Settings", payload.showBluetoothSettings) {
                onUpdate(layer.copy(payload = payload.copy(showBluetoothSettings = it)))
            }
            SwitchOptionRow("Volume Up (+)", payload.showVolumeUp) {
                onUpdate(layer.copy(payload = payload.copy(showVolumeUp = it)))
            }
            SwitchOptionRow("Volume Down (-)", payload.showVolumeDown) {
                onUpdate(layer.copy(payload = payload.copy(showVolumeDown = it)))
            }
            SwitchOptionRow("Refresh Widget Data", payload.showRefresh) {
                onUpdate(layer.copy(payload = payload.copy(showRefresh = it)))
            }
            SwitchOptionRow("Open W builder Shortcut", payload.showStudioShortcut) {
                onUpdate(layer.copy(payload = payload.copy(showStudioShortcut = it)))
            }
        }
        is LayerPayload.CalendarView -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarMode.entries.forEach { m ->
                    FilterChip(
                        selected = payload.mode == m,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(mode = m))) },
                        label = { Text(if (m == CalendarMode.MINI_MONTH) "Mini Month" else "Agenda List") }
                    )
                }
            }
        }
        is LayerPayload.ShapeContainer -> {
            val hasBoxBg = parseHexColorSafe(layer.style.backgroundColorHex, Color.Transparent).alpha > 0.01f ||
                layer.style.borderWidthDp > 0f || layer.style.glassBlurEnabled || layer.style.gradient.enabled
            if (hasBoxBg) {
                OutlinedButton(
                    onClick = {
                        onUpdate(
                            layer.copy(
                                style = layer.style.copy(
                                    backgroundColorHex = "#00000000",
                                    borderWidthDp = 0f,
                                    glassBlurEnabled = false,
                                    gradient = layer.style.gradient.copy(enabled = false)
                                )
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("shape_remove_background_button")
                ) {
                    Icon(Icons.Rounded.Opacity, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Remove Shape Box Background & Border")
                }
            }
            Text("Shape Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShapeKind.entries.forEach { kind ->
                    FilterChip(
                        selected = payload.shapeKind == kind,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(shapeKind = kind))) },
                        label = { Text(kind.label) },
                        modifier = Modifier.testTag("shape_kind_${kind.name.lowercase()}")
                    )
                }
            }
            if (payload.shapeKind != ShapeKind.LINE && payload.shapeKind != ShapeKind.ARC) {
                Text("Shape Style", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = payload.filled,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(filled = true))) },
                        label = { Text("Filled Shape") },
                        modifier = Modifier.testTag("shape_fill_mode_filled")
                    )
                    FilterChip(
                        selected = !payload.filled,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(filled = false))) },
                        label = { Text("Outline Only") },
                        modifier = Modifier.testTag("shape_fill_mode_outline")
                    )
                }
            }
            if (payload.shapeKind == ShapeKind.LINE || payload.shapeKind == ShapeKind.ARC || !payload.filled) {
                SliderPropertyRow("Stroke Thickness", payload.lineThicknessDp, 1.5f..18f, "dp") {
                    onUpdate(layer.copy(payload = payload.copy(lineThicknessDp = it)))
                }
            }
            if (payload.shapeKind == ShapeKind.CIRCLE_SLICE || payload.shapeKind == ShapeKind.ARC) {
                SliderPropertyRow("Start Angle", payload.startAngleDeg, -180f..180f, "°") {
                    onUpdate(layer.copy(payload = payload.copy(startAngleDeg = it)))
                }
                SliderPropertyRow("Sweep Angle", payload.sweepAngleDeg, 5f..360f, "°") {
                    onUpdate(layer.copy(payload = payload.copy(sweepAngleDeg = it)))
                }
            }
            if (payload.shapeKind == ShapeKind.PATH) {
                Text("Path Presets (0..100 Coordinate Grid)", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomShapePathParser.PRESETS.forEach { (presetLabel, presetData) ->
                        FilterChip(
                            selected = payload.customPathData == presetData,
                            onClick = { onUpdate(layer.copy(payload = payload.copy(customPathData = presetData))) },
                            label = { Text(presetLabel) }
                        )
                    }
                }
                OutlinedTextField(
                    value = payload.customPathData,
                    onValueChange = { onUpdate(layer.copy(payload = payload.copy(customPathData = it))) },
                    label = { Text("SVG Path Commands (M, L, H, V, Q, C, Z)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shape_custom_path_input")
                )
            }
        }
        is LayerPayload.VectorIcon -> {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudioVectorIconCatalog.forEach { k ->
                    FilterChip(
                        selected = payload.symbolKey == k,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(symbolKey = k))) },
                        leadingIcon = {
                            Icon(
                                imageVector = resolveVectorIcon(k),
                                contentDescription = k,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(k) },
                        modifier = Modifier.testTag("inspector_icon_$k")
                    )
                }
            }
        }
        is LayerPayload.ImageMedia -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("pick_photo_image_button")
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Photo Picker")
                }
                Button(
                    onClick = onOpenAiArtwork,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("inspector_ai_image_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Create/Edit")
                }
            }
        }
        is LayerPayload.AudioVolumeBar -> {
            SwitchOptionRow("Show Volume Percentage", payload.showPercent) {
                onUpdate(layer.copy(payload = payload.copy(showPercent = it)))
            }
            SwitchOptionRow("Show Mute Status", payload.showMuteBadge) {
                onUpdate(layer.copy(payload = payload.copy(showMuteBadge = it)))
            }
        }
        is LayerPayload.GreetingBanner -> {
            OutlinedTextField(
                value = payload.userName,
                onValueChange = { onUpdate(layer.copy(payload = payload.copy(userName = it))) },
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchOptionRow("Show Subtitle Line", payload.showSubtitle) {
                onUpdate(layer.copy(payload = payload.copy(showSubtitle = it)))
            }
            if (payload.showSubtitle) {
                OutlinedTextField(
                    value = payload.subtitleTemplate,
                    onValueChange = { onUpdate(layer.copy(payload = payload.copy(subtitleTemplate = it))) },
                    label = { Text("Subtitle Template") },
                    supportingText = { Text("Supports {{day}}, {{weather_temp}}, {{battery}}") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        is LayerPayload.WorldClock -> {
            OutlinedTextField(
                value = payload.cityLabel,
                onValueChange = { onUpdate(layer.copy(payload = payload.copy(cityLabel = it))) },
                label = { Text("City Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Timezone Preset", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Tokyo" to "Asia/Tokyo",
                    "London" to "Europe/London",
                    "New York" to "America/New_York",
                    "Dubai" to "Asia/Dubai",
                    "Sydney" to "Australia/Sydney",
                    "UTC" to "UTC"
                ).forEach { (city, tz) ->
                    FilterChip(
                        selected = payload.timeZoneId == tz,
                        onClick = {
                            onUpdate(layer.copy(payload = payload.copy(cityLabel = city, timeZoneId = tz)))
                        },
                        label = { Text(city) }
                    )
                }
            }
            SwitchOptionRow("24-Hour Format", payload.is24Hour) {
                onUpdate(layer.copy(payload = payload.copy(is24Hour = it)))
            }
        }
        is LayerPayload.ProgressRing -> {
            Text("Display Style", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProgressBarStyle.entries.forEach { barStyle ->
                    FilterChip(
                        selected = payload.style == barStyle,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(style = barStyle))) },
                        label = { Text(barStyle.label) },
                        modifier = Modifier.testTag("progress_style_${barStyle.name.lowercase()}")
                    )
                }
            }

            if (payload.style == ProgressBarStyle.SEGMENTED_BAR) {
                SliderPropertyRow("Segments Count", payload.segmentsCount.toFloat(), 4f..24f, "") {
                    onUpdate(layer.copy(payload = payload.copy(segmentsCount = it.roundToInt().coerceIn(4, 24))))
                }
            }

            Text("Data Source Mode", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GaugeMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = payload.metric == metric,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(metric = metric))) },
                        label = { Text(metric.label) },
                        modifier = Modifier.testTag("gauge_mode_${metric.name.lowercase()}")
                    )
                }
            }

            if (payload.metric == GaugeMetric.CUSTOM) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Custom Data Binding",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "Battery" to "{{battery}}",
                                "RAM" to "{{ram_used}}",
                                "Storage" to "{{storage_used}}",
                                "Volume" to "{{volume}}",
                                "Temp" to "{{weather_temp}}",
                                "Notifs" to "{{notif_count}}"
                            ).forEach { (tokLabel, tok) ->
                                FilterChip(
                                    selected = payload.customValueExpression == tok,
                                    onClick = {
                                        onUpdate(
                                            layer.copy(
                                                payload = payload.copy(
                                                    customValueExpression = tok,
                                                    customLabel = tokLabel
                                                )
                                            )
                                        )
                                    },
                                    label = { Text(tokLabel) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = payload.customValueExpression,
                            onValueChange = { onUpdate(layer.copy(payload = payload.copy(customValueExpression = it))) },
                            label = { Text("Custom Value / Token Expression") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_gauge_expression_input")
                        )
                        OutlinedTextField(
                            value = payload.customLabel,
                            onValueChange = { onUpdate(layer.copy(payload = payload.copy(customLabel = it))) },
                            label = { Text("Custom Metric Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = payload.customMin.toInt().toString(),
                                onValueChange = { txt ->
                                    txt.toFloatOrNull()?.let { v ->
                                        onUpdate(layer.copy(payload = payload.copy(customMin = v)))
                                    }
                                },
                                label = { Text("Min Value") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = payload.customMax.toInt().toString(),
                                onValueChange = { txt ->
                                    txt.toFloatOrNull()?.let { v ->
                                        onUpdate(layer.copy(payload = payload.copy(customMax = v)))
                                    }
                                },
                                label = { Text("Max Value") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SwitchOptionRow("Show Metric Label", payload.showLabel) {
                onUpdate(layer.copy(payload = payload.copy(showLabel = it)))
            }
            SliderPropertyRow(
                label = if (payload.style == ProgressBarStyle.CIRCULAR_GAUGE) "Ring Thickness" else "Bar Thickness",
                value = payload.strokeWidthDp,
                range = 3f..16f,
                unit = "dp"
            ) {
                onUpdate(layer.copy(payload = payload.copy(strokeWidthDp = it)))
            }
        }
        is LayerPayload.OverlapGroup -> {
            GroupChildrenManager(
                groupLayer = layer,
                children = payload.embeddedLayers,
                isStackGroup = false,
                onChildrenUpdated = { updatedChildren ->
                    onUpdate(layer.copy(payload = payload.copy(embeddedLayers = updatedChildren)))
                },
                onAddChildToGroup = onAddChildToGroup,
                onUngroupLayer = onUngroupLayer
            )
        }
        is LayerPayload.StackGroup -> {
            Text("Stack Orientation", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StackOrientation.entries.forEach { orient ->
                    FilterChip(
                        selected = payload.orientation == orient,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(orientation = orient))) },
                        label = { Text(orient.label) },
                        modifier = Modifier.testTag("stack_orient_${orient.name.lowercase()}")
                    )
                }
            }
            SliderPropertyRow("Item Spacing", payload.spacingDp, 0f..24f, "dp") {
                onUpdate(layer.copy(payload = payload.copy(spacingDp = it)))
            }
            GroupChildrenManager(
                groupLayer = layer,
                children = payload.embeddedLayers,
                isStackGroup = true,
                onChildrenUpdated = { updatedChildren ->
                    onUpdate(layer.copy(payload = payload.copy(embeddedLayers = updatedChildren)))
                },
                onAddChildToGroup = onAddChildToGroup,
                onUngroupLayer = onUngroupLayer
            )
        }
        is LayerPayload.QuickNote -> {
            OutlinedTextField(
                value = payload.noteText,
                onValueChange = { onUpdate(layer.copy(payload = payload.copy(noteText = it))) },
                label = { Text("Sticky Note / Todo Text") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchOptionRow("Todo Checklist Mode", payload.isChecklist) {
                onUpdate(layer.copy(payload = payload.copy(isChecklist = it)))
            }
            SwitchOptionRow("Show Accent Icon", payload.showAccentBar) {
                onUpdate(layer.copy(payload = payload.copy(showAccentBar = it)))
            }
        }
        is LayerPayload.ToggleButton -> {
            Text("Toggle Function", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ToggleButtonKind.entries.forEach { k ->
                    FilterChip(
                        selected = payload.kind == k,
                        onClick = { onUpdate(layer.copy(payload = payload.copy(kind = k))) },
                        label = { Text(k.label) },
                        modifier = Modifier.testTag("toggle_kind_${k.name.lowercase()}")
                    )
                }
            }
            SwitchOptionRow("Show Status Label", payload.showLabel) {
                onUpdate(layer.copy(payload = payload.copy(showLabel = it)))
            }
        }
        is LayerPayload.MusicPlayer -> {
            SwitchOptionRow("Show Playback Controls (Prev / Play / Next)", payload.showPlaybackControls) {
                onUpdate(layer.copy(payload = payload.copy(showPlaybackControls = it)))
            }
            SwitchOptionRow("Compact Mode", payload.compactMode) {
                onUpdate(layer.copy(payload = payload.copy(compactMode = it)))
            }
        }
        is LayerPayload.CustomXmlSnippet -> {
            Button(
                onClick = { onOpenXmlEditor(payload) },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("open_xml_code_editor_button")
            ) {
                Icon(Icons.Rounded.Code, null)
                Spacer(Modifier.width(8.dp))
                Text("Edit XML / JSON Markup")
            }
        }
        else -> {
            Text(
                text = "Live device data binds automatically on canvas and home screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (layer.payload !is LayerPayload.CustomXmlSnippet) {
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = onConvertToCustomCode,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("convert_to_custom_code_button")
        ) {
            Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Customize UI with Custom Code")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TapActionTab(
    layer: WidgetLayer,
    installedApps: List<LaunchableAppInfo>,
    exportedActivities: List<ExportedActivityInfo>,
    shortcutPresets: List<ShortcutPresetInfo>,
    globalSwitches: Map<String, Boolean>,
    onUpdate: (WidgetLayer) -> Unit,
    onTestTapAction: (TapAction) -> Unit
) {
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var showActivityPickerDialog by remember { mutableStateOf(false) }
    var activitySearchQuery by remember { mutableStateOf("") }
    val currentAction = layer.tapAction

    Text("Action Menu", style = MaterialTheme.typography.titleMedium)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = currentAction is TapAction.None,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.None)) },
            label = { Text("None") },
            modifier = Modifier.testTag("tap_action_none")
        )
        FilterChip(
            selected = currentAction is TapAction.StudioAction,
            onClick = {
                if (currentAction !is TapAction.StudioAction) {
                    onUpdate(layer.copy(tapAction = TapAction.StudioAction()))
                }
            },
            label = { Text("Custom Action") },
            modifier = Modifier.testTag("tap_action_custom")
        )
        FilterChip(
            selected = currentAction is TapAction.LaunchApp,
            onClick = { showAppPickerDialog = true },
            label = {
                Text(
                    if (currentAction is TapAction.LaunchApp) {
                        "App: ${currentAction.appLabel.ifBlank { currentAction.packageName }}"
                    } else "Launch App"
                )
            },
            modifier = Modifier.testTag("tap_action_launch_app")
        )
        FilterChip(
            selected = currentAction is TapAction.LaunchShortcut,
            onClick = {
                if (currentAction !is TapAction.LaunchShortcut) {
                    val preset = shortcutPresets.firstOrNull()
                    onUpdate(
                        layer.copy(
                            tapAction = TapAction.LaunchShortcut(
                                shortcutUri = preset?.uri ?: "settings://android.settings.SETTINGS",
                                label = preset?.label ?: "System Settings"
                            )
                        )
                    )
                }
            },
            label = { Text("Launch Shortcut") },
            modifier = Modifier.testTag("tap_action_launch_shortcut")
        )
        FilterChip(
            selected = currentAction is TapAction.LaunchActivity,
            onClick = {
                if (currentAction !is TapAction.LaunchActivity) {
                    val first = exportedActivities.firstOrNull()
                    onUpdate(
                        layer.copy(
                            tapAction = TapAction.LaunchActivity(
                                packageName = first?.packageName.orEmpty(),
                                className = first?.className.orEmpty(),
                                label = first?.label ?: "Select Activity"
                            )
                        )
                    )
                }
                showActivityPickerDialog = true
            },
            label = { Text("Launch Activity") },
            modifier = Modifier.testTag("tap_action_launch_activity")
        )
        FilterChip(
            selected = currentAction is TapAction.MediaPlayPause ||
                currentAction is TapAction.MediaPrevious ||
                currentAction is TapAction.MediaNext,
            onClick = {
                if (currentAction !is TapAction.MediaPlayPause &&
                    currentAction !is TapAction.MediaPrevious &&
                    currentAction !is TapAction.MediaNext
                ) {
                    onUpdate(layer.copy(tapAction = TapAction.MediaPlayPause))
                }
            },
            label = { Text("Music Controls") },
            modifier = Modifier.testTag("tap_action_music_controls")
        )
        FilterChip(
            selected = currentAction is TapAction.ToggleGlobalSwitch,
            onClick = {
                if (currentAction !is TapAction.ToggleGlobalSwitch) {
                    onUpdate(layer.copy(tapAction = TapAction.ToggleGlobalSwitch("global_1")))
                }
            },
            label = { Text("Toggle Global Switch") },
            modifier = Modifier.testTag("tap_action_toggle_global")
        )
        FilterChip(
            selected = currentAction is TapAction.OpenUrl,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.OpenUrl("https://"))) },
            label = { Text("Open Link") },
            modifier = Modifier.testTag("tap_action_open_url")
        )
        FilterChip(
            selected = currentAction is TapAction.TriggerFlow,
            onClick = {
                if (currentAction !is TapAction.TriggerFlow) {
                    onUpdate(
                        layer.copy(
                            tapAction = TapAction.TriggerFlow(
                                flowName = "Quick Flow",
                                steps = listOf(TapAction.RefreshWidget)
                            )
                        )
                    )
                }
            },
            label = { Text("Trigger Flow") },
            modifier = Modifier.testTag("tap_action_trigger_flow")
        )
        FilterChip(
            selected = currentAction is TapAction.ChangeVolume ||
                currentAction is TapAction.VolumeUp ||
                currentAction is TapAction.VolumeDown,
            onClick = {
                if (currentAction !is TapAction.ChangeVolume) {
                    onUpdate(layer.copy(tapAction = TapAction.ChangeVolume()))
                }
            },
            label = { Text("Change Volume") },
            modifier = Modifier.testTag("tap_action_change_volume")
        )
        FilterChip(
            selected = currentAction is TapAction.DisabledBlockTouch,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.DisabledBlockTouch)) },
            label = { Text("Disabled (Block Touch)") },
            modifier = Modifier.testTag("tap_action_block_touch")
        )
    }

    // Contextual Configuration Panels
    when (currentAction) {
        is TapAction.StudioAction -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Mode Menu (Custom Action)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StudioActionMode.entries.forEach { mode ->
                            FilterChip(
                                selected = currentAction.mode == mode,
                                onClick = {
                                    onUpdate(layer.copy(tapAction = currentAction.copy(mode = mode)))
                                },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("studio_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    if (currentAction.mode == StudioActionMode.FORCE_RSS_XML_UPDATE ||
                        currentAction.mode == StudioActionMode.FORCE_IMAGE_UPDATE
                    ) {
                        OutlinedTextField(
                            value = currentAction.targetUrl,
                            onValueChange = { url ->
                                onUpdate(layer.copy(tapAction = currentAction.copy(targetUrl = url)))
                            },
                            label = {
                                Text(
                                    if (currentAction.mode == StudioActionMode.FORCE_RSS_XML_UPDATE) {
                                        "RSS / Text / XML URL"
                                    } else {
                                        "Optional Image URL (refreshes layer images too)"
                                    }
                                )
                            },
                            placeholder = {
                                Text(
                                    if (currentAction.mode == StudioActionMode.FORCE_RSS_XML_UPDATE) {
                                        "https://hnrss.org/frontpage"
                                    } else {
                                        "https://example.com/cover.jpg"
                                    }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_mode_url_input")
                        )
                        if (currentAction.mode == StudioActionMode.FORCE_RSS_XML_UPDATE) {
                            Text(
                                text = "Use {{rss_title}}, {{rss_desc}}, or {{feed_text}} inside any Custom Text or XML block.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (currentAction.mode == StudioActionMode.OPEN_NOTIFICATION ||
                        currentAction.mode == StudioActionMode.DISMISS_NOTIFICATION
                    ) {
                        Text("Notification Slot", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (0..4).forEach { slotIdx ->
                                FilterChip(
                                    selected = currentAction.notificationIndex == slotIdx,
                                    onClick = {
                                        onUpdate(
                                            layer.copy(
                                                tapAction = currentAction.copy(notificationIndex = slotIdx)
                                            )
                                        )
                                    },
                                    label = { Text("Slot ${slotIdx + 1}") }
                                )
                            }
                        }
                        Text(
                            text = "Dynamic tokens: {{notif_count}}, {{notif_title}}, {{notif_text}}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is TapAction.LaunchShortcut -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Shortcut Target",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        shortcutPresets.forEach { preset ->
                            FilterChip(
                                selected = currentAction.shortcutUri == preset.uri,
                                onClick = {
                                    onUpdate(
                                        layer.copy(
                                            tapAction = TapAction.LaunchShortcut(
                                                shortcutUri = preset.uri,
                                                label = preset.label
                                            )
                                        )
                                    )
                                },
                                label = { Text(preset.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = currentAction.label,
                        onValueChange = {
                            onUpdate(layer.copy(tapAction = currentAction.copy(label = it)))
                        },
                        label = { Text("Shortcut Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentAction.shortcutUri,
                        onValueChange = {
                            onUpdate(layer.copy(tapAction = currentAction.copy(shortcutUri = it)))
                        },
                        label = { Text("Shortcut / Intent / Deep-link URI") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_shortcut_uri_input")
                    )
                }
            }
        }

        is TapAction.LaunchActivity -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Exported Activity Target",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { showActivityPickerDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("browse_exported_activities_button")
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pick Exported Activity (${currentAction.label})")
                    }
                    OutlinedTextField(
                        value = currentAction.packageName,
                        onValueChange = {
                            onUpdate(layer.copy(tapAction = currentAction.copy(packageName = it)))
                        },
                        label = { Text("Package Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentAction.className,
                        onValueChange = {
                            onUpdate(layer.copy(tapAction = currentAction.copy(className = it)))
                        },
                        label = { Text("Activity Class Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        is TapAction.ToggleGlobalSwitch -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Global Switch Variable",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("global_1", "dark_mode", "focus_mode", "compact_mode").forEach { presetName ->
                            val isOn = globalSwitches[presetName] == true
                            FilterChip(
                                selected = currentAction.switchName == presetName,
                                onClick = {
                                    onUpdate(layer.copy(tapAction = TapAction.ToggleGlobalSwitch(presetName)))
                                },
                                label = { Text("$presetName (${if (isOn) "ON" else "OFF"})") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = currentAction.switchName,
                        onValueChange = {
                            onUpdate(layer.copy(tapAction = TapAction.ToggleGlobalSwitch(it)))
                        },
                        label = { Text("Switch Key Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("global_switch_name_input")
                    )
                    Text(
                        text = "Bind in text/XML using {{global.${currentAction.switchName.ifBlank { "global_1" }}}} (resolves to ON or OFF).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is TapAction.TriggerFlow -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Trigger Flow (${currentAction.steps.size} steps)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = currentAction.flowName,
                        onValueChange = {
                            onUpdate(layer.copy(tapAction = currentAction.copy(flowName = it)))
                        },
                        label = { Text("Flow Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trigger_flow_name_input")
                    )
                    currentAction.steps.forEachIndexed { idx, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. ${describeTapAction(step)}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val remaining = currentAction.steps.toMutableList().apply { removeAt(idx) }
                                    onUpdate(layer.copy(tapAction = currentAction.copy(steps = remaining)))
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove step")
                            }
                        }
                    }
                    Text("Add Flow Step", style = MaterialTheme.typography.labelMedium)
                    val presetSteps = listOf(
                        "Flashlight" to TapAction.ToggleFlashlight,
                        "Toggle Global" to TapAction.ToggleGlobalSwitch("global_1"),
                        "Weather Sync" to TapAction.StudioAction(StudioActionMode.FORCE_WEATHER_UPDATE),
                        "RSS Sync" to TapAction.StudioAction(StudioActionMode.FORCE_RSS_XML_UPDATE),
                        "Play/Pause" to TapAction.MediaPlayPause,
                        "Silent/Vib" to TapAction.ToggleSilentVibrate,
                        "Volume +1" to TapAction.ChangeVolume(VolumeStreamType.MUSIC, 1, true),
                        "Clear Notifs" to TapAction.StudioAction(StudioActionMode.DISMISS_ALL_NOTIFICATIONS),
                        "Refresh" to TapAction.RefreshWidget
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetSteps.forEach { (stepLabel, stepAction) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onUpdate(
                                        layer.copy(
                                            tapAction = currentAction.copy(
                                                steps = currentAction.steps + stepAction
                                            )
                                        )
                                    )
                                },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(stepLabel) }
                            )
                        }
                    }
                }
            }
        }

        is TapAction.ChangeVolume -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Volume Stream & Step",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VolumeStreamType.entries.forEach { stream ->
                            FilterChip(
                                selected = currentAction.stream == stream,
                                onClick = {
                                    onUpdate(layer.copy(tapAction = currentAction.copy(stream = stream)))
                                },
                                label = { Text(stream.label) }
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-3, -1, 1, 3).forEach { delta ->
                            FilterChip(
                                selected = currentAction.deltaSteps == delta,
                                onClick = {
                                    onUpdate(layer.copy(tapAction = currentAction.copy(deltaSteps = delta)))
                                },
                                label = { Text(if (delta > 0) "+$delta steps" else "$delta steps") }
                            )
                        }
                    }
                    SwitchOptionRow(
                        label = "Show System Volume UI",
                        checked = currentAction.showUi
                    ) { show ->
                        onUpdate(layer.copy(tapAction = currentAction.copy(showUi = show)))
                    }
                }
            }
        }

        is TapAction.OpenUrl -> {
            OutlinedTextField(
                value = currentAction.url,
                onValueChange = { onUpdate(layer.copy(tapAction = TapAction.OpenUrl(it))) },
                label = { Text("Target URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        is TapAction.DisabledBlockTouch -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Touch Blocking Enabled: Tapping this component absorbs the touch event so it does not trigger underlying layers or the widget root action.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        else -> Unit
    }

    if (currentAction !is TapAction.None && currentAction !is TapAction.DisabledBlockTouch) {
        OutlinedButton(
            onClick = { onTestTapAction(currentAction) },
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("test_tap_action_button")
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Test Action Now")
        }
    }

    Text("Quick Device Toggles & Media", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = currentAction is TapAction.ToggleFlashlight,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.ToggleFlashlight)) },
            label = { Text("Toggle Flashlight") },
            modifier = Modifier.testTag("tap_action_flashlight")
        )
        FilterChip(
            selected = currentAction is TapAction.ToggleSilentVibrate,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.ToggleSilentVibrate)) },
            label = { Text("Silent / Vibrate Mode") },
            modifier = Modifier.testTag("tap_action_silent_vibrate")
        )
        FilterChip(
            selected = currentAction is TapAction.ToggleMute,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.ToggleMute)) },
            label = { Text("Mute / Unmute") },
            modifier = Modifier.testTag("tap_action_mute")
        )
        FilterChip(
            selected = currentAction is TapAction.RefreshWidget,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.RefreshWidget)) },
            label = { Text("Refresh Data") },
            modifier = Modifier.testTag("tap_action_refresh")
        )
        FilterChip(
            selected = currentAction is TapAction.MediaPlayPause,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.MediaPlayPause)) },
            label = { Text("Play / Pause") },
            modifier = Modifier.testTag("tap_action_play_pause")
        )
        FilterChip(
            selected = currentAction is TapAction.MediaPrevious,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.MediaPrevious)) },
            label = { Text("Previous Track") },
            modifier = Modifier.testTag("tap_action_prev")
        )
        FilterChip(
            selected = currentAction is TapAction.MediaNext,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.MediaNext)) },
            label = { Text("Next Track") },
            modifier = Modifier.testTag("tap_action_next")
        )
        FilterChip(
            selected = currentAction is TapAction.VolumeUp,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.VolumeUp)) },
            label = { Text("Volume +") },
            modifier = Modifier.testTag("tap_action_vol_up")
        )
        FilterChip(
            selected = currentAction is TapAction.VolumeDown,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.VolumeDown)) },
            label = { Text("Volume -") },
            modifier = Modifier.testTag("tap_action_vol_down")
        )
    }

    Text("System Settings & Panels", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = currentAction is TapAction.OpenWifiSettings,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.OpenWifiSettings)) },
            label = { Text("Wi-Fi Settings") },
            modifier = Modifier.testTag("tap_action_wifi")
        )
        FilterChip(
            selected = currentAction is TapAction.OpenBluetoothSettings,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.OpenBluetoothSettings)) },
            label = { Text("Bluetooth") },
            modifier = Modifier.testTag("tap_action_bluetooth")
        )
        FilterChip(
            selected = currentAction is TapAction.OpenSoundSettings,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.OpenSoundSettings)) },
            label = { Text("Sound Settings") },
            modifier = Modifier.testTag("tap_action_sound")
        )
        FilterChip(
            selected = currentAction is TapAction.OpenDisplaySettings,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.OpenDisplaySettings)) },
            label = { Text("Display") },
            modifier = Modifier.testTag("tap_action_display")
        )
        FilterChip(
            selected = currentAction is TapAction.OpenAlarmClock,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.OpenAlarmClock)) },
            label = { Text("Alarms / Clock") },
            modifier = Modifier.testTag("tap_action_alarm")
        )
        FilterChip(
            selected = currentAction is TapAction.OpenCamera,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.OpenCamera)) },
            label = { Text("Camera") },
            modifier = Modifier.testTag("tap_action_camera")
        )
        FilterChip(
            selected = currentAction is TapAction.OpenStudio,
            onClick = { onUpdate(layer.copy(tapAction = TapAction.OpenStudio)) },
            label = { Text("Open W builder") },
            modifier = Modifier.testTag("tap_action_studio")
        )
    }

    if (showAppPickerDialog) {
        AlertDialog(
            onDismissRequest = { showAppPickerDialog = false },
            title = { Text("Choose App to Launch") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(installedApps) { app ->
                        TextButton(
                            onClick = {
                                onUpdate(
                                    layer.copy(
                                        tapAction = TapAction.LaunchApp(
                                            packageName = app.packageName,
                                            appLabel = app.label
                                        )
                                    )
                                )
                                showAppPickerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${app.label} (${app.packageName})",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPickerDialog = false }) { Text("Close") }
            }
        )
    }

    if (showActivityPickerDialog) {
        val filteredActivities = remember(exportedActivities, activitySearchQuery) {
            val q = activitySearchQuery.trim().lowercase()
            if (q.isEmpty()) exportedActivities
            else exportedActivities.filter {
                it.label.lowercase().contains(q) ||
                    it.packageName.lowercase().contains(q) ||
                    it.className.lowercase().contains(q)
            }
        }
        AlertDialog(
            onDismissRequest = { showActivityPickerDialog = false },
            title = { Text("Pick Exported Activity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activitySearchQuery,
                        onValueChange = { activitySearchQuery = it },
                        label = { Text("Search app or activity") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredActivities) { act ->
                            TextButton(
                                onClick = {
                                    onUpdate(
                                        layer.copy(
                                            tapAction = TapAction.LaunchActivity(
                                                packageName = act.packageName,
                                                className = act.className,
                                                label = act.label
                                            )
                                        )
                                    )
                                    showActivityPickerDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(act.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${act.packageName}/${act.className.substringAfterLast('.')}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActivityPickerDialog = false }) { Text("Close") }
            }
        )
    }
}

private fun describeTapAction(action: TapAction): String = when (action) {
    is TapAction.None -> "None"
    is TapAction.LaunchApp -> "App: ${action.appLabel.ifBlank { action.packageName }}"
    is TapAction.LaunchShortcut -> "Shortcut: ${action.label}"
    is TapAction.LaunchActivity -> "Activity: ${action.label}"
    is TapAction.StudioAction -> "Custom: ${action.mode.label}"
    is TapAction.ToggleGlobalSwitch -> "Global: ${action.switchName}"
    is TapAction.TriggerFlow -> "Flow: ${action.flowName}"
    is TapAction.ChangeVolume -> "${action.stream.label} (${if (action.deltaSteps > 0) "+${action.deltaSteps}" else action.deltaSteps})"
    is TapAction.DisabledBlockTouch -> "Disabled (Block Touch)"
    is TapAction.ToggleFlashlight -> "Toggle Flashlight"
    is TapAction.ToggleSilentVibrate -> "Silent / Vibrate Mode"
    is TapAction.ToggleMute -> "Mute / Unmute"
    is TapAction.MediaPlayPause -> "Media Play / Pause"
    is TapAction.MediaNext -> "Media Next"
    is TapAction.MediaPrevious -> "Media Previous"
    is TapAction.VolumeUp -> "Volume +"
    is TapAction.VolumeDown -> "Volume -"
    is TapAction.OpenWifiSettings -> "Wi-Fi Settings"
    is TapAction.OpenBluetoothSettings -> "Bluetooth Settings"
    is TapAction.OpenSoundSettings -> "Sound Settings"
    is TapAction.OpenDisplaySettings -> "Display Settings"
    is TapAction.OpenAlarmClock -> "Alarm Clock"
    is TapAction.OpenCamera -> "Camera"
    is TapAction.OpenUrl -> "Open Link: ${action.url}"
    is TapAction.RefreshWidget -> "Refresh Widget"
    is TapAction.OpenStudio -> "Advanced Editor"
}

@Composable
private fun SliderPropertyRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("${value.toInt()}$unit", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun SliderDecimalRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("${String.format(Locale.US, "%.1f", value)}$unit", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun ColorHexPickerRow(
    label: String,
    hexValue: String,
    onHexChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = hexValue,
            onValueChange = onHexChange,
            label = { Text(label) },
            singleLine = true,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(parseHexColorSafe(hexValue, Color.Transparent))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StudioSwatchPalette) { swatch ->
                val color = parseHexColorSafe(swatch, Color.Transparent)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (hexValue.equals(swatch, ignoreCase = true)) 2.5.dp else 1.dp,
                            color = if (hexValue.equals(swatch, ignoreCase = true)) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape
                        )
                        .clickable { onHexChange(swatch) }
                )
            }
        }
    }
}

@Composable
private fun SwitchOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun WeatherLocationConfigSection(
    useDeviceLocation: Boolean,
    currentCity: String,
    unitCelsius: Boolean,
    citySearchResults: List<GeocodingPlaceDto>,
    isSearchingCities: Boolean,
    onToggleDeviceLocation: (Boolean) -> Unit,
    onManualCityChange: (String) -> Unit,
    onToggleUnit: (Boolean) -> Unit,
    onSearchCities: (String) -> Unit,
    onSelectCity: (GeocodingPlaceDto) -> Unit
) {
    var searchQuery by remember(currentCity) { mutableStateOf(currentCity) }

    SwitchOptionRow(
        label = "Use Device Location (GPS)",
        checked = useDeviceLocation,
        onCheckedChange = onToggleDeviceLocation
    )

    OutlinedTextField(
        value = searchQuery,
        onValueChange = {
            searchQuery = it
            onManualCityChange(it)
            onSearchCities(it)
        },
        label = { Text("Search City (Open-Meteo)") },
        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null) },
        trailingIcon = {
            if (isSearchingCities) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Search, contentDescription = "Search city")
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_city_search_input")
    )

    if (citySearchResults.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            citySearchResults.take(4).forEach { place ->
                Surface(
                    onClick = {
                        searchQuery = place.name
                        onSelectCity(place)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = place.displayLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    SwitchOptionRow(
        label = "Use Celsius (°C)",
        checked = unitCelsius,
        onCheckedChange = onToggleUnit
    )
}

private fun extractAlphaFraction(hex: String): Float {
    val clean = hex.trim().removePrefix("#")
    return if (clean.length == 8) {
        val alphaByte = clean.substring(0, 2).toIntOrNull(16) ?: 255
        (alphaByte / 255f).coerceIn(0f, 1f)
    } else {
        1f
    }
}

private fun applyAlphaToHex(baseHex: String, alphaFraction: Float): String {
    val clean = baseHex.trim().removePrefix("#")
    val rgb = when (clean.length) {
        8 -> clean.substring(2, 8)
        6 -> clean
        else -> "181C2E"
    }
    val alphaInt = (alphaFraction.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
    val alphaStr = alphaInt.toString(16).uppercase().padStart(2, '0')
    return "#$alphaStr$rgb"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupChildrenManager(
    groupLayer: WidgetLayer,
    children: List<WidgetLayer>,
    isStackGroup: Boolean,
    onChildrenUpdated: (List<WidgetLayer>) -> Unit,
    onAddChildToGroup: (LayerType) -> Unit,
    onUngroupLayer: () -> Unit
) {
    var selectedChildId by remember(groupLayer.id) { mutableStateOf<String?>(null) }
    val selectedIdx = children.indexOfFirst { it.id == selectedChildId }

    Text(
        text = if (isStackGroup) "Stack Items (${children.size}) — Tap to Edit" else "Overlap Children (${children.size}) — Tap to Edit",
        style = MaterialTheme.typography.labelLarge
    )

    if (children.isEmpty()) {
        Text(
            text = "No items inside group yet. Add children below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            children.forEachIndexed { idx, child ->
                val isSelected = child.id == selectedChildId
                Surface(
                    onClick = { selectedChildId = if (isSelected) null else child.id },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${idx + 1}. ${child.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${child.style.widthDp.roundToInt()}×${child.style.heightDp.roundToInt()}dp" +
                                    if (!isStackGroup) " · (${child.style.xDp.roundToInt()}, ${child.style.yDp.roundToInt()})" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (idx > 0) {
                                        val mutable = children.toMutableList()
                                        val moved = mutable.removeAt(idx)
                                        mutable.add(idx - 1, moved)
                                        onChildrenUpdated(mutable.mapIndexed { i, item -> item.copy(style = item.style.copy(zIndex = i)) })
                                    }
                                },
                                enabled = idx > 0,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move child up", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (idx < children.lastIndex) {
                                        val mutable = children.toMutableList()
                                        val moved = mutable.removeAt(idx)
                                        mutable.add(idx + 1, moved)
                                        onChildrenUpdated(mutable.mapIndexed { i, item -> item.copy(style = item.style.copy(zIndex = i)) })
                                    }
                                },
                                enabled = idx < children.lastIndex,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move child down", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { selectedChildId = if (isSelected) null else child.id },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit child", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    val updated = children.toMutableList().apply { removeAt(idx) }
                                        .mapIndexed { i, item -> item.copy(style = item.style.copy(zIndex = i)) }
                                    if (isSelected) selectedChildId = null
                                    onChildrenUpdated(updated)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove child", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedIdx in children.indices) {
        val child = children[selectedIdx]
        val updateChild: (WidgetLayer) -> Unit = { modified ->
            val updated = children.toMutableList().apply { set(selectedIdx, modified) }
            onChildrenUpdated(updated)
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Child: ${child.name}", style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(
                        onClick = {
                            updateChild(
                                child.copy(
                                    style = child.style.copy(
                                        backgroundColorHex = "#00000000",
                                        borderWidthDp = 0f,
                                        glassBlurEnabled = false,
                                        gradient = child.style.gradient.copy(enabled = false)
                                    )
                                )
                            )
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Transparent Bg")
                    }
                }

                if (!isStackGroup) {
                    SliderPropertyRow("Child X Offset", child.style.xDp, 0f..groupLayer.style.widthDp.coerceAtLeast(40f), "dp") {
                        updateChild(child.copy(style = child.style.copy(xDp = it)))
                    }
                    SliderPropertyRow("Child Y Offset", child.style.yDp, 0f..groupLayer.style.heightDp.coerceAtLeast(30f), "dp") {
                        updateChild(child.copy(style = child.style.copy(yDp = it)))
                    }
                }
                SliderPropertyRow("Child Width", child.style.widthDp, 20f..groupLayer.style.widthDp.coerceAtLeast(160f), "dp") {
                    updateChild(child.copy(style = child.style.copy(widthDp = it)))
                }
                SliderPropertyRow("Child Height", child.style.heightDp, 16f..groupLayer.style.heightDp.coerceAtLeast(120f), "dp") {
                    updateChild(child.copy(style = child.style.copy(heightDp = it)))
                }
                SliderPropertyRow("Font Size", child.style.fontSizeSp, 8f..48f, "sp") {
                    updateChild(child.copy(style = child.style.copy(fontSizeSp = it)))
                }

                ColorHexPickerRow(
                    label = "Child Foreground Color",
                    hexValue = child.style.foregroundColorHex,
                    onHexChange = { updateChild(child.copy(style = child.style.copy(foregroundColorHex = it))) }
                )

                when (val cp = child.payload) {
                    is LayerPayload.DigitalClock -> {
                        SwitchOptionRow("24-Hour Format", cp.is24Hour) {
                            updateChild(child.copy(payload = cp.copy(is24Hour = it)))
                        }
                    }
                    is LayerPayload.DateText -> {
                        SwitchOptionRow("Uppercase Date", cp.uppercase) {
                            updateChild(child.copy(payload = cp.copy(uppercase = it)))
                        }
                    }
                    is LayerPayload.CustomText -> {
                        OutlinedTextField(
                            value = cp.text,
                            onValueChange = { updateChild(child.copy(payload = cp.copy(text = it))) },
                            label = { Text("Child Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is LayerPayload.QuickNote -> {
                        OutlinedTextField(
                            value = cp.noteText,
                            onValueChange = { updateChild(child.copy(payload = cp.copy(noteText = it))) },
                            label = { Text("Note Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is LayerPayload.ShapeContainer -> {
                        Text("Child Shape", style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ShapeKind.entries.forEach { k ->
                                FilterChip(
                                    selected = cp.shapeKind == k,
                                    onClick = { updateChild(child.copy(payload = cp.copy(shapeKind = k))) },
                                    label = { Text(k.label) }
                                )
                            }
                        }
                        if (cp.shapeKind != ShapeKind.LINE && cp.shapeKind != ShapeKind.ARC) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = cp.filled,
                                    onClick = { updateChild(child.copy(payload = cp.copy(filled = true))) },
                                    label = { Text("Filled") }
                                )
                                FilterChip(
                                    selected = !cp.filled,
                                    onClick = { updateChild(child.copy(payload = cp.copy(filled = false))) },
                                    label = { Text("Outline Only") }
                                )
                            }
                        }
                    }
                    is LayerPayload.ProgressRing -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ProgressBarStyle.entries.forEach { st ->
                                FilterChip(
                                    selected = cp.style == st,
                                    onClick = { updateChild(child.copy(payload = cp.copy(style = st))) },
                                    label = { Text(st.label) }
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    Text(if (isStackGroup) "Add Item to Stack" else "Add Child Layer to Group", style = MaterialTheme.typography.labelMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            LayerType.DIGITAL_CLOCK to "Clock",
            LayerType.DATE_TEXT to "Date",
            LayerType.CUSTOM_TEXT to "Text",
            LayerType.SHAPE_CONTAINER to "Shape",
            LayerType.VECTOR_ICON to "Icon",
            LayerType.PROGRESS_BAR to "Progress Bar",
            LayerType.BATTERY_INFO to "Battery",
            LayerType.WEATHER_CARD to "Weather"
        ).forEach { (childType, childLabel) ->
            FilterChip(
                selected = false,
                onClick = { onAddChildToGroup(childType) },
                leadingIcon = { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp)) },
                label = { Text(childLabel) }
            )
        }
    }
    if (children.isNotEmpty()) {
        OutlinedButton(
            onClick = onUngroupLayer,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
        ) {
            Icon(Icons.Rounded.LayersClear, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ungroup to Individual Canvas Layers")
        }
    }
}

