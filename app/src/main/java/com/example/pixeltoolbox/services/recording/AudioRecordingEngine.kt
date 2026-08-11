/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.example.pixeltoolbox.services.recording

import android.app.Service
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import com.example.pixeltoolbox.IShellService
import com.example.pixeltoolbox.R
import com.example.pixeltoolbox.data.AppPreferences
import com.example.pixeltoolbox.data.call.CallDirection
import com.example.pixeltoolbox.data.call.EnrichedCallData
import com.example.pixeltoolbox.integrations.scrcpy.ScrcpyAudioCodec
import com.example.pixeltoolbox.integrations.scrcpy.ScrcpyAudioMuxer
import com.example.pixeltoolbox.integrations.scrcpy.ScrcpyAudioSource
import com.example.pixeltoolbox.integrations.scrcpy.ScrcpyClient
import com.example.pixeltoolbox.integrations.scrcpy.ScrcpyConfig
import com.example.pixeltoolbox.integrations.scrcpy.ServerExtractor
import com.example.pixeltoolbox.system.storage.SafHelper
import com.example.pixeltoolbox.utils.AppLogger
import com.example.pixeltoolbox.utils.RecordingFileNameFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages the audio recording pipeline, including the connection to the shell service, reading from the audio pipe,
 * parsing scrcpy-server custom stream format, and writing to the output container via [ScrcpyAudioMuxer].
 *
 * Call [startPipeline] to initialize and start the recording, and [release] to clean up resources when done.
 */
class AudioRecordingEngine {

    /**
     * Parses the raw byte stream that arrives from the shell process pipe.
     *
     * Calls the attached callbacks with parsed audio packets and stream metadata.
     */
    var scrcpyClient: ScrcpyClient? = null

    /** Writes scrcpy decoded audio packets into the output container (OPUS/AAC). */
    var scrcpyAudioMuxer: ScrcpyAudioMuxer? = null

    /** Metadata captured during the [startPipeline] and locked. Used for checks in [release]. */
    var initializationMetadata: EnrichedCallData? = null
        set(value) {
            if (field == null) {
                field = value
            } else {
                AppLogger.w( "Attempt to overwrite recording session metadata ignored. THIS SHOULD NOT HAPPEN. Original: $field, New: $value")
            }
        }

    /**
     * Read end of the kernel pipe owned by the shell process.
     * The shell process writes scrcpy-server audio bytes into the write end; this service
     * reads from the read end. Android's [ParcelFileDescriptor] wraps a native file descriptor
     * so it can be transferred across processes via Binder.
     */
    var audioReadPipePfd: ParcelFileDescriptor? = null

    /**
     * Write-access file descriptor for the output file.
     * This is kept open for the duration of the recording so [ScrcpyAudioMuxer] can write to it,
     * and is closed in [release] after the muxer finalizes the container header.
     */
    var outputPfd: ParcelFileDescriptor? = null

    /**
     * URI of the current recording file.
     * Used to delete the file if recording fails to start mid-initialization.
     */
    var currentRecordingUri: Uri? = null

    /**
     * Active codec enum resolved from the user's preference and confirmed by the stream header.
     * Updated once [ScrcpyClient.AudioPacketListener.onMetadataReceived] fires.
     * Defaults to [ScrcpyAudioCodec.OPUS] as a safe initial value before the stream header is read.
     */
    var currentCodecEnum: ScrcpyAudioCodec = ScrcpyAudioCodec.OPUS

    /**
     * Coroutine scope for reading from the audio pipe data returned by the shell service.
     * Initialised in [startPipeline] and cancelled in [release].
     */
    var audioPipeReadScope: CoroutineScope? = null

    /**
     * The active pipe reading job.
     * We keep a reference so we can wait to finish reading any late bytes during [release].
     */
    var audioPipeReadJob: Job? = null

    /** Whether the recording is currently paused by the user. */
    @Volatile
    var isPaused: Boolean = false

    /**
     * Orchestrates the initialization and connection of the entire recording pipeline.
     * @throws PipelineInitializationException if any step of the initialization fails, with details for user-friendly and technical error reporting.
     */
    fun startPipeline(context: Service, service: IShellService, metadata: EnrichedCallData) {
        initializationMetadata = metadata
        val preferences = AppPreferences(context)
        val folderUri = preferences.getRecordingFolderUri()
        val folderValid = SafHelper.isFolderValid(context, folderUri)

        val codecEnum = ScrcpyAudioCodec.fromKey(preferences.getAudioCodec())
        val bitRate = preferences.getAudioBitRate().takeIf { it > 0 } ?: codecEnum.defaultBitRate
        val audioSourceEnum = ScrcpyAudioSource.fromKey(preferences.getAudioSource())

        AppLogger.i( "Starting recording pipeline: source=${audioSourceEnum.cliKey} codec=${codecEnum.cliKey} bitrate=$bitRate folderValid=$folderValid")

        // Compute the auto-incremented sequence number (3-digit, e.g. 001) based on files already
        // present in the target directory with the same date/direction/phone prefix.
        val datePrefix = SimpleDateFormat("yyyyMMdd", Locale.CANADA).format(Date())
        val directionStr = when (metadata.direction) {
            CallDirection.INCOMING -> "来电"
            CallDirection.OUTGOING -> "去电"
        }
        val phoneStr = metadata.getBestNumber()
        val sequencePrefix = "${datePrefix}_${directionStr}_${phoneStr}_"
        val existingCount = SafHelper.countExistingRecordingFiles(context, folderUri, sequencePrefix)
        val sequence = String.format(Locale.US, "%03d", existingCount + 1)

        val fileName = RecordingFileNameFormatter.formatFileName(context, metadata, codecEnum, sequence = sequence)

        val safResult = if (folderValid) {
            SafHelper.createAudioFile(context, folderUri!!, fileName, codecEnum.mimeType)
        } else {
            // Fall back to the system Downloads/PixelToolboxCallRecordings directory when no valid folder is configured.
            SafHelper.createAudioFileInDownloads(context, fileName, codecEnum.mimeType)
        } ?: throw PipelineInitializationException(
            userFriendlyMessage = context.getString(R.string.recording_error_file_creation),
            technicalLogMessage = "Failed to create audio file in storage (folderValid=$folderValid)"
        )

        AppLogger.d( "Created recording file: ${safResult.uri} (sequence=$sequence, folderValid=$folderValid)")

        currentRecordingUri = safResult.uri
        outputPfd = safResult.descriptor

        val serverPath = ScrcpyConfig.getServerPath(context)
        if (!ServerExtractor.ensureServerFile(context, serverPath)) {
            throw PipelineInitializationException(
                userFriendlyMessage = context.getString(R.string.recording_error_server_missing),
                technicalLogMessage = "scrcpy-server missing or SHA256 check was invalid at $serverPath"
            )
        }

        scrcpyAudioMuxer = ScrcpyAudioMuxer(outputPfd!!.fileDescriptor, safResult.displayName)

        try {
            audioReadPipePfd = service.startRecording(
                audioSourceEnum.cliKey,
                codecEnum.cliKey,
                bitRate,
                serverPath,
                preferences.isDebugEnabled()
            )
        } catch (e: Exception) {
            throw PipelineInitializationException(
                userFriendlyMessage = e.localizedMessage ?: context.getString(R.string.recording_error_start_failed),
                technicalLogMessage = "Remote exception calling startRecording",
                cause = e
            )
        }

        val inputPfd = audioReadPipePfd ?: throw PipelineInitializationException(
            userFriendlyMessage = context.getString(R.string.recording_error_start_failed),
            technicalLogMessage = "Shell service returned null pipe – cannot start recording"
        )

        currentCodecEnum = codecEnum
        scrcpyAudioMuxer?.initialize(currentCodecEnum)

        scrcpyClient = ScrcpyClient(
            inputPfd = inputPfd,
            expectedCodec = codecEnum,
            listener = object : ScrcpyClient.AudioPacketListener {
                /**
                 * Called once after the 4-byte codec FourCC is verified from the stream header.
                 * We re-initialise the muxer with the confirmed codec in case it differs from our initial assumption.
                 */
                override fun onMetadataReceived(codec: ScrcpyAudioCodec) {
                    AppLogger.d( "Stream metadata confirmed: codec=${codec.cliKey} fourCC=0x${codec.codecFourCC.toString(16)}")
                    currentCodecEnum = codec
                    scrcpyAudioMuxer?.initialize(codec)
                }

                /** Called for every audio frame received from the pipe. */
                override fun onAudioPacket(packet: ScrcpyClient.AudioPacket) {
                    if (isPaused) return // Drop packets while paused, do not write to muxer
                    scrcpyAudioMuxer?.writePacket(packet, currentCodecEnum)
                }

                /** Called when the stream ends normally (EOF) or with an error. */
                override fun onStreamEnd(error: String?) {
                    if (error != null) {
                        AppLogger.w( "Scrcpy-client reported stopping parsing due to an audio stream error: $error")
                    } else {
                        AppLogger.d( "Scrcpy-client reported our pipe read stream ended normally (EOF)")
                    }
                }
            }
        )

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        audioPipeReadScope = scope
        audioPipeReadJob = scope.launch(Dispatchers.IO) {
            try {
                scrcpyClient?.start()
            } catch (e: Exception) {
                AppLogger.w( "Audio reader ended: ${e.message}")
            }
        }
    }

    /**
     * Safely releases all held resources in the correct order.
     * Everything is wrapped in runCatching to ignore any exceptions and continue the cleanup.
     *
     * 1. Stops the remote shell service process natively, which gives scrcpy-server a grace period
     *    to write its final audio bytes before closing the pipe from the sender side.
     * 2. Waits for the local reading coroutine to reach EOF and finish parsing the late bytes.
     * 3. Cancels the active reading coroutine and scrcpy client as a fallback.
     * 4. Closes the inbound pipe.
     * 5. Closes the muxer and output file descriptor to finalize the container header.
     */
    fun release(shellService: IShellService?) {
        AppLogger.i( "Releasing session resources and recording pipeline...")
        runCatching { shellService?.stopRecording() }

        runCatching {
            runBlocking {
                withTimeoutOrNull(2000L) {
                    audioPipeReadJob?.join()
                }
            }
        }

        runCatching { scrcpyClient?.stop() }
        runCatching { audioPipeReadScope?.cancel() }
        runCatching { audioReadPipePfd?.close() }
        runCatching { scrcpyAudioMuxer?.close() }
        runCatching { outputPfd?.close() }
    }

    /**
     * Trigger the normal [release] flow, then followed by an attempt to delete the incomplete recording file if it was created
     * during the pipeline initialization.
     */
    fun cancel(context: Context, shellService: IShellService?) {
        release(shellService)
        try {
            currentRecordingUri?.let { uri ->
                DocumentFile.fromSingleUri(context, uri)?.delete()
            }
            AppLogger.d( "Cleaned up empty file after start failure")
        } catch (e: Exception) {
            AppLogger.w( "Failed to cleanup empty file", e)
        }
    }
}

/**
 * Custom exception to carry a user-friendly message for UI display
 * and a technical log message for debugging when the pipeline initialization fails.
 */
class PipelineInitializationException(
    val userFriendlyMessage: String,
    technicalLogMessage: String,
    cause: Throwable? = null
) : Exception(technicalLogMessage, cause)
