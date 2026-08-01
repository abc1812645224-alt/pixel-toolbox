package com.example.pixeltoolbox.shizuku

import android.content.Context
import android.content.pm.PackageManager
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
                val bundle = ImsModifier.buildBundle(
                    carrierName = null,
                    countryISO = null,
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

                val msg = ShizukuProviderWrapper.overrideImsConfig(context, bundle)
                if (msg == null) {
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
                map["5ga_icon"] = nrAdv == 110000

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
     * 一键注入 5G：shell 命令（保持 executeCommand）+ CarrierConfig（走 instrumentation）
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
    fun executeCommandWithStdin(command: String, stdinData: ByteArray, useShell: Boolean = false): Result<String> {
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
                countryISO = null,
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
