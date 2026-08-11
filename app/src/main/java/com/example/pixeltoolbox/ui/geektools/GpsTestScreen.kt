package com.example.pixeltoolbox.ui.geektools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import java.util.Calendar
import java.util.TimeZone
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.pixeltoolbox.ui.theme.GlassCard
import com.example.pixeltoolbox.ui.theme.iOSBackground
import com.example.pixeltoolbox.ui.theme.iOSBlue
import com.example.pixeltoolbox.ui.theme.iOSButton
import com.example.pixeltoolbox.ui.theme.iOSGreen
import com.example.pixeltoolbox.ui.theme.iOSLabel
import com.example.pixeltoolbox.ui.theme.iOSOutlineButton
import com.example.pixeltoolbox.ui.theme.iOSRed
import com.example.pixeltoolbox.ui.theme.iOSSecondaryLabel
import kotlin.math.*
import androidx.compose.material3.MaterialTheme

/**
 * Single satellite data snapshot
 */
data class SatelliteInfo(
    val svid: Int,
    val constellationType: Int,
    val azimuth: Float,
    val elevation: Float,
    val cn0DbHz: Float,
    val usedInFix: Boolean,
    val constellationName: String
)

data class GpsLocationData(
    val latitude: Double = Double.NaN,
    val longitude: Double = Double.NaN,
    val altitude: Double = Double.NaN,
    val accuracy: Float = Float.NaN,
    val speed: Float = Float.NaN,
    val utcTime: Long = 0L
)

// Constellation color map
private val constellationColors = mapOf(
    GnssStatus.CONSTELLATION_GPS to Color(0xFF4A90D9),        // blue
    GnssStatus.CONSTELLATION_GLONASS to Color(0xFFE53E3E),    // red
    GnssStatus.CONSTELLATION_BEIDOU to Color(0xFFFF8C00),     // orange
    GnssStatus.CONSTELLATION_GALILEO to Color(0xFF38A169),    // green
    GnssStatus.CONSTELLATION_QZSS to Color(0xFF9B59B6),      // purple
    GnssStatus.CONSTELLATION_IRNSS to Color(0xFF00BCD4)      // cyan
)

private fun getConstellationName(type: Int): String = when (type) {
    GnssStatus.CONSTELLATION_GPS -> "GPS（美国全球定位系统）"
    GnssStatus.CONSTELLATION_GLONASS -> "格洛纳斯"
    GnssStatus.CONSTELLATION_BEIDOU -> "北斗"
    GnssStatus.CONSTELLATION_GALILEO -> "伽利略"
    GnssStatus.CONSTELLATION_QZSS -> "准天顶"
    GnssStatus.CONSTELLATION_IRNSS -> "印度导航"
    else -> "未知"
}

private fun getConstellationAbbr(type: Int): String = when (type) {
    GnssStatus.CONSTELLATION_GPS -> "G"
    GnssStatus.CONSTELLATION_GLONASS -> "L"
    GnssStatus.CONSTELLATION_BEIDOU -> "B"
    GnssStatus.CONSTELLATION_GALILEO -> "E"
    GnssStatus.CONSTELLATION_QZSS -> "J"
    GnssStatus.CONSTELLATION_IRNSS -> "I"
    else -> "U"
}

private fun getConstellationShortName(type: Int): String = when (type) {
    GnssStatus.CONSTELLATION_GPS -> "GPS"
    GnssStatus.CONSTELLATION_GLONASS -> "格洛纳斯"
    GnssStatus.CONSTELLATION_BEIDOU -> "北斗"
    GnssStatus.CONSTELLATION_GALILEO -> "伽利略"
    GnssStatus.CONSTELLATION_QZSS -> "准天顶"
    GnssStatus.CONSTELLATION_IRNSS -> "印度导航"
    else -> "未知"
}

private fun getConstellationColor(type: Int): Color =
    constellationColors[type] ?: Color.Gray

@OptIn(ExperimentalLayoutApi::class)
@Suppress("ExperimentalMaterial3Api", "ExperimentalLayoutApi")
@Composable
fun GpsTestScreen(context: Context, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    // States
    var satellites by remember { mutableStateOf<List<SatelliteInfo>>(emptyList()) }
    var locationData by remember { mutableStateOf(GpsLocationData()) }
    var hasPermission by remember { mutableStateOf(
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
    ) }
    var permissionRequested by remember { mutableStateOf(false) }
    var gpsEnabled by remember { mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) }

    // Weather layers
    var cloudBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var typhoonTracks by remember { mutableStateOf<List<WeatherLayerManager.TyphoonTrack>>(emptyList()) }

    LaunchedEffect(Unit) {
        cloudBitmap = WeatherLayerManager.fetchCloudBitmap(context)
        typhoonTracks = WeatherLayerManager.fetchTyphoonTracks(context)
    }

    // Request permission
    fun requestPermission() {
        permissionRequested = true
        if (context is androidx.activity.ComponentActivity) {
            context.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    // Re-check permission after returning from settings
    LaunchedEffect(permissionRequested) {
        if (permissionRequested) {
            hasPermission = (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
        }
    }

    // Register GNSS callback when permission granted
    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            onDispose { }
        } else {
            val gnssCallback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    val list = (0 until status.satelliteCount).map { i ->
                        SatelliteInfo(
                            svid = status.getSvid(i),
                            constellationType = status.getConstellationType(i),
                            azimuth = status.getAzimuthDegrees(i),
                            elevation = status.getElevationDegrees(i),
                            cn0DbHz = status.getCn0DbHz(i),
                            usedInFix = status.usedInFix(i),
                            constellationName = getConstellationName(status.getConstellationType(i))
                        )
                    }.sortedByDescending { it.cn0DbHz }
                    satellites = list
                }
            }

            try {
                locationManager.registerGnssStatusCallback(gnssCallback)
            } catch (_: SecurityException) {}

            val locationListener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    locationData = GpsLocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        accuracy = loc.accuracy,
                        speed = if (loc.speed < 1.0f) 0.0f else loc.speed,
                        utcTime = loc.time
                    )
                }
                override fun onProviderDisabled(provider: String) {
                    if (provider == LocationManager.GPS_PROVIDER) gpsEnabled = false
                }
                override fun onProviderEnabled(provider: String) {
                    if (provider == LocationManager.GPS_PROVIDER) gpsEnabled = true
                }
            }

            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener)
            } catch (_: SecurityException) {}

            onDispose {
                try { locationManager.unregisterGnssStatusCallback(gnssCallback) } catch (_: Exception) {}
                try { locationManager.removeUpdates(locationListener) } catch (_: Exception) {}
            }
        }
    }

    val textColor = iOSLabel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("GPS 测试", style = MaterialTheme.typography.headlineMedium, color = textColor)
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            if (!hasPermission) {
                // Permission request card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text("需要位置权限", style = MaterialTheme.typography.titleLarge, color = textColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("获取 GPS 卫星数据需要精确定位权限", style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel)
                        Spacer(modifier = Modifier.height(16.dp))
                        iOSButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { requestPermission() }
                        ) { Text("授予权限", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            } else if (!gpsEnabled) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text("GPS 已关闭", style = MaterialTheme.typography.titleLarge, color = textColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("请在系统设置中开启位置服务", style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel)
                        Spacer(modifier = Modifier.height(16.dp))
                        iOSButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        ) { Text("前往设置", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            } else if (satellites.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = iOSBlue,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("正在搜索卫星…", style = MaterialTheme.typography.bodyLarge, color = iOSLabel)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("请移至开阔地带以获得更好的信号", style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel)
                    }
                }
            } else {
                // Skyplot
                GlobeCard(satellites, textColor, cloudBitmap, typhoonTracks)

                // SNR Bar Chart
                Spacer(modifier = Modifier.height(16.dp))
                SnrBarChartCard(satellites, textColor)

                // Data Panel
                Spacer(modifier = Modifier.height(16.dp))
                DataPanelCard(locationData, satellites, textColor)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GlobeCard(
    satellites: List<SatelliteInfo>,
    textColor: Color,
    cloudBitmap: Bitmap? = null,
    typhoonTracks: List<WeatherLayerManager.TyphoonTrack> = emptyList()
) {
    var rotY by remember { mutableFloatStateOf(-30f) }
    var rotX by remember { mutableFloatStateOf(15f) }
    var scale by remember { mutableFloatStateOf(1f) }
    val context = LocalContext.current
    val earthBitmap = remember {
        try { BitmapFactory.decodeStream(context.assets.open("earth_texture.jpg")) }
        catch (_: Exception) { null }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("卫星地球", style = MaterialTheme.typography.titleMedium, color = textColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text("拖拽旋转  |  ● 定位中  ○ 仅可见",
                style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
            Spacer(modifier = Modifier.height(8.dp))

            // Legend — FlowRow for natural wrapping
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                constellationColors.entries.take(6).forEach { (type, color) ->
                    val hasSat = satellites.any { it.constellationType == type }
                    if (hasSat) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = color)
                            }
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(getConstellationName(type), style = MaterialTheme.typography.labelSmall, color = color)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var prevPos = down.position
                            var prevCentroid = Offset.Zero
                            var consumedByHoriz = false
                            do {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.count { it.pressed }
                                if (pressed >= 2) {
                                    // 多指:全部消费,阻止父 verticalScroll 抢手势
                                    event.changes.forEach { it.consume() }
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    if (prevCentroid != Offset.Zero) {
                                        rotY += (centroid.x - prevCentroid.x) * 0.4f
                                        rotX += (centroid.y - prevCentroid.y) * 0.4f
                                        rotX = rotX.coerceIn(-80f, 80f)
                                    }
                                    if (zoom != 1f) scale = (scale * zoom).coerceIn(0.3f, 3f)
                                    if (pan != Offset.Zero) {
                                        rotY += pan.x * 0.4f
                                        rotX += pan.y * 0.4f
                                        rotX = rotX.coerceIn(-80f, 80f)
                                    }
                                    prevCentroid = centroid
                                    consumedByHoriz = false
                                } else if (pressed == 1) {
                                    val change = event.changes.firstOrNull() ?: break
                                    val delta = change.position - prevPos
                                    prevPos = change.position
                                    // 单指旋转：水平→rotY，垂直→rotX
                                    change.consume()
                                    rotY += delta.x * 0.4f
                                    rotX += delta.y * 0.4f
                                    rotX = rotX.coerceIn(-80f, 80f)
                                    consumedByHoriz = true
                                    prevCentroid = Offset.Zero
                                } else {
                                    prevCentroid = Offset.Zero
                                    consumedByHoriz = false
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val R = (size.minDimension / 2f - 8f) * scale
                val rotYR = Math.toRadians(rotY.toDouble()).toFloat()
                val rotXR = Math.toRadians(rotX.toDouble()).toFloat()

                // --- 3D transform ---
                fun rotate(p: FloatArray): FloatArray {
                    val x = p[0]; val y = p[1]; val z = p[2]
                    val y1 = y * cos(rotXR) - z * sin(rotXR)
                    val z1 = y * sin(rotXR) + z * cos(rotXR)
                    val x2 = x * cos(rotYR) + z1 * sin(rotYR)
                    val z2 = -x * sin(rotYR) + z1 * cos(rotYR)
                    return floatArrayOf(x2, y1, z2)
                }

                fun project(p: FloatArray): Offset = Offset(cx + p[0], cy - p[1])

                // --- Sun position (real-time based on UTC) ---
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                val hourUTC = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
                // Solar declination
                val sunDecl = Math.toRadians(-23.44 * cos(Math.toRadians(360.0 * (dayOfYear + 10) / 365.0))).toFloat()
                // Solar hour angle (0 at noon UTC = sun over prime meridian)
                val sunHA = Math.toRadians((hourUTC - 12) * 15.0).toFloat()
                // Sun direction vector (pointing from Earth center to sun)
                val sunDir = floatArrayOf(
                    cos(sunDecl) * cos(sunHA),
                    sin(sunDecl),
                    -cos(sunDecl) * sin(sunHA)
                )

                // --- Draw textured earth via mesh ---
                val bmp = earthBitmap
                if (bmp != null) {
                    val meshW = 60
                    val meshH = 30
                    val vCount = (meshW + 1) * (meshH + 1)
                    val verts = FloatArray(vCount * 2)
                    val colors = IntArray(vCount)

                    for (j in 0..meshH) {
                        val lat = Math.toRadians(90.0 - j * 180.0 / meshH).toFloat()
                        for (i in 0..meshW) {
                            val lon = Math.toRadians(i * 360.0 / meshW - 180.0).toFloat()
                            // Normal vector at this surface point
                            val nx = cos(lat) * cos(lon)
                            val ny = sin(lat)
                            val nz = cos(lat) * sin(lon)
                            // Dot with sun direction
                            val dot = nx * sunDir[0] + ny * sunDir[1] + nz * sunDir[2]
                            // Smooth day/night transition - wider zone, brighter night side
                            val sunny = ((dot + 0.15f) / 0.30f).coerceIn(0f, 1f)
                            val lighting = 0.28f + 0.72f * sunny
                            val l = (lighting * 255).toInt().coerceIn(0, 255)
                            val ci = j * (meshW + 1) + i

                            val x3 = R * nx
                            val y3 = R * ny
                            val z3 = R * nz
                            val pr = rotate(floatArrayOf(x3, y3, z3))
                            val idx = ci * 2
                            val isFront = pr[2] > 0.05f
                            verts[idx] = cx + pr[0]
                            verts[idx + 1] = cy - pr[1]
                            if (isFront) {
                                colors[ci] = (0xFF shl 24) or (l shl 16) or (l shl 8) or l
                            } else {
                                colors[ci] = 0  // transparent: hide back face
                            }
                        }
                    }

                    drawContext.canvas.nativeCanvas.drawBitmapMesh(
                        bmp, meshW, meshH, verts, 0, colors, 0, null
                    )
                } else {
                    // Fallback: plain sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.3f to Color(0xFF2E86AB),
                            1.0f to Color(0xFF0B3D5C),
                            center = Offset(cx, cy), radius = R
                        ),
                        radius = R, center = Offset(cx, cy)
                    )
                }

                // Atmosphere rim
                drawCircle(Color(0x30FFFFFF), R, Offset(cx, cy), style = Stroke(1.5f))

                // --- Cloud overlay (drawBitmapMesh, alpha=0.35) ---
                val cbmp = cloudBitmap
                if (cbmp != null) {
                    val meshW = 60
                    val meshH = 30
                    val vCount = (meshW + 1) * (meshH + 1)
                    val verts = FloatArray(vCount * 2)
                    val colors = IntArray(vCount) { (0x59 shl 24) or 0xFFFFFF } // alpha ≈ 0.35

                    for (j in 0..meshH) {
                        val lat = Math.toRadians(90.0 - j * 180.0 / meshH).toFloat()
                        for (i in 0..meshW) {
                            val lon = Math.toRadians(i * 360.0 / meshW - 180.0).toFloat()
                            val nx = cos(lat) * cos(lon)
                            val ny = sin(lat)
                            val nz = cos(lat) * sin(lon)
                            val pr = rotate(floatArrayOf(R * nx, R * ny, R * nz))
                            val idx = (j * (meshW + 1) + i) * 2
                            verts[idx] = cx + pr[0]
                            verts[idx + 1] = cy - pr[1]
                            if (pr[2] <= 0.05f) colors[j * (meshW + 1) + i] = 0
                        }
                    }
                    drawContext.canvas.nativeCanvas.drawBitmapMesh(
                        cbmp, meshW, meshH, verts, 0, colors, 0, null
                    )
                }

                // --- Typhoon tracks ---
                val trackDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFE53935.toInt()
                    style = Paint.Style.FILL
                }
                val trackLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFFCDD2.toInt()
                    textSize = 10f * density
                    textAlign = Paint.Align.CENTER
                }

                val trackR = R + 2f
                for (track in typhoonTracks) {
                    val p = track.points.firstOrNull() ?: continue
                    val latRad = Math.toRadians(p.lat).toFloat()
                    val lonRad = Math.toRadians(p.lon).toFloat()
                    val tx = trackR * cos(latRad) * cos(lonRad)
                    val ty = trackR * sin(latRad)
                    val tz = trackR * cos(latRad) * sin(lonRad)
                    val pr = rotate(floatArrayOf(tx, ty, tz))
                    if (pr[2] <= -0.1f) continue
                    val pos = project(pr)
                    drawContext.canvas.nativeCanvas.drawCircle(pos.x, pos.y, 5f * density, trackDotPaint)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${track.name}（台风）", pos.x, pos.y - 12f * density, trackLabelPaint
                    )
                }

                // --- Draw satellites ---
                val satR = R + 6.dp.toPx()
                val frontSats = satellites.map { sat ->
                    val elRad = Math.toRadians((90f - sat.elevation).toDouble()).toFloat()
                    val azRad = Math.toRadians(sat.azimuth.toDouble()).toFloat()
                    val sx = satR * sin(elRad) * sin(azRad)
                    val sy = satR * cos(elRad)
                    val sz = satR * sin(elRad) * cos(azRad)
                    val pr = rotate(floatArrayOf(sx, sy, sz))
                    Triple(sat, pr, project(pr))
                }.filter { it.second[2] > -0.1f }
                    .sortedByDescending { it.second[2] }

                for ((sat, p3, pos) in frontSats) {
                    val satColor = getConstellationColor(sat.constellationType)
                    val dotR = if (sat.usedInFix) 4.5.dp.toPx() else 3.5.dp.toPx()
                    // Satellite lighting: dot raw direction with sun
                    val elRad = Math.toRadians((90f - sat.elevation).toDouble()).toFloat()
                    val azRad = Math.toRadians(sat.azimuth.toDouble()).toFloat()
                    val snx = sin(elRad) * sin(azRad)
                    val sny = cos(elRad)
                    val snz = sin(elRad) * cos(azRad)
                    val satDot = snx * sunDir[0] + sny * sunDir[1] + snz * sunDir[2]
                    val sunFactor = ((satDot + 0.08f) / 0.18f).coerceIn(0f, 1f)
                    val sunLit = 0.25f + 0.75f * sunFactor
                    val alpha = ((p3[2] / satR).coerceIn(0.2f, 1f)) * sunLit

                    if (sat.usedInFix) {
                        drawCircle(satColor.copy(alpha = alpha), dotR, pos)
                        drawCircle(Color.White.copy(alpha = alpha), dotR, pos, style = Stroke(1.dp.toPx()))
                    } else {
                        drawCircle(satColor.copy(alpha = alpha), dotR, pos, style = Stroke(1.2.dp.toPx()))
                    }

                    // Label
                    val labelPaint = android.graphics.Paint().apply {
                        val c = satColor
                        color = android.graphics.Color.argb(
                            (alpha * 255).toInt(),
                            (c.red * 255).toInt(),
                            (c.green * 255).toInt(),
                            (c.blue * 255).toInt()
                        )
                        textSize = 7f * density
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        getConstellationAbbr(sat.constellationType) + sat.svid,
                        pos.x, pos.y - dotR - 3f,
                        labelPaint
                    )
                }

                // --- North pole marker ---
                val north = rotate(floatArrayOf(0f, R, 0f))
                if (north[2] > 0) {
                    val np = project(north)
                    drawCircle(Color(0xAAFFFFFF), 2f, np)
                    val npPaint = android.graphics.Paint().apply {
                        color = 0xAAFFFFFF.toInt()
                        textSize = 9f * density
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText("N", np.x, np.y - 8f, npPaint)
                }
            }

            // Disclaimer
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                "天气数据仅供参考，来源：NASA GIBS / CMA 台风网",
                style = MaterialTheme.typography.labelSmall,
                color = iOSSecondaryLabel.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SnrBarChartCard(satellites: List<SatelliteInfo>, textColor: Color) {
    // Show top 12 by SNR
    val displayList = satellites.take(12)

    // Constellation count summary
    val constCounts = satellites.groupBy { it.constellationType }
        .mapValues { it.value.size }
        .entries.sortedByDescending { it.value }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("信号强度（信噪比）", style = MaterialTheme.typography.titleMedium, color = textColor)

            // Constellation count row — short names + natural wrapping
            if (constCounts.isNotEmpty()) {
                Text(
                    "扫描到 ${
                        constCounts.joinToString("  ") { (type, count) ->
                            "${getConstellationName(type)} ${count}颗"
                        }
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = iOSSecondaryLabel,
                    maxLines = Int.MAX_VALUE
                )
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            displayList.forEach { sat ->
                val satColor = getConstellationColor(sat.constellationType)
                val snrNorm = (sat.cn0DbHz / 50f).coerceIn(0f, 1f)
                val barColor = if (sat.usedInFix) satColor else satColor.copy(alpha = 0.4f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Satellite name
                    Text(
                        text = "${getConstellationShortName(sat.constellationType)} ${sat.svid}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sat.usedInFix) textColor else iOSSecondaryLabel,
                        modifier = Modifier.width(80.dp),
                        maxLines = 1
                    )

                    // Bar
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.7f)
                                .fillMaxWidth(snrNorm)
                                .align(Alignment.CenterStart)
                                .background(barColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                        )
                    }

                    // SNR value
                    Text(
                        text = "${sat.cn0DbHz.toInt()} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sat.usedInFix) textColor else iOSSecondaryLabel,
                        modifier = Modifier.width(38.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun DataPanelCard(locationData: GpsLocationData, satellites: List<SatelliteInfo>, textColor: Color) {
    val usedCount = satellites.count { it.usedInFix }
    val hasFix = usedCount >= 4

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("定位数据", style = MaterialTheme.typography.titleMedium, color = textColor)
            Spacer(modifier = Modifier.height(12.dp))

            // 2x3 grid
            val gridData = listOf(
                "纬度" to (if (!locationData.latitude.isNaN()) String.format("%.6f°", locationData.latitude) else "--"),
                "经度" to (if (!locationData.longitude.isNaN()) String.format("%.6f°", locationData.longitude) else "--"),
                "海拔" to (if (!locationData.altitude.isNaN()) String.format("%.1f m", locationData.altitude) else "--"),
                "定位精度" to (if (!locationData.accuracy.isNaN()) String.format("%.1f m", locationData.accuracy) else "--"),
                "速度" to (if (!locationData.speed.isNaN()) if (locationData.speed == 0.0f) "静止" else String.format("%.1f m/s", locationData.speed) else "--"),
                "修复状态" to (if (hasFix) "已定位 (${usedCount}颗)" else if (usedCount > 0) "搜星中 (${usedCount}颗)" else "等待信号")
            )

            Column {
                for (i in gridData.indices step 2) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (j in 0..1) {
                            if (i + j < gridData.size) {
                                val (label, value) = gridData[i + j]
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (label == "修复状态" && hasFix) iOSGreen
                                                else if (label == "修复状态" && usedCount > 0) Color(0xFFFF9500)
                                                else textColor
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // UTC / 北京时间
            if (locationData.utcTime > 0) {
                val utcFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val beijingFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
                }
                val timeMillis = locationData.utcTime
                Spacer(modifier = Modifier.height(4.dp))
                Text("UTC 时间    ${utcFormat.format(java.util.Date(timeMillis))}", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
                Text("北京时间    ${beijingFormat.format(java.util.Date(timeMillis))}", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
            }
        }
    }
}
