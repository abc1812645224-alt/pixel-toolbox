package com.example.pixeltoolbox.widget

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import kotlin.math.roundToInt

data class SystemMetrics(
    val cpuPercent: Int = 0,
    val ramPercent: Int = 0,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val batteryPercent: Int = 0,
    val batteryTemp: Float = 0f,
    val batteryCharging: Boolean = false,
    val networkDlBps: Long = 0,
    val networkUlBps: Long = 0,
    val uptimeText: String = "--",
    val updateTimestamp: Long = System.currentTimeMillis()
)

class MetricsCollector(private val context: Context) {

    private var prevRxBytes: Long = -1
    private var prevTxBytes: Long = -1
    private var prevSampleTime: Long = 0L
    private var prevIdle: Long = 0
    private var prevTotal: Long = 0

    suspend fun collect(): SystemMetrics = withContext(Dispatchers.IO) {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val battPercent = if (scale > 0) (level * 100f / scale).roundToInt() else 0
        val temp = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val cpu = readCpuUsage()
        val ram = readRamUsage()

        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()

        var dlBps = 0L
        var ulBps = 0L
        if (prevRxBytes >= 0 && prevSampleTime > 0) {
            val elapsedSec = (now - prevSampleTime) / 1000f
            if (elapsedSec > 0) {
                dlBps = ((rxBytes - prevRxBytes) / elapsedSec).toLong().coerceAtLeast(0)
                ulBps = ((txBytes - prevTxBytes) / elapsedSec).toLong().coerceAtLeast(0)
            }
        }
        prevRxBytes = rxBytes
        prevTxBytes = txBytes
        prevSampleTime = now

        val seconds = SystemClock.elapsedRealtime() / 1000
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val mins = (seconds % 3600) / 60
        val uptime = if (days > 0) "${days}d${hours}h" else "${hours}h${mins}m"

        SystemMetrics(
            cpuPercent = cpu,
            ramPercent = ram.first,
            ramUsedGb = ram.second,
            ramTotalGb = ram.third,
            batteryPercent = battPercent,
            batteryTemp = temp,
            batteryCharging = charging,
            networkDlBps = dlBps,
            networkUlBps = ulBps,
            uptimeText = uptime
        )
    }

    private fun readCpuUsage(): Int {
        return try {
            val raf = RandomAccessFile("/proc/stat", "r")
            val line = raf.readLine()
            raf.close()

            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 8) return 0

            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val iowait = parts[5].toLong()
            val irq = parts[6].toLong()
            val softirq = parts[7].toLong()

            val total = user + nice + system + idle + iowait + irq + softirq
            val idleTotal = idle + iowait

            if (prevTotal > 0) {
                val totalDiff = total - prevTotal
                val idleDiff = idleTotal - prevIdle
                if (totalDiff > 0) {
                    prevIdle = idleTotal
                    prevTotal = total
                    return ((totalDiff - idleDiff).toFloat() / totalDiff * 100).roundToInt().coerceIn(0, 100)
                }
            }
            prevIdle = idleTotal
            prevTotal = total
            0
        } catch (_: Exception) {
            0
        }
    }

    private fun readRamUsage(): Triple<Int, Float, Float> {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)

            if (memInfo.totalMem <= 0) Triple(0, 0f, 0f)

            val totalGb = memInfo.totalMem / 1024f / 1024f / 1024f
            val usedGb = (memInfo.totalMem - memInfo.availMem) / 1024f / 1024f / 1024f
            val percent = ((memInfo.totalMem - memInfo.availMem).toFloat() / memInfo.totalMem * 100).roundToInt()
            Triple(percent, usedGb, totalGb)
        } catch (_: Exception) {
            Triple(0, 0f, 0f)
        }
    }

    /** Non-suspend collect for use in AppWidgetProvider (non-coroutine context) */
    fun runBlockingCollect(): SystemMetrics = collectBlocking()

    private fun collectBlocking(): SystemMetrics {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val battPercent = if (scale > 0) (level * 100f / scale).roundToInt() else 0
        val temp = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val cpu = readCpuUsage()
        val ram = readRamUsage()

        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()

        var dlBps = 0L
        var ulBps = 0L
        if (prevRxBytes >= 0 && prevSampleTime > 0) {
            val elapsedSec = (now - prevSampleTime) / 1000f
            if (elapsedSec > 0) {
                dlBps = ((rxBytes - prevRxBytes) / elapsedSec).toLong().coerceAtLeast(0)
                ulBps = ((txBytes - prevTxBytes) / elapsedSec).toLong().coerceAtLeast(0)
            }
        }
        prevRxBytes = rxBytes
        prevTxBytes = txBytes
        prevSampleTime = now

        val seconds = SystemClock.elapsedRealtime() / 1000
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val mins = (seconds % 3600) / 60
        val uptime = if (days > 0) "${days}d${hours}h" else "${hours}h${mins}m"

        return SystemMetrics(
            cpuPercent = cpu,
            ramPercent = ram.first,
            ramUsedGb = ram.second,
            ramTotalGb = ram.third,
            batteryPercent = battPercent,
            batteryTemp = temp,
            batteryCharging = charging,
            networkDlBps = dlBps,
            networkUlBps = ulBps,
            uptimeText = uptime
        )
    }

    companion object {
        fun formatSpeed(bps: Long): String {
            return when {
                bps >= 1_000_000 -> String.format("%.1fM", bps / 1_000_000f)
                bps >= 1_000 -> String.format("%.0fK", bps / 1_000f)
                bps > 0 -> "${bps}B"
                else -> "0"
            }
        }

        fun formatRam(gb: Float): String {
            return String.format("%.1fG", gb)
        }
    }
}
