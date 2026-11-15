package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.remote.GeocodingPlaceDto
import com.example.data.remote.OpenMeteoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

data class DailyForecastPoint(
    val dayLabel: String,
    val weatherCode: Int,
    val minCelsius: Double,
    val maxCelsius: Double
) {
    fun formatRange(useCelsius: Boolean): String {
        val minV = if (useCelsius) minCelsius else (minCelsius * 9.0 / 5.0) + 32.0
        val maxV = if (useCelsius) maxCelsius else (maxCelsius * 9.0 / 5.0) + 32.0
        return "${maxV.roundToInt()}°/${minV.roundToInt()}°"
    }
}

data class WeatherSnapshot(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val humidityPercent: Int,
    val windKmh: Double,
    val minCelsius: Double,
    val maxCelsius: Double,
    val weatherCode: Int,
    val conditionLabel: String,
    val forecastDays: List<DailyForecastPoint> = emptyList(),
    val isLive: Boolean,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    fun formatTemp(useCelsius: Boolean): String {
        val value = if (useCelsius) temperatureCelsius else (temperatureCelsius * 9.0 / 5.0) + 32.0
        val unit = if (useCelsius) "°C" else "°F"
        return "${value.roundToInt()}$unit"
    }

    fun formatFeelsLike(useCelsius: Boolean): String {
        val value = if (useCelsius) feelsLikeCelsius else (feelsLikeCelsius * 9.0 / 5.0) + 32.0
        val unit = if (useCelsius) "°C" else "°F"
        return "Feels ${value.roundToInt()}$unit"
    }

    fun formatMinMax(useCelsius: Boolean): String {
        val minV = if (useCelsius) minCelsius else (minCelsius * 9.0 / 5.0) + 32.0
        val maxV = if (useCelsius) maxCelsius else (maxCelsius * 9.0 / 5.0) + 32.0
        return "H:${maxV.roundToInt()}° L:${minV.roundToInt()}°"
    }
}

class WeatherRepository(
    private val context: Context,
    private val api: OpenMeteoApi = OpenMeteoApi.create()
) {
    @Volatile
    private var cachedSnapshot: WeatherSnapshot? = null

    suspend fun searchCities(query: String): Result<List<GeocodingPlaceDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = query.trim()
            if (trimmed.length < 2) return@runCatching emptyList()
            api.searchLocations(query = trimmed).results.orEmpty()
        }
    }

    suspend fun fetchWeather(
        fallbackCity: String,
        fallbackLat: Double,
        fallbackLon: Double,
        useDeviceLocation: Boolean
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val (lat, lon, resolvedCity) = if (useDeviceLocation) {
                resolveCoarseLocationOrNull() ?: Triple(fallbackLat, fallbackLon, fallbackCity)
            } else {
                Triple(fallbackLat, fallbackLon, fallbackCity)
            }

            val response = api.getForecast(latitude = lat, longitude = lon)
            val tempC = response.current?.temperature2m
                ?: error("Weather service did not return current temperature.")
            val feelsC = response.current.apparentTemperature ?: tempC
            val humidity = response.current.relativeHumidity2m ?: 50
            val wind = response.current.windSpeed10m ?: 0.0
            val code = response.current.weatherCode ?: 0
            val minC = response.daily?.temperatureMin?.firstOrNull() ?: (tempC - 3.0)
            val maxC = response.daily?.temperatureMax?.firstOrNull() ?: (tempC + 4.0)

            val days = buildForecastPoints(response.daily)

            WeatherSnapshot(
                cityName = resolvedCity,
                latitude = lat,
                longitude = lon,
                temperatureCelsius = tempC,
                feelsLikeCelsius = feelsC,
                humidityPercent = humidity,
                windKmh = wind,
                minCelsius = minC,
                maxCelsius = maxC,
                weatherCode = code,
                conditionLabel = mapWmoCodeToLabel(code),
                forecastDays = days,
                isLive = true
            ).also { cachedSnapshot = it }
        }
    }

    fun getLastCachedOrNull(): WeatherSnapshot? = cachedSnapshot

    fun clearCache() {
        cachedSnapshot = null
    }

    private fun buildForecastPoints(daily: com.example.data.remote.DailyWeatherDto?): List<DailyForecastPoint> {
        if (daily == null) return emptyList()
        val times = daily.time.orEmpty()
        val codes = daily.weatherCodes.orEmpty()
        val mins = daily.temperatureMin.orEmpty()
        val maxs = daily.temperatureMax.orEmpty()
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())

        return times.indices.map { i ->
            val dayLabel = if (i == 0) "Today" else {
                runCatching {
                    parser.parse(times[i])?.let { dayFmt.format(it) }
                }.getOrNull() ?: "+${i}d"
            }
            DailyForecastPoint(
                dayLabel = dayLabel,
                weatherCode = codes.getOrElse(i) { 0 },
                minCelsius = mins.getOrElse(i) { 15.0 },
                maxCelsius = maxs.getOrElse(i) { 22.0 }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun resolveCoarseLocationOrNull(): Triple<Double, Double, String>? {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val location = locationManager.getProviders(true)
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: return null

        val city = resolveCityName(location.latitude, location.longitude) ?: "Local Area"
        return Triple(location.latitude, location.longitude, city)
    }

    @Suppress("DEPRECATION")
    private fun resolveCityName(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        return runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var cityResult: String? = null
                val latch = java.util.concurrent.CountDownLatch(1)
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    cityResult = addresses.firstOrNull()?.locality
                        ?: addresses.firstOrNull()?.subAdminArea
                    latch.countDown()
                }
                latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                cityResult
            } else {
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea
            }
        }.getOrNull()
    }

    companion object {
        fun mapWmoCodeToLabel(code: Int): String = when (code) {
            0 -> "Clear"
            1, 2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55, 56, 57 -> "Drizzle"
            61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
            71, 73, 75, 77, 85, 86 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Fair"
        }
    }
}
