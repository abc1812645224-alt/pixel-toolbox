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

package com.example.pixeltoolbox.ui.custom

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import com.example.pixeltoolbox.ui.theme.GlassCard
import com.example.pixeltoolbox.ui.theme.iOSBackground
import com.example.pixeltoolbox.ui.theme.iOSBlue
import com.example.pixeltoolbox.ui.theme.iOSGreen
import com.example.pixeltoolbox.ui.theme.iOSLabel
import com.example.pixeltoolbox.ui.theme.iOSOrange
import com.example.pixeltoolbox.ui.theme.iOSRed
import com.example.pixeltoolbox.ui.theme.iOSSecondaryLabel
import com.example.pixeltoolbox.ui.theme.iOSSeparator
import com.example.pixeltoolbox.ui.theme.iOSButton
import com.example.pixeltoolbox.ui.theme.iOSOutlineButton
import com.example.pixeltoolbox.ui.theme.PixelToolboxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

class BatteryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelToolboxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = iOSBackground
                ) {
                    BatteryScreen()
                }
            }
        }
    }
}

data class BatteryInfo(
    val level: String = "--",
    val status: String = "--",
    val health: String = "--",
    val temp: String = "--",
    val voltage: String = "--",
    val current: String = "--",
    val chargeType: String = "--",
    val cycleCount: String = "--",
    val capacityNow: String = "--",
    val capacityDesign: String = "--",
    val healthPercent: String = "--",
    val chargingLimit: String = "--",
    val pluggedType: String = "--"
)

private fun readSysfs(path: String): String? {
    val res = ShizukuUtils.executeCommand("cat $path 2>/dev/null")
    return res.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

private fun collectBatteryInfo(context: Context): BatteryInfo {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    val rawLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val rawScale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val levelPct = if (rawLevel >= 0 && rawScale > 0) (rawLevel * 100 / rawScale).toString() + "%" else "--"

    val rawStatus = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val statusStr = when (rawStatus) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
        BatteryManager.BATTERY_STATUS_FULL -> "已充满"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "已充满"
        else -> "未知"
    }

    val rawHealth = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
    val healthStr = when (rawHealth) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
        BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
        BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
        else -> "未知"
    }

    val rawPlugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val pluggedStr = when {
        (rawPlugged and BatteryManager.BATTERY_PLUGGED_AC) != 0 -> "AC 电源"
        (rawPlugged and BatteryManager.BATTERY_PLUGGED_USB) != 0 -> "USB"
        (rawPlugged and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0 -> "无线充电"
        else -> "未连接"
    }

    val sysfsTemp = readSysfs("/sys/class/power_supply/battery/temp")?.let {
        "%.1f°C".format(it.toFloatOrNull()?.div(10f) ?: 0f)
    } ?: "--"

    val sysfsVoltage = readSysfs("/sys/class/power_supply/battery/voltage_now")?.let {
        "%.1fV".format(it.toFloatOrNull()?.div(1_000_000f) ?: 0f)
    } ?: "--"

    val sysfsCurrent = readSysfs("/sys/class/power_supply/battery/current_now")?.let { raw ->
        val ma = (raw.toFloatOrNull() ?: 0f) / 1000f
        if (ma > 0) "+%.0fmA".format(ma) else "%.0fmA".format(ma)
    } ?: "--"

    val sysfsChargeType = readSysfs("/sys/class/power_supply/battery/charge_type")?.let { raw ->
        when (raw) {
            "0" -> "未知 (0)"
            "1" -> "USB SDP (1)"
            "2" -> "USB CDP (2)"
            "3" -> "交流充电 DCP (3)"
            "4" -> "USB PD (4)"
            "5" -> "无线充电 (5)"
            else -> raw
        }
    } ?: "--"

    val sysfsCycleCount = readSysfs("/sys/class/power_supply/battery/cycle_count") ?: "--"

    val chargeFull = readSysfs("/sys/class/power_supply/battery/charge_full")?.toFloatOrNull() ?: 0f
    val chargeDesign = readSysfs("/sys/class/power_supply/battery/charge_full_design")?.toFloatOrNull() ?: 0f

    val capacityNowStr = if (chargeFull > 0) "%.0f mAh".format(chargeFull / 1000f) else "--"
    val capacityDesignStr = if (chargeDesign > 0) "%.0f mAh".format(chargeDesign / 1000f) else "--"
    val healthPctStr = if (chargeFull > 0 && chargeDesign > 0) {
        "%.1f%%".format(chargeFull / chargeDesign * 100f)
    } else "--"

    val chargeLimit = readSysfs("/sys/class/power_supply/battery/charge_limit")?.let {
        if (it.toIntOrNull()?.let { v -> v > 0 } == true) it + "%" else "未限制"
    } ?: "不支持"

    return BatteryInfo(
        level = levelPct,
        status = statusStr,
        health = healthStr,
        temp = sysfsTemp,
        voltage = sysfsVoltage,
        current = sysfsCurrent,
        chargeType = sysfsChargeType,
        cycleCount = sysfsCycleCount,
        capacityNow = capacityNowStr,
        capacityDesign = capacityDesignStr,
        healthPercent = healthPctStr,
        chargingLimit = chargeLimit,
        pluggedType = pluggedStr
    )
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = iOSSecondaryLabel)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor)
    }
}

@Composable
fun BatteryInfoItem(label: String, value: String, valueColor: Color = iOSLabel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = iOSSecondaryLabel)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor)
    }
}

@Composable
fun SectionTitle(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = iOSLabel)
        Text(desc, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = iOSSecondaryLabel)
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
fun BatteryScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var batteryInfo by remember { mutableStateOf(BatteryInfo()) }
    var statusMsg by remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                batteryInfo = collectBatteryInfo(context)
                delay(3000)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── 主卡片 ──
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // 标题
                Text("充电控制", style = MaterialTheme.typography.headlineMedium, color = iOSLabel)
                Text("实时电池信息与充电策略管理", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = iOSSecondaryLabel)
                Spacer(modifier = Modifier.height(20.dp))

                // ── 电量与状态 ──
                SectionTitle("电量与状态", "每 3 秒自动刷新")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BatteryInfoItem("当前电量", batteryInfo.level, iOSBlue)
                    BatteryInfoItem("充电状态", batteryInfo.status)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BatteryInfoItem("电源连接", batteryInfo.pluggedType)
                    BatteryInfoItem("充电类型", batteryInfo.chargeType)
                    BatteryInfoItem("充电限制", batteryInfo.chargingLimit)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = iOSSeparator, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ── 实时参数 ──
                SectionTitle("实时参数", "温度 / 电压 / 电流")
                val currentIsNegative = batteryInfo.current.startsWith("-")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BatteryInfoItem("温度", batteryInfo.temp)
                    BatteryInfoItem("电压", batteryInfo.voltage)
                    BatteryInfoItem("电流", batteryInfo.current, if (currentIsNegative) iOSOrange else iOSGreen)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = iOSSeparator, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ── 电池健康 ──
                SectionTitle("电池健康", "容量与循环寿命")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BatteryInfoItem("健康状态", batteryInfo.health)
                    BatteryInfoItem("健康度", batteryInfo.healthPercent)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BatteryInfoItem("当前容量", batteryInfo.capacityNow)
                    BatteryInfoItem("设计容量", batteryInfo.capacityDesign)
                    BatteryInfoItem("循环次数", batteryInfo.cycleCount)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = iOSSeparator, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ── 充电策略 ──
                SectionTitle("充电策略", "关闭系统充电限制以解除充电速度上限")

                iOSButton(
                    onClick = {
                        coroutineScope.launch {
                            val cmds = listOf(
                                "settings put secure adaptive_charging_enabled 0",
                                "settings put global battery_charge_director 0",
                                "settings put global battery_charge_director_game_cube 0"
                            )
                            val results = cmds.map { cmd -> ShizukuUtils.executeCommand(cmd) }
                            val allOk = results.all { it.isSuccess }
                            statusMsg = if (allOk) {
                                "加速充电已完成：已关闭自适应充电、充电分配器及游戏充电优化"
                            } else {
                                val errs = results.filter { it.isFailure }.map { it.exceptionOrNull()?.message }.filterNotNull()
                                "加速充电失败：${errs.joinToString("；")}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = iOSRed.copy(alpha = 0.85f)
                ) {
                    Text("加速充电", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "⚠ 加速充电将关闭自适应充电、充电分配器及游戏充电优化。可能导致电池温度升高、加速电池老化，长期使用可能影响电池寿命。本人自行承担风险。",
                    style = MaterialTheme.typography.labelSmall,
                    color = iOSRed,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                iOSOutlineButton(
                    onClick = {
                        coroutineScope.launch {
                            val cmds = listOf(
                                "settings put secure adaptive_charging_enabled 1",
                                "settings put global battery_charge_director 1",
                                "settings put global battery_charge_director_game_cube 1"
                            )
                            val results = cmds.map { cmd -> ShizukuUtils.executeCommand(cmd) }
                            val allOk = results.all { it.isSuccess }
                            statusMsg = if (allOk) {
                                "已恢复默认充电限制"
                            } else {
                                val errs = results.filter { it.isFailure }.map { it.exceptionOrNull()?.message }.filterNotNull()
                                "恢复默认失败：${errs.joinToString("；")}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("恢复默认", fontWeight = FontWeight.Medium, color = iOSSecondaryLabel)
                }

                if (statusMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(statusMsg, style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, lineHeight = 15.sp)
                }
            }
        }
    }
}
