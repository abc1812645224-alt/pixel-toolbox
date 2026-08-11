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
import kotlinx.coroutines.delay

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import androidx.compose.material3.MaterialTheme

@Composable
fun BarometerScreen(context: Context, onBack: () -> Unit) {
    BackHandler { onBack() }
    
    var isRunning by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(10) }
    var testResult by remember { mutableStateOf("") }
    val maxDataPoints = 200
    var dataPoints by remember { mutableStateOf(listOf<Float>()) }
    
    LaunchedEffect(isRunning) {
        if (isRunning) {
            countdown = 10
            testResult = ""
            while (countdown > 0) {
                delay(1000)
                countdown -= 1
            }
            isRunning = false
            
            val maxVal = dataPoints.maxOrNull() ?: 0f
            val minVal = dataPoints.minOrNull() ?: 0f
            val diff = maxVal - minVal
            testResult = if (dataPoints.isEmpty()) {
                "未能获取气压数据"
            } else if (diff > 2.0f) {
                "气密性极佳 (差值: ${String.format("%.2f", diff)} hPa)"
            } else if (diff > 0.5f) {
                "气密性良好 (差值: ${String.format("%.2f", diff)} hPa)"
            } else {
                "气密性极差或未按压 (差值: ${String.format("%.2f", diff)} hPa)"
            }
        }    }

    
    // Sensor setup
    DisposableEffect(context, isRunning) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        
        if (pressureSensor == null) {
            onDispose { }
        } else if (!isRunning) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val currentP = event.values[0]
                    dataPoints = (dataPoints + currentP).takeLast(maxDataPoints)
                }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    val maxVal = dataPoints.maxOrNull() ?: 0f
    val minVal = dataPoints.minOrNull() ?: 0f
    val rangeVal = if (dataPoints.isNotEmpty()) maxVal - minVal else 0f
    val currentVal = dataPoints.lastOrNull() ?: 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "气密性测试",
            style = MaterialTheme.typography.displayLarge,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        if (isRunning) {
            Text("测试中，请按压屏幕...剩余 ${countdown}秒", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        } else if (testResult.isNotEmpty()) {
            Text(testResult, style = MaterialTheme.typography.titleLarge, color = if (testResult.contains("佳") || testResult.contains("好")) Color(0xFF34C759) else Color(0xFFFF3B30))
        } else {
            Text("点击启动后用力按压屏幕", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Buttons
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { isRunning = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isRunning) Color(0xFFD0E0FF) else Color(0xFFE5E5EA),
                    contentColor = if (!isRunning) Color(0xFF007AFF) else Color(0xFF8E8E93)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("暂停", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Divider(modifier = Modifier.height(30.dp).width(1.dp).align(Alignment.CenterVertically), color = Color(0xFFD1D1D6))
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { 
                    isRunning = true
                    if (!isRunning) dataPoints = emptyList() // clear on restart if needed, or just continue
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFD0E0FF) else Color(0xFFE5E5EA),
                    contentColor = if (isRunning) Color(0xFF007AFF) else Color(0xFF8E8E93)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("启动", style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Chart Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(top = 24.dp, bottom = 24.dp, start = 12.dp, end = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 32.dp, top = 20.dp) // space for Y axis and X axis labels
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        val gridLineColor = Color(0xFFD1D1D6)
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 30f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        
                        val yTextPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 30f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }

                        // Draw X axis grid and labels
                        for (i in 0..10) {
                            val x = canvasWidth * i / 10f
                            drawLine(
                                color = gridLineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, canvasHeight),
                                strokeWidth = 2f,
                                pathEffect = dashEffect
                            )
                            val label = "${i * 20}"
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                x,
                                -20f,
                                textPaint
                            )
                        }
                        
                        var yMin = minVal - 1f
                        var yMax = maxVal + 1f
                        if (dataPoints.isEmpty() || yMax - yMin < 10f) {
                            val center = if (dataPoints.isEmpty()) 1000f else (yMax + yMin) / 2f
                            yMin = center - 20f
                            yMax = center + 20f
                        }
                        
                        // Round to nice numbers
                        val minRounded = floor(yMin / 10f) * 10f
                        val maxRounded = ceil(yMax / 10f) * 10f
                        
                        val range = maxRounded - minRounded
                        
                        // Draw Y axis grid and labels
                        val numSteps = 5
                        for (i in 0..numSteps) {
                            val y = canvasHeight - (canvasHeight * i / numSteps.toFloat())
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 2f,
                                pathEffect = dashEffect
                            )
                            val yValue = minRounded + (range * i / numSteps.toFloat())
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format(Locale.US, "%.0f", yValue),
                                -15f,
                                y + 10f,
                                yTextPaint
                            )
                        }
                        
                        if (dataPoints.isEmpty()) return@Canvas
                        
                        val path = Path()
                        val stepX = canvasWidth / (maxDataPoints - 1)
                        
                        dataPoints.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = canvasHeight - ((value - minRounded) / range * canvasHeight).toFloat()
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        
                        drawPath(
                            path = path,
                            color = Color(0xFF007AFF),
                            style = Stroke(width = 6.dp.toPx())
                        )
                    }
                    
                    if (dataPoints.isNotEmpty()) {
                        Text(
                            text = String.format(Locale.US, "%.1fhPa", currentVal),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Black,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 10.dp, y = 10.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
        if (isRunning) {
            Text("测试中，请按压屏幕...剩余 ${countdown}秒", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        } else if (testResult.isNotEmpty()) {
            Text(testResult, style = MaterialTheme.typography.titleLarge, color = if (testResult.contains("佳") || testResult.contains("好")) Color(0xFF34C759) else Color(0xFFFF3B30))
        } else {
            Text("点击启动后用力按压屏幕", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
                
                // Legend
                Row(
                    modifier = Modifier.padding(start = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF007AFF)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("气压计", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
        if (isRunning) {
            Text("测试中，请按压屏幕...剩余 ${countdown}秒", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        } else if (testResult.isNotEmpty()) {
            Text(testResult, style = MaterialTheme.typography.titleLarge, color = if (testResult.contains("佳") || testResult.contains("好")) Color(0xFF34C759) else Color(0xFFFF3B30))
        } else {
            Text("点击启动后用力按压屏幕", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
                
                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("最大", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format(Locale.US, "%.1fhPa", if (dataPoints.isEmpty()) 0f else maxVal), style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                    }
                    Divider(modifier = Modifier.height(30.dp).width(1.dp), color = Color(0xFFE5E5EA))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("极差", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format(Locale.US, "%.1fhPa", if (dataPoints.isEmpty()) 0f else rangeVal), style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                    }
                    Divider(modifier = Modifier.height(30.dp).width(1.dp), color = Color(0xFFE5E5EA))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("最小", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format(Locale.US, "%.1fhPa", if (dataPoints.isEmpty()) 0f else minVal), style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "参考标准（结果仅供参考）：\n极差 > 2.0 hPa：气密性极佳\n极差 > 0.5 hPa：气密性良好\n极差 < 0.5 hPa：气密性极差 (气密受损或未用力按压)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}




