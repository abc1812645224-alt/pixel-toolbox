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
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import com.example.pixeltoolbox.ui.theme.GlassCard
import com.example.pixeltoolbox.ui.theme.iOSButton
import com.example.pixeltoolbox.ui.theme.iOSTertiaryLabel
import com.example.pixeltoolbox.ui.theme.iOSBlue
import com.example.pixeltoolbox.ui.theme.iOSGreen
import com.example.pixeltoolbox.ui.theme.iOSOutlineButton
import com.example.pixeltoolbox.ui.theme.iOSRed
import com.example.pixeltoolbox.ui.theme.iOSSecondaryLabel
import com.example.pixeltoolbox.ui.theme.AutoSizeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.material3.MaterialTheme

@Composable
fun GeekToolsCard(context: Context, textColor: Color, addLog: (String) -> Unit, onOpenBootManager: () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()

    // 电池健康状态
    var batteryData by remember { mutableStateOf<Map<String, String>?>(null) }
    var batteryError by remember { mutableStateOf<String?>(null) }

    // 安装器弹窗
    var showInstallWarning by remember { mutableStateOf(false) }
    var apkUriToInstall by remember { mutableStateOf<Uri?>(null) }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            apkUriToInstall = uri
            showInstallWarning = true
        }
    }

    if (showInstallWarning && apkUriToInstall != null) {
        AlertDialog(
            onDismissRequest = { showInstallWarning = false },
            title = { Text("强行降级安装警告", fontWeight = FontWeight.Bold) },
            text = {
                Text("即将绕过系统安全限制安装该应用。\n\n" +
                     "注意：如果您要强行降级应用，由于签名和数据库版本问题，某些应用降级后可能会直接闪退。这属于安卓系统的正常安全机制保护现象，并不是安装失败。\n\n" +
                     "如果降级后闪退，建议彻底卸载旧版再重新安装。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showInstallWarning = false
                    val uri = apkUriToInstall!!
                    coroutineScope.launch {
                        Toast.makeText(context, "正在复制文件并安装，请稍候...", Toast.LENGTH_SHORT).show()
                        val cacheFile = copyUriToCache(context, uri)
                        if (cacheFile != null) {
                            addLog("正在提取并安装 APK: ${cacheFile.name}")
                            val result = ShizukuUtils.installApk(cacheFile.absolutePath)
                            if (result.isSuccess) {
                                addLog("✅ 应用强行安装成功！")
                                Toast.makeText(context, "安装成功！", Toast.LENGTH_LONG).show()
                            } else {
                                addLog("❌ 安装失败: ${result.exceptionOrNull()?.message}")
                                Toast.makeText(context, "安装失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                            cacheFile.delete()
                        } else {
                            Toast.makeText(context, "无法读取文件", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("我已了解，强制安装", color = iOSRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallWarning = false }) {
                    Text("取消", color = iOSBlue)
                }
            }
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("极客工具箱", style = MaterialTheme.typography.headlineMedium, color = textColor)
            Text("深入系统底层的功能大全", style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel)
            Spacer(modifier = Modifier.height(20.dp))

            // 1. 刷新率控制
            SectionTitle("全局刷新率强制锁定", "强制锁定屏幕刷新率，解决部分场景卡顿掉帧")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60f to "60Hz", 90f to "90Hz", 120f to "120Hz").forEach { (rate, label) ->
                    iOSButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val res = ShizukuUtils.setRefreshRate(rate)
                            if (res.isSuccess) {
                                addLog("✅ 刷新率已强制锁定为 $label")
                                Toast.makeText(context, res.getOrNull(), Toast.LENGTH_SHORT).show()
                            } else {
                                addLog("❌ 刷新率锁定失败: ${res.exceptionOrNull()?.message}")
                                Toast.makeText(context, res.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        AutoSizeText(label, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 充电控制
            SectionTitle("充电提速与电池状态检查", "管理底层充电节点，强制限制或恢复充电以保护电池")
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(context, com.example.pixeltoolbox.ui.custom.BatteryActivity::class.java))
                }
            ) { AutoSizeText("充电控制页面", color = Color.White, fontWeight = FontWeight.SemiBold) }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 高级安装器
            SectionTitle("突破限制降级安装器", "绕过 SDK 版本与签名限制，支持强行降级安装目标应用")
            Spacer(modifier = Modifier.height(8.dp))
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    apkPickerLauncher.launch("application/vnd.android.package-archive")
                }
            ) {
                AutoSizeText("选择 APK 文件并强行安装", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 已安装应用 APK 一键提取器
            SectionTitle("已安装应用 APK 一键提取器", "快速提取并导出手机内任意应用的安装包 (APK) 到 Download 目录")
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intent = Intent(context, com.example.pixeltoolbox.ui.custom.AppListActivity::class.java).apply {
                        putExtra("MODE", "EXTRACT")
                    }
                    context.startActivity(intent)
                }
            ) { AutoSizeText("提取已安装应用 APK", color = Color.White, fontWeight = FontWeight.SemiBold) }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. 极客冰箱
            SectionTitle("极客冰箱", "底层冻结 (disable-user) 闲置应用，实现零后台电量与内存占用")
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(context, com.example.pixeltoolbox.ui.custom.AppListActivity::class.java))
                }
            ) { AutoSizeText("极客冰箱", color = Color.White, fontWeight = FontWeight.SemiBold) }

            Spacer(modifier = Modifier.height(24.dp))

            // 5.5 自启管理
            SectionTitle("自启管理", "禁用第三方应用的开机自启广播，减少开机内存占用")
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenBootManager
            ) { AutoSizeText("自启管理", color = Color.White, fontWeight = FontWeight.SemiBold) }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. 去除 WiFi 感叹号
            SectionTitle("去除 WiFi 感叹号", "部署连通正常的 Captive Portal 验证节点，彻底去除 WiFi 小感叹号")
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(context, com.example.pixeltoolbox.ui.custom.WifiFixActivity::class.java))
                }
            ) { AutoSizeText("去除 WiFi 感叹号", color = Color.White, fontWeight = FontWeight.SemiBold) }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. 状态栏净化（高级）
            SectionTitle("状态栏净化（高级控制）", "自由选择并隐藏闹钟、蓝牙、WiFi、电量等状态栏系统图标")
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(context, com.example.pixeltoolbox.ui.custom.StatusBarActivity::class.java))
                }
            ) { AutoSizeText("状态栏净化", color = Color.White, fontWeight = FontWeight.SemiBold) }

            Spacer(modifier = Modifier.height(24.dp))

            // 7.5 游戏模式
            SectionTitle("游戏模式", "一键开启高性能电源、关闭动画、清理后台与缓存，提升游戏帧率与响应速度")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                iOSButton(
                    modifier = Modifier.weight(1f),
                    backgroundColor = iOSRed,
                    onClick = {
                        coroutineScope.launch {
                            addLog("🎮 正在开启游戏模式...")
                            var allOk = true

                            // 1. 高性能电源模式（含锁定峰值刷新率，覆盖省电模式留下的 60Hz 锁）
                            val powerResult = ShizukuUtils.executeCommand("cmd power set-mode 0 2>/dev/null; cmd power set-fixed-performance-mode-enabled true 2>/dev/null; r=\$(settings get system peak_refresh_rate 2>/dev/null); [ \"\$r\" = \"null\" ] && r=120; [ -z \"\$r\" ] && r=120; settings put system min_refresh_rate \$r 2>/dev/null; settings put system peak_refresh_rate \$r 2>/dev/null; echo 'performance' > /data/local/tmp/pixel_cpu_mode")
                            if (powerResult.isSuccess) addLog("✅ 高性能电源模式已开启（已锁定峰值刷新率）")
                            else { addLog("⚠️ 电源模式设置失败: ${powerResult.exceptionOrNull()?.message}"); allOk = false }

                            // 2. 保存动画原值并关闭系统动画（取消时据此完整恢复）
                            val animKeys = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
                            val origAnims = animKeys.map { key ->
                                ShizukuUtils.executeCommand("settings get global $key").getOrNull()?.trim() ?: ""
                            }
                            ShizukuUtils.executeCommand("echo '${origAnims.joinToString("|")}' > /data/local/tmp/pixel_game_anim")
                            val animCmds = animKeys.map { key -> "settings put global $key 0" }
                            animCmds.forEach { cmd ->
                                val r = ShizukuUtils.executeCommand(cmd)
                                if (r.isSuccess) addLog("✅ 动画已关闭: ${cmd.substringAfterLast(" ")}")
                                else { addLog("⚠️ 动画关闭失败: ${r.exceptionOrNull()?.message}"); allOk = false }
                            }

                            // 3. 杀后台进程（排除自身）
                            val killResult = ShizukuUtils.executeCommand(
                                "for pkg in \$(pm list packages -3 | sed 's/package://' | grep -v com.example.pixeltoolbox); " +
                                "do am force-stop \$pkg; done"
                            )
                            if (killResult.isSuccess) addLog("✅ 后台进程已清理")
                            else { addLog("⚠️ 后台清理失败: ${killResult.exceptionOrNull()?.message}"); allOk = false }

                            // 4. 清除内存缓存
                            val cacheResult = ShizukuUtils.executeCommand("am kill-all")
                            if (cacheResult.isSuccess) addLog("✅ 内存缓存已清除")
                            else { addLog("⚠️ 缓存清除失败: ${cacheResult.exceptionOrNull()?.message}"); allOk = false }

                            // 5. 触控优化（软件层）
                            val touchCmds = listOf(
                                "settings put secure touch_slop_distance 2",
                                "settings put secure touch_block_delay 8",
                                "settings put system touch_sample_rate 5"
                            )
                            touchCmds.forEach { cmd ->
                                val r = ShizukuUtils.executeCommand(cmd)
                                val key = cmd.substringAfterLast(" ")
                                if (r.isSuccess) addLog("✅ 触控优化: ${cmd.substringAfterLast(" ")}")
                                else { addLog("⚠️ 触控优化失败: ${r.exceptionOrNull()?.message}"); allOk = false }
                            }

                            if (allOk) {
                                addLog("🏆 游戏模式已就绪，祝老板场场 MVP！")
                            } else {
                                addLog("🎮 游戏模式已部分开启，请检查上述失败项")
                            }
                            Toast.makeText(context, "游戏模式已开启\n建议手动打开免打扰模式以获得最佳体验", Toast.LENGTH_LONG).show()
                        }
                    }
                ) { AutoSizeText("游戏模式", color = Color.White, style = MaterialTheme.typography.labelLarge) }

                iOSButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            addLog("🔄 正在取消游戏模式...")
                            var allOk = true

                            // 1. 恢复默认电源模式（set-mode 0 + 关闭固定性能锁频 + 删除刷新率锁恢复自动，不残留）
                            val powerResult = ShizukuUtils.executeCommand("cmd power set-mode 0 2>/dev/null; cmd power set-fixed-performance-mode-enabled false 2>/dev/null; settings delete system min_refresh_rate 2>/dev/null; settings delete system peak_refresh_rate 2>/dev/null; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo schedutil > \$g 2>/dev/null; done; echo 'default' > /data/local/tmp/pixel_cpu_mode")
                            if (powerResult.isSuccess) addLog("✅ 电源模式已恢复默认（刷新率已恢复系统默认）")
                            else { addLog("⚠️ 电源模式恢复失败: ${powerResult.exceptionOrNull()?.message}"); allOk = false }

                            // 2. 恢复系统动画（优先恢复开启前保存的原值，无记录则回退 1.0）
                            val animKeys = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
                            val savedAnims = ShizukuUtils.executeCommand("cat /data/local/tmp/pixel_game_anim").getOrNull()?.trim()
                            val savedValues = savedAnims?.split("|")
                            animKeys.forEachIndexed { i, key ->
                                var restoreValue = "1.0"
                                if (savedValues != null && i < savedValues.size && savedValues[i].isNotBlank() && savedValues[i] != "null") {
                                    restoreValue = savedValues[i]
                                }
                                val r = ShizukuUtils.executeCommand("settings put global $key $restoreValue")
                                if (r.isSuccess) addLog("✅ 动画已恢复: $key = $restoreValue")
                                else { addLog("⚠️ 动画恢复失败: ${r.exceptionOrNull()?.message}"); allOk = false }
                            }
                            ShizukuUtils.executeCommand("rm -f /data/local/tmp/pixel_game_anim 2>/dev/null")

                            // 3. 恢复触控默认值
                            val touchRestore = listOf(
                                "settings delete secure touch_slop_distance",
                                "settings delete secure touch_block_delay",
                                "settings delete system touch_sample_rate"
                            )
                            touchRestore.forEach { cmd ->
                                val r = ShizukuUtils.executeCommand(cmd)
                                if (r.isSuccess) addLog("✅ 触控已恢复默认")
                                else { addLog("⚠️ 触控恢复失败: ${r.exceptionOrNull()?.message}"); allOk = false }
                            }

                            if (allOk) addLog("✅ 游戏模式已取消，系统恢复默认")
                            else addLog("⚠️ 游戏模式已部分取消，请检查上述失败项")
                            Toast.makeText(context, "游戏模式已取消", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { AutoSizeText("取消游戏模式", color = Color.White, style = MaterialTheme.typography.labelLarge) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7.6 应用分身
            SectionTitle("应用分身", "为应用创建独立分身实例，与主应用数据完全隔离。仅供学习研究使用，下载后请于 24 小时内删除。请遵守相关法律法规及目标应用的服务条款，使用者自行承担全部责任")
            iOSButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(context, com.example.pixeltoolbox.ui.geektools.AppCloneActivity::class.java))
                }
            ) { AutoSizeText("应用分身", color = Color.White, fontWeight = FontWeight.SemiBold) }

            Spacer(modifier = Modifier.height(24.dp))

            // 8. 时区与时间同步修复
            SectionTitle("时区与时间同步修复", "强制将系统定位到亚洲/上海，并将 NTP 时间同步服务器修改为阿里云，解决时间慢的问题，有助于抢票等对毫秒级时间敏感的场景")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                iOSButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            addLog("正在修复时区和 NTP 服务器...")
                            val cmd = "cmd alarm set-timezone Asia/Shanghai || setprop persist.sys.timezone Asia/Shanghai; settings put global ntp_server ntp.aliyun.com"
                            val result = ShizukuUtils.executeCommand(cmd)
                            if (result.isSuccess) {
                                addLog("✅ 时区与 NTP 修复已应用")
                                Toast.makeText(context, "时区与 NTP 修复已应用", Toast.LENGTH_SHORT).show()
                            } else {
                                addLog("❌ 执行失败: ${result.exceptionOrNull()?.message}")
                                Toast.makeText(context, "执行失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { AutoSizeText("一键修复时间", color = Color.White, fontWeight = FontWeight.SemiBold) }
                
                iOSOutlineButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch {
                            val cmd = "settings delete global ntp_server"
                            val result = ShizukuUtils.executeCommand(cmd)
                            if (result.isSuccess) {
                                addLog("✅ 已恢复系统默认 NTP")
                                Toast.makeText(context, "已恢复系统默认 NTP", Toast.LENGTH_SHORT).show()
                            } else {
                                addLog("❌ 执行失败: ${result.exceptionOrNull()?.message}")
                                Toast.makeText(context, "执行失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { AutoSizeText("恢复默认", fontWeight = FontWeight.SemiBold) }
            }

            Spacer(modifier = Modifier.height(24.dp))

        }
    }
}

@Composable
fun SectionTitle(title: String, desc: String? = null) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel, letterSpacing = 0.5.sp)
        if (desc != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.labelSmall, color = iOSTertiaryLabel)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

suspend fun copyUriToCache(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val cacheFile = File(context.cacheDir, "temp_install.apk")
            val outputStream = FileOutputStream(cacheFile)
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            return@withContext cacheFile
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext null
}