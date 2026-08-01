package com.example.pixeltoolbox.util

import java.text.SimpleDateFormat
import java.util.*

object LogCollector {
    private const val MAX_ENTRIES = 500

    data class LogEntry(
        val timestamp: Long,
        val status: String,
        val description: String,
        val errorDetail: String? = null
    )

    private val entries = mutableListOf<LogEntry>()

    @Synchronized
    fun log(status: String, description: String, errorDetail: String? = null) {
        if (entries.size >= MAX_ENTRIES) {
            entries.removeAt(0)
        }
        entries.add(LogEntry(System.currentTimeMillis(), status, description, errorDetail))
    }

    @Synchronized
    fun getEntries(): List<LogEntry> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
    }

    fun exportToString(deviceInfo: String, appVersion: String): String {
        val sb = StringBuilder()
        sb.appendLine("========================================")
        sb.appendLine("Pixel Toolbox 诊断日志")
        sb.appendLine("========================================")
        sb.appendLine("导出时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}")
        sb.appendLine(deviceInfo)
        sb.appendLine("应用版本: $appVersion")
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
