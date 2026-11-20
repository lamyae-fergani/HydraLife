package com.example.hydralife.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WeatherInfo(
    val cityName: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val conditionText: String
)

object WeatherApiService {

    private val tag = "HydraLifeWeather"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getWeatherData(cityName: String): WeatherInfo? = withContext(Dispatchers.IO) {
        try {
            val encodedName = URLEncoder.encode(cityName, "UTF-8")
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedName&count=1&language=en&format=json"
            Log.d(tag, "Requesting Geocoding URL: $geoUrl")

            val geoRequest = Request.Builder()
                .url(geoUrl)
                .addHeader("User-Agent", "HydraLife-Android")
                .get()
                .build()

            client.newCall(geoRequest).execute().use { response ->
                val code = response.code
                val bodyStr = response.body?.string() ?: ""
                Log.d(tag, "Geocoding response HTTP $code | Body length: ${bodyStr.length}")

                if (!response.isSuccessful) {
                    Log.e(tag, "Geocoding failed with HTTP $code: $bodyStr")
                    return@withContext null
                }

                val json = JSONObject(bodyStr)
                val results = json.optJSONArray("results")
                if (results == null || results.length() == 0) {
                    Log.w(tag, "Geocoding found no results for city: $cityName")
                    return@withContext null
                }

                val firstResult = results.getJSONObject(0)
                val lat = firstResult.getDouble("latitude")
                val lon = firstResult.getDouble("longitude")
                val name = firstResult.optString("name", cityName)
                Log.d(tag, "Geocoding resolved $cityName -> Lat: $lat, Lon: $lon, Name: $name")

                return@withContext getWeatherDataByCoords(lat, lon, name)
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during geocoding for $cityName: ${e.localizedMessage}", e)
            null
        }
    }

    suspend fun getWeatherDataByCoords(lat: Double, lon: Double, customCityName: String = "Current Location"): WeatherInfo? = withContext(Dispatchers.IO) {
        try {
            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,apparent_temperature,weather_code"
            Log.d(tag, "Requesting Forecast URL: $weatherUrl")

            val request = Request.Builder()
                .url(weatherUrl)
                .addHeader("User-Agent", "HydraLife-Android")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val bodyStr = response.body?.string() ?: ""
                Log.d(tag, "Forecast response HTTP $code | Body length: ${bodyStr.length}")

                if (!response.isSuccessful) {
                    Log.e(tag, "Forecast failed with HTTP $code: $bodyStr")
                    return@withContext null
                }

                val json = JSONObject(bodyStr)
                val current = json.optJSONObject("current")
                if (current == null) {
                    Log.e(tag, "Forecast JSON missing 'current' object: $bodyStr")
                    return@withContext null
                }

                val temp = current.getDouble("temperature_2m")
                val apparentTemp = current.optDouble("apparent_temperature", temp)
                val weatherCode = current.getInt("weather_code")
                val condition = getWeatherConditionText(weatherCode)

                Log.d(tag, "Successfully parsed weather for $customCityName: ${temp}°C, $condition")
                return@withContext WeatherInfo(
                    cityName = customCityName,
                    temperature = temp,
                    apparentTemperature = apparentTemp,
                    conditionText = condition
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during forecast fetch for coords ($lat, $lon): ${e.localizedMessage}", e)
            null
        }
    }

    private fun getWeatherConditionText(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Fair"
        }
    }
}
