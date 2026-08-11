/*
 * Pixel Toolbox (像素工具箱)
 * Copyright (C) 2026 Pixel Toolbox Project
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.example.pixeltoolbox.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Calendar

object WeatherProvider {
    
    data class DailyForecast(
        val dayName: String,
        val iconEmoji: String, // Changed to string for Emoji
        val temp: String
    )

    data class DashboardWeatherInfo(
        val currentTemp: String,
        val highTemp: String,
        val lowTemp: String,
        val currentIconEmoji: String,
        val location: String,
        val feelsLike: String,
        val windSpeed: String,
        val humidity: String,
        val pressure: String,
        val forecast: List<DailyForecast>
    )

    suspend fun getRealWeather(context: Context): DashboardWeatherInfo = withContext(Dispatchers.IO) {
        var lat = 30.46
        var lon = 106.63
        var cityName = "广安市"

        // 1. Try to get Location
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) 
                          ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                          ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                
                if (loc != null) {
                    lat = loc.latitude
                    lon = loc.longitude
                    
                    try {
                        val geocoder = Geocoder(context, Locale.CHINA)
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: cityName
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fetch Open-Meteo Weather
        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,apparent_temperature,relative_humidity_2m,surface_pressure,wind_speed_10m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=Asia%2FShanghai"
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            val current = json.getJSONObject("current")
            val currentTemp = Math.round(current.getDouble("temperature_2m")).toString() + "°C"
            val apparentTemp = Math.round(current.getDouble("apparent_temperature")).toString() + "°C"
            val humidity = current.getInt("relative_humidity_2m").toString() + "%"
            val pressure = Math.round(current.getDouble("surface_pressure") / 10.0).toString() + "kPa"
            val windSpeed = Math.round(current.getDouble("wind_speed_10m") * 1000 / 3600.0).toString() + "M/S"
            val currentCode = current.getInt("weather_code")
            val currentIcon = getWeatherEmoji(currentCode)

            val daily = json.getJSONObject("daily")
            val timeArr = daily.getJSONArray("time")
            val maxTempArr = daily.getJSONArray("temperature_2m_max")
            val minTempArr = daily.getJSONArray("temperature_2m_min")
            val codeArr = daily.getJSONArray("weather_code")

            val highTemp = Math.round(maxTempArr.getDouble(0)).toString() + "°C"
            val lowTemp = Math.round(minTempArr.getDouble(0)).toString() + "°C"

            val forecastList = mutableListOf<DailyForecast>()
            val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (i in 1 until Math.min(5, timeArr.length())) {
                val dateStr = timeArr.getString(i)
                val date = sdf.parse(dateStr)
                val cal = Calendar.getInstance()
                cal.time = date!!
                val dayName = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]
                val fTemp = Math.round(maxTempArr.getDouble(i)).toString() + "°C"
                val fEmoji = getWeatherEmoji(codeArr.getInt(i))
                forecastList.add(DailyForecast(dayName, fEmoji, fTemp))
            }
            
            while (forecastList.size < 4) {
                forecastList.add(DailyForecast("--", "☁️", "--"))
            }

            return@withContext DashboardWeatherInfo(
                currentTemp = currentTemp,
                highTemp = highTemp,
                lowTemp = lowTemp,
                currentIconEmoji = currentIcon,
                location = cityName,
                feelsLike = apparentTemp,
                windSpeed = windSpeed,
                humidity = humidity,
                pressure = pressure,
                forecast = forecastList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext getFallbackWeather(cityName)
        }
    }

    private fun getWeatherEmoji(code: Int): String {
        return when (code) {
            0 -> "☀️"
            1, 2, 3 -> "⛅"
            45, 48 -> "🌫️"
            51, 53, 55, 56, 57 -> "🌧️"
            61, 63, 65, 66, 67 -> "🌧️"
            71, 73, 75, 77 -> "❄️"
            80, 81, 82 -> "🌧️"
            85, 86 -> "❄️"
            95, 96, 99 -> "⛈️"
            else -> "☁️"
        }
    }

    private fun getFallbackWeather(cityName: String): DashboardWeatherInfo {
        return DashboardWeatherInfo(
            currentTemp = "29°C",
            highTemp = "39°C",
            lowTemp = "27°C",
            currentIconEmoji = "🌙",
            location = cityName,
            feelsLike = "29°C",
            windSpeed = "2M/S",
            humidity = "63%",
            pressure = "100kPa",
            forecast = listOf(
                DailyForecast("周五", "☀️", "27°C"),
                DailyForecast("周六", "☀️", "27°C"),
                DailyForecast("周日", "⛅", "28°C"),
                DailyForecast("周一", "🌧️", "29°C")
            )
        )
    }
}
