package com.example.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.WidgetApplyBridge
import com.example.data.backup.AssetStorageManager
import com.example.data.remote.GeocodingPlaceDto
import com.example.data.repository.AiImageRepository
import com.example.data.repository.CustomComponentRepository
import com.example.data.repository.DeviceTelemetrySnapshot
import com.example.data.repository.ExportedActivityInfo
import com.example.data.repository.LaunchableAppInfo
import com.example.data.repository.RssFeedSnapshot
import com.example.data.repository.ShortcutPresetInfo
import com.example.data.repository.StudioActionsRepository
import com.example.data.repository.SystemTelemetryRepository
import com.example.data.repository.WeatherRepository
import com.example.data.repository.WeatherSnapshot
import com.example.data.repository.WidgetRepository
import com.example.domain.model.CalendarMode
import com.example.domain.model.CustomComponent
import com.example.domain.model.GaugeMetric
import android.view.KeyEvent
import com.example.domain.model.GradientConfig
import com.example.domain.model.LayerPayload
import com.example.domain.model.LayerStyle
import com.example.domain.model.LayerType
import com.example.domain.model.ProgressBarStyle
import com.example.domain.model.ShapeKind
import com.example.domain.model.StackOrientation
import com.example.domain.model.StudioActionMode
import com.example.domain.model.TapAction
import com.example.domain.model.ToggleButtonKind
import com.example.domain.model.WidgetDimension
import com.example.domain.model.WidgetLayer
import com.example.domain.model.WidgetProject
import com.example.domain.model.computeVisualBounds
import com.example.ui.ai.AiImageUiState
import com.google.firebase.ai.type.AspectRatio
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CanvasEdge {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM
}

data class EditorUiState(
    val isLoading: Boolean = true,
    val isComponentMode: Boolean = false,
    val targetId: String = "",
    val title: String = "Untitled",
    val componentCategory: String = "General",
    val dimension: WidgetDimension = WidgetDimension.SIZE_4X2,
    val canvasWidthDp: Float = 320f,
    val canvasHeightDp: Float = 160f,
    val gridSnapDp: Float = 8f,
    val snapEnabled: Boolean = true,
    val showGridLines: Boolean = true,
    val zoomScale: Float = 1f,
    val backgroundColorHex: String = "#FF181C2E",
    val backgroundGradient: GradientConfig = GradientConfig(),
    val backgroundCornerRadiusDp: Float = 24f,
    val layers: List<WidgetLayer> = emptyList(),
    val selectedLayerIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val savedComponents: List<CustomComponent> = emptyList(),
    val telemetry: DeviceTelemetrySnapshot = DeviceTelemetrySnapshot(),
    val weather: WeatherSnapshot? = null,
    val citySearchResults: List<GeocodingPlaceDto> = emptyList(),
    val isSearchingCities: Boolean = false,
    val installedApps: List<LaunchableAppInfo> = emptyList(),
    val exportedActivities: List<ExportedActivityInfo> = emptyList(),
    val shortcutPresets: List<ShortcutPresetInfo> = emptyList(),
    val globalSwitches: Map<String, Boolean> = emptyMap(),
    val rssSnapshot: RssFeedSnapshot? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isFlashlightOn: Boolean = SystemTelemetryRepository.isTorchEnabled,
    val aiImageState: AiImageUiState = AiImageUiState.Idle,
    val statusMessage: String? = null,
    val isSaving: Boolean = false
) {
    val primarySelectedLayer: WidgetLayer?
        get() = selectedLayerIds.firstOrNull()?.let { id -> layers.firstOrNull { it.id == id } }

    val isWindowTransparent: Boolean
        get() = !backgroundGradient.enabled && backgroundColorHex.trim().removePrefix("#").let { hex ->
            hex.length == 8 && hex.startsWith("00")
        }
}

private data class CanvasSnapshot(
    val layers: List<WidgetLayer>,
    val backgroundColorHex: String,
    val backgroundGradient: GradientConfig,
    val backgroundCornerRadiusDp: Float
)

class EditorViewModel(
    private val targetId: String,
    private val isComponentMode: Boolean,
    private val widgetRepository: WidgetRepository,
    private val componentRepository: CustomComponentRepository,
    private val assetStorageManager: AssetStorageManager,
    private val telemetryRepository: SystemTelemetryRepository,
    private val weatherRepository: WeatherRepository,
    private val studioActionsRepository: StudioActionsRepository,
    private val aiImageRepository: AiImageRepository,
    private val widgetApplyBridge: WidgetApplyBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(isComponentMode = isComponentMode, targetId = targetId))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<CanvasSnapshot>()
    private val redoStack = ArrayDeque<CanvasSnapshot>()
    private var loadedProject: WidgetProject? = null
    private var loadedComponent: CustomComponent? = null

    init {
        loadTarget()
        observeCustomComponents()
        observeGlobalSwitches()
        refreshLiveData()
        loadInstalledApps()
    }

    private fun loadTarget() {
        viewModelScope.launch {
            if (isComponentMode) {
                val component = componentRepository.getComponentById(targetId)
                if (component != null) {
                    loadedComponent = component
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = component.name,
                            componentCategory = component.category,
                            canvasWidthDp = component.widthDp,
                            canvasHeightDp = component.heightDp,
                            layers = component.layers,
                            selectedLayerIds = component.layers.firstOrNull()?.let { l -> setOf(l.id) } ?: emptySet()
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Component not found.") }
                }
            } else {
                val project = widgetRepository.getProjectOnce(targetId)
                if (project != null) {
                    loadedProject = project
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = project.name,
                            dimension = project.dimension,
                            canvasWidthDp = project.resolvedWidthDp,
                            canvasHeightDp = project.resolvedHeightDp,
                            gridSnapDp = project.gridSnapDp,
                            backgroundColorHex = project.backgroundColorHex,
                            backgroundGradient = project.backgroundGradient,
                            backgroundCornerRadiusDp = project.backgroundCornerRadiusDp,
                            layers = project.layers,
                            selectedLayerIds = project.layers.firstOrNull()?.let { l -> setOf(l.id) } ?: emptySet()
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Widget project not found.") }
                }
            }
        }
    }

    private fun observeCustomComponents() {
        viewModelScope.launch {
            componentRepository.observeAllComponents().collect { list ->
                _uiState.update { it.copy(savedComponents = list) }
            }
        }
    }

    private fun observeGlobalSwitches() {
        viewModelScope.launch {
            studioActionsRepository.globalSwitches.collect { switches ->
                _uiState.update { it.copy(globalSwitches = switches) }
            }
        }
    }

    fun refreshLiveData() {
        viewModelScope.launch {
            val snap = telemetryRepository.collectSnapshot()
            _uiState.update { it.copy(telemetry = snap) }

            val weatherInfo = _uiState.value.layers.firstNotNullOfOrNull { layer ->
                when (val p = layer.payload) {
                    is LayerPayload.WeatherCard -> Triple(p.latitude, p.longitude, p.cityName to p.useDeviceLocation)
                    is LayerPayload.WeatherLocation -> Triple(p.latitude, p.longitude, p.cityName to p.useDeviceLocation)
                    is LayerPayload.WeatherForecast -> Triple(p.latitude, p.longitude, p.cityName to p.useDeviceLocation)
                    else -> null
                }
            }
            val lat = weatherInfo?.first ?: 37.7749
            val lon = weatherInfo?.second ?: -122.4194
            val city = weatherInfo?.third?.first ?: "San Francisco"
            val useLoc = weatherInfo?.third?.second ?: true

            weatherRepository.fetchWeather(city, lat, lon, useLoc)
                .onSuccess { weather ->
                    _uiState.update { it.copy(weather = weather) }
                }
        }
    }

    fun searchWeatherCities(query: String) {
        if (query.trim().length < 2) {
            _uiState.update { it.copy(citySearchResults = emptyList(), isSearchingCities = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCities = true) }
            weatherRepository.searchCities(query)
                .onSuccess { results ->
                    _uiState.update { it.copy(citySearchResults = results, isSearchingCities = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(citySearchResults = emptyList(), isSearchingCities = false) }
                }
        }
    }

    fun applyCityToWeatherLayer(layerId: String, place: GeocodingPlaceDto) {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        val updatedPayload = when (val p = layer.payload) {
            is LayerPayload.WeatherCard -> p.copy(
                cityName = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                useDeviceLocation = false
            )
            is LayerPayload.WeatherLocation -> p.copy(
                cityName = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                useDeviceLocation = false
            )
            is LayerPayload.WeatherForecast -> p.copy(
                cityName = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                useDeviceLocation = false
            )
            else -> p
        }
        updateLayer(layer.copy(payload = updatedPayload))
        _uiState.update { it.copy(citySearchResults = emptyList()) }
        refreshLiveData()
    }

    fun dispatchMediaControl(keyCode: Int) {
        telemetryRepository.dispatchMediaKey(keyCode)
        viewModelScope.launch {
            delay(180L)
            val snap = telemetryRepository.collectSnapshot()
            _uiState.update { it.copy(telemetry = snap) }
        }
    }

    fun adjustVolumeControl(direction: Int) {
        telemetryRepository.adjustMediaVolume(direction)
        viewModelScope.launch {
            delay(100L)
            val snap = telemetryRepository.collectSnapshot()
            _uiState.update { it.copy(telemetry = snap) }
        }
    }

    fun executeTapAction(action: TapAction) {
        when (action) {
            is TapAction.None -> Unit
            is TapAction.ToggleFlashlight -> {
                val next = telemetryRepository.toggleFlashlight()
                _uiState.update {
                    it.copy(
                        isFlashlightOn = next,
                        statusMessage = if (next) "Flashlight turned ON" else "Flashlight turned OFF"
                    )
                }
            }
            is TapAction.ToggleSilentVibrate -> {
                val mode = telemetryRepository.toggleSilentVibrate()
                viewModelScope.launch {
                    delay(100L)
                    val snap = telemetryRepository.collectSnapshot()
                    _uiState.update {
                        it.copy(
                            telemetry = snap,
                            statusMessage = "Ringer mode: $mode"
                        )
                    }
                }
            }
            is TapAction.ToggleMute -> {
                val muted = telemetryRepository.toggleMediaMute()
                viewModelScope.launch {
                    delay(100L)
                    val snap = telemetryRepository.collectSnapshot()
                    _uiState.update {
                        it.copy(
                            telemetry = snap,
                            statusMessage = if (muted) "Media audio muted" else "Media audio unmuted"
                        )
                    }
                }
            }
            is TapAction.MediaPlayPause -> dispatchMediaControl(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            is TapAction.MediaNext -> dispatchMediaControl(KeyEvent.KEYCODE_MEDIA_NEXT)
            is TapAction.MediaPrevious -> dispatchMediaControl(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            is TapAction.VolumeUp -> adjustVolumeControl(android.media.AudioManager.ADJUST_RAISE)
            is TapAction.VolumeDown -> adjustVolumeControl(android.media.AudioManager.ADJUST_LOWER)
            is TapAction.OpenWifiSettings -> telemetryRepository.openSystemAction("WIFI")
            is TapAction.OpenBluetoothSettings -> telemetryRepository.openSystemAction("BLUETOOTH")
            is TapAction.OpenSoundSettings -> telemetryRepository.openSystemAction("SOUND")
            is TapAction.OpenDisplaySettings -> telemetryRepository.openSystemAction("DISPLAY")
            is TapAction.OpenAlarmClock -> telemetryRepository.openSystemAction("ALARM")
            is TapAction.OpenCamera -> telemetryRepository.openSystemAction("CAMERA")
            is TapAction.RefreshWidget -> {
                refreshLiveData()
                _uiState.update { it.copy(statusMessage = "Live telemetry & weather refreshed") }
            }
            is TapAction.OpenStudio -> _uiState.update { it.copy(statusMessage = "Studio already open") }
            is TapAction.LaunchApp -> _uiState.update { it.copy(statusMessage = "Tap action launches ${action.appLabel}") }
            is TapAction.OpenUrl -> _uiState.update { it.copy(statusMessage = "Tap action opens ${action.url}") }
            is TapAction.StudioAction -> executeStudioAction(action)
            is TapAction.LaunchShortcut -> {
                val ok = studioActionsRepository.launchShortcutUri(action.shortcutUri)
                _uiState.update {
                    it.copy(statusMessage = if (ok) "Launched ${action.label}" else "Invalid or unavailable shortcut URI")
                }
            }
            is TapAction.LaunchActivity -> {
                val ok = studioActionsRepository.launchExportedActivity(action.packageName, action.className)
                _uiState.update {
                    it.copy(statusMessage = if (ok) "Launched ${action.label}" else "Unable to launch exported activity")
                }
            }
            is TapAction.ToggleGlobalSwitch -> {
                val next = studioActionsRepository.toggleGlobalSwitch(action.switchName)
                viewModelScope.launch {
                    val snap = telemetryRepository.collectSnapshot()
                    _uiState.update {
                        it.copy(
                            telemetry = snap,
                            statusMessage = "Global \"${action.switchName}\" -> ${if (next) "ON" else "OFF"}"
                        )
                    }
                }
            }
            is TapAction.TriggerFlow -> {
                viewModelScope.launch {
                    action.steps.forEach { step ->
                        if (step !is TapAction.TriggerFlow) {
                            executeTapAction(step)
                            delay(80L)
                        }
                    }
                    _uiState.update { it.copy(statusMessage = "Executed flow \"${action.flowName}\" (${action.steps.size} steps)") }
                }
            }
            is TapAction.ChangeVolume -> {
                studioActionsRepository.adjustStreamVolume(
                    stream = action.stream.androidStream,
                    deltaSteps = action.deltaSteps,
                    showUi = action.showUi
                )
                viewModelScope.launch {
                    delay(100L)
                    val snap = telemetryRepository.collectSnapshot()
                    val dir = if (action.deltaSteps >= 0) "+${action.deltaSteps}" else "${action.deltaSteps}"
                    _uiState.update { it.copy(telemetry = snap, statusMessage = "${action.stream.label} volume $dir") }
                }
            }
            is TapAction.DisabledBlockTouch -> {
                _uiState.update { it.copy(statusMessage = "Touch blocked on this component") }
            }
        }
    }

    private fun executeStudioAction(action: TapAction.StudioAction) {
        viewModelScope.launch {
            when (action.mode) {
                StudioActionMode.FORCE_WEATHER_UPDATE -> {
                    weatherRepository.clearCache()
                    refreshLiveData()
                    _uiState.update { it.copy(statusMessage = "Forced live weather refresh") }
                }
                StudioActionMode.FORCE_RSS_XML_UPDATE -> {
                    _uiState.update { it.copy(statusMessage = "Syncing RSS / Text / XML feed...") }
                    studioActionsRepository.forceUpdateRssOrTextXml(action.targetUrl)
                        .onSuccess { feed ->
                            val snap = telemetryRepository.collectSnapshot()
                            _uiState.update {
                                it.copy(
                                    telemetry = snap,
                                    rssSnapshot = feed,
                                    statusMessage = "Feed updated: ${feed.title}"
                                )
                            }
                        }
                        .onFailure { err ->
                            _uiState.update { it.copy(statusMessage = err.localizedMessage ?: "Failed to update RSS/XML feed") }
                        }
                }
                StudioActionMode.FORCE_IMAGE_UPDATE -> {
                    val layerUrls = _uiState.value.layers
                        .mapNotNull { (it.payload as? LayerPayload.ImageMedia)?.assetPathOrUri }
                    val urls = (listOf(action.targetUrl) + layerUrls).filter { it.isNotBlank() }
                    studioActionsRepository.forceUpdateDownloadedImages(urls)
                        .onSuccess { count ->
                            _uiState.update { it.copy(statusMessage = "Refreshed $count downloaded image cache entries") }
                        }
                        .onFailure { err ->
                            _uiState.update { it.copy(statusMessage = err.localizedMessage ?: "Image refresh failed") }
                        }
                }
                StudioActionMode.OPEN_NOTIFICATION -> {
                    val msg = studioActionsRepository.openNotificationOrSettings(action.notificationIndex)
                    _uiState.update { it.copy(statusMessage = msg) }
                }
                StudioActionMode.DISMISS_NOTIFICATION -> {
                    val msg = studioActionsRepository.dismissNotification(action.notificationIndex)
                    val snap = telemetryRepository.collectSnapshot()
                    _uiState.update { it.copy(telemetry = snap, statusMessage = msg) }
                }
                StudioActionMode.DISMISS_ALL_NOTIFICATIONS -> {
                    val msg = studioActionsRepository.dismissAllNotifications()
                    val snap = telemetryRepository.collectSnapshot()
                    _uiState.update { it.copy(telemetry = snap, statusMessage = msg) }
                }
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = telemetryRepository.queryLaunchableApps()
            val activities = studioActionsRepository.queryExportedActivities()
            val shortcuts = studioActionsRepository.getBuiltinShortcutPresets()
            val rss = studioActionsRepository.getLatestRssSnapshot()
            _uiState.update {
                it.copy(
                    installedApps = apps,
                    exportedActivities = activities,
                    shortcutPresets = shortcuts,
                    rssSnapshot = rss
                )
            }
        }
    }

    private fun pushUndoSnapshot() {
        val state = _uiState.value
        undoStack.addLast(
            CanvasSnapshot(
                layers = state.layers,
                backgroundColorHex = state.backgroundColorHex,
                backgroundGradient = state.backgroundGradient,
                backgroundCornerRadiusDp = state.backgroundCornerRadiusDp
            )
        )
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = false) }
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        val current = _uiState.value
        redoStack.addLast(
            CanvasSnapshot(
                layers = current.layers,
                backgroundColorHex = current.backgroundColorHex,
                backgroundGradient = current.backgroundGradient,
                backgroundCornerRadiusDp = current.backgroundCornerRadiusDp
            )
        )
        _uiState.update {
            it.copy(
                layers = prev.layers,
                backgroundColorHex = prev.backgroundColorHex,
                backgroundGradient = prev.backgroundGradient,
                backgroundCornerRadiusDp = prev.backgroundCornerRadiusDp,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        val current = _uiState.value
        undoStack.addLast(
            CanvasSnapshot(
                layers = current.layers,
                backgroundColorHex = current.backgroundColorHex,
                backgroundGradient = current.backgroundGradient,
                backgroundCornerRadiusDp = current.backgroundCornerRadiusDp
            )
        )
        _uiState.update {
            it.copy(
                layers = next.layers,
                backgroundColorHex = next.backgroundColorHex,
                backgroundGradient = next.backgroundGradient,
                backgroundCornerRadiusDp = next.backgroundCornerRadiusDp,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun setDimension(dimension: WidgetDimension) {
        _uiState.update {
            it.copy(
                dimension = dimension,
                canvasWidthDp = dimension.defaultWidthDp,
                canvasHeightDp = dimension.defaultHeightDp
            )
        }
    }

    fun setCustomDimension(widthDp: Float, heightDp: Float) {
        val w = widthDp.coerceIn(80f, 400f)
        val h = heightDp.coerceIn(60f, 400f)
        _uiState.update {
            it.copy(
                dimension = WidgetDimension.CUSTOM,
                canvasWidthDp = w,
                canvasHeightDp = h,
                statusMessage = "Canvas set to Custom (${w.toInt()}×${h.toInt()}dp)"
            )
        }
    }

    fun toggleSnapToGrid() {
        _uiState.update { it.copy(snapEnabled = !it.snapEnabled) }
    }

    fun toggleGridLines() {
        _uiState.update { it.copy(showGridLines = !it.showGridLines) }
    }

    fun setZoom(scale: Float) {
        _uiState.update { it.copy(zoomScale = scale.coerceIn(0.6f, 2.0f)) }
    }

    fun toggleMultiSelectMode() {
        _uiState.update { state ->
            val nextMulti = !state.isMultiSelectMode
            val nextSelection = if (!nextMulti && state.selectedLayerIds.size > 1) {
                setOf(state.selectedLayerIds.first())
            } else {
                state.selectedLayerIds
            }
            state.copy(isMultiSelectMode = nextMulti, selectedLayerIds = nextSelection)
        }
    }

    fun selectLayer(layerId: String?) {
        _uiState.update { state ->
            if (layerId == null) {
                state.copy(selectedLayerIds = emptySet())
            } else if (state.isMultiSelectMode) {
                val updated = if (layerId in state.selectedLayerIds) {
                    state.selectedLayerIds - layerId
                } else {
                    state.selectedLayerIds + layerId
                }
                state.copy(selectedLayerIds = updated)
            } else {
                state.copy(selectedLayerIds = setOf(layerId))
            }
        }
    }

    fun addBuiltInLayer(type: LayerType) {
        pushUndoSnapshot()
        val state = _uiState.value
        val maxZ = state.layers.maxOfOrNull { it.style.zIndex } ?: 0
        val newId = UUID.randomUUID().toString()
        val (defaultStyle, defaultPayload, defaultAction) = createDefaultsForType(type, maxZ + 1)

        val newLayer = WidgetLayer(
            id = newId,
            projectId = targetId,
            name = type.displayName,
            type = type,
            style = defaultStyle,
            payload = defaultPayload,
            tapAction = defaultAction
        )

        _uiState.update {
            it.copy(
                layers = it.layers + newLayer,
                selectedLayerIds = setOf(newId)
            )
        }
    }

    fun insertCustomComponent(component: CustomComponent) {
        pushUndoSnapshot()
        val state = _uiState.value
        val maxZ = state.layers.maxOfOrNull { it.style.zIndex } ?: 0
        val newId = UUID.randomUUID().toString()
        val layer = WidgetLayer(
            id = newId,
            projectId = targetId,
            name = component.name,
            type = LayerType.CUSTOM_COMPONENT_REF,
            style = LayerStyle(
                xDp = 16f,
                yDp = 16f,
                widthDp = component.widthDp.coerceAtMost(state.canvasWidthDp),
                heightDp = component.heightDp.coerceAtMost(state.canvasHeightDp),
                paddingDp = 0f,
                zIndex = maxZ + 1
            ),
            payload = LayerPayload.CustomComponentRef(
                componentId = component.id,
                componentName = component.name,
                embeddedLayers = component.layers
            )
        )
        _uiState.update {
            it.copy(
                layers = it.layers + layer,
                selectedLayerIds = setOf(newId)
            )
        }
    }

    fun beginDragOrResize() {
        pushUndoSnapshot()
    }

    fun moveLayerBy(layerId: String, deltaXDp: Float, deltaYDp: Float, applySnap: Boolean) {
        val state = _uiState.value
        val snap = if (applySnap && state.snapEnabled) state.gridSnapDp else 1f
        _uiState.update { current ->
            val updated = current.layers.map { layer ->
                if (layer.id == layerId && !layer.isLocked) {
                    val vis = layer.style.computeVisualBounds()
                    val offsetX = layer.style.xDp - vis.leftDp
                    val offsetY = layer.style.yDp - vis.topDp

                    val minVisLeft = if (vis.widthDp <= current.canvasWidthDp) -4f else (current.canvasWidthDp - vis.widthDp - 8f)
                    val maxVisLeft = if (vis.widthDp <= current.canvasWidthDp) (current.canvasWidthDp - vis.widthDp + 4f) else 8f
                    val minVisTop = if (vis.heightDp <= current.canvasHeightDp) -4f else (current.canvasHeightDp - vis.heightDp - 8f)
                    val maxVisTop = if (vis.heightDp <= current.canvasHeightDp) (current.canvasHeightDp - vis.heightDp + 4f) else 8f

                    val rawVisLeft = (vis.leftDp + deltaXDp).coerceIn(minVisLeft, maxVisLeft)
                    val rawVisTop = (vis.topDp + deltaYDp).coerceIn(minVisTop, maxVisTop)

                    val snappedVisLeft = if (applySnap && current.snapEnabled) {
                        snapVisualEdge(rawVisLeft, vis.widthDp, current.canvasWidthDp, snap)
                    } else rawVisLeft

                    val snappedVisTop = if (applySnap && current.snapEnabled) {
                        snapVisualEdge(rawVisTop, vis.heightDp, current.canvasHeightDp, snap)
                    } else rawVisTop

                    layer.copy(
                        style = layer.style.copy(
                            xDp = snappedVisLeft + offsetX,
                            yDp = snappedVisTop + offsetY
                        )
                    )
                } else layer
            }
            current.copy(layers = updated)
        }
    }

    fun resizeLayerBy(layerId: String, deltaWDp: Float, deltaHDp: Float, applySnap: Boolean) {
        val state = _uiState.value
        val snap = if (applySnap && state.snapEnabled) state.gridSnapDp else 1f
        _uiState.update { current ->
            val updated = current.layers.map { layer ->
                if (layer.id == layerId && !layer.isLocked) {
                    val rawW = (layer.style.widthDp + deltaWDp).coerceIn(24f, current.canvasWidthDp + 40f)
                    val rawH = (layer.style.heightDp + deltaHDp).coerceIn(20f, current.canvasHeightDp + 40f)
                    val snappedW = if (applySnap && current.snapEnabled) snapValue(rawW, snap).coerceAtLeast(24f) else rawW
                    val snappedH = if (applySnap && current.snapEnabled) snapValue(rawH, snap).coerceAtLeast(20f) else rawH
                    layer.copy(style = layer.style.copy(widthDp = snappedW, heightDp = snappedH))
                } else layer
            }
            current.copy(layers = updated)
        }
    }

    fun rotateLayerBy(layerId: String, deltaDegrees: Float, recordUndo: Boolean = true) {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        if (layer.isLocked) return
        val next = normalizeDegrees(layer.style.rotationDegrees + deltaDegrees)
        val rotatedStyle = clampRotatedStyleWithinCanvas(
            layer.style.copy(rotationDegrees = next),
            _uiState.value.canvasWidthDp,
            _uiState.value.canvasHeightDp
        )
        updateLayer(layer.copy(style = rotatedStyle), recordUndo = recordUndo)
    }

    fun setLayerRotation(layerId: String, degrees: Float, recordUndo: Boolean = false) {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        if (layer.isLocked) return
        val rotatedStyle = clampRotatedStyleWithinCanvas(
            layer.style.copy(rotationDegrees = normalizeDegrees(degrees)),
            _uiState.value.canvasWidthDp,
            _uiState.value.canvasHeightDp
        )
        updateLayer(layer.copy(style = rotatedStyle), recordUndo = recordUndo)
    }

    fun setLayerOpacity(layerId: String, opacity: Float, recordUndo: Boolean = false) {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        updateLayer(
            layer.copy(style = layer.style.copy(opacity = opacity.coerceIn(0.05f, 1f))),
            recordUndo = recordUndo
        )
    }

    fun centerLayerHorizontally(layerId: String) {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        if (layer.isLocked) return
        val targetX = (_uiState.value.canvasWidthDp - layer.style.widthDp) / 2f
        updateLayer(layer.copy(style = layer.style.copy(xDp = targetX)), recordUndo = true)
    }

    fun centerLayerVertically(layerId: String) {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        if (layer.isLocked) return
        val targetY = (_uiState.value.canvasHeightDp - layer.style.heightDp) / 2f
        updateLayer(layer.copy(style = layer.style.copy(yDp = targetY)), recordUndo = true)
    }

    fun alignLayerToEdge(layerId: String, edge: CanvasEdge) {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        if (layer.isLocked) return
        val state = _uiState.value
        val vis = layer.style.computeVisualBounds()
        val offsetX = layer.style.xDp - vis.leftDp
        val offsetY = layer.style.yDp - vis.topDp
        val updatedStyle = when (edge) {
            CanvasEdge.LEFT -> layer.style.copy(xDp = 0f + offsetX)
            CanvasEdge.TOP -> layer.style.copy(yDp = 0f + offsetY)
            CanvasEdge.RIGHT -> layer.style.copy(xDp = (state.canvasWidthDp - vis.widthDp) + offsetX)
            CanvasEdge.BOTTOM -> layer.style.copy(yDp = (state.canvasHeightDp - vis.heightDp) + offsetY)
        }
        updateLayer(layer.copy(style = updatedStyle), recordUndo = true)
    }

    fun nudgeLayer(layerId: String, dxDp: Float, dyDp: Float) {
        pushUndoSnapshot()
        moveLayerBy(layerId, dxDp, dyDp, applySnap = false)
    }

    private fun clampRotatedStyleWithinCanvas(
        style: LayerStyle,
        canvasW: Float,
        canvasH: Float
    ): LayerStyle {
        val vis = style.computeVisualBounds()
        val offsetX = style.xDp - vis.leftDp
        val offsetY = style.yDp - vis.topDp
        val clampedVisLeft = if (vis.widthDp <= canvasW) {
            vis.leftDp.coerceIn(0f, canvasW - vis.widthDp)
        } else {
            (canvasW - vis.widthDp) / 2f
        }
        val clampedVisTop = if (vis.heightDp <= canvasH) {
            vis.topDp.coerceIn(0f, canvasH - vis.heightDp)
        } else {
            (canvasH - vis.heightDp) / 2f
        }
        return style.copy(xDp = clampedVisLeft + offsetX, yDp = clampedVisTop + offsetY)
    }

    private fun snapVisualEdge(rawEdge: Float, visSize: Float, canvasSize: Float, step: Float): Float {
        if (abs(rawEdge) <= 4f) return 0f
        val flushEnd = canvasSize - visSize
        if (visSize <= canvasSize && abs(rawEdge - flushEnd) <= 4f) return flushEnd
        return snapValue(rawEdge, step)
    }

    private fun normalizeDegrees(deg: Float): Float {
        var value = deg
        while (value > 180f) value -= 360f
        while (value < -180f) value += 360f
        return value
    }

    fun updateLayer(updatedLayer: WidgetLayer, recordUndo: Boolean = true) {
        if (recordUndo) pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map { if (it.id == updatedLayer.id) updatedLayer else it }
            )
        }
    }

    fun updateCanvasBackground(
        colorHex: String = _uiState.value.backgroundColorHex,
        gradient: GradientConfig = _uiState.value.backgroundGradient,
        cornerRadiusDp: Float = _uiState.value.backgroundCornerRadiusDp
    ) {
        pushUndoSnapshot()
        _uiState.update {
            it.copy(
                backgroundColorHex = colorHex,
                backgroundGradient = gradient,
                backgroundCornerRadiusDp = cornerRadiusDp.coerceIn(0f, 48f)
            )
        }
    }

    fun toggleTransparentWindow() {
        val state = _uiState.value
        pushUndoSnapshot()
        if (state.isWindowTransparent) {
            _uiState.update {
                it.copy(
                    backgroundColorHex = "#FF181C2E",
                    statusMessage = "Widget window background restored."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    backgroundColorHex = "#00000000",
                    backgroundGradient = state.backgroundGradient.copy(enabled = false),
                    statusMessage = "Main window is now transparent — your wallpaper will show behind components."
                )
            }
        }
    }

    fun convertLayerToCustomCode(layerId: String): LayerPayload.CustomXmlSnippet? {
        val layer = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return null
        if (layer.payload is LayerPayload.CustomXmlSnippet) return layer.payload
        val fg = layer.style.foregroundColorHex
        val accent = layer.style.accentColorHex
        val bg = layer.style.backgroundColorHex.takeUnless { it.equals("#00000000", ignoreCase = true) } ?: "#221E293B"
        val r = layer.style.cornerRadiusDp.toInt()
        val font = layer.style.fontFamilyName
        val snippetXml = when (layer.payload) {
            is LayerPayload.DigitalClock, is LayerPayload.WorldClock ->
                """<LinearLayout orientation="vertical" padding="10" background="$bg" cornerRadius="$r" borderColor="$accent" borderWidth="1">
  <Row gravity="center" spacing="6">
    <Icon name="alarm" color="$accent" size="18" />
    <Text text="{{time}}" textSize="28" textColor="$fg" bold="true" fontFamily="$font" />
  </Row>
  <Divider color="#33FFFFFF" thickness="1" />
  <Text text="{{date}}" textSize="12" textColor="$accent" />
</LinearLayout>"""
            is LayerPayload.WeatherCard, is LayerPayload.WeatherLocation, is LayerPayload.WeatherForecast ->
                """<LinearLayout orientation="vertical" padding="12" gradientStart="#331E293B" gradientEnd="#330F172A" cornerRadius="$r" borderColor="$accent" borderWidth="1">
  <Row gravity="center" spacing="8">
    <Icon name="location" color="$accent" size="16" />
    <Text text="{{weather_city}}" textSize="13" textColor="$accent" bold="true" />
  </Row>
  <Row gravity="center" spacing="8">
    <Icon name="sun" color="#FFFBBF24" size="22" />
    <Text text="{{weather_temp}}" textSize="24" textColor="$fg" bold="true" fontFamily="Space Grotesk" />
    <Text text="{{weather_cond}} · {{humidity}}" textSize="12" textColor="#CCFFFFFF" />
  </Row>
</LinearLayout>"""
            is LayerPayload.BatteryInfo, is LayerPayload.ProgressRing, is LayerPayload.AudioVolumeBar ->
                """<LinearLayout orientation="vertical" padding="10" background="$bg" cornerRadius="$r">
  <Row gravity="center" spacing="6">
    <Icon name="bolt" color="$accent" size="16" />
    <Text text="Battery {{battery}}% · Vol {{volume}}" textSize="13" textColor="$fg" bold="true" fontFamily="JetBrains Mono" />
  </Row>
  <ProgressBar progress="{{battery}}" max="100" indicatorColor="$accent" trackColor="#33FFFFFF" height="6" />
</LinearLayout>"""
            is LayerPayload.MusicPlayer ->
                """<LinearLayout orientation="horizontal" gravity="center" padding="10" background="$bg" cornerRadius="$r" borderColor="$accent" borderWidth="1" spacing="8">
  <Icon name="music" color="$accent" size="22" />
  <Column spacing="2">
    <Text text="Now Playing" textSize="14" textColor="$fg" bold="true" />
    <Text text="Volume {{volume}} · {{time}}" textSize="11" textColor="#BBFFFFFF" />
  </Column>
</LinearLayout>"""
            else ->
                """<LinearLayout orientation="vertical" padding="12" background="$bg" cornerRadius="$r" borderColor="$accent" borderWidth="1">
  <Text text="${layer.name}" textSize="16" textColor="$fg" bold="true" fontFamily="$font" />
  <Divider color="#33FFFFFF" thickness="1" />
  <Text text="{{greeting}} · {{weather_temp}} · {{battery}}%" textSize="12" textColor="$accent" />
</LinearLayout>"""
        }
        val snippet = LayerPayload.CustomXmlSnippet(rawMarkup = snippetXml, isJsonFormat = false)
        updateLayer(
            layer.copy(
                type = LayerType.CUSTOM_XML_SNIPPET,
                payload = snippet
            )
        )
        return snippet
    }

    fun bringLayerForward(layerId: String) {
        pushUndoSnapshot()
        val ordered = _uiState.value.layers.sortedBy { it.style.zIndex }.toMutableList()
        val idx = ordered.indexOfFirst { it.id == layerId }
        if (idx in 0 until ordered.lastIndex) {
            val item = ordered.removeAt(idx)
            ordered.add(idx + 1, item)
            val reindexed = ordered.mapIndexed { i, l -> l.copy(style = l.style.copy(zIndex = i)) }
            _uiState.update { it.copy(layers = reindexed) }
        }
    }

    fun sendLayerBackward(layerId: String) {
        pushUndoSnapshot()
        val ordered = _uiState.value.layers.sortedBy { it.style.zIndex }.toMutableList()
        val idx = ordered.indexOfFirst { it.id == layerId }
        if (idx > 0) {
            val item = ordered.removeAt(idx)
            ordered.add(idx - 1, item)
            val reindexed = ordered.mapIndexed { i, l -> l.copy(style = l.style.copy(zIndex = i)) }
            _uiState.update { it.copy(layers = reindexed) }
        }
    }

    fun bringLayerToFront(layerId: String) {
        pushUndoSnapshot()
        val ordered = _uiState.value.layers.sortedBy { it.style.zIndex }.toMutableList()
        val idx = ordered.indexOfFirst { it.id == layerId }
        if (idx in 0 until ordered.lastIndex) {
            val item = ordered.removeAt(idx)
            ordered.add(item)
            val reindexed = ordered.mapIndexed { i, l -> l.copy(style = l.style.copy(zIndex = i)) }
            _uiState.update { it.copy(layers = reindexed, statusMessage = "Brought \"${item.name}\" to front") }
        }
    }

    fun sendLayerToBack(layerId: String) {
        pushUndoSnapshot()
        val ordered = _uiState.value.layers.sortedBy { it.style.zIndex }.toMutableList()
        val idx = ordered.indexOfFirst { it.id == layerId }
        if (idx > 0) {
            val item = ordered.removeAt(idx)
            ordered.add(0, item)
            val reindexed = ordered.mapIndexed { i, l -> l.copy(style = l.style.copy(zIndex = i)) }
            _uiState.update { it.copy(layers = reindexed, statusMessage = "Sent \"${item.name}\" to back") }
        }
    }

    fun groupSelectedLayers(
        asStackGroup: Boolean = false,
        orientation: StackOrientation = StackOrientation.VERTICAL
    ) {
        val state = _uiState.value
        val selectedIds = state.selectedLayerIds
        if (selectedIds.size < 2) {
            _uiState.update { it.copy(statusMessage = "Select at least 2 layers using Multi-Select to group them.") }
            return
        }
        val toGroup = state.layers.filter { it.id in selectedIds }
            .let { list ->
                if (asStackGroup) {
                    if (orientation == StackOrientation.HORIZONTAL) {
                        list.sortedBy { it.style.xDp }
                    } else {
                        list.sortedBy { it.style.yDp }
                    }
                } else {
                    list.sortedBy { it.style.zIndex }
                }
            }
        if (toGroup.size < 2) return
        pushUndoSnapshot()

        val minX = toGroup.minOf { it.style.xDp }
        val minY = toGroup.minOf { it.style.yDp }
        val maxX = toGroup.maxOf { it.style.xDp + it.style.widthDp }
        val maxY = toGroup.maxOf { it.style.yDp + it.style.heightDp }
        val groupId = UUID.randomUUID().toString()
        val maxZ = state.layers.maxOfOrNull { it.style.zIndex } ?: 0

        val groupLayer = if (asStackGroup) {
            val spacing = 6f
            val stackChildren = toGroup.mapIndexed { idx, child ->
                child.copy(
                    projectId = groupId,
                    style = child.style.copy(
                        xDp = 0f,
                        yDp = 0f,
                        zIndex = idx
                    )
                )
            }
            val groupW = max(80f, (toGroup.maxOfOrNull { it.style.widthDp } ?: 120f) + 12f)
                .coerceAtMost(state.canvasWidthDp)
            val totalH = stackChildren.sumOf { it.style.heightDp.toDouble() }.toFloat() +
                spacing * (stackChildren.size - 1).coerceAtLeast(0) + 12f
            val groupH = totalH.coerceIn(48f, state.canvasHeightDp)
            WidgetLayer(
                id = groupId,
                projectId = targetId,
                name = "Stack Group (${stackChildren.size})",
                type = LayerType.STACK_GROUP,
                style = LayerStyle(
                    xDp = minX,
                    yDp = minY,
                    widthDp = groupW,
                    heightDp = groupH,
                    paddingDp = 6f,
                    zIndex = maxZ + 1
                ),
                payload = LayerPayload.StackGroup(
                    orientation = orientation,
                    spacingDp = spacing,
                    embeddedLayers = stackChildren
                )
            )
        } else {
            val relativeChildren = toGroup.mapIndexed { idx, child ->
                child.copy(
                    projectId = groupId,
                    style = child.style.copy(
                        xDp = (child.style.xDp - minX).coerceAtLeast(0f),
                        yDp = (child.style.yDp - minY).coerceAtLeast(0f),
                        zIndex = idx
                    )
                )
            }
            WidgetLayer(
                id = groupId,
                projectId = targetId,
                name = "Overlap Group (${relativeChildren.size})",
                type = LayerType.OVERLAP_GROUP,
                style = LayerStyle(
                    xDp = minX,
                    yDp = minY,
                    widthDp = max(40f, maxX - minX),
                    heightDp = max(32f, maxY - minY),
                    paddingDp = 0f,
                    zIndex = maxZ + 1
                ),
                payload = LayerPayload.OverlapGroup(
                    embeddedLayers = relativeChildren
                )
            )
        }

        val remaining = state.layers.filterNot { it.id in selectedIds } + groupLayer
        _uiState.update {
            it.copy(
                layers = remaining,
                selectedLayerIds = setOf(groupId),
                isMultiSelectMode = false,
                statusMessage = "Grouped ${toGroup.size} elements into ${if (asStackGroup) "Stack" else "Overlap"} Group."
            )
        }
    }

    fun ungroupSelectedLayer(layerId: String) {
        val state = _uiState.value
        val target = state.layers.firstOrNull { it.id == layerId } ?: return
        val embedded = when (val p = target.payload) {
            is LayerPayload.CustomComponentRef -> p.embeddedLayers
            is LayerPayload.OverlapGroup -> p.embeddedLayers
            is LayerPayload.StackGroup -> p.embeddedLayers
            else -> emptyList()
        }
        if (embedded.isEmpty()) return
        pushUndoSnapshot()

        val baseZ = target.style.zIndex
        val restored = if (target.payload is LayerPayload.StackGroup) {
            var cursorX = 0f
            var cursorY = 0f
            embedded.mapIndexed { idx, child ->
                val rx = if (target.payload.orientation == StackOrientation.HORIZONTAL) cursorX else 0f
                val ry = if (target.payload.orientation == StackOrientation.VERTICAL) cursorY else 0f
                if (target.payload.orientation == StackOrientation.HORIZONTAL) {
                    cursorX += child.style.widthDp + target.payload.spacingDp
                } else {
                    cursorY += child.style.heightDp + target.payload.spacingDp
                }
                child.copy(
                    id = UUID.randomUUID().toString(),
                    projectId = targetId,
                    style = child.style.copy(
                        xDp = (target.style.xDp + rx).coerceIn(0f, max(0f, state.canvasWidthDp - 24f)),
                        yDp = (target.style.yDp + ry).coerceIn(0f, max(0f, state.canvasHeightDp - 20f)),
                        zIndex = baseZ + idx
                    )
                )
            }
        } else {
            embedded.mapIndexed { idx, child ->
                child.copy(
                    id = UUID.randomUUID().toString(),
                    projectId = targetId,
                    style = child.style.copy(
                        xDp = (target.style.xDp + child.style.xDp).coerceIn(0f, max(0f, state.canvasWidthDp - 24f)),
                        yDp = (target.style.yDp + child.style.yDp).coerceIn(0f, max(0f, state.canvasHeightDp - 20f)),
                        zIndex = baseZ + idx
                    )
                )
            }
        }
        val remaining = state.layers.filterNot { it.id == layerId } + restored
        _uiState.update {
            it.copy(
                layers = remaining,
                selectedLayerIds = restored.map { r -> r.id }.toSet(),
                isMultiSelectMode = true,
                statusMessage = "Ungrouped into ${restored.size} elements."
            )
        }
    }

    fun addChildToGroup(groupId: String, childType: LayerType) {
        val group = _uiState.value.layers.firstOrNull { it.id == groupId } ?: return
        val existing = when (val p = group.payload) {
            is LayerPayload.OverlapGroup -> p.embeddedLayers
            is LayerPayload.StackGroup -> p.embeddedLayers
            else -> return
        }
        val (defStyle, defPayload, defAction) = createDefaultsForType(childType, existing.size)
        val isHorizontalStack = (group.payload as? LayerPayload.StackGroup)?.orientation == StackOrientation.HORIZONTAL
        val childW = when {
            isHorizontalStack -> min(defStyle.widthDp, 84f).coerceAtLeast(28f)
            group.payload is LayerPayload.StackGroup -> min(defStyle.widthDp, (group.style.widthDp - 16f).coerceAtLeast(60f))
            else -> min(defStyle.widthDp, group.style.widthDp.coerceAtLeast(44f))
        }
        val childH = min(defStyle.heightDp, group.style.heightDp.coerceAtLeast(28f))
        val offsetStep = if (group.payload is LayerPayload.OverlapGroup) (existing.size * 8f) % 36f else 0f
        val newChild = WidgetLayer(
            id = UUID.randomUUID().toString(),
            projectId = groupId,
            name = childType.displayName,
            type = childType,
            style = defStyle.copy(
                xDp = offsetStep,
                yDp = offsetStep,
                widthDp = childW,
                heightDp = childH,
                zIndex = existing.size
            ),
            payload = defPayload,
            tapAction = defAction
        )
        val updatedPayload = when (val p = group.payload) {
            is LayerPayload.OverlapGroup -> p.copy(embeddedLayers = existing + newChild)
            is LayerPayload.StackGroup -> p.copy(embeddedLayers = existing + newChild)
            else -> return
        }
        updateLayer(group.copy(payload = updatedPayload), recordUndo = true)
    }

    fun duplicateLayer(layerId: String) {
        val original = _uiState.value.layers.firstOrNull { it.id == layerId } ?: return
        pushUndoSnapshot()
        val newId = UUID.randomUUID().toString()
        val copy = original.copy(
            id = newId,
            name = "${original.name} Copy",
            style = original.style.copy(
                xDp = original.style.xDp + 12f,
                yDp = original.style.yDp + 12f,
                zIndex = (_uiState.value.layers.maxOfOrNull { it.style.zIndex } ?: 0) + 1
            )
        )
        _uiState.update { it.copy(layers = it.layers + copy, selectedLayerIds = setOf(newId)) }
    }

    fun deleteLayer(layerId: String) {
        pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(
                layers = state.layers.filterNot { it.id == layerId },
                selectedLayerIds = state.selectedLayerIds - layerId
            )
        }
    }

    fun importCustomFontForSelectedLayer(uri: Uri) {
        val layer = _uiState.value.primarySelectedLayer ?: return
        viewModelScope.launch {
            assetStorageManager.copyFontFromUri(uri)
                .onSuccess { (fontName, path) ->
                    updateLayer(
                        layer.copy(
                            style = layer.style.copy(
                                fontFamilyName = fontName,
                                fontAssetPath = path
                            )
                        )
                    )
                    _uiState.update { it.copy(statusMessage = "Imported font \"$fontName\".") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = err.localizedMessage ?: "Failed to import font.") }
                }
        }
    }

    fun importImageForSelectedLayer(uri: Uri) {
        viewModelScope.launch {
            assetStorageManager.copyImageFromUri(uri)
                .onSuccess { path ->
                    applyImageAssetPath(path, isAiGenerated = false, prompt = null)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(statusMessage = err.localizedMessage ?: "Failed to import image.") }
                }
        }
    }

    fun generateAiImage(prompt: String, aspectRatio: AspectRatio) {
        viewModelScope.launch {
            _uiState.update { it.copy(aiImageState = AiImageUiState.Loading) }
            aiImageRepository.generateImage(prompt, aspectRatio)
                .onSuccess { bitmap ->
                    _uiState.update { it.copy(aiImageState = AiImageUiState.Success(bitmap, prompt)) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            aiImageState = AiImageUiState.Error(
                                err.localizedMessage ?: "AI image generation failed."
                            )
                        )
                    }
                }
        }
    }

    fun editAiImage(sourceBitmap: Bitmap, instruction: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(aiImageState = AiImageUiState.Loading) }
            aiImageRepository.editImage(sourceBitmap, instruction)
                .onSuccess { bitmap ->
                    _uiState.update { it.copy(aiImageState = AiImageUiState.Success(bitmap, instruction)) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            aiImageState = AiImageUiState.Error(
                                err.localizedMessage ?: "AI image editing failed."
                            )
                        )
                    }
                }
        }
    }

    fun applyAiBitmapToCanvas(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            assetStorageManager.saveBitmapAsAsset(bitmap, prefix = "gemini_art")
                .onSuccess { path ->
                    applyImageAssetPath(path, isAiGenerated = true, prompt = prompt)
                    _uiState.update {
                        it.copy(
                            aiImageState = AiImageUiState.Idle,
                            statusMessage = "AI artwork applied to canvas."
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(statusMessage = err.localizedMessage ?: "Could not save AI image.")
                    }
                }
        }
    }

    private fun applyImageAssetPath(path: String, isAiGenerated: Boolean, prompt: String?) {
        val selected = _uiState.value.primarySelectedLayer
        if (selected != null && selected.payload is LayerPayload.ImageMedia) {
            updateLayer(
                selected.copy(
                    payload = selected.payload.copy(
                        assetPathOrUri = path,
                        isAiGenerated = isAiGenerated,
                        lastAiPrompt = prompt
                    )
                )
            )
        } else {
            pushUndoSnapshot()
            val state = _uiState.value
            val maxZ = state.layers.maxOfOrNull { it.style.zIndex } ?: 0
            val newId = UUID.randomUUID().toString()
            val layer = WidgetLayer(
                id = newId,
                projectId = targetId,
                name = if (isAiGenerated) "AI Artwork" else "Custom Image",
                type = LayerType.IMAGE_MEDIA,
                style = LayerStyle(
                    xDp = 12f,
                    yDp = 12f,
                    widthDp = min(140f, state.canvasWidthDp - 24f),
                    heightDp = min(110f, state.canvasHeightDp - 24f),
                    paddingDp = 0f,
                    cornerRadiusDp = 16f,
                    zIndex = maxZ + 1
                ),
                payload = LayerPayload.ImageMedia(
                    assetPathOrUri = path,
                    isAiGenerated = isAiGenerated,
                    lastAiPrompt = prompt
                )
            )
            _uiState.update { it.copy(layers = it.layers + layer, selectedLayerIds = setOf(newId)) }
        }
    }

    fun saveSelectedAsCustomComponent(name: String, category: String) {
        val state = _uiState.value
        val selectedLayers = if (state.selectedLayerIds.isNotEmpty()) {
            state.layers.filter { it.id in state.selectedLayerIds }
        } else {
            state.layers
        }
        if (selectedLayers.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Select at least one layer to save as a component.") }
            return
        }

        viewModelScope.launch {
            val minX = selectedLayers.minOf { it.style.xDp }
            val minY = selectedLayers.minOf { it.style.yDp }
            val maxX = selectedLayers.maxOf { it.style.xDp + it.style.widthDp }
            val maxY = selectedLayers.maxOf { it.style.yDp + it.style.heightDp }
            val compId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val normalizedLayers = selectedLayers.map { layer ->
                layer.copy(
                    id = UUID.randomUUID().toString(),
                    projectId = compId,
                    style = layer.style.copy(
                        xDp = (layer.style.xDp - minX).coerceAtLeast(0f),
                        yDp = (layer.style.yDp - minY).coerceAtLeast(0f)
                    )
                )
            }

            val component = CustomComponent(
                id = compId,
                name = name.trim().ifBlank { "Custom Module" },
                category = category.trim().ifBlank { "General" },
                widthDp = max(60f, maxX - minX),
                heightDp = max(40f, maxY - minY),
                layers = normalizedLayers,
                createdAt = now,
                updatedAt = now
            )
            componentRepository.saveComponent(component)
            _uiState.update {
                it.copy(
                    isMultiSelectMode = false,
                    statusMessage = "Saved \"${component.name}\" to My Custom Components!"
                )
            }
        }
    }

    fun saveAndApply(pinToHomeScreen: Boolean) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            if (isComponentMode) {
                val base = loadedComponent ?: CustomComponent(id = targetId, name = state.title)
                val updated = base.copy(
                    name = state.title.trim().ifBlank { "Custom Module" },
                    category = state.componentCategory,
                    widthDp = state.canvasWidthDp,
                    heightDp = state.canvasHeightDp,
                    layers = state.layers.map { it.copy(projectId = targetId) },
                    updatedAt = now
                )
                componentRepository.saveComponent(updated)
                loadedComponent = updated
                _uiState.update {
                    it.copy(isSaving = false, statusMessage = "Custom component saved.")
                }
            } else {
                val base = loadedProject ?: WidgetProject(id = targetId, name = state.title)
                val updated = base.copy(
                    name = state.title.trim().ifBlank { "Untitled Widget" },
                    dimension = state.dimension,
                    customWidthDp = state.canvasWidthDp,
                    customHeightDp = state.canvasHeightDp,
                    gridSnapDp = state.gridSnapDp,
                    backgroundColorHex = state.backgroundColorHex,
                    backgroundGradient = state.backgroundGradient,
                    backgroundCornerRadiusDp = state.backgroundCornerRadiusDp,
                    layers = state.layers.map { it.copy(projectId = targetId) },
                    updatedAt = now
                )
                widgetRepository.saveProject(updated)
                loadedProject = updated
                val msg = if (pinToHomeScreen) {
                    widgetApplyBridge.syncAndPinWidget(targetId)
                        .getOrElse { it.localizedMessage ?: "Saved widget." }
                } else {
                    widgetApplyBridge.refreshWidgetsForProject(targetId)
                    "Widget saved to studio."
                }
                _uiState.update { it.copy(isSaving = false, statusMessage = msg) }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun snapValue(value: Float, step: Float): Float {
        if (step <= 1f) return value
        return (value / step).roundToInt() * step
    }

    private fun createDefaultsForType(type: LayerType, zIndex: Int): Triple<LayerStyle, LayerPayload, TapAction> {
        val pair: Pair<LayerStyle, LayerPayload> = when (type) {
            LayerType.DIGITAL_CLOCK -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 148f, heightDp = 50f,
                fontFamilyName = "Space Grotesk", fontSizeSp = 32f
            ) to LayerPayload.DigitalClock()

            LayerType.ANALOG_CLOCK -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 96f, heightDp = 96f, paddingDp = 6f
            ) to LayerPayload.AnalogClock()

            LayerType.DATE_TEXT -> LayerStyle(
                xDp = 16f, yDp = 68f, widthDp = 160f, heightDp = 28f,
                fontSizeSp = 14f, foregroundColorHex = "#FF5CE1E6"
            ) to LayerPayload.DateText()

            LayerType.DAY_OF_WEEK -> LayerStyle(
                xDp = 16f, yDp = 48f, widthDp = 130f, heightDp = 28f,
                fontSizeSp = 13f, foregroundColorHex = "#FFFF8A7A"
            ) to LayerPayload.DayOfWeek()

            LayerType.CUSTOM_TEXT -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 140f, heightDp = 36f, fontSizeSp = 16f
            ) to LayerPayload.CustomText()

            LayerType.BATTERY_INFO -> LayerStyle(
                xDp = 16f, yDp = 92f, widthDp = 136f, heightDp = 44f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 14f, fontSizeSp = 14f
            ) to LayerPayload.BatteryInfo()

            LayerType.SYSTEM_RAM_STORAGE -> LayerStyle(
                xDp = 16f, yDp = 92f, widthDp = 156f, heightDp = 44f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 14f, fontSizeSp = 12f
            ) to LayerPayload.SystemRamStorage()

            LayerType.NETWORK_STATUS -> LayerStyle(
                xDp = 180f, yDp = 16f, widthDp = 112f, heightDp = 34f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 14f, fontSizeSp = 13f
            ) to LayerPayload.NetworkStatus()

            LayerType.NEXT_ALARM -> LayerStyle(
                xDp = 180f, yDp = 56f, widthDp = 120f, heightDp = 34f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 14f, fontSizeSp = 13f
            ) to LayerPayload.NextAlarm()

            LayerType.WEATHER_CARD -> LayerStyle(
                xDp = 168f, yDp = 16f, widthDp = 136f, heightDp = 64f,
                backgroundColorHex = "#2438BDF8", cornerRadiusDp = 18f, fontSizeSp = 20f
            ) to LayerPayload.WeatherCard()

            LayerType.WEATHER_LOCATION -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 240f, heightDp = 84f,
                backgroundColorHex = "#281E293B", cornerRadiusDp = 20f, borderWidthDp = 1f, fontSizeSp = 22f
            ) to LayerPayload.WeatherLocation()

            LayerType.WEATHER_FORECAST -> LayerStyle(
                xDp = 16f, yDp = 84f, widthDp = 280f, heightDp = 64f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 16f, fontSizeSp = 12f
            ) to LayerPayload.WeatherForecast()

            LayerType.LOCATION_BADGE -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 156f, heightDp = 36f,
                backgroundColorHex = "#265CE1E6", cornerRadiusDp = 18f, fontSizeSp = 13f
            ) to LayerPayload.LocationBadge()

            LayerType.CALENDAR_MINI -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 152f, heightDp = 128f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 18f, fontSizeSp = 11f
            ) to LayerPayload.CalendarView(mode = CalendarMode.MINI_MONTH)

            LayerType.CALENDAR_AGENDA -> LayerStyle(
                xDp = 16f, yDp = 92f, widthDp = 220f, heightDp = 58f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 16f, fontSizeSp = 12f
            ) to LayerPayload.CalendarView(mode = CalendarMode.AGENDA_LIST)

            LayerType.COUNTDOWN_TIMER -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 148f, heightDp = 60f,
                backgroundColorHex = "#26FF8A7A", cornerRadiusDp = 18f, fontSizeSp = 18f
            ) to LayerPayload.CountdownTimer()

            LayerType.SHAPE_CONTAINER -> LayerStyle(
                xDp = 12f, yDp = 12f, widthDp = 104f, heightDp = 76f,
                backgroundColorHex = "#00000000", cornerRadiusDp = 16f, borderWidthDp = 0f, paddingDp = 0f,
                foregroundColorHex = "#FF5CE1E6"
            ) to LayerPayload.ShapeContainer(shapeKind = ShapeKind.RECTANGLE)

            LayerType.GLASS_CARD -> LayerStyle(
                xDp = 12f, yDp = 12f, widthDp = 156f, heightDp = 88f,
                backgroundColorHex = "#00000000", glassBlurEnabled = true, glassTintAlpha = 0.22f,
                cornerRadiusDp = 22f, borderWidthDp = 1f, borderColorHex = "#55FFFFFF"
            ) to LayerPayload.ShapeContainer(shapeKind = ShapeKind.GLASS_CARD)

            LayerType.VECTOR_ICON -> LayerStyle(
                xDp = 20f, yDp = 20f, widthDp = 44f, heightDp = 44f, paddingDp = 6f,
                foregroundColorHex = "#FF5CE1E6"
            ) to LayerPayload.VectorIcon()

            LayerType.IMAGE_MEDIA -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 120f, heightDp = 100f,
                paddingDp = 0f, cornerRadiusDp = 16f
            ) to LayerPayload.ImageMedia()

            LayerType.MUSIC_PLAYER -> LayerStyle(
                xDp = 16f, yDp = 92f, widthDp = 240f, heightDp = 54f,
                backgroundColorHex = "#28FFFFFF", cornerRadiusDp = 18f, fontSizeSp = 13f
            ) to LayerPayload.MusicPlayer()

            LayerType.QUICK_TOGGLE_BAR -> LayerStyle(
                xDp = 16f, yDp = 96f, widthDp = 224f, heightDp = 48f,
                backgroundColorHex = "#24FFFFFF", cornerRadiusDp = 22f, fontSizeSp = 12f
            ) to LayerPayload.QuickToggleBar()

            LayerType.TOGGLE_FLASHLIGHT -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 112f, heightDp = 46f,
                backgroundColorHex = "#26FBBF24", cornerRadiusDp = 20f, borderWidthDp = 1f, fontSizeSp = 13f
            ) to LayerPayload.ToggleButton(kind = ToggleButtonKind.FLASHLIGHT)

            LayerType.TOGGLE_WIFI -> LayerStyle(
                xDp = 16f, yDp = 68f, widthDp = 104f, heightDp = 46f,
                backgroundColorHex = "#2638BDF8", cornerRadiusDp = 20f, borderWidthDp = 1f, fontSizeSp = 13f
            ) to LayerPayload.ToggleButton(kind = ToggleButtonKind.WIFI)

            LayerType.TOGGLE_BLUETOOTH -> LayerStyle(
                xDp = 128f, yDp = 16f, widthDp = 112f, heightDp = 46f,
                backgroundColorHex = "#264F8EF7", cornerRadiusDp = 20f, borderWidthDp = 1f, fontSizeSp = 13f
            ) to LayerPayload.ToggleButton(kind = ToggleButtonKind.BLUETOOTH)

            LayerType.TOGGLE_SILENT_VIBRATE -> LayerStyle(
                xDp = 128f, yDp = 68f, widthDp = 120f, heightDp = 46f,
                backgroundColorHex = "#26FF8A7A", cornerRadiusDp = 20f, borderWidthDp = 1f, fontSizeSp = 13f
            ) to LayerPayload.ToggleButton(kind = ToggleButtonKind.SILENT_VIBRATE)

            LayerType.TOGGLE_MUTE -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 96f, heightDp = 46f,
                backgroundColorHex = "#268B5CF6", cornerRadiusDp = 20f, borderWidthDp = 1f, fontSizeSp = 13f
            ) to LayerPayload.ToggleButton(kind = ToggleButtonKind.MUTE)

            LayerType.TOGGLE_PLAY_PAUSE -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 96f, heightDp = 46f,
                backgroundColorHex = "#265CE1E6", cornerRadiusDp = 20f, borderWidthDp = 1f, fontSizeSp = 13f
            ) to LayerPayload.ToggleButton(kind = ToggleButtonKind.PLAY_PAUSE)

            LayerType.TOGGLE_REFRESH -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 96f, heightDp = 46f,
                backgroundColorHex = "#2634D399", cornerRadiusDp = 20f, borderWidthDp = 1f, fontSizeSp = 13f
            ) to LayerPayload.ToggleButton(kind = ToggleButtonKind.REFRESH)

            LayerType.AUDIO_VOLUME_BAR -> LayerStyle(
                xDp = 16f, yDp = 96f, widthDp = 156f, heightDp = 48f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 16f, fontSizeSp = 14f
            ) to LayerPayload.AudioVolumeBar()

            LayerType.GREETING_BANNER -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 220f, heightDp = 58f,
                fontFamilyName = "Space Grotesk", fontSizeSp = 19f
            ) to LayerPayload.GreetingBanner()

            LayerType.WORLD_CLOCK -> LayerStyle(
                xDp = 168f, yDp = 16f, widthDp = 128f, heightDp = 56f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 16f, fontFamilyName = "Space Grotesk", fontSizeSp = 17f
            ) to LayerPayload.WorldClock()

            LayerType.PROGRESS_RING -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 96f, heightDp = 96f,
                backgroundColorHex = "#1EFFFFFF", cornerRadiusDp = 24f, fontSizeSp = 15f
            ) to LayerPayload.ProgressRing(style = ProgressBarStyle.CIRCULAR_GAUGE)

            LayerType.PROGRESS_BAR -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 192f, heightDp = 52f,
                backgroundColorHex = "#1EFFFFFF", cornerRadiusDp = 16f, fontSizeSp = 14f
            ) to LayerPayload.ProgressRing(style = ProgressBarStyle.LINEAR_BAR, strokeWidthDp = 7f)

            LayerType.OVERLAP_GROUP -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 180f, heightDp = 96f,
                backgroundColorHex = "#00000000", cornerRadiusDp = 20f, borderWidthDp = 0f, paddingDp = 0f
            ) to LayerPayload.OverlapGroup(
                embeddedLayers = listOf(
                    WidgetLayer(
                        id = UUID.randomUUID().toString(),
                        projectId = targetId,
                        name = "Backdrop Shape",
                        type = LayerType.SHAPE_CONTAINER,
                        style = LayerStyle(
                            xDp = 0f, yDp = 0f, widthDp = 180f, heightDp = 96f,
                            backgroundColorHex = "#00000000", borderWidthDp = 0f,
                            paddingDp = 0f, cornerRadiusDp = 20f, zIndex = 0
                        ),
                        payload = LayerPayload.ShapeContainer(shapeKind = ShapeKind.GLASS_CARD)
                    ),
                    WidgetLayer(
                        id = UUID.randomUUID().toString(),
                        projectId = targetId,
                        name = "Digital Clock",
                        type = LayerType.DIGITAL_CLOCK,
                        style = LayerStyle(
                            xDp = 14f, yDp = 14f, widthDp = 152f, heightDp = 42f,
                            backgroundColorHex = "#00000000", cornerRadiusDp = 0f,
                            fontFamilyName = "Space Grotesk", fontSizeSp = 28f, zIndex = 1
                        ),
                        payload = LayerPayload.DigitalClock()
                    ),
                    WidgetLayer(
                        id = UUID.randomUUID().toString(),
                        projectId = targetId,
                        name = "Date",
                        type = LayerType.DATE_TEXT,
                        style = LayerStyle(
                            xDp = 14f, yDp = 58f, widthDp = 152f, heightDp = 24f,
                            backgroundColorHex = "#00000000", cornerRadiusDp = 0f,
                            fontSizeSp = 12f, foregroundColorHex = "#FF5CE1E6", zIndex = 2
                        ),
                        payload = LayerPayload.DateText()
                    )
                )
            )

            LayerType.STACK_GROUP -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 196f, heightDp = 92f,
                backgroundColorHex = "#22FFFFFF", cornerRadiusDp = 20f, borderWidthDp = 1f, paddingDp = 8f
            ) to LayerPayload.StackGroup(
                orientation = StackOrientation.VERTICAL,
                spacingDp = 4f,
                embeddedLayers = listOf(
                    WidgetLayer(
                        id = UUID.randomUUID().toString(),
                        projectId = targetId,
                        name = "Clock",
                        type = LayerType.DIGITAL_CLOCK,
                        style = LayerStyle(
                            xDp = 0f, yDp = 0f, widthDp = 86f, heightDp = 36f,
                            backgroundColorHex = "#00000000", cornerRadiusDp = 0f,
                            paddingDp = 2f, fontFamilyName = "Space Grotesk", fontSizeSp = 24f, zIndex = 0
                        ),
                        payload = LayerPayload.DigitalClock()
                    ),
                    WidgetLayer(
                        id = UUID.randomUUID().toString(),
                        projectId = targetId,
                        name = "Date",
                        type = LayerType.DATE_TEXT,
                        style = LayerStyle(
                            xDp = 0f, yDp = 0f, widthDp = 86f, heightDp = 24f,
                            backgroundColorHex = "#00000000", cornerRadiusDp = 0f,
                            paddingDp = 2f, fontSizeSp = 12f, foregroundColorHex = "#FF5CE1E6", zIndex = 1
                        ),
                        payload = LayerPayload.DateText()
                    )
                )
            )

            LayerType.QUICK_NOTE -> LayerStyle(
                xDp = 16f, yDp = 72f, widthDp = 200f, heightDp = 64f,
                backgroundColorHex = "#26FBBF24", cornerRadiusDp = 16f, fontSizeSp = 13f
            ) to LayerPayload.QuickNote()

            LayerType.CUSTOM_COMPONENT_REF -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 150f, heightDp = 70f, paddingDp = 0f
            ) to LayerPayload.CustomComponentRef(componentId = "", componentName = "Module")

            LayerType.CUSTOM_XML_SNIPPET -> LayerStyle(
                xDp = 16f, yDp = 16f, widthDp = 200f, heightDp = 96f, paddingDp = 0f
            ) to LayerPayload.CustomXmlSnippet()
        }
        val defaultAction = when (type) {
            LayerType.TOGGLE_FLASHLIGHT -> TapAction.ToggleFlashlight
            LayerType.TOGGLE_WIFI -> TapAction.OpenWifiSettings
            LayerType.TOGGLE_BLUETOOTH -> TapAction.OpenBluetoothSettings
            LayerType.TOGGLE_SILENT_VIBRATE -> TapAction.ToggleSilentVibrate
            LayerType.TOGGLE_MUTE -> TapAction.ToggleMute
            LayerType.TOGGLE_PLAY_PAUSE -> TapAction.MediaPlayPause
            LayerType.TOGGLE_REFRESH -> TapAction.RefreshWidget
            else -> TapAction.None
        }
        return Triple(pair.first.copy(zIndex = zIndex), pair.second, defaultAction)
    }

    companion object {
        fun provideFactory(
            targetId: String,
            isComponentMode: Boolean,
            widgetRepository: WidgetRepository,
            componentRepository: CustomComponentRepository,
            assetStorageManager: AssetStorageManager,
            telemetryRepository: SystemTelemetryRepository,
            weatherRepository: WeatherRepository,
            studioActionsRepository: StudioActionsRepository,
            aiImageRepository: AiImageRepository,
            widgetApplyBridge: WidgetApplyBridge
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditorViewModel(
                    targetId = targetId,
                    isComponentMode = isComponentMode,
                    widgetRepository = widgetRepository,
                    componentRepository = componentRepository,
                    assetStorageManager = assetStorageManager,
                    telemetryRepository = telemetryRepository,
                    weatherRepository = weatherRepository,
                    studioActionsRepository = studioActionsRepository,
                    aiImageRepository = aiImageRepository,
                    widgetApplyBridge = widgetApplyBridge
                )
            }
        }
    }
}
