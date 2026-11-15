package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SystemStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BATTERY_CHANGED,
            "android.app.action.NEXT_ALARM_CLOCK_CHANGED" -> {
                WidgetUpdateWorker.enqueueImmediateSync(context.applicationContext)
            }
        }
    }
}
