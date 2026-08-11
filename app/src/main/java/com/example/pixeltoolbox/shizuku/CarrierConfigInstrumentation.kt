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

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CarrierConfigInstrumentation : Instrumentation() {

    companion object {
        const val KEY_SELECT_SIM_ID = "select_sim_id"
        const val KEY_RESULT = "result"
        const val KEY_RESULT_MSG = "result_msg"

        const val KEY_VONR = "vonr"
        const val KEY_5G_NR = "nr_5g"
        const val KEY_5G_SIGNAL = "5g_signal"
        const val KEY_5GA_ICON = "5ga_icon"
        const val KEY_VOLTE = "volte"
        const val KEY_VOWIFI = "vowifi"
        const val KEY_VILTE = "vilte"
        const val KEY_LTE_4G = "lte_4g"
        const val KEY_CROSS_SIM = "cross_sim"
        const val KEY_UT = "ut"

        private val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    }

    private var logFile: File? = null

    private fun log(msg: String) {
        val ts = Companion.sdf.format(Date())
        val line = "$ts $msg\n"
        val f = logFile ?: return
        try {
            f.appendText(line)
        } catch (_: Throwable) {}
    }

    override fun onCreate(arguments: Bundle?) {
        val args = arguments ?: Bundle()
        val results = Bundle()

        // 确定日志路径 —— 多级 fallback
        logFile = try { File(targetContext.getExternalFilesDir(null), "instrument_log.txt") } catch (_: Throwable) { null }
        if (logFile == null) {
            logFile = try {
                val sdcard = Environment.getExternalStorageDirectory()
                File(sdcard, "pixeltoolbox_result.txt")
            } catch (_: Throwable) { null }
        }
        if (logFile == null) {
            logFile = File("/data/local/tmp/pixeltoolbox_result.txt")
        }
        // 清理旧日志
        try { logFile?.delete() } catch (_: Throwable) {}
        log("Log file: ${logFile?.absolutePath}")

        log("=== Instrumentation started ===")

        try {
            // Step 1: ping Shizuku binder
            log("Step1: pinging Shizuku binder")
            var waited = 0
            var binderOk = false
            try {
                while (!rikka.shizuku.Shizuku.pingBinder() && waited < 50) {
                    Thread.sleep(100)
                    waited++
                }
                binderOk = waited < 50
            } catch (t: Throwable) {
                log("Step1 ERROR: pingBinder threw ${t.javaClass.simpleName}: ${t.message}")
            }
            if (!binderOk) {
                log("Step1 FAIL: binder not ready after ${waited * 100}ms")
                results.putBoolean(KEY_RESULT, false)
                results.putString(KEY_RESULT_MSG, "Shizuku binder not ready")
                finish(Activity.RESULT_OK, results)
                log("=== Instrumentation ended (early: binder fail) ===")
                return
            }
            log("Step1 OK: binder connected in ${waited * 100}ms")

            // Step 2: run overrideConfig
            log("Step2: entering overrideConfig")
            try {
                overrideConfig(args)
                log("Step2 OK: overrideConfig completed")
                results.putBoolean(KEY_RESULT, true)
            } catch (t: Throwable) {
                log("Step2 FAIL: overrideConfig error - ${t.javaClass.simpleName}: ${t.message}")
                var cause: Throwable? = t.cause
                var depth = 0
                while (cause != null && depth < 5) {
                    log("  Caused by: ${cause.javaClass.simpleName}: ${cause.message}")
                    cause = cause.cause
                    depth++
                }
                results.putBoolean(KEY_RESULT, false)
                results.putString(KEY_RESULT_MSG, "${t.javaClass.simpleName}: ${t.message}")
            }
        } catch (t: Throwable) {
            log("FATAL: onCreate crashed - ${t.javaClass.simpleName}: ${t.message}")
            try {
                results.putBoolean(KEY_RESULT, false)
                results.putString(KEY_RESULT_MSG, "FATAL: ${t.javaClass.simpleName}: ${t.message}")
            } catch (_: Throwable) {}
        }

        try {
            finish(Activity.RESULT_OK, results)
            log("=== Instrumentation ended (normal) ===")
        } catch (t: Throwable) {
            log("FATAL: finish() crashed - ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun overrideConfig(arguments: Bundle) {
        log("  overrideConfig: getting binder")
        val binder = try {
            android.os.ServiceManager.getService(Context.ACTIVITY_SERVICE)
        } catch (t: Throwable) {
            log("  overrideConfig ERROR: getService threw ${t.javaClass.simpleName}: ${t.message}")
            throw t
        }
        log("  overrideConfig: binder=$binder")

        val wrappedBinder = rikka.shizuku.ShizukuBinderWrapper(binder)
        val am = try {
            // Android 17: use reflection to avoid NoSuchMethodError from
            // APK-packed IActivityManager$Stub clashing with framework.jar
            val stubClass = Class.forName("android.app.IActivityManager\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            asInterfaceMethod.invoke(null, wrappedBinder)
        } catch (t: Throwable) {
            log("  overrideConfig ERROR: asInterface/ShizukuBinderWrapper threw ${t.javaClass.simpleName}: ${t.message}")
            throw t
        }
        log("  overrideConfig: am OK, calling startDelegateShellPermissionIdentity")

        try {
            val startMethod = am.javaClass.getMethod(
                "startDelegateShellPermissionIdentity",
                Int::class.javaPrimitiveType,
                Array<String>::class.java
            )
            startMethod.invoke(am, android.system.Os.getuid(), null)
            log("  overrideConfig: delegateShellPermission OK")
        } catch (t: Throwable) {
            log("  overrideConfig ERROR: startDelegateShellPermissionIdentity threw ${t.javaClass.simpleName}: ${t.message}")
            throw t
        }

        try {
            log("  overrideConfig: getting CarrierConfigManager")
            val cm = targetContext.getSystemService(CarrierConfigManager::class.java)!!
            val sm = targetContext.getSystemService(SubscriptionManager::class.java)!!
            log("  overrideConfig: got CM and SM")

            val selectedSubId = arguments.getInt(KEY_SELECT_SIM_ID, -1)
            val subIds: IntArray = if (selectedSubId != -1) {
                intArrayOf(selectedSubId)
            } else {
                val activeSubIds = sm.activeSubscriptionInfoList?.map { it.subscriptionId }?.toMutableList()
                    ?: mutableListOf()
                val defaultSubId = SubscriptionManager.getDefaultSubscriptionId()
                val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                if (defaultSubId > 0 && !activeSubIds.contains(defaultSubId)) activeSubIds.add(defaultSubId)
                if (dataSubId > 0 && !activeSubIds.contains(dataSubId)) activeSubIds.add(dataSubId)
                if (activeSubIds.isEmpty()) intArrayOf(1, 2) else activeSubIds.toIntArray()
            }
            log("  overrideConfig: subIds=${subIds.contentToString()}")

            for (sid in subIds) {
                log("  overrideConfig: processing subId=$sid")
                val pb = PersistableBundle()
                val configVersion = ":${System.currentTimeMillis()}"

                if (arguments.getBoolean(KEY_5G_NR, false)) {
                    pb.putIntArray(
                        CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                        intArrayOf(1, 2)
                    )
                }
                if (arguments.getBoolean(KEY_VONR, false) && Build.VERSION.SDK_INT >= 34) {
                    pb.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true)
                    pb.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
                }
                if (arguments.getBoolean(KEY_5G_SIGNAL, false)) {
                    pb.putIntArray(
                        CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                        intArrayOf(-128, -118, -108, -98)
                    )
                }
                if (arguments.getBoolean(KEY_5GA_ICON, false)) {
                    // 5G+ 带宽阈值：四大运营商（移动/联通/电信/广电）NR 带宽普遍为 100MHz，
                    // 阈值设为 100MHz(100000kHz)，实际带宽 >=100MHz 即触发 5G+（NR_ADVANCED）。
                    pb.putInt("nr_advanced_threshold_bandwidth_khz_int", 100000)
                    pb.putBoolean("include_lte_for_nr_advanced_threshold_bandwidth_bool", false)
                    pb.putIntArray("additional_nr_advanced_bands_int_array",
                        intArrayOf(1, 3, 8, 28, 41, 78, 79))
                    pb.putString("5g_icon_configuration_string",
                        "connected_mmwave:5G_Plus,connected:5G,connected_rrc_idle:5G," +
                        "not_restricted_rrc_idle:5G,not_restricted_rrc_con:5G")
                    pb.putInt("nr_advanced_capable_pco_id_int", 0)
                }
                if (arguments.getBoolean(KEY_VOLTE, false)) {
                    pb.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
                    pb.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
                    pb.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
                    pb.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false)
                }
                if (arguments.getBoolean(KEY_VOWIFI, false)) {
                    pb.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true)
                    pb.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true)
                    pb.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true)
                    pb.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true)
                    pb.putBoolean("show_wifi_calling_icon_in_status_bar_bool", true)
                    pb.putInt("wfc_spn_format_idx_int", 6)
                }
                if (arguments.getBoolean(KEY_VILTE, false)) {
                    pb.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true)
                }
                if (arguments.getBoolean(KEY_LTE_4G, false)) {
                    pb.putBoolean("show_4g_for_lte_data_icon_bool", true)
                }
                if (arguments.getBoolean(KEY_CROSS_SIM, false)) {
                    pb.putBoolean("carrier_cross_sim_ims_available_bool", true)
                    pb.putBoolean("enable_cross_sim_calling_on_opportunistic_data_bool", true)
                }
                if (arguments.getBoolean(KEY_UT, false)) {
                    pb.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true)
                }

                pb.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING, configVersion)
                log("  overrideConfig: calling overrideConfig for subId=$sid")

                // CarrierIMS 模式：反射 CarrierConfigManager.overrideConfig()
                // Instrumentation + delegateShellPermissionIdentity 上下文有
                // 完整的 framework 服务初始化，无需绕路到 ICarrierConfigLoader。
                var methodUsed = "overrideConfig(subId,bundle,persist=true)"
                try {
                    cm.javaClass.getMethod(
                        "overrideConfig",
                        Int::class.javaPrimitiveType,
                        PersistableBundle::class.java,
                        Boolean::class.javaPrimitiveType
                    ).invoke(cm, sid, pb, true)
                } catch (persistentError: Throwable) {
                    log("  overrideConfig: persist mode failed (${persistentError.javaClass.simpleName}), trying fallback")
                    try {
                        cm.javaClass.getMethod(
                            "overrideConfig",
                            Int::class.javaPrimitiveType,
                            PersistableBundle::class.java
                        ).invoke(cm, sid, pb)
                        methodUsed = "overrideConfig(subId,bundle)"
                    } catch (fallbackError: Throwable) {
                        log("  overrideConfig ERROR: fallback mode failed - ${fallbackError.javaClass.simpleName}: ${fallbackError.message}")
                        fallbackError.addSuppressed(persistentError)
                        throw fallbackError
                    }
                }
                log("  overrideConfig: subId=$sid OK method=$methodUsed")
            }
        } finally {
            try {
                val stopMethod = am.javaClass.getMethod("stopDelegateShellPermissionIdentity")
                stopMethod.invoke(am)
                log("  overrideConfig: stopDelegateShellPermissionIdentity OK")
            } catch (t: Throwable) {
                log("  overrideConfig: stopDelegateShellPermissionIdentity threw ${t.javaClass.simpleName}: ${t.message}")
            }
        }
    }
}
