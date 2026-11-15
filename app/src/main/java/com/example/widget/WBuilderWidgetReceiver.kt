package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.WBuilderAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class WBuilderWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = WBuilderGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.schedulePeriodicSync(context.applicationContext)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateWorker.schedulePeriodicSync(context.applicationContext)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val container = WBuilderAppContainer(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { id ->
                container.widgetRepository.unbindAppWidget(id)
            }
        }
    }
}

class WBuilderWidget2x1Receiver : WBuilderWidgetReceiver()
class WBuilderWidget3x2Receiver : WBuilderWidgetReceiver()
class WBuilderWidget4x1Receiver : WBuilderWidgetReceiver()
class WBuilderWidget4x2Receiver : WBuilderWidgetReceiver()
class WBuilderWidget4x3Receiver : WBuilderWidgetReceiver()
class WBuilderWidget4x4Receiver : WBuilderWidgetReceiver()
