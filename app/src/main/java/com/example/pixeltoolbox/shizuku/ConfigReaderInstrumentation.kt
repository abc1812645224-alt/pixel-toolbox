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
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager

class ConfigReaderInstrumentation : Instrumentation() {

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
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        val results = Bundle()
        val args: Bundle = arguments ?: Bundle()

        var waited = 0
        while (!rikka.shizuku.Shizuku.pingBinder() && waited < 50) {
            Thread.sleep(100)
            waited++
        }
        if (waited >= 50) {
            results.putBoolean(KEY_RESULT, false)
            results.putString(KEY_RESULT_MSG, "Shizuku binder not ready")
            finish(Activity.RESULT_OK, results)
            return
        }

        try {
            val binder = android.os.ServiceManager.getService(Context.ACTIVITY_SERVICE)
            val am = Class.forName("android.app.IActivityManager\$Stub")
                .getMethod("asInterface", android.os.IBinder::class.java)
                .invoke(null, rikka.shizuku.ShizukuBinderWrapper(binder))
            val startMethod = am.javaClass.getMethod(
                "startDelegateShellPermissionIdentity",
                Int::class.javaPrimitiveType,
                Array<String>::class.java
            )
            startMethod.invoke(am, android.system.Os.getuid(), null)

            try {
                val cm: CarrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
                val selectedSubId: Int = args.getInt(KEY_SELECT_SIM_ID, -1)
                val subId: Int
                if (selectedSubId != -1) {
                    subId = selectedSubId
                } else {
                    val sm = context.getSystemService(SubscriptionManager::class.java)!!
                    val activeList = sm.activeSubscriptionInfoList
                    subId = if (activeList != null && activeList.isNotEmpty()) {
                        activeList[0].subscriptionId
                    } else {
                        SubscriptionManager.getDefaultDataSubscriptionId()
                    }
                }

                val getConfigMethod = cm.javaClass.getMethod("getConfig", Int::class.javaPrimitiveType)
                val config = getConfigMethod.invoke(cm, subId) as PersistableBundle

                // 5G Network
                val nrArray: IntArray? = config.getIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY)
                results.putBoolean(KEY_5G_NR, nrArray != null && nrArray.contains(1) && nrArray.contains(2))

                if (Build.VERSION.SDK_INT >= 34) {
                    results.putBoolean(KEY_VONR, config.getBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, false))
                }

                val ssrsrp: IntArray? = config.getIntArray(CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY)
                results.putBoolean(KEY_5G_SIGNAL, ssrsrp != null && ssrsrp.size >= 4)

                val nrAdv: Int = config.getInt("nr_advanced_threshold_bandwidth_khz_int", 0)
                results.putBoolean(KEY_5GA_ICON, nrAdv == 100000)

                // Voice
                results.putBoolean(KEY_VOLTE, config.getBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, false))
                results.putBoolean(KEY_VOWIFI, config.getBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, false))
                results.putBoolean(KEY_VILTE, config.getBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, false))

                // Display & Auxiliary
                results.putBoolean(KEY_LTE_4G, config.getBoolean("show_4g_for_lte_data_icon_bool", false))
                results.putBoolean(KEY_CROSS_SIM, config.getBoolean("carrier_cross_sim_ims_available_bool", false))
                results.putBoolean(KEY_UT, config.getBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, false))

                results.putBoolean(KEY_RESULT, true)
            } finally {
                am.javaClass.getMethod("stopDelegateShellPermissionIdentity").invoke(am)
            }
        } catch (t: Throwable) {
            results.putBoolean(KEY_RESULT, false)
            results.putString(KEY_RESULT_MSG, t.message ?: t.javaClass.simpleName)
        }
        finish(Activity.RESULT_OK, results)
    }
}
