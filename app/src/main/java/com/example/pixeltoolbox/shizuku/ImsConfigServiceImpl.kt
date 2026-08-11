package com.example.pixeltoolbox.shizuku

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class ImsConfigServiceImpl : IImsConfigService.Stub() {

    companion object {
        private const val TAG = "ImsConfigService"
    }

    override fun destroy() {
        Log.i(TAG, "Service destroyed")
        exitProcess(0)
    }

    override fun applyFullImsConfig(subId: Int): Boolean {
        Log.w(TAG, "applyFullImsConfig is deprecated, use applyCarrierConfig instead")
        return false
    }

    /**
     * Shizuku UserService 进程内通过 shell 命令注入配置。
     * Shizuku 进程拥有 shell 级权限，shell 命令可直接操作 system_server。
     */
    override fun applyCarrierConfig(subId: Int, toggleJson: String): Boolean {
        return try {
            applyCarrierConfigInternal(subId, toggleJson)
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error applying carrier config", e)
            false
        }
    }

    private fun exec(cmd: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val exitCode = process.waitFor()
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText().trim()
            val ok = exitCode == 0
            Log.i(TAG, (if (ok) "OK" else "FAIL($exitCode)") + ": $cmd")
            if (stdout.isNotEmpty()) Log.i(TAG, "  stdout: $stdout")
            if (stderr.isNotEmpty()) Log.w(TAG, "  stderr: $stderr")
            ok
        } catch (e: Exception) {
            Log.e(TAG, "exec failed: $cmd | ${e.message}", e)
            false
        }
    }

    private fun applyCarrierConfigInternal(subId: Int, toggleJson: String): Boolean {
        Log.i(TAG, "=== Shell-based CarrierConfig override for subId=$subId ===")
        Log.i(TAG, "toggleJson=$toggleJson")

        val json = JSONObject(toggleJson)
        val subIdArg = if (subId > 0) subId.toString() else ""

        var ok = true

        // ─── 5G NR core ───
        if (json.optBoolean("nr_5g", false)) {
            ok = exec("cmd phone set-preferred-network-type 20") && ok
        }

        // ─── VoNR ───
        if (json.optBoolean("vonr", false)) {
            ok = exec("settings put global vonr_enabled 1") && ok
        }

        // ─── VoLTE ───
        if (json.optBoolean("volte", false)) {
            ok = exec("settings put global volte_vt_enabled 1") && ok
        }

        // ─── VoWiFi ───
        if (json.optBoolean("vowifi", false)) {
            ok = exec("settings put global wfc_ims_enabled 1") && ok
            exec("settings put global wfc_ims_mode 2")
        }

        // ─── ViLTE ───
        if (json.optBoolean("vilte", false)) {
            ok = exec("settings put global vt_ims_enabled 1") && ok
        }

        // ─── LTE → 4G icon ───
        if (json.optBoolean("lte_4g", false)) {
            ok = exec("settings put global show_4g_for_lte_data_icon 1") && ok
        }

        // ─── 5G signal thresholds ───
        if (json.optBoolean("5g_signal", false)) {
            exec("settings put global nr_ssrsrp_thresholds -128,-118,-108,-98")
        }

        // ─── 5GA icon ───
        if (json.optBoolean("5ga_icon", false)) {
            exec("settings put global nr_advanced_threshold_bandwidth_khz 100000")
        }

        // ─── Cross SIM ───
        if (json.optBoolean("cross_sim", false)) {
            exec("settings put global cross_sim_ims_available 1")
        }

        // ─── UT (Supplementary Services) ───
        if (json.optBoolean("ut", false)) {
            exec("settings put global ss_over_ut_enabled 1")
        }

        Log.i(TAG, "=== Shell-based override done: $ok ===")
        return ok
    }
}
