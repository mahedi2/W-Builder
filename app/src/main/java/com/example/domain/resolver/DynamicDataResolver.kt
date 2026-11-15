package com.example.domain.resolver

import com.example.data.repository.DeviceTelemetrySnapshot
import com.example.data.repository.WeatherSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DynamicDataResolver {

    fun resolveTokens(
        template: String,
        telemetry: DeviceTelemetrySnapshot,
        weather: WeatherSnapshot?,
        useCelsius: Boolean = true,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (!template.contains("{{")) return template

        val date = Date(nowMillis)
        val locale = Locale.getDefault()
        val time12 = SimpleDateFormat("hh:mm a", locale).format(date)
        val time24 = SimpleDateFormat("HH:mm", locale).format(date)
        val dateStr = SimpleDateFormat("EEE, MMM d", locale).format(date)
        val dayStr = SimpleDateFormat("EEEE", locale).format(date)

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }

        val replacements = mapOf(
            "{{time}}" to time12,
            "{{time_24}}" to time24,
            "{{date}}" to dateStr,
            "{{day}}" to dayStr,
            "{{greeting}}" to greeting,
            "{{battery}}" to telemetry.batteryPercent.toString(),
            "{{battery_status}}" to if (telemetry.isCharging) "Charging" else "Discharging",
            "{{ram_used}}" to "${telemetry.ramUsedPercent}%",
            "{{storage_used}}" to "${telemetry.storageUsedPercent}%",
            "{{volume}}" to "${telemetry.mediaVolumePercent}%",
            "{{network}}" to telemetry.networkLabel,
            "{{alarm}}" to (telemetry.nextAlarmText ?: "No alarm"),
            "{{weather_temp}}" to (weather?.formatTemp(useCelsius) ?: "--°"),
            "{{weather_city}}" to (weather?.cityName ?: "Offline"),
            "{{weather_cond}}" to (weather?.conditionLabel ?: "Syncing"),
            "{{weather_feels}}" to (weather?.formatFeelsLike(useCelsius) ?: "--°"),
            "{{humidity}}" to (weather?.let { "${it.humidityPercent}%" } ?: "--%"),
            "{{wind}}" to (weather?.let { "${it.windKmh.toInt()} km/h" } ?: "--"),
            "{{rss_title}}" to telemetry.rssTitle.ifBlank { "No Feed" },
            "{{rss_desc}}" to telemetry.rssDescription,
            "{{feed_text}}" to listOf(telemetry.rssTitle, telemetry.rssDescription).filter { it.isNotBlank() }.joinToString(" · "),
            "{{notif_count}}" to telemetry.notifCount.toString(),
            "{{notif_title}}" to telemetry.notifTitle.ifBlank { "No Notifications" },
            "{{notif_text}}" to telemetry.notifText
        )

        var output = template
        replacements.forEach { (token, value) ->
            output = output.replace(token, value, ignoreCase = true)
        }
        val globalRegex = Regex("\\{\\{global\\.([a-zA-Z0-9_\\-]+)\\}\\}", RegexOption.IGNORE_CASE)
        output = globalRegex.replace(output) { match ->
            val key = match.groupValues[1]
            val enabled = telemetry.globalSwitches[key] == true
            if (enabled) "ON" else "OFF"
        }
        return output
    }
}
