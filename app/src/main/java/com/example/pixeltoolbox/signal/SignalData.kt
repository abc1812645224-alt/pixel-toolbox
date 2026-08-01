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
    val monthTotalTraffic: String = "--",
    val monthDlTraffic: String = "--",
    val monthUlTraffic: String = "--",
    val monthDlPercent: Float = 0f,
    val uptimeText: String = "--",
    val aggregatedBands: String = "--",
    val caStateText: String = "--",
    val lastUpdateTime: String = "--"
)

