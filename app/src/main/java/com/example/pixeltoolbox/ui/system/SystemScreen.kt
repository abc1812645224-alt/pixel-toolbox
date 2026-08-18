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

package com.example.pixeltoolbox.ui.system

import java.io.File
import java.io.FileOutputStream
import com.example.pixeltoolbox.ui.signal.ImsGroupSwitchRow
import com.example.pixeltoolbox.ExecutionLogCard
import com.example.pixeltoolbox.ui.signal.SignalScreen
import com.example.pixeltoolbox.ui.theme.AutoSizeText
import com.example.pixeltoolbox.ui.about.installDesktopLauncher
import com.example.pixeltoolbox.ui.about.createLockScreenShortcut
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
import androidx.compose.material.icons.filled.Check
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
import com.example.pixeltoolbox.ui.signal.ImsGroupSwitchRow
import com.example.pixeltoolbox.ExecutionLogCard
import com.example.pixeltoolbox.services.KeepAliveService
import com.example.pixeltoolbox.ui.system.AppItem
import com.example.pixeltoolbox.ui.system.loadInstalledApps
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import android.os.VibratorManager
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemScreen(
    executionLogs: List<String>,
    batTemp: Float, batVolt: Int, batteryStatus: Int, batCurrentNA: Int,
    dpiInput: String, setDpiInput: (String) -> Unit,
    context: Context, coroutineScope: CoroutineScope, addLog: (String) -> Unit,
    onOpenGpsTest: () -> Unit,
    onOpenBarometerTest: () -> Unit
) {    ExecutionLogCard(executionLogs)
    val iOSOrange = androidx.compose.ui.graphics.Color(0xFFFF9500)
    var hasShizuku by remember { mutableStateOf(ShizukuUtils.hasShizukuPermission()) }
    // removed unused updateShizuku variable
    Spacer(modifier = Modifier.height(16.dp))
    // 电池状态信息
    BatteryInfoCard(batTemp, batVolt, batteryStatus, batCurrentNA)
    Spacer(modifier = Modifier.height(16.dp))
    // ========== 暴力清理 ==========
    Spacer(modifier = Modifier.height(20.dp))
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("暴力清理", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Text("一键清理后台非系统进程与应用缓存", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 按钮 1：一键清后台
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val launcherResult = ShizukuUtils.executeCommand(
                                "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1"
                            )
                            val launcherPkg = launcherResult.getOrNull()?.trim().orEmpty()
                            val imeResult = ShizukuUtils.executeCommand("settings get secure default_input_method")
                            val imePkg = imeResult.getOrNull()?.trim()?.substringBefore("/").orEmpty()
                            val myPid = Process.myPid()
                            val psResult = ShizukuUtils.executeCommand("ps -A")
                            val psRaw = psResult.getOrNull() ?: ""
                            val psLines = if (psRaw.isNotBlank())
                                psResult.getOrNull()!!.lines().drop(1).filter { it.isNotBlank() }
                            else
                                emptyList()
                            if (psLines.isEmpty()) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(context, "无法获取进程列表，请确认 Shizuku 已授权", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            val whitelistNames = setOf(
                                "system_server", "zygote", "zygote64", "zygote32",
                                "surfaceflinger", "servicemanager", "hwservicemanager",
                                "audioserver", "cameraserver", "mediaserver", "drmserver",
                                "netd", "vold", "installd", "keystore",
                                "logd", "lmkd", "statsd", "storaged", "healthd"
                            )
                            fun isWhitelisted(name: String): Boolean = when {
                                name in whitelistNames -> true
                                name.startsWith("thermal") -> true
                                name.startsWith("android.") -> true
                                name.contains("com.android.systemui") -> true
                                name.contains("com.android.phone") -> true
                                name.contains("com.android.settings") -> true
                                name.contains("com.example.pixeltoolbox") -> true
                                name.contains("com.xiaomi.xmsf") -> true
                                name.contains("moe.shizuku") -> true
                                name.contains("com.android.bluetooth") -> true
                                name.contains("com.google.android.gms") -> true
                                name.contains("com.google.android.gsf") -> true
                                name.contains("com.android.providers.media") -> true
                                imePkg.isNotEmpty() && name.contains(imePkg) -> true
                                launcherPkg.isNotEmpty() && name.contains(launcherPkg) -> true
                                else -> false
                            }
                            val packagesToKill = linkedSetOf<String>()
                            for (line in psLines) {
                                val cols = line.trim().split(Regex("\\s+"))
                                if (cols.size < 9) continue
                                val pid = cols[1]
                                val name = cols.last()
                                if (pid.toIntOrNull() == myPid) continue
                                if (isWhitelisted(name)) continue
                                val basePkg = name.substringBefore(":")
                                if (basePkg.contains(".")) {
                                    packagesToKill.add(basePkg)
                                }
                            }
                            if (packagesToKill.isEmpty()) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(context, "没有可清理的进程", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            val killCmd = packagesToKill.joinToString(" & ") { "am force-stop $it" } + " & wait"
                            ShizukuUtils.executeCommand(killCmd)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                Toast.makeText(context, "已清理 ${packagesToKill.size} 个应用", Toast.LENGTH_SHORT).show()
                                addLog("暴力清后台：已清理 ${packagesToKill.size} 个应用")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE53E3E)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("清理后台", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
                // 按钮 2：一键清缓存
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val result = ShizukuUtils.executeCommand("pm trim-caches 999G")
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (result.isSuccess) {
                                    Toast.makeText(context, "缓存已清理", Toast.LENGTH_SHORT).show()
                                    addLog("缓存已清理")
                                } else {
                                    Toast.makeText(context, "清理缓存失败，请确认 Shizuku 已授权", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFED8936)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("清理缓存", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // GPS 测试
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("GPS 测试", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Text("实时查看卫星分布、信号强度与定位数据", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
            Spacer(modifier = Modifier.height(12.dp))
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenGpsTest
            ) { Text("GPS 测试", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    var currentCpuMode by remember { mutableStateOf("default") }
    var currentVibLevel by remember { mutableStateOf(2) }
    
    LaunchedEffect(Unit) {
        if (com.example.pixeltoolbox.shizuku.ShizukuUtils.hasShizukuPermission()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // 读取振动级别
                val res = com.example.pixeltoolbox.shizuku.ShizukuUtils.executeCommand("settings get system hardware_haptic_feedback_intensity").getOrNull()?.trim()
                val level = res?.toIntOrNull()
                if (level != null && level in 0..3) {
                    currentVibLevel = level
                }
                
                // 读取真实CPU模式：以系统真实状态为准。
                // 省电 = cmd power get-mode == 1（系统真实省电状态）
                // 性能 = cmd power get-fixed-performance-mode-enabled == true/1（系统真实固定性能模式）
                // 标记文件仅作辅助/兜底，避免 UI 与实际状态脱节
                val realModeRes = com.example.pixeltoolbox.shizuku.ShizukuUtils.executeCommand("cmd power get-mode 2>/dev/null").getOrNull()?.trim()
                val perfModeRes = com.example.pixeltoolbox.shizuku.ShizukuUtils.executeCommand("cmd power get-fixed-performance-mode-enabled 2>/dev/null").getOrNull()?.trim()
                val cpuModeRes = com.example.pixeltoolbox.shizuku.ShizukuUtils.executeCommand("cat /data/local/tmp/pixel_cpu_mode").getOrNull()?.trim()
                val perfRealOn = perfModeRes == "true" || perfModeRes == "1" || perfModeRes == "enabled"
                currentCpuMode = when {
                    realModeRes == "1" -> "saver"
                    perfRealOn -> "performance"
                    cpuModeRes == "performance" -> "performance"
                    else -> "default"
                }
            }
        }
    }
    // CPU & 系统性能调度模式
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("CPU & 系统性能调度模式", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Text("请检查CPU使用情况与功耗设置", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "saver" to "省电",
                    "default" to "默认",
                    "performance" to "性能"
                ).forEach { (mode, label) ->
                    val cmd = when (mode) {
                        "saver" -> "cmd power set-fixed-performance-mode-enabled false 2>/dev/null; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo powersave > \$g 2>/dev/null; done; settings put system min_refresh_rate 60 2>/dev/null; settings put system peak_refresh_rate 60 2>/dev/null; echo 'saver' > /data/local/tmp/pixel_cpu_mode"
                        "performance" -> "cmd power set-mode 0 2>/dev/null; cmd power set-fixed-performance-mode-enabled true 2>/dev/null; r=\$(settings get system peak_refresh_rate 2>/dev/null); [ \"\$r\" = \"null\" ] && r=120; [ -z \"\$r\" ] && r=120; settings delete system min_refresh_rate 2>/dev/null; settings put system peak_refresh_rate \$r 2>/dev/null; echo 'performance' > /data/local/tmp/pixel_cpu_mode"
                        else -> "cmd power set-mode 0 2>/dev/null; cmd power set-fixed-performance-mode-enabled false 2>/dev/null; settings delete system min_refresh_rate 2>/dev/null; settings delete system peak_refresh_rate 2>/dev/null; echo 'default' > /data/local/tmp/pixel_cpu_mode"
                    }
                    val successMsg = when (mode) {
                        "saver" -> "省电模式 (CPU降频 + 60Hz刷新率)"
                        "performance" -> "性能模式 (性能锁频 + 动态高刷新率)"
                        else -> "均衡默认模式 (恢复系统调度与刷新率)"
                    }
                    val onClick = fun() {
                        if (!ShizukuUtils.hasShizukuPermission()) {
                            Toast.makeText(context, "请先授权 Shizuku 权限", Toast.LENGTH_LONG).show()
                            return
                        }
                        currentCpuMode = mode
                        coroutineScope.launch {
                            val result = ShizukuUtils.executeCommand(cmd)
                            result.onSuccess {
                                addLog(successMsg)
                                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                            }.onFailure { e ->
                                val errMsg = e.message ?: "未知错误"
                                addLog("失败: $errMsg")
                                Toast.makeText(context, "执行失败: $errMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    if (currentCpuMode == mode) {
                        iOSButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onClick() }
                        ) { AutoSizeText(label, color = Color.White, style = MaterialTheme.typography.labelSmall) }
                    } else {
                        iOSOutlineButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onClick() }
                        ) { AutoSizeText(label, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    // DNS 优化
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("DNS 网络加密加速", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Text("一键防 DNS 劫持，加速域名解析与防弹窗", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                iOSButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val cmd = "settings put global private_dns_mode hostname && settings put global private_dns_specifier dns.alidns.com"
                            handleResult(context, ShizukuUtils.executeCommand(cmd), "阿里 DNS 已开启", addLog)
                        }
                    }
                ) { Text("阿里\nDNS", color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) }
                iOSButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val cmd = "settings put global private_dns_mode hostname && settings put global private_dns_specifier dot.pub"
                            handleResult(context, ShizukuUtils.executeCommand(cmd), "腾讯 DNS 已开启", addLog)
                        }
                    }
                ) { Text("腾讯\nDNS", color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) }
                iOSButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val cmd = "settings put global private_dns_mode hostname && settings put global private_dns_specifier dns.adguard.com"
                            handleResult(context, ShizukuUtils.executeCommand(cmd), "去广告 DNS 已开启", addLog)
                        }
                    }
                ) { Text("全局\n去广", color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) }
                iOSButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val cmd = "settings put global private_dns_mode opportunistic"
                            handleResult(context, ShizukuUtils.executeCommand(cmd), "默认 DNS 已恢复", addLog)
                        }
                    }
                ) { Text("恢复\n默认", color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    // Pixel 震动反馈
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Pixel 触觉震动强度调校", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Text("调整打字、触摸与通知系统级触感震动百分比", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "关闭", 1 to "柔和", 2 to "标准", 3 to "强劲").forEach { (level, label) ->
                    val onSelect = fun() {
                        if (!ShizukuUtils.hasShizukuPermission()) {
                            Toast.makeText(context, "请先授权 Shizuku 权限", Toast.LENGTH_LONG).show()
                            return
                        }
                        if (level > 0) {
                            val vib = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
} else {
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
}
                            if (vib == null) {
                                Toast.makeText(context, "设备无震动器", Toast.LENGTH_LONG).show()
                                return
                            }
                            if (!vib.hasVibrator()) {
                                Toast.makeText(context, "震动器不支持", Toast.LENGTH_LONG).show()
                                return
                            }
                            try {
                                when (level) {
                                    1 -> vib.vibrate(VibrationEffect.createOneShot(30L, 128))
                                    2 -> vib.vibrate(VibrationEffect.createWaveform(
                                        longArrayOf(30, 40, 60), intArrayOf(128, 0, 192), -1))
                                    3 -> vib.vibrate(VibrationEffect.createWaveform(
                                        longArrayOf(30, 40, 80, 50, 120), intArrayOf(128, 0, 200, 0, 255), -1))
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "震动失败: ${e.message}", Toast.LENGTH_LONG).show()
                                return
                            }
                        }
                        currentVibLevel = level
                        coroutineScope.launch {
                            Toast.makeText(context, "正在设置 $label...", Toast.LENGTH_SHORT).show()
                            val cmd = "settings put secure haptic_feedback_intensity $level 2>/dev/null; " +
                                      "settings put system haptic_feedback_intensity $level 2>/dev/null; " +
                                      "settings put system hardware_haptic_feedback_intensity $level 2>/dev/null"
                            handleResult(context, ShizukuUtils.executeCommand(cmd), "触控强度已设置: $label", addLog)
                        }
                    }
                    if (currentVibLevel == level) {
                        iOSButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect() }
                        ) { Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall) }
                    } else {
                        iOSOutlineButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect() }
                        ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    // 气密性测试
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("气密性测试", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Spacer(modifier = Modifier.height(12.dp))
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenBarometerTest
            ) { Text("进入测试页面", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    // 双击桌面锁屏
    var showLockScreenOptions by remember { mutableStateOf(false) }
    if (showLockScreenOptions) {
        var isInstallingLauncher by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isInstallingLauncher) showLockScreenOptions = false },
            title = { Text(text = if (isInstallingLauncher) "正在安装" else "选择锁屏方案") },
            text = { 
                if (isInstallingLauncher) {
                    Text("请稍候，正在安装并配置纯净锁屏桌面...")
                } else {
                    Text("方案1：创建桌面快捷方式\n方案2：一键安装纯净锁屏桌面") 
                }
            },
            confirmButton = {
                if (!isInstallingLauncher) {
                    TextButton(onClick = {
                        showLockScreenOptions = false
                        createLockScreenShortcut(context)
                    }) {
                        Text("方案1")
                    }
                }
            },
            dismissButton = {
                if (!isInstallingLauncher) {
                    TextButton(onClick = { 
                        isInstallingLauncher = true
                        coroutineScope.launch {
                            installDesktopLauncher(context)
                            isInstallingLauncher = false
                            showLockScreenOptions = false
                        }
                    }) {
                        Text("方案2")
                    }
                }
            }
        )
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("双击桌面锁屏", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
                Text("提供两种完美的息屏锁屏方案", style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel)
            }
            iOSButton(
                onClick = { showLockScreenOptions = true }
            ) {
                Text("选择方案", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    // DPI 定制
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("屏幕深度定制", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = dpiInput,
                    onValueChange = setDpiInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入新 DPI (如 420)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.width(8.dp))
                iOSButton(
                    onClick = {
                        coroutineScope.launch {
                            val dpi = dpiInput.toIntOrNull()
                            if (dpi != null && dpi in 100..1000) {
                                handleResult(context, ShizukuUtils.executeCommand("wm density $dpi"), "DPI 已修改", addLog)
                            } else {
                                Toast.makeText(context, "请输入有效的 DPI 值 (100 -> 1000)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { Text("应用", color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            iOSOutlineButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    coroutineScope.launch {
                        handleResult(context, ShizukuUtils.executeCommand("wm density reset"), "DPI 已重置", addLog)
                    }
                }
            ) { Text("恢复默认", fontWeight = FontWeight.SemiBold) }
        }
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("通话录音", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
                    Text("Shizuku 通话录音 · 来电/去电自动录音", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
                    Text("重装 App 后需重新授权，否则通话检测不生效", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
                }
                IconButton(onClick = {
                    context.startActivity(Intent(context, CallRecordingSettingsActivity::class.java))
                }) {
                    Icon(Icons.Filled.Settings, contentDescription = "通话录音设置", tint = iOSBlue)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val prefs = remember { AppPreferences(context) }
            var recorderEnabled by remember { mutableStateOf(prefs.isCallRecorderEnabled()) }
            var autoIncoming by remember { mutableStateOf(prefs.isAutoRecordIncomingEnabled()) }
            var autoOutgoing by remember { mutableStateOf(prefs.isAutoRecordOutgoingEnabled()) }
            var shizukuReady by remember { mutableStateOf(ShizukuUtils.hasShizukuPermission() && ManageOngoingCalls.isGranted(context)) }
            var granting by remember { mutableStateOf(false) }
            ImsGroupSwitchRow(
                label = "录音总开关",
                checked = recorderEnabled,
                textColor = iOSBlue,
                onCheckedChange = { enable ->
                    recorderEnabled = enable
                    prefs.setCallRecorderEnabled(enable)
                    if (!enable) shizukuReady = ShizukuUtils.hasShizukuPermission()
                }
            )
            ImsGroupSwitchRow(
                label = "来电自动录音",
                checked = autoIncoming,
                textColor = iOSBlue,
                onCheckedChange = { enable ->
                    autoIncoming = enable
                    prefs.setAutoRecordIncomingEnabled(enable)
                }
            )
            ImsGroupSwitchRow(
                label = "去电自动录音",
                checked = autoOutgoing,
                textColor = iOSBlue,
                onCheckedChange = { enable ->
                    autoOutgoing = enable
                    prefs.setAutoRecordOutgoingEnabled(enable)
                }
            )
            if (recorderEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                if (!ShizukuUtils.hasShizukuPermission()) {
                    Text(
                        "Shizuku 未授权：请先启动 Shizuku 并在其中授予本应用权限",
                        color = iOSOrange, style = MaterialTheme.typography.bodySmall
                    )
                } else if (!shizukuReady) {
                    Text(
                        "需要 Shizuku 授权「管理进行中的通话」(manage_ongoing_calls) 才能自动录音",
                        color = iOSOrange, style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    iOSButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            granting = true
                            coroutineScope.launch {
                                val ok = ManageOngoingCalls.grant(context)
                                granting = false
                                shizukuReady = ShizukuUtils.hasShizukuPermission() && ManageOngoingCalls.isGranted(context)
                                Toast.makeText(context, if (ok) "已授权 manage_ongoing_calls，自动录音已就绪" else "授权失败，请确认 Shizuku 服务已启动", Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Text(if (granting) "授权中..." else "一键授权", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    // 物理级强制保活
    var selectedApps by remember { mutableStateOf(emptyList<AppItem>()) }
    var isKeepAliveEnabled by remember { mutableStateOf(KeepAliveService.isRunning) }
    var showAppPicker by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var loadingApps by remember { mutableStateOf(false) }
    val pm = context.packageManager
    
    if (showAppPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAppPicker = false },
            sheetState = sheetState,
            containerColor = iOSCardBackground,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showAppPicker = false }) {
                        Text("取消", color = iOSBlue, fontSize = 17.sp)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "选择保活应用",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = iOSLabel
                        )
                        Text(
                            "最多 3 个",
                            style = MaterialTheme.typography.labelSmall,
                            color = iOSSecondaryLabel
                        )
                    }
                    TextButton(onClick = { showAppPicker = false }) {
                        Text("完成", color = iOSBlue, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
                
                if (loadingApps) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = iOSBlue)
                    }
                } else {
                    GlassCard(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(installedApps, key = { it.packageName }) { app ->
                                val isSelected = selectedApps.any { it.packageName == app.packageName }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            if (isSelected) {
                                                selectedApps = selectedApps.filter { it.packageName != app.packageName }
                                            } else {
                                                if (selectedApps.size < 3) {
                                                    selectedApps = selectedApps + app
                                                } else {
                                                    Toast.makeText(context, "最多只能选择3个应用", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }.padding(vertical = 12.dp, horizontal = 16.dp)
                                    ) {
                                        app.icon?.let { icon ->
                                            Image(
                                                bitmap = icon,
                                                contentDescription = "${app.name} icon",
                                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(app.name, fontWeight = FontWeight.SemiBold, color = iOSLabel, fontSize = 16.sp)
                                            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = iOSBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Divider(color = iOSSeparator, thickness = 0.5.dp, modifier = Modifier.padding(start = 68.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("物理级强制保活 (免 Root)", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Spacer(modifier = Modifier.height(6.dp))
            Text("【高耗电警告】通过定时唤醒锁强制拉起 CPU，防止消息延迟。重启后失效。", style = MaterialTheme.typography.labelMedium, color = iOSRed)
            Spacer(modifier = Modifier.height(12.dp))
            
            // 已选应用展示
            Text("已选应用：", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            if (selectedApps.isEmpty()) {
                Text("未选择任何应用", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            } else {
                selectedApps.forEach { app ->
                    val appIcon = remember(app.packageName) {
                        runCatching {
                            context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
                        }.getOrNull()
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = "${app.name} icon",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("• ${app.name}", style = MaterialTheme.typography.bodyMedium, color = iOSLabel)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!isKeepAliveEnabled) {
                iOSOutlineButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        loadingApps = true
                        showAppPicker = true
                        coroutineScope.launch {
                            if (installedApps.isEmpty()) {
                                installedApps = loadInstalledApps(pm)
                            }
                            loadingApps = false
                        }
                    }
                ) { Text("+ 添加 / 修改应用", fontWeight = FontWeight.SemiBold) }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (isKeepAliveEnabled) {
                iOSOutlineButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(context, KeepAliveService::class.java)
                        context.stopService(intent)
                        isKeepAliveEnabled = false
                        KeepAliveService.isRunning = false
                        Toast.makeText(context, "已停止强制保活", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("停止唤醒", color = iOSRed, fontWeight = FontWeight.SemiBold) }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (selectedApps.isEmpty()) {
                            Toast.makeText(context, "请先选择至少一个应用", Toast.LENGTH_SHORT).show()
                            return@Surface
                        }
                        val intent = Intent(context, KeepAliveService::class.java).apply {
                            putExtra("PACKAGE_NAMES", selectedApps.map { it.packageName }.toTypedArray())
                            putExtra("INTERVAL_MINS", 3)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        isKeepAliveEnabled = true
                        KeepAliveService.isRunning = true
                        Toast.makeText(context, "开启 3分钟/次 的后台唤醒", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = iOSRed
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("开启强制唤醒", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
}







