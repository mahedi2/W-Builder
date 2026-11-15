package com.example.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "wbuilder_prefs")

data class StudioPreferences(
    val defaultGridSnapDp: Float = 8f,
    val showCanvasGrid: Boolean = true,
    val weatherUseCelsius: Boolean = true,
    val fallbackCityName: String = "San Francisco",
    val fallbackLat: Double = 37.7749,
    val fallbackLon: Double = -122.4194,
    val lastActiveProjectId: String? = null,
    val widgetRefreshRevision: Long = 0L
)

class StudioPreferencesDataStore(private val context: Context) {

    private object Keys {
        val DEFAULT_GRID_SNAP_DP = floatPreferencesKey("default_grid_snap_dp")
        val SHOW_CANVAS_GRID = booleanPreferencesKey("show_canvas_grid")
        val WEATHER_USE_CELSIUS = booleanPreferencesKey("weather_use_celsius")
        val FALLBACK_CITY_NAME = stringPreferencesKey("fallback_city_name")
        val FALLBACK_LAT = doublePreferencesKey("fallback_lat")
        val FALLBACK_LON = doublePreferencesKey("fallback_lon")
        val LAST_ACTIVE_PROJECT_ID = stringPreferencesKey("last_active_project_id")
        val WIDGET_REFRESH_REVISION = longPreferencesKey("widget_refresh_revision")
    }

    val preferences: Flow<StudioPreferences> = context.studioDataStore.data.map { prefs ->
        StudioPreferences(
            defaultGridSnapDp = prefs[Keys.DEFAULT_GRID_SNAP_DP] ?: 8f,
            showCanvasGrid = prefs[Keys.SHOW_CANVAS_GRID] ?: true,
            weatherUseCelsius = prefs[Keys.WEATHER_USE_CELSIUS] ?: true,
            fallbackCityName = prefs[Keys.FALLBACK_CITY_NAME] ?: "San Francisco",
            fallbackLat = prefs[Keys.FALLBACK_LAT] ?: 37.7749,
            fallbackLon = prefs[Keys.FALLBACK_LON] ?: -122.4194,
            lastActiveProjectId = prefs[Keys.LAST_ACTIVE_PROJECT_ID],
            widgetRefreshRevision = prefs[Keys.WIDGET_REFRESH_REVISION] ?: 0L
        )
    }

    suspend fun setShowCanvasGrid(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.SHOW_CANVAS_GRID] = enabled }
    }

    suspend fun setDefaultGridSnapDp(snapDp: Float) {
        context.studioDataStore.edit { it[Keys.DEFAULT_GRID_SNAP_DP] = snapDp }
    }

    suspend fun setWeatherUnit(useCelsius: Boolean) {
        context.studioDataStore.edit { it[Keys.WEATHER_USE_CELSIUS] = useCelsius }
    }

    suspend fun setFallbackLocation(cityName: String, lat: Double, lon: Double) {
        context.studioDataStore.edit {
            it[Keys.FALLBACK_CITY_NAME] = cityName
            it[Keys.FALLBACK_LAT] = lat
            it[Keys.FALLBACK_LON] = lon
        }
    }

    suspend fun setLastActiveProjectId(projectId: String?) {
        context.studioDataStore.edit {
            if (projectId == null) {
                it.remove(Keys.LAST_ACTIVE_PROJECT_ID)
            } else {
                it[Keys.LAST_ACTIVE_PROJECT_ID] = projectId
            }
        }
    }

    suspend fun bumpWidgetRefreshRevision(): Long {
        var newRevision = 0L
        context.studioDataStore.edit { prefs ->
            newRevision = (prefs[Keys.WIDGET_REFRESH_REVISION] ?: 0L) + 1L
            prefs[Keys.WIDGET_REFRESH_REVISION] = newRevision
        }
        return newRevision
    }
}
