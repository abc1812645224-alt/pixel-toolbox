package com.example.pixeltoolbox.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object LogCollector {
    private const val MAX_ENTRIES = 500
    private const val LOG_FILE_NAME = "pixel_toolbox_clone.log"
    private const val MAX_FILE_SIZE = 512 * 1024L // 512KB

    data class LogEntry(
        val timestamp: Long,
        val status: String,
        val description: String,
        val errorDetail: String? = null
    )

    private val entries = mutableListOf<LogEntry>()
    @Volatile private var logFile: File? = null
    private var fileWriter: FileWriter? = null

    fun init(context: Context) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        logFile = File(dir, LOG_FILE_NAME)
        // Truncate if over max size
        if (logFile!!.length() > MAX_FILE_SIZE) {
            logFile!!.writeText("")
        }
    }

    @Synchronized
    fun log(status: String, description: String, errorDetail: String? = null) {
        if (entries.size >= MAX_ENTRIES) {
            entries.removeAt(0)
        }
        entries.add(LogEntry(System.currentTimeMillis(), status, description, errorDetail))

        // Persist to file
        val f = logFile ?: return
        try {
            if (f.length() > MAX_FILE_SIZE) {
                f.writeText("")
            }
            val line = buildString {
                append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date()))
                append(" [${status}] ${description}")
                if (errorDetail != null) append(": ${errorDetail}")
                append("\n")
            }
            f.appendText(line)
        } catch (e: Exception) {
            Log.w("LogCollector", "Failed to write log: ${e.message}")
        }
    }

    @Synchronized
    fun getEntries(): List<LogEntry> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
    }

    fun getLogFilePath(): String? = logFile?.absolutePath

    fun exportToString(deviceInfo: String, appVersion: String): String {
        val sb = StringBuilder()
        sb.appendLine("========================================")
        sb.appendLine("Pixel Toolbox 诊断日志")
        sb.appendLine("========================================")
        sb.appendLine("导出时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}")
        sb.appendLine(deviceInfo)
        sb.appendLine("应用版本: $appVersion")
        sb.appendLine("日志文件: ${logFile?.absolutePath ?: "未初始化"}")
        sb.appendLine("========================================")
        sb.appendLine()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        for (entry in entries) {
            val time = sdf.format(Date(entry.timestamp))
            if (entry.errorDetail != null) {
                sb.appendLine("[$time] [${entry.status}] ${entry.description}: ${entry.errorDetail}")
            } else {
                sb.appendLine("[$time] [${entry.status}] ${entry.description}")
            }
        }

        return sb.toString()
    }
}
