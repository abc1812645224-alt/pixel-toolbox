/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.example.pixeltoolbox.system.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.pixeltoolbox.system.storage.SafHelper.createAudioFile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import java.io.File

/**
 * SafHelper provides utility functions for working with the Android Storage Access Framework (SAF).
 *
 * Users explicitly grant access to a folder via the system document-tree picker.
 */
object SafHelper {

    /**
     * Holds the result of a successful [createAudioFile] call.
     *
     * @param uri         The content URI of the newly created file (e.g. content://…).
     * @param descriptor  An open [ParcelFileDescriptor] in read-write mode.
     *                    Must be closed after use (after [ScrcpyAudioMuxer] finalises the container).
     * @param displayName A human-readable path for logging (e.g. "Recordings/call_incoming_….webm").
     */
    data class SafResult(
        val uri: Uri,
        val descriptor: ParcelFileDescriptor,
        val displayName: String
    )

    /**
     * Directory name used for the Downloads fallback (toolbox name + call-recordings name).
     * Recordings saved without a custom SAF folder land in Download/PixelToolboxCallRecordings/.
     */
    private const val RECORDING_DIR_NAME = "PixelToolboxCallRecordings"

    /**
     * Creates a new audio file inside the user-chosen SAF folder.
     *
     * @param context    App context used to resolve the [DocumentFile] and open the FD.
     * @param folderUri  The tree URI of the destination folder (from the document-tree picker).
     * @param fileName   The desired file name including extension (e.g. "call_incoming_….webm").
     * @param mimeType   The MIME type of the file (e.g. "audio/webm" for Opus, "audio/mp4" for AAC).
     * @return A [SafResult] with the URI, open FD, and display name; or null on failure.
     */
    fun createAudioFile(context: Context, folderUri: Uri, fileName: String, mimeType: String): SafResult? {
        val directory = DocumentFile.fromTreeUri(context, folderUri) ?: return null
        if (!directory.canWrite()) return null

        val newFile = directory.createFile(mimeType, fileName) ?: return null
        // Open the file in read-write mode so MediaMuxer can seek back to write headers.
        val fileDescriptor = context.contentResolver.openFileDescriptor(newFile.uri, "rw") ?: return null
        val displayName = "${directory.name}/$fileName"
        return SafResult(newFile.uri, fileDescriptor, displayName)
    }

    /**
     * Creates a new audio file in the system default Downloads directory (public Download folder).
     * Used as a fallback when the user has not configured a custom SAF folder.
     *
     * On Android 10+ (API 29+) writes go through [MediaStore.Downloads] so no storage permission
     * is required. On older devices it falls back to direct file access in
     * [Environment.getExternalStoragePublicDirectory].
     *
     * @param context  App context used to resolve the content resolver.
     * @param fileName The desired file name including extension (e.g. "20260806_185234_来电_138…_001.ogg").
     * @param mimeType The MIME type of the file (e.g. "audio/ogg" for Opus, "audio/mp4" for AAC).
     * @return A [SafResult] with the URI, open FD, and display name; or null on failure.
     */
    fun createAudioFileInDownloads(context: Context, fileName: String, mimeType: String): SafResult? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createAudioFileInDownloadsQ(context, fileName, mimeType)
        } else {
            createAudioFileInDownloadsLegacy(context, fileName, mimeType)
        }
    }

    private fun createAudioFileInDownloadsQ(context: Context, fileName: String, mimeType: String): SafResult? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/" + RECORDING_DIR_NAME
            )
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "rw") ?: return null
        return SafResult(uri, fileDescriptor, "Download/$RECORDING_DIR_NAME/$fileName")
    }

    @Suppress("DEPRECATION")
    private fun createAudioFileInDownloadsLegacy(context: Context, fileName: String, mimeType: String): SafResult? {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val recordDir = File(downloadDir, RECORDING_DIR_NAME)
        if (!recordDir.exists() && !recordDir.mkdirs()) return null

        val targetFile = File(recordDir, fileName)
        val fileDescriptor = ParcelFileDescriptor.open(
            targetFile,
            ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE
        )
        return SafResult(Uri.fromFile(targetFile), fileDescriptor, "Download/$RECORDING_DIR_NAME/$fileName")
    }

    /**
     * Counts existing recording files inside the target directory (either a custom SAF folder
     * or the system Downloads/PixelToolboxCallRecordings fallback directory).
     *
     * Used to generate the auto-incremented {sequence} placeholder in the file name template.
     *
     * @param context   App context used to resolve the directory.
     * @param folderUri The user-chosen SAF folder URI, or null when using the Downloads fallback.
     * @param prefix    File name prefix (without extension) to match, e.g. "20260806_185234_来电_138…".
     * @return The number of matching files already present in the directory.
     */
    fun countExistingRecordingFiles(context: Context, folderUri: Uri?, prefix: String): Int {
        return if (folderUri != null && isFolderValid(context, folderUri)) {
            val directory = DocumentFile.fromTreeUri(context, folderUri) ?: return 0
            directory.listFiles().count { file ->
                file.name?.startsWith(prefix) == true
            }
        } else {
            countExistingRecordingFilesInDownloads(context, prefix)
        }
    }

    private fun countExistingRecordingFilesInDownloads(context: Context, prefix: String): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("$prefix%")
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor -> return cursor.count } ?: 0
        } else {
            @Suppress("DEPRECATION")
            val recordDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                RECORDING_DIR_NAME
            )
            recordDir.listFiles { file -> file.name.startsWith(prefix) }?.size ?: 0
        }
    }

    /**
     * Returns true if [folderUri] points to an existing, writable SAF folder.
     * Used to validate the user's chosen recording folder before starting a session.
     *
     * @param context   App context used to resolve the [DocumentFile].
     * @param folderUri The tree URI to validate, or null.
     * @return true if the folder exists and is writable; false if null or inaccessible.
     */
    @OptIn(ExperimentalContracts::class)
    fun isFolderValid(context: Context, folderUri: Uri?): Boolean {
        // Tells the compiler: if we returns true, folderUri is not null. Prevent false compiler error and warnings.
        contract {
            returns(true) implies (folderUri != null)
        }
        if (folderUri == null) return false
        val directory = DocumentFile.fromTreeUri(context, folderUri)
        return directory != null && directory.exists() && directory.canWrite()
    }

    /**
     * Returns a human-readable display name for a SAF folder URI.
     * Used in the Settings screen to show which folder recordings are saved to.
     *
     * @param context   App context used to resolve the [DocumentFile].
     * @param folderUri The tree URI, or null.
     * @return The folder name (e.g. "Recordings"), or null.
     */
    fun getFolderDisplayNameOrNull(context: Context, folderUri: Uri?): String? {
        if (folderUri == null) return null
        val directory = DocumentFile.fromTreeUri(context, folderUri)
        return directory?.name
    }
}
