/*
 * Pixel Toolbox (像素工具箱)
 * Copyright (C) 2026 Pixel Toolbox Project
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.pixeltoolbox.services.push

import android.content.Context
import android.content.pm.PackageManager
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ManagedPushApp(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean,
    val iconBitmap: android.graphics.Bitmap? = null
)

object UnifiedPushManager {

    const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    private const val WECHAT_PACKAGE = "com.tencent.mm"
    private const val QQ_PACKAGE = "com.tencent.mobileqq"

    /**
     * 检测设备上是否已安装小米推送框架 (com.xiaomi.xmsf)
     */
    fun isXmsfInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(XMSF_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            val res = ShizukuUtils.executeCommandOrNull("pm list packages $XMSF_PACKAGE")
            !res.isNullOrBlank() && res.contains(XMSF_PACKAGE)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 实时检测底层统一推送进程与服务是否真正处于运行中
     */
    suspend fun isPushServiceRunning(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!isXmsfInstalled(context)) return@withContext false

        val sp = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)
        val isSpOn = sp.getBoolean("mipush_enabled", false)

        val settingsRes = ShizukuUtils.executeCommandOrNull("settings get global pixeltoolbox_mipush_enabled")?.trim() == "1"
        val psRes = ShizukuUtils.executeCommandOrNull("ps -A | grep $XMSF_PACKAGE")
        val isProcessActive = !psRes.isNullOrBlank() && psRes.lines().any { it.contains(XMSF_PACKAGE) }

        return@withContext isSpOn || settingsRes || isProcessActive
    }

    /**
     * 实时检测微信/QQ 厂商推送伪装状态
     */
    suspend fun isTencentSpoofEnabled(context: Context): Boolean = withContext(Dispatchers.IO) {
        val sp = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)
        val isSpOn = sp.getBoolean("tencent_spoof_enabled", false)
        val propRes = ShizukuUtils.executeCommandOrNull("getprop ro.miui.ui.version.name")?.trim() == "V140"

        return@withContext isSpOn || propRes
    }

    /**
     * 实时查询设备上支持/已绑定统一推送 (MiPush/FCM) 的 App 列表
     */
    suspend fun getManagedApps(context: Context): List<ManagedPushApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val list = mutableListOf<ManagedPushApp>()

        val result = ShizukuUtils.executeCommandOrNull("pm query-receivers -a com.xiaomi.mipush.RECEIVE_MESSAGE --brief") ?: ""
        val lines = result.lines().map { it.trim() }.filter { it.contains("/") }

        val pkgs = lines.map { it.substringBefore("/") }.toMutableSet()

        val tencentSpoofed = isTencentSpoofEnabled(context)
        if (tencentSpoofed) {
            pkgs.add(WECHAT_PACKAGE)
            pkgs.add(QQ_PACKAGE)
        }

        for (pkg in pkgs) {
            if (pkg == context.packageName || pkg == XMSF_PACKAGE) continue
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val iconBitmap: android.graphics.Bitmap? = try {
                    val drawable = pm.getApplicationIcon(pkg)
                    val bmp = android.graphics.Bitmap.createBitmap(72, 72, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, 72, 72)
                    drawable.draw(canvas)
                    bmp
                } catch (e: Exception) { null }

                list.add(ManagedPushApp(pkg, label, true, iconBitmap))
            } catch (_: Exception) {}
        }

        list.sortBy { it.appName.lowercase() }
        return@withContext list
    }

    /**
     * 一键无感开启统一推送托管框架（包含挂载守护与状态双重持久化 + xmsf.apk 自动补全双重保险）
     */
    suspend fun enablePushService(context: Context): Result<String> = withContext(Dispatchers.IO) {
        // 双重保险 1: 检测 xmsf 是否已安装，若未安装或丢失则自动从 assets 释放并 pm install 静默安装
        try {
            context.packageManager.getApplicationInfo(XMSF_PACKAGE, 0)
        } catch (e: Exception) {
            try {
                val apkFile = java.io.File(context.cacheDir, "xmsf.apk")
                context.assets.open("xmsf.apk").use { input ->
                    java.io.FileOutputStream(apkFile).use { output ->
                        input.copyTo(output)
                    }
                }
                ShizukuUtils.executeCommand("pm install -r -g ${apkFile.absolutePath}")
                apkFile.delete()
            } catch (_: Exception) {}
        }

        // 双重保险 2: 解锁权限、挂载守护、加入电池白名单、启动推送守护服务
        val cmds = listOf(
            "pm enable $XMSF_PACKAGE 2>/dev/null",
            "pm grant $XMSF_PACKAGE android.permission.POST_NOTIFICATIONS 2>/dev/null",
            "cmd notification set_notifications_enabled $XMSF_PACKAGE true 2>/dev/null",
            "cmd appops set $XMSF_PACKAGE POST_NOTIFICATION allow 2>/dev/null",
            "pm disable $XMSF_PACKAGE/top.trumeet.mipushframework.wizard.WelcomeActivity 2>/dev/null",
            "cmd appops set $XMSF_PACKAGE RUN_IN_BACKGROUND allow 2>/dev/null",
            "cmd appops set $XMSF_PACKAGE WAKE_LOCK allow 2>/dev/null",
            "cmd appops set $XMSF_PACKAGE AUTO_START allow 2>/dev/null",
            "dumpsys deviceidle whitelist +$XMSF_PACKAGE 2>/dev/null",
            "am startservice -n $XMSF_PACKAGE/.push.service.XMPushService 2>/dev/null",
            "echo 'enabled=true' > /data/system/pixeltoolbox_mipush.xml",
            "chmod 644 /data/system/pixeltoolbox_mipush.xml",
            "chcon u:object_r:system_file:s0 /data/system/pixeltoolbox_mipush.xml"
        ).joinToString("; ")

        val sp = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)
        sp.edit().putBoolean("mipush_enabled", true).apply()

        return@withContext ShizukuUtils.executeCommand(cmds)
    }

    /**
     * 关闭统一推送服务保活
     */
    suspend fun disablePushService(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val cmds = listOf(
            "pm disable $XMSF_PACKAGE 2>/dev/null",
            "am force-stop $XMSF_PACKAGE 2>/dev/null",
            "settings put global pixeltoolbox_mipush_enabled 0"
        ).joinToString("; ")

        val sp = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)
        sp.edit().putBoolean("mipush_enabled", false).apply()

        return@withContext ShizukuUtils.executeCommand(cmds)
    }

    /**
     * 一键从 assets/xmsf.apk 在后台通过 Shizuku ADB 静默安装小米推送服务框架并完成全自动开启
     */
    suspend fun installBuiltinXmsf(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cacheFile = java.io.File(context.cacheDir, "xmsf.apk")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                try {
                    context.assets.open("xmsf.apk").use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("内置 xmsf.apk 放置在 assets/xmsf.apk 后即可支持静默一键安装"))
                }
            }

            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                return@withContext Result.failure(Exception("未在 assets 目录找到 xmsf.apk 资源包"))
            }

            val tmpPath = "/data/local/tmp/xmsf_temp.apk"
            val pushRes = ShizukuUtils.streamFileTo("cat > $tmpPath", cacheFile)
            if (pushRes.isFailure) {
                return@withContext Result.failure(Exception("推送到临时目录失败: ${pushRes.exceptionOrNull()?.message}"))
            }

            val installRes = ShizukuUtils.executeCommand("pm install -r -g $tmpPath; rm -f $tmpPath")
            if (installRes.isFailure) {
                return@withContext Result.failure(Exception("静默安装失败: ${installRes.exceptionOrNull()?.message}"))
            }

            // 安装完成后自动调用 enablePushService 赋予自启动与保活
            enablePushService(context)
            return@withContext Result.success("MiPush 框架已成功后台静默安装并开启托管！")
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * 开启 微信/QQ 伪装
     */
    suspend fun enableTencentSpoof(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val cmds = listOf(
            "setprop persist.sys.miui.version V140 2>/dev/null",
            "settings put global pixeltoolbox_tencent_mipush 1"
        ).joinToString("; ")

        val sp = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)
        sp.edit().putBoolean("tencent_spoof_enabled", true).apply()

        return@withContext ShizukuUtils.executeCommand(cmds)
    }

    /**
     * 关闭 微信/QQ 伪装
     */
    suspend fun disableTencentSpoof(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val cmds = listOf(
            "settings put global pixeltoolbox_tencent_mipush 0"
        ).joinToString("; ")

        val sp = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)
        sp.edit().putBoolean("tencent_spoof_enabled", false).apply()

        return@withContext ShizukuUtils.executeCommand(cmds)
    }
}
