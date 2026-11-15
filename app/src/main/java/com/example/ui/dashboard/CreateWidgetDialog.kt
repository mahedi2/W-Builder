package com.example.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.WidgetDimension

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateWidgetDialog(
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        dimension: WidgetDimension,
        customWidthDp: Float,
        customHeightDp: Float,
        includeStarterClock: Boolean,
        transparentBackground: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedDimension by remember { mutableStateOf(WidgetDimension.SIZE_4X2) }
    var customWidthText by remember { mutableStateOf("280") }
    var customHeightText by remember { mutableStateOf("180") }
    var includeStarterClock by remember { mutableStateOf(true) }
    var transparentBackground by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New Widget",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Widget Name") },
                    placeholder = { Text("e.g. Minimal Weather Pill") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_widget_name_input")
                )

                Text(
                    text = "Canvas Dimensions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WidgetDimension.entries.forEach { dim ->
                        FilterChip(
                            selected = selectedDimension == dim,
                            onClick = { selectedDimension = dim },
                            label = { Text(dim.label) },
                            modifier = Modifier.testTag("dimension_chip_${dim.name.lowercase()}")
                        )
                    }
                }

                if (selectedDimension == WidgetDimension.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customWidthText,
                            onValueChange = { customWidthText = it.filter(Char::isDigit) },
                            label = { Text("Width (dp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_width_input")
                        )
                        OutlinedTextField(
                            value = customHeightText,
                            onValueChange = { customHeightText = it.filter(Char::isDigit) },
                            label = { Text("Height (dp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_height_input")
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start with Clock + Date",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Turn off for a completely blank canvas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = includeStarterClock,
                        onCheckedChange = { includeStarterClock = it },
                        modifier = Modifier.testTag("starter_clock_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Transparent Window",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Let home screen wallpaper show behind components",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = transparentBackground,
                        onCheckedChange = { transparentBackground = it },
                        modifier = Modifier.testTag("transparent_window_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = customWidthText.toFloatOrNull() ?: 280f
                    val h = customHeightText.toFloatOrNull() ?: 180f
                    onCreate(name, selectedDimension, w, h, includeStarterClock, transparentBackground)
                },
                modifier = Modifier.testTag("confirm_create_widget_button")
            ) {
                Text("Open Canvas")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_create_widget_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateCustomComponentDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, category: String, widthDp: Float, heightDp: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Weather") }
    var widthText by remember { mutableStateOf("180") }
    var heightText by remember { mutableStateOf("84") }
    val presetCategories = remember { listOf("Weather", "Media", "Clock", "System", "General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Custom Component",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Component Name") },
                    placeholder = { Text("e.g. Frosted Weather Pill") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_component_name_input")
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetCategories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetCategories.drop(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { widthText = it.filter(Char::isDigit) },
                        label = { Text("Width (dp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it.filter(Char::isDigit) },
                        label = { Text("Height (dp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        name,
                        category,
                        widthText.toFloatOrNull() ?: 180f,
                        heightText.toFloatOrNull() ?: 84f
                    )
                },
                modifier = Modifier.testTag("confirm_create_component_button")
            ) {
                Text("Build Module")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
