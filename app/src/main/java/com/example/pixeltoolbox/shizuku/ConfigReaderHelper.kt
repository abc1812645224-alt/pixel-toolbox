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
import org.json.JSONObject

/**
 * 通过 app_process 运行，以 Shell UID 身份直接读取 CarrierConfig 并输出 JSON 到 stdout。
 */
object ConfigReaderHelper {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            android.os.Looper.prepareMainLooper()
        } catch (e: Exception) {}

        // Android 17: force Shizuku class init in app_process context where
        // Looper might not be ready, catching ExceptionInInitializerError
        try {
            Class.forName("rikka.shizuku.Shizuku")
        } catch (_: Throwable) {
            // Shizuku unavailable in this process; continue with fallback
        }

        val subId = args.getOrNull(0)?.toIntOrNull() ?: -1

        try {
            val atClass = Class.forName("android.app.ActivityThread")
            val at = atClass.getMethod("systemMain").invoke(null)
            val context = atClass.getMethod("getSystemContext").invoke(at) as Context

            val cm = context.getSystemService(CarrierConfigManager::class.java)!!
            val sm = context.getSystemService(SubscriptionManager::class.java)!!

            val effectiveSubId = if (subId == -1) {
                try {
                    val ids = sm.javaClass.getMethod("getActiveSubscriptionIdList").invoke(sm) as IntArray
                    ids.firstOrNull() ?: SubscriptionManager.getDefaultDataSubscriptionId()
                } catch (e: Exception) {
                    SubscriptionManager.getDefaultDataSubscriptionId()
                }
            } else {
                subId
            }

            val getConfigMethod = cm.javaClass.getMethod("getConfig", Int::class.javaPrimitiveType)
            val config = getConfigMethod.invoke(cm, effectiveSubId) as PersistableBundle

            val json = JSONObject()
            val nrArray = config.getIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY)
            json.put("nr_5g", nrArray != null && nrArray.contains(1) && nrArray.contains(2))
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                json.put("vonr", config.getBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, false))
            } else {
                json.put("vonr", false)
            }
            val ssrsrp = config.getIntArray(CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY)
            json.put("5g_signal", ssrsrp != null && ssrsrp.size >= 4)
            json.put("5ga_icon", config.getInt("nr_advanced_threshold_bandwidth_khz_int") == 130000)
            json.put("volte", config.getBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, false))
            json.put("vowifi", config.getBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, false))
            json.put("vilte", config.getBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, false))
            json.put("lte_4g", config.getBoolean("show_4g_for_lte_data_icon_bool", false))
            json.put("cross_sim", config.getBoolean("carrier_cross_sim_ims_available_bool", false))
            json.put("ut", config.getBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, false))

            System.out.println(json.toString())
            System.exit(0)
        } catch (e: Throwable) {
            System.err.println("Error: ${e.message}")
            e.printStackTrace()
            System.exit(1)
        }
    }
}
