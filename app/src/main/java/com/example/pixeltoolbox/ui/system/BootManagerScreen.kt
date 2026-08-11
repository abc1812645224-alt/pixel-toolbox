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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import androidx.compose.material3.MaterialTheme

@Composable
fun BootManagerScreen(context: Context, addLog: (String) -> Unit, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var receivers by remember { mutableStateOf<List<BootReceiverItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var restrictedPkgs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDisabledOnly by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
            .statusBarsPadding()
    ) {
        // 顶部导航栏 - iOS 风格
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("自启管理", style = MaterialTheme.typography.headlineMedium, color = iOSLabel)
        }
        // 内容区
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text("限制应用后台运行以防止自启，不影响手动打开", style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel)
            Text("仅扫描第三方应用，跳过系统关键组件", style = MaterialTheme.typography.labelSmall, color = iOSRed)
            Text("效果弱于冻结（需 Root），但可正常使用应用", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9500))
            Spacer(modifier = Modifier.height(16.dp))
            if (receivers.isEmpty() && !isLoading) {
                iOSButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            val items = withContext(kotlinx.coroutines.Dispatchers.IO) { scanBootReceivers(context) }
                            receivers = items
                            // 恢复已限制的应用状态
        val pkgs = items.map { it.packageName }.distinct()
                            val denied = mutableSetOf<String>()
                            for (pkg in pkgs) {
                                val output = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    ShizukuUtils.executeCommand("cmd appops get $pkg RUN_IN_BACKGROUND")
                                }
                                if (output.isSuccess && output.getOrDefault("").toString().contains("deny")) {
                                    denied.add(pkg)
                                }
                            }
                            restrictedPkgs = denied
                            isLoading = false
                            addLog("扫描完成：发现 ${items.size} 个接收器，${denied.size} 个已限制")
                        }
                    }
                ) { Text("批量限制应用", color = Color.White, fontWeight = FontWeight.SemiBold) }
            } else if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = iOSBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("正在扫描...", style = MaterialTheme.typography.bodyLarge, color = iOSSecondaryLabel)
                }
            } else {
                // 操作栏：重新扫描 + 全部禁用 + 筛选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iOSOutlineButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isLoading = true
                            receivers = emptyList()
                            restrictedPkgs = emptySet()
                            showDisabledOnly = false
                            coroutineScope.launch {
                                val items = withContext(kotlinx.coroutines.Dispatchers.IO) { scanBootReceivers(context) }
                                receivers = items
                            // 恢复已限制的应用状态
        val pkgs = items.map { it.packageName }.distinct()
                                val denied = mutableSetOf<String>()
                                for (pkg in pkgs) {
                                    val output = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        ShizukuUtils.executeCommand("cmd appops get $pkg RUN_IN_BACKGROUND")
                                    }
                                    if (output.isSuccess && output.getOrDefault("").toString().contains("deny")) {
                                        denied.add(pkg)
                                    }
                                }
                                restrictedPkgs = denied
                                isLoading = false
                                addLog("重新扫描完成：发现 ${items.size} 个接收器，${denied.size} 个已限制")
                            }
                        }
                    ) { Text("重新扫描", style = MaterialTheme.typography.labelMedium) }
                    iOSButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            coroutineScope.launch {
                                // 鎸夊寘鍚嶅幓
                                val pkgsToRestrict = receivers
                                    .filter { it.packageName !in restrictedPkgs }
                                    .distinctBy { it.packageName }
                                if (pkgsToRestrict.isEmpty()) {
                                    addLog("没有需限制的应用")
                                    return@launch
                                }
                                addLog("开始批量限制 ${pkgsToRestrict.size} 个应用")
                                var successCount = 0
                                for (item in pkgsToRestrict) {
                                    val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        ShizukuUtils.executeCommand("cmd appops set ${item.packageName} RUN_IN_BACKGROUND deny")
                                    }
                                    if (result.isSuccess) {
                                        restrictedPkgs = restrictedPkgs + item.packageName
                                        successCount++
                                    }
                                }
                                addLog("全部限制完成：成功 {successCount / ${pkgsToRestrict.size}")
                            }
                        }
                    ) { Text("全部限制", color = Color.White, style = MaterialTheme.typography.labelMedium) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                // 杩囨护鍣?
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "已限制 ${restrictedPkgs.size} / ${receivers.distinctBy { it.packageName }.size } 个应用",
                        style = MaterialTheme.typography.bodyLarge, color = iOSSecondaryLabel
                    )
                    TextButton(onClick = { showDisabledOnly = !showDisabledOnly }) {
                        Text(
                            if (showDisabledOnly) "显示全部" else "仅看已限制",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (showDisabledOnly) iOSBlue else iOSSecondaryLabel,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val filtered = if (showDisabledOnly)
                    receivers.filter { it.packageName in restrictedPkgs }
                else receivers.filter { it.packageName !in restrictedPkgs }
                if (filtered.isEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("暂无已限制的应用", style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel)
                }
                var showAll by remember { mutableStateOf(false) }
                val displayed = if (showAll) filtered else filtered.take(15)
                displayed.forEachIndexed { idx, item ->
                    val isRestricted = restrictedPkgs.contains(item.packageName)
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 应用图标
                            if (item.iconBitmap != null) {
                                Image(
                                    bitmap = item.iconBitmap.asImageBitmap(),
                                    contentDescription = item.appLabel,
                                    modifier = Modifier.size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(iOSSeparator.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("?", style = MaterialTheme.typography.titleMedium, color = iOSSecondaryLabel)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.appLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isRestricted) iOSRed else iOSLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.componentShort,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRestricted) iOSRed.copy(alpha = 0.6f) else iOSSecondaryLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = !isRestricted,
                                onCheckedChange = { enable ->
                                    coroutineScope.launch {
                                        val cmd = if (enable) "cmd appops set ${item.packageName} RUN_IN_BACKGROUND allow"
                                                  else "cmd appops set ${item.packageName} RUN_IN_BACKGROUND deny"
                                        val result = withContext(kotlinx.coroutines.Dispatchers.IO) { ShizukuUtils.executeCommand(cmd) }
                                        result.onSuccess {
                                            restrictedPkgs = if (enable)
                                                restrictedPkgs - item.packageName
                                            else
                                                restrictedPkgs + item.packageName
                                            addLog("${if (enable) "已解除限制" else "已限制"} ${item.appLabel}")
                                        }.onFailure { e ->
                                            addLog("操作失败: ${e.message}")
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = iOSBlue,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = iOSRed.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
                if (filtered.size > 15) {
                    Spacer(modifier = Modifier.height(8.dp))
                    iOSOutlineButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showAll = !showAll }
                    ) { Text(if (showAll) "收起列表" else "显示全部 (${filtered.size})", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
data class BootReceiverItem(
    val packageName: String,
    val appLabel: String,
    val fullComponent: String,
    val componentShort: String,
    val iconBitmap: android.graphics.Bitmap?
)
private fun scanBootReceivers(context: Context): List<BootReceiverItem> {
    val output = ShizukuUtils.executeCommand("pm query-receivers -a android.intent.action.BOOT_COMPLETED --brief")
        .getOrElse { return emptyList() }
    val lines = output.lines().map { it.trim() }.filter { it.isNotEmpty() && it.contains("/") }
    // 鎸夊寘鍚嶅垎
    val byPkg = linkedMapOf<String, MutableList<String>>()
    for (line in lines) {
        val pkg = line.substringBefore("/")
        byPkg.getOrPut(pkg) { mutableListOf() }.add(line)
    }
    val pm = context.packageManager
    val items = mutableListOf<BootReceiverItem>()
    for ((pkg, components) in byPkg) {
        // 跳过系统应用
        try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) continue
        } catch (e: Exception) { continue }
        val label = try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) { pkg }
        // 加载应用图标
        val icon: android.graphics.Bitmap? = try {
            val drawable = pm.getApplicationIcon(pkg)
            val bmp = android.graphics.Bitmap.createBitmap(72, 72, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, 72, 72)
            drawable.draw(canvas)
            bmp
        } catch (e: Exception) { null }
        for (comp in components) {
            val shortName = comp.substringAfter("/")
            items.add(BootReceiverItem(pkg, label, comp, shortName, icon))
        }
    }
    items.sortBy { it.appLabel.lowercase() }
    return items
}
@Composable
fun BatteryData(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = iOSSecondaryLabel)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = iOSLabel)
    }
}
@Composable
fun BatteryInfoCard(
    batTemp: Float, batVolt: Int, batteryStatus: Int, batCurrentNA: Int
) {
    var localVoltage by remember { mutableStateOf(batVolt) }
    var localCurrent by remember { mutableStateOf(batCurrentNA) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                val voltResult = ShizukuUtils.executeCommand("cat /sys/class/power_supply/battery/voltage_now")
                voltResult.onSuccess { v ->
                    val uv = v.trim().toIntOrNull()
                    if (uv != null && uv > 0) localVoltage = uv
                }
                val curResult = ShizukuUtils.executeCommand("cat /sys/class/power_supply/battery/current_now")
                curResult.onSuccess { c ->
                    val ua = c.trim().toIntOrNull()
                    if (ua != null) localCurrent = ua
                }
            }
        }
    }
    // 当外部 batVolt/batCurrentNA 更新时同步（BatteryManager 优先）
    LaunchedEffect(batVolt) {
        if (batVolt > 0) localVoltage = batVolt * 1000 // mV → µV for consistency
    }
    LaunchedEffect(batCurrentNA) {
        if (batCurrentNA != 0) localCurrent = batCurrentNA / 1000 // nA → µA，与 sysfs 统一单位
    }
    val voltageV: String = if (localVoltage > 0) {
        String.format("%.3f", localVoltage / 1000000f) + "V"
    } else "--"
    val currentStr: String = if (localCurrent != 0) {
        val ma = kotlin.math.abs(localCurrent / 1000f)
        if (ma >= 1000f) String.format("%.2f", ma / 1000f) + "A" else String.format("%.0f", ma) + "mA"
    } else "--"
    val powerW: String = if (localVoltage > 0 && localCurrent != 0) {
        val v = localVoltage / 1000000f
        val a = kotlin.math.abs(localCurrent / 1000f) / 1000f
        String.format("%.2f", v * a) + "W"
    } else "--"
    val tempStr = "$batTemp °C"
    val statusStr = when (batteryStatus) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
        BatteryManager.BATTERY_STATUS_FULL -> "已充满"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "已充满"
        else -> "未知($batteryStatus)"
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("电池实时信息", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BatteryData("温度", tempStr)
                BatteryData("状态", statusStr)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BatteryData("电压", voltageV)
                BatteryData("电流", currentStr)
                BatteryData("功率", powerW)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("每 2 秒刷新", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
fun handleResult(context: Context, result: Result<String>, successMsg: String, addLog: (String) -> Unit) {
    result.onSuccess {
        addLog(successMsg)
        Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
    }.onFailure {
        val errMsg = it.message ?: "未知错误"
        addLog("失败: $errMsg")
        Toast.makeText(context, "执行失败: $errMsg", Toast.LENGTH_LONG).show()
    }
}
@Composable
fun ImsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = iOSLabel)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iOSBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = iOSSeparator
            )
        )
    }
}
// ======================= ABOUT SCREEN =======================
