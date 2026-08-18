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

package com.example.pixeltoolbox.ui.about

import com.example.pixeltoolbox.BuildConfig
import com.example.pixeltoolbox.R
import com.example.pixeltoolbox.LockScreenActivity

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
import androidx.compose.material3.MaterialTheme

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showExportButton by remember { mutableStateOf(false) }
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon + Name + Version
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime > 8000) {
                                tapCount = 0
                            }
                            tapCount++
                            lastTapTime = now
                            if (tapCount >= 8) {
                                showExportButton = true
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(100)
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF3B82F6)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEF4444)))
                            }
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF22C55E)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF59E0B)))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "i",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "像素工具箱 (Shizuku版)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = iOSLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "版本 ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = iOSSecondaryLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Shizuku 13.1.5",
                    style = MaterialTheme.typography.bodySmall,
                    color = iOSSecondaryLabel
                )
            }
        }
        // Export Log Button (hidden, revealed after 8 taps on icon)
        if (showExportButton) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val scope = kotlinx.coroutines.MainScope()
                        scope.launch(Dispatchers.IO) {
                            try {
                                val deviceInfo = "设备: ${Build.MODEL}\n" +
                                        "Android: ${Build.VERSION.RELEASE}\n" +
                                        "SDK: ${Build.VERSION.SDK_INT}\n" +
                                        "制造商: ${Build.MANUFACTURER}\n" +
                                        "硬件: ${Build.HARDWARE}"
                                val logContent = com.example.pixeltoolbox.util.LogCollector.exportToString(
                                    deviceInfo,
                                    BuildConfig.VERSION_NAME
                                )
                                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                                val filename = "pixel_toolbox_log_${sdf.format(java.util.Date())}.txt"
                                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                    ?: java.io.File(context.filesDir, "logs")
                                if (!dir.exists()) dir.mkdirs()
                                val file = java.io.File(dir, filename)
                                file.writeText(logContent, Charsets.UTF_8)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "日志已保存: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("导出日志", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        // Open Source Acknowledgments Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "开源致谢",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = iOSLabel
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AcknowledgementItem("ryfineZ", "carrier-ims", "github.com/ryfineZ/carrier-ims-for-pixel", "特别致谢 ryfineZ 的开源 carrier-ims 项目，为本工具提供了 Pixel 5G/VoLTE 蜂窝网络全特性优化的核心实现思路")
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AcknowledgementItem("Trumeet", "MiPushFramework (xmsf)", "github.com/Trumeet/MiPushFramework", "感谢 Trumeet 开源的 xmsf 服务框架")
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AcknowledgementItem("RikkaApps", "Shizuku", "github.com/RikkaApps/Shizuku", "感谢 RikkaApps 团队维护 Shizuku，让 Android 系统级定制成为可能")
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AcknowledgementItem("Google / AOSP", "Jetpack Compose", "developer.android.com/jetpack/compose", "感谢 Google 与 AOSP 社区推出 Jetpack Compose，重新定义了 Android UI 开发体验")
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AcknowledgementItem("Google / AOSP", "Material 3", "m3.material.io", "感谢 Material 3 设计系统，为本工具提供了优雅、现代的视觉语言")
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AcknowledgementItem("REAndroid", "ARSCLib", "github.com/REAndroid/ARSCLib", "读写 Android 二进制资源文件 (resources.arsc)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AcknowledgementItem("Google", "apksig", "developer.android.com/studio/command-line/apksigner", "APK v1+v2 签名方案")
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AcknowledgementItem("LSPosed", "AndroidHiddenApiBypass", "github.com/LSPosed/AndroidHiddenApiBypass", "绕过 Android 隐藏 API 限制")
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AcknowledgementItem("JetBrains", "Kotlin", "kotlinlang.org", "感谢 JetBrains 创造了 Kotlin 语言，让 Android 开发更简洁、安全、富有表现力")
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "开源许可说明",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = iOSLabel
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "项目开源地址：github.com/abc1812645224-alt/pixel-toolbox",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = iOSBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "本项目已在 GitHub 上开源（GPL-3.0）。引用的开源组件大多遵循 Apache License 2.0，可自由使用、修改与分发，需保留版权声明；其中 ShizuCallRecorder 遵循 GPL-3.0，因其为直接参考的通话录音核心实现来源，本项目整体以 GPL-3.0 开源以保持兼容。完整致谢与许可说明见仓库 docs/credits.md。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = iOSSecondaryLabel
                    )
                }
            }
        }
        // Bottom text
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "感谢所有开源贡献者",
                style = MaterialTheme.typography.bodyMedium,
                color = iOSSecondaryLabel,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
@Composable
fun AcknowledgementItem(author: String, project: String, url: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = author,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = iOSLabel
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "- $project",
                style = MaterialTheme.typography.bodyLarge,
                color = iOSBlue
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.labelSmall,
            color = iOSSecondaryLabel.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = iOSSecondaryLabel
        )
    }
}
// ══════════════════════════════════════════════════════════════
// 免责声明
// ══════════════════════════════════════════════════════════════
@Composable
fun DisclaimerScreen(onAgree: () -> Unit, onRefuse: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "风险告知与免责声明",
                style = MaterialTheme.typography.headlineMedium,
                color = iOSLabel,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "请仔细阅读以下全部条款",
                style = MaterialTheme.typography.bodyMedium,
                color = iOSSecondaryLabel,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DisclaimerSection("第一条 总则") {
                        Text(
                            "本软件（以下简称「本应用」）是一款面向具备专业技术能力的开发者与高级用户的移动设备底层调试与网络参数配置工具。使用本应用所提供的任何功能，均视为使用者已充分理解并自愿承担由此产生的一切直接或间接后果。本应用的开发者（以下简称「开发者」）不对任何因使用或无法使用本应用而导致的损失、损害、数据丢失、设备故障、服务中断、合规争议或第三方索赔承担责任。本声明之全部条款构成本应用与使用者之间具有法律约束力的协议。使用者一经点击「我同意」即视为已阅读、理解并不可撤销地接受本声明之全部内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSSecondaryLabel,
                            lineHeight = 20.sp
                        )
                    }
                    DisclaimerSection("第二条 适用主体与前提条件") {
                        Text(
                            "2.1 本应用仅供年满十八周岁、具备完全民事行为能力且具有相关专业技术背景的自然人使用。使用者确认其具备独立评估本应用所涉技术风险的能力，包括但不限于对 Android 操作系统架构、无线通信协议栈、调制解调器（Modem）固件、eSIM/UICC 配置文件及运营商网络参数的深入理解。\n\n" +
                            "2.2 使用者确认其已对目标设备进行了完整的数据备份，且具备在设备出现软件故障时自行恢复系统固件的能力。开发者不提供任何形式的数据恢复、设备维修或售后技术支持服务。\n\n" +
                            "2.3 使用者确认其对本应用的使用行为符合所在司法管辖区的法律法规及电信监管要求。使用者自行承担因违规使用本应用而产生的行政处罚、刑事责任或民事赔偿责任",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSSecondaryLabel,
                            lineHeight = 20.sp
                        )
                    }
                    DisclaimerSection("第三条 功能风险告知") {
                        Text(
                            "3.1 电信网络参数修改：本应用提供的运营商配置注入、IMS 参数修改、网络模式强制切换、频段锁定及 VoLTE/VoWiFi/ViLTE 配置等功能，均通过 Android 系统底层接口或第三方权限管理框架（包括但不限于 Shizuku）直接操作设备调制解调器与射频子系统。此类操作可能导致以下后果（包括但不限于）：(a) 语音通话服务中断或无法正常建立呼叫；(b) 数据网络连接丢失或速率严重下降；(c) IMS 注册失败导致 VoLTE/VoWiFi 不可用；(d) 紧急呼叫（如 1101920 等）功能异常或完全失效；(e) 运营商侧检测到异常信令后对用户账户实施限速、停用或列入黑名单；(f) 违反与运营商签订的服务协议导致合约终止或产生违约金。\n\n" +
                            "3.2 系统底层修改：本应用涉及 Android 系统属性（System Properties）修改、内核参数调整、DPM（Device Policy Manager）策略注入及 CarrierConfig 覆写操作，可能导致：(a) 系统 OTA 更新失败或更新后设备无法正常启动；(b) SafetyNet/Play Integrity 认证失效，导致部分依赖认证的应用（如银行、支付、企业办公软件）拒绝运行；(c) 设备 Warranty（保修）条款触发导致厂商拒绝提供保修服务；(d) Android 安全模型降级，增加恶意软件攻击面。\n\n" +
                            "3.3 硬件风险：本应用的部分功能（如高功率射频模式强制启用）可能使设备射频组件在超出制造商设计规范的工况下运行，从而导致：(a) 射频功放芯片（PA）加速老化或永久性损坏；(b) 设备发热量异常增加，极端情况下可能导致电池鼓包、漏液或热失控；(c) SAR（电磁辐射比吸收率）超出当地法定限值。\n\n" +
                            "3.4 eSIM 与 UICC 操作风险：本应用提供的 eSIM 配置文件管理UICC 逻辑通道操作功能，可能导致：(a) eSIM Profile 不可逆损坏或永久丢失b) 物理 SIM 卡文件系统损坏导致卡片报废；(c) 运营商发放的 eSIM 激活码（Activation Code）因重复使用被服务端锁定",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSSecondaryLabel,
                            lineHeight = 20.sp
                        )
                    }
                    DisclaimerSection("第四条 知识产权与合规声明") {
                        Text(
                            "4.1 本应用不包含、不提供、不分发任何受版权保护的第三方固件、基带文件、运营商私有配置数据或 DRM 保护内容。使用者通过本应用导入的任何外部配置文件、APN 参数、CarrierSettings 数据，其来源合法性由使用者自行负责。\n\n" +
                            "4.2 本应用所采用的技术方案和实现方式均为独立研发，不构成对任何第三方专利、商标或商业秘密的侵权。使用者在特定司法管辖区内使用本应用特定功能（如频段解锁、运营商锁解除）的行为，可能违反当地电信法规或设备进口管制条例，使用者应自行咨询专业法律意见",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSSecondaryLabel,
                            lineHeight = 20.sp
                        )
                    }
                    DisclaimerSection("第五条 责任限制与免责范围") {
                        Text(
                            "5.1 本应用按「现状」（AS IS）及「可用」（AS AVAILABLE）基础提供，不附带任何明示或默示的保证，包括但不限于适销性保证、特定用途适用性保证、所有权保证及不侵权保证。开发者不保证本应用的功能满足使用者的特定需求，不保证本应用的运行不受中断或不出错，不保证本应用所涉及的缺陷将被修正。\n\n" +
                            "5.2 在任何情况下，开发者均不对因使用或无法使用本应用而产生的任何直接、间接、附带、特殊、惩罚性或结果性损害承担责任，包括但不限于：利润损失、商誉损失、数据丢失、设备损坏、业务中断、人身伤害或死亡，无论该等损害是否基于合同、侵权（包括过失）、严格责任或其他法律理论，即使开发者已被告知该等损害的可能性。\n\n" +
                            "5.3 部分司法管辖区不允许排除或限制附带或结果性损害的责任，因此上述限制或排除可能不适用于特定使用者。在此情况下，开发者的责任范围应以适用法律所允许的最低限度为限。\n\n" +
                            "5.4 使用者同意，因使用者违反本声明任何条款或不当使用本应用而引起的任何第三方索赔、诉讼或损害赔偿（包括合理的律师费），使用者将全额赔偿开发者并使其免受损害",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSSecondaryLabel,
                            lineHeight = 20.sp
                        )
                    }
                    DisclaimerSection("第六条 数据与隐私") {
                        Text(
                            "6.1 本应用不会主动收集、上传或分享使用者的个人身份信息、位置数据、联系人、通话记录或短信内容至开发者服务器或任何第三方。\n\n" +
                            "6.2 本应用在运行过程中产生的本地日志文件可能包含设备标识符（如 IMEI、IMSI、ICCID 等）、网络参数及系统配置快照。使用者应妥善保管这些日志文件，因其可能被用于追踪设备或关联用户身份。开发者不对因使用者主动分享或泄露日志文件而导致的隐私风险承担责任。\n\n" +
                            "6.3 本应用通过 Shizuku 等第三方框架获取的系统级权限，其权限管理机制由对应框架的开发者负责。因第三方框架的安全漏洞或权限滥用导致的数据泄露，不在本应用开发者的责任范围之内",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSSecondaryLabel,
                            lineHeight = 20.sp
                        )
                    }
                    DisclaimerSection("第七条 协议变更与可分割性") {
                        Text(
                            "7.1 开发者保留随时修改本免责声明条款的权利。修改后的条款将在本应用更新时随新版本一并发布，使用者继续使用即视为接受修改后的条款。开发者无义务就条款变更向使用者进行单独通知。\n\n" +
                            "7.2 若本声明中的任何条款被具有管辖权的法院或仲裁机构认定为无效或不可执行，该条款应在法律允许的最小必要范围内予以限制或删除，并以最接近原条款意图的有效条款替代。其余条款的效力不受影响，应继续完全有效。\n\n" +
                            "7.3 本声明之最终解释权归开发者所有",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSSecondaryLabel,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAgree
            ) {
                Text("我同意，继续使用", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            iOSOutlineButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRefuse
            ) {
                Text("我拒绝，退出应用", style = MaterialTheme.typography.titleMedium, color = iOSRed)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
@Composable
private fun DisclaimerSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = iOSLabel
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}
fun createLockScreenShortcut(context: Context) {
    if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        val intent = Intent(context, LockScreenActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }
        val shortcutInfo = ShortcutInfoCompat.Builder(context, "lock_screen_shortcut")
            .setShortLabel("锁屏")
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_lock_screen))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }
}
suspend fun installDesktopLauncher(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "正在解包并覆盖安装桌面...", android.widget.Toast.LENGTH_SHORT).show()
            }
            val apkName = "pixel_launcher.apk"
            val targetPath = "/data/local/tmp/$apkName"
            val assetManager = context.assets
            val inStream = assetManager.open(apkName)
            val outFile = File(context.cacheDir, apkName)
            val outStream = FileOutputStream(outFile)
            inStream.copyTo(outStream)
            inStream.close()
            outStream.close()

            com.example.pixeltoolbox.shizuku.ShizukuUtils.streamFileTo("cat > $targetPath", outFile)
            val res = com.example.pixeltoolbox.shizuku.ShizukuUtils.executeCommand("pm install -r -d -g $targetPath; am force-stop com.google.android.apps.nexuslauncher 2>/dev/null; rm -f $targetPath")
            
            if (res.getOrNull()?.contains("Success", ignoreCase = true) == true) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "✅ 无搜索框桌面已成功部署并重启生效！", android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    val err = res.getOrNull() ?: res.exceptionOrNull()?.message ?: "安装未响应"
                    android.widget.Toast.makeText(context, "安装结果: $err", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "安装异常: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
