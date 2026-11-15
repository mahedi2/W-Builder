package com.example.domain.resolver

import com.example.data.repository.DeviceTelemetrySnapshot
import com.example.data.repository.WeatherSnapshot
import com.example.domain.model.GaugeMetric
import com.example.domain.model.LayerPayload
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ResolvedProgressValue(
    val fraction: Float,
    val displayText: String,
    val subLabel: String
)

object ProgressMetricResolver {

    fun resolve(
        payload: LayerPayload.ProgressRing,
        telemetry: DeviceTelemetrySnapshot,
        weather: WeatherSnapshot?,
        nowMillis: Long = System.currentTimeMillis()
    ): ResolvedProgressValue {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val date = Date(nowMillis)
        val locale = Locale.getDefault()

        return when (payload.metric) {
            GaugeMetric.HOURS_12 -> {
                val h = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
                ResolvedProgressValue(
                    fraction = (h / 12f).coerceIn(0f, 1f),
                    displayText = String.format(locale, "%02d", h),
                    subLabel = "Hours (12h)"
                )
            }
            GaugeMetric.HOURS_24 -> {
                val h = cal.get(Calendar.HOUR_OF_DAY)
                ResolvedProgressValue(
                    fraction = (h / 24f).coerceIn(0f, 1f),
                    displayText = String.format(locale, "%02d", h),
                    subLabel = "Hours (24h)"
                )
            }
            GaugeMetric.MINUTES -> {
                val m = cal.get(Calendar.MINUTE)
                ResolvedProgressValue(
                    fraction = (m / 60f).coerceIn(0f, 1f),
                    displayText = String.format(locale, "%02d", m),
                    subLabel = "Minutes"
                )
            }
            GaugeMetric.MINUTES_5_STEP -> {
                val m = (cal.get(Calendar.MINUTE) / 5) * 5
                ResolvedProgressValue(
                    fraction = (m / 60f).coerceIn(0f, 1f),
                    displayText = String.format(locale, "%02d", m),
                    subLabel = "Min (5m)"
                )
            }
            GaugeMetric.BATTERY -> {
                val pct = telemetry.batteryPercent.coerceIn(0, 100)
                ResolvedProgressValue(
                    fraction = pct / 100f,
                    displayText = "$pct%",
                    subLabel = "Battery"
                )
            }
            GaugeMetric.BATTERY_0_TO_9 -> {
                val step = ((telemetry.batteryPercent.coerceIn(0, 100) * 9f) / 100f)
                    .roundToInt()
                    .coerceIn(0, 9)
                ResolvedProgressValue(
                    fraction = step / 9f,
                    displayText = "$step/9",
                    subLabel = "Battery (0-9)"
                )
            }
            GaugeMetric.DAY_OF_WEEK -> {
                val dow1To7 = ((cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) + 1).coerceIn(1, 7)
                val name = SimpleDateFormat("EEEE", locale).format(date)
                ResolvedProgressValue(
                    fraction = dow1To7 / 7f,
                    displayText = name,
                    subLabel = "Day Of Week"
                )
            }
            GaugeMetric.DAY_OF_WEEK_SHORT -> {
                val dow1To7 = ((cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) + 1).coerceIn(1, 7)
                val shortName = SimpleDateFormat("EEE", locale).format(date)
                ResolvedProgressValue(
                    fraction = dow1To7 / 7f,
                    displayText = shortName,
                    subLabel = "Weekday"
                )
            }
            GaugeMetric.DAY_OF_WEEK_NUM -> {
                val isoDow = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
                ResolvedProgressValue(
                    fraction = isoDow / 7f,
                    displayText = "$isoDow/7",
                    subLabel = "Day #"
                )
            }
            GaugeMetric.DAY_OF_MONTH -> {
                val dom = cal.get(Calendar.DAY_OF_MONTH)
                val maxDom = cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                ResolvedProgressValue(
                    fraction = (dom.toFloat() / maxDom.toFloat()).coerceIn(0f, 1f),
                    displayText = String.format(locale, "%02d", dom),
                    subLabel = "Day Of Month"
                )
            }
            GaugeMetric.DAY_OF_MONTH_SHORT -> {
                val dom = cal.get(Calendar.DAY_OF_MONTH)
                val maxDom = cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                ResolvedProgressValue(
                    fraction = (dom.toFloat() / maxDom.toFloat()).coerceIn(0f, 1f),
                    displayText = dom.toString(),
                    subLabel = "Day"
                )
            }
            GaugeMetric.DAY_OF_MONTH_NUMBER -> {
                val dom = cal.get(Calendar.DAY_OF_MONTH)
                val maxDom = cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                ResolvedProgressValue(
                    fraction = (dom.toFloat() / maxDom.toFloat()).coerceIn(0f, 1f),
                    displayText = "$dom/$maxDom",
                    subLabel = "Month Day"
                )
            }
            GaugeMetric.MONTH -> {
                val m = cal.get(Calendar.MONTH) + 1
                val name = SimpleDateFormat("MMMM", locale).format(date)
                ResolvedProgressValue(
                    fraction = (m / 12f).coerceIn(0f, 1f),
                    displayText = name,
                    subLabel = "Month"
                )
            }
            GaugeMetric.MONTH_SHORT -> {
                val m = cal.get(Calendar.MONTH) + 1
                val shortName = SimpleDateFormat("MMM", locale).format(date)
                ResolvedProgressValue(
                    fraction = (m / 12f).coerceIn(0f, 1f),
                    displayText = shortName,
                    subLabel = "Month"
                )
            }
            GaugeMetric.RAM -> {
                val pct = telemetry.ramUsedPercent.coerceIn(0, 100)
                ResolvedProgressValue(pct / 100f, "$pct%", "RAM")
            }
            GaugeMetric.STORAGE -> {
                val pct = telemetry.storageUsedPercent.coerceIn(0, 100)
                ResolvedProgressValue(pct / 100f, "$pct%", "Storage")
            }
            GaugeMetric.VOLUME -> {
                val pct = telemetry.mediaVolumePercent.coerceIn(0, 100)
                ResolvedProgressValue(pct / 100f, "$pct%", "Volume")
            }
            GaugeMetric.CUSTOM -> {
                val resolved = DynamicDataResolver.resolveTokens(
                    template = payload.customValueExpression,
                    telemetry = telemetry,
                    weather = weather,
                    nowMillis = nowMillis
                ).trim()
                val numMatch = Regex("-?\\d+(?:\\.\\d+)?").find(resolved)?.value?.toFloatOrNull()
                val minV = payload.customMin
                val maxV = payload.customMax.let { if (it <= minV) minV + 100f else it }
                val value = numMatch ?: minV
                val frac = ((value - minV) / (maxV - minV)).coerceIn(0f, 1f)
                val display = if (resolved.isNotBlank()) resolved else "${(frac * 100).roundToInt()}%"
                ResolvedProgressValue(
                    fraction = frac,
                    displayText = display,
                    subLabel = payload.customLabel.ifBlank { "Custom" }
                )
            }
        }
    }
}
