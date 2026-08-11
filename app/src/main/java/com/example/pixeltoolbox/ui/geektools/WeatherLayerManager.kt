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

package com.example.pixeltoolbox.ui.geektools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Manages weather layer data: cloud imagery and typhoon tracks.
 * Caches data locally with automatic cleanup.
 */
object WeatherLayerManager {
    private const val GIBS_BASE = "https://gibs.earthdata.nasa.gov/wmts/epsg4326/best"
    private const val CLOUD_LAYER = "VIIRS_SNPP_CorrectedReflectance_TrueColor"
    private const val TILE_MATRIX = "250m"
    private const val CMA_TYPHOON_LIST = "http://typhoon.nmc.cn/weatherservice/typhoon/jsons/list_%d"
    private const val CMA_TYPHOON_VIEW = "http://typhoon.nmc.cn/weatherservice/typhoon/jsons/view_%d"
    private const val CACHE_MAX_AGE_DAYS = 3L

    // Cloud tiles at zoom 1: 4 cols × 2 rows → 1024×512 total
    private const val CLOUD_ZOOM = 1
    private const val CLOUD_COLS = 4
    private const val CLOUD_ROWS = 2
    private const val TILE_SIZE = 256

    data class TyphoonTrack(
        val id: Int,
        val name: String,
        val enName: String,
        val status: String, // "start", "stop"
        val points: List<TrackPoint>
    )

    data class TrackPoint(
        val lat: Double,
        val lon: Double,
        val windSpeed: Double, // m/s
        val pressure: Double,  // hPa
        val time: String
    )

    // --- Public API ---

    suspend fun fetchCloudBitmap(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "weather/clouds")
        val dateStr = currentDateBucket()
        val cached = File(cacheDir, "$dateStr.png")
        if (cached.exists() && cached.lastModified() > System.currentTimeMillis() - TimeUnit.HOURS.toMillis(12)) {
            return@withContext BitmapFactory.decodeFile(cached.absolutePath)
        }
        val bitmap = downloadCloudComposite(dateStr)
        if (bitmap != null) {
            cacheDir.mkdirs()
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, cached.outputStream())
            cleanupCache(cacheDir)
        }
        bitmap
    }

    suspend fun fetchTyphoonTracks(context: Context): List<TyphoonTrack> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "weather/typhoons_2026.json")
        // Cache for 6 hours
        if (cacheFile.exists() && cacheFile.lastModified() > System.currentTimeMillis() - TimeUnit.HOURS.toMillis(6)) {
            val cached = cacheFile.readText()
            if (cached.isNotBlank()) return@withContext parseTyphoonList(cached)
        }
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val listUrl = String.format(CMA_TYPHOON_LIST, year)
        val jsonp = httpGet(listUrl, mapOf("User-Agent" to "Mozilla/5.0"))
            ?: return@withContext emptyList()
        val json = extractJsonFromJsonp(jsonp) ?: return@withContext emptyList()
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText(json)
        cleanupCache(cacheFile.parentFile!!)
        parseTyphoonList(json)
    }

    // --- Cloud tile download ---

    private fun downloadCloudComposite(dateStr: String): Bitmap? {
        val fullWidth = TILE_SIZE * CLOUD_COLS
        val fullHeight = TILE_SIZE * CLOUD_ROWS
        val result = Bitmap.createBitmap(fullWidth, fullHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (row in 0 until CLOUD_ROWS) {
            for (col in 0 until CLOUD_COLS) {
                val url = "$GIBS_BASE/$CLOUD_LAYER/default/$dateStr/$TILE_MATRIX/$CLOUD_ZOOM/$row/$col.jpg"
                val tile = downloadTile(url) ?: return null
                canvas.drawBitmap(tile, (col * TILE_SIZE).toFloat(), (row * TILE_SIZE).toFloat(), paint)
                tile.recycle()
            }
        }
        return result
    }

    private fun downloadTile(urlStr: String): Bitmap? {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.requestMethod = "GET"
            val input: InputStream = conn.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            conn.disconnect()
            bitmap
        } catch (_: Exception) { null }
    }

    // --- Typhoon parsing ---

    private fun parseTyphoonList(json: String): List<TyphoonTrack> {
        return try {
            val root = JSONObject(json)
            val list = root.optJSONArray("typhoonList") ?: return emptyList()
            val tracks = mutableListOf<TyphoonTrack>()
            for (i in 0 until list.length()) {
                val item = list.getJSONArray(i)
                val id = item.getInt(0)
                val enName = item.optString(1, "")
                val cnName = item.optString(2, "")
                // Start date: item[3], End date: item[4], Start timestamp: item[5]
                val status = item.optString(7, "stop")
                if (status != "start") continue // Only active typhoons

                val track = TyphoonTrack(id, cnName, enName, status, emptyList())
                // Fetch detailed track only for active typhoons
                val detailUrl = String.format(CMA_TYPHOON_VIEW, id)
                val detailJsonp = httpGet(detailUrl, mapOf("User-Agent" to "Mozilla/5.0"))
                if (detailJsonp != null) {
                    val detailJson = extractJsonFromJsonp(detailJsonp)
                    if (detailJson != null) {
                        tracks.add(track.copy(points = parseTrackPoints(detailJson)))
                    } else {
                        tracks.add(track)
                    }
                } else {
                    tracks.add(track)
                }
            }
            tracks
        } catch (_: Exception) { emptyList() }
    }

    private fun parseTrackPoints(json: String): List<TrackPoint> {
        return try {
            val root = JSONObject(json)
            val typhoonArr = root.optJSONArray("typhoon") ?: return emptyList()
            // typhoon[8] contains the track points array
            val typhoonObj = typhoonArr.getJSONArray(8)
            val points = mutableListOf<TrackPoint>()
            for (i in 0 until typhoonObj.length()) {
                val p = typhoonObj.getJSONArray(i)
                // Format: [time, lon, lat, wind, pressure, ...]
                points.add(TrackPoint(
                    lat = p.optDouble(2, 0.0),
                    lon = p.optDouble(1, 0.0),
                    windSpeed = p.optDouble(3, 0.0),
                    pressure = p.optDouble(4, 0.0),
                    time = p.optString(0, "")
                ))
            }
            points
        } catch (_: Exception) { emptyList() }
    }

    // --- Utilities ---

    private fun currentDateBucket(): String {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    private fun httpGet(urlStr: String, headers: Map<String, String>): String? {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.requestMethod = "GET"
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            val input: InputStream = conn.inputStream
            val data = input.bufferedReader().use { it.readText() }
            input.close()
            conn.disconnect()
            data
        } catch (_: Exception) { null }
    }

    private fun extractJsonFromJsonp(jsonp: String): String? {
        // JSONP format: "callbackName({...})"
        val start = jsonp.indexOf('(')
        val end = jsonp.lastIndexOf(')')
        if (start < 0 || end <= start) return null
        return jsonp.substring(start + 1, end)
    }

    private fun cleanupCache(dir: File) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(CACHE_MAX_AGE_DAYS)
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) file.delete()
        }
    }
}
