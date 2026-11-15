package com.example.widget

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.example.MainActivity
import com.example.WBuilderAppContainer
import com.example.data.repository.StudioActionsRepository
import com.example.data.repository.SystemTelemetryRepository
import com.example.domain.model.LayerPayload
import com.example.domain.model.StudioActionMode
import com.example.domain.model.TapAction
import com.example.domain.model.VolumeStreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object WidgetActionKeys {
    val PACKAGE_NAME = ActionParameters.Key<String>("wbuilder_package_name")
    val CLASS_NAME = ActionParameters.Key<String>("wbuilder_class_name")
    val URL = ActionParameters.Key<String>("wbuilder_url")
    val SHORTCUT_URI = ActionParameters.Key<String>("wbuilder_shortcut_uri")
    val MEDIA_KEY_CODE = ActionParameters.Key<Int>("wbuilder_media_key_code")
    val VOLUME_DIRECTION = ActionParameters.Key<Int>("wbuilder_volume_direction")
    val VOLUME_STREAM = ActionParameters.Key<Int>("wbuilder_volume_stream")
    val VOLUME_DELTA = ActionParameters.Key<Int>("wbuilder_volume_delta")
    val VOLUME_SHOW_UI = ActionParameters.Key<Boolean>("wbuilder_volume_show_ui")
    val SYSTEM_TARGET = ActionParameters.Key<String>("wbuilder_system_target")
    val STUDIO_MODE = ActionParameters.Key<String>("wbuilder_studio_mode")
    val SWITCH_NAME = ActionParameters.Key<String>("wbuilder_switch_name")
    val NOTIFICATION_INDEX = ActionParameters.Key<Int>("wbuilder_notification_index")
    val FLOW_JSON = ActionParameters.Key<String>("wbuilder_flow_json")
}

private const val TAG = "WidgetActionCallback"
private val actionFlowJson = Json { ignoreUnknownKeys = true }

class LaunchAppActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val pkg = parameters[WidgetActionKeys.PACKAGE_NAME] ?: return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntent != null) {
            runCatching { context.startActivity(launchIntent) }
                .onFailure { Log.e(TAG, "Failed to launch app $pkg", it) }
        }
    }
}

class LaunchShortcutActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val uri = parameters[WidgetActionKeys.SHORTCUT_URI] ?: return
        StudioActionsRepository(context).launchShortcutUri(uri)
    }
}

class LaunchActivityActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val pkg = parameters[WidgetActionKeys.PACKAGE_NAME] ?: return
        val cls = parameters[WidgetActionKeys.CLASS_NAME] ?: return
        StudioActionsRepository(context).launchExportedActivity(pkg, cls)
    }
}

class OpenUrlActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val rawUrl = parameters[WidgetActionKeys.URL]?.trim().orEmpty()
        if (rawUrl.isEmpty()) return
        val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Log.e(TAG, "Failed to open URL $normalized", it) }
    }
}

class ToggleFlashlightActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.Main) {
            runCatching {
                SystemTelemetryRepository(context).toggleFlashlight()
            }.onFailure {
                Log.e(TAG, "Unable to toggle flashlight", it)
            }
        }
        WBuilderGlanceWidget().update(context, glanceId)
    }
}

class ToggleMuteActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.Main) {
            runCatching {
                SystemTelemetryRepository(context).toggleMediaMute()
            }.onFailure {
                Log.e(TAG, "Unable to toggle mute", it)
            }
        }
        delay(120L)
        WBuilderGlanceWidget().update(context, glanceId)
    }
}

class ToggleSilentVibrateActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.Main) {
            runCatching {
                SystemTelemetryRepository(context).toggleSilentVibrate()
            }.onFailure {
                Log.e(TAG, "Unable to toggle silent/vibrate", it)
            }
        }
        delay(120L)
        WBuilderGlanceWidget().update(context, glanceId)
    }
}

class ToggleGlobalSwitchActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val name = parameters[WidgetActionKeys.SWITCH_NAME] ?: "global_1"
        StudioActionsRepository(context).toggleGlobalSwitch(name)
        WBuilderGlanceWidget().update(context, glanceId)
    }
}

class OpenSystemActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val target = parameters[WidgetActionKeys.SYSTEM_TARGET] ?: "SETTINGS"
        SystemTelemetryRepository(context).openSystemAction(target)
    }
}

class MediaControlActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val keyCode = parameters[WidgetActionKeys.MEDIA_KEY_CODE] ?: KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        withContext(Dispatchers.Main) {
            runCatching {
                SystemTelemetryRepository(context).dispatchMediaKey(keyCode)
            }.onFailure {
                Log.e(TAG, "Failed to dispatch media key $keyCode", it)
            }
        }
        delay(200L)
        WBuilderGlanceWidget().update(context, glanceId)
    }
}

class AdjustVolumeActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val direction = parameters[WidgetActionKeys.VOLUME_DIRECTION] ?: AudioManager.ADJUST_RAISE
        withContext(Dispatchers.Main) {
            runCatching {
                SystemTelemetryRepository(context).adjustMediaVolume(direction)
            }.onFailure {
                Log.e(TAG, "Failed to adjust volume", it)
            }
        }
        delay(120L)
        WBuilderGlanceWidget().update(context, glanceId)
    }
}

class ChangeVolumeStreamActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val stream = parameters[WidgetActionKeys.VOLUME_STREAM] ?: AudioManager.STREAM_MUSIC
        val delta = parameters[WidgetActionKeys.VOLUME_DELTA] ?: 1
        val showUi = parameters[WidgetActionKeys.VOLUME_SHOW_UI] ?: true
        withContext(Dispatchers.Main) {
            StudioActionsRepository(context).adjustStreamVolume(stream, delta, showUi)
        }
        delay(120L)
        WBuilderGlanceWidget().update(context, glanceId)
    }
}

class StudioModeActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val modeName = parameters[WidgetActionKeys.STUDIO_MODE] ?: StudioActionMode.FORCE_WEATHER_UPDATE.name
        val mode = runCatching { StudioActionMode.valueOf(modeName) }.getOrDefault(StudioActionMode.FORCE_WEATHER_UPDATE)
        val url = parameters[WidgetActionKeys.URL].orEmpty()
        val notifIndex = parameters[WidgetActionKeys.NOTIFICATION_INDEX] ?: 0
        executeStudioModeInWidget(context, glanceId, mode, url, notifIndex)
    }
}

class TriggerFlowActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val raw = parameters[WidgetActionKeys.FLOW_JSON].orEmpty()
        if (raw.isBlank()) return
        val steps = runCatching {
            actionFlowJson.decodeFromString(ListSerializer(TapAction.serializer()), raw)
        }.getOrDefault(emptyList())
        steps.forEach { step ->
            executeStepInWidget(context, glanceId, step)
            delay(80L)
        }
        WBuilderGlanceWidget().update(context, glanceId)
    }
}

class NoOpBlockTouchActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Intentionally absorbs touch events so underlying or root tap actions do not trigger.
    }
}

class RefreshWidgetActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WBuilderGlanceWidget().update(context, glanceId)
    }
}

class OpenStudioActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
}

private suspend fun executeStudioModeInWidget(
    context: Context,
    glanceId: GlanceId,
    mode: StudioActionMode,
    targetUrl: String,
    notificationIndex: Int
) {
    val container = WBuilderAppContainer(context)
    val studioRepo = container.studioActionsRepository
    when (mode) {
        StudioActionMode.FORCE_WEATHER_UPDATE -> {
            container.weatherRepository.clearCache()
            val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(glanceId) }.getOrDefault(-1)
            val projectId = if (appWidgetId != -1) container.widgetRepository.getBoundProjectId(appWidgetId) else null
            val project = projectId?.let { container.widgetRepository.getProjectOnce(it) }
            val weatherInfo = project?.layers?.firstNotNullOfOrNull { layer ->
                when (val p = layer.payload) {
                    is LayerPayload.WeatherCard -> Triple(p.cityName, p.latitude to p.longitude, p.useDeviceLocation)
                    is LayerPayload.WeatherLocation -> Triple(p.cityName, p.latitude to p.longitude, p.useDeviceLocation)
                    is LayerPayload.WeatherForecast -> Triple(p.cityName, p.latitude to p.longitude, p.useDeviceLocation)
                    else -> null
                }
            }
            container.weatherRepository.fetchWeather(
                fallbackCity = weatherInfo?.first ?: "San Francisco",
                fallbackLat = weatherInfo?.second?.first ?: 37.7749,
                fallbackLon = weatherInfo?.second?.second ?: -122.4194,
                useDeviceLocation = weatherInfo?.third ?: true
            )
            WBuilderGlanceWidget().update(context, glanceId)
        }
        StudioActionMode.FORCE_RSS_XML_UPDATE -> {
            studioRepo.forceUpdateRssOrTextXml(targetUrl)
            WBuilderGlanceWidget().update(context, glanceId)
        }
        StudioActionMode.FORCE_IMAGE_UPDATE -> {
            val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(glanceId) }.getOrDefault(-1)
            val projectId = if (appWidgetId != -1) container.widgetRepository.getBoundProjectId(appWidgetId) else null
            val project = projectId?.let { container.widgetRepository.getProjectOnce(it) }
            val layerUrls = project?.layers
                ?.mapNotNull { (it.payload as? LayerPayload.ImageMedia)?.assetPathOrUri }
                .orEmpty()
            val urls = (listOf(targetUrl) + layerUrls).filter { it.isNotBlank() }
            studioRepo.forceUpdateDownloadedImages(urls)
            WBuilderGlanceWidget().update(context, glanceId)
        }
        StudioActionMode.OPEN_NOTIFICATION -> {
            studioRepo.openNotificationOrSettings(notificationIndex)
        }
        StudioActionMode.DISMISS_NOTIFICATION -> {
            studioRepo.dismissNotification(notificationIndex)
            WBuilderGlanceWidget().update(context, glanceId)
        }
        StudioActionMode.DISMISS_ALL_NOTIFICATIONS -> {
            studioRepo.dismissAllNotifications()
            WBuilderGlanceWidget().update(context, glanceId)
        }
    }
}

private suspend fun executeStepInWidget(
    context: Context,
    glanceId: GlanceId,
    step: TapAction
) {
    val telemetryRepo = SystemTelemetryRepository(context)
    val studioRepo = StudioActionsRepository(context)
    when (step) {
        is TapAction.None, is TapAction.DisabledBlockTouch -> Unit
        is TapAction.LaunchApp -> {
            context.packageManager.getLaunchIntentForPackage(step.packageName)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(it) }
            }
        }
        is TapAction.LaunchShortcut -> studioRepo.launchShortcutUri(step.shortcutUri)
        is TapAction.LaunchActivity -> studioRepo.launchExportedActivity(step.packageName, step.className)
        is TapAction.ToggleFlashlight -> withContext(Dispatchers.Main) { telemetryRepo.toggleFlashlight() }
        is TapAction.ToggleSilentVibrate -> withContext(Dispatchers.Main) { telemetryRepo.toggleSilentVibrate() }
        is TapAction.ToggleMute -> withContext(Dispatchers.Main) { telemetryRepo.toggleMediaMute() }
        is TapAction.ToggleGlobalSwitch -> studioRepo.toggleGlobalSwitch(step.switchName)
        is TapAction.MediaPlayPause -> withContext(Dispatchers.Main) { telemetryRepo.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
        is TapAction.MediaNext -> withContext(Dispatchers.Main) { telemetryRepo.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) }
        is TapAction.MediaPrevious -> withContext(Dispatchers.Main) { telemetryRepo.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
        is TapAction.VolumeUp -> withContext(Dispatchers.Main) { telemetryRepo.adjustMediaVolume(AudioManager.ADJUST_RAISE) }
        is TapAction.VolumeDown -> withContext(Dispatchers.Main) { telemetryRepo.adjustMediaVolume(AudioManager.ADJUST_LOWER) }
        is TapAction.ChangeVolume -> withContext(Dispatchers.Main) {
            studioRepo.adjustStreamVolume(step.stream.androidStream, step.deltaSteps, step.showUi)
        }
        is TapAction.OpenWifiSettings -> telemetryRepo.openSystemAction("WIFI")
        is TapAction.OpenBluetoothSettings -> telemetryRepo.openSystemAction("BLUETOOTH")
        is TapAction.OpenSoundSettings -> telemetryRepo.openSystemAction("SOUND")
        is TapAction.OpenDisplaySettings -> telemetryRepo.openSystemAction("DISPLAY")
        is TapAction.OpenAlarmClock -> telemetryRepo.openSystemAction("ALARM")
        is TapAction.OpenCamera -> telemetryRepo.openSystemAction("CAMERA")
        is TapAction.OpenUrl -> {
            val raw = step.url.trim()
            if (raw.isNotEmpty()) {
                val url = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        is TapAction.StudioAction -> executeStudioModeInWidget(context, glanceId, step.mode, step.targetUrl, step.notificationIndex)
        is TapAction.TriggerFlow -> step.steps.forEach { nested ->
            if (nested !is TapAction.TriggerFlow) executeStepInWidget(context, glanceId, nested)
        }
        is TapAction.RefreshWidget -> WBuilderGlanceWidget().update(context, glanceId)
        is TapAction.OpenStudio -> {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }
}
