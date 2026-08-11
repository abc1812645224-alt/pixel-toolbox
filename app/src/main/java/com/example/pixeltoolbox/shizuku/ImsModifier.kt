package com.example.pixeltoolbox.shizuku

import android.app.Activity
import android.app.IActivityManager
import android.app.Instrumentation
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.os.ServiceManager
import android.system.Os
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.android.internal.telephony.ITelephony
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

class ImsModifier : Instrumentation() {
    companion object Companion {
        private const val TAG = "ImsModifier"
        private const val KEY_NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ = "nr_advanced_threshold_bandwidth_khz_int"
        private const val KEY_ADDITIONAL_NR_ADVANCED_BANDS = "additional_nr_advanced_bands_int_array"
        private const val KEY_5G_ICON_CONFIGURATION = "5g_icon_configuration_string"
        private const val KEY_NR_ADVANCED_CAPABLE_PCO_ID = "nr_advanced_capable_pco_id_int"
        private const val KEY_INCLUDE_LTE_FOR_NR_ADVANCED_THRESHOLD_BANDWIDTH =
            "include_lte_for_nr_advanced_threshold_bandwidth_bool"
        // 5G+ 带宽阈值：四大运营商（移动/联通/电信/广电）NR 带宽普遍为 100MHz，
        // 阈值设为 100MHz(100000kHz)，实际带宽 >=100MHz 即触发 5G+（NR_ADVANCED）。
        private const val NR_ADVANCED_THRESHOLD_KHZ_FOR_5GA = 100_000
        private const val NR_ICON_CONFIGURATION_5GA =
            "connected_mmwave:5G_Plus,connected:5G_Plus,connected_rrc_idle:5G,not_restricted_rrc_idle:5G,not_restricted_rrc_con:5G"
        private const val BUNDLE_COUNTRY_MCC_OVERRIDE = "country_mcc_override"
        private const val BUNDLE_COUNTRY_MNC_HINT = "country_mnc_hint"
        private val NR_ADVANCED_BANDS_FOR_CHINA = intArrayOf(
            1, 3, 8, 28, 41, 78, 79
        )
        const val BUNDLE_SELECT_SIM_ID = "select_sim_id"
        const val BUNDLE_RESET = "reset"
        const val BUNDLE_PREFER_PERSISTENT = "prefer_persistent"
        const val BUNDLE_RESULT = "result"
        const val BUNDLE_RESULT_MSG = "result_msg"

        fun buildResetBundle(): Bundle = Bundle().apply {
            putBoolean(BUNDLE_RESET, true)
        }

        fun buildBundle(
            carrierName: String?,
            countryISO: String?,
            countryMcc: String?,
            countryMncHint: String?,
            enableVoLTE: Boolean,
            enableVoWiFi: Boolean,
            enableVT: Boolean,
            enableVoNR: Boolean,
            enableCrossSIM: Boolean,
            enableUT: Boolean,
            enable5GNR: Boolean,
            enable5GThreshold: Boolean,
            enable5GPlusIcon: Boolean,
            enableShow4GForLTE: Boolean,
            show5GA: Boolean,
            enable5GIconUpgrade: Boolean = false,
            enableCallRecording: Boolean? = null,
        ): Bundle {
            val bundle = Bundle()
            // 运营商名称
            if (carrierName?.isNotBlank() == true) {
                bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
                bundle.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, carrierName)
                bundle.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING, ":3")
            }
            // 运营商国家码
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (countryISO?.isNotBlank() == true) {
                    bundle.putString(
                        CarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING,
                        countryISO
                    )
                }
            }
            normalizeMccForOverride(countryMcc)?.let {
                bundle.putString(BUNDLE_COUNTRY_MCC_OVERRIDE, it)
            }
            normalizeMncForOverride(countryMncHint)?.let {
                bundle.putString(BUNDLE_COUNTRY_MNC_HINT, it)
            }
            // VoLTE 配置
            if (enableVoLTE) {
                bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
                bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
                bundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
                bundle.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false)
                bundle.putBoolean(CarrierConfigManager.KEY_ENHANCED_4G_LTE_ON_BY_DEFAULT_BOOL, true)
                bundle.putBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL, true)
                bundle.putBoolean(CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, true)
            }

            // LTE 显示为 4G
            if (enableShow4GForLTE) {
                bundle.putBoolean("show_4g_for_lte_data_icon_bool", true)
            }

            // VT (视频通话) 配置
            if (enableVT) {
                bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true)
            }

            // UT 补充服务配置
            if (enableUT) {
                bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true)
            }

            // 跨 SIM 通话配置
            if (enableCrossSIM) {
                bundle.putBoolean(
                    CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                    true
                )
                bundle.putBoolean(
                    CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL,
                    true
                )
            }

            // VoWiFi 配置
            if (enableVoWiFi) {
                bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true)
                bundle.putBoolean(
                    CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL,
                    true
                )
                bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true)
                bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true)
                bundle.putBoolean("show_wifi_calling_icon_in_status_bar_bool", true)
                bundle.putInt("wfc_spn_format_idx_int", 6)
                bundle.putBoolean("carrier_default_wfc_ims_roaming_enabled_bool", true)
            }

            // VoNR (5G 语音) 配置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (enableVoNR) {
                    bundle.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true)
                    bundle.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
                }
            }

            // 5G NR 配置
            if (enable5GNR) {
                // SA 禁用策略：0 = 不主动禁用 SA（最宽容档），
                // 显式兜底防止运营商配置误禁 SA。
                bundle.putInt("nr_sa_disable_policy_int", 0)
                bundle.putIntArray(
                    CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                    intArrayOf(
                        CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                        CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA
                    )
                )
                if (enable5GPlusIcon) {
                    bundle.putInt(KEY_NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ, NR_ADVANCED_THRESHOLD_KHZ_FOR_5GA)
                    bundle.putBoolean(
                        KEY_INCLUDE_LTE_FOR_NR_ADVANCED_THRESHOLD_BANDWIDTH,
                        false
                    )
                    bundle.putIntArray(KEY_ADDITIONAL_NR_ADVANCED_BANDS, NR_ADVANCED_BANDS_FOR_CHINA)
                    bundle.putString(KEY_5G_ICON_CONFIGURATION, NR_ICON_CONFIGURATION_5GA)
                    bundle.putInt(KEY_NR_ADVANCED_CAPABLE_PCO_ID, 0)
                    // NR_ADVANCED（5G+）状态时显示 5G+ 图标，缺省会回落为普通 5G
                    bundle.putBoolean("display_5g_advanced_icon_bool", true)
                }
                if (enable5GThreshold) {
                    bundle.putIntArray(
                        CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                        intArrayOf(-128, -118, -108, -98)
                    )
                    // 国内信号阈值增强：SS-RSRQ / SS-SINR 按 3GPP 档位对齐，
                    // 仅影响状态栏信号格显示档位，不影响网络注册与速率。
                    bundle.putIntArray(
                        CarrierConfigManager.KEY_5G_NR_SSRSRQ_THRESHOLDS_INT_ARRAY,
                        intArrayOf(-38, -28, -18, -8)
                    )
                    bundle.putIntArray(
                        CarrierConfigManager.KEY_5G_NR_SSSINR_THRESHOLDS_INT_ARRAY,
                        intArrayOf(-23, -13, -3, 7)
                    )
                }
            }

            // Pixel 通话录音（实验功能）：CarrierConfig 注入 call_recording_enabled
            // enableCallRecording = true 开启；false 显式恢复（关闭注入）；null 不处理
            if (enableCallRecording != null) {
                bundle.putBoolean("call_recording_enabled", enableCallRecording)
            }

            return bundle
        }

        private fun normalizeMccForOverride(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val firstPart = raw.trim().substringBefore('-')
            val digits = firstPart.filter { it.isDigit() }.take(3)
            return digits.takeIf { it.length == 3 }
        }

        private fun normalizeMncForOverride(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val digits = raw.filter { it.isDigit() }.take(3)
            return digits.takeIf { it.length in 2..3 }
        }
    }

    override fun onCreate(arguments: Bundle) {
        var index = 0
        val maxRetries = 50 // 最多等待 5 秒
        while (!Shizuku.pingBinder()) {
            index++
            Log.d(TAG, "wait for shizuku binder ready")
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                break
            }
            if (index >= maxRetries) {
                break
            }
        }
        val results = Bundle()
        if (index >= maxRetries) {
            results.putBoolean(BUNDLE_RESULT, false)
            results.putString(BUNDLE_RESULT_MSG, "shizuku binder is not ready")
            finish(Activity.RESULT_OK, results)
            return
        }
        Log.i(TAG, "shizuku binder is ready")

        try {
            overrideConfig(arguments)
            Log.i(TAG, "overrideConfig success")
            results.putBoolean(BUNDLE_RESULT, true)
        } catch (t: Throwable) {
            Log.e(TAG, "failed to override config", t)
            results.putBoolean(BUNDLE_RESULT, false)
            results.putString(BUNDLE_RESULT_MSG, t.message ?: t.javaClass.simpleName)
        }
        finish(Activity.RESULT_OK, results)
    }

    @Throws(Exception::class)
    private fun overrideConfig(arguments: Bundle) {
        val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
        val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))
        Log.i(TAG, "starting shell permission delegation")
        am.startDelegateShellPermissionIdentity(Os.getuid(), null)
        try {
            val cm = context.getSystemService(CarrierConfigManager::class.java)
            val sm = context.getSystemService(SubscriptionManager::class.java)

            val selectedSubId = arguments.getInt(BUNDLE_SELECT_SIM_ID, -1)
            arguments.remove(BUNDLE_SELECT_SIM_ID)

            val subIds: IntArray = if (selectedSubId == -1) {
                sm.javaClass.getMethod("getActiveSubscriptionIdList").invoke(sm) as IntArray
            } else {
                intArrayOf(selectedSubId)
            }
            val reset = arguments.getBoolean(BUNDLE_RESET, false)
            arguments.remove(BUNDLE_RESET)
            val preferPersistent = arguments.getBoolean(BUNDLE_PREFER_PERSISTENT, false)
            arguments.remove(BUNDLE_PREFER_PERSISTENT)
            val countryMccOverride = arguments.getString(BUNDLE_COUNTRY_MCC_OVERRIDE)
            arguments.remove(BUNDLE_COUNTRY_MCC_OVERRIDE)
            val countryMncHint = arguments.getString(BUNDLE_COUNTRY_MNC_HINT)
            arguments.remove(BUNDLE_COUNTRY_MNC_HINT)
            val baseValues = if (reset) null else arguments.toPersistableBundle()
            for (subId in subIds) {
                val values = baseValues?.let { PersistableBundle(it) }
                // 差分检测：读取当前配置，仅当有差异时才写入，避免无变化注入触发 IMS 重注册
                if (values != null && isConfigUnchanged(cm, subId, values)) {
                    Log.i(TAG, "config unchanged for subId $subId, skipping override to avoid IMS re-registration")
                    continue
                }
                Log.i(TAG, "overrideConfig for subId $subId with values $values")
                applyOverrideConfig(cm, subId, values, preferPersistent = preferPersistent)
                if (reset) {
                    clearCarrierTestOverride(subId)
                } else if (!countryMccOverride.isNullOrBlank()) {
                    applyCarrierTestMccOverride(subId, countryMccOverride, countryMncHint)
                }
            }
        } finally {
            am.stopDelegateShellPermissionIdentity()
            Log.i(TAG, "stopped shell permission delegation")
        }
    }

    @Throws(Exception::class)
    private fun applyOverrideConfig(
        cm: CarrierConfigManager,
        subId: Int,
        values: PersistableBundle?,
        preferPersistent: Boolean,
    ) {
        if (!preferPersistent) {
            invokeOverrideConfig(cm, subId, values, persistent = false)
            return
        }
        try {
            invokeOverrideConfig(cm, subId, values, persistent = true)
            Log.i(TAG, "overrideConfig persistent success for subId $subId")
        } catch (persistentError: Throwable) {
            Log.w(
                TAG,
                "overrideConfig persistent failed for subId $subId, fallback to non-persistent",
                persistentError
            )
            try {
                invokeOverrideConfig(cm, subId, values, persistent = false)
                Log.i(TAG, "overrideConfig fallback non-persistent success for subId $subId")
            } catch (fallbackError: Throwable) {
                fallbackError.addSuppressed(persistentError)
                throw fallbackError
            }
        }
    }

    @Throws(Exception::class)
    private fun invokeOverrideConfig(
        cm: CarrierConfigManager,
        subId: Int,
        values: PersistableBundle?,
        persistent: Boolean,
    ) {
        try {
            cm.javaClass.getMethod(
                "overrideConfig",
                Int::class.javaPrimitiveType,
                PersistableBundle::class.java,
                Boolean::class.javaPrimitiveType
            ).invoke(cm, subId, values, persistent)
        } catch (_: NoSuchMethodException) {
            cm.javaClass.getMethod(
                "overrideConfig",
                Int::class.javaPrimitiveType,
                PersistableBundle::class.java
            ).invoke(cm, subId, values)
        }
    }

    /**
     * 对比待注入的值与当前 CarrierConfig，若无差异则返回 true（跳过注入）。
     * 跳过无差异注入可避免不必要的 IMS 重注册。
     */
    private fun isConfigUnchanged(
        cm: CarrierConfigManager,
        subId: Int,
        newValues: PersistableBundle,
    ): Boolean {
        val current = try {
            cm.getConfigForSubId(subId)
        } catch (_: Exception) {
            Log.w(TAG, "failed to read current config for subId $subId, will proceed with override")
            return false
        } ?: return false

        for (key in newValues.keySet()) {
            if (key == CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING) continue
            val newVal = newValues.get(key)
            val curVal = current.get(key)
            if (!valuesEqual(newVal, curVal)) {
                Log.i(TAG, "config diff: key=$key current=$curVal new=$newVal")
                return false
            }
        }
        return true
    }

    private fun valuesEqual(a: Any?, b: Any?): Boolean {
        if (a === b) return true
        if (a == null || b == null) return false
        if (a is IntArray && b is IntArray) return a.contentEquals(b)
        if (a is LongArray && b is LongArray) return a.contentEquals(b)
        if (a is BooleanArray && b is BooleanArray) return a.contentEquals(b)
        if (a is DoubleArray && b is DoubleArray) return a.contentEquals(b)
        if (a is Array<*> && b is Array<*>) return a.contentEquals(b)
        return a == b
    }

    @Throws(Exception::class)
    private fun applyCarrierTestMccOverride(
        subId: Int,
        mccOverrideRaw: String,
        mncHintRaw: String?,
    ) {
        val normalizedMcc = mccOverrideRaw.filter { it.isDigit() }.take(3)
        if (normalizedMcc.length != 3) {
            Log.w(TAG, "skip carrier test override: invalid MCC=$mccOverrideRaw")
            return
        }
        val normalizedMnc = mncHintRaw
            ?.filter { it.isDigit() }
            ?.take(3)
            ?.takeIf { it.length in 2..3 }
            ?: resolveCurrentMnc(subId)
            ?: throw IllegalStateException("unable to resolve MNC for subId=$subId")
        val mccmnc = normalizedMcc + normalizedMnc
        val binder = ServiceManager.getService(Context.TELEPHONY_SERVICE)
            ?: throw IllegalStateException("phone service unavailable")
        val telephony = ITelephony.Stub.asInterface(ShizukuBinderWrapper(binder))
            ?: throw IllegalStateException("ITelephony unavailable")
        Log.i(TAG, "setCarrierTestOverride for subId=$subId mccmnc=$mccmnc")
        telephony.setCarrierTestOverride(
            subId,
            mccmnc,
            "",
            "",
            "",
            "",
            "",
            "",
            null,
            null
        )
    }

    @Throws(Exception::class)
    private fun clearCarrierTestOverride(subId: Int) {
        val binder = ServiceManager.getService(Context.TELEPHONY_SERVICE)
            ?: throw IllegalStateException("phone service unavailable")
        val telephony = ITelephony.Stub.asInterface(ShizukuBinderWrapper(binder))
            ?: throw IllegalStateException("ITelephony unavailable")

        val clearMethod = runCatching {
            telephony.javaClass.getMethod("clearCarrierTestOverride", Int::class.javaPrimitiveType)
        }.getOrNull()

        if (clearMethod != null) {
            Log.i(TAG, "clearCarrierTestOverride for subId=$subId")
            clearMethod.invoke(telephony, subId)
            return
        }

        val currentMccMnc = resolveActiveSubscriptionMccMnc(subId)
        if (currentMccMnc.isNullOrBlank()) {
            Log.w(
                TAG,
                "clearCarrierTestOverride unavailable and unable to resolve MCCMNC for subId=$subId; skip fallback to avoid empty operator override"
            )
            return
        }

        Log.i(
            TAG,
            "clearCarrierTestOverride unavailable, fallback setCarrierTestOverride(current=$currentMccMnc) for subId=$subId"
        )
        telephony.setCarrierTestOverride(
            subId,
            currentMccMnc,
            "",
            "",
            "",
            "",
            "",
            "",
            null,
            null
        )
    }

    private fun resolveActiveSubscriptionMccMnc(subId: Int): String? {
        val sm = context.getSystemService(SubscriptionManager::class.java) ?: return null
        val info = sm.activeSubscriptionInfoList?.firstOrNull { it.subscriptionId == subId } ?: return null

        val mcc = info.mccString
            ?.filter { it.isDigit() }
            ?.takeIf { it.length == 3 }
            ?: info.mcc.takeIf { it in 0..999 }?.toString()?.padStart(3, '0')

        val mnc = info.mncString
            ?.filter { it.isDigit() }
            ?.takeIf { it.length in 2..3 }
            ?: info.mnc.takeIf { it in 0..999 }?.toString()?.padStart(2, '0')

        return if (mcc != null && mnc != null) mcc + mnc else null
    }

    private fun resolveCurrentMnc(subId: Int): String? {
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return null
        val bySub = telephony.createForSubscriptionId(subId).simOperator.orEmpty()
        val operator = bySub.filter { it.isDigit() }
        if (operator.length >= 5) {
            return operator.substring(3)
        }
        val fallback = telephony.simOperator.orEmpty().filter { it.isDigit() }
        return if (fallback.length >= 5) fallback.substring(3) else null
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    fun Bundle.toPersistableBundle(): PersistableBundle {
        val pb = PersistableBundle()
        for (key in this.keySet()) {
            val value = this.get(key)
            when (value) {
                is Int -> pb.putInt(key, value)
                is Long -> pb.putLong(key, value)
                is Double -> pb.putDouble(key, value)
                is String -> pb.putString(key, value)
                is Boolean -> pb.putBoolean(key, value)
                is IntArray -> pb.putIntArray(key, value)
                is LongArray -> pb.putLongArray(key, value)
                is DoubleArray -> pb.putDoubleArray(key, value)
                is BooleanArray -> pb.putBooleanArray(key, value)
                else -> {
                    if (value is Array<*> && value.isArrayOf<String>()) {
                        pb.putStringArray(key, value as Array<String>)
                    } else {
                        Log.i(TAG, "toPersistableBundle: unsupported type for key $key")
                    }
                }
            }
        }
        return pb
    }
}
