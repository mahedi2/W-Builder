package com.example.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WidgetDimension(
    val label: String,
    val columns: Int,
    val rows: Int,
    val defaultWidthDp: Float,
    val defaultHeightDp: Float
) {
    SIZE_2X2("2×2", 2, 2, 160f, 160f),
    SIZE_4X1("4×1", 4, 1, 320f, 80f),
    SIZE_4X2("4×2", 4, 2, 320f, 160f),
    SIZE_4X4("4×4", 4, 4, 320f, 320f),
    CUSTOM("Custom", 4, 2, 280f, 180f)
}

@Serializable
enum class LayerType(val displayName: String, val category: LayerCategory) {
    DIGITAL_CLOCK("Digital Clock", LayerCategory.TIME_AND_TEXT),
    ANALOG_CLOCK("Analog Clock", LayerCategory.TIME_AND_TEXT),
    WORLD_CLOCK("World Clock", LayerCategory.TIME_AND_TEXT),
    DATE_TEXT("Date", LayerCategory.TIME_AND_TEXT),
    DAY_OF_WEEK("Day of Week", LayerCategory.TIME_AND_TEXT),
    GREETING_BANNER("Smart Greeting", LayerCategory.TIME_AND_TEXT),
    QUICK_NOTE("Sticky Note", LayerCategory.TIME_AND_TEXT),
    CUSTOM_TEXT("Custom Text", LayerCategory.TIME_AND_TEXT),

    BATTERY_INFO("Battery", LayerCategory.SYSTEM),
    SYSTEM_RAM_STORAGE("RAM & Storage", LayerCategory.SYSTEM),
    PROGRESS_RING("Circular Gauge", LayerCategory.SYSTEM),
    PROGRESS_BAR("Progress Bar", LayerCategory.SYSTEM),
    AUDIO_VOLUME_BAR("Volume & Sound", LayerCategory.SYSTEM),
    NETWORK_STATUS("Wi-Fi & Network", LayerCategory.SYSTEM),
    NEXT_ALARM("Next Alarm", LayerCategory.SYSTEM),

    WEATHER_CARD("Weather", LayerCategory.WEATHER_AND_CALENDAR),
    WEATHER_LOCATION("Location Weather", LayerCategory.WEATHER_AND_CALENDAR),
    WEATHER_FORECAST("3-Day Forecast", LayerCategory.WEATHER_AND_CALENDAR),
    LOCATION_BADGE("Location Pin", LayerCategory.WEATHER_AND_CALENDAR),
    CALENDAR_MINI("Mini Calendar", LayerCategory.WEATHER_AND_CALENDAR),
    CALENDAR_AGENDA("Agenda Events", LayerCategory.WEATHER_AND_CALENDAR),
    COUNTDOWN_TIMER("Countdown", LayerCategory.WEATHER_AND_CALENDAR),

    SHAPE_CONTAINER("Shape", LayerCategory.SHAPES_AND_MEDIA),
    GLASS_CARD("Glassmorphism Card", LayerCategory.SHAPES_AND_MEDIA),
    OVERLAP_GROUP("Overlap Group", LayerCategory.SHAPES_AND_MEDIA),
    STACK_GROUP("Stack Group", LayerCategory.SHAPES_AND_MEDIA),
    VECTOR_ICON("Vector Icon", LayerCategory.SHAPES_AND_MEDIA),
    IMAGE_MEDIA("Image / AI Art", LayerCategory.SHAPES_AND_MEDIA),
    MUSIC_PLAYER("Music Controls", LayerCategory.SHAPES_AND_MEDIA),
    QUICK_TOGGLE_BAR("Multi-Toggle Bar", LayerCategory.SHAPES_AND_MEDIA),

    TOGGLE_FLASHLIGHT("Flashlight Button", LayerCategory.TOGGLES),
    TOGGLE_WIFI("Wi-Fi Button", LayerCategory.TOGGLES),
    TOGGLE_BLUETOOTH("Bluetooth Button", LayerCategory.TOGGLES),
    TOGGLE_SILENT_VIBRATE("Silent / Vibrate", LayerCategory.TOGGLES),
    TOGGLE_MUTE("Mute Button", LayerCategory.TOGGLES),
    TOGGLE_PLAY_PAUSE("Play / Pause", LayerCategory.TOGGLES),
    TOGGLE_REFRESH("Refresh Button", LayerCategory.TOGGLES),

    CUSTOM_COMPONENT_REF("Custom Component", LayerCategory.CUSTOM),
    CUSTOM_XML_SNIPPET("Custom XML / JSON", LayerCategory.CUSTOM)
}

@Serializable
enum class LayerCategory(val title: String) {
    TIME_AND_TEXT("Time & Text"),
    SYSTEM("System Info"),
    WEATHER_AND_CALENDAR("Weather & Calendar"),
    SHAPES_AND_MEDIA("Shapes, Groups & Media"),
    TOGGLES("Individual Toggles"),
    CUSTOM("Custom Modules")
}

@Serializable
enum class ShapeKind(val label: String) {
    SQUARE("Square"),
    CIRCLE("Circle"),
    RECTANGLE("Rectangle"),
    OVAL("Oval"),
    PILL("Pill"),
    LINE("Line"),
    GLASS_CARD("Glass Card"),
    TRIANGLE("Triangle"),
    RIGHT_TRIANGLE("Right Triangle"),
    HEXAGON("Hexagon"),
    CIRCLE_SLICE("Circle Slice"),
    ARC("Arc"),
    SQUIRCLE("Squircle"),
    DIAMOND("Diamond"),
    STAR("Star"),
    RING("Ring"),
    ARROW_BADGE("Chevron"),
    PATH("Path")
}

@Serializable
enum class FontWeightOption {
    LIGHT,
    NORMAL,
    MEDIUM,
    SEMIBOLD,
    BOLD
}

@Serializable
enum class TextAlignOption {
    START,
    CENTER,
    END
}

@Serializable
enum class ImageScaleOption {
    CROP,
    FIT,
    FILL
}

@Serializable
enum class CalendarMode {
    MINI_MONTH,
    AGENDA_LIST
}

@Serializable
enum class ProgressBarStyle(val label: String) {
    CIRCULAR_GAUGE("Circular Gauge"),
    LINEAR_BAR("Linear Bar"),
    SEGMENTED_BAR("Segmented Bar")
}

@Serializable
enum class StackOrientation(val label: String) {
    VERTICAL("Vertical"),
    HORIZONTAL("Horizontal")
}

@Serializable
enum class GaugeMetric(val label: String) {
    HOURS_12("Hours (12h)"),
    HOURS_24("Hours (24h)"),
    MINUTES("Minutes"),
    MINUTES_5_STEP("Minutes (5 mins step)"),
    BATTERY("Battery"),
    BATTERY_0_TO_9("Battery (0 to 9)"),
    DAY_OF_WEEK("Day Of Week"),
    DAY_OF_WEEK_SHORT("Day Of Week Short"),
    DAY_OF_WEEK_NUM("Day Of Week Num"),
    DAY_OF_MONTH("Day Of Month"),
    DAY_OF_MONTH_SHORT("Day Of Month Short"),
    DAY_OF_MONTH_NUMBER("Day Of Month Number"),
    MONTH("Month"),
    MONTH_SHORT("Month Short"),
    RAM("RAM"),
    STORAGE("Storage"),
    VOLUME("Volume"),
    CUSTOM("Custom")
}

@Serializable
enum class GradientType {
    LINEAR,
    RADIAL
}

@Serializable
data class GradientConfig(
    val enabled: Boolean = false,
    val type: GradientType = GradientType.LINEAR,
    val startColorHex: String = "#FF4F8EF7",
    val endColorHex: String = "#FF8B5CF6",
    val angleDegrees: Float = 45f
)

@Serializable
data class LayerStyle(
    val xDp: Float = 16f,
    val yDp: Float = 16f,
    val widthDp: Float = 140f,
    val heightDp: Float = 56f,
    val rotationDegrees: Float = 0f,
    val opacity: Float = 1f,
    val zIndex: Int = 0,
    val paddingDp: Float = 8f,
    val marginDp: Float = 0f,
    val cornerRadiusDp: Float = 16f,
    val borderWidthDp: Float = 0f,
    val borderColorHex: String = "#33FFFFFF",
    val backgroundColorHex: String = "#00000000",
    val gradient: GradientConfig = GradientConfig(),
    val foregroundColorHex: String = "#FFFFFFFF",
    val accentColorHex: String = "#FF38BDF8",
    val shadowElevationDp: Float = 0f,
    val shadowColorHex: String = "#66000000",
    val glowRadiusDp: Float = 0f,
    val glowColorHex: String = "#FF5CE1E6",
    val glassBlurEnabled: Boolean = false,
    val glassTintAlpha: Float = 0.18f,
    val fontFamilyName: String = "Default",
    val fontAssetPath: String? = null,
    val fontSizeSp: Float = 16f,
    val lineHeightMultiplier: Float = 1.2f,
    val letterSpacingSp: Float = 0f,
    val fontWeight: FontWeightOption = FontWeightOption.MEDIUM,
    val textAlign: TextAlignOption = TextAlignOption.START
)

@Serializable
sealed interface TapAction {
    @Serializable
    @SerialName("none")
    data object None : TapAction

    @Serializable
    @SerialName("launch_app")
    data class LaunchApp(
        val packageName: String,
        val appLabel: String = ""
    ) : TapAction

    @Serializable
    @SerialName("toggle_flashlight")
    data object ToggleFlashlight : TapAction

    @Serializable
    @SerialName("toggle_silent_vibrate")
    data object ToggleSilentVibrate : TapAction

    @Serializable
    @SerialName("toggle_mute")
    data object ToggleMute : TapAction

    @Serializable
    @SerialName("media_play_pause")
    data object MediaPlayPause : TapAction

    @Serializable
    @SerialName("media_next")
    data object MediaNext : TapAction

    @Serializable
    @SerialName("media_previous")
    data object MediaPrevious : TapAction

    @Serializable
    @SerialName("volume_up")
    data object VolumeUp : TapAction

    @Serializable
    @SerialName("volume_down")
    data object VolumeDown : TapAction

    @Serializable
    @SerialName("open_wifi_settings")
    data object OpenWifiSettings : TapAction

    @Serializable
    @SerialName("open_bluetooth_settings")
    data object OpenBluetoothSettings : TapAction

    @Serializable
    @SerialName("open_sound_settings")
    data object OpenSoundSettings : TapAction

    @Serializable
    @SerialName("open_display_settings")
    data object OpenDisplaySettings : TapAction

    @Serializable
    @SerialName("open_alarm_clock")
    data object OpenAlarmClock : TapAction

    @Serializable
    @SerialName("open_camera")
    data object OpenCamera : TapAction

    @Serializable
    @SerialName("open_url")
    data class OpenUrl(val url: String) : TapAction

    @Serializable
    @SerialName("refresh_widget")
    data object RefreshWidget : TapAction

    @Serializable
    @SerialName("open_studio")
    data object OpenStudio : TapAction

    @Serializable
    @SerialName("studio_action")
    data class StudioAction(
        val mode: StudioActionMode = StudioActionMode.FORCE_WEATHER_UPDATE,
        val targetUrl: String = "",
        val notificationIndex: Int = 0
    ) : TapAction

    @Serializable
    @SerialName("launch_shortcut")
    data class LaunchShortcut(
        val shortcutUri: String = "",
        val label: String = "Shortcut"
    ) : TapAction

    @Serializable
    @SerialName("launch_activity")
    data class LaunchActivity(
        val packageName: String = "",
        val className: String = "",
        val label: String = "Activity"
    ) : TapAction

    @Serializable
    @SerialName("toggle_global_switch")
    data class ToggleGlobalSwitch(
        val switchName: String = "global_1"
    ) : TapAction

    @Serializable
    @SerialName("trigger_flow")
    data class TriggerFlow(
        val flowName: String = "Quick Flow",
        val steps: List<TapAction> = emptyList()
    ) : TapAction

    @Serializable
    @SerialName("change_volume")
    data class ChangeVolume(
        val stream: VolumeStreamType = VolumeStreamType.MUSIC,
        val deltaSteps: Int = 1,
        val showUi: Boolean = true
    ) : TapAction

    @Serializable
    @SerialName("disabled_block_touch")
    data object DisabledBlockTouch : TapAction
}

@Serializable
enum class StudioActionMode(val label: String) {
    FORCE_WEATHER_UPDATE("Force Weather Update"),
    FORCE_RSS_XML_UPDATE("Force RSS/Text/XML Update"),
    FORCE_IMAGE_UPDATE("Force Downloaded Images Update"),
    OPEN_NOTIFICATION("Open Notification"),
    DISMISS_NOTIFICATION("Dismiss Notification"),
    DISMISS_ALL_NOTIFICATIONS("Dismiss All Notifications")
}

@Serializable
enum class VolumeStreamType(val label: String, val androidStream: Int) {
    MUSIC("Media / Music", android.media.AudioManager.STREAM_MUSIC),
    RING("Ringtone", android.media.AudioManager.STREAM_RING),
    ALARM("Alarm", android.media.AudioManager.STREAM_ALARM),
    NOTIFICATION("Notification", android.media.AudioManager.STREAM_NOTIFICATION)
}

data class VisualLayerBounds(
    val leftDp: Float,
    val topDp: Float,
    val widthDp: Float,
    val heightDp: Float
) {
    val rightDp: Float get() = leftDp + widthDp
    val bottomDp: Float get() = topDp + heightDp
}

fun LayerStyle.computeVisualBounds(): VisualLayerBounds {
    val rad = kotlin.math.abs(rotationDegrees) * kotlin.math.PI / 180.0
    val cosA = kotlin.math.abs(kotlin.math.cos(rad)).toFloat()
    val sinA = kotlin.math.abs(kotlin.math.sin(rad)).toFloat()
    val visW = widthDp * cosA + heightDp * sinA
    val visH = widthDp * sinA + heightDp * cosA
    val visLeft = xDp + (widthDp - visW) / 2f
    val visTop = yDp + (heightDp - visH) / 2f
    return VisualLayerBounds(visLeft, visTop, visW, visH)
}

@Serializable
sealed interface LayerPayload {
    @Serializable
    @SerialName("digital_clock")
    data class DigitalClock(
        val is24Hour: Boolean = false,
        val showSeconds: Boolean = false,
        val showAmPm: Boolean = true,
        val customPattern: String? = null
    ) : LayerPayload

    @Serializable
    @SerialName("analog_clock")
    data class AnalogClock(
        val showSecondHand: Boolean = true,
        val showHourTicks: Boolean = true,
        val dialBorderWidthDp: Float = 2f
    ) : LayerPayload

    @Serializable
    @SerialName("date_text")
    data class DateText(
        val pattern: String = "EEE, MMM d",
        val uppercase: Boolean = false
    ) : LayerPayload

    @Serializable
    @SerialName("day_of_week")
    data class DayOfWeek(
        val fullName: Boolean = true,
        val uppercase: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("custom_text")
    data class CustomText(
        val text: String = "Hello Studio",
        val dynamicTemplate: String? = null,
        val maxLines: Int = 2
    ) : LayerPayload

    @Serializable
    @SerialName("battery_info")
    data class BatteryInfo(
        val showPercentage: Boolean = true,
        val showProgressBar: Boolean = true,
        val showChargingStatus: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("system_ram_storage")
    data class SystemRamStorage(
        val showRam: Boolean = true,
        val showStorage: Boolean = true,
        val showProgressBars: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("network_status")
    data class NetworkStatus(
        val showTransportType: Boolean = true,
        val showConnectionLabel: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("next_alarm")
    data class NextAlarm(
        val emptyText: String = "No alarm set",
        val showIcon: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("weather_card")
    data class WeatherCard(
        val cityName: String = "San Francisco",
        val latitude: Double = 37.7749,
        val longitude: Double = -122.4194,
        val useDeviceLocation: Boolean = true,
        val unitCelsius: Boolean = true,
        val showMinMax: Boolean = true,
        val showConditionIcon: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("weather_location")
    data class WeatherLocation(
        val cityName: String = "San Francisco",
        val latitude: Double = 37.7749,
        val longitude: Double = -122.4194,
        val useDeviceLocation: Boolean = true,
        val unitCelsius: Boolean = true,
        val showHumidityWind: Boolean = true,
        val showFeelsLike: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("weather_forecast")
    data class WeatherForecast(
        val cityName: String = "San Francisco",
        val latitude: Double = 37.7749,
        val longitude: Double = -122.4194,
        val useDeviceLocation: Boolean = true,
        val unitCelsius: Boolean = true,
        val daysCount: Int = 3
    ) : LayerPayload

    @Serializable
    @SerialName("location_badge")
    data class LocationBadge(
        val fallbackCity: String = "San Francisco",
        val showCoordinates: Boolean = false,
        val showPinIcon: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("countdown_timer")
    data class CountdownTimer(
        val title: String = "Launch Day",
        val targetEpochMillis: Long = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L
    ) : LayerPayload

    @Serializable
    @SerialName("quick_toggle_bar")
    data class QuickToggleBar(
        val showFlashlight: Boolean = true,
        val showMuteToggle: Boolean = true,
        val showPlayPause: Boolean = true,
        val showWifiSettings: Boolean = true,
        val showBluetoothSettings: Boolean = false,
        val showVolumeUp: Boolean = false,
        val showVolumeDown: Boolean = false,
        val showRefresh: Boolean = true,
        val showStudioShortcut: Boolean = false
    ) : LayerPayload

    @Serializable
    @SerialName("audio_volume_bar")
    data class AudioVolumeBar(
        val showPercent: Boolean = true,
        val showMuteBadge: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("greeting_banner")
    data class GreetingBanner(
        val userName: String = "Creator",
        val showSubtitle: Boolean = true,
        val subtitleTemplate: String = "{{day}} · {{weather_temp}}"
    ) : LayerPayload

    @Serializable
    @SerialName("world_clock")
    data class WorldClock(
        val cityLabel: String = "Tokyo",
        val timeZoneId: String = "Asia/Tokyo",
        val is24Hour: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("progress_ring")
    data class ProgressRing(
        val metric: GaugeMetric = GaugeMetric.BATTERY,
        val style: ProgressBarStyle = ProgressBarStyle.CIRCULAR_GAUGE,
        val showLabel: Boolean = true,
        val strokeWidthDp: Float = 6f,
        val segmentsCount: Int = 10,
        val customValueExpression: String = "{{battery}}",
        val customMin: Float = 0f,
        val customMax: Float = 100f,
        val customLabel: String = "Custom"
    ) : LayerPayload

    @Serializable
    @SerialName("quick_note")
    data class QuickNote(
        val noteText: String = "Focus on what matters today.",
        val showAccentBar: Boolean = true,
        val isChecklist: Boolean = false
    ) : LayerPayload

    @Serializable
    @SerialName("toggle_button")
    data class ToggleButton(
        val kind: ToggleButtonKind = ToggleButtonKind.FLASHLIGHT,
        val showLabel: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("calendar_view")
    data class CalendarView(
        val mode: CalendarMode = CalendarMode.AGENDA_LIST,
        val maxEvents: Int = 3,
        val showEventTime: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("shape_container")
    data class ShapeContainer(
        val shapeKind: ShapeKind = ShapeKind.RECTANGLE,
        val lineThicknessDp: Float = 2f,
        val startAngleDeg: Float = -90f,
        val sweepAngleDeg: Float = 120f,
        val customPathData: String = "M 50 4 L 96 38 L 78 94 L 22 94 L 4 38 Z",
        val filled: Boolean = true
    ) : LayerPayload

    @Serializable
    @SerialName("overlap_group")
    data class OverlapGroup(
        val embeddedLayers: List<WidgetLayer> = emptyList()
    ) : LayerPayload

    @Serializable
    @SerialName("stack_group")
    data class StackGroup(
        val orientation: StackOrientation = StackOrientation.VERTICAL,
        val spacingDp: Float = 6f,
        val embeddedLayers: List<WidgetLayer> = emptyList()
    ) : LayerPayload

    @Serializable
    @SerialName("vector_icon")
    data class VectorIcon(
        val symbolKey: String = "bolt"
    ) : LayerPayload

    @Serializable
    @SerialName("image_media")
    data class ImageMedia(
        val assetPathOrUri: String? = null,
        val scaleOption: ImageScaleOption = ImageScaleOption.CROP,
        val isAiGenerated: Boolean = false,
        val lastAiPrompt: String? = null
    ) : LayerPayload

    @Serializable
    @SerialName("music_player")
    data class MusicPlayer(
        val showAlbumArt: Boolean = true,
        val showPlaybackControls: Boolean = true,
        val compactMode: Boolean = false
    ) : LayerPayload

    @Serializable
    @SerialName("custom_component_ref")
    data class CustomComponentRef(
        val componentId: String,
        val componentName: String,
        val embeddedLayers: List<WidgetLayer> = emptyList()
    ) : LayerPayload

    @Serializable
    @SerialName("custom_xml_snippet")
    data class CustomXmlSnippet(
        val rawMarkup: String = DEFAULT_SNIPPET,
        val isJsonFormat: Boolean = false
    ) : LayerPayload {
        companion object {
            const val DEFAULT_SNIPPET =
                """<LinearLayout orientation="vertical" padding="12" background="#221E293B" cornerRadius="16">
  <Text text="{{time}}" textSize="24" textColor="#FFFFFFFF" />
  <Text text="Battery {{battery}}%" textSize="13" textColor="#FF38BDF8" />
</LinearLayout>"""
        }
    }
}

@Serializable
enum class ToggleButtonKind(val label: String) {
    FLASHLIGHT("Flashlight"),
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth"),
    SILENT_VIBRATE("Silent / Vibrate"),
    MUTE("Mute"),
    PLAY_PAUSE("Play / Pause"),
    REFRESH("Refresh")
}

