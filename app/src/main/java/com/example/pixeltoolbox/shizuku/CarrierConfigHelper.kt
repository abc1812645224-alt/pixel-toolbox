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
import android.os.PersistableBundle
import android.os.SystemClock
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager

/**
 * 通过 app_process + Shizuku newProcess 运行，直接调用 CarrierConfigManager.overrideConfig()。
 * 参数: Base64 编码的 JSON，包含 subId 和 toggleMap。仅写入 toggle 为 true 的配置项。
 */
object CarrierConfigHelper {
    @JvmStatic
    fun main(args: Array<String>) {
        // 等待 Shizuku binder
        var waited = 0
        while (!rikka.shizuku.Shizuku.pingBinder() && waited < 50) {
            SystemClock.sleep(100)
            waited++
        }
        if (waited >= 50) {
            System.err.println("Shizuku binder not ready")
            System.exit(1)
            return
        }

        // 解析 Base64 JSON 参数: { "subId": int, "nr_5g": bool, "vonr": bool, ... }
        val encoded = args.getOrNull(0) ?: ""
        val argsJson = if (encoded.isNotEmpty()) {
            String(android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP))
        } else {
            "{}"
        }
        val json = org.json.JSONObject(argsJson)
        val subId = json.optInt("subId", -1)

        try {
            // 1. 通过反射获取 IActivityManager，用 ShizukuBinderWrapper 包装
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getDeclaredMethod("getService", String::class.java)
            val binder = getService.invoke(null, "activity") as android.os.IBinder

            val iamStub = Class.forName("android.app.IActivityManager\$Stub")
            val asInterface = iamStub.getDeclaredMethod("asInterface", android.os.IBinder::class.java)
            val am = asInterface.invoke(null, rikka.shizuku.ShizukuBinderWrapper(binder))

            // 2. startDelegateShellPermissionIdentity
            val osClass = Class.forName("android.system.Os")
            val uid = osClass.getDeclaredMethod("getuid").invoke(null) as Int
            am.javaClass.getDeclaredMethod(
                "startDelegateShellPermissionIdentity",
                Integer.TYPE,
                Array<String>::class.java
            ).invoke(am, uid, null)

            try {
                // 3. 通过 ActivityThread 获取系统 Context
                val atClass = Class.forName("android.app.ActivityThread")
                val at = atClass.getMethod("systemMain").invoke(null)
                val context = atClass.getMethod("getSystemContext").invoke(at) as Context

                val cm = context.getSystemService(CarrierConfigManager::class.java)!!
                val sm = context.getSystemService(SubscriptionManager::class.java)!!

                // 确定要注入的 subId
                val subIds: IntArray = if (subId == -1) {
                    try {
                        sm.javaClass.getMethod("getActiveSubscriptionIdList").invoke(sm) as IntArray
                    } catch (e: Exception) {
                        val infoList = sm.activeSubscriptionInfoList
                        infoList?.map { it.subscriptionId }?.toIntArray() ?: intArrayOf()
                    }
                } else {
                    intArrayOf(subId)
                }

                for (sid in subIds) {
                    val pb = PersistableBundle()

                    // === 5G NR NSA/SA ===
                    if (json.optBoolean("nr_5g", false)) {
                        pb.putIntArray(
                            CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                            intArrayOf(1, 2) // 1=NSA, 2=SA
                        )
                    }

                    // === VoNR === (Android 14+ only)
                    if (json.optBoolean("vonr", false) && android.os.Build.VERSION.SDK_INT >= 34) {
                        pb.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true)
                        pb.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
                    }

                    // === VoLTE ===
                    if (json.optBoolean("volte", false)) {
                        pb.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
                        pb.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
                        pb.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
                        pb.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false)
                    }

                    // === LTE显示4G ===
                    if (json.optBoolean("lte_4g", false)) {
                        pb.putBoolean("show_4g_for_lte_data_icon_bool", true)
                    }

                    // === ViLTE ===
                    if (json.optBoolean("vilte", false)) {
                        pb.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true)
                    }

                    // === UT补充服务 ===
                    if (json.optBoolean("ut", false)) {
                        pb.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true)
                    }

                    // === 跨SIM通话 ===
                    if (json.optBoolean("cross_sim", false)) {
                        pb.putBoolean("carrier_cross_sim_ims_available_bool", true)
                        pb.putBoolean("enable_cross_sim_calling_on_opportunistic_data_bool", true)
                    }

                    // === VoWiFi ===
                    if (json.optBoolean("vowifi", false)) {
                        pb.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true)
                        pb.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true)
                        pb.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true)
                        pb.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true)
                        pb.putBoolean("show_wifi_calling_icon_in_status_bar_bool", true)
                        pb.putInt("wfc_spn_format_idx_int", 6)
                    }

                    // === 5G信号强度调整 ===
                    if (json.optBoolean("5g_signal", false)) {
                        pb.putIntArray(
                            CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                            intArrayOf(-128, -118, -108, -98)
                        )
                    }

                    // === 5GA / 5G+ 图标 ===
                    // 带宽阈值：四大运营商（移动/联通/电信/广电）NR 带宽普遍为 100MHz，
                    // 阈值设为 100MHz(100000kHz)，实际带宽 >=100MHz 即触发 5G+（NR_ADVANCED）。
                    if (json.optBoolean("5ga_icon", false)) {
                        pb.putInt("nr_advanced_threshold_bandwidth_khz_int", 100000)
                        pb.putBoolean("include_lte_for_nr_advanced_threshold_bandwidth_bool", false)
                        pb.putIntArray("additional_nr_advanced_bands_int_array",
                            intArrayOf(1, 3, 8, 28, 41, 78, 79))
                        pb.putString("5g_icon_configuration_string",
                            "connected_mmwave:5G_Plus,connected:5G,connected_rrc_idle:5G," +
                            "not_restricted_rrc_idle:5G,not_restricted_rrc_con:5G")
                        pb.putInt("nr_advanced_capable_pco_id_int", 0)
                    }

                    // 反射调用 overrideConfig，带 persistent 回退策略
                    try {
                        cm.javaClass.getMethod(
                            "overrideConfig",
                            Int::class.javaPrimitiveType,
                            PersistableBundle::class.java,
                            Boolean::class.javaPrimitiveType
                        ).invoke(cm, sid, pb, true)
                    } catch (persistentError: Throwable) {
                        try {
                            cm.javaClass.getMethod(
                                "overrideConfig",
                                Int::class.javaPrimitiveType,
                                PersistableBundle::class.java
                            ).invoke(cm, sid, pb)
                        } catch (fallbackError: Throwable) {
                            fallbackError.addSuppressed(persistentError)
                            throw fallbackError
                        }
                    }
                }

                System.out.println("CarrierConfig override success for ${subIds.size} SIM(s)")
                System.exit(0)
            } finally {
                try {
                    am.javaClass.getDeclaredMethod("stopDelegateShellPermissionIdentity").invoke(am)
                } catch (_: Exception) {}
            }
        } catch (e: Throwable) {
            System.err.println("Error: ${e.message}")
            e.printStackTrace()
            System.exit(1)
        }
    }
}
