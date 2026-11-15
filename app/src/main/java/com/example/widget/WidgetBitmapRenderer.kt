package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.repository.DeviceTelemetrySnapshot
import com.example.data.repository.SystemTelemetryRepository
import com.example.data.repository.WeatherSnapshot
import com.example.domain.model.FontWeightOption
import com.example.domain.model.GaugeMetric
import com.example.domain.model.GradientConfig
import com.example.domain.model.GradientType
import com.example.domain.model.LayerPayload
import com.example.domain.model.ProgressBarStyle
import com.example.domain.model.ShapeKind
import com.example.domain.model.StackOrientation
import com.example.domain.model.TextAlignOption
import com.example.domain.model.ToggleButtonKind
import com.example.domain.model.WidgetLayer
import com.example.domain.model.WidgetProject
import com.example.domain.resolver.DynamicDataResolver
import com.example.domain.resolver.ProgressMetricResolver
import com.example.domain.xml.CustomXmlNode
import com.example.domain.xml.CustomXmlParser
import com.example.domain.xml.XmlOrientation
import com.example.ui.components.CustomShapePathParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders a [WidgetProject] into a memory-bounded [Bitmap] suitable for Jetpack Glance
 * `RemoteViews` while preserving custom `.ttf`/`.otf` fonts, gradients, shapes, analog clocks,
 * AI/Photo Picker images, and parsed Custom XML layouts.
 */
object WidgetBitmapRenderer {

    private const val MAX_WIDGET_BITMAP_BYTES = 2_200_000L // Well under RemoteViews 6*W*H cap

    fun renderProjectToBitmap(
        context: Context,
        project: WidgetProject,
        targetWidthDp: Float,
        targetHeightDp: Float,
        telemetry: DeviceTelemetrySnapshot,
        weather: WeatherSnapshot?
    ): Bitmap {
        val density = context.resources.displayMetrics.density.coerceIn(1f, 3f)
        val designW = project.resolvedWidthDp.coerceAtLeast(80f)
        val designH = project.resolvedHeightDp.coerceAtLeast(50f)

        var pxW = (targetWidthDp.coerceAtLeast(80f) * density).roundToInt()
        var pxH = (targetHeightDp.coerceAtLeast(50f) * density).roundToInt()

        // Enforce strict RemoteViews memory ceiling (4 bytes per ARGB_8888 pixel)
        val estimatedBytes = pxW.toLong() * pxH.toLong() * 4L
        if (estimatedBytes > MAX_WIDGET_BITMAP_BYTES) {
            val downscale = kotlin.math.sqrt(MAX_WIDGET_BITMAP_BYTES.toDouble() / estimatedBytes.toDouble())
            pxW = (pxW * downscale).roundToInt().coerceAtLeast(120)
            pxH = (pxH * downscale).roundToInt().coerceAtLeast(80)
        }

        val bitmap = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scaleX = pxW / designW
        val scaleY = pxH / designH
        val scale = min(scaleX, scaleY)

        // Draw widget container background (skip completely when transparent so home wallpaper shows)
        val bgRect = RectF(0f, 0f, pxW.toFloat(), pxH.toFloat())
        val bgRadius = project.backgroundCornerRadiusDp * scale
        val bgColorParsed = parseColorInt(project.backgroundColorHex, Color.TRANSPARENT)
        if (project.backgroundGradient.enabled || Color.alpha(bgColorParsed) > 0) {
            val bgPaint = createFillPaint(
                solidHex = project.backgroundColorHex,
                gradient = project.backgroundGradient,
                rect = bgRect,
                fallbackColor = bgColorParsed
            )
            canvas.drawRoundRect(bgRect, bgRadius, bgRadius, bgPaint)
        }

        val nowMillis = System.currentTimeMillis()
        project.layers
            .filter { it.isVisible }
            .sortedBy { it.style.zIndex }
            .forEach { layer ->
                drawLayer(
                    canvas = canvas,
                    layer = layer,
                    scale = scale,
                    telemetry = telemetry,
                    weather = weather,
                    nowMillis = nowMillis
                )
            }

        return bitmap
    }

    private fun drawLayer(
        canvas: Canvas,
        layer: WidgetLayer,
        scale: Float,
        telemetry: DeviceTelemetrySnapshot,
        weather: WeatherSnapshot?,
        nowMillis: Long
    ) {
        val style = layer.style
        val margin = style.marginDp * scale
        val left = style.xDp * scale + margin
        val top = style.yDp * scale + margin
        val width = (style.widthDp * scale - margin * 2f).coerceAtLeast(8f)
        val height = (style.heightDp * scale - margin * 2f).coerceAtLeast(8f)
        val rect = RectF(left, top, left + width, top + height)
        val radius = style.cornerRadiusDp * scale
        val alphaInt = (style.opacity.coerceIn(0f, 1f) * 255).roundToInt()

        canvas.save()
        if (style.rotationDegrees != 0f) {
            canvas.rotate(style.rotationDegrees, rect.centerX(), rect.centerY())
        }

        // Glow & Shadow backdrop halo
        if (style.glowRadiusDp > 0f) {
            val glowColor = applyAlpha(parseColorInt(style.glowColorHex, Color.parseColor("#FF5CE1E6")), 0.65f * style.opacity)
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = applyAlpha(glowColor, 0.18f)
                setShadowLayer((style.glowRadiusDp * scale).coerceIn(1f, 36f), 0f, 0f, glowColor)
            }
            canvas.drawRoundRect(rect, radius, radius, glowPaint)
        }
        if (style.shadowElevationDp > 0f) {
            val shadowColor = applyAlpha(parseColorInt(style.shadowColorHex, Color.argb(120, 0, 0, 0)), style.opacity)
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((18 * style.opacity).roundToInt(), 0, 0, 0)
                setShadowLayer(
                    (style.shadowElevationDp * scale).coerceIn(1f, 32f),
                    0f,
                    (style.shadowElevationDp * 0.45f * scale),
                    shadowColor
                )
            }
            canvas.drawRoundRect(rect, radius, radius, shadowPaint)
        }

        // Layer Background / Glassmorphism tint
        if (style.glassBlurEnabled) {
            val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((style.glassTintAlpha.coerceIn(0.05f, 0.45f) * alphaInt).roundToInt(), 255, 255, 255)
            }
            canvas.drawRoundRect(rect, radius, radius, glassPaint)
        } else if (style.gradient.enabled || !style.backgroundColorHex.equals("#00000000", ignoreCase = true)) {
            val bgPaint = createFillPaint(style.backgroundColorHex, style.gradient, rect, Color.TRANSPARENT)
            bgPaint.alpha = (bgPaint.alpha * style.opacity).roundToInt().coerceIn(0, 255)
            canvas.drawRoundRect(rect, radius, radius, bgPaint)
        }

        // Layer Border
        if (style.borderWidthDp > 0f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = (style.borderWidthDp * scale).coerceAtLeast(1f)
                color = applyAlpha(parseColorInt(style.borderColorHex, Color.argb(64, 255, 255, 255)), style.opacity)
            }
            canvas.drawRoundRect(rect, radius, radius, borderPaint)
        }

        val pad = style.paddingDp * scale
        val inner = RectF(
            rect.left + pad,
            rect.top + pad,
            (rect.right - pad).coerceAtLeast(rect.left + 4f),
            (rect.bottom - pad).coerceAtLeast(rect.top + 4f)
        )

        val fgColor = applyAlpha(parseColorInt(style.foregroundColorHex, Color.WHITE), style.opacity)
        val accentColor = applyAlpha(parseColorInt(style.accentColorHex, Color.parseColor("#FF38BDF8")), style.opacity)
        val typeface = resolveTypeface(style.fontFamilyName, style.fontAssetPath, style.fontWeight)
        val textSizePx = (style.fontSizeSp * scale).coerceAtLeast(10f)
        val lhMult = style.lineHeightMultiplier.coerceIn(0.8f, 2.5f)
        val lsSp = style.letterSpacingSp

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
                val text = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(nowMillis))
                drawTextInRect(canvas, text, inner, fgColor, textSizePx, typeface, style.textAlign, 1, lhMult, lsSp)
            }
            is LayerPayload.AnalogClock -> {
                drawAnalogClock(canvas, inner, fgColor, accentColor, payload, scale, nowMillis)
            }
            is LayerPayload.DateText -> {
                val raw = runCatching {
                    SimpleDateFormat(payload.pattern, Locale.getDefault()).format(Date(nowMillis))
                }.getOrElse {
                    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
                }
                val text = if (payload.uppercase) raw.uppercase(Locale.getDefault()) else raw
                drawTextInRect(canvas, text, inner, fgColor, textSizePx, typeface, style.textAlign, 1, lhMult, lsSp)
            }
            is LayerPayload.DayOfWeek -> {
                val raw = SimpleDateFormat(if (payload.fullName) "EEEE" else "EEE", Locale.getDefault()).format(Date(nowMillis))
                val text = if (payload.uppercase) raw.uppercase(Locale.getDefault()) else raw
                drawTextInRect(canvas, text, inner, fgColor, textSizePx, typeface, style.textAlign, 1, lhMult, lsSp)
            }
            is LayerPayload.CustomText -> {
                val raw = payload.dynamicTemplate?.takeIf { it.isNotBlank() } ?: payload.text
                val resolved = DynamicDataResolver.resolveTokens(raw, telemetry, weather, nowMillis = nowMillis)
                drawTextInRect(canvas, resolved, inner, fgColor, textSizePx, typeface, style.textAlign, payload.maxLines, lhMult, lsSp)
            }
            is LayerPayload.BatteryInfo -> {
                val label = buildString {
                    if (payload.showPercentage) append("${telemetry.batteryPercent}%")
                    if (payload.showChargingStatus && telemetry.isCharging) append(" ⚡")
                }.ifEmpty { "${telemetry.batteryPercent}%" }
                val textRect = RectF(inner.left, inner.top, inner.right, inner.top + inner.height() * 0.65f)
                drawTextInRect(canvas, label, textRect, fgColor, textSizePx, typeface, style.textAlign, 1)
                if (payload.showProgressBar) {
                    val barTop = inner.bottom - (6f * scale).coerceAtLeast(4f)
                    val barRect = RectF(inner.left, barTop, inner.right, inner.bottom)
                    drawProgressBar(canvas, barRect, telemetry.batteryPercent / 100f, fgColor, accentColor, scale)
                }
            }
            is LayerPayload.SystemRamStorage -> {
                val lines = buildList {
                    if (payload.showRam) add("RAM ${telemetry.ramUsedPercent}%")
                    if (payload.showStorage) add("Disk ${telemetry.storageUsedPercent}%")
                }.joinToString(" · ")
                drawTextInRect(canvas, lines, inner, fgColor, textSizePx, typeface, style.textAlign, 2)
            }
            is LayerPayload.NetworkStatus -> {
                drawTextInRect(canvas, telemetry.networkLabel, inner, fgColor, textSizePx, typeface, style.textAlign, 1)
            }
            is LayerPayload.NextAlarm -> {
                val label = "⏰ ${telemetry.nextAlarmText ?: payload.emptyText}"
                drawTextInRect(canvas, label, inner, fgColor, textSizePx, typeface, style.textAlign, 1)
            }
            is LayerPayload.WeatherCard -> {
                val temp = weather?.formatTemp(payload.unitCelsius) ?: "--°"
                val city = weather?.cityName ?: payload.cityName
                val cond = weather?.conditionLabel ?: "Syncing"
                val text = if (payload.showMinMax && weather != null) {
                    "$temp  $city\n$cond · ${weather.formatMinMax(payload.unitCelsius)}"
                } else {
                    "$temp  $city\n$cond"
                }
                drawTextInRect(canvas, text, inner, fgColor, textSizePx * 0.85f, typeface, style.textAlign, 2)
            }
            is LayerPayload.WeatherLocation -> {
                val city = if (payload.useDeviceLocation) (weather?.cityName ?: payload.cityName) else payload.cityName
                val temp = weather?.formatTemp(payload.unitCelsius) ?: "--°"
                val cond = weather?.conditionLabel ?: "Syncing"
                val extra = buildList {
                    if (payload.showFeelsLike && weather != null) add(weather.formatFeelsLike(payload.unitCelsius))
                    if (payload.showHumidityWind && weather != null) add("💧${weather.humidityPercent}% 💨${weather.windKmh.roundToInt()}km/h")
                }.joinToString(" · ")
                val text = buildString {
                    append("📍 $city\n$temp  $cond")
                    if (extra.isNotEmpty()) append("\n$extra")
                }
                drawTextInRect(canvas, text, inner, fgColor, textSizePx * 0.72f, typeface, style.textAlign, 3)
            }
            is LayerPayload.WeatherForecast -> {
                val city = if (payload.useDeviceLocation) (weather?.cityName ?: payload.cityName) else payload.cityName
                val count = payload.daysCount.coerceIn(1, 3)
                val days = weather?.forecastDays?.take(count).orEmpty()
                val forecastLine = if (days.isEmpty()) {
                    "3-Day Forecast · Syncing..."
                } else {
                    days.joinToString("  |  ") { "${it.dayLabel} ${it.formatRange(payload.unitCelsius)}" }
                }
                val text = "📍 $city\n$forecastLine"
                drawTextInRect(canvas, text, inner, fgColor, textSizePx * 0.82f, typeface, style.textAlign, 2)
            }
            is LayerPayload.LocationBadge -> {
                val city = weather?.cityName ?: payload.fallbackCity
                val prefix = if (payload.showPinIcon) "📍 " else ""
                val coords = if (payload.showCoordinates && weather != null) {
                    " (${String.format(Locale.US, "%.2f, %.2f", weather.latitude, weather.longitude)})"
                } else ""
                drawTextInRect(canvas, "$prefix$city$coords", inner, fgColor, textSizePx, typeface, style.textAlign, 1)
            }
            is LayerPayload.CountdownTimer -> {
                val remMs = (payload.targetEpochMillis - nowMillis).coerceAtLeast(0L)
                val days = remMs / 86_400_000L
                val hours = (remMs % 86_400_000L) / 3_600_000L
                val counter = if (remMs == 0L) "Arrived!" else "${days}d ${hours}h"
                drawTextInRect(canvas, "⏳ $counter\n${payload.title}", inner, fgColor, textSizePx * 0.82f, typeface, style.textAlign, 2)
            }
            is LayerPayload.QuickToggleBar -> {
                val slots = buildList {
                    if (payload.showFlashlight) add(if (SystemTelemetryRepository.isTorchEnabled) "💡" else "🔦")
                    if (payload.showMuteToggle) add(if (telemetry.isMediaMuted) "🔇" else "🔊")
                    if (payload.showPlayPause) add(if (telemetry.isMusicPlaying) "⏸" else "▶")
                    if (payload.showWifiSettings) add("📶")
                    if (payload.showBluetoothSettings) add("🔵")
                    if (payload.showVolumeUp) add("➕")
                    if (payload.showVolumeDown) add("➖")
                    if (payload.showRefresh) add("🔄")
                    if (payload.showStudioShortcut) add("🎨")
                }
                if (slots.isNotEmpty()) {
                    val slotW = inner.width() / slots.size
                    slots.forEachIndexed { idx, glyph ->
                        val slotRect = RectF(
                            inner.left + slotW * idx,
                            inner.top,
                            inner.left + slotW * (idx + 1),
                            inner.bottom
                        )
                        drawTextInRect(
                            canvas,
                            glyph,
                            slotRect,
                            if (idx == 0 && payload.showFlashlight && SystemTelemetryRepository.isTorchEnabled) accentColor else fgColor,
                            (textSizePx * 1.15f).coerceAtMost(slotW * 0.65f),
                            typeface,
                            TextAlignOption.CENTER,
                            1
                        )
                    }
                } else {
                    drawTextInRect(canvas, "⚡", inner, fgColor, textSizePx * 1.15f, typeface, TextAlignOption.CENTER, 1)
                }
            }
            is LayerPayload.AudioVolumeBar -> {
                val icon = if (telemetry.isMediaMuted) "🔇" else "🔊"
                val label = if (payload.showPercent) "$icon ${telemetry.mediaVolumePercent}%" else icon
                val textRect = RectF(inner.left, inner.top, inner.right, inner.top + inner.height() * 0.62f)
                drawTextInRect(canvas, label, textRect, fgColor, textSizePx, typeface, style.textAlign, 1)
                val barTop = inner.bottom - (6f * scale).coerceAtLeast(4f)
                drawProgressBar(canvas, RectF(inner.left, barTop, inner.right, inner.bottom), telemetry.mediaVolumePercent / 100f, fgColor, accentColor, scale)
            }
            is LayerPayload.GreetingBanner -> {
                val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
                val greet = when {
                    hour < 12 -> "Good Morning"
                    hour < 17 -> "Good Afternoon"
                    else -> "Good Evening"
                }
                val title = if (payload.userName.isNotBlank()) "$greet, ${payload.userName}" else greet
                val sub = if (payload.showSubtitle) {
                    "\n" + DynamicDataResolver.resolveTokens(payload.subtitleTemplate, telemetry, weather, nowMillis = nowMillis)
                } else ""
                drawTextInRect(canvas, "$title$sub", inner, fgColor, textSizePx * 0.85f, typeface, style.textAlign, 2)
            }
            is LayerPayload.WorldClock -> {
                val fmt = SimpleDateFormat(if (payload.is24Hour) "HH:mm" else "hh:mm a", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone(payload.timeZoneId)
                }
                val timeStr = fmt.format(Date(nowMillis))
                drawTextInRect(canvas, "${payload.cityLabel}\n$timeStr", inner, fgColor, textSizePx * 0.85f, typeface, style.textAlign, 2)
            }
            is LayerPayload.ProgressRing -> {
                val res = ProgressMetricResolver.resolve(payload, telemetry, weather, nowMillis)
                when (payload.style) {
                    ProgressBarStyle.CIRCULAR_GAUGE -> {
                        val cx = inner.centerX()
                        val cy = inner.centerY()
                        val stroke = (payload.strokeWidthDp * scale).coerceAtLeast(3f)
                        val r = (min(inner.width(), inner.height()) / 2f - stroke).coerceAtLeast(8f)
                        val arcRect = RectF(cx - r, cy - r, cx + r, cy + r)
                        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            this.style = Paint.Style.STROKE
                            this.strokeWidth = stroke
                            this.color = applyAlpha(fgColor, 0.22f)
                        }
                        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            this.style = Paint.Style.STROKE
                            this.strokeWidth = stroke
                            this.strokeCap = Paint.Cap.ROUND
                            this.color = accentColor
                        }
                        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)
                        canvas.drawArc(arcRect, -90f, 360f * res.fraction, false, arcPaint)
                        val centerLabel = if (payload.showLabel) "${res.displayText}\n${res.subLabel}" else res.displayText
                        drawTextInRect(canvas, centerLabel, inner, fgColor, (textSizePx * 0.72f).coerceAtLeast(9f), typeface, TextAlignOption.CENTER, 2, lhMult, lsSp)
                    }
                    ProgressBarStyle.LINEAR_BAR -> {
                        val headerText = if (payload.showLabel) "${res.subLabel}  ${res.displayText}" else res.displayText
                        val labelRect = RectF(inner.left, inner.top, inner.right, inner.top + inner.height() * 0.56f)
                        drawTextInRect(canvas, headerText, labelRect, fgColor, (textSizePx * 0.78f).coerceAtLeast(9f), typeface, style.textAlign, 1, lhMult, lsSp)
                        val barH = (payload.strokeWidthDp * scale).coerceIn(4f, inner.height() * 0.36f)
                        val barTop = (inner.bottom - barH).coerceAtLeast(labelRect.bottom + 2f)
                        drawProgressBar(canvas, RectF(inner.left, barTop, inner.right, inner.bottom), res.fraction, fgColor, accentColor, scale)
                    }
                    ProgressBarStyle.SEGMENTED_BAR -> {
                        val headerText = if (payload.showLabel) "${res.subLabel}  ${res.displayText}" else res.displayText
                        val labelRect = RectF(inner.left, inner.top, inner.right, inner.top + inner.height() * 0.54f)
                        drawTextInRect(canvas, headerText, labelRect, fgColor, (textSizePx * 0.78f).coerceAtLeast(9f), typeface, style.textAlign, 1, lhMult, lsSp)
                        val count = payload.segmentsCount.coerceIn(4, 24)
                        val gap = (3f * scale).coerceAtLeast(1.5f)
                        val segW = ((inner.width() - gap * (count - 1)) / count).coerceAtLeast(2f)
                        val barH = (payload.strokeWidthDp * scale).coerceIn(5f, inner.height() * 0.36f)
                        val barTop = (inner.bottom - barH).coerceAtLeast(labelRect.bottom + 2f)
                        val activeCount = (res.fraction * count).roundToInt().coerceIn(0, count)
                        val segRadius = barH / 2f
                        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
                        val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = applyAlpha(fgColor, 0.22f) }
                        for (i in 0 until count) {
                            val sl = inner.left + i * (segW + gap)
                            val sr = (sl + segW).coerceAtMost(inner.right)
                            canvas.drawRoundRect(
                                RectF(sl, barTop, sr, inner.bottom),
                                segRadius,
                                segRadius,
                                if (i < activeCount) activePaint else inactivePaint
                            )
                        }
                    }
                }
            }
            is LayerPayload.QuickNote -> {
                val prefix = when {
                    payload.isChecklist -> "☑ "
                    payload.showAccentBar -> "📝 "
                    else -> ""
                }
                drawTextInRect(canvas, "$prefix${payload.noteText}", inner, fgColor, textSizePx * 0.9f, typeface, style.textAlign, 4, lhMult, lsSp)
            }
            is LayerPayload.ToggleButton -> {
                val (glyph, statusLabel, activeTint) = when (payload.kind) {
                    ToggleButtonKind.FLASHLIGHT -> Triple(
                        if (SystemTelemetryRepository.isTorchEnabled) "💡" else "🔦",
                        if (SystemTelemetryRepository.isTorchEnabled) "Torch ON" else "Flashlight",
                        if (SystemTelemetryRepository.isTorchEnabled) Color.parseColor("#FFFBBF24") else accentColor
                    )
                    ToggleButtonKind.WIFI -> Triple("📶", telemetry.networkLabel, accentColor)
                    ToggleButtonKind.BLUETOOTH -> Triple("🔵", "Bluetooth", accentColor)
                    ToggleButtonKind.SILENT_VIBRATE -> Triple(
                        when (telemetry.ringerModeLabel) {
                            "Silent" -> "🔕"
                            "Vibrate" -> "📳"
                            else -> "🔔"
                        },
                        telemetry.ringerModeLabel,
                        if (telemetry.ringerModeLabel != "Normal") Color.parseColor("#FFFF8A7A") else accentColor
                    )
                    ToggleButtonKind.MUTE -> Triple(
                        if (telemetry.isMediaMuted) "🔇" else "🔊",
                        if (telemetry.isMediaMuted) "Muted" else "Sound",
                        if (telemetry.isMediaMuted) Color.parseColor("#FFFF8A7A") else fgColor
                    )
                    ToggleButtonKind.PLAY_PAUSE -> Triple(
                        if (telemetry.isMusicPlaying) "⏸" else "▶",
                        if (telemetry.isMusicPlaying) "Pause" else "Play",
                        accentColor
                    )
                    ToggleButtonKind.REFRESH -> Triple("🔄", "Refresh", accentColor)
                }
                val text = if (payload.showLabel) "$glyph $statusLabel" else glyph
                val tint = if (payload.showLabel) fgColor else activeTint
                drawTextInRect(canvas, text, inner, tint, textSizePx * if (payload.showLabel) 0.85f else 1.2f, typeface, TextAlignOption.CENTER, 1, lhMult, lsSp)
            }
            is LayerPayload.CalendarView -> {
                val events = telemetry.upcomingEvents.take(payload.maxEvents)
                val text = when {
                    !telemetry.calendarPermissionGranted -> "Calendar permission needed"
                    events.isEmpty() -> "No upcoming events"
                    else -> events.joinToString("\n") {
                        if (payload.showEventTime) "${it.formattedTime} · ${it.title}" else it.title
                    }
                }
                drawTextInRect(canvas, text, inner, fgColor, textSizePx, typeface, style.textAlign, payload.maxEvents + 1, lhMult, lsSp)
            }
            is LayerPayload.ShapeContainer -> {
                drawShapeContainer(canvas, inner, payload, fgColor, accentColor, radius, scale)
            }
            is LayerPayload.VectorIcon -> {
                val glyph = vectorGlyph(payload.symbolKey)
                drawTextInRect(canvas, glyph, inner, fgColor, min(inner.width(), inner.height()) * 0.7f, typeface, TextAlignOption.CENTER, 1)
            }
            is LayerPayload.ImageMedia -> {
                val file = payload.assetPathOrUri?.let { File(it) }?.takeIf { it.exists() }
                if (file != null) {
                    val decoded = decodeDownsampledBitmap(file, inner.width().roundToInt(), inner.height().roundToInt())
                    if (decoded != null) {
                        val path = android.graphics.Path().apply {
                            addRoundRect(inner, radius, radius, android.graphics.Path.Direction.CW)
                        }
                        canvas.save()
                        canvas.clipPath(path)
                        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = alphaInt
                        }
                        canvas.drawBitmap(decoded, null, inner, imgPaint)
                        canvas.restore()
                        decoded.recycle()
                    }
                }
            }
            is LayerPayload.MusicPlayer -> {
                val infoWidth = if (payload.showPlaybackControls) inner.width() * 0.50f else inner.width()
                val infoRect = RectF(inner.left, inner.top, inner.left + infoWidth, inner.bottom)
                val infoText = "♫ ${telemetry.mediaTitle}" + if (!payload.compactMode) "\n${telemetry.mediaArtist}" else ""
                drawTextInRect(canvas, infoText, infoRect, fgColor, textSizePx * 0.85f, typeface, TextAlignOption.START, 2)
                if (payload.showPlaybackControls) {
                    val controlsLeft = inner.left + inner.width() * 0.50f
                    val slotW = (inner.width() * 0.50f) / 3f
                    val playGlyph = if (telemetry.isMusicPlaying) "⏸" else "▶"
                    drawTextInRect(canvas, "⏮", RectF(controlsLeft, inner.top, controlsLeft + slotW, inner.bottom), fgColor, textSizePx * 1.15f, typeface, TextAlignOption.CENTER, 1)
                    drawTextInRect(canvas, playGlyph, RectF(controlsLeft + slotW, inner.top, controlsLeft + slotW * 2f, inner.bottom), accentColor, textSizePx * 1.3f, typeface, TextAlignOption.CENTER, 1)
                    drawTextInRect(canvas, "⏭", RectF(controlsLeft + slotW * 2f, inner.top, inner.right, inner.bottom), fgColor, textSizePx * 1.15f, typeface, TextAlignOption.CENTER, 1)
                }
            }
            is LayerPayload.CustomComponentRef -> {
                canvas.save()
                canvas.translate(inner.left, inner.top)
                payload.embeddedLayers.forEach { child ->
                    drawLayer(canvas, child, scale, telemetry, weather, nowMillis)
                }
                canvas.restore()
            }
            is LayerPayload.OverlapGroup -> {
                canvas.save()
                canvas.translate(inner.left, inner.top)
                canvas.clipRect(0f, 0f, inner.width(), inner.height())
                payload.embeddedLayers.filter { it.isVisible }.sortedBy { it.style.zIndex }.forEach { child ->
                    drawLayer(canvas, child, scale, telemetry, weather, nowMillis)
                }
                canvas.restore()
            }
            is LayerPayload.StackGroup -> {
                canvas.save()
                canvas.translate(inner.left, inner.top)
                canvas.clipRect(0f, 0f, inner.width(), inner.height())
                val visible = payload.embeddedLayers.filter { it.isVisible }.sortedBy { it.style.zIndex }
                val innerW = (inner.width() / scale).coerceAtLeast(16f)
                val innerH = (inner.height() / scale).coerceAtLeast(14f)
                val spacingTotal = payload.spacingDp * max(0, visible.size - 1)
                val xFitScale = if (payload.orientation == StackOrientation.HORIZONTAL && visible.isNotEmpty()) {
                    val totalW = visible.sumOf { it.style.widthDp.toDouble() }.toFloat() + spacingTotal
                    if (totalW > innerW) ((innerW - spacingTotal).coerceAtLeast(24f) / visible.sumOf { it.style.widthDp.toDouble() }.toFloat()).coerceIn(0.25f, 1f) else 1f
                } else 1f
                val yFitScale = if (payload.orientation == StackOrientation.VERTICAL && visible.isNotEmpty()) {
                    val totalH = visible.sumOf { it.style.heightDp.toDouble() }.toFloat() + spacingTotal
                    if (totalH > innerH) ((innerH - spacingTotal).coerceAtLeast(20f) / visible.sumOf { it.style.heightDp.toDouble() }.toFloat()).coerceIn(0.25f, 1f) else 1f
                } else 1f
                var cursorX = 0f
                var cursorY = 0f
                visible.forEach { child ->
                    val fitW = if (payload.orientation == StackOrientation.HORIZONTAL) {
                        (child.style.widthDp * xFitScale).coerceAtLeast(16f)
                    } else {
                        min(child.style.widthDp, innerW).coerceAtLeast(16f)
                    }
                    val fitH = if (payload.orientation == StackOrientation.VERTICAL) {
                        (child.style.heightDp * yFitScale).coerceAtLeast(14f)
                    } else {
                        min(child.style.heightDp, innerH).coerceAtLeast(14f)
                    }
                    val positioned = child.copy(
                        style = child.style.copy(
                            xDp = if (payload.orientation == StackOrientation.HORIZONTAL) cursorX else 0f,
                            yDp = if (payload.orientation == StackOrientation.VERTICAL) cursorY else 0f,
                            widthDp = fitW,
                            heightDp = fitH
                        )
                    )
                    drawLayer(canvas, positioned, scale, telemetry, weather, nowMillis)
                    if (payload.orientation == StackOrientation.HORIZONTAL) {
                        cursorX += fitW + payload.spacingDp
                    } else {
                        cursorY += fitH + payload.spacingDp
                    }
                }
                canvas.restore()
            }
            is LayerPayload.CustomXmlSnippet -> {
                val node = CustomXmlParser.parse(payload.rawMarkup, payload.isJsonFormat).getOrNull()
                if (node != null) {
                    drawXmlNode(canvas, node, inner, scale, telemetry, weather, nowMillis)
                }
            }
        }

        canvas.restore()
    }

    private fun vectorGlyph(key: String): String = when (key.lowercase()) {
        "star" -> "★"
        "sun" -> "☀"
        "cloud" -> "☁"
        "rain" -> "🌧"
        "snow" -> "❄"
        "storm" -> "⛈"
        "wind" -> "💨"
        "moon" -> "🌙"
        "thermostat" -> "🌡"
        "location" -> "📍"
        "compass" -> "🧭"
        "music" -> "♫"
        "headphones" -> "🎧"
        "camera" -> "📷"
        "calendar" -> "📅"
        "rocket" -> "🚀"
        "palette" -> "🎨"
        "heart" -> "♥"
        "home" -> "🏠"
        "alarm" -> "⏰"
        "timer" -> "⏱"
        "bell" -> "🔔"
        "search" -> "🔍"
        "settings" -> "⚙"
        "wifi" -> "📶"
        "bluetooth" -> "🔵"
        "battery" -> "🔋"
        else -> "⚡"
    }

    private fun drawXmlNode(
        canvas: Canvas,
        node: CustomXmlNode,
        bounds: RectF,
        scale: Float,
        telemetry: DeviceTelemetrySnapshot,
        weather: WeatherSnapshot?,
        nowMillis: Long
    ) {
        val pad = node.paddingDp * scale
        val r = node.cornerRadiusDp * scale
        if (node is CustomXmlNode.Container && !node.gradientStartHex.isNullOrBlank() && !node.gradientEndHex.isNullOrBlank()) {
            val gradConfig = GradientConfig(
                enabled = true,
                startColorHex = node.gradientStartHex,
                endColorHex = node.gradientEndHex,
                angleDegrees = node.gradientAngleDegrees
            )
            val gradPaint = createFillPaint("#00000000", gradConfig, bounds, Color.TRANSPARENT)
            gradPaint.alpha = (gradPaint.alpha * node.alpha).roundToInt().coerceIn(0, 255)
            canvas.drawRoundRect(bounds, r, r, gradPaint)
        } else {
            node.backgroundColorHex?.let { hex ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = applyAlpha(parseColorInt(hex, Color.TRANSPARENT), node.alpha)
                }
                canvas.drawRoundRect(bounds, r, r, paint)
            }
        }
        if (node is CustomXmlNode.Container && node.borderWidthDp > 0f && !node.borderColorHex.isNullOrBlank()) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (node.borderWidthDp * scale).coerceAtLeast(1f)
                color = applyAlpha(parseColorInt(node.borderColorHex, Color.WHITE), node.alpha)
            }
            canvas.drawRoundRect(bounds, r, r, borderPaint)
        }
        val inner = RectF(bounds.left + pad, bounds.top + pad, (bounds.right - pad).coerceAtLeast(bounds.left + 4f), (bounds.bottom - pad).coerceAtLeast(bounds.top + 4f))
        when (node) {
            is CustomXmlNode.Container -> {
                if (node.children.isEmpty()) return
                val count = node.children.size
                val spacing = node.spacingDp * scale
                if (node.orientation == XmlOrientation.HORIZONTAL) {
                    val slotW = ((inner.width() - spacing * (count - 1)) / count).coerceAtLeast(8f)
                    node.children.forEachIndexed { idx, child ->
                        val childLeft = inner.left + idx * (slotW + spacing)
                        drawXmlNode(canvas, child, RectF(childLeft, inner.top, childLeft + slotW, inner.bottom), scale, telemetry, weather, nowMillis)
                    }
                } else if (node.orientation == XmlOrientation.VERTICAL) {
                    val slotH = ((inner.height() - spacing * (count - 1)) / count).coerceAtLeast(10f)
                    node.children.forEachIndexed { idx, child ->
                        val childTop = inner.top + idx * (slotH + spacing)
                        drawXmlNode(canvas, child, RectF(inner.left, childTop, inner.right, childTop + slotH), scale, telemetry, weather, nowMillis)
                    }
                } else {
                    node.children.forEach { child ->
                        drawXmlNode(canvas, child, inner, scale, telemetry, weather, nowMillis)
                    }
                }
            }
            is CustomXmlNode.Text -> {
                val resolved = DynamicDataResolver.resolveTokens(node.text, telemetry, weather, nowMillis = nowMillis)
                val color = applyAlpha(parseColorInt(node.textColorHex, Color.WHITE), node.alpha)
                val tf = resolveTypeface(node.fontFamily, null, if (node.bold) FontWeightOption.BOLD else FontWeightOption.NORMAL)
                val align = when (node.align.lowercase()) {
                    "center" -> TextAlignOption.CENTER
                    "end", "right" -> TextAlignOption.END
                    else -> TextAlignOption.START
                }
                drawTextInRect(canvas, resolved, inner, color, (node.textSizeSp * scale).coerceAtLeast(10f), tf, align, node.maxLines)
            }
            is CustomXmlNode.Progress -> {
                val value = DynamicDataResolver.resolveTokens(node.progressExpression, telemetry, weather, nowMillis = nowMillis)
                    .replace("%", "").trim().toFloatOrNull() ?: 50f
                val track = parseColorInt(node.trackColorHex, Color.argb(60, 255, 255, 255))
                val indicator = parseColorInt(node.indicatorColorHex, Color.parseColor("#FF38BDF8"))
                drawProgressBar(canvas, RectF(inner.left, inner.centerY() - 3f * scale, inner.right, inner.centerY() + 3f * scale), (value / node.max).coerceIn(0f, 1f), track, indicator, scale)
            }
            is CustomXmlNode.Image -> {
                val file = node.source.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
                if (file != null) {
                    decodeDownsampledBitmap(file, inner.width().roundToInt(), inner.height().roundToInt())?.let { bmp ->
                        canvas.drawBitmap(bmp, null, inner, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        bmp.recycle()
                    }
                }
            }
            is CustomXmlNode.Icon -> {
                val color = applyAlpha(parseColorInt(node.tintColorHex, Color.parseColor("#FF5CE1E6")), node.alpha)
                drawTextInRect(canvas, vectorGlyph(node.symbolKey), inner, color, min(inner.width(), inner.height()) * 0.8f, Typeface.DEFAULT, TextAlignOption.CENTER, 1)
            }
            is CustomXmlNode.Divider -> {
                val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = applyAlpha(parseColorInt(node.colorHex, Color.argb(60, 255, 255, 255)), node.alpha)
                    strokeWidth = (node.thicknessDp * scale).coerceAtLeast(1f)
                }
                canvas.drawLine(inner.left, inner.centerY(), inner.right, inner.centerY(), divPaint)
            }
            is CustomXmlNode.Spacer -> Unit
        }
    }

    private fun drawAnalogClock(
        canvas: Canvas,
        rect: RectF,
        fgColor: Int,
        accentColor: Int,
        payload: LayerPayload.AnalogClock,
        scale: Float,
        nowMillis: Long
    ) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val radius = min(rect.width(), rect.height()) / 2f
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = applyAlpha(fgColor, 0.14f) }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (payload.dialBorderWidthDp * scale).coerceAtLeast(1.5f)
            color = applyAlpha(fgColor, 0.55f)
        }
        canvas.drawCircle(cx, cy, radius, fillPaint)
        canvas.drawCircle(cx, cy, radius, strokePaint)

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val minVal = cal.get(Calendar.MINUTE).toFloat()
        val hourVal = (cal.get(Calendar.HOUR) % 12) + minVal / 60f

        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            strokeWidth = (3.5f * scale).coerceAtLeast(2f)
            strokeCap = Paint.Cap.ROUND
        }
        val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            strokeWidth = (2.5f * scale).coerceAtLeast(1.5f)
            strokeCap = Paint.Cap.ROUND
        }

        val hAngle = (hourVal * 30.0 - 90.0) * PI / 180.0
        canvas.drawLine(cx, cy, cx + (radius * 0.5f * cos(hAngle)).toFloat(), cy + (radius * 0.5f * sin(hAngle)).toFloat(), hourPaint)

        val mAngle = (minVal * 6.0 - 90.0) * PI / 180.0
        canvas.drawLine(cx, cy, cx + (radius * 0.75f * cos(mAngle)).toFloat(), cy + (radius * 0.75f * sin(mAngle)).toFloat(), minPaint)
    }

    private fun drawProgressBar(
        canvas: Canvas,
        rect: RectF,
        fraction: Float,
        trackColor: Int,
        indicatorColor: Int,
        scale: Float
    ) {
        val r = rect.height() / 2f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = applyAlpha(trackColor, 0.25f) }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = indicatorColor }
        canvas.drawRoundRect(rect, r, r, trackPaint)
        val fillWidth = (rect.width() * fraction.coerceIn(0f, 1f)).coerceAtLeast(rect.height())
        val fillRect = RectF(rect.left, rect.top, (rect.left + fillWidth).coerceAtMost(rect.right), rect.bottom)
        canvas.drawRoundRect(fillRect, r, r, fillPaint)
    }

    private fun drawShapeContainer(
        canvas: Canvas,
        inner: RectF,
        payload: LayerPayload.ShapeContainer,
        fgColor: Int,
        accentColor: Int,
        radius: Float,
        scale: Float
    ) {
        val strokeWOutline = (payload.lineThicknessDp * scale).coerceAtLeast(3f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (payload.filled) {
                style = Paint.Style.FILL
                color = applyAlpha(fgColor, 0.28f)
            } else {
                style = Paint.Style.STROKE
                strokeWidth = strokeWOutline
                color = applyAlpha(fgColor, 0.9f)
            }
        }
        val minDim = min(inner.width(), inner.height())
        val cx = inner.centerX()
        val cy = inner.centerY()

        when (payload.shapeKind) {
            ShapeKind.SQUARE -> {
                val sqLeft = cx - minDim / 2f
                val sqTop = cy - minDim / 2f
                canvas.drawRoundRect(RectF(sqLeft, sqTop, sqLeft + minDim, sqTop + minDim), radius, radius, fillPaint)
            }
            ShapeKind.CIRCLE -> canvas.drawCircle(cx, cy, (minDim - if (payload.filled) 0f else strokeWOutline) / 2f, fillPaint)
            ShapeKind.OVAL -> canvas.drawOval(inner, fillPaint)
            ShapeKind.RING -> {
                val strokeW = (minDim * 0.14f).coerceAtLeast(4f)
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    color = applyAlpha(fgColor, 0.85f)
                }
                canvas.drawCircle(cx, cy, (minDim - strokeW) / 2f, ringPaint)
            }
            ShapeKind.CIRCLE_SLICE -> {
                val sliceLeft = cx - minDim / 2f
                val sliceTop = cy - minDim / 2f
                val sliceRect = RectF(sliceLeft, sliceTop, sliceLeft + minDim, sliceTop + minDim)
                if (payload.filled) {
                    canvas.drawArc(sliceRect, payload.startAngleDeg, payload.sweepAngleDeg, true, fillPaint)
                }
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = if (payload.filled) 1.6f * scale else strokeWOutline
                    color = if (payload.filled) applyAlpha(accentColor, 0.72f) else applyAlpha(fgColor, 0.9f)
                }
                canvas.drawArc(sliceRect, payload.startAngleDeg, payload.sweepAngleDeg, true, strokePaint)
            }
            ShapeKind.ARC -> {
                val strokeW = (payload.lineThicknessDp * scale).coerceAtLeast(4f)
                val arcDim = (minDim - strokeW).coerceAtLeast(4f)
                val arcLeft = cx - arcDim / 2f
                val arcTop = cy - arcDim / 2f
                val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    strokeCap = Paint.Cap.ROUND
                    color = applyAlpha(fgColor, 0.85f)
                }
                canvas.drawArc(RectF(arcLeft, arcTop, arcLeft + arcDim, arcTop + arcDim), payload.startAngleDeg, payload.sweepAngleDeg, false, arcPaint)
            }
            ShapeKind.PILL -> {
                val r = inner.height() / 2f
                canvas.drawRoundRect(inner, r, r, fillPaint)
            }
            ShapeKind.LINE -> {
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = fgColor
                    strokeWidth = (payload.lineThicknessDp * scale).coerceAtLeast(2f)
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawLine(inner.left, cy, inner.right, cy, linePaint)
            }
            ShapeKind.RECTANGLE -> canvas.drawRoundRect(inner, radius, radius, fillPaint)
            ShapeKind.SQUIRCLE -> {
                val r = minDim * 0.34f
                canvas.drawRoundRect(inner, r, r, fillPaint)
            }
            ShapeKind.GLASS_CARD -> {
                val r = radius.coerceAtLeast(12f * scale)
                if (payload.filled) {
                    val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            inner.left, inner.top, inner.right, inner.bottom,
                            Color.argb(72, 255, 255, 255),
                            Color.argb(20, 255, 255, 255),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRoundRect(inner, r, r, glassPaint)
                }
                val glassBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = if (payload.filled) 1.8f * scale else strokeWOutline
                    color = Color.argb(165, 255, 255, 255)
                }
                canvas.drawRoundRect(inner, r, r, glassBorder)
            }
            ShapeKind.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(cx, inner.top)
                    lineTo(inner.right, inner.bottom)
                    lineTo(inner.left, inner.bottom)
                    close()
                }
                canvas.drawPath(path, fillPaint)
            }
            ShapeKind.RIGHT_TRIANGLE -> {
                val path = Path().apply {
                    moveTo(inner.left, inner.top)
                    lineTo(inner.right, inner.bottom)
                    lineTo(inner.left, inner.bottom)
                    close()
                }
                canvas.drawPath(path, fillPaint)
            }
            ShapeKind.DIAMOND -> {
                val path = Path().apply {
                    moveTo(cx, inner.top)
                    lineTo(inner.right, cy)
                    lineTo(cx, inner.bottom)
                    lineTo(inner.left, cy)
                    close()
                }
                canvas.drawPath(path, fillPaint)
            }
            ShapeKind.HEXAGON -> {
                val path = Path()
                val rad = minDim * 0.48f
                for (i in 0 until 6) {
                    val a = (i * 60.0 - 90.0) * PI / 180.0
                    val px = cx + (rad * cos(a)).toFloat()
                    val py = cy + (rad * sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, fillPaint)
            }
            ShapeKind.STAR -> {
                val path = Path()
                val outerR = minDim * 0.48f
                val innerR = minDim * 0.21f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val a = (-90.0 + i * 36.0) * PI / 180.0
                    val px = cx + (r * cos(a)).toFloat()
                    val py = cy + (r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                if (payload.filled) {
                    val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = applyAlpha(accentColor, 0.35f) }
                    canvas.drawPath(path, starPaint)
                }
                val starStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = if (payload.filled) 1.6f * scale else strokeWOutline
                    color = applyAlpha(fgColor, 0.85f)
                }
                canvas.drawPath(path, starStroke)
            }
            ShapeKind.ARROW_BADGE -> {
                val tipX = inner.left + inner.width() * 0.72f
                val path = Path().apply {
                    moveTo(inner.left, inner.top)
                    lineTo(tipX, inner.top)
                    lineTo(inner.right, cy)
                    lineTo(tipX, inner.bottom)
                    lineTo(inner.left, inner.bottom)
                    lineTo(inner.left + inner.width() * 0.16f, cy)
                    close()
                }
                val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    if (payload.filled) {
                        color = applyAlpha(accentColor, 0.28f)
                    } else {
                        style = Paint.Style.STROKE
                        strokeWidth = strokeWOutline
                        color = applyAlpha(fgColor, 0.9f)
                    }
                }
                canvas.drawPath(path, arrowPaint)
            }
            ShapeKind.PATH -> {
                val path = CustomShapePathParser.toAndroidPath(
                    raw = payload.customPathData,
                    left = inner.left,
                    top = inner.top,
                    width = inner.width(),
                    height = inner.height()
                )
                if (payload.filled) {
                    canvas.drawPath(path, fillPaint)
                }
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = if (payload.filled) 1.6f * scale else strokeWOutline
                    color = if (payload.filled) applyAlpha(accentColor, 0.75f) else applyAlpha(fgColor, 0.9f)
                }
                canvas.drawPath(path, strokePaint)
            }
        }
    }

    private fun drawTextInRect(
        canvas: Canvas,
        text: String,
        rect: RectF,
        color: Int,
        textSizePx: Float,
        typeface: Typeface,
        align: TextAlignOption,
        maxLines: Int,
        lineHeightMultiplier: Float = 1.15f,
        letterSpacingSp: Float = 0f
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSizePx
            this.typeface = typeface
            if (letterSpacingSp != 0f) {
                this.letterSpacing = (letterSpacingSp / 16f).coerceIn(-0.25f, 0.6f)
            }
        }
        val layoutAlign = when (align) {
            TextAlignOption.START -> Layout.Alignment.ALIGN_NORMAL
            TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignOption.END -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val widthPx = rect.width().roundToInt().coerceAtLeast(8)
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(layoutAlign)
            .setLineSpacing(0f, lineHeightMultiplier.coerceIn(0.8f, 2.5f))
            .setMaxLines(maxLines.coerceAtLeast(1))
            .build()

        canvas.save()
        val dy = ((rect.height() - staticLayout.height) / 2f).coerceAtLeast(0f)
        canvas.translate(rect.left, rect.top + dy)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun decodeDownsampledBitmap(file: File, targetW: Int, targetH: Int): Bitmap? {
        val reqW = targetW.coerceIn(32, 600)
        val reqH = targetH.coerceIn(32, 600)
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
        var inSample = 1
        val halfW = boundsOpts.outWidth / 2
        val halfH = boundsOpts.outHeight / 2
        while (halfW / inSample >= reqW && halfH / inSample >= reqH) {
            inSample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = inSample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return runCatching { BitmapFactory.decodeFile(file.absolutePath, decodeOpts) }.getOrNull()
    }

    private fun createFillPaint(
        solidHex: String,
        gradient: GradientConfig,
        rect: RectF,
        fallbackColor: Int
    ): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (gradient.enabled) {
                val start = parseColorInt(gradient.startColorHex, fallbackColor)
                val end = parseColorInt(gradient.endColorHex, fallbackColor)
                shader = if (gradient.type == GradientType.RADIAL) {
                    val radius = (max(rect.width(), rect.height()) / 2f).coerceAtLeast(1f)
                    RadialGradient(rect.centerX(), rect.centerY(), radius, start, end, Shader.TileMode.CLAMP)
                } else {
                    val rad = gradient.angleDegrees * (PI / 180.0)
                    val endX = rect.left + (rect.width() * ((cos(rad) + 1.0) / 2.0)).toFloat()
                    val endY = rect.top + (rect.height() * ((sin(rad) + 1.0) / 2.0)).toFloat()
                    LinearGradient(rect.left, rect.top, endX, endY, start, end, Shader.TileMode.CLAMP)
                }
            } else {
                color = parseColorInt(solidHex, fallbackColor)
            }
        }
    }

    private fun resolveTypeface(fontName: String, fontAssetPath: String?, weight: FontWeightOption): Typeface {
        if (!fontAssetPath.isNullOrBlank()) {
            val file = File(fontAssetPath)
            if (file.exists() && file.isFile) {
                runCatching { return Typeface.createFromFile(file) }
            }
        }
        val style = if (weight == FontWeightOption.BOLD || weight == FontWeightOption.SEMIBOLD) {
            Typeface.BOLD
        } else {
            Typeface.NORMAL
        }
        val family = when (fontName.lowercase()) {
            "jetbrains mono", "monospace" -> Typeface.MONOSPACE
            "serif" -> Typeface.SERIF
            else -> Typeface.SANS_SERIF
        }
        return Typeface.create(family, style)
    }

    private fun parseColorInt(hex: String, fallback: Int): Int {
        return runCatching { Color.parseColor(hex.trim()) }.getOrDefault(fallback)
    }

    private fun applyAlpha(color: Int, opacity: Float): Int {
        val alpha = (Color.alpha(color) * opacity.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
