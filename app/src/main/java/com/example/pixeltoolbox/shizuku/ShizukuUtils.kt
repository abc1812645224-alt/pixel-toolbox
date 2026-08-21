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

package com.example.pixeltoolbox.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

data class SimSlotInfo(
    val slotIndex: Int,
    val subId: Int,
    val carrierName: String,
    val mccMnc: String,
    val isEmbedded: Boolean = false
)

object ShizukuUtils {

    private const val TAG = "ShizukuUtils"

    fun isShizukuInstalled(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuInstalled()) return false
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun requestShizukuPermission(requestCode: Int) {
        try {
            if (!isShizukuInstalled()) return
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                return
            }
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
        }
    }

    /**
     * 通过 instrumentation 通道注入 CarrierConfig，走 ShizukuProviderWrapper.startInstrumentation。
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun applyCarrierConfig(context: Context, subId: Int, toggleMap: Map<String, Boolean>, onResult: (Boolean, String) -> Unit) {
        if (!hasShizukuPermission()) {
            onResult(false, "Shizuku 未授权")
            return
        }

        GlobalScope.launch {
            try {
                // ---- D 组：基带调优（setprop，ADB Shell 独立开关）----
                executeCommand("setprop persist.vendor.radio.nr_sa_fast_camp ${if (toggleMap["nr_sa_fast_camp"] == true) "1" else "0"}")
                executeCommand("setprop persist.vendor.radio.5g_ca_enable ${if (toggleMap["5g_ca_enable"] == true) "1" else "0"}")
                executeCommand("setprop persist.vendor.radio.dynamic_sar ${if (toggleMap["dynamic_sar"] == true) "0" else "1"}")
                executeCommand("setprop persist.vendor.radio.smart_data_switch ${if (toggleMap["smart_data_switch"] == true) "1" else "0"}")
                executeCommand("settings put global vonr_enabled 1")

                if (toggleMap["unlock_network_types"] == true) {
                    executeCommand("cmd phone set-allowed-network-types-for-users -s 0 11001111101111111111")
                    executeCommand("cmd phone set-allowed-network-types-for-users -s 1 11001111101111111111")
                }

                if (toggleMap["net_optimize"] == true) {
                    executeCommand("sysctl -w net.core.rmem_max=16777216 2>/dev/null")
                    executeCommand("sysctl -w net.core.wmem_max=16777216 2>/dev/null")
                    executeCommand("sysctl -w net.ipv4.tcp_fastopen=3 2>/dev/null")
                    executeCommand("sysctl -w net.ipv4.tcp_slow_start_after_idle=0 2>/dev/null")
                }

                val bundle = ImsModifier.buildBundle(
                    carrierName = null,
                    countryISO = "cn",
                    countryMcc = null,
                    countryMncHint = null,
                    enableVoLTE = toggleMap["volte"] == true,
                    enableVoWiFi = toggleMap["vowifi"] == true,
                    enableVT = toggleMap["vilte"] == true,
                    enableVoNR = toggleMap["vonr"] == true,
                    enableCrossSIM = toggleMap["cross_sim"] == true,
                    enableUT = toggleMap["ut"] == true,
                    enable5GNR = toggleMap["nr_5g"] == true,
                    enable5GThreshold = toggleMap["5g_signal"] == true,
                    enable5GPlusIcon = toggleMap["5ga_icon"] == true,
                    enableShow4GForLTE = toggleMap["lte_4g"] == true,
                    show5GA = toggleMap["5ga_icon"] == true,
                    enable5GIconUpgrade = toggleMap["5ga_icon"] == true,
                    enableCallRecording = null
                )
                bundle.putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, subId)

                val msg = ShizukuProviderWrapper.overrideImsConfig(context, bundle)
                if (msg == null) {
                    // 注入成功后自动刷新一次飞行模式，触发基带握手
                    executeCommand("cmd connectivity airplane-mode enable; sleep 1; cmd connectivity airplane-mode disable")
                    onResult(true, "CarrierConfig 注入成功")
                } else {
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "applyCarrierConfig error", e)
                onResult(false, "写入失败: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    /**
     * 同步读取 CarrierConfig 状态（A/B/C 组）
     */
    fun readCarrierConfigStates(context: Context): Map<String, Boolean> {
        val map = mutableMapOf<String, Boolean>()
        try {
            val subId = android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
            val result = kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withTimeoutOrNull(3000) {
                    ShizukuProviderWrapper.readCarrierConfig(context, subId)
                }
            } ?: return map

            val nrArray = result.getIntArray(android.telephony.CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY)
            map["nr_5g"] = nrArray != null && nrArray.contains(1) && nrArray.contains(2)

            if (Build.VERSION.SDK_INT >= 34) {
                map["vonr"] = result.getBoolean(android.telephony.CarrierConfigManager.KEY_VONR_ENABLED_BOOL, false)
            }

            val ssrsrp = result.getIntArray(android.telephony.CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY)
            map["5g_signal"] = ssrsrp != null && ssrsrp.size >= 4

            val nrAdv = result.getInt("nr_advanced_threshold_bandwidth_khz_int", 0)
            map["5ga_icon"] = nrAdv == 130000

            map["volte"] = result.getBoolean(android.telephony.CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, false)
            map["vowifi"] = result.getBoolean(android.telephony.CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, false)
            map["vilte"] = result.getBoolean(android.telephony.CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, false)
            map["lte_4g"] = result.getBoolean("show_4g_for_lte_data_icon_bool", false)
            map["cross_sim"] = result.getBoolean("carrier_cross_sim_ims_available_bool", false)
            map["ut"] = result.getBoolean(android.telephony.CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, false)
        } catch (_: Exception) {}
        return map
    }

    /**
     * 读取 D 组基带 setprop 状态
     */
    fun readNetworkPropStates(): Map<String, Boolean> {
        val map = mutableMapOf<String, Boolean>()
        try {
            val saFast = executeCommandOrNull("getprop persist.vendor.radio.nr_sa_fast_camp")?.trim() == "1"
            val caEnable = executeCommandOrNull("getprop persist.vendor.radio.5g_ca_enable")?.trim() == "1"
            val sarOff = executeCommandOrNull("getprop persist.vendor.radio.dynamic_sar")?.trim() == "0"
            val smartSwitch = executeCommandOrNull("getprop persist.vendor.radio.smart_data_switch")?.trim() == "1"
            val netOptimize = executeCommandOrNull("cat /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null")?.trim() == "3"
            val unlockNet = executeCommandOrNull("cmd phone get-allowed-network-types-for-users -s 0 2>/dev/null")?.trim()?.contains("NR") == true

            map["nr_sa_fast_camp"] = saFast
            map["5g_ca_enable"] = caEnable
            map["dynamic_sar"] = sarOff
            map["smart_data_switch"] = smartSwitch
            map["net_optimize"] = netOptimize
            map["unlock_network_types"] = unlockNet
        } catch (_: Exception) {}
        return map
    }

    /**
     * 通过 instrumentation 通道读取 CarrierConfig。
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun readCarrierConfig(context: Context, subId: Int, onResult: (Map<String, Boolean>?, String?) -> Unit) {
        if (!hasShizukuPermission()) {
            onResult(null, "Shizuku 未授权")
            return
        }

        GlobalScope.launch {
            try {
                val keys = arrayOf(
                    android.telephony.CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL,
                    android.telephony.CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL,
                    android.telephony.CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL,
                    android.telephony.CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                    android.telephony.CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                    android.telephony.CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL,
                    "nr_advanced_threshold_bandwidth_khz_int",
                    "show_4g_for_lte_data_icon_bool",
                    "carrier_cross_sim_ims_available_bool",
                )
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    keys.plus(android.telephony.CarrierConfigManager.KEY_VONR_ENABLED_BOOL)
                }

                val result = ShizukuProviderWrapper.readCarrierConfig(context, subId)
                if (result == null) {
                    onResult(null, "读取结果为空")
                    return@launch
                }

                val map = mutableMapOf<String, Boolean>()

                // 5G Network
                val nrArray: IntArray? = result.getIntArray(
                    android.telephony.CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY
                )
                map["nr_5g"] = nrArray != null && nrArray.contains(1) && nrArray.contains(2)

                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    map["vonr"] = result.getBoolean(
                        android.telephony.CarrierConfigManager.KEY_VONR_ENABLED_BOOL, false
                    )
                }

                val ssrsrp: IntArray? = result.getIntArray(
                    android.telephony.CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY
                )
                map["5g_signal"] = ssrsrp != null && ssrsrp.size >= 4

                val nrAdv: Int = result.getInt("nr_advanced_threshold_bandwidth_khz_int", 0)
                map["5ga_icon"] = nrAdv == 130000

                // Voice
                map["volte"] = result.getBoolean(
                    android.telephony.CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, false
                )
                map["vowifi"] = result.getBoolean(
                    android.telephony.CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, false
                )
                map["vilte"] = result.getBoolean(
                    android.telephony.CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, false
                )

                // Display & Auxiliary
                map["lte_4g"] = result.getBoolean("show_4g_for_lte_data_icon_bool", false)
                map["cross_sim"] = result.getBoolean("carrier_cross_sim_ims_available_bool", false)
                map["ut"] = result.getBoolean(
                    android.telephony.CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, false
                )

                if (map.isEmpty()) {
                    onResult(null, "读取结果解析为空")
                } else {
                    onResult(map, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "readCarrierConfig error", e)
                onResult(null, "读取失败: ${e.message}")
            }
        }
    }

    /**
     * 还原 CarrierConfig：通过 instrumentation 通道注入 reset bundle。
     */
    fun restoreCarrierConfig(context: Context, subId: Int, onResult: (Boolean, String) -> Unit) {
        if (!hasShizukuPermission()) {
            onResult(false, "Shizuku 未授权")
            return
        }
        GlobalScope.launch {
            try {
                val bundle = ImsModifier.buildResetBundle()
                bundle.putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, subId)
                val msg = ShizukuProviderWrapper.overrideImsConfig(context, bundle)

                // 还原 D 组网络属性
                executeCommand("setprop persist.vendor.radio.nr_sa_fast_camp '' 2>/dev/null; " +
                               "setprop 5g_ca_enable '' 2>/dev/null; " +
                               "setprop dynamic_sar '' 2>/dev/null; " +
                               "setprop smart_data_switch '' 2>/dev/null")

                // 自动开关一次飞行模式刷新基带
                executeCommand("cmd connectivity airplane-mode enable; sleep 1; cmd connectivity airplane-mode disable")

                if (msg == null) {
                    onResult(true, "CarrierConfig 已还原")
                } else {
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "restoreCarrierConfig error", e)
                onResult(false, "还原失败: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    /**
     * ⚠️ 副本（旧版）：一键注入 5G 的旧入口，功能已与现行 applyCarrierConfig 同步。
     *
     * 本函数是 applyCarrierConfig 的副本，仅保留给历史调用方（如旧小组件/旧入口）兼容使用。
     * 当前功能与信号页「一键注入以上配置」完全一致：
     * - 5G 核心网络：VoNR / 5G SA（含 nr_sa_disable_policy_int=0 兜底）/ 跨 SIM 通话
     * - 信号显示与阈值增强：5G+ 图标 / 国内信号阈值（RSRP + SS-RSRQ + SS-SINR 按 3GPP 档位）
     * - 基础通信：VoLTE / ViLTE / UT / LTE 显 4G 图标 / VoWiFi
     * 若 applyCarrierConfig 后续有新增强项，请同步更新本副本的 fullMap。
     */
    @Deprecated("Use applyCarrierConfig instead")
    @OptIn(DelicateCoroutinesApi::class)
    fun applyImsConfig(context: Context, subId: Int = -1, onResult: (Boolean, String) -> Unit) {
        if (!hasShizukuPermission()) {
            onResult(false, "Shizuku 未授权")
            return
        }

        GlobalScope.launch {
            try {
                executeCommand("cmd phone set-preferred-network-type 20")
                executeCommand("settings put global vonr_enabled 1")
                executeCommand("settings put global carrier_config_version 3")
            } catch (_: Exception) {}
        }

        // 与信号页 SignalDashboardUI 的 toggleMap 保持一致（全量开启）
        val fullMap = mapOf(
            "vonr" to true, "nr_5g" to true, "5g_signal" to true,
            "5ga_icon" to true, "volte" to true, "vowifi" to true,
            "vilte" to true, "lte_4g" to true, "cross_sim" to true, "ut" to true
        )
        applyCarrierConfig(context, subId, fullMap, onResult)
    }

    /**
     * 获取可用 SIM 卡槽列表
     */
    fun getAvailableSimSlots(context: Context): List<SimSlotInfo> {
        val slots = mutableListOf<SimSlotInfo>()
        try {
            val subscriptionManager = context.getSystemService(android.telephony.SubscriptionManager::class.java)
                ?: return emptyList()

            val subInfoList = if (hasShizukuPermission()) {
                try {
                    val method = subscriptionManager.javaClass.getMethod("getAvailableSubscriptionInfoList")
                    method.invoke(subscriptionManager) as? List<*> ?: subscriptionManager.activeSubscriptionInfoList
                } catch (e: Exception) {
                    subscriptionManager.activeSubscriptionInfoList
                }
            } else {
                subscriptionManager.activeSubscriptionInfoList
            }

            subInfoList?.forEach { info ->
                if (info != null) {
                    val subId = try {
                        info.javaClass.getMethod("getSubscriptionId").invoke(info) as Int
                    } catch (e: Exception) { -1 }
                    val slotIndex = try {
                        info.javaClass.getMethod("getSimSlotIndex").invoke(info) as Int
                    } catch (e: Exception) { -1 }
                    val carrierName = try {
                        info.javaClass.getMethod("getCarrierName").invoke(info)?.toString() ?: ""
                    } catch (e: Exception) { "" }
                    val mccMnc = try {
                        val mcc = info.javaClass.getMethod("getMcc").invoke(info) as? Int ?: 0
                        val mnc = info.javaClass.getMethod("getMnc").invoke(info) as? Int ?: 0
                        if (mcc > 0) String.format("%03d%02d", mcc, mnc) else ""
                    } catch (e: Exception) { "" }

                    if (subId > 0 && slotIndex >= 0) {
                        val embedded = try {
                            info.javaClass.getMethod("isEmbedded").invoke(info) as? Boolean ?: false
                        } catch (e: Exception) { false }
                        slots.add(SimSlotInfo(slotIndex, subId, carrierName.ifEmpty { "SIM ${slotIndex + 1}" }, mccMnc, embedded))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return slots.sortedBy { it.slotIndex }
    }

    // ==========================================
    // Shell 底层命令（仅用于 settings / sysfs / pm 等非 instrumentation 场景）
    // ==========================================

    /**
     * 执行 shell 命令（Shizuku newProcess，shell 级权限）
     * 仅用于 settings、cat sysfs、pm install 等场景，不再用于 instrumentation。
     */
    fun executeCommand(command: String): Result<String> {
        if (!hasShizukuPermission()) {
            return Result.failure(Exception("Shizuku 未授权"))
        }
        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val errReader = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream))
            val errorOut = StringBuilder()
            var errLine: String?
            while (errReader.readLine().also { errLine = it } != null) {
                errorOut.append(errLine).append("\n")
            }

            process.waitFor()
            val exitCode = process.exitValue()

            if (exitCode == 0) {
                Result.success(output.toString().trim())
            } else {
                val errReason = errorOut.toString().trim().ifEmpty { "退出码: $exitCode, 无详细报错" }
                Result.failure(Exception(errReason))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @JvmStatic
    fun executeCommandOrNull(command: String): String? = executeCommand(command).getOrNull()

    /**
     * 流式写入：将 inputFile 分块 pipe 到 Shizuku 进程的 stdin，
     * 然后执行 shell 命令把 stdin 重定向到 outputPath。
     * 全程不把文件加载到内存，解决大文件 OOM 和跨分区权限问题。
     */
    fun streamFileTo(command: String, inputFile: java.io.File): Result<String> {
        if (!hasShizukuPermission()) {
            return Result.failure(Exception("Shizuku 未授权"))
        }
        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true

            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

            val output = StringBuilder()
            val errorOut = StringBuilder()

            val stdoutThread = Thread {
                try {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        output.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }

            val stderrThread = Thread {
                try {
                    val errReader = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream))
                    var errLine: String?
                    while (errReader.readLine().also { errLine = it } != null) {
                        errorOut.append(errLine).append("\n")
                    }
                } catch (_: Exception) {}
            }

            stdoutThread.start()
            stderrThread.start()

            // Stream file content to stdin in 64KB chunks (avoids OOM)
            val os = process.outputStream
            java.io.FileInputStream(inputFile).use { fis ->
                val buf = ByteArray(65536)
                var read: Int
                while (fis.read(buf).also { read = it } != -1) {
                    os.write(buf, 0, read)
                }
            }
            os.flush()
            os.close()

            process.waitFor()
            stdoutThread.join(5000)
            stderrThread.join(5000)
            val exitCode = process.exitValue()

            if (exitCode == 0) {
                Result.success(output.toString().trim())
            } else {
                val errReason = errorOut.toString().trim().ifEmpty { "退出码: $exitCode, 无详细报错" }
                Result.failure(Exception(errReason))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行 shell 命令并通过 stdin 传入数据（用于 pm install-write - 等需要管道的场景）
     */
    fun executeCommandWithStdin(command: String, stdinData: ByteArray, useShell: Boolean = true): Result<String> {
        if (!hasShizukuPermission()) {
            return Result.failure(Exception("Shizuku 未授权"))
        }
        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true

            // Use array form by default (no shell) to avoid shell buffering stdin
            // Set useShell=true for commands with redirection (>, <, |)
            val cmdArray = if (useShell) {
                arrayOf("sh", "-c", command)
            } else {
                command.split(" ").filter { it.isNotEmpty() }.toTypedArray()
            }
            val process = newProcessMethod.invoke(null, cmdArray, null, null) as Process

            // Read stdout and stderr concurrently
            val output = StringBuilder()
            val errorOut = StringBuilder()

            val stdoutThread = Thread {
                try {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        output.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }

            val stderrThread = Thread {
                try {
                    val errReader = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream))
                    var errLine: String?
                    while (errReader.readLine().also { errLine = it } != null) {
                        errorOut.append(errLine).append("\n")
                    }
                } catch (_: Exception) {}
            }

            stdoutThread.start()
            stderrThread.start()

            val os = process.outputStream
            os.write(stdinData)
            os.flush()
            os.close()

            process.waitFor()
            stdoutThread.join(5000)
            stderrThread.join(5000)
            val exitCode = process.exitValue()

            if (exitCode == 0) {
                Result.success(output.toString().trim())
            } else {
                val errReason = errorOut.toString().trim().ifEmpty { "退出码: $exitCode, 无详细报错" }
                Result.failure(Exception(errReason))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 极客工具箱 (Geek Tools) 底层方法
    // ==========================================

    fun getBatteryHealth(): Result<Map<String, String>> {
        val cycleCountRes = executeCommand("cat /sys/class/power_supply/battery/cycle_count")
        val chargeFullRes = executeCommand("cat /sys/class/power_supply/battery/charge_full")
        val chargeDesignRes = executeCommand("cat /sys/class/power_supply/battery/charge_full_design")
        val tempRes = executeCommand("cat /sys/class/power_supply/battery/temp")

        if (cycleCountRes.isFailure) return Result.failure(Exception("无法读取电池节点，可能设备不支持或 Shizuku 未授权"))

        val data = mutableMapOf<String, String>()
        data["cycle_count"] = cycleCountRes.getOrNull() ?: "未知"
        data["charge_full"] = chargeFullRes.getOrNull()?.let { (it.toFloatOrNull() ?: 0f) / 1000 }.toString() + " mAh"
        data["charge_design"] = chargeDesignRes.getOrNull()?.let { (it.toFloatOrNull() ?: 0f) / 1000 }.toString() + " mAh"
        data["temp"] = tempRes.getOrNull()?.let { (it.toFloatOrNull() ?: 0f) / 10 }.toString() + " °C"

        return Result.success(data)
    }

    fun setRefreshRate(rate: Float): Result<String> {
        val minRes = executeCommand("settings put system min_refresh_rate $rate")
        val peakRes = executeCommand("settings put system peak_refresh_rate $rate")
        return if (minRes.isSuccess && peakRes.isSuccess) {
            Result.success("已锁定刷新率至 ${rate}Hz")
        } else {
            Result.failure(Exception("修改失败"))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun injectImsConfig(
        context: Context,
        groupBasic: Boolean,
        group5gCore: Boolean,
        groupUiEnhancement: Boolean,
        subId: Int = -1
    ): Result<String> {
        try {
            if (!hasShizukuPermission()) {
                return Result.failure(Exception("Shizuku permission not granted"))
            }

            val toggleMap = mapOf(
                "volte" to groupBasic,
                "vowifi" to groupBasic,
                "vilte" to groupBasic,
                "ut" to groupBasic,
                "lte_4g" to (groupBasic || groupUiEnhancement),
                "nr_5g" to group5gCore,
                "vonr" to group5gCore,
                "cross_sim" to group5gCore,
                "5g_signal" to groupUiEnhancement,
                "5ga_icon" to groupUiEnhancement,
                "show_5ga" to groupUiEnhancement,
                "5g_icon_upgrade" to groupUiEnhancement
            )

            val bundle = ImsModifier.buildBundle(
                carrierName = null,
                countryISO = "cn",
                countryMcc = null,
                countryMncHint = null,
                enableVoLTE = toggleMap["volte"] == true,
                enableVoWiFi = toggleMap["vowifi"] == true,
                enableVT = toggleMap["vilte"] == true,
                enableVoNR = toggleMap["vonr"] == true,
                enableCrossSIM = toggleMap["cross_sim"] == true,
                enableUT = toggleMap["ut"] == true,
                enable5GNR = toggleMap["nr_5g"] == true,
                enable5GThreshold = toggleMap["5g_signal"] == true,
                enable5GPlusIcon = toggleMap["5ga_icon"] == true,
                enableShow4GForLTE = toggleMap["lte_4g"] == true,
                show5GA = toggleMap["show_5ga"] == true,
                enable5GIconUpgrade = toggleMap["5g_icon_upgrade"] == true
            )
            bundle.putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, subId)

            // injectImsConfig 是同步方法，需要在协程中调用 suspend 函数
            // 使用 runBlocking 桥接，因为调用方期望同步返回
            return kotlinx.coroutines.runBlocking {
                val msg = ShizukuProviderWrapper.overrideImsConfig(context, bundle)
                if (msg == null) Result.success("注入成功") else Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun setStatusBarIcons(blacklist: String): Result<String> {
        return executeCommand("settings put secure icon_blacklist \"$blacklist\"")
    }

    fun installApk(apkPath: String): Result<String> {
        return executeCommand("pm install --bypass-low-target-sdk-block -d \"$apkPath\"")
    }
}
