package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.WBuilderAppContainer
import com.example.domain.model.LayerPayload
import com.example.domain.model.StackOrientation
import com.example.domain.model.TapAction
import com.example.domain.model.ToggleButtonKind
import com.example.domain.model.WidgetLayer
import com.example.domain.model.WidgetProject
import com.example.domain.model.computeVisualBounds
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val widgetFlowJson = Json { ignoreUnknownKeys = true }

class WBuilderGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = WBuilderAppContainer(context)
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(-1)

        val boundProjectId = if (appWidgetId != -1) {
            container.widgetRepository.getBoundProjectId(appWidgetId)
        } else null

        val project: WidgetProject? = when {
            boundProjectId != null -> container.widgetRepository.getProjectOnce(boundProjectId)
            else -> {
                val prefProjectId = container.preferencesDataStore.preferences.first().lastActiveProjectId
                val fallback = prefProjectId?.let { container.widgetRepository.getProjectOnce(it) }
                    ?: container.widgetRepository.getAllProjectsOnce().firstOrNull()
                if (fallback != null && appWidgetId != -1) {
                    container.widgetRepository.bindAppWidget(appWidgetId, fallback.id)
                }
                fallback
            }
        }

        val telemetry = container.telemetryRepository.collectSnapshot()
        val weatherLayerInfo = project?.layers
            ?.firstNotNullOfOrNull { layer ->
                when (val p = layer.payload) {
                    is LayerPayload.WeatherCard -> Triple(p.cityName, p.latitude to p.longitude, p.useDeviceLocation)
                    is LayerPayload.WeatherLocation -> Triple(p.cityName, p.latitude to p.longitude, p.useDeviceLocation)
                    is LayerPayload.WeatherForecast -> Triple(p.cityName, p.latitude to p.longitude, p.useDeviceLocation)
                    is LayerPayload.LocationBadge -> Triple(p.fallbackCity, 37.7749 to -122.4194, true)
                    else -> null
                }
            }
        val weather = if (weatherLayerInfo != null) {
            container.weatherRepository.fetchWeather(
                fallbackCity = weatherLayerInfo.first,
                fallbackLat = weatherLayerInfo.second.first,
                fallbackLon = weatherLayerInfo.second.second,
                useDeviceLocation = weatherLayerInfo.third
            ).getOrNull() ?: container.weatherRepository.getLastCachedOrNull()
        } else {
            container.weatherRepository.getLastCachedOrNull()
        }

        provideContent {
            val size = LocalSize.current
            if (project == null) {
                EmptyGlanceWidgetCard()
            } else {
                val renderedBitmap = WidgetBitmapRenderer.renderProjectToBitmap(
                    context = context,
                    project = project,
                    targetWidthDp = size.width.value.coerceAtLeast(project.resolvedWidthDp),
                    targetHeightDp = size.height.value.coerceAtLeast(project.resolvedHeightDp),
                    telemetry = telemetry,
                    weather = weather
                )
                RenderedGlanceWidget(
                    project = project,
                    bitmap = renderedBitmap,
                    targetWidthDp = size.width.value,
                    targetHeightDp = size.height.value
                )
            }
        }
    }

    @Composable
    private fun RenderedGlanceWidget(
        project: WidgetProject,
        bitmap: Bitmap,
        targetWidthDp: Float,
        targetHeightDp: Float
    ) {
        val primaryTapAction = project.layers
            .map { it.tapAction }
            .firstOrNull { it !is TapAction.None && it !is TapAction.DisabledBlockTouch }
            ?: TapAction.OpenStudio

        val rootClickModifier = buildClickModifier(primaryTapAction)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .then(rootClickModifier),
            contentAlignment = Alignment.TopStart
        ) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = project.name,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize()
            )

            // Interactive hitboxes for layers with explicit TapActions AND built-in interactive controls
            val scaleX = (targetWidthDp / project.resolvedWidthDp.coerceAtLeast(1f)).coerceAtLeast(0.3f)
            val scaleY = (targetHeightDp / project.resolvedHeightDp.coerceAtLeast(1f)).coerceAtLeast(0.3f)

            project.layers
                .filter { it.isVisible }
                .forEach { layer ->
                    RenderLayerHitboxes(
                        layer = layer,
                        offsetX = 0f,
                        offsetY = 0f,
                        scaleX = scaleX,
                        scaleY = scaleY
                    )
                }
        }
    }

    @Composable
    private fun RenderLayerHitboxes(
        layer: WidgetLayer,
        offsetX: Float,
        offsetY: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        val vis = layer.style.computeVisualBounds()
        val absLeft = offsetX + vis.leftDp
        val absTop = offsetY + vis.topDp
        val padLeft = (absLeft * scaleX).coerceAtLeast(0f).dp
        val padTop = (absTop * scaleY).coerceAtLeast(0f).dp
        val hitW = (vis.widthDp * scaleX).coerceAtLeast(36f).dp
        val hitH = (vis.heightDp * scaleY).coerceAtLeast(36f).dp

        when (val payload = layer.payload) {
            is LayerPayload.OverlapGroup -> {
                if (layer.tapAction !is TapAction.None) {
                    HitboxZone(padLeft, padTop, hitW, hitH, buildClickModifier(layer.tapAction))
                }
                val innerPad = layer.style.paddingDp
                payload.embeddedLayers.filter { it.isVisible }.forEach { child ->
                    RenderLayerHitboxes(
                        layer = child,
                        offsetX = absLeft + innerPad,
                        offsetY = absTop + innerPad,
                        scaleX = scaleX,
                        scaleY = scaleY
                    )
                }
            }
            is LayerPayload.StackGroup -> {
                if (layer.tapAction !is TapAction.None) {
                    HitboxZone(padLeft, padTop, hitW, hitH, buildClickModifier(layer.tapAction))
                }
                val innerPad = layer.style.paddingDp
                var cursorX = 0f
                var cursorY = 0f
                payload.embeddedLayers.filter { it.isVisible }.sortedBy { it.style.zIndex }.forEach { child ->
                    val positioned = child.copy(style = child.style.copy(xDp = cursorX, yDp = cursorY))
                    RenderLayerHitboxes(
                        layer = positioned,
                        offsetX = absLeft + innerPad,
                        offsetY = absTop + innerPad,
                        scaleX = scaleX,
                        scaleY = scaleY
                    )
                    if (payload.orientation == StackOrientation.HORIZONTAL) {
                        cursorX += child.style.widthDp + payload.spacingDp
                    } else {
                        cursorY += child.style.heightDp + payload.spacingDp
                    }
                }
            }
            is LayerPayload.MusicPlayer -> {
                if (payload.showPlaybackControls) {
                    val btnW = ((hitW.value * 0.16f).coerceAtLeast(28f)).dp
                    val prevStart = padLeft + ((hitW.value * 0.50f).coerceAtLeast(0f)).dp
                    val playStart = prevStart + btnW
                    val nextStart = playStart + btnW
                    HitboxZone(prevStart, padTop, btnW, hitH, MediaControlHitbox(KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                    HitboxZone(playStart, padTop, btnW, hitH, MediaControlHitbox(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    HitboxZone(nextStart, padTop, btnW, hitH, MediaControlHitbox(KeyEvent.KEYCODE_MEDIA_NEXT))
                } else if (layer.tapAction !is TapAction.None) {
                    HitboxZone(padLeft, padTop, hitW, hitH, buildClickModifier(layer.tapAction))
                }
            }
            is LayerPayload.QuickToggleBar -> {
                val slots = listOfNotNull(
                    if (payload.showFlashlight) buildClickModifier(TapAction.ToggleFlashlight) else null,
                    if (payload.showMuteToggle) buildClickModifier(TapAction.ToggleMute) else null,
                    if (payload.showPlayPause) buildClickModifier(TapAction.MediaPlayPause) else null,
                    if (payload.showWifiSettings) buildClickModifier(TapAction.OpenWifiSettings) else null,
                    if (payload.showBluetoothSettings) buildClickModifier(TapAction.OpenBluetoothSettings) else null,
                    if (payload.showVolumeUp) buildClickModifier(TapAction.VolumeUp) else null,
                    if (payload.showVolumeDown) buildClickModifier(TapAction.VolumeDown) else null,
                    if (payload.showRefresh) buildClickModifier(TapAction.RefreshWidget) else null,
                    if (payload.showStudioShortcut) buildClickModifier(TapAction.OpenStudio) else null
                )
                if (slots.isNotEmpty()) {
                    val slotW = (hitW.value / slots.size).coerceAtLeast(24f).dp
                    slots.forEachIndexed { idx, mod ->
                        HitboxZone(padLeft + (slotW.value * idx).dp, padTop, slotW, hitH, mod)
                    }
                }
            }
            is LayerPayload.AudioVolumeBar -> {
                val halfW = (hitW.value / 2f).coerceAtLeast(28f).dp
                HitboxZone(
                    padLeft,
                    padTop,
                    halfW,
                    hitH,
                    GlanceModifier.clickable(
                        actionRunCallback<AdjustVolumeActionCallback>(
                            actionParametersOf(WidgetActionKeys.VOLUME_DIRECTION to AudioManager.ADJUST_LOWER)
                        )
                    )
                )
                HitboxZone(
                    padLeft + halfW,
                    padTop,
                    halfW,
                    hitH,
                    GlanceModifier.clickable(
                        actionRunCallback<AdjustVolumeActionCallback>(
                            actionParametersOf(WidgetActionKeys.VOLUME_DIRECTION to AudioManager.ADJUST_RAISE)
                        )
                    )
                )
            }
            is LayerPayload.ToggleButton -> {
                val resolvedAction = if (layer.tapAction !is TapAction.None) {
                    layer.tapAction
                } else {
                    when (payload.kind) {
                        ToggleButtonKind.FLASHLIGHT -> TapAction.ToggleFlashlight
                        ToggleButtonKind.WIFI -> TapAction.OpenWifiSettings
                        ToggleButtonKind.BLUETOOTH -> TapAction.OpenBluetoothSettings
                        ToggleButtonKind.SILENT_VIBRATE -> TapAction.ToggleSilentVibrate
                        ToggleButtonKind.MUTE -> TapAction.ToggleMute
                        ToggleButtonKind.PLAY_PAUSE -> TapAction.MediaPlayPause
                        ToggleButtonKind.REFRESH -> TapAction.RefreshWidget
                    }
                }
                HitboxZone(padLeft, padTop, hitW, hitH, buildClickModifier(resolvedAction))
            }
            else -> {
                if (layer.tapAction !is TapAction.None) {
                    HitboxZone(padLeft, padTop, hitW, hitH, buildClickModifier(layer.tapAction))
                }
            }
        }
    }

    @Composable
    private fun HitboxZone(
        padLeft: androidx.compose.ui.unit.Dp,
        padTop: androidx.compose.ui.unit.Dp,
        width: androidx.compose.ui.unit.Dp,
        height: androidx.compose.ui.unit.Dp,
        clickModifier: GlanceModifier
    ) {
        Box(modifier = GlanceModifier.padding(start = padLeft, top = padTop)) {
            Box(modifier = GlanceModifier.size(width = width, height = height).then(clickModifier)) {}
        }
    }

    private fun MediaControlHitbox(keyCode: Int): GlanceModifier = GlanceModifier.clickable(
        actionRunCallback<MediaControlActionCallback>(
            actionParametersOf(WidgetActionKeys.MEDIA_KEY_CODE to keyCode)
        )
    )

    @Composable
    private fun EmptyGlanceWidgetCard() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF141829))
                .padding(16.dp)
                .clickable(actionRunCallback<OpenStudioActionCallback>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "W builder",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF5CE1E6)),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = "Tap to design & apply a widget",
                style = TextStyle(color = ColorProvider(Color.White))
            )
        }
    }

    private fun buildClickModifier(action: TapAction): GlanceModifier = when (action) {
        is TapAction.None -> GlanceModifier
        is TapAction.LaunchApp -> GlanceModifier.clickable(
            actionRunCallback<LaunchAppActionCallback>(
                actionParametersOf(WidgetActionKeys.PACKAGE_NAME to action.packageName)
            )
        )
        is TapAction.OpenUrl -> GlanceModifier.clickable(
            actionRunCallback<OpenUrlActionCallback>(
                actionParametersOf(WidgetActionKeys.URL to action.url)
            )
        )
        is TapAction.ToggleFlashlight -> GlanceModifier.clickable(
            actionRunCallback<ToggleFlashlightActionCallback>()
        )
        is TapAction.ToggleSilentVibrate -> GlanceModifier.clickable(
            actionRunCallback<ToggleSilentVibrateActionCallback>()
        )
        is TapAction.ToggleMute -> GlanceModifier.clickable(
            actionRunCallback<ToggleMuteActionCallback>()
        )
        is TapAction.MediaPlayPause -> MediaControlHitbox(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        is TapAction.MediaNext -> MediaControlHitbox(KeyEvent.KEYCODE_MEDIA_NEXT)
        is TapAction.MediaPrevious -> MediaControlHitbox(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        is TapAction.VolumeUp -> GlanceModifier.clickable(
            actionRunCallback<AdjustVolumeActionCallback>(
                actionParametersOf(WidgetActionKeys.VOLUME_DIRECTION to AudioManager.ADJUST_RAISE)
            )
        )
        is TapAction.VolumeDown -> GlanceModifier.clickable(
            actionRunCallback<AdjustVolumeActionCallback>(
                actionParametersOf(WidgetActionKeys.VOLUME_DIRECTION to AudioManager.ADJUST_LOWER)
            )
        )
        is TapAction.OpenWifiSettings -> SystemTargetHitbox("WIFI")
        is TapAction.OpenBluetoothSettings -> SystemTargetHitbox("BLUETOOTH")
        is TapAction.OpenSoundSettings -> SystemTargetHitbox("SOUND")
        is TapAction.OpenDisplaySettings -> SystemTargetHitbox("DISPLAY")
        is TapAction.OpenAlarmClock -> SystemTargetHitbox("ALARM")
        is TapAction.OpenCamera -> SystemTargetHitbox("CAMERA")
        is TapAction.RefreshWidget -> GlanceModifier.clickable(
            actionRunCallback<RefreshWidgetActionCallback>()
        )
        is TapAction.OpenStudio -> GlanceModifier.clickable(
            actionRunCallback<OpenStudioActionCallback>()
        )
        is TapAction.StudioAction -> GlanceModifier.clickable(
            actionRunCallback<StudioModeActionCallback>(
                actionParametersOf(
                    WidgetActionKeys.STUDIO_MODE to action.mode.name,
                    WidgetActionKeys.URL to action.targetUrl,
                    WidgetActionKeys.NOTIFICATION_INDEX to action.notificationIndex
                )
            )
        )
        is TapAction.LaunchShortcut -> GlanceModifier.clickable(
            actionRunCallback<LaunchShortcutActionCallback>(
                actionParametersOf(WidgetActionKeys.SHORTCUT_URI to action.shortcutUri)
            )
        )
        is TapAction.LaunchActivity -> GlanceModifier.clickable(
            actionRunCallback<LaunchActivityActionCallback>(
                actionParametersOf(
                    WidgetActionKeys.PACKAGE_NAME to action.packageName,
                    WidgetActionKeys.CLASS_NAME to action.className
                )
            )
        )
        is TapAction.ToggleGlobalSwitch -> GlanceModifier.clickable(
            actionRunCallback<ToggleGlobalSwitchActionCallback>(
                actionParametersOf(WidgetActionKeys.SWITCH_NAME to action.switchName)
            )
        )
        is TapAction.TriggerFlow -> {
            val encoded = widgetFlowJson.encodeToString(ListSerializer(TapAction.serializer()), action.steps)
            GlanceModifier.clickable(
                actionRunCallback<TriggerFlowActionCallback>(
                    actionParametersOf(WidgetActionKeys.FLOW_JSON to encoded)
                )
            )
        }
        is TapAction.ChangeVolume -> GlanceModifier.clickable(
            actionRunCallback<ChangeVolumeStreamActionCallback>(
                actionParametersOf(
                    WidgetActionKeys.VOLUME_STREAM to action.stream.androidStream,
                    WidgetActionKeys.VOLUME_DELTA to action.deltaSteps,
                    WidgetActionKeys.VOLUME_SHOW_UI to action.showUi
                )
            )
        )
        is TapAction.DisabledBlockTouch -> GlanceModifier.clickable(
            actionRunCallback<NoOpBlockTouchActionCallback>()
        )
    }

    private fun SystemTargetHitbox(target: String): GlanceModifier = GlanceModifier.clickable(
        actionRunCallback<OpenSystemActionCallback>(
            actionParametersOf(WidgetActionKeys.SYSTEM_TARGET to target)
        )
    )
}
