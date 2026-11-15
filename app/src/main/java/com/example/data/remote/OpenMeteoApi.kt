package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @param:Json(name = "current") val current: CurrentWeatherDto? = null,
    @param:Json(name = "daily") val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @param:Json(name = "temperature_2m") val temperature2m: Double? = null,
    @param:Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @param:Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @param:Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    @param:Json(name = "weather_code") val weatherCode: Int? = null
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    @param:Json(name = "time") val time: List<String>? = null,
    @param:Json(name = "weather_code") val weatherCodes: List<Int>? = null,
    @param:Json(name = "temperature_2m_max") val temperatureMax: List<Double>? = null,
    @param:Json(name = "temperature_2m_min") val temperatureMin: List<Double>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingSearchResponse(
    @param:Json(name = "results") val results: List<GeocodingPlaceDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingPlaceDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "country") val country: String? = null,
    @param:Json(name = "admin1") val admin1: String? = null,
    @param:Json(name = "latitude") val latitude: Double,
    @param:Json(name = "longitude") val longitude: Double
) {
    val displayLabel: String
        get() = listOfNotNull(name, admin1, country).distinct().joinToString(", ")
}

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("forecast_days") forecastDays: Int = 3,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse

    @GET
    suspend fun searchLocations(
        @Url url: String = "https://geocoding-api.open-meteo.com/v1/search",
        @Query("name") query: String,
        @Query("count") count: Int = 6,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingSearchResponse

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/"

        fun create(): OpenMeteoApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OpenMeteoApi::class.java)
        }
    }
}
