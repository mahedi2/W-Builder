package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.DeviceTelemetrySnapshot
import com.example.data.repository.WeatherSnapshot
import com.example.domain.model.CalendarMode
import com.example.domain.model.GaugeMetric
import com.example.domain.model.ImageScaleOption
import com.example.domain.model.LayerPayload
import com.example.domain.model.ProgressBarStyle
import com.example.domain.model.ShapeKind
import com.example.domain.model.StackOrientation
import com.example.domain.model.TapAction
import com.example.domain.model.TextAlignOption
import com.example.domain.model.ToggleButtonKind
import com.example.domain.model.WidgetLayer
import com.example.domain.resolver.DynamicDataResolver
import com.example.domain.resolver.ProgressMetricResolver
import com.example.domain.xml.CustomXmlParser
import com.example.ui.components.CustomXmlComposeRenderer
import com.example.ui.components.ShapeVectorBox
import com.example.ui.components.buildGradientOrSolidBrush
import com.example.ui.components.parseHexColorSafe
import com.example.ui.components.resolveComposeFontFamily
import com.example.ui.components.toComposeWeight
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

val StudioVectorIconCatalog = listOf(
    "bolt", "star", "sun", "cloud", "rain", "snow", "storm", "wind",
    "moon", "thermostat", "location", "compass", "heart", "home",
    "alarm", "timer", "bell", "search", "settings", "wifi",
    "bluetooth", "battery", "music", "headphones", "camera",
    "calendar", "rocket", "palette"
)

fun resolveVectorIcon(key: String): ImageVector = when (key.lowercase()) {
    "star" -> Icons.Rounded.Star
    "sun" -> Icons.Rounded.WbSunny
    "cloud" -> Icons.Rounded.Cloud
    "rain" -> Icons.Rounded.WaterDrop
    "snow" -> Icons.Rounded.AcUnit
    "storm" -> Icons.Rounded.Thunderstorm
    "wind" -> Icons.Rounded.Air
    "moon" -> Icons.Rounded.NightsStay
    "thermostat" -> Icons.Rounded.DeviceThermostat
    "location" -> Icons.Rounded.LocationOn
    "compass" -> Icons.Rounded.Explore
    "music" -> Icons.Rounded.MusicNote
    "headphones" -> Icons.Rounded.Headphones
    "camera" -> Icons.Rounded.PhotoCamera
    "calendar" -> Icons.Rounded.CalendarMonth
    "rocket" -> Icons.Rounded.RocketLaunch
    "palette" -> Icons.Rounded.Palette
    "heart" -> Icons.Rounded.Favorite
    "home" -> Icons.Rounded.Home
    "alarm" -> Icons.Rounded.Alarm
    "timer" -> Icons.Rounded.Timer
    "bell" -> Icons.Rounded.Notifications
    "search" -> Icons.Rounded.Search
    "settings" -> Icons.Rounded.Settings
    "wifi" -> Icons.Rounded.Wifi
    "bluetooth" -> Icons.Rounded.Bluetooth
    "battery" -> Icons.Rounded.BatteryChargingFull
    else -> Icons.Rounded.Bolt
}

private fun weatherConditionIcon(wmoCode: Int): ImageVector = when (wmoCode) {
    0, 1 -> Icons.Rounded.WbSunny
    2, 3, 45, 48 -> Icons.Rounded.Cloud
    in 51..67, in 80..82 -> Icons.Rounded.WaterDrop
    in 71..77, 85, 86 -> Icons.Rounded.AcUnit
    95, 96, 99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.WbSunny
}

@Composable
fun InteractiveCanvasSurface(
    uiState: EditorUiState,
    onSelectLayer: (String?) -> Unit,
    onDragStart: () -> Unit,
    onMoveLayer: (layerId: String, dxDp: Float, dyDp: Float) -> Unit,
    onResizeLayer: (layerId: String, dwDp: Float, dhDp: Float) -> Unit,
    onRotateLayer: (layerId: String, deltaDegrees: Float) -> Unit = { _, _ -> },
    onMediaAction: (Int) -> Unit = {},
    onVolumeAction: (Int) -> Unit = {},
    onQuickToggle: (TapAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val bgBrush = remember(uiState.backgroundColorHex, uiState.backgroundGradient) {
        buildGradientOrSolidBrush(
            solidHex = uiState.backgroundColorHex,
            gradient = uiState.backgroundGradient,
            fallbackColor = Color.Transparent
        )
    }
    val showCheckerboard = remember(uiState.backgroundColorHex, uiState.backgroundGradient) {
        !uiState.backgroundGradient.enabled && parseHexColorSafe(uiState.backgroundColorHex, Color(0xFF181C2E)).alpha < 0.99f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = { onSelectLayer(null) }
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        val safeW = uiState.canvasWidthDp.coerceAtLeast(60f)
        val safeH = uiState.canvasHeightDp.coerceAtLeast(40f)
        val fitScale = min(maxWidth.value / safeW, maxHeight.value / safeH) * uiState.zoomScale
        val renderScale = fitScale.coerceIn(0.45f, 2.2f)
        val renderedWidthDp = (safeW * renderScale).dp
        val renderedHeightDp = (safeH * renderScale).dp
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .size(width = renderedWidthDp, height = renderedHeightDp)
                .clip(RoundedCornerShape((uiState.backgroundCornerRadiusDp * renderScale).dp))
                .background(if (showCheckerboard) Color(0xFF0E1322) else Color.Transparent)
                .testTag("interactive_widget_canvas")
        ) {
            if (showCheckerboard) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellPx = 12.dp.toPx()
                    val cols = (size.width / cellPx).toInt() + 1
                    val rows = (size.height / cellPx).toInt() + 1
                    for (r in 0..rows) {
                        for (c in 0..cols) {
                            if ((r + c) % 2 == 0) {
                                drawRect(
                                    color = Color.White.copy(alpha = 0.06f),
                                    topLeft = Offset(c * cellPx, r * cellPx),
                                    size = androidx.compose.ui.geometry.Size(cellPx, cellPx)
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape((uiState.backgroundCornerRadiusDp * renderScale).dp)
                    )
            )
            // Optional Grid Lines
            if (uiState.showGridLines) {
                val gridColor = Color.White.copy(alpha = 0.08f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepPx = with(density) { (uiState.gridSnapDp * 2f * renderScale).dp.toPx() }
                        .coerceAtLeast(16f)
                    var x = stepPx
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += stepPx
                    }
                    var y = stepPx
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += stepPx
                    }
                }
            }

            if (uiState.layers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Tap + Add Component below",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }

            uiState.layers
                .filter { it.isVisible }
                .sortedBy { it.style.zIndex }
                .forEach { layer ->
                    val isSelected = layer.id in uiState.selectedLayerIds
                    DraggableCanvasLayer(
                        layer = layer,
                        scale = renderScale,
                        isSelected = isSelected,
                        isFlashlightOn = uiState.isFlashlightOn,
                        telemetry = uiState.telemetry,
                        weather = uiState.weather,
                        nowMillis = nowMillis,
                        onSelect = { onSelectLayer(layer.id) },
                        onDragStart = onDragStart,
                        onDragDelta = { dxDp, dyDp -> onMoveLayer(layer.id, dxDp, dyDp) },
                        onResizeDelta = { dwDp, dhDp -> onResizeLayer(layer.id, dwDp, dhDp) },
                        onRotateDelta = { degDelta -> onRotateLayer(layer.id, degDelta) },
                        onMediaAction = onMediaAction,
                        onVolumeAction = onVolumeAction,
                        onQuickToggle = onQuickToggle
                    )
                }
        }
    }
}

@Composable
private fun DraggableCanvasLayer(
    layer: WidgetLayer,
    scale: Float,
    isSelected: Boolean,
    isFlashlightOn: Boolean,
    telemetry: DeviceTelemetrySnapshot,
    weather: WeatherSnapshot?,
    nowMillis: Long,
    onSelect: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onResizeDelta: (Float, Float) -> Unit,
    onRotateDelta: (Float) -> Unit,
    onMediaAction: (Int) -> Unit,
    onVolumeAction: (Int) -> Unit,
    onQuickToggle: (TapAction) -> Unit
) {
    val density = LocalDensity.current
    val style = layer.style
    val wDp = (style.widthDp * scale).coerceAtLeast(24f).dp
    val hDp = (style.heightDp * scale).coerceAtLeast(20f).dp
    val xDp = (style.xDp * scale).dp
    val yDp = (style.yDp * scale).dp
    val cornerDp = (style.cornerRadiusDp * scale).dp
    val padDp = (style.paddingDp * scale).dp
    val marginDp = (style.marginDp * scale).dp
    val accentFallback = parseHexColorSafe(style.accentColorHex, Color(0xFF38BDF8))
    val glowColor = parseHexColorSafe(style.glowColorHex, accentFallback)
    val shadowColor = parseHexColorSafe(style.shadowColorHex, Color(0x66000000))

    val bgBrush = remember(style.backgroundColorHex, style.gradient, style.glassBlurEnabled, style.glassTintAlpha) {
        if (style.glassBlurEnabled) {
            val tint = Color.White.copy(alpha = style.glassTintAlpha.coerceIn(0.06f, 0.45f))
            Brush.linearGradient(listOf(tint, tint.copy(alpha = tint.alpha * 0.55f)))
        } else {
            buildGradientOrSolidBrush(style.backgroundColorHex, style.gradient, Color.Transparent)
        }
    }

    val selectionColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .size(width = wDp, height = hDp)
            .pointerInput(layer.id, scale, layer.isLocked) {
                if (!layer.isLocked) {
                    detectDragGestures(
                        onDragStart = {
                            onSelect()
                            onDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val dxDp = with(density) { dragAmount.x.toDp().value / scale }
                            val dyDp = with(density) { dragAmount.y.toDp().value / scale }
                            onDragDelta(dxDp, dyDp)
                        }
                    )
                }
            }
            .rotate(style.rotationDegrees)
            .alpha(style.opacity.coerceIn(0f, 1f))
            .padding(marginDp)
            .then(
                if (style.glowRadiusDp > 0f) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = glowColor.copy(alpha = 0.28f),
                            cornerRadius = CornerRadius(cornerDp.toPx(), cornerDp.toPx())
                        )
                    }
                } else Modifier
            )
            .then(
                if (style.shadowElevationDp > 0f) {
                    Modifier.shadow(
                        elevation = (style.shadowElevationDp * scale).coerceAtLeast(1f).dp,
                        shape = RoundedCornerShape(cornerDp),
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(cornerDp))
            .background(bgBrush)
            .then(
                if (style.borderWidthDp > 0f) {
                    Modifier.border(
                        width = (style.borderWidthDp * scale).coerceAtLeast(0.75f).dp,
                        color = parseHexColorSafe(style.borderColorHex, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(cornerDp)
                    )
                } else Modifier
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.8.dp,
                        color = selectionColor,
                        shape = RoundedCornerShape(cornerDp)
                    )
                } else Modifier
            )
            .clickable { onSelect() }
            .testTag("canvas_layer_${layer.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padDp),
            contentAlignment = when (style.textAlign) {
                TextAlignOption.START -> Alignment.CenterStart
                TextAlignOption.CENTER -> Alignment.Center
                TextAlignOption.END -> Alignment.CenterEnd
            }
        ) {
            LiveLayerContent(
                layer = layer,
                scale = scale,
                telemetry = telemetry,
                weather = weather,
                nowMillis = nowMillis,
                isFlashlightOn = isFlashlightOn,
                onMediaAction = onMediaAction,
                onVolumeAction = onVolumeAction,
                onQuickToggle = onQuickToggle
            )
        }

        // Top-right rotation handle & bottom-right corner resize handle when selected & unlocked
        if (isSelected && !layer.isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
                    .border(1.5.dp, Color.White, CircleShape)
                    .pointerInput(layer.id) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaDeg = with(density) { (dragAmount.x + dragAmount.y * 0.35f).toDp().value * 1.15f }
                                onRotateDelta(deltaDeg)
                            }
                        )
                    }
                    .testTag("rotate_handle_${layer.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.RotateRight,
                    contentDescription = "Rotate layer",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(selectionColor)
                    .border(1.5.dp, Color.White, CircleShape)
                    .pointerInput(layer.id, scale) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dwDp = with(density) { dragAmount.x.toDp().value / scale }
                                val dhDp = with(density) { dragAmount.y.toDp().value / scale }
                                onResizeDelta(dwDp, dhDp)
                            }
                        )
                    }
                    .testTag("resize_handle_${layer.id}")
            )
        }
    }
}

@Composable
fun LiveLayerContent(
    layer: WidgetLayer,
    scale: Float,
    telemetry: DeviceTelemetrySnapshot,
    weather: WeatherSnapshot?,
    nowMillis: Long,
    isFlashlightOn: Boolean = false,
    onMediaAction: (Int) -> Unit = {},
    onVolumeAction: (Int) -> Unit = {},
    onQuickToggle: (TapAction) -> Unit = {}
) {
    val style = layer.style
    val fgColor = parseHexColorSafe(style.foregroundColorHex, Color.White)
    val accentColor = parseHexColorSafe(style.accentColorHex, Color(0xFF38BDF8))
    val fontFamily = remember(style.fontFamilyName, style.fontAssetPath) {
        resolveComposeFontFamily(style.fontFamilyName, style.fontAssetPath)
    }
    val fontSize = (style.fontSizeSp * scale).coerceIn(7f, 64f).sp
    val lineHeight = (fontSize.value * style.lineHeightMultiplier.coerceIn(0.8f, 2.5f)).sp
    val letterSpacing = style.letterSpacingSp.sp
    val textAlign = when (style.textAlign) {
        TextAlignOption.START -> TextAlign.Start
        TextAlignOption.CENTER -> TextAlign.Center
        TextAlignOption.END -> TextAlign.End
    }

    when (val payload = layer.payload) {
        is LayerPayload.DigitalClock -> {
            val pattern = payload.customPattern?.takeIf { it.isNotBlank() } ?: when {
                payload.is24Hour && payload.showSeconds -> "HH:mm:ss"
                payload.is24Hour -> "HH:mm"
                payload.showSeconds && payload.showAmPm -> "hh:mm:ss a"
                payload.showSeconds -> "hh:mm:ss"
                payload.showAmPm -> "hh:mm a"
                else -> "hh:mm"
            }
            val formatted = remember(nowMillis / 1000L, pattern) {
                SimpleDateFormat(pattern, Locale.getDefault()).format(Date(nowMillis))
            }
            Text(
                text = formatted,
                color = fgColor,
                fontFamily = fontFamily,
                fontWeight = style.fontWeight.toComposeWeight(),
                fontSize = fontSize,
                lineHeight = lineHeight,
                letterSpacing = letterSpacing,
                textAlign = textAlign,
                maxLines = 1
            )
        }
        is LayerPayload.AnalogClock -> {
            val cal = remember(nowMillis / 1000L) {
                Calendar.getInstance().apply { timeInMillis = nowMillis }
            }
            val sec = cal.get(Calendar.SECOND)
            val minVal = cal.get(Calendar.MINUTE) + sec / 60f
            val hourVal = (cal.get(Calendar.HOUR) % 12) + minVal / 60f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                drawCircle(
                    color = fgColor.copy(alpha = 0.14f),
                    radius = radius
                )
                drawCircle(
                    color = fgColor.copy(alpha = 0.5f),
                    radius = radius,
                    style = Stroke(width = (payload.dialBorderWidthDp * scale).coerceAtLeast(1f))
                )
                if (payload.showHourTicks) {
                    for (i in 0 until 12) {
                        val angle = (i * 30.0 - 90.0) * PI / 180.0
                        val start = Offset(
                            center.x + (radius * 0.82f * cos(angle)).toFloat(),
                            center.y + (radius * 0.82f * sin(angle)).toFloat()
                        )
                        val end = Offset(
                            center.x + (radius * 0.94f * cos(angle)).toFloat(),
                            center.y + (radius * 0.94f * sin(angle)).toFloat()
                        )
                        drawLine(fgColor.copy(alpha = 0.7f), start, end, strokeWidth = 1.5f)
                    }
                }
                val hAngle = (hourVal * 30.0 - 90.0) * PI / 180.0
                drawLine(
                    color = fgColor,
                    start = center,
                    end = Offset(
                        center.x + (radius * 0.5f * cos(hAngle)).toFloat(),
                        center.y + (radius * 0.5f * sin(hAngle)).toFloat()
                    ),
                    strokeWidth = (3.5f * scale).coerceAtLeast(1.5f),
                    cap = StrokeCap.Round
                )
                val mAngle = (minVal * 6.0 - 90.0) * PI / 180.0
                drawLine(
                    color = fgColor,
                    start = center,
                    end = Offset(
                        center.x + (radius * 0.72f * cos(mAngle)).toFloat(),
                        center.y + (radius * 0.72f * sin(mAngle)).toFloat()
                    ),
                    strokeWidth = (2.4f * scale).coerceAtLeast(1f),
                    cap = StrokeCap.Round
                )
                if (payload.showSecondHand) {
                    val sAngle = (sec * 6.0 - 90.0) * PI / 180.0
                    drawLine(
                        color = accentColor,
                        start = center,
                        end = Offset(
                            center.x + (radius * 0.8f * cos(sAngle)).toFloat(),
                            center.y + (radius * 0.8f * sin(sAngle)).toFloat()
                        ),
                        strokeWidth = (1.5f * scale).coerceAtLeast(1f),
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(color = accentColor, radius = (3f * scale).coerceAtLeast(2f))
            }
        }
        is LayerPayload.DateText -> {
            val formatted = remember(nowMillis / 60_000L, payload.pattern, payload.uppercase) {
                val raw = runCatching {
                    SimpleDateFormat(payload.pattern, Locale.getDefault()).format(Date(nowMillis))
                }.getOrElse {
                    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
                }
                if (payload.uppercase) raw.uppercase(Locale.getDefault()) else raw
            }
            Text(
                text = formatted,
                color = fgColor,
                fontFamily = fontFamily,
                fontWeight = style.fontWeight.toComposeWeight(),
                fontSize = fontSize,
                lineHeight = lineHeight,
                letterSpacing = letterSpacing,
                textAlign = textAlign,
                maxLines = 1
            )
        }
        is LayerPayload.DayOfWeek -> {
            val pattern = if (payload.fullName) "EEEE" else "EEE"
            val formatted = remember(nowMillis / 60_000L, pattern, payload.uppercase) {
                val raw = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(nowMillis))
                if (payload.uppercase) raw.uppercase(Locale.getDefault()) else raw
            }
            Text(
                text = formatted,
                color = fgColor,
                fontFamily = fontFamily,
                fontWeight = style.fontWeight.toComposeWeight(),
                fontSize = fontSize,
                lineHeight = lineHeight,
                letterSpacing = letterSpacing,
                textAlign = textAlign,
                maxLines = 1
            )
        }
        is LayerPayload.CustomText -> {
            val template = payload.dynamicTemplate?.takeIf { it.isNotBlank() } ?: payload.text
            val resolved = DynamicDataResolver.resolveTokens(
                template = template,
                telemetry = telemetry,
                weather = weather,
                nowMillis = nowMillis
            )
            Text(
                text = resolved,
                color = fgColor,
                fontFamily = fontFamily,
                fontWeight = style.fontWeight.toComposeWeight(),
                fontSize = fontSize,
                lineHeight = lineHeight,
                letterSpacing = letterSpacing,
                textAlign = textAlign,
                maxLines = payload.maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
        is LayerPayload.BatteryInfo -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (payload.showChargingStatus) {
                        Icon(
                            imageVector = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size((16f * scale).coerceAtLeast(10f).dp)
                        )
                        Spacer(Modifier.width((4f * scale).dp))
                    }
                    if (payload.showPercentage) {
                        Text(
                            text = "${telemetry.batteryPercent}%",
                            color = fgColor,
                            fontFamily = fontFamily,
                            fontWeight = style.fontWeight.toComposeWeight(),
                            fontSize = fontSize,
                            maxLines = 1
                        )
                    }
                }
                if (payload.showProgressBar) {
                    Spacer(Modifier.height((4f * scale).dp))
                    LinearProgressIndicator(
                        progress = { telemetry.batteryPercent / 100f },
                        color = accentColor,
                        trackColor = fgColor.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((5f * scale).coerceAtLeast(3f).dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
        is LayerPayload.SystemRamStorage -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((3f * scale).dp)
            ) {
                if (payload.showRam) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Memory, null, tint = accentColor, modifier = Modifier.size((13f * scale).dp))
                        Spacer(Modifier.width((4f * scale).dp))
                        Text("RAM ${telemetry.ramUsedPercent}%", color = fgColor, fontSize = fontSize, maxLines = 1)
                    }
                }
                if (payload.showStorage) {
                    Text("Disk ${telemetry.storageUsedPercent}%", color = fgColor.copy(alpha = 0.85f), fontSize = fontSize, maxLines = 1)
                }
            }
        }
        is LayerPayload.NetworkStatus -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (telemetry.isNetworkConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size((16f * scale).coerceAtLeast(10f).dp)
                )
                Spacer(Modifier.width((5f * scale).dp))
                Text(
                    text = telemetry.networkLabel,
                    color = fgColor,
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    maxLines = 1
                )
            }
        }
        is LayerPayload.NextAlarm -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (payload.showIcon) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((15f * scale).coerceAtLeast(10f).dp)
                    )
                    Spacer(Modifier.width((5f * scale).dp))
                }
                Text(
                    text = telemetry.nextAlarmText ?: payload.emptyText,
                    color = fgColor,
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        is LayerPayload.WeatherCard -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (payload.showConditionIcon) {
                    Icon(
                        imageVector = weatherConditionIcon(weather?.weatherCode ?: 0),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((24f * scale).coerceAtLeast(12f).dp)
                    )
                    Spacer(Modifier.width((6f * scale).dp))
                }
                Column {
                    Text(
                        text = weather?.formatTemp(payload.unitCelsius) ?: "--°",
                        color = fgColor,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        maxLines = 1
                    )
                    Text(
                        text = weather?.cityName ?: payload.cityName,
                        color = fgColor.copy(alpha = 0.78f),
                        fontSize = (fontSize.value * 0.62f).coerceAtLeast(7f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (payload.showMinMax && weather != null) {
                        Text(
                            text = weather.formatMinMax(payload.unitCelsius),
                            color = accentColor,
                            fontSize = (fontSize.value * 0.56f).coerceAtLeast(7f).sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        is LayerPayload.WeatherLocation -> {
            val activeCity = if (payload.useDeviceLocation) {
                weather?.cityName ?: payload.cityName
            } else {
                payload.cityName
            }
            Column(verticalArrangement = Arrangement.spacedBy((2f * scale).dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((14f * scale).coerceAtLeast(10f).dp)
                    )
                    Spacer(Modifier.width((3f * scale).dp))
                    Text(
                        text = activeCity,
                        color = accentColor,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (fontSize.value * 0.6f).coerceAtLeast(8f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = weatherConditionIcon(weather?.weatherCode ?: 0),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((22f * scale).coerceAtLeast(12f).dp)
                    )
                    Spacer(Modifier.width((6f * scale).dp))
                    Text(
                        text = weather?.formatTemp(payload.unitCelsius) ?: "--°",
                        color = fgColor,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        maxLines = 1
                    )
                    Spacer(Modifier.width((8f * scale).dp))
                    Column {
                        Text(
                            text = weather?.conditionLabel ?: "Syncing",
                            color = fgColor.copy(alpha = 0.85f),
                            fontSize = (fontSize.value * 0.55f).coerceAtLeast(7f).sp,
                            maxLines = 1
                        )
                        if (payload.showFeelsLike && weather != null) {
                            Text(
                                text = weather.formatFeelsLike(payload.unitCelsius),
                                color = fgColor.copy(alpha = 0.68f),
                                fontSize = (fontSize.value * 0.48f).coerceAtLeast(6.5f).sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (payload.showHumidityWind && weather != null) {
                    Text(
                        text = "Humidity ${weather.humidityPercent}% · Wind ${weather.windKmh.toInt()} km/h",
                        color = fgColor.copy(alpha = 0.72f),
                        fontSize = (fontSize.value * 0.48f).coerceAtLeast(6.5f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        is LayerPayload.WeatherForecast -> {
            val count = payload.daysCount.coerceIn(1, 3)
            val days = weather?.forecastDays?.take(count).orEmpty()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((3f * scale).dp)
            ) {
                Text(
                    text = if (payload.useDeviceLocation) (weather?.cityName ?: payload.cityName) else payload.cityName,
                    color = accentColor,
                    fontSize = (fontSize.value * 0.82f).coerceAtLeast(7f).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (days.isEmpty()) {
                        listOf("Today", "Day 2", "Day 3").take(count).forEach { label ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, color = fgColor.copy(alpha = 0.75f), fontSize = (fontSize.value * 0.78f).coerceAtLeast(6.5f).sp)
                                Icon(Icons.Rounded.WbSunny, null, tint = accentColor, modifier = Modifier.size((14f * scale).coerceAtLeast(9f).dp))
                                Text("--°/--°", color = fgColor, fontSize = (fontSize.value * 0.75f).coerceAtLeast(6.5f).sp)
                            }
                        }
                    } else {
                        days.forEach { day ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day.dayLabel, color = fgColor.copy(alpha = 0.75f), fontSize = (fontSize.value * 0.78f).coerceAtLeast(6.5f).sp, maxLines = 1)
                                Icon(weatherConditionIcon(day.weatherCode), null, tint = accentColor, modifier = Modifier.size((14f * scale).coerceAtLeast(9f).dp))
                                Text(day.formatRange(payload.unitCelsius), color = fgColor, fontSize = (fontSize.value * 0.75f).coerceAtLeast(6.5f).sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        is LayerPayload.LocationBadge -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (payload.showPinIcon) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((15f * scale).coerceAtLeast(10f).dp)
                    )
                    Spacer(Modifier.width((4f * scale).dp))
                }
                val cityText = weather?.cityName ?: payload.fallbackCity
                val text = if (payload.showCoordinates && weather != null) {
                    "$cityText (${String.format(Locale.US, "%.2f, %.2f", weather.latitude, weather.longitude)})"
                } else {
                    cityText
                }
                Text(
                    text = text,
                    color = fgColor,
                    fontFamily = fontFamily,
                    fontWeight = style.fontWeight.toComposeWeight(),
                    fontSize = fontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        is LayerPayload.CountdownTimer -> {
            val remainingMs = (payload.targetEpochMillis - nowMillis).coerceAtLeast(0L)
            val days = remainingMs / 86_400_000L
            val hours = (remainingMs % 86_400_000L) / 3_600_000L
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size((18f * scale).coerceAtLeast(11f).dp)
                )
                Spacer(Modifier.width((6f * scale).dp))
                Column {
                    Text(
                        text = if (remainingMs == 0L) "Arrived!" else "${days}d ${hours}h",
                        color = fgColor,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        maxLines = 1
                    )
                    Text(
                        text = payload.title,
                        color = fgColor.copy(alpha = 0.75f),
                        fontSize = (fontSize.value * 0.62f).coerceAtLeast(7f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        is LayerPayload.QuickToggleBar -> {
            val iconSize = (18f * scale).coerceAtLeast(12f).dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (payload.showFlashlight) {
                    Icon(
                        imageVector = Icons.Rounded.FlashlightOn,
                        contentDescription = "Flashlight toggle",
                        tint = if (isFlashlightOn) Color(0xFFFBBF24) else accentColor,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQuickToggle(TapAction.ToggleFlashlight) }
                    )
                }
                if (payload.showMuteToggle) {
                    Icon(
                        imageVector = if (telemetry.isMediaMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = "Mute toggle",
                        tint = if (telemetry.isMediaMuted) Color(0xFFFF8A7A) else fgColor,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQuickToggle(TapAction.ToggleMute) }
                    )
                }
                if (payload.showPlayPause) {
                    Icon(
                        imageVector = if (telemetry.isMusicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play or pause media",
                        tint = accentColor,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQuickToggle(TapAction.MediaPlayPause) }
                    )
                }
                if (payload.showWifiSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = "Wi-Fi settings",
                        tint = if (telemetry.isNetworkConnected) fgColor else fgColor.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQuickToggle(TapAction.OpenWifiSettings) }
                    )
                }
                if (payload.showBluetoothSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = "Bluetooth settings",
                        tint = fgColor,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQuickToggle(TapAction.OpenBluetoothSettings) }
                    )
                }
                if (payload.showVolumeUp) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = "Volume up",
                        tint = fgColor,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQuickToggle(TapAction.VolumeUp) }
                    )
                }
                if (payload.showVolumeDown) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeDown,
                        contentDescription = "Volume down",
                        tint = fgColor,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQuickToggle(TapAction.VolumeDown) }
                    )
                }
                if (payload.showRefresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh widget",
                        tint = fgColor,
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQuickToggle(TapAction.RefreshWidget) }
                    )
                }
                if (payload.showStudioShortcut) {
                    Icon(
                        imageVector = Icons.Rounded.DashboardCustomize,
                        contentDescription = "Open W builder",
                        tint = fgColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(iconSize)
                            .clickable { onQuickToggle(TapAction.OpenStudio) }
                    )
                }
            }
        }
        is LayerPayload.AudioVolumeBar -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVolumeAction(AudioManager.ADJUST_RAISE) },
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (telemetry.isMediaMuted && payload.showMuteBadge) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                            contentDescription = "Volume",
                            tint = accentColor,
                            modifier = Modifier.size((15f * scale).coerceAtLeast(10f).dp)
                        )
                        Spacer(Modifier.width((4f * scale).dp))
                        Text(
                            text = "Media",
                            color = fgColor,
                            fontFamily = fontFamily,
                            fontSize = (fontSize.value * 0.85f).coerceAtLeast(8f).sp,
                            maxLines = 1
                        )
                    }
                    if (payload.showPercent) {
                        Text(
                            text = "${telemetry.mediaVolumePercent}%",
                            color = fgColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = (fontSize.value * 0.85f).coerceAtLeast(8f).sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height((4f * scale).dp))
                LinearProgressIndicator(
                    progress = { telemetry.mediaVolumePercent / 100f },
                    color = accentColor,
                    trackColor = fgColor.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((5f * scale).coerceAtLeast(3f).dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
        is LayerPayload.GreetingBanner -> {
            val hour = remember(nowMillis / 60_000L) {
                Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
            }
            val greet = when {
                hour < 12 -> "Good Morning"
                hour < 17 -> "Good Afternoon"
                else -> "Good Evening"
            }
            Column(verticalArrangement = Arrangement.spacedBy((2f * scale).dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WavingHand, null, tint = accentColor, modifier = Modifier.size((15f * scale).coerceAtLeast(10f).dp))
                    Spacer(Modifier.width((5f * scale).dp))
                    Text(
                        text = if (payload.userName.isNotBlank()) "$greet, ${payload.userName}" else greet,
                        color = fgColor,
                        fontFamily = fontFamily,
                        fontWeight = style.fontWeight.toComposeWeight(),
                        fontSize = fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (payload.showSubtitle) {
                    val sub = DynamicDataResolver.resolveTokens(payload.subtitleTemplate, telemetry, weather, nowMillis = nowMillis)
                    Text(
                        text = sub,
                        color = fgColor.copy(alpha = 0.72f),
                        fontSize = (fontSize.value * 0.65f).coerceAtLeast(7f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        is LayerPayload.WorldClock -> {
            val formatted = remember(nowMillis / 1000L, payload.timeZoneId, payload.is24Hour) {
                val fmt = SimpleDateFormat(if (payload.is24Hour) "HH:mm" else "hh:mm a", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone(payload.timeZoneId)
                }
                fmt.format(Date(nowMillis))
            }
            Column(verticalArrangement = Arrangement.spacedBy((1f * scale).dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Public, null, tint = accentColor, modifier = Modifier.size((12f * scale).coerceAtLeast(9f).dp))
                    Spacer(Modifier.width((4f * scale).dp))
                    Text(
                        text = payload.cityLabel,
                        color = accentColor,
                        fontSize = (fontSize.value * 0.68f).coerceAtLeast(7f).sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatted,
                    color = fgColor,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    maxLines = 1
                )
            }
        }
        is LayerPayload.ProgressRing -> {
            val res = remember(nowMillis / 1000L, payload, telemetry, weather) {
                ProgressMetricResolver.resolve(payload, telemetry, weather, nowMillis)
            }
            when (payload.style) {
                ProgressBarStyle.CIRCULAR_GAUGE -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = (payload.strokeWidthDp * scale).coerceAtLeast(2f)
                            drawArc(
                                color = fgColor.copy(alpha = 0.2f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke)
                            )
                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = 360f * res.fraction,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = res.displayText,
                                color = fgColor,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (fontSize.value * 0.82f).coerceAtLeast(8f).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (payload.showLabel) {
                                Text(
                                    text = res.subLabel,
                                    color = fgColor.copy(alpha = 0.72f),
                                    fontSize = (fontSize.value * 0.52f).coerceAtLeast(6.5f).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                ProgressBarStyle.LINEAR_BAR -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (payload.showLabel) {
                                Text(
                                    text = res.subLabel,
                                    color = fgColor.copy(alpha = 0.78f),
                                    fontFamily = fontFamily,
                                    fontSize = (fontSize.value * 0.72f).coerceAtLeast(7f).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width((6f * scale).dp))
                            }
                            Text(
                                text = res.displayText,
                                color = fgColor,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (fontSize.value * 0.8f).coerceAtLeast(8f).sp,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height((4f * scale).dp))
                        LinearProgressIndicator(
                            progress = { res.fraction },
                            color = accentColor,
                            trackColor = fgColor.copy(alpha = 0.22f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((payload.strokeWidthDp * scale).coerceIn(3f, 16f).dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
                ProgressBarStyle.SEGMENTED_BAR -> {
                    val count = payload.segmentsCount.coerceIn(4, 24)
                    val activeCount = (res.fraction * count).roundToInt().coerceIn(0, count)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (payload.showLabel) {
                                Text(
                                    text = res.subLabel,
                                    color = fgColor.copy(alpha = 0.78f),
                                    fontFamily = fontFamily,
                                    fontSize = (fontSize.value * 0.72f).coerceAtLeast(7f).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width((6f * scale).dp))
                            }
                            Text(
                                text = res.displayText,
                                color = fgColor,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (fontSize.value * 0.8f).coerceAtLeast(8f).sp,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height((4f * scale).dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy((2.5f * scale).coerceAtLeast(1.5f).dp)
                        ) {
                            for (i in 0 until count) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height((payload.strokeWidthDp * scale).coerceIn(4f, 16f).dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (i < activeCount) accentColor else fgColor.copy(alpha = 0.22f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
        is LayerPayload.QuickNote -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (payload.isChecklist || payload.showAccentBar) {
                    Icon(
                        imageVector = if (payload.isChecklist) Icons.Rounded.CheckBox else Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((16f * scale).coerceAtLeast(10f).dp)
                    )
                    Spacer(Modifier.width((5f * scale).dp))
                }
                Text(
                    text = payload.noteText,
                    color = fgColor,
                    fontFamily = fontFamily,
                    fontWeight = style.fontWeight.toComposeWeight(),
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    letterSpacing = letterSpacing,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        is LayerPayload.ToggleButton -> {
            val meta = when (payload.kind) {
                ToggleButtonKind.FLASHLIGHT -> ToggleVisualMeta(
                    icon = Icons.Rounded.FlashlightOn,
                    label = if (isFlashlightOn) "Torch ON" else "Flashlight",
                    tint = if (isFlashlightOn) Color(0xFFFBBF24) else accentColor,
                    action = TapAction.ToggleFlashlight
                )
                ToggleButtonKind.WIFI -> ToggleVisualMeta(
                    icon = if (telemetry.isNetworkConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                    label = telemetry.networkLabel,
                    tint = if (telemetry.isNetworkConnected) accentColor else fgColor.copy(alpha = 0.65f),
                    action = TapAction.OpenWifiSettings
                )
                ToggleButtonKind.BLUETOOTH -> ToggleVisualMeta(
                    icon = Icons.Rounded.Bluetooth,
                    label = "Bluetooth",
                    tint = accentColor,
                    action = TapAction.OpenBluetoothSettings
                )
                ToggleButtonKind.SILENT_VIBRATE -> ToggleVisualMeta(
                    icon = when (telemetry.ringerModeLabel) {
                        "Silent" -> Icons.Rounded.NotificationsOff
                        "Vibrate" -> Icons.Rounded.Vibration
                        else -> Icons.Rounded.NotificationsActive
                    },
                    label = telemetry.ringerModeLabel,
                    tint = if (telemetry.ringerModeLabel != "Normal") Color(0xFFFF8A7A) else accentColor,
                    action = TapAction.ToggleSilentVibrate
                )
                ToggleButtonKind.MUTE -> ToggleVisualMeta(
                    icon = if (telemetry.isMediaMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    label = if (telemetry.isMediaMuted) "Muted" else "Sound",
                    tint = if (telemetry.isMediaMuted) Color(0xFFFF8A7A) else accentColor,
                    action = TapAction.ToggleMute
                )
                ToggleButtonKind.PLAY_PAUSE -> ToggleVisualMeta(
                    icon = if (telemetry.isMusicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    label = if (telemetry.isMusicPlaying) "Pause" else "Play",
                    tint = accentColor,
                    action = TapAction.MediaPlayPause
                )
                ToggleButtonKind.REFRESH -> ToggleVisualMeta(
                    icon = Icons.Rounded.Refresh,
                    label = "Refresh",
                    tint = accentColor,
                    action = TapAction.RefreshWidget
                )
            }
            val effectiveAction = if (layer.tapAction !is TapAction.None) layer.tapAction else meta.action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onQuickToggle(effectiveAction) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = meta.label,
                    tint = meta.tint,
                    modifier = Modifier.size((18f * scale).coerceAtLeast(12f).dp)
                )
                if (payload.showLabel) {
                    Spacer(Modifier.width((5f * scale).dp))
                    Text(
                        text = meta.label,
                        color = fgColor,
                        fontFamily = fontFamily,
                        fontWeight = style.fontWeight.toComposeWeight(),
                        fontSize = (fontSize.value * 0.85f).coerceAtLeast(8f).sp,
                        letterSpacing = letterSpacing,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        is LayerPayload.CalendarView -> {
            if (payload.mode == CalendarMode.MINI_MONTH) {
                MiniMonthCalendarPreview(
                    fgColor = fgColor,
                    accentColor = accentColor,
                    scale = scale
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy((2f * scale).dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = accentColor, modifier = Modifier.size((14f * scale).dp))
                        Spacer(Modifier.width((4f * scale).dp))
                        Text("Agenda", color = accentColor, fontSize = (fontSize.value * 0.85f).coerceAtLeast(7f).sp, fontWeight = FontWeight.Bold)
                    }
                    val events = telemetry.upcomingEvents.take(payload.maxEvents)
                    if (!telemetry.calendarPermissionGranted) {
                        Text("Grant Calendar permission", color = fgColor.copy(alpha = 0.7f), fontSize = fontSize, maxLines = 1)
                    } else if (events.isEmpty()) {
                        Text("No upcoming events", color = fgColor.copy(alpha = 0.7f), fontSize = fontSize, maxLines = 1)
                    } else {
                        events.forEach { ev ->
                            val line = if (payload.showEventTime) "${ev.formattedTime} · ${ev.title}" else ev.title
                            Text(line, color = fgColor, fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        is LayerPayload.ShapeContainer -> {
            val density = LocalDensity.current
            val cornerPx = with(density) { (style.cornerRadiusDp * scale).dp.toPx() }
            val linePx = with(density) { (payload.lineThicknessDp * scale).coerceAtLeast(1.5f).dp.toPx() }
            ShapeVectorBox(
                shapeKind = payload.shapeKind,
                fgColor = fgColor,
                accentColor = accentColor,
                cornerRadiusPx = cornerPx,
                lineThicknessPx = linePx,
                startAngleDeg = payload.startAngleDeg,
                sweepAngleDeg = payload.sweepAngleDeg,
                customPathData = payload.customPathData,
                filled = payload.filled
            )
        }
        is LayerPayload.VectorIcon -> {
            Icon(
                imageVector = resolveVectorIcon(payload.symbolKey),
                contentDescription = payload.symbolKey,
                tint = fgColor,
                modifier = Modifier.fillMaxSize()
            )
        }
        is LayerPayload.ImageMedia -> {
            val file = payload.assetPathOrUri?.let { File(it) }?.takeIf { it.exists() }
            if (file != null) {
                val cScale = when (payload.scaleOption) {
                    ImageScaleOption.CROP -> ContentScale.Crop
                    ImageScaleOption.FIT -> ContentScale.Fit
                    ImageScaleOption.FILL -> ContentScale.FillBounds
                }
                AsyncImage(
                    model = file,
                    contentDescription = payload.lastAiPrompt ?: "Widget image",
                    contentScale = cScale,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = "Pick or generate image", tint = fgColor)
                }
            }
        }
        is LayerPayload.MusicPlayer -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (payload.showAlbumArt) {
                    Surface(
                        shape = RoundedCornerShape((8f * scale).dp),
                        color = accentColor.copy(alpha = 0.25f),
                        modifier = Modifier.size((28f * scale).coerceAtLeast(16f).dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MusicNote, null, tint = accentColor, modifier = Modifier.size((16f * scale).dp))
                        }
                    }
                    Spacer(Modifier.width((6f * scale).dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(telemetry.mediaTitle, color = fgColor, fontSize = fontSize, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!payload.compactMode) {
                        Text(telemetry.mediaArtist, color = fgColor.copy(alpha = 0.7f), fontSize = (fontSize.value * 0.75f).coerceAtLeast(7f).sp, maxLines = 1)
                    }
                }
                if (payload.showPlaybackControls) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = fgColor,
                        modifier = Modifier
                            .size((20f * scale).coerceAtLeast(14f).dp)
                            .clickable { onMediaAction(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
                    )
                    Icon(
                        imageVector = if (telemetry.isMusicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play or pause",
                        tint = accentColor,
                        modifier = Modifier
                            .size((24f * scale).coerceAtLeast(16f).dp)
                            .clickable { onMediaAction(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
                    )
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next track",
                        tint = fgColor,
                        modifier = Modifier
                            .size((20f * scale).coerceAtLeast(14f).dp)
                            .clickable { onMediaAction(KeyEvent.KEYCODE_MEDIA_NEXT) }
                    )
                }
            }
        }
        is LayerPayload.CustomComponentRef -> {
            Box(modifier = Modifier.fillMaxSize()) {
                payload.embeddedLayers.forEach { child ->
                    Box(
                        modifier = Modifier
                            .offset(x = (child.style.xDp * scale).dp, y = (child.style.yDp * scale).dp)
                            .size(
                                width = (child.style.widthDp * scale).coerceAtLeast(16f).dp,
                                height = (child.style.heightDp * scale).coerceAtLeast(14f).dp
                            )
                            .padding((child.style.paddingDp * scale).dp)
                    ) {
                        LiveLayerContent(
                            layer = child,
                            scale = scale,
                            telemetry = telemetry,
                            weather = weather,
                            nowMillis = nowMillis,
                            isFlashlightOn = isFlashlightOn,
                            onMediaAction = onMediaAction,
                            onVolumeAction = onVolumeAction,
                            onQuickToggle = onQuickToggle
                        )
                    }
                }
            }
        }
        is LayerPayload.OverlapGroup -> {
            Box(modifier = Modifier.fillMaxSize()) {
                payload.embeddedLayers.filter { it.isVisible }.sortedBy { it.style.zIndex }.forEach { child ->
                    val cs = child.style
                    CanvasGroupChildBox(
                        child = child,
                        scale = scale,
                        telemetry = telemetry,
                        weather = weather,
                        nowMillis = nowMillis,
                        isFlashlightOn = isFlashlightOn,
                        onMediaAction = onMediaAction,
                        onVolumeAction = onVolumeAction,
                        onQuickToggle = onQuickToggle,
                        modifier = Modifier
                            .offset(x = (cs.xDp * scale).dp, y = (cs.yDp * scale).dp)
                            .size(
                                width = (cs.widthDp * scale).coerceAtLeast(16f).dp,
                                height = (cs.heightDp * scale).coerceAtLeast(12f).dp
                            )
                            .rotate(cs.rotationDegrees)
                    )
                }
            }
        }
        is LayerPayload.StackGroup -> {
            val visibleChildren = payload.embeddedLayers.filter { it.isVisible }.sortedBy { it.style.zIndex }
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availW = maxWidth.value.coerceAtLeast(24f)
                val availH = maxHeight.value.coerceAtLeast(20f)
                val rawSpacing = (payload.spacingDp * scale).coerceAtLeast(0f)
                if (payload.orientation == StackOrientation.HORIZONTAL) {
                    val count = visibleChildren.size
                    val totalSpacing = rawSpacing * (count - 1).coerceAtLeast(0)
                    val totalRawW = visibleChildren.sumOf { (it.style.widthDp * scale).coerceAtLeast(16f).toDouble() }.toFloat()
                    val fitScaleX = if (totalRawW > 0f && totalRawW + totalSpacing > availW) {
                        ((availW - totalSpacing).coerceAtLeast(16f * count) / totalRawW).coerceIn(0.25f, 1f)
                    } else 1f
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy((rawSpacing * fitScaleX).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visibleChildren.forEach { child ->
                            val cs = child.style
                            val childW = ((cs.widthDp * scale).coerceAtLeast(16f) * fitScaleX).coerceAtLeast(14f).dp
                            val childH = (cs.heightDp * scale).coerceAtLeast(12f).coerceAtMost(availH).dp
                            CanvasGroupChildBox(
                                child = child,
                                scale = scale * fitScaleX.coerceAtLeast(0.7f),
                                telemetry = telemetry,
                                weather = weather,
                                nowMillis = nowMillis,
                                isFlashlightOn = isFlashlightOn,
                                onMediaAction = onMediaAction,
                                onVolumeAction = onVolumeAction,
                                onQuickToggle = onQuickToggle,
                                modifier = Modifier.size(width = childW, height = childH)
                            )
                        }
                    }
                } else {
                    val count = visibleChildren.size
                    val totalSpacing = rawSpacing * (count - 1).coerceAtLeast(0)
                    val totalRawH = visibleChildren.sumOf { (it.style.heightDp * scale).coerceAtLeast(12f).toDouble() }.toFloat()
                    val fitScaleY = if (totalRawH > 0f && totalRawH + totalSpacing > availH) {
                        ((availH - totalSpacing).coerceAtLeast(12f * count) / totalRawH).coerceIn(0.25f, 1f)
                    } else 1f
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy((rawSpacing * fitScaleY).dp)
                    ) {
                        visibleChildren.forEach { child ->
                            val cs = child.style
                            val childW = (cs.widthDp * scale).coerceAtLeast(16f).coerceAtMost(availW).dp
                            val childH = ((cs.heightDp * scale).coerceAtLeast(12f) * fitScaleY).coerceAtLeast(12f).dp
                            CanvasGroupChildBox(
                                child = child,
                                scale = scale * fitScaleY.coerceAtLeast(0.75f),
                                telemetry = telemetry,
                                weather = weather,
                                nowMillis = nowMillis,
                                isFlashlightOn = isFlashlightOn,
                                onMediaAction = onMediaAction,
                                onVolumeAction = onVolumeAction,
                                onQuickToggle = onQuickToggle,
                                modifier = Modifier.size(width = childW, height = childH)
                            )
                        }
                    }
                }
            }
        }
        is LayerPayload.CustomXmlSnippet -> {
            val parsed = remember(payload.rawMarkup, payload.isJsonFormat) {
                CustomXmlParser.parse(payload.rawMarkup, payload.isJsonFormat)
            }
            val node = parsed.getOrNull()
            if (node != null) {
                CustomXmlComposeRenderer(
                    node = node,
                    scale = scale,
                    telemetry = telemetry,
                    weather = weather,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Code, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("XML Parse Error", color = MaterialTheme.colorScheme.error, fontSize = fontSize)
                }
            }
        }
    }
}

@Composable
private fun MiniMonthCalendarPreview(
    fgColor: Color,
    accentColor: Color,
    scale: Float
) {
    val cal = remember { Calendar.getInstance() }
    val monthTitle = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time) }
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy((2f * scale).dp)) {
        Text(
            text = monthTitle,
            color = accentColor,
            fontSize = (10f * scale).coerceAtLeast(7f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Text(d, color = fgColor.copy(alpha = 0.6f), fontSize = (8f * scale).coerceAtLeast(6f).sp)
            }
        }
        val weeks = (1..daysInMonth).chunked(7).take(4)
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    Text(
                        text = day.toString(),
                        color = if (day == today) accentColor else fgColor,
                        fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                        fontSize = (8.5f * scale).coerceAtLeast(6f).sp
                    )
                }
            }
        }
    }
}

private data class ToggleVisualMeta(
    val icon: ImageVector,
    val label: String,
    val tint: Color,
    val action: TapAction
)

@Composable
private fun CanvasGroupChildBox(
    child: WidgetLayer,
    scale: Float,
    telemetry: DeviceTelemetrySnapshot,
    weather: WeatherSnapshot?,
    nowMillis: Long,
    isFlashlightOn: Boolean,
    onMediaAction: (Int) -> Unit,
    onVolumeAction: (Int) -> Unit,
    onQuickToggle: (TapAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = child.style
    val cornerDp = (cs.cornerRadiusDp * scale).dp
    val childBrush = remember(cs.backgroundColorHex, cs.gradient, cs.glassBlurEnabled, cs.glassTintAlpha) {
        if (cs.glassBlurEnabled) {
            val tint = Color.White.copy(alpha = cs.glassTintAlpha.coerceIn(0.06f, 0.45f))
            Brush.linearGradient(listOf(tint, tint.copy(alpha = tint.alpha * 0.55f)))
        } else {
            buildGradientOrSolidBrush(cs.backgroundColorHex, cs.gradient, Color.Transparent)
        }
    }
    Box(
        modifier = modifier
            .alpha(cs.opacity.coerceIn(0f, 1f))
            .clip(RoundedCornerShape(cornerDp))
            .background(childBrush)
            .then(
                if (cs.borderWidthDp > 0f) {
                    Modifier.border(
                        width = (cs.borderWidthDp * scale).coerceAtLeast(0.5f).dp,
                        color = parseHexColorSafe(cs.borderColorHex, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(cornerDp)
                    )
                } else Modifier
            )
            .padding((cs.paddingDp * scale).dp),
        contentAlignment = when (cs.textAlign) {
            TextAlignOption.START -> Alignment.CenterStart
            TextAlignOption.CENTER -> Alignment.Center
            TextAlignOption.END -> Alignment.CenterEnd
        }
    ) {
        LiveLayerContent(
            layer = child,
            scale = scale,
            telemetry = telemetry,
            weather = weather,
            nowMillis = nowMillis,
            isFlashlightOn = isFlashlightOn,
            onMediaAction = onMediaAction,
            onVolumeAction = onVolumeAction,
            onQuickToggle = onQuickToggle
        )
    }
}


