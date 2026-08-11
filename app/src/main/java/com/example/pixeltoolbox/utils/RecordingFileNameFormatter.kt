/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.example.pixeltoolbox.utils

import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import androidx.annotation.StringRes
import com.example.pixeltoolbox.R
import com.example.pixeltoolbox.data.AppPreferences
import com.example.pixeltoolbox.data.call.CallDirection
import com.example.pixeltoolbox.data.call.EnrichedCallData
import com.example.pixeltoolbox.integrations.scrcpy.ScrcpyAudioCodec
import java.util.Date
import java.util.Locale

object RecordingFileNameFormatter {
    /**
     * Represents the supported placeholders that can be used in the file name template.
     * Binds the literal tag used in formatting to a localized description for the UI.
     * @param tag The literal placeholder string that will be replaced in the template (e.g., "{date}").
     * @param descriptionResId The string resource ID for the description of this placeholder
     * @param supportedModes The set of CallDetectionModes in which this placeholder can be used/may be expected to work.
     */
    enum class FileNamePlaceholder(val tag: String, @param:StringRes val descriptionResId: Int) {
        DATE("{date}", R.string.placeholder_date_desc),
        DATE_YEAR("{date:year}", R.string.placeholder_date_year_desc),
        DATE_MONTH("{date:month}", R.string.placeholder_date_month_desc),
        DATE_DAY("{date:day}", R.string.placeholder_date_day_desc),
        DATE_HOURS("{date:hours}", R.string.placeholder_date_hours_desc),
        DATE_MINUTES("{date:minutes}", R.string.placeholder_date_minutes_desc),
        DATE_SECONDS("{date:seconds}", R.string.placeholder_date_seconds_desc),
        DIRECTION("{direction}", R.string.placeholder_direction_desc),
        PHONE_NUMBER("{phone_number}", R.string.placeholder_phone_number_desc),
        SEQUENCE("{sequence}", R.string.placeholder_sequence_desc),
        CALLER_NAME("{caller_name}", R.string.placeholder_caller_name_desc),
        CROSS_COUNTRY("{cross_country}", R.string.placeholder_cross_country_desc),
        PACKAGE_NAME("{package_name}", R.string.placeholder_package_name_desc)
    }

    /**
     * Formats a filename based on the user defined string template and the recording metadata and audio codec.
     * Supported placeholders: [FileNamePlaceholder]
     *
     * @param context The context needed to resolve contacts and read preferences.
     * @param metadata Defines the main properties (direction, phone number, cross country).
     * @param codec The selected ScrcpyAudioCodec used to determine the file extension.
     * @param customFormat An optional custom format string to use instead of the one from preferences. Useful for testing or one-off formatting without changing user settings.
     * @return A filesystem-safe filename string.
     */
    fun formatFileName(
        context: Context,
        metadata: EnrichedCallData,
        codec: ScrcpyAudioCodec,
        customFormat: String? = null,
        sequence: String = ""
    ): String {
        val template = customFormat ?: AppPreferences(context).getFileNameTemplate()

        // Capture a single instant so that {date} and the granular {date:...} sub-fields all describe the same moment.
        val now = Date()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CANADA).format(now)
        val dateYearStr = SimpleDateFormat("yyyy", Locale.CANADA).format(now)
        val dateMonthStr = SimpleDateFormat("MM", Locale.CANADA).format(now)
        val dateDayStr = SimpleDateFormat("dd", Locale.CANADA).format(now)
        val dateHoursStr = SimpleDateFormat("HH", Locale.CANADA).format(now)
        val dateMinutesStr = SimpleDateFormat("mm", Locale.CANADA).format(now)
        val dateSecondsStr = SimpleDateFormat("ss", Locale.CANADA).format(now)

        val directionStr = when (metadata.direction) {
            CallDirection.INCOMING -> "来电"
            CallDirection.OUTGOING -> "去电"
        }

        val phoneStr = metadata.getBestNumber()
        var callerNameStr = ""

        if (template.contains(FileNamePlaceholder.CALLER_NAME.tag) && phoneStr.isNotEmpty()) {
            callerNameStr = metadata.callerName ?: ""
        }

        val crossCountryStr = metadata.isCrossCountry.toString()

        val packageName = if (metadata.packageName.isNullOrBlank())
        {
            "" // If package name is not available, return empty string.
        } else
        {
            getAppName(context, metadata.packageName)
        }

        val baseName = template
            .replace(FileNamePlaceholder.DATE_YEAR.tag, dateYearStr)
            .replace(FileNamePlaceholder.DATE_MONTH.tag, dateMonthStr)
            .replace(FileNamePlaceholder.DATE_DAY.tag, dateDayStr)
            .replace(FileNamePlaceholder.DATE_HOURS.tag, dateHoursStr)
            .replace(FileNamePlaceholder.DATE_MINUTES.tag, dateMinutesStr)
            .replace(FileNamePlaceholder.DATE_SECONDS.tag, dateSecondsStr)
            .replace(FileNamePlaceholder.DATE.tag, dateStr)
            .replace(FileNamePlaceholder.DIRECTION.tag, directionStr)
            .replace(FileNamePlaceholder.PHONE_NUMBER.tag, phoneStr)
            .replace(FileNamePlaceholder.SEQUENCE.tag, sequence)
            .replace(FileNamePlaceholder.CALLER_NAME.tag, callerNameStr)
            .replace(FileNamePlaceholder.CROSS_COUNTRY.tag, crossCountryStr)
            .replace(FileNamePlaceholder.PACKAGE_NAME.tag, packageName)

        AppLogger.v( "Formatted base filename: '$baseName' with template '$template'")
        return "$baseName${codec.containerExtension}"
    }

    /**
     * Attempts to resolve the user-friendly app name from a package name. If resolution fails, it falls back to returning the package name itself.
     */
    private fun getAppName(context: Context, packageName: String): String {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            AppLogger.w( "Could not resolve app name for package '$packageName', got NameNotFoundException (privacy restriction?). Returning package name as fallback.")
            // Fallback: return the package name itself
            packageName
        }
    }
}