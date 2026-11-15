package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.example.R
import com.example.WBuilderAppContainer
import com.example.WidgetApplyBridge
import com.example.domain.model.WidgetDimension
import com.example.domain.model.WidgetProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class PinSuccessReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val projectId = intent.getStringExtra(EXTRA_PROJECT_ID) ?: return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val container = WBuilderAppContainer(appContext)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.widgetRepository.bindAppWidget(appWidgetId, projectId)
                val glanceId = GlanceAppWidgetManager(appContext).getGlanceIdBy(appWidgetId)
                WBuilderGlanceWidget().update(appContext, glanceId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PROJECT_ID = "extra_project_id"
    }
}

class GlanceWidgetApplyBridge(
    private val context: Context,
    private val container: WBuilderAppContainer
) : WidgetApplyBridge {

    override suspend fun syncAndPinWidget(projectId: String): Result<String> = runCatching {
        container.preferencesDataStore.setLastActiveProjectId(projectId)
        container.preferencesDataStore.bumpWidgetRefreshRevision()
        refreshWidgetsForProject(projectId)
        WidgetUpdateWorker.schedulePeriodicSync(context)

        val project = container.widgetRepository.getProjectOnce(projectId)
        val (receiverClass, dimDisplay) = resolveReceiverForProject(project)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
            val provider = ComponentName(context, receiverClass)
            val callbackIntent = Intent(context, PinSuccessReceiver::class.java).apply {
                putExtra(PinSuccessReceiver.EXTRA_PROJECT_ID, projectId)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val successCallback = PendingIntent.getBroadcast(
                context,
                (projectId + dimDisplay).hashCode(),
                callbackIntent,
                flags
            )
            val previewExtras = buildPinPreviewBundle(project)
            val requested = appWidgetManager.requestPinAppWidget(provider, previewExtras, successCallback)
            if (requested) {
                "Saved ($dimDisplay)! Confirm the Home Screen pin prompt."
            } else {
                "Saved ($dimDisplay)! Add W builder from your launcher's widget drawer."
            }
        } else {
            "Saved ($dimDisplay)! Add W builder from your launcher's widget picker."
        }
    }

    private fun resolveReceiverForProject(project: WidgetProject?): Pair<Class<*>, String> {
        if (project == null) return WBuilderWidgetReceiver::class.java to "2×2"
        return when (project.dimension) {
            WidgetDimension.SIZE_2X2 -> WBuilderWidgetReceiver::class.java to "2×2"
            WidgetDimension.SIZE_4X1 -> WBuilderWidget4x1Receiver::class.java to "4×1"
            WidgetDimension.SIZE_4X2 -> WBuilderWidget4x2Receiver::class.java to "4×2"
            WidgetDimension.SIZE_4X4 -> WBuilderWidget4x4Receiver::class.java to "4×4"
            WidgetDimension.CUSTOM -> {
                val cols = (project.resolvedWidthDp / 76f).roundToInt().coerceIn(2, 4)
                val rows = (project.resolvedHeightDp / 76f).roundToInt().coerceIn(1, 4)
                val candidates = listOf(
                    Triple(2, 1, WBuilderWidget2x1Receiver::class.java),
                    Triple(2, 2, WBuilderWidgetReceiver::class.java),
                    Triple(3, 2, WBuilderWidget3x2Receiver::class.java),
                    Triple(4, 1, WBuilderWidget4x1Receiver::class.java),
                    Triple(4, 2, WBuilderWidget4x2Receiver::class.java),
                    Triple(4, 3, WBuilderWidget4x3Receiver::class.java),
                    Triple(4, 4, WBuilderWidget4x4Receiver::class.java)
                )
                val best = candidates.minBy { (c, r, _) -> abs(c - cols) + abs(r - rows) }
                best.third to "${best.first}×${best.second}"
            }
        }
    }

    private suspend fun buildPinPreviewBundle(project: WidgetProject?): Bundle? {
        if (project == null) return null
        return runCatching {
            val telemetry = container.telemetryRepository.collectSnapshot()
            val weather = container.weatherRepository.getLastCachedOrNull()
            val bmp = WidgetBitmapRenderer.renderProjectToBitmap(
                context = context,
                project = project,
                targetWidthDp = project.resolvedWidthDp,
                targetHeightDp = project.resolvedHeightDp,
                telemetry = telemetry,
                weather = weather
            )
            val rv = RemoteViews(context.packageName, R.layout.widget_pin_preview).apply {
                setImageViewBitmap(R.id.widget_pin_preview_image, bmp)
            }
            Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, rv)
            }
        }.getOrNull()
    }

    override suspend fun refreshWidgetsForProject(projectId: String) {
        val glanceWidget = WBuilderGlanceWidget()
        val glanceManager = GlanceAppWidgetManager(context)
        val boundIds = container.widgetRepository.getBindingsForProject(projectId)
        if (boundIds.isEmpty()) {
            glanceWidget.updateAll(context)
        } else {
            boundIds.forEach { appWidgetId ->
                runCatching {
                    val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                    glanceWidget.update(context, glanceId)
                }
            }
        }
    }
}
