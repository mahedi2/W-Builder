package com.example.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.WBuilderAppContainer
import java.util.concurrent.TimeUnit

private const val TAG = "WidgetUpdateWorker"

class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = WBuilderAppContainer(applicationContext)
            container.preferencesDataStore.bumpWidgetRefreshRevision()
            WBuilderGlanceWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update W builder Glance widgets", e)
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "wbuilder_periodic_widget_sync"
        private const val IMMEDIATE_WORK_NAME = "wbuilder_immediate_widget_sync"

        fun schedulePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15,
                TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueImmediateSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
