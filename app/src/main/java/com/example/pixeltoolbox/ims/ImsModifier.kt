package com.example.pixeltoolbox.ims

import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.os.SystemClock
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager

object ImsModifier {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            android.os.Looper.prepareMainLooper()
        } catch (e: Exception) {}

        // Android 17: catch Shizuku init failure in app_process context
        try {
            Class.forName("rikka.shizuku.Shizuku")
        } catch (_: Throwable) {}

        // defer cleanup
        var am: Any? = null

        try {
            val groupBasic = args.getOrNull(0)?.toBoolean() ?: false
            val group5gCore = args.getOrNull(1)?.toBoolean() ?: false
            val groupUiEnhancement = args.getOrNull(2)?.toBoolean() ?: false
            val selectedSubId = args.getOrNull(3)?.toInt() ?: -1

            // ───── 提权：startDelegateShellPermissionIdentity ─────
            // Android 17 禁止 shell UID 直接调用 carrier_config 服务的
            // overrideConfig。对齐 CarrierConfigWriter：先获取 IActivityManager，
            // 通过 ShizukuBinderWrapper + startDelegateShellPermissionIdentity
            // 将 shell UID 提升为 app 自身 UID，再调用 CarrierConfigManager。
            var waited = 0
            while (!rikka.shizuku.Shizuku.pingBinder() && waited < 50) {
                SystemClock.sleep(100)
                waited++
            }

            val smClass = Class.forName("android.os.ServiceManager")
            val activityBinder = smClass.getDeclaredMethod("getService", String::class.java)
                .invoke(null, "activity") as android.os.IBinder
            val iamStub = Class.forName("android.app.IActivityManager\$Stub")
            val asInterfaceMethod = iamStub.declaredMethods.first { m ->
                m.name == "asInterface" && m.parameterTypes.size == 1 &&
                android.os.IBinder::class.java.isAssignableFrom(m.parameterTypes[0])
            }
            am = asInterfaceMethod.invoke(
                null,
                rikka.shizuku.ShizukuBinderWrapper(activityBinder)
            )
            val osClass = Class.forName("android.system.Os")
            val uid = osClass.getDeclaredMethod("getuid").invoke(null) as Int
            am.javaClass.getDeclaredMethod(
                "startDelegateShellPermissionIdentity",
                Int::class.javaPrimitiveType,
                Array<String>::class.java
            ).invoke(am, uid, null)

            // ───── 构建 CarrierConfig ─────
            val atClass = Class.forName("android.app.ActivityThread")
            val at = atClass.getMethod("systemMain").invoke(null)
            val context = atClass.getMethod("getSystemContext").invoke(at) as Context

            val cm = context.getSystemService(CarrierConfigManager::class.java)!!
            val sm = context.getSystemService(SubscriptionManager::class.java)!!

            val effectiveSubId = if (selectedSubId == -1) {
                try {
                    val ids = sm.javaClass.getMethod("getActiveSubscriptionIdList").invoke(sm) as IntArray
                    ids.firstOrNull() ?: SubscriptionManager.getDefaultDataSubscriptionId()
                } catch (e: Exception) {
                    SubscriptionManager.getDefaultDataSubscriptionId()
                }
            } else {
                selectedSubId
            }

            val b = PersistableBundle()

            if (groupBasic) {
                b.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
                b.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
                b.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
                b.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false)

                b.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true)
                b.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true)
                b.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true)
                b.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true)
                b.putBoolean("show_wifi_calling_icon_in_status_bar_bool", true)
                b.putInt("wfc_spn_format_idx_int", 6)

                b.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true)
                b.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true)
            }

            if (group5gCore) {
                b.putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY, intArrayOf(1, 2))
                if (Build.VERSION.SDK_INT >= 34) {
                    b.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true)
                    b.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
                }
                b.putInt("nr_sa_disable_policy_int", 0)
                b.putBoolean("carrier_cross_sim_ims_available_bool", true)
                b.putBoolean("enable_cross_sim_calling_on_opportunistic_data_bool", true)
            }

            if (groupUiEnhancement) {
                b.putBoolean("show_4g_for_lte_data_icon_bool", true)
                val thresholds = intArrayOf(-128, -118, -108, -98)
                b.putIntArray(CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY, thresholds)
                b.putIntArray(CarrierConfigManager.KEY_5G_NR_SSRSRQ_THRESHOLDS_INT_ARRAY, intArrayOf(-38, -28, -18, -8))
                b.putIntArray(CarrierConfigManager.KEY_5G_NR_SSSINR_THRESHOLDS_INT_ARRAY, intArrayOf(-23, -13, -3, 7))
                // 5G+ 带宽阈值：四大运营商（移动/联通/电信/广电）NR 带宽普遍为 100MHz，
                // 阈值设为 100MHz(100000kHz)，实际带宽 >=100MHz 即触发 5G+（NR_ADVANCED）。
                b.putInt("nr_advanced_threshold_bandwidth_khz_int", 100000)
                b.putBoolean("include_lte_for_nr_advanced_threshold_bandwidth_bool", false)
                b.putIntArray("additional_nr_advanced_bands_int_array", intArrayOf(1, 3, 8, 28, 41, 78, 79))
                b.putString("5g_icon_configuration_string", "connected_mmwave:5G_Plus,connected:5G,connected_rrc_idle:5G,not_restricted_rrc_idle:5G,not_restricted_rrc_con:5G")
                b.putInt("nr_advanced_capable_pco_id_int", 0)
            }

            b.putInt("pixel_toolbox_config_version", 3)
            b.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING, ":" + System.currentTimeMillis())

            // ───── 注入：使用 CarrierConfigManager.overrideConfig()（对齐 CarrierConfigWriter） ─────
            if (!b.isEmpty) {
                try {
                    cm.javaClass.getMethod(
                        "overrideConfig",
                        Int::class.javaPrimitiveType,
                        PersistableBundle::class.java,
                        Boolean::class.javaPrimitiveType
                    ).invoke(cm, effectiveSubId, b, true)
                } catch (_: NoSuchMethodException) {
                    cm.javaClass.getMethod(
                        "overrideConfig",
                        Int::class.javaPrimitiveType,
                        PersistableBundle::class.java
                    ).invoke(cm, effectiveSubId, b)
                }
            }

            println("SUCCESS")
            System.exit(0)
        } catch (t: Throwable) {
            println("ERROR: ${t.message ?: t.javaClass.simpleName}")
            t.printStackTrace()
            System.exit(1)
        } finally {
            // ───── 清理：stopDelegateShellPermissionIdentity ─────
            if (am != null) {
                try {
                    am.javaClass.getDeclaredMethod("stopDelegateShellPermissionIdentity").invoke(am)
                } catch (_: Throwable) {}
            }
        }
    }
}
