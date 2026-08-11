package com.example.pixeltoolbox.signal

data class SignalInfo(
    val type: CellType,
    val isRegistered: Boolean, // true for Serving, false for Neighbor
    val pci: Int,
    val earfcn: Int,
    val band: String,
    val bandwidth: String = "",
    val rsrp: Int,
    val sinr: Int,
    val rsrq: Int,
    val rssi: Int
)

enum class CellType(val displayName: String) {
    NR("5G NR"),
    LTE("4G LTE"),
    WCDMA("3G WCDMA"),
    GSM("2G GSM"),
    UNKNOWN("未知")
}

data class SignalDashboardState(
    val servingCells: List<SignalInfo> = emptyList(),
    val neighborCells: List<SignalInfo> = emptyList(),
    val networkMode: String = "未知",
    val dataState: String = "未知",
    val carrierName: String = "未知",
    val subscriptionDownlink: String = "未知",
    val subscriptionUplink: String = "未知",
    val qci: String = "未知",
    val serviceState: String = "未知",
    val hasPermission: Boolean = false,
    val permissionHint: String = "",
    val deviceModel: String = "Google Pixel",
    val firmwareVersion: String = "Android 14",
    val cpuUsage: Float = 0f,
    val ramUsage: Float = 0f,
    val todayTraffic: String = "--",
    val todayDlTraffic: String = "--",
    val todayUlTraffic: String = "--",
    val monthTotalTraffic: String = "--",
    val monthDlTraffic: String = "--",
    val monthUlTraffic: String = "--",
    val monthDlPercent: Float = 0f,
    val wifiTodayTraffic: String = "--",
    val wifiMonthTotalTraffic: String = "--",
    val wifiMonthDlTraffic: String = "--",
    val wifiMonthUlTraffic: String = "--",
    val uptimeText: String = "--",
    val aggregatedBands: String = "--",
    val caStateText: String = "--",
    val lastUpdateTime: String = "--"
)

/**
 * 按域拆分状态，避免某一域（如 lastUpdateTime 秒级变化）触发全页 6 张卡片全量重组。
 * 各域为 data class，Compose 通过 equals 判断参数未变化时自动跳过对应卡片重组。
 */
data class SignalMetrics(
    val servingCells: List<SignalInfo> = emptyList(),
    val neighborCells: List<SignalInfo> = emptyList(),
    val networkMode: String = "未知",
    val dataState: String = "未知",
    val carrierName: String = "未知",
    val serviceState: String = "未知",
    val aggregatedBands: String = "--",
    val caStateText: String = "--"
)

data class NetworkMetrics(
    val subscriptionDownlink: String = "未知",
    val subscriptionUplink: String = "未知",
    val qci: String = "未知"
)

data class DeviceMetrics(
    val deviceModel: String = "Google Pixel",
    val firmwareVersion: String = "Android 14",
    val cpuUsage: Float = 0f,
    val ramUsage: Float = 0f
)

data class TrafficMetrics(
    val todayTraffic: String = "--",
    val todayDlTraffic: String = "--",
    val todayUlTraffic: String = "--",
    val monthTotalTraffic: String = "--",
    val monthDlTraffic: String = "--",
    val monthUlTraffic: String = "--",
    val monthDlPercent: Float = 0f,
    val wifiTodayTraffic: String = "--",
    val wifiMonthTotalTraffic: String = "--",
    val wifiMonthDlTraffic: String = "--",
    val wifiMonthUlTraffic: String = "--"
)

data class SystemMetrics(
    val uptimeText: String = "--",
    val lastUpdateTime: String = "--"
)

