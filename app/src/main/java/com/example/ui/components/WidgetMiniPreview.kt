package com.example.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.CustomComponent
import com.example.domain.model.FontWeightOption
import com.example.domain.model.GradientConfig
import com.example.domain.model.GradientType
import com.example.domain.model.LayerPayload
import com.example.domain.model.ProgressBarStyle
import com.example.domain.model.ShapeKind
import com.example.domain.model.StackOrientation
import com.example.domain.model.ToggleButtonKind
import com.example.domain.model.WidgetLayer
import com.example.domain.model.WidgetProject
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun parseHexColorSafe(hex: String, fallback: Color = Color.Transparent): Color {
    return runCatching {
        val cleaned = hex.trim().removePrefix("#")
        val parsed = when (cleaned.length) {
            6 -> 0xFF000000L or cleaned.toLong(16)
            8 -> cleaned.toLong(16)
            else -> return fallback
        }
        Color(parsed.toInt())
    }.getOrDefault(fallback)
}

fun resolveComposeFontFamily(fontFamilyName: String, fontAssetPath: String?): FontFamily {
    if (!fontAssetPath.isNullOrBlank()) {
        val file = File(fontAssetPath)
        if (file.exists() && file.isFile) {
            runCatching {
                val typeface = Typeface.createFromFile(file)
                return FontFamily(typeface)
            }
        }
    }
    return when (fontFamilyName.lowercase()) {
        "space grotesk" -> SpaceGroteskFontFamily
        "plus jakarta sans" -> PlusJakartaSansFontFamily
        "jetbrains mono", "monospace" -> JetBrainsMonoFontFamily
        "serif" -> FontFamily.Serif
        else -> FontFamily.SansSerif
    }
}

fun FontWeightOption.toComposeWeight(): FontWeight = when (this) {
    FontWeightOption.LIGHT -> FontWeight.Light
    FontWeightOption.NORMAL -> FontWeight.Normal
    FontWeightOption.MEDIUM -> FontWeight.Medium
    FontWeightOption.SEMIBOLD -> FontWeight.SemiBold
    FontWeightOption.BOLD -> FontWeight.Bold
}

fun buildGradientOrSolidBrush(
    solidHex: String,
    gradient: GradientConfig,
    fallbackColor: Color = Color.Transparent
): Brush {
    if (gradient.enabled) {
        val start = parseHexColorSafe(gradient.startColorHex, fallbackColor)
        val end = parseHexColorSafe(gradient.endColorHex, fallbackColor)
        if (gradient.type == GradientType.RADIAL) {
            return Brush.radialGradient(colors = listOf(start, end))
        }
        val rad = gradient.angleDegrees * (PI / 180.0)
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        return Brush.linearGradient(
            colors = listOf(start, end),
            start = Offset(0f, 0f),
            end = Offset(dx * 400f + 100f, dy * 400f + 100f)
        )
    }
    val color = parseHexColorSafe(solidHex, fallbackColor)
    return Brush.linearGradient(colors = listOf(color, color))
}

@Composable
fun WidgetMiniPreview(
    project: WidgetProject,
    modifier: Modifier = Modifier
) {
    WidgetCanvasSurfacePreview(
        designWidthDp = project.resolvedWidthDp,
        designHeightDp = project.resolvedHeightDp,
        backgroundColorHex = project.backgroundColorHex,
        backgroundGradient = project.backgroundGradient,
        cornerRadiusDp = project.backgroundCornerRadiusDp,
        layers = project.layers,
        modifier = modifier
    )
}

@Composable
fun CustomComponentMiniPreview(
    component: CustomComponent,
    modifier: Modifier = Modifier
) {
    WidgetCanvasSurfacePreview(
        designWidthDp = component.widthDp.coerceAtLeast(80f),
        designHeightDp = component.heightDp.coerceAtLeast(48f),
        backgroundColorHex = "#FF1C2237",
        backgroundGradient = GradientConfig(),
        cornerRadiusDp = 16f,
        layers = component.layers,
        modifier = modifier
    )
}

@Composable
fun WidgetCanvasSurfacePreview(
    designWidthDp: Float,
    designHeightDp: Float,
    backgroundColorHex: String,
    backgroundGradient: GradientConfig,
    cornerRadiusDp: Float,
    layers: List<WidgetLayer>,
    modifier: Modifier = Modifier
) {
    val safeW = designWidthDp.coerceAtLeast(40f)
    val safeH = designHeightDp.coerceAtLeast(40f)
    val bgBrush = remember(backgroundColorHex, backgroundGradient) {
        buildGradientOrSolidBrush(backgroundColorHex, backgroundGradient, Color(0xFF181C2E))
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(safeW / safeH)
                .clip(RoundedCornerShape(cornerRadiusDp.coerceIn(0f, 32f).dp))
                .background(bgBrush)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(cornerRadiusDp.coerceIn(0f, 32f).dp)
                )
        ) {
            val scale = min(maxWidth.value / safeW, maxHeight.value / safeH).coerceAtLeast(0.15f)

            if (layers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Empty Canvas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            } else {
                layers
                    .filter { it.isVisible }
                    .sortedBy { it.style.zIndex }
                    .forEach { layer ->
                        ScaledLayerPreviewItem(layer = layer, scale = scale)
                    }
            }
        }
    }
}

@Composable
private fun ScaledLayerPreviewItem(
    layer: WidgetLayer,
    scale: Float
) {
    val style = layer.style
    val wDp: Dp = (style.widthDp * scale).coerceAtLeast(10f).dp
    val hDp: Dp = (style.heightDp * scale).coerceAtLeast(10f).dp
    val xDp: Dp = (style.xDp * scale).dp
    val yDp: Dp = (style.yDp * scale).dp
    val radiusDp: Dp = (style.cornerRadiusDp * scale).dp
    val padDp: Dp = (style.paddingDp * scale).dp
    val marginDp: Dp = (style.marginDp * scale).dp
    val fgColor = parseHexColorSafe(style.foregroundColorHex, Color.White)
    val accentColor = parseHexColorSafe(style.accentColorHex, Color(0xFF38BDF8))
    val glowColor = parseHexColorSafe(style.glowColorHex, accentColor)
    val shadowColor = parseHexColorSafe(style.shadowColorHex, Color(0x66000000))
    val fontFamily = remember(style.fontFamilyName, style.fontAssetPath) {
        resolveComposeFontFamily(style.fontFamilyName, style.fontAssetPath)
    }
    val fontSizeSp = (style.fontSizeSp * scale).coerceIn(6f, 48f).sp
    val bgBrush = remember(style.backgroundColorHex, style.gradient, style.glassBlurEnabled, style.glassTintAlpha) {
        if (style.glassBlurEnabled) {
            val glassColor = Color.White.copy(alpha = style.glassTintAlpha.coerceIn(0.05f, 0.45f))
            Brush.linearGradient(listOf(glassColor, glassColor.copy(alpha = glassColor.alpha * 0.6f)))
        } else {
            buildGradientOrSolidBrush(style.backgroundColorHex, style.gradient, Color.Transparent)
        }
    }

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .size(width = wDp, height = hDp)
            .rotate(style.rotationDegrees)
            .alpha(style.opacity.coerceIn(0f, 1f))
            .padding(marginDp)
            .then(
                if (style.glowRadiusDp > 0f) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = glowColor.copy(alpha = 0.28f),
                            cornerRadius = CornerRadius(radiusDp.toPx(), radiusDp.toPx())
                        )
                    }
                } else Modifier
            )
            .then(
                if (style.shadowElevationDp > 0f) {
                    Modifier.shadow(
                        elevation = (style.shadowElevationDp * scale).coerceAtLeast(1f).dp,
                        shape = RoundedCornerShape(radiusDp),
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(radiusDp))
            .background(bgBrush)
            .then(
                if (style.borderWidthDp > 0f) {
                    Modifier.border(
                        width = (style.borderWidthDp * scale).coerceAtLeast(0.5f).dp,
                        color = parseHexColorSafe(style.borderColorHex, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(radiusDp)
                    )
                } else Modifier
            )
            .padding(padDp),
        contentAlignment = Alignment.CenterStart
    ) {
        when (val payload = layer.payload) {
            is LayerPayload.DigitalClock -> {
                Text(
                    text = if (payload.is24Hour) "14:30" else "02:30",
                    color = fgColor,
                    fontFamily = fontFamily,
                    fontWeight = style.fontWeight.toComposeWeight(),
                    fontSize = fontSizeSp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            is LayerPayload.AnalogClock -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(color = fgColor.copy(alpha = 0.25f), radius = r)
                    drawLine(
                        color = fgColor,
                        start = center,
                        end = Offset(center.x, center.y - r * 0.55f),
                        strokeWidth = (2.5f * scale).coerceAtLeast(1f),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = accentColor,
                        start = center,
                        end = Offset(center.x + r * 0.65f, center.y),
                        strokeWidth = (1.8f * scale).coerceAtLeast(1f),
                        cap = StrokeCap.Round
                    )
                }
            }
            is LayerPayload.DateText -> {
                Text(
                    text = if (payload.uppercase) "SUN, NOV 15" else "Sun, Nov 15",
                    color = fgColor,
                    fontFamily = fontFamily,
                    fontWeight = style.fontWeight.toComposeWeight(),
                    fontSize = fontSizeSp,
                    maxLines = 1
                )
            }
            is LayerPayload.DayOfWeek -> {
                Text(
                    text = if (payload.fullName) "SUNDAY" else "SUN",
                    color = fgColor,
                    fontFamily = fontFamily,
                    fontWeight = style.fontWeight.toComposeWeight(),
                    fontSize = fontSizeSp,
                    maxLines = 1
                )
            }
            is LayerPayload.CustomText -> {
                Text(
                    text = payload.text,
                    color = fgColor,
                    fontFamily = fontFamily,
                    fontWeight = style.fontWeight.toComposeWeight(),
                    fontSize = fontSizeSp,
                    maxLines = payload.maxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is LayerPayload.BatteryInfo -> {
                Column(verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryChargingFull,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size((14f * scale).coerceAtLeast(8f).dp)
                        )
                        Spacer(Modifier.width((3f * scale).dp))
                        Text(
                            text = "86%",
                            color = fgColor,
                            fontSize = fontSizeSp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    if (payload.showProgressBar) {
                        Spacer(Modifier.height((3f * scale).dp))
                        LinearProgressIndicator(
                            progress = { 0.86f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((4f * scale).coerceAtLeast(2f).dp),
                            color = accentColor,
                            trackColor = fgColor.copy(alpha = 0.2f)
                        )
                    }
                }
            }
            is LayerPayload.SystemRamStorage -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp)
                    )
                    Spacer(Modifier.width((3f * scale).dp))
                    Text(
                        text = "RAM 54%",
                        color = fgColor,
                        fontSize = fontSizeSp,
                        maxLines = 1
                    )
                }
            }
            is LayerPayload.NetworkStatus -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp)
                    )
                    Spacer(Modifier.width((3f * scale).dp))
                    Text(
                        text = "Wi-Fi",
                        color = fgColor,
                        fontSize = fontSizeSp,
                        maxLines = 1
                    )
                }
            }
            is LayerPayload.NextAlarm -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp)
                    )
                    Spacer(Modifier.width((3f * scale).dp))
                    Text(
                        text = "07:30 AM",
                        color = fgColor,
                        fontSize = fontSizeSp,
                        maxLines = 1
                    )
                }
            }
            is LayerPayload.WeatherCard -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((18f * scale).coerceAtLeast(10f).dp)
                    )
                    Spacer(Modifier.width((4f * scale).dp))
                    Column {
                        Text(
                            text = if (payload.unitCelsius) "21°C" else "70°F",
                            color = fgColor,
                            fontSize = fontSizeSp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = payload.cityName,
                            color = fgColor.copy(alpha = 0.72f),
                            fontSize = (fontSizeSp.value * 0.68f).coerceAtLeast(6f).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            is LayerPayload.WeatherLocation -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = accentColor, modifier = Modifier.size((14f * scale).coerceAtLeast(8f).dp))
                    Spacer(Modifier.width((3f * scale).dp))
                    Text(
                        text = "${payload.cityName} · ${if (payload.unitCelsius) "21°C" else "70°F"}",
                        color = fgColor,
                        fontSize = fontSizeSp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            is LayerPayload.WeatherForecast -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Today 22°", "Mon 20°", "Tue 19°").forEach { item ->
                        Text(item, color = fgColor, fontSize = (fontSizeSp.value * 0.8f).coerceAtLeast(6f).sp, maxLines = 1)
                    }
                }
            }
            is LayerPayload.LocationBadge -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = accentColor, modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp))
                    Spacer(Modifier.width((3f * scale).dp))
                    Text(payload.fallbackCity, color = fgColor, fontSize = fontSizeSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            is LayerPayload.CountdownTimer -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, tint = accentColor, modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp))
                    Spacer(Modifier.width((3f * scale).dp))
                    Text("7d · ${payload.title}", color = fgColor, fontSize = fontSizeSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            is LayerPayload.QuickToggleBar -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (payload.showFlashlight) {
                        Icon(Icons.Rounded.FlashlightOn, null, tint = accentColor, modifier = Modifier.size((12f * scale).coerceAtLeast(7f).dp))
                    }
                    if (payload.showMuteToggle) {
                        Icon(Icons.Rounded.VolumeUp, null, tint = fgColor, modifier = Modifier.size((12f * scale).coerceAtLeast(7f).dp))
                    }
                    if (payload.showPlayPause) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = accentColor, modifier = Modifier.size((12f * scale).coerceAtLeast(7f).dp))
                    }
                    if (payload.showWifiSettings) {
                        Icon(Icons.Rounded.Wifi, null, tint = fgColor, modifier = Modifier.size((12f * scale).coerceAtLeast(7f).dp))
                    }
                    if (payload.showRefresh) {
                        Icon(Icons.Rounded.Refresh, null, tint = fgColor, modifier = Modifier.size((12f * scale).coerceAtLeast(7f).dp))
                    }
                }
            }
            is LayerPayload.AudioVolumeBar -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VolumeUp, null, tint = accentColor, modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp))
                    Spacer(Modifier.width((3f * scale).dp))
                    Text("Vol 65%", color = fgColor, fontSize = fontSizeSp, maxLines = 1)
                }
            }
            is LayerPayload.GreetingBanner -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WavingHand, null, tint = accentColor, modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp))
                    Spacer(Modifier.width((3f * scale).dp))
                    Text("Hello, ${payload.userName}", color = fgColor, fontSize = fontSizeSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            is LayerPayload.WorldClock -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Public, null, tint = accentColor, modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp))
                    Spacer(Modifier.width((3f * scale).dp))
                    Text("${payload.cityLabel} 21:30", color = fgColor, fontSize = fontSizeSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            is LayerPayload.ProgressRing -> {
                if (payload.style == ProgressBarStyle.CIRCULAR_GAUGE) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = (4f * scale).coerceAtLeast(2f)
                        drawArc(fgColor.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(width = stroke))
                        drawArc(accentColor, -90f, 270f, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    }
                } else {
                    LinearProgressIndicator(
                        progress = { 0.72f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((5f * scale).coerceAtLeast(3f).dp)
                            .clip(RoundedCornerShape(50)),
                        color = accentColor,
                        trackColor = fgColor.copy(alpha = 0.22f)
                    )
                }
            }
            is LayerPayload.QuickNote -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (payload.isChecklist) Icons.Rounded.CheckBox else Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp)
                    )
                    Spacer(Modifier.width((3f * scale).dp))
                    Text(payload.noteText, color = fgColor, fontSize = fontSizeSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            is LayerPayload.ToggleButton -> {
                val iconVec = when (payload.kind) {
                    ToggleButtonKind.FLASHLIGHT -> Icons.Rounded.FlashlightOn
                    ToggleButtonKind.WIFI -> Icons.Rounded.Wifi
                    ToggleButtonKind.BLUETOOTH -> Icons.Rounded.Bluetooth
                    ToggleButtonKind.SILENT_VIBRATE -> Icons.Rounded.NotificationsOff
                    ToggleButtonKind.MUTE -> Icons.Rounded.VolumeOff
                    ToggleButtonKind.PLAY_PAUSE -> Icons.Rounded.PlayArrow
                    ToggleButtonKind.REFRESH -> Icons.Rounded.Refresh
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(iconVec, null, tint = accentColor, modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp))
                    if (payload.showLabel) {
                        Spacer(Modifier.width((3f * scale).dp))
                        Text(payload.kind.label, color = fgColor, fontSize = (fontSizeSp.value * 0.85f).coerceAtLeast(6f).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            is LayerPayload.CalendarView -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((14f * scale).coerceAtLeast(8f).dp)
                    )
                    Spacer(Modifier.width((4f * scale).dp))
                    Text(
                        text = "Design Sync 2PM",
                        color = fgColor,
                        fontSize = fontSizeSp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            is LayerPayload.ShapeContainer -> {
                val density = LocalDensity.current
                val radiusPx = with(density) { radiusDp.toPx() }
                val linePx = with(density) { (payload.lineThicknessDp * scale).coerceAtLeast(1.5f).dp.toPx() }
                ShapeVectorBox(
                    shapeKind = payload.shapeKind,
                    fgColor = fgColor,
                    accentColor = accentColor,
                    cornerRadiusPx = radiusPx,
                    lineThicknessPx = linePx,
                    startAngleDeg = payload.startAngleDeg,
                    sweepAngleDeg = payload.sweepAngleDeg,
                    customPathData = payload.customPathData,
                    filled = payload.filled
                )
            }
            is LayerPayload.VectorIcon -> {
                Icon(
                    imageVector = com.example.ui.editor.resolveVectorIcon(payload.symbolKey),
                    contentDescription = null,
                    tint = fgColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is LayerPayload.ImageMedia -> {
                val file = payload.assetPathOrUri?.let { File(it) }?.takeIf { it.exists() }
                if (file != null) {
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = fgColor.copy(alpha = 0.7f),
                            modifier = Modifier.size((18f * scale).coerceAtLeast(10f).dp)
                        )
                    }
                }
            }
            is LayerPayload.MusicPlayer -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = accentColor, modifier = Modifier.size((14f * scale).coerceAtLeast(8f).dp))
                    Text("Now Playing", color = fgColor, fontSize = fontSizeSp, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                    Icon(Icons.Rounded.SkipPrevious, null, tint = fgColor, modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp))
                    Icon(Icons.Rounded.PlayArrow, null, tint = accentColor, modifier = Modifier.size((15f * scale).coerceAtLeast(9f).dp))
                    Icon(Icons.Rounded.SkipNext, null, tint = fgColor, modifier = Modifier.size((13f * scale).coerceAtLeast(8f).dp))
                }
            }
            is LayerPayload.CustomComponentRef -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    payload.embeddedLayers.forEach { child ->
                        ScaledLayerPreviewItem(layer = child, scale = scale)
                    }
                }
            }
            is LayerPayload.OverlapGroup -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    payload.embeddedLayers.filter { it.isVisible }.sortedBy { it.style.zIndex }.forEach { child ->
                        ScaledLayerPreviewItem(layer = child, scale = scale)
                    }
                }
            }
            is LayerPayload.StackGroup -> {
                val visibleChildren = payload.embeddedLayers.filter { it.isVisible }.sortedBy { it.style.zIndex }
                androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val availW = maxWidth.value.coerceAtLeast(16f)
                    val availH = maxHeight.value.coerceAtLeast(12f)
                    val rawSpacing = (payload.spacingDp * scale).coerceAtLeast(0f)
                    val count = visibleChildren.size
                    val totalSpacing = rawSpacing * (count - 1).coerceAtLeast(0)
                    if (payload.orientation == StackOrientation.HORIZONTAL) {
                        val totalRawW = visibleChildren.sumOf { (it.style.widthDp * scale).coerceAtLeast(10f).toDouble() }.toFloat()
                        val fitScaleX = if (totalRawW > 0f && totalRawW + totalSpacing > availW) {
                            ((availW - totalSpacing).coerceAtLeast(10f * count) / totalRawW).coerceIn(0.25f, 1f)
                        } else 1f
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy((rawSpacing * fitScaleX).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            visibleChildren.forEach { child ->
                                ScaledLayerPreviewItem(
                                    layer = child.copy(
                                        style = child.style.copy(
                                            xDp = 0f,
                                            yDp = 0f,
                                            widthDp = child.style.widthDp * fitScaleX,
                                            heightDp = minOf(child.style.heightDp, availH / scale.coerceAtLeast(0.1f))
                                        )
                                    ),
                                    scale = scale
                                )
                            }
                        }
                    } else {
                        val totalRawH = visibleChildren.sumOf { (it.style.heightDp * scale).coerceAtLeast(10f).toDouble() }.toFloat()
                        val fitScaleY = if (totalRawH > 0f && totalRawH + totalSpacing > availH) {
                            ((availH - totalSpacing).coerceAtLeast(10f * count) / totalRawH).coerceIn(0.25f, 1f)
                        } else 1f
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy((rawSpacing * fitScaleY).dp)
                        ) {
                            visibleChildren.forEach { child ->
                                ScaledLayerPreviewItem(
                                    layer = child.copy(
                                        style = child.style.copy(
                                            xDp = 0f,
                                            yDp = 0f,
                                            widthDp = minOf(child.style.widthDp, availW / scale.coerceAtLeast(0.1f)),
                                            heightDp = child.style.heightDp * fitScaleY
                                        )
                                    ),
                                    scale = scale
                                )
                            }
                        }
                    }
                }
            }
            is LayerPayload.CustomXmlSnippet -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((14f * scale).coerceAtLeast(8f).dp)
                    )
                    Spacer(Modifier.width((4f * scale).dp))
                    Text(
                        text = if (payload.isJsonFormat) "JSON Layout" else "XML Layout",
                        color = fgColor,
                        fontSize = fontSizeSp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
