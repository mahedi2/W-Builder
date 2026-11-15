package com.example.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.CustomComponent
import com.example.domain.model.LayerCategory
import com.example.domain.model.LayerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentPickerSheet(
    savedComponents: List<CustomComponent>,
    onPickBuiltIn: (LayerType) -> Unit,
    onPickCustomComponent: (CustomComponent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Add Component",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            // Saved Custom Components Section
            if (savedComponents.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "My Custom Components",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(savedComponents, key = { it.id }) { component ->
                                Surface(
                                    onClick = {
                                        onPickCustomComponent(component)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .width(150.dp)
                                        .testTag("picker_custom_component_${component.id}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Icon(
                                            imageVector = Icons.Rounded.DashboardCustomize,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = component.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${component.layers.size} layers · ${component.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Built-in Categories
            val grouped = LayerType.entries
                .filter { it != LayerType.CUSTOM_COMPONENT_REF }
                .groupBy { it.category }

            LayerCategory.entries.forEach { category ->
                val itemsInCategory = grouped[category].orEmpty()
                if (itemsInCategory.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(itemsInCategory) { type ->
                                    Surface(
                                        onClick = {
                                            onPickBuiltIn(type)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .width(126.dp)
                                            .testTag("picker_type_${type.name.lowercase()}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = iconForLayerType(type),
                                                contentDescription = type.displayName,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = type.displayName,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconForLayerType(type: LayerType): ImageVector = when (type) {
    LayerType.DIGITAL_CLOCK -> Icons.Rounded.AccessTime
    LayerType.ANALOG_CLOCK -> Icons.Rounded.Schedule
    LayerType.WORLD_CLOCK -> Icons.Rounded.Public
    LayerType.DATE_TEXT -> Icons.Rounded.Today
    LayerType.DAY_OF_WEEK -> Icons.Rounded.Today
    LayerType.GREETING_BANNER -> Icons.Rounded.WavingHand
    LayerType.QUICK_NOTE -> Icons.Rounded.EditNote
    LayerType.CUSTOM_TEXT -> Icons.Rounded.TextFields
    LayerType.BATTERY_INFO -> Icons.Rounded.BatteryChargingFull
    LayerType.SYSTEM_RAM_STORAGE -> Icons.Rounded.Memory
    LayerType.PROGRESS_RING -> Icons.Rounded.DonutLarge
    LayerType.PROGRESS_BAR -> Icons.Rounded.LinearScale
    LayerType.AUDIO_VOLUME_BAR -> Icons.Rounded.VolumeUp
    LayerType.NETWORK_STATUS -> Icons.Rounded.Wifi
    LayerType.NEXT_ALARM -> Icons.Rounded.Alarm
    LayerType.WEATHER_CARD -> Icons.Rounded.WbSunny
    LayerType.WEATHER_LOCATION -> Icons.Rounded.LocationOn
    LayerType.WEATHER_FORECAST -> Icons.Rounded.Cloud
    LayerType.LOCATION_BADGE -> Icons.Rounded.Explore
    LayerType.CALENDAR_MINI -> Icons.Rounded.CalendarMonth
    LayerType.CALENDAR_AGENDA -> Icons.Rounded.EventNote
    LayerType.COUNTDOWN_TIMER -> Icons.Rounded.HourglassBottom
    LayerType.SHAPE_CONTAINER -> Icons.Rounded.RoundedCorner
    LayerType.GLASS_CARD -> Icons.Rounded.AutoAwesomeMosaic
    LayerType.OVERLAP_GROUP -> Icons.Rounded.Layers
    LayerType.STACK_GROUP -> Icons.Rounded.ViewAgenda
    LayerType.VECTOR_ICON -> Icons.Rounded.Star
    LayerType.IMAGE_MEDIA -> Icons.Rounded.Image
    LayerType.MUSIC_PLAYER -> Icons.Rounded.MusicNote
    LayerType.QUICK_TOGGLE_BAR -> Icons.Rounded.FlashlightOn
    LayerType.TOGGLE_FLASHLIGHT -> Icons.Rounded.FlashlightOn
    LayerType.TOGGLE_WIFI -> Icons.Rounded.Wifi
    LayerType.TOGGLE_BLUETOOTH -> Icons.Rounded.Bluetooth
    LayerType.TOGGLE_SILENT_VIBRATE -> Icons.Rounded.NotificationsOff
    LayerType.TOGGLE_MUTE -> Icons.Rounded.VolumeOff
    LayerType.TOGGLE_PLAY_PAUSE -> Icons.Rounded.PlayArrow
    LayerType.TOGGLE_REFRESH -> Icons.Rounded.Refresh
    LayerType.CUSTOM_COMPONENT_REF -> Icons.Rounded.DashboardCustomize
    LayerType.CUSTOM_XML_SNIPPET -> Icons.Rounded.Code
}
