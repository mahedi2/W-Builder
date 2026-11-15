package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AlignHorizontalCenter
import androidx.compose.material.icons.rounded.AlignVerticalCenter
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.LayerPayload
import com.example.domain.model.WidgetLayer

private enum class QuickToolPanel {
    NONE,
    ROTATION,
    TRANSPARENCY,
    ICON_PICKER,
    NUDGE_ALIGN
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickTransformBar(
    selectedLayer: WidgetLayer,
    onRotateBy: (Float) -> Unit,
    onSetRotation: (Float) -> Unit,
    onSetOpacity: (Float) -> Unit,
    onSelectIconKey: (String) -> Unit,
    onCenterH: () -> Unit,
    onCenterV: () -> Unit,
    onAlignEdge: (CanvasEdge) -> Unit,
    onNudge: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var activePanel by remember(selectedLayer.id) { mutableStateOf(QuickToolPanel.NONE) }
    val isVectorIcon = selectedLayer.payload is LayerPayload.VectorIcon

    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = activePanel == QuickToolPanel.ROTATION,
                        onClick = {
                            activePanel = if (activePanel == QuickToolPanel.ROTATION) QuickToolPanel.NONE else QuickToolPanel.ROTATION
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Rotate ${selectedLayer.style.rotationDegrees.toInt()}°") },
                        modifier = Modifier.testTag("quick_tool_rotate")
                    )
                }
                item {
                    FilterChip(
                        selected = activePanel == QuickToolPanel.TRANSPARENCY,
                        onClick = {
                            activePanel = if (activePanel == QuickToolPanel.TRANSPARENCY) QuickToolPanel.NONE else QuickToolPanel.TRANSPARENCY
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Opacity, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Opacity ${(selectedLayer.style.opacity * 100).toInt()}%") },
                        modifier = Modifier.testTag("quick_tool_opacity")
                    )
                }
                if (isVectorIcon) {
                    item {
                        FilterChip(
                            selected = activePanel == QuickToolPanel.ICON_PICKER,
                            onClick = {
                                activePanel = if (activePanel == QuickToolPanel.ICON_PICKER) QuickToolPanel.NONE else QuickToolPanel.ICON_PICKER
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Change Icon") },
                            modifier = Modifier.testTag("quick_tool_icon")
                        )
                    }
                }
                item {
                    FilterChip(
                        selected = activePanel == QuickToolPanel.NUDGE_ALIGN,
                        onClick = {
                            activePanel = if (activePanel == QuickToolPanel.NUDGE_ALIGN) QuickToolPanel.NONE else QuickToolPanel.NUDGE_ALIGN
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.AlignHorizontalCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Align & Nudge") },
                        modifier = Modifier.testTag("quick_tool_align")
                    )
                }
            }

            AnimatedVisibility(visible = activePanel != QuickToolPanel.NONE) {
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    when (activePanel) {
                        QuickToolPanel.ROTATION -> {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Rotation: ${selectedLayer.style.rotationDegrees.toInt()}°",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        TextButton(onClick = { onSetRotation(-90f) }) { Text("-90°") }
                                        TextButton(onClick = { onSetRotation(0f) }) { Text("0°") }
                                        TextButton(onClick = { onSetRotation(90f) }) { Text("90°") }
                                        TextButton(onClick = { onSetRotation(180f) }) { Text("180°") }
                                    }
                                }
                                Slider(
                                    value = selectedLayer.style.rotationDegrees,
                                    onValueChange = onSetRotation,
                                    valueRange = -180f..180f,
                                    modifier = Modifier.testTag("quick_rotation_slider")
                                )
                            }
                        }
                        QuickToolPanel.TRANSPARENCY -> {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Transparency: ${(selectedLayer.style.opacity * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(0.25f to "25%", 0.5f to "50%", 0.75f to "75%", 1f to "100%").forEach { (v, lbl) ->
                                            TextButton(onClick = { onSetOpacity(v) }) { Text(lbl) }
                                        }
                                    }
                                }
                                Slider(
                                    value = selectedLayer.style.opacity,
                                    onValueChange = onSetOpacity,
                                    valueRange = 0.05f..1f,
                                    modifier = Modifier.testTag("quick_opacity_slider")
                                )
                            }
                        }
                        QuickToolPanel.ICON_PICKER -> {
                            val currentKey = (selectedLayer.payload as? LayerPayload.VectorIcon)?.symbolKey.orEmpty()
                            FlowRow(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StudioVectorIconCatalog.forEach { key ->
                                    FilterChip(
                                        selected = currentKey == key,
                                        onClick = { onSelectIconKey(key) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = resolveVectorIcon(key),
                                                contentDescription = key,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text(key) }
                                    )
                                }
                            }
                        }
                        QuickToolPanel.NUDGE_ALIGN -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = onCenterH,
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Rounded.AlignHorizontalCenter, contentDescription = "Center horizontally")
                                        }
                                        IconButton(
                                            onClick = onCenterV,
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Rounded.AlignVerticalCenter, contentDescription = "Center vertically")
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onNudge(-2f, 0f) }) {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Nudge left")
                                        }
                                        IconButton(onClick = { onNudge(0f, -2f) }) {
                                            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Nudge up")
                                        }
                                        IconButton(onClick = { onNudge(0f, 2f) }) {
                                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Nudge down")
                                        }
                                        IconButton(onClick = { onNudge(2f, 0f) }) {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Nudge right")
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TextButton(
                                        onClick = { onAlignEdge(CanvasEdge.LEFT) },
                                        modifier = Modifier.testTag("align_edge_left")
                                    ) { Text("Left Edge") }
                                    TextButton(
                                        onClick = { onAlignEdge(CanvasEdge.TOP) },
                                        modifier = Modifier.testTag("align_edge_top")
                                    ) { Text("Top Edge") }
                                    TextButton(
                                        onClick = { onAlignEdge(CanvasEdge.RIGHT) },
                                        modifier = Modifier.testTag("align_edge_right")
                                    ) { Text("Right Edge") }
                                    TextButton(
                                        onClick = { onAlignEdge(CanvasEdge.BOTTOM) },
                                        modifier = Modifier.testTag("align_edge_bottom")
                                    ) { Text("Bottom") }
                                }
                            }
                        }
                        QuickToolPanel.NONE -> Unit
                    }
                }
            }
        }
    }
}
