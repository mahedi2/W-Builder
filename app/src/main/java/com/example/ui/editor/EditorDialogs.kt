package com.example.ui.editor

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.repository.DeviceTelemetrySnapshot
import com.example.data.repository.WeatherSnapshot
import com.example.domain.xml.CustomXmlParser
import com.example.domain.xml.CustomXmlSchema
import com.example.ui.components.CustomXmlComposeRenderer
import com.example.ui.theme.JetBrainsMonoFontFamily

private data class XmlIdentityPreset(
    val label: String,
    val markup: String
)

private val UniqueIdentityPresets = listOf(
    XmlIdentityPreset(
        label = "Cyber Glass",
        markup = """<LinearLayout orientation="vertical" padding="12" gradientStart="#CC0F172A" gradientEnd="#CC1E1B4B" cornerRadius="20" borderColor="#FF38BDF8" borderWidth="1" spacing="6">
  <Row gravity="center" spacing="8">
    <Icon name="bolt" color="#FF38BDF8" size="18" />
    <Text text="{{time_24}}" textSize="24" textColor="#FFFFFFFF" bold="true" fontFamily="JetBrains Mono" />
  </Row>
  <Divider color="#4438BDF8" thickness="1" />
  <Text text="{{weather_city}} · {{weather_temp}} · BAT {{battery}}%" textSize="11" textColor="#FF7DD3FC" fontFamily="JetBrains Mono" />
  <ProgressBar progress="{{battery}}" max="100" indicatorColor="#FF38BDF8" trackColor="#33FFFFFF" height="5" />
</LinearLayout>"""
    ),
    XmlIdentityPreset(
        label = "Editorial Serif",
        markup = """<LinearLayout orientation="vertical" padding="14" background="#E6FAF7F2" cornerRadius="22" borderColor="#FF1F2937" borderWidth="1" spacing="6">
  <Text text="{{greeting}}" textSize="12" textColor="#FF6B7280" fontFamily="Serif" />
  <Text text="{{time}}" textSize="28" textColor="#FF111827" bold="true" fontFamily="Serif" />
  <Divider color="#33111827" thickness="1" />
  <Row gravity="center" spacing="6">
    <Icon name="sun" color="#FFD97706" size="16" />
    <Text text="{{day}} · {{weather_temp}} · {{weather_cond}}" textSize="12" textColor="#FF374151" fontFamily="Serif" />
  </Row>
</LinearLayout>"""
    ),
    XmlIdentityPreset(
        label = "Aurora Pill",
        markup = """<LinearLayout orientation="horizontal" gravity="center" padding="12" gradientStart="#E60D9488" gradientEnd="#E64F46E5" cornerRadius="28" spacing="10">
  <Icon name="sun" color="#FFFDE047" size="24" />
  <Column spacing="2">
    <Text text="{{weather_temp}} · {{weather_city}}" textSize="16" textColor="#FFFFFFFF" bold="true" fontFamily="Space Grotesk" />
    <Text text="Feels {{weather_feels}} · {{humidity}} · Vol {{volume}}" textSize="11" textColor="#DDFFFFFF" />
  </Column>
</LinearLayout>"""
    ),
    XmlIdentityPreset(
        label = "Clear Wallpaper",
        markup = """<LinearLayout orientation="vertical" padding="8" background="#00000000" spacing="4">
  <Text text="{{time}}" textSize="30" textColor="#FFFFFFFF" bold="true" fontFamily="Space Grotesk" />
  <Row gravity="center" spacing="6">
    <Icon name="location" color="#FF5CE1E6" size="14" />
    <Text text="{{date}} · {{weather_temp}} · {{battery}}%" textSize="12" textColor="#EEFFFFFF" bold="true" />
  </Row>
</LinearLayout>"""
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomXmlEditorDialog(
    initialMarkup: String,
    initialIsJson: Boolean,
    telemetry: DeviceTelemetrySnapshot,
    weather: WeatherSnapshot?,
    onApply: (markup: String, isJson: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var markup by remember { mutableStateOf(initialMarkup) }
    var isJson by remember { mutableStateOf(initialIsJson) }
    val parseResult = remember(markup, isJson) {
        CustomXmlParser.parse(markup, forceJson = isJson)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Code / Custom UI Identity",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isJson) "JSON AST Mode" else "Android XML Snippet Mode",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Switch(
                        checked = isJson,
                        onCheckedChange = { isJson = it },
                        modifier = Modifier.testTag("xml_json_mode_switch")
                    )
                }

                Text(
                    text = "Unique UI Identity Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UniqueIdentityPresets.forEach { preset ->
                        FilterChip(
                            selected = markup.trim() == preset.markup.trim(),
                            onClick = {
                                isJson = false
                                markup = preset.markup
                            },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("xml_identity_preset_${preset.label.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                OutlinedTextField(
                    value = markup,
                    onValueChange = { markup = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    minLines = 6,
                    maxLines = 11,
                    isError = parseResult.isFailure,
                    supportingText = {
                        Text("Tags: <LinearLayout>, <Row>, <Column>, <Text>, <Icon>, <Divider>, <ProgressBar>. Attrs: gradientStart, gradientEnd, fontFamily, align.")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_xml_code_input")
                )

                Text(
                    text = "Tap a live data token to insert:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CustomXmlSchema.supportedTokens.forEach { token ->
                        AssistChip(
                            onClick = { markup += " $token" },
                            label = { Text(token, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                parseResult.fold(
                    onSuccess = { node ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF141829),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(10.dp)) {
                                CustomXmlComposeRenderer(
                                    node = node,
                                    scale = 1f,
                                    telemetry = telemetry,
                                    weather = weather
                                )
                            }
                        }
                    },
                    onFailure = { err ->
                        Text(
                            text = err.localizedMessage ?: "Invalid markup",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(markup, isJson) },
                enabled = parseResult.isSuccess,
                modifier = Modifier.testTag("apply_custom_xml_button")
            ) {
                Text("Apply to Canvas")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaveCustomComponentDialog(
    selectedCount: Int,
    onSave: (name: String, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    val categories = remember { listOf("Weather", "Media", "Clock", "System", "General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as Custom Component") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (selectedCount > 0) {
                        "Bundle $selectedCount selected layer(s) into a reusable module."
                    } else {
                        "Bundle all canvas layers into a reusable module."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Component Name") },
                    placeholder = { Text("e.g. Music Control Pill") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_component_name_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, category) },
                modifier = Modifier.testTag("confirm_save_custom_component_button")
            ) {
                Text("Save Component")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
