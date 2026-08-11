/*
 * Ported from ShizuCallRecorder (GPL-3.0) - minimal manage_ongoing_calls AppOp check/grant.
 * The InCallService detection path (Android 12+) requires the MANAGE_ONGOING_CALLS AppOps
 * to be granted to the app, otherwise Telecom will not bind our non-UI InCallService.
 */
package com.example.pixeltoolbox.services.recording

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.example.pixeltoolbox.integrations.shizuku.ShizukuConnectionManager
import com.example.pixeltoolbox.utils.AppLogger

/**
 * Helper for checking / granting the `android:manage_ongoing_calls` AppOp.
 *
 * Android 12+ (API 31+) uses this AppOp when InCallController decides whether to bind
 * our InCallService (IN_CALL_SERVICE_TYPE_NON_UI). Without it, no call events are received.
 * Granting requires elevated (Shizuku) privileges: we first try the package-level op,
 * then the UID-level op, verifying after each step.
 */
object ManageOngoingCalls {

    private const val OP_MANAGE_ONGOING_CALLS = "android:manage_ongoing_calls"

    /** True when no AppOp is needed (API < 31) or the op is already allowed. */
    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return runCatching {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.checkOpNoThrow(OP_MANAGE_ONGOING_CALLS, Process.myUid(), context.packageName) ==
                    AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    /**
     * Attempts to grant the op through Shizuku (package-level first, then UID-level).
     * @return true if the op is granted after the chain, false otherwise.
     */
    suspend fun grant(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        if (isGranted(context)) return true
        if (!ShizukuConnectionManager.isAvailable() || !ShizukuConnectionManager.hasPermission(context)) {
            AppLogger.e("ManageOngoingCalls: Shizuku not available/permission not granted")
            return false
        }

        val manager = ShizukuConnectionManager(context)
        return try {
            val shellService = manager.getShellService()
            val uid = Process.myUid()
            val userId = Process.myUserHandle().hashCode()

            shellService.grantAppOpByPackage(context.packageName, OP_MANAGE_ONGOING_CALLS, userId)
            if (isGranted(context)) {
                AppLogger.i("ManageOngoingCalls: granted via package AppOp")
                return true
            }

            shellService.grantAppOpByUid(uid, OP_MANAGE_ONGOING_CALLS, userId)
            val granted = isGranted(context)
            if (granted) AppLogger.i("ManageOngoingCalls: granted via UID AppOp")
            else AppLogger.e("ManageOngoingCalls: all escalation steps exhausted")
            granted
        } catch (e: Exception) {
            AppLogger.e("ManageOngoingCalls: exception during grant", e)
            false
        } finally {
            manager.unbind()
        }
    }
}
