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

package com.example.pixeltoolbox.ui.tools

import com.example.pixeltoolbox.BuildConfig
import com.example.pixeltoolbox.R

import java.io.File
import java.io.FileOutputStream
import com.example.pixeltoolbox.ui.signal.ImsGroupSwitchRow
import com.example.pixeltoolbox.ui.signal.SignalScreen
import com.example.pixeltoolbox.ui.system.SystemScreen
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.core.app.ActivityCompat
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import com.example.pixeltoolbox.shizuku.SimSlotInfo
import com.example.pixeltoolbox.ui.theme.GlassCard
import com.example.pixeltoolbox.ui.theme.iOSButton
import com.example.pixeltoolbox.ui.theme.iOSOutlineButton
import com.example.pixeltoolbox.ui.theme.iOSBackground
import com.example.pixeltoolbox.ui.theme.iOSLabel
import com.example.pixeltoolbox.ui.theme.iOSSecondaryLabel
import com.example.pixeltoolbox.ui.theme.iOSNavUnselected
import com.example.pixeltoolbox.ui.theme.iOSBlue
import com.example.pixeltoolbox.ui.theme.iOSGreen
import com.example.pixeltoolbox.ui.theme.iOSRed
import com.example.pixeltoolbox.ui.theme.iOSCardBackground
import com.example.pixeltoolbox.ui.theme.iOSSeparator
import com.example.pixeltoolbox.ui.theme.PixelToolboxTheme
import com.example.pixeltoolbox.signal.SignalMonitor
import com.example.pixeltoolbox.signal.SignalDashboardState
import com.example.pixeltoolbox.signal.SignalMetrics
import com.example.pixeltoolbox.signal.NetworkMetrics
import com.example.pixeltoolbox.signal.DeviceMetrics
import com.example.pixeltoolbox.signal.TrafficMetrics
import com.example.pixeltoolbox.signal.SystemMetrics
import com.example.pixeltoolbox.ui.signal.SignalDashboard
import com.example.pixeltoolbox.ui.geektools.GeekToolsCard
import com.example.pixeltoolbox.ui.geektools.SectionTitle
import rikka.shizuku.Shizuku
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.PrintWriter
import java.io.StringWriter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.pixeltoolbox.data.AppPreferences
import com.example.pixeltoolbox.services.recording.ManageOngoingCalls
import com.example.pixeltoolbox.ui.custom.CallRecordingSettingsActivity
import com.example.pixeltoolbox.ExecutionLogCard
import com.example.pixeltoolbox.ui.system.SystemScreen
import androidx.compose.material3.MaterialTheme

@Composable
fun ToolboxScreen(
    executionLogs: List<String>,
    terminalInput: String, setTerminalInput: (String) -> Unit,
    terminalOutput: String, setTerminalOutput: (String) -> Unit,
    context: Context, coroutineScope: CoroutineScope, addLog: (String) -> Unit,
    onOpenBootManager: () -> Unit
) {
    ExecutionLogCard(executionLogs)
    Spacer(modifier = Modifier.height(16.dp))
    // 极客工具箱大满贯
    GeekToolsCard(context = context, textColor = iOSLabel, addLog = addLog, onOpenBootManager = onOpenBootManager)
    Spacer(modifier = Modifier.height(16.dp))
    // 极客终端 (ADB Shell)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("极客终端", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFF1A1A1A), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = terminalOutput,
                    color = Color(0xFF00FF00),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = terminalInput,
                    onValueChange = setTerminalInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入 shell 命令...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            coroutineScope.launch {
                                val result = ShizukuUtils.executeCommand(terminalInput)
                                setTerminalOutput("$> $terminalInput\n${result.getOrDefault(result.exceptionOrNull()?.message ?: "")}\n\n$terminalOutput")
                                setTerminalInput("")
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                iOSButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = ShizukuUtils.executeCommand(terminalInput)
                            setTerminalOutput("$> $terminalInput\n${result.getOrDefault(result.exceptionOrNull()?.message ?: "")}\n\n$terminalOutput")
                            setTerminalInput("")
                        }
                    }
                ) { Text("执行", color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    DonateCard()
    Spacer(modifier = Modifier.height(16.dp))
    QqGroupCard()
}
@Composable
fun DonateCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("赞助开发者", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Text("如果这个小工具帮到了您，\n欢迎微信扫码赞赏支持！\n\n温馨提示：重启手机后，部分底层功能可能会失效或还原，这是正常现象，重新进入本工具再次开启即可", style = MaterialTheme.typography.labelLarge, color = iOSSecondaryLabel, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.wechat_donate),
                contentDescription = "赞助二维码",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("http://www.coolapk.com/u/758776"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, iOSBlue)
            ) {
                Text("关注开发者(酷安主页)", color = iOSBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
fun QqGroupCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("加入QQ群", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "群号：607463891\n点击下方按钮快捷入群",
                style = MaterialTheme.typography.bodyLarge,
                color = iOSSecondaryLabel,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://qm.qq.com/q/37saPikmxO")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
            ) {
                Text("一键加群", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
