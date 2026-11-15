package com.example

import android.content.Context
import com.example.data.backup.AssetStorageManager
import com.example.data.backup.WBuilderArchiveManager
import com.example.data.local.WBuilderDatabase
import com.example.data.local.prefs.StudioPreferencesDataStore
import com.example.data.repository.AiImageRepository
import com.example.data.repository.CustomComponentRepository
import com.example.data.repository.StudioActionsRepository
import com.example.data.repository.SystemTelemetryRepository
import com.example.data.repository.WeatherRepository
import com.example.data.repository.WidgetRepository
import com.example.widget.GlanceWidgetApplyBridge

interface WidgetApplyBridge {
    suspend fun syncAndPinWidget(projectId: String): Result<String>
    suspend fun refreshWidgetsForProject(projectId: String)
}

class WBuilderAppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: WBuilderDatabase by lazy {
        WBuilderDatabase.getInstance(appContext)
    }

    val preferencesDataStore: StudioPreferencesDataStore by lazy {
        StudioPreferencesDataStore(appContext)
    }

    val assetStorageManager: AssetStorageManager by lazy {
        AssetStorageManager(appContext)
    }

    val widgetRepository: WidgetRepository by lazy {
        WidgetRepository(database)
    }

    val customComponentRepository: CustomComponentRepository by lazy {
        CustomComponentRepository(database)
    }

    val archiveManager: WBuilderArchiveManager by lazy {
        WBuilderArchiveManager(
            context = appContext,
            widgetRepository = widgetRepository,
            componentRepository = customComponentRepository,
            assetStorage = assetStorageManager
        )
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(appContext)
    }

    val telemetryRepository: SystemTelemetryRepository by lazy {
        SystemTelemetryRepository(appContext)
    }

    val studioActionsRepository: StudioActionsRepository by lazy {
        StudioActionsRepository(appContext)
    }

    val aiImageRepository: AiImageRepository by lazy {
        AiImageRepository()
    }

    val widgetApplyBridge: WidgetApplyBridge by lazy {
        GlanceWidgetApplyBridge(appContext, this)
    }
}
