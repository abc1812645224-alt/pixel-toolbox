/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.example.pixeltoolbox.services.recording

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.documentfile.provider.DocumentFile
import com.example.pixeltoolbox.R
import com.example.pixeltoolbox.utils.AppLogger

/**
 * This is a simple activity that shows a confirmation dialog to the user before deleting a recording file.
 */
class DeleteDialogConfirmationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // We do NOT call setContent() here, and we define a transparent theme (dialog) in the manifest for this activity.

        val fileUri: Uri? = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (fileUri == null) {
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_recording_confirmation_title))
            .setMessage(getString(R.string.delete_recording_confirmation_message))
            .setPositiveButton(getString(R.string.general_delete)) { dialog, _ ->
                try {
                    val deleted = DocumentFile.fromSingleUri(this, fileUri)?.delete() == true
                    if (deleted) {
                        Toast.makeText(this, getString(R.string.general_deleted), Toast.LENGTH_SHORT).show()
                        // Remove the notifications from the notification tray since file no longer exists
                        val manager = getSystemService(android.app.NotificationManager::class.java)
                        manager.cancel(RecordingNotificationHelper.POST_RECORDING_FILE_ACTIONS_NOTIFICATION_ID)
                    }
                } catch (e: Exception) {
                    AppLogger.e( "Failed to delete file: $fileUri", e)
                    Toast.makeText(this, getString(R.string.delete_recording_confirmation_failed), Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.general_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                finish()
            }.show()
    }
}