/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

/*
 * Ported from ShizuCallRecorder (GPL-3.0) - minimal InCallService call detection.
 * Listens for onCallAdded / STATE_ACTIVE to trigger automatic recording and onCallRemoved to stop.
 *
 * Note: tracks only one call at a time (same limitation as upstream). Requires the
 * MANAGE_ONGOING_CALLS AppOp (Android 12+) so that Telecom binds our non-UI InCallService.
 */
package com.example.pixeltoolbox.services.recording

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import com.example.pixeltoolbox.data.AppPreferences
import com.example.pixeltoolbox.data.call.CallDirection
import com.example.pixeltoolbox.data.call.EnrichedCallData
import com.example.pixeltoolbox.data.call.RawCallData
import com.example.pixeltoolbox.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * InCallService implementation responsible for detecting call state changes and relaying
 * them to [RecordingForegroundService] (start/stop intents).
 */
@RequiresApi(Build.VERSION_CODES.S)
class InCallRecordingService : InCallService() {

    private lateinit var appPreferences: AppPreferences
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var activeTrackedCall: Call? = null
    private var isPipelineExecuted = false

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            handleCallStateChanged(call, state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { serviceScope.cancel() }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        AppLogger.v("InCallRecordingService: onCallAdded ${call.details}")

        if (activeTrackedCall != null) {
            AppLogger.d("InCallRecordingService: parallel call detected, discarding (dual-call not supported)")
            return
        }

        activeTrackedCall = call
        call.registerCallback(callCallback)
        AppLogger.i("InCallRecordingService: tracking call, state=${callStateToString(call.details.state)} (${call.details.state})")

        if (call.details.state == Call.STATE_ACTIVE) {
            handleCallStateChanged(call, Call.STATE_ACTIVE)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        AppLogger.v("InCallRecordingService: onCallRemoved ${call.details}")

        if (call == activeTrackedCall) {
            AppLogger.i("InCallRecordingService: primary call removed, stopping recording")
            releasePrimaryTrackedCall()
        }
    }

    private fun handleCallStateChanged(call: Call, state: Int) {
        if (call != activeTrackedCall) return
        AppLogger.d("InCallRecordingService: primary call state=${callStateToString(state)} ($state)")

        // STATE_SELECT_PHONE_ACCOUNT may not include an account handle on dual-SIM OEM builds; ignore it.
        if (call.details.state == Call.STATE_SELECT_PHONE_ACCOUNT) {
            AppLogger.d("InCallRecordingService: STATE_SELECT_PHONE_ACCOUNT, ignoring")
            return
        }

        if (state == Call.STATE_ACTIVE) {
            if (isPipelineExecuted) return
            isPipelineExecuted = true

            val details = call.details
            val rawNumber = details.handle?.schemeSpecificPart ?: ""

            val direction = when (details.callDirection) {
                Call.Details.DIRECTION_INCOMING -> CallDirection.INCOMING
                Call.Details.DIRECTION_OUTGOING -> CallDirection.OUTGOING
                else -> CallDirection.OUTGOING
            }

            val rawCallData = RawCallData(
                rawPhoneNumber = rawNumber,
                direction = direction,
                osProvidedCallerName = details.contactDisplayName ?: details.callerDisplayName
            )

            serviceScope.launch {
                val metadata = EnrichedCallData.enrichMetadata(this@InCallRecordingService, rawCallData)
                val started = tryStartRecording(metadata)
                if (!started) {
                    AppLogger.e("InCallRecordingService: decision rejected or intent failed, resetting flag")
                    isPipelineExecuted = false
                }
            }
        }
    }

    /**
     * Applies the user's recording preferences and fires the START_RECORDING intent.
     * @return true if an intent was dispatched to the foreground service.
     */
    private suspend fun tryStartRecording(metadata: EnrichedCallData): Boolean {
        // Master switch
        if (!appPreferences.isCallRecorderEnabled()) {
            AppLogger.i("InCallRecordingService: master switch disabled, skip")
            return false
        }
        // Direction switches
        when (metadata.direction) {
            CallDirection.INCOMING -> {
                if (!appPreferences.isAutoRecordIncomingEnabled()) {
                    AppLogger.i("InCallRecordingService: incoming auto-record disabled, skip")
                    return false
                }
                // Ignore anonymous incoming calls when requested
                if (appPreferences.isIgnoreAnonymousIncomingEnabled() && metadata.normalisedPhoneNumber.isBlank()) {
                    AppLogger.i("InCallRecordingService: anonymous incoming call ignored by user setting")
                    return false
                }
            }
            CallDirection.OUTGOING -> {
                if (!appPreferences.isAutoRecordOutgoingEnabled()) {
                    AppLogger.i("InCallRecordingService: outgoing auto-record disabled, skip")
                    return false
                }
            }
        }
        // Shizuku + MANAGE_ONGOING_CALLS must be available; otherwise the pipeline cannot run.
        if (!ManageOngoingCalls.isGranted(this)) {
            AppLogger.e("InCallRecordingService: MANAGE_ONGOING_CALLS AppOp not granted, cannot record")
            return false
        }

        val intent = Intent(this, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_START_RECORDING
            putExtra(EnrichedCallData.EXTRA_METADATA, metadata)
        }
        return runCatching {
            startForegroundService(intent)
            true
        }.getOrElse {
            AppLogger.e("InCallRecordingService: failed to start foreground service", it)
            false
        }
    }

    private fun releasePrimaryTrackedCall() {
        val trackedCall = activeTrackedCall ?: return
        trackedCall.unregisterCallback(callCallback)

        if (isPipelineExecuted) {
            val intent = Intent(this, RecordingForegroundService::class.java).apply {
                action = RecordingForegroundService.ACTION_STOP_RECORDING
            }
            runCatching { startForegroundService(intent) }
                .onFailure { AppLogger.e("InCallRecordingService: failed to stop recording", it) }
            isPipelineExecuted = false
        }
        activeTrackedCall = null
    }

    private fun callStateToString(state: Int): String = when (state) {
        Call.STATE_NEW -> "NEW"
        Call.STATE_DIALING -> "DIALING"
        Call.STATE_RINGING -> "RINGING"
        Call.STATE_HOLDING -> "HOLDING"
        Call.STATE_ACTIVE -> "ACTIVE"
        Call.STATE_DISCONNECTED -> "DISCONNECTED"
        Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
        else -> "UNKNOWN_STATE($state)"
    }
}
