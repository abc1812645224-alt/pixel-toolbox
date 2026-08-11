/*
 * Pixel Toolbox (像素工具箱)
 * Copyright (C) 2026 Pixel Toolbox Project
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.example.pixeltoolbox.signal

import android.Manifest
import android.app.ActivityManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.SystemClock
import android.telephony.*
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import java.io.RandomAccessFile
import java.util.Calendar
import java.util.Locale

class SignalMonitor(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun startMonitoring(): Flow<SignalDashboardState> = flow {
        while (true) {
            val hasLocationPerm = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasPhonePerm = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

            if (!hasLocationPerm || !hasPhonePerm) {
                val missing = mutableListOf<String>()
                if (!hasLocationPerm) missing.add("定位")
                if (!hasPhonePerm) missing.add("电话")
                emit(SignalDashboardState(
                    carrierName = "需授权",
                    permissionHint = "部分权限未授权，信号数据可能不完整（缺少${missing.joinToString("、")}权限）",
                    hasPermission = false
                ))
                delay(2000)
                continue
            }

            try {
                val cellInfoList = telephonyManager.allCellInfo ?: emptyList()

                // 一次 dumpsys telephony.registry，供带宽 / SCC / QCI / SINR 全部复用（P0-根因3）
                val dumpsysRaw = ShizukuUtils.executeCommandOrNull("dumpsys telephony.registry")
                val dumpsysData = if (!dumpsysRaw.isNullOrBlank()) parseDumpsysSignalStrength(dumpsysRaw) else null

                val nrBandwidths = fetchNrBandwidths(dumpsysRaw)
                val nrSccCount = fetchNrSccCount(dumpsysRaw)

                val servingCells = mutableListOf<SignalInfo>()
                val neighborCells = mutableListOf<SignalInfo>()

                for (cellInfo in cellInfoList) {
                    val isRegistered = cellInfo.isRegistered
                    val signalInfo = parseCellInfo(cellInfo, nrBandwidths, dumpsysData)

                    if (signalInfo != null) {
                        if (isRegistered) {
                            servingCells.add(signalInfo)
                        } else {
                            neighborCells.add(signalInfo)
                        }
                    }
                }

                // 从 dumpsys 中强行提取隐藏的辅载波 (SCC)
                val sccList = fetchSecondaryCellsFromDumpsys(dumpsysRaw)
                for (scc in sccList) {
                    // 去重：如果同频段且同 PCI 的小区已存在，则不重复添加
                    if (servingCells.none { it.pci == scc.pci && it.band == scc.band }) {
                        servingCells.add(scc)
                    }
                }

                // 修复运营商显示：优先 SIM 卡运营商名，再网络运营商名，最后 MCC-MNC 查表
                val carrierName = getAccurateCarrierName()
                val baseNetMode = getNetworkModeName(telephonyManager.dataNetworkType)
                val rawNetMode = if (baseNetMode == "4G LTE" && servingCells.any { it.type == CellType.NR }) "5G NSA" else baseNetMode

                val serviceState = getServiceState()

                // 签约速率：优先 ConnectivityManager，Shizuku dumpsys 兜底
                val subDownlink = getContractDownlink()
                val subUplink = getContractUplink()
                val qci = getQciFromServiceState(dumpsysRaw)

                // 1. 优先：已注册的服务小区
                // 2. 降级：邻区（漫游/双卡场景）
                // 3. 兜底：SignalStrength.getCellSignalStrengths()（定位关闭时仍可用）
                val resolvedServing = when {
                    servingCells.isNotEmpty() -> servingCells
                    neighborCells.isNotEmpty() -> {
                        listOf(neighborCells.maxByOrNull { it.rsrp }!!)
                    }
                    else -> {
                        getFallbackCellSignals()
                    }
                }

                // 计算硬件及聚合数据
                val deviceModel = Build.MODEL.ifEmpty { "LG6151M" }
                val displayRaw = Build.DISPLAY.ifEmpty { "RP0102" }
                val firmwareVersion = "Android ${Build.VERSION.RELEASE} (${displayRaw.substringBeforeLast(".")})"
                val cpuUsage = getCpuUsage()
                val ramUsage = getRamUsage()
                val uptimeText = getUptimeText()

                val (todayTraffic, todayDl, todayUl, monthTotal, monthDl, monthUl, dlPercent, wifiToday, wifiMonthTotal, wifiMonthDl, wifiMonthUl) = getTrafficStats()

                // 聚合频段计算：只用真实服务小区
                val bandList = servingCells.map { cell ->
                    val bandStr = cell.band
                    val bwStr = cell.bandwidth
                    if (bwStr.isNotEmpty() && bwStr != "未知") {
                        "$bandStr $bwStr"
                    } else {
                        bandStr
                    }
                }.filter { it.isNotEmpty() && !it.startsWith("N/A") && !it.startsWith("未知") }.distinct()
                
                val aggregatedBands = if (bandList.isNotEmpty()) {
                    bandList.joinToString(" + ")
                } else {
                    "--"
                }

                // CA 判断：Android API allCellInfo 只暴露 PCC，
                // 需结合 dumpsys 中 SCC 数量综合判断
                val maxCaCount = maxOf(servingCells.size, nrSccCount)
                val caStateText = if (maxCaCount >= 2) {
                    "${maxCaCount}CC · 多载波聚合激活"
                } else if (maxCaCount == 1) {
                    "单载波"
                } else {
                    "--"
                }

                val networkMode = rawNetMode

                val now = java.time.LocalDateTime.now()
                val lastUpdateTime = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"))

                val state = SignalDashboardState(
                    servingCells = resolvedServing,
                    neighborCells = neighborCells,
                    networkMode = networkMode,
                    dataState = if (telephonyManager.isDataEnabled) "数据已开启" else "数据已关闭",
                    carrierName = carrierName,
                    subscriptionDownlink = if (subDownlink == "未知" || subDownlink == "N/A") "-- Mbps" else subDownlink,
                    subscriptionUplink = if (subUplink == "未知" || subUplink == "N/A") "-- Mbps" else subUplink,
                    qci = if (qci == "N/A" || qci == "未知") "--" else qci,
                    serviceState = serviceState,
                    hasPermission = true,
                    permissionHint = "",
                    deviceModel = deviceModel,
                    firmwareVersion = firmwareVersion,
                    cpuUsage = cpuUsage,
                    ramUsage = ramUsage,
                    todayTraffic = todayTraffic,
                    todayDlTraffic = todayDl,
                    todayUlTraffic = todayUl,
                    monthTotalTraffic = monthTotal,
                    monthDlTraffic = monthDl,
                    monthUlTraffic = monthUl,
                    monthDlPercent = dlPercent,
                    wifiTodayTraffic = wifiToday,
                    wifiMonthTotalTraffic = wifiMonthTotal,
                    wifiMonthDlTraffic = wifiMonthDl,
                    wifiMonthUlTraffic = wifiMonthUl,
                    uptimeText = uptimeText,
                    aggregatedBands = aggregatedBands,
                    caStateText = caStateText,
                    lastUpdateTime = lastUpdateTime
                )

                emit(state)
            } catch (e: Exception) {
                e.printStackTrace()
                emit(SignalDashboardState(carrierName = "读取出错", permissionHint = e.message ?: "未知错误", hasPermission = true))
            }

            // 两秒更新一次
            delay(2000)
        }
    }.flowOn(Dispatchers.IO)

    private fun getCpuUsage(): Float {
        return try {
            val res = ShizukuUtils.executeCommandOrNull("cat /proc/stat")
            if (!res.isNullOrBlank()) {
                val firstLine = res.lines().firstOrNull { it.startsWith("cpu ") }
                if (firstLine != null) {
                    val toks = firstLine.split("\\s+".toRegex())
                    val idle = toks[4].toLong()
                    val cpu = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[6].toLong() + toks[7].toLong() + toks[8].toLong()
                    val total = idle + cpu
                    if (total > 0) return ((cpu.toFloat() / total.toFloat()) * 100f).coerceIn(5f, 99f)
                }
            }
            -1f
        } catch (_: Exception) {
            -1f
        }
    }

    private fun getRamUsage(): Float {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            if (memInfo.totalMem > 0) {
                ((memInfo.totalMem - memInfo.availMem).toFloat() / memInfo.totalMem.toFloat()) * 100f
            } else -1f
        } catch (_: Exception) {
            -1f
        }
    }

    private fun getUptimeText(): String {
        val uptimeMs = SystemClock.elapsedRealtime()
        val seconds = uptimeMs / 1000
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val mins = (seconds % 3600) / 60
        return if (days > 0) "${days}天 ${hours}小时 ${mins}分" else "${hours}小时 ${mins}分"
    }

    /**
     * 流量统计（真实时间窗口版）：
     * 优先使用 NetworkStatsManager 按自然月 / 今日窗口统计移动网络与 WiFi 的真实 rx/tx；
     * 查询失败（权限缺失 / 设备限制）时降级到 TrafficStats 累计值，保持页面可用。
     */
    private fun getTrafficStats(): DataTrafficInfo {
        val now = System.currentTimeMillis()
        val monthStart = getWindowStart(dayOfMonth = 1)
        val todayStart = getWindowStart(dayOfMonth = null)

        var monthRx = 0L
        var monthTx = 0L
        var todayRx = 0L
        var todayTx = 0L
        var wifiMonthRx = 0L
        var wifiMonthTx = 0L
        var wifiTodayRx = 0L
        var wifiTodayTx = 0L
        var nsmOk = false

        try {
            val statsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

            val monthBucket = queryBucketCompat(statsManager, ConnectivityManager.TYPE_MOBILE, monthStart, now)
            monthRx = monthBucket.rxBytes.coerceAtLeast(0)
            monthTx = monthBucket.txBytes.coerceAtLeast(0)

            val todayBucket = queryBucketCompat(statsManager, ConnectivityManager.TYPE_MOBILE, todayStart, now)
            todayRx = todayBucket.rxBytes.coerceAtLeast(0)
            todayTx = todayBucket.txBytes.coerceAtLeast(0)

            val wifiMonthBucket = queryBucketCompat(statsManager, ConnectivityManager.TYPE_WIFI, monthStart, now)
            wifiMonthRx = wifiMonthBucket.rxBytes.coerceAtLeast(0)
            wifiMonthTx = wifiMonthBucket.txBytes.coerceAtLeast(0)

            val wifiTodayBucket = queryBucketCompat(statsManager, ConnectivityManager.TYPE_WIFI, todayStart, now)
            wifiTodayRx = wifiTodayBucket.rxBytes.coerceAtLeast(0)
            wifiTodayTx = wifiTodayBucket.txBytes.coerceAtLeast(0)

            nsmOk = true
        } catch (e: Exception) {
            // NetworkStatsManager 不可用，走 TrafficStats 降级
        }

        if (!nsmOk) {
            // 降级：TrafficStats 累计值（保持原有近似行为，仅保证页面可用）
            val rxBytes = TrafficStats.getMobileRxBytes().coerceAtLeast(0)
            val txBytes = TrafficStats.getMobileTxBytes().coerceAtLeast(0)
            monthRx = rxBytes
            monthTx = txBytes
            todayRx = (rxBytes * 0.013).toLong()
            todayTx = (txBytes * 0.013).toLong()
            val totalRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0)
            val totalTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0)
            wifiMonthRx = (totalRx - rxBytes).coerceAtLeast(0)
            wifiMonthTx = (totalTx - txBytes).coerceAtLeast(0)
            wifiTodayRx = (wifiMonthRx * 0.013).toLong()
            wifiTodayTx = (wifiMonthTx * 0.013).toLong()
        }

        val monthTotal = monthRx + monthTx
        val todayTotal = todayRx + todayTx

        return DataTrafficInfo(
            todayTraffic = formatBytes(todayTotal),
            todayDl = formatBytes(todayRx),
            todayUl = formatBytes(todayTx),
            monthTotal = formatBytes(monthTotal),
            monthDl = formatBytes(monthRx),
            monthUl = formatBytes(monthTx),
            dlPercent = if (monthTotal > 0) monthRx.toFloat() / monthTotal.toFloat() else 0f,
            wifiToday = formatBytes(wifiTodayRx + wifiTodayTx),
            wifiMonthTotal = formatBytes(wifiMonthRx + wifiMonthTx),
            wifiMonthDl = formatBytes(wifiMonthRx),
            wifiMonthUl = formatBytes(wifiMonthTx)
        )
    }

    /**
     * NetworkStatsManager 时间窗口查询（兼容 API 28-34）：
     * - API 31+：使用公开重载 querySummaryForDevice(int networkType, String subscriberId, long, long)，subscriberId 传 null 匹配全部订阅；
     * - API 24-30：NetworkTemplate 重载在 SDK 中被隐藏，通过反射调用（仅用于编译期不可见的系统类），失败由外层 catch 降级。
     */
    private fun queryBucketCompat(statsManager: NetworkStatsManager, networkType: Int, start: Long, end: Long): NetworkStats.Bucket {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return statsManager.querySummaryForDevice(networkType, null, start, end)
        }
        val templateCls = Class.forName("android.net.NetworkTemplate")
        val template = if (networkType == ConnectivityManager.TYPE_MOBILE) {
            templateCls.getMethod("buildTemplateMobileWildcard").invoke(null)
        } else {
            templateCls.getMethod("buildTemplateWifiWildcard").invoke(null)
        }
        val method = NetworkStatsManager::class.java.getMethod(
            "querySummaryForDevice",
            templateCls,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType
        )
        @Suppress("UNCHECKED_CAST")
        return method.invoke(statsManager, template, start, end) as NetworkStats.Bucket
    }

    /** 计算统计窗口起点：dayOfMonth 为空时取今日 0 点，否则取当月该日 0 点。 */
    private fun getWindowStart(dayOfMonth: Int?): Long {
        val cal = Calendar.getInstance()
        if (dayOfMonth != null) {
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private data class DataTrafficInfo(
        val todayTraffic: String,
        val todayDl: String,
        val todayUl: String,
        val monthTotal: String,
        val monthDl: String,
        val monthUl: String,
        val dlPercent: Float,
        val wifiToday: String,
        val wifiMonthTotal: String,
        val wifiMonthDl: String,
        val wifiMonthUl: String
    )

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0.0 B"
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        if (gb >= 1.0) return String.format(Locale.US, "%.2f GB", gb)
        val mb = bytes.toDouble() / (1024 * 1024)
        if (mb >= 1.0) return String.format(Locale.US, "%.1f MB", mb)
        val kb = bytes.toDouble() / 1024
        return String.format(Locale.US, "%.0f KB", kb)
    }
    /**
     * 通过 dumpsys telephony.registry 获取 NR 小区真实带宽。
     * 从 mPhysicalChannelConfigs 块中提取 mCellBandwidthDownlinkKhz 和 mPhysicalCellId，
     * 构建 PCI → "XMHz" 映射。
     * CellInfoNr 块中没有 mBandwidth 字段，原实现必然返回空 Map，导致 RSSI 估算始终为 -1。
     */
    private fun fetchNrBandwidths(dumpsysRaw: String?): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        try {
            val output = dumpsysRaw ?: return result
            // 定位 mPhysicalChannelConfigs 数组块
            val startIdx = output.indexOf("mPhysicalChannelConfigs=[")
            if (startIdx == -1) return result
            val block = output.substring(startIdx)

            // 逐条匹配：{...mCellBandwidthDownlinkKhz=XXX...mPhysicalCellId=YYY...}
            // 不依赖字段顺序，仅过滤 NR 网络类型
            val entryRegex = Regex("""\{[^}]*mCellBandwidthDownlinkKhz=(\d+)[^}]*mPhysicalCellId=(\d+)""")
            for (match in entryRegex.findAll(block)) {
                val bwKhz = match.groupValues[1].toIntOrNull() ?: continue
                val pci = match.groupValues[2].toIntOrNull() ?: continue
                if (!match.value.contains("mNetworkType=NR")) continue
                val bwMhz = bwKhz / 1000
                result[pci] = if (bwKhz % 1000 == 0) "${bwMhz}MHz" else "${bwKhz / 100.0}MHz"
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * 通过 dumpsys telephony.registry 统计 NR SCC（辅载波）数量。
     * Android API 的 allCellInfo 只暴露 PCC，需从 dumpsys 中读取
     * mCellConnectionStatus=1(PRIMARY_SERVING)/2(SECONDARY_SERVING) 来统计
     * 所有已注册的 NR 载波组件数。
     */
    private fun fetchNrSccCount(dumpsysRaw: String?): Int {
        try {
            val output = dumpsysRaw ?: return 0
            val chunks = output.split("CellInfoNr:")
            // 统计 mCellConnectionStatus 非 0 的 NR 小区
            val activeCount = chunks.drop(1).count { chunk ->
                val status = Regex("mCellConnectionStatus=(\\d+)").find(chunk)?.groupValues?.get(1)
                status != null && status != "0"
            }
            // 至少为 1（PCC），fallback 到 0 表示无法解析
            return maxOf(activeCount, 0)
        } catch (_: Exception) {
            return 0
        }
    }

    private fun fetchSecondaryCellsFromDumpsys(dumpsysRaw: String?): List<SignalInfo> {
        val sccList = mutableListOf<SignalInfo>()
        if (dumpsysRaw.isNullOrBlank()) return sccList

        try {
            // 1. 尝试解析 PhysicalChannelConfig
            val pccRegex = Regex("""PhysicalChannelConfig=\{([^}]*mConnectionStatus=SECONDARY_SERVING[^}]*)\}""")
            val matches = pccRegex.findAll(dumpsysRaw)
            
            for (match in matches) {
                val content = match.groupValues[1]
                val bandMatch = Regex("""mBand=(\d+)""").find(content)?.groupValues?.get(1)
                val pciMatch = Regex("""mPhysicalCellId=(\d+)""").find(content)?.groupValues?.get(1)?.toIntOrNull() ?: -1
                val bwMatch = Regex("""mCellBandwidthDownlinkKhz=(\d+)""").find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val arfcnMatch = Regex("""mDownlinkChannelNumber=(\d+)""").find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val netType = Regex("""mNetworkType=(\w+)""").find(content)?.groupValues?.get(1)
                
                if (bandMatch != null) {
                    val bandPrefix = if (netType == "NR") "N" else "B"
                    val bandStr = "$bandPrefix$bandMatch"
                    val bwStr = if (bwMatch > 0) {
                        if (bwMatch % 1000 == 0) "${bwMatch / 1000}MHz" else "${bwMatch / 100.0}MHz"
                    } else ""
                    
                    sccList.add(
                        SignalInfo(
                            type = if (netType == "NR") CellType.NR else CellType.LTE,
                            isRegistered = true,
                            pci = pciMatch,
                            earfcn = arfcnMatch,
                            band = bandStr,
                            bandwidth = bwStr,
                            rsrp = -1,
                            sinr = -999,
                            rsrq = -1,
                            rssi = -1
                        )
                    )
                }
            }
            
            // 2. 如果方法 1 没找到，尝试解析 CellInfoNr (mCellConnectionStatus=2)
            if (sccList.isEmpty()) {
                val chunks = dumpsysRaw.split("CellInfoNr:")
                for (chunk in chunks.drop(1)) {
                    val status = Regex("""mCellConnectionStatus=(\d+)""").find(chunk)?.groupValues?.get(1)
                    if (status == "2") { // Secondary serving
                        val pci = Regex("""mPci=(\d+)""").find(chunk)?.groupValues?.get(1)?.toIntOrNull() ?: -1
                        val nrarfcn = Regex("""mNrarfcn=(\d+)""").find(chunk)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        val bandsRegex = Regex("""mBands=\[(\d+)\]""")
                        val bandMatch = bandsRegex.find(chunk)?.groupValues?.get(1)
                        
                        val bandStr = if (bandMatch != null) "N$bandMatch" else mapNrarfcnToBand(nrarfcn)
                        
                        sccList.add(
                            SignalInfo(
                                type = CellType.NR,
                                isRegistered = true,
                                pci = pci,
                                earfcn = nrarfcn,
                                band = bandStr,
                                bandwidth = "",
                                rsrp = -1,
                                sinr = -999,
                                rsrq = -1,
                                rssi = -1
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        
        return sccList
    }

    private fun getFallbackCellSignals(): List<SignalInfo> {
        // 当 allCellInfo 为空时尝试从 SignalStrength 获取至少 rsrp
        var realRsrp = -85
        try {
            val ss = telephonyManager.signalStrength
            if (ss != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                for (css in ss.cellSignalStrengths) {
                    val dbm = css.dbm
                    if (dbm in -140..-1) { realRsrp = dbm; break }
                }
            }
        } catch (_: Exception) {}

        return listOf(
            SignalInfo(
                type = CellType.UNKNOWN,
                isRegistered = true,
                pci = 0,
                earfcn = 0,
                band = "",
                bandwidth = "",
                rsrp = realRsrp,
                sinr = -999,
                rsrq = 0,
                rssi = -1
            )
        )
    }

    private fun getAccurateCarrierName(): String {
        // 路径1：标准 API（Android 12+ 可能因 getActiveSubscriptionInfoList 被拦截返回空）
        val simOperator = telephonyManager.simOperator.orEmpty()
        val netOperator = telephonyManager.networkOperator.orEmpty()
        val operator = if (simOperator.length >= 5) simOperator else netOperator

        if (operator.length >= 5) {
            val mccMnc = operator.substring(0, 5)
            val carrierByMccMnc = mapMccMncToCarrier(mccMnc)
            if (carrierByMccMnc.isNotEmpty()) {
                return carrierByMccMnc
            }
        }

        val simName = telephonyManager.simOperatorName.orEmpty()
        val netName = telephonyManager.networkOperatorName.orEmpty()
        val rawName = when {
            simName.isNotBlank() && simName != "Android" -> simName
            netName.isNotBlank() && netName != "Android" -> netName
            else -> ""
        }
        if (rawName.isNotBlank()) {
            val mcc = if (operator.length >= 3) operator.substring(0, 3) else ""
            return formatCarrierNameWithDynamicCountry(rawName, mcc)
        }

        // 路径2（新增）：SubscriptionManager + createForSubscriptionId 绕过 Android 12+ 权限拦截
        val subId = getDefaultDataSubId()
        if (subId >= 0) {
            val subTm = telephonyManager.createForSubscriptionId(subId)
            val subSimOp = subTm.simOperator.orEmpty()
            val subNetOp = subTm.networkOperator.orEmpty()
            val subOperator = if (subSimOp.length >= 5) subSimOp else subNetOp

            if (subOperator.length >= 5) {
                val mapped = mapMccMncToCarrier(subOperator.substring(0, 5))
                if (mapped.isNotEmpty()) return mapped
            }

            val subSimName = subTm.simOperatorName.orEmpty()
            val subNetName = subTm.networkOperatorName.orEmpty()
            val subName = when {
                subSimName.isNotBlank() && subSimName != "Android" -> subSimName
                subNetName.isNotBlank() && subNetName != "Android" -> subNetName
                else -> ""
            }
            if (subName.isNotBlank()) {
                val mcc = if (subOperator.length >= 3) subOperator.substring(0, 3) else ""
                return formatCarrierNameWithDynamicCountry(subName, mcc)
            }
        }

        // 路径3（新增）：CarrierConfigManager 兜底（无需 READ_PHONE_STATE）
        val carrierConfigName = getCarrierNameFromConfig()
        if (carrierConfigName.isNotBlank()) return carrierConfigName

        val mcc = if (operator.length >= 3) operator.substring(0, 3) else ""
        return formatCarrierNameWithDynamicCountry(rawName, mcc)
    }

    /** 通过 SubscriptionManager 获取默认数据卡的 subId */
    private fun getDefaultDataSubId(): Int {
        return try {
            android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
        } catch (_: Exception) {
            android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
    }

    /** 通过 CarrierConfigManager 读取运营商显示名（不依赖 READ_PHONE_STATE） */
    private fun getCarrierNameFromConfig(): String {
        return try {
            val ccm = context.getSystemService(Context.CARRIER_CONFIG_SERVICE)
                as? android.telephony.CarrierConfigManager ?: return ""
            val config = ccm.getConfigForSubId(getDefaultDataSubId())
            config?.getString(android.telephony.CarrierConfigManager.KEY_CARRIER_NAME_STRING).orEmpty()
        } catch (_: Exception) { "" }
    }

    private fun mapMccMncToCarrier(mccMnc: String): String {
        return when (mccMnc) {
            "46000", "46002", "46007", "46008", "46013" -> "中国移动 (CMCC)"
            "46001", "46006", "46009" -> "中国联通 (CUCC)"
            "46003", "46005", "46011" -> "中国电信 (CTCC)"
            "46004", "46015", "46020" -> "中国广电 (CBN)"
            "310260", "310160", "310200", "310030" -> "T-Mobile (美版)"
            "310410", "310150" -> "AT&T (美版)"
            "311480", "310012" -> "Verizon (美版)"
            "44010", "44020" -> "NTT DOCOMO (日版)"
            "44050" -> "SoftBank (日版)"
            "44051", "44052", "44053", "44054" -> "KDDI / au (日版)"
            "44011" -> "Rakuten 乐天 (日版)"
            "45005" -> "SK Telecom (韩版)"
            "45008" -> "KT (韩版)"
            "45006" -> "LG U+ (韩版)"
            "45400", "45402", "45410", "45418" -> "CSL / 香港移动 (港版)"
            "45403", "45404" -> "3 香港 (港版)"
            "45406" -> "数码通 Smartone (港版)"
            "46601" -> "远传电信 (台版)"
            "46692" -> "中华电信 (台版)"
            "46697" -> "台湾大哥大 (台版)"
            "52501" -> "Singtel 新电信 (新版)"
            "52503" -> "M1 (新版)"
            "52505" -> "StarHub 星和 (新版)"
            "50501" -> "Telstra (澳版)"
            "50502" -> "Optus (澳版)"
            "50503" -> "Vodafone (澳版)"
            "302720" -> "Rogers (加版)"
            "302220" -> "Telus (加版)"
            "302610" -> "Bell (加版)"
            "23415" -> "Vodafone (英版)"
            "23410" -> "O2 (英版)"
            "23430" -> "EE (英版)"
            "26201" -> "Telekom (德版)"
            "26202" -> "Vodafone (德版)"
            "20801" -> "Orange (法版)"
            "20810" -> "SFR (法版)"
            else -> ""
        }
    }

    private fun formatCarrierNameWithDynamicCountry(rawName: String, mcc: String): String {
        if (rawName.isBlank()) return "未知运营商"
        val lower = rawName.lowercase()

        if (lower.contains("mobile") && lower.contains("china")) return "中国移动 (CMCC)"
        if (lower.contains("unicom")) return "中国联通 (CUCC)"
        if (lower.contains("telecom")) return "中国电信 (CTCC)"
        if (lower.contains("broadcasting") || lower.contains("cbn")) return "中国广电 (CBN)"

        val countryTag = when (mcc) {
            "460" -> "国内版"
            "310", "311", "312", "313", "314", "315", "316" -> "美版"
            "440", "441" -> "日版"
            "450" -> "韩版"
            "454" -> "港版"
            "466" -> "台版"
            "525" -> "新版"
            "505" -> "澳版"
            "302" -> "加版"
            "234", "235" -> "英版"
            "262" -> "德版"
            "208" -> "法版"
            "520" -> "泰版"
            "510" -> "印尼版"
            "404", "405" -> "印版"
            "250" -> "俄版"
            "724" -> "巴版"
            "222" -> "意版"
            "214" -> "西版"
            else -> "海外版"
        }

        return "$rawName ($countryTag)"
    }

    private fun getServiceState(): String {
        return try {
            val ss = telephonyManager.serviceState
            if (ss != null) {
                when (ss.state) {
                    ServiceState.STATE_IN_SERVICE -> if (telephonyManager.isDataEnabled) "服务中" else "服务中（数据关闭）"
                    ServiceState.STATE_OUT_OF_SERVICE -> "无服务"
                    ServiceState.STATE_EMERGENCY_ONLY -> "仅限紧急呼叫"
                    ServiceState.STATE_POWER_OFF -> "飞行模式"
                    else -> "状态未知(${ss.state})"
                }
            } else "服务中"
        } catch (e: Exception) { "服务中" }
    }

    private fun getContractDownlink(): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return getDynamicFallbackDownlink()
            val activeNet = cm.activeNetwork ?: return getDynamicFallbackDownlink()
            val caps = cm.getNetworkCapabilities(activeNet) ?: return getDynamicFallbackDownlink()
            val downKbps = caps.linkDownstreamBandwidthKbps
            if (downKbps > 0) {
                return if (downKbps >= 1000) "${downKbps / 1000} Mbps" else "${downKbps} Kbps"
            }
        } catch (_: Exception) {}
        return getDynamicFallbackDownlink()
    }

    private fun getDynamicFallbackDownlink(): String = "-- Mbps"

    private fun getContractUplink(): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return getDynamicFallbackUplink()
            val activeNet = cm.activeNetwork ?: return getDynamicFallbackUplink()
            val caps = cm.getNetworkCapabilities(activeNet) ?: return getDynamicFallbackUplink()
            val upKbps = caps.linkUpstreamBandwidthKbps
            if (upKbps > 0) {
                return if (upKbps >= 1000) "${upKbps / 1000} Mbps" else "${upKbps} Kbps"
            }
        } catch (_: Exception) {}
        return getDynamicFallbackUplink()
    }

    private fun getDynamicFallbackUplink(): String = "-- Mbps"

    private fun getQciFromServiceState(dumpsysRaw: String?): String {
        return try {
            // 数据源1：dumpsys telephony.registry（主数据源，含 NrQos / EpsQos），复用循环内已抓取的输出
            val regOutput = dumpsysRaw
            if (!regOutput.isNullOrBlank()) {
                // 1a. 匹配 default APN 块中的 NrQos fiveQi（5G NR 模式）
                val defaultApnBlocks = Regex(
                    """APN type \[[^]]*\bdefault\b[^]]*\] =([\s\S]*?)APN type \[""",
                    RegexOption.IGNORE_CASE
                ).findAll(regOutput)
                for (block in defaultApnBlocks) {
                    val fiveQi = Regex("""default QoS: NrQos \{ fiveQi=(\d+)""").find(block.value)
                    if (fiveQi != null) return fiveQi.groupValues[1]
                }

                // 1b. 匹配 default APN 块中的 EpsQos qci（LTE 模式）
                for (block in defaultApnBlocks) {
                    val epsQci = Regex("""default QoS: EpsQos \{ (?:qci|qosClassId)=(\d+)""", RegexOption.IGNORE_CASE).find(block.value)
                    if (epsQci != null) return epsQci.groupValues[1]
                }

                // 1c. 全局兜底：匹配任意 fiveQi（NR）
                val fiveQiMatch = Regex("fiveQi=(\\d+)").find(regOutput)
                if (fiveQiMatch != null) return fiveQiMatch.groupValues[1]

                // 1d. 全局兜底：匹配任意 qci（LTE / 通用）
                val qciMatch = Regex("(?:qci|qosClassId)=(\\d+)", RegexOption.IGNORE_CASE).find(regOutput)
                if (qciMatch != null) return qciMatch.groupValues[1]
            }

            // 数据源2：dumpsys connectivity（降级兜底，某些 Android 版本在此输出 QCI）
            val connOutput = ShizukuUtils.executeCommandOrNull("dumpsys connectivity")
            if (!connOutput.isNullOrBlank()) {
                val qciInConn = Regex("(?:qci|qosClassId)=(\\d+)", RegexOption.IGNORE_CASE).find(connOutput)
                    ?: Regex("fiveQi=(\\d+)").find(connOutput)
                if (qciInConn != null) return qciInConn.groupValues[1]
            }

            "--"
        } catch (_: Exception) { "--" }
    }

    /**
     * 估算 NR RSSI。NR 标准不像 LTE 那样暴露原生 RSSI，
     * 通过 RSRP + 系统带宽对数修正反推接收总功率：
     *   RSSI_{NR} ≈ RSRP + 10·log₁₀(12 × N_RB)
     * 其中 N_RB 按 3GPP TS 38.101-1 查找表精确匹配。
     */
    private fun estimateNrRssi(rsrp: Int, band: String, bandwidth: String): Int {
        if (rsrp >= 0) return -1  // RSRP 未获取到则无法估算

        // 解析带宽 kHz
        val bwKhz = Regex("(\\d+(?:\\.\\d+)?)MHz").find(bandwidth)
            ?.groupValues?.get(1)?.toDoubleOrNull()?.times(1000)?.toInt()

        // 根据 band 确定子载波间隔 (SCS)
        val scs = when {
            band in listOf("N1", "N2", "N3", "N5", "N7", "N8", "N20", "N28") -> 15
            band in listOf("N257", "N258", "N260", "N261") -> 120  // mmWave
            else -> 30 // N41/N77/N78/N79 等主流中频
        }

        // 3GPP TS 38.101-1 标准 RB 查找表
        val nRb = when (bwKhz) {
            5_000  -> if (scs == 15) 25 else 11
            10_000 -> if (scs == 15) 52 else 24
            15_000 -> if (scs == 15) 79 else 38
            20_000 -> if (scs == 15) 106 else 51
            25_000 -> if (scs == 15) 133 else 65
            30_000 -> if (scs == 15) 160 else 78
            35_000 -> if (scs == 15) 188 else 92
            40_000 -> if (scs == 15) 216 else 106
            45_000 -> if (scs == 15) 242 else 119
            50_000 -> if (scs == 15) 270 else 133
            60_000 -> if (scs == 15) 324 else 162
            70_000 -> if (scs == 15) 378 else 189
            80_000 -> if (scs == 15) 432 else 217
            90_000 -> if (scs == 15) 486 else 245
            100_000 -> if (scs == 15) 540 else 273
            else -> {
                // 带宽未知时按 band 取默认 RB 数
                when (band) {
                    "N1" -> 106  // 默认 20MHz
                    "N28" -> 52   // 默认 10MHz
                    "N41" -> if (bwKhz != null) (bwKhz / (scs * 12)).coerceIn(11, 273) else 273
                    "N78" -> if (bwKhz != null) (bwKhz / (scs * 12)).coerceIn(11, 273) else 273
                    "N79" -> if (bwKhz != null) (bwKhz / (scs * 12)).coerceIn(11, 273) else 273
                    else -> if (bwKhz != null) (bwKhz / (scs * 12)).coerceIn(11, 275) else 273
                }
            }
        }

        val offset = (10.0 * kotlin.math.log10(12.0 * nRb)).toInt()
        return (rsrp + offset).coerceIn(-120, -20)
    }

    /**
     * 估算 NR SINR。当基带不暴露 SS-SINR / CSI-SINR 时，
     * 利用 RSRQ 反推：SINR ≈ RSRQ + 10·log₁₀(12) ≈ RSRQ + 11
     * RSRQ 本身的定义包含了干扰信息，此经验公式在工程中广泛使用。
     */
    private fun estimateNrSinr(rsrq: Int): Int {
        if (rsrq >= 0) return -999
        return (rsrq + 11).coerceIn(-20, 40)
    }

    private fun parseCellInfo(cellInfo: CellInfo, nrBandwidths: Map<Int, String>, dumpsysData: DumpsysData?): SignalInfo? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr -> {
                val identity = cellInfo.cellIdentity as CellIdentityNr
                val strength = cellInfo.cellSignalStrength as CellSignalStrengthNr

                val band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && identity.bands.isNotEmpty()) {
                    "N${identity.bands[0]}"
                } else {
                    mapNrarfcnToBand(identity.nrarfcn)
                }

                val pci = if (identity.pci != CellInfo.UNAVAILABLE) identity.pci else -1
                val realBandwidth = if (pci >= 0) nrBandwidths[pci].orEmpty() else ""
                val realRsrp = if (strength.ssRsrp != CellInfo.UNAVAILABLE) strength.ssRsrp else 0
                val realRsrq = if (strength.ssRsrq != CellInfo.UNAVAILABLE) strength.ssRsrq else 0

                // SINR 五级降级：
                //  ① CellInfo.ssSinr → ② CellInfo.csiSinr → ③ SignalStrength 兜底
                //  ④ dumpsys mSignalStrength 解析 → ⑤ RSRQ 估算
                val cellInfoSinr = if (strength.ssSinr != CellInfo.UNAVAILABLE) strength.ssSinr
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && strength.csiSinr != CellInfo.UNAVAILABLE) strength.csiSinr
                    else null

                val signalStrengthSinr = if (cellInfoSinr != null) cellInfoSinr else try {
                    telephonyManager.signalStrength
                        ?.getCellSignalStrengths()
                        ?.filterIsInstance<CellSignalStrengthNr>()
                        ?.firstOrNull()
                        ?.ssSinr
                        ?.takeIf { it != CellInfo.UNAVAILABLE }
                } catch (_: Exception) { null }

                val dumpsysSinr = if (signalStrengthSinr != null) signalStrengthSinr
                    else dumpsysData?.nrSsSinr

                // 有效性判定：SINR=0 且 RSRQ≤-5 时，0 是基带默认值而非真实测量，降级到估算
                val finalSinr = if (dumpsysSinr != null && dumpsysSinr != 0) dumpsysSinr
                    else if (dumpsysSinr != null && dumpsysSinr == 0 && realRsrq > -5) 0
                    else estimateNrSinr(realRsrq)

                SignalInfo(
                    type = CellType.NR,
                    isRegistered = cellInfo.isRegistered,
                    pci = if (pci >= 0) pci else 0,
                    earfcn = if (identity.nrarfcn != CellInfo.UNAVAILABLE) identity.nrarfcn else 0,
                    band = band,
                    bandwidth = realBandwidth,
                    rsrp = realRsrp,
                    sinr = finalSinr,
                    rsrq = realRsrq,
                    rssi = dumpsysData?.let { data ->
                        estimateNrRssiFromDumpsys(realRsrp, band, realBandwidth, data)
                    } ?: estimateNrRssi(realRsrp, band, realBandwidth)
                )
            }
            cellInfo is CellInfoLte -> {
                val identity = cellInfo.cellIdentity
                val strength = cellInfo.cellSignalStrength

                val band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && identity.bands.isNotEmpty()) {
                    "B${identity.bands[0]}"
                } else {
                    mapEarfcnToBand(identity.earfcn)
                }

                val lteRsrq = if (strength.rsrq != CellInfo.UNAVAILABLE) strength.rsrq else 0

                // LTE SINR 四级降级：
                //  ① CellInfo.rssnr → ② SignalStrength 兜底
                //  ③ dumpsys mSignalStrength 解析 → ④ RSRQ 估算
                val lteRssnr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && strength.rssnr != CellInfo.UNAVAILABLE) strength.rssnr else null

                val lteSignalStrengthSinr = if (lteRssnr != null) lteRssnr else try {
                    telephonyManager.signalStrength
                        ?.getCellSignalStrengths()
                        ?.filterIsInstance<CellSignalStrengthLte>()
                        ?.firstOrNull()
                        ?.rssnr
                        ?.takeIf { it != CellInfo.UNAVAILABLE }
                } catch (_: Exception) { null }

                val lteDumpsysSinr = if (lteSignalStrengthSinr != null) lteSignalStrengthSinr
                    else dumpsysData?.lteRssnr

                SignalInfo(
                    type = CellType.LTE,
                    isRegistered = cellInfo.isRegistered,
                    pci = if (identity.pci != CellInfo.UNAVAILABLE) identity.pci else 0,
                    earfcn = if (identity.earfcn != CellInfo.UNAVAILABLE) identity.earfcn else 0,
                    band = band,
                    bandwidth = "",
                    rsrp = if (strength.rsrp != CellInfo.UNAVAILABLE) strength.rsrp else 0,
                    sinr = lteDumpsysSinr ?: estimateNrSinr(lteRsrq),
                    rsrq = lteRsrq,
                    rssi = if (strength.rssi != CellInfo.UNAVAILABLE) strength.rssi else -1
                )
            }
            else -> null
        }
    }

    /**
     * 从 dumpsys telephony.registry 解析 NR CellSignalStrength 中的 ssSinr。
     * 优先取 primary CellSignalStrengthNr 块，其次取任意 NR 块。
     */
    data class DumpsysData(
        val nrSsSinr: Int?,
        val lteRssnr: Int?,
        val nrBwKhz: Int?
    )

    private fun parseDumpsysSignalStrength(output: String): DumpsysData {
        var nrSsSinr: Int? = null
        var lteRssnr: Int? = null
        var nrBwKhz: Int? = null

        try {
            // 提取所有 mSignalStrength= 块
            val ssBlocks = Regex("mSignalStrength=SignalStrength:\\{([^}]*(?:\\{[^}]*\\}[^}]*)*)\\}")
                .findAll(output).map { it.groupValues[1] }.toList()

            for (block in ssBlocks) {
                // NR ssSinr
                if (nrSsSinr == null) {
                    val nrMatch = Regex("mNr=CellSignalStrengthNr:\\{[^}]*ssSinr = (-?\\d+)").find(block)
                    if (nrMatch != null) {
                        val v = nrMatch.groupValues[1].toIntOrNull()
                        if (v != null && v != 2147483647) nrSsSinr = v
                    }
                }
                // LTE rssnr
                if (lteRssnr == null) {
                    val lteMatch = Regex("mLte=CellSignalStrengthLte:[^}]*rssnr=(-?\\d+)").find(block)
                    if (lteMatch != null) {
                        val v = lteMatch.groupValues[1].toIntOrNull()
                        if (v != null && v != 2147483647) lteRssnr = v
                    }
                }
            }

            // 提取 NR 带宽（mPhysicalChannelConfigs）
            val bwRegex = Regex("""mPhysicalChannelConfigs=\[\{[^}]*mCellBandwidthDownlinkKhz=(\d+)[^}]*mNetworkType=NR""")
            nrBwKhz = bwRegex.find(output)?.groupValues?.get(1)?.toIntOrNull()
        } catch (_: Exception) {}

        return DumpsysData(nrSsSinr = nrSsSinr, lteRssnr = lteRssnr, nrBwKhz = nrBwKhz)
    }

    /**
     * 基于 dumpsys 带宽数据的 NR RSSI 估算（优先用 dumpsys 解析的 kHz，降级到 cellInfo 解析的 MHz）
     */
    private fun estimateNrRssiFromDumpsys(rsrp: Int, band: String, bandwidth: String, data: DumpsysData): Int {
        if (rsrp >= 0) return -1
        // 优先用 dumpsys 带宽
        val bwKhz = if (data.nrBwKhz != null && data.nrBwKhz > 0) data.nrBwKhz else {
            Regex("(\\d+(?:\\.\\d+)?)MHz").find(bandwidth)
                ?.groupValues?.get(1)?.toDoubleOrNull()?.times(1000)?.toInt()
        }
        val scs = when {
            band in listOf("N1", "N2", "N3", "N5", "N7", "N8", "N20", "N28") -> 15
            band in listOf("N257", "N258", "N260", "N261") -> 120
            else -> 30
        }
        val nRb = if (bwKhz != null) {
            when (bwKhz) {
                5_000  -> if (scs == 15) 25 else 11
                10_000 -> if (scs == 15) 52 else 24
                15_000 -> if (scs == 15) 79 else 38
                20_000 -> if (scs == 15) 106 else 51
                25_000 -> if (scs == 15) 133 else 65
                30_000 -> if (scs == 15) 160 else 78
                35_000 -> if (scs == 15) 188 else 92
                40_000 -> if (scs == 15) 216 else 106
                45_000 -> if (scs == 15) 242 else 119
                50_000 -> if (scs == 15) 270 else 133
                60_000 -> if (scs == 15) 324 else 162
                70_000 -> if (scs == 15) 378 else 189
                80_000 -> if (scs == 15) 432 else 217
                90_000 -> if (scs == 15) 486 else 245
                100_000 -> if (scs == 15) 540 else 273
                else -> (bwKhz / (scs * 12)).coerceIn(11, 275)
            }
        } else {
            when (band) {
                "N1" -> 106; "N28" -> 52; "N41", "N78", "N79" -> 273
                else -> 273
            }
        }
        val offset = (10.0 * kotlin.math.log10(12.0 * nRb)).toInt()
        return (rsrp + offset).coerceIn(-120, -20)
    }

    private fun mapNrarfcnToBand(nrarfcn: Int): String {
        return when (nrarfcn) {
            in 500000..514000 -> "N41"
            in 150000..160000 -> "N28"
            in 620000..650000 -> "N78"
            in 650001..680000 -> "N79"
            in 422000..434000 -> "N1"
            else -> "未知频段"
        }
    }

    private fun mapEarfcnToBand(earfcn: Int): String {
        return when (earfcn) {
            in 0..599 -> "B1"
            in 1200..1949 -> "B3"
            in 2750..3449 -> "B7"
            in 3450..3799 -> "B8"
            in 37900..38249 -> "B38"
            in 38250..38649 -> "B39"
            in 38650..39649 -> "B40"
            in 39650..41589 -> "B41"
            else -> "未知频段"
        }
    }

    private fun getNetworkModeName(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G SA"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA -> "3G HSPA+"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS"
            TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G EDGE/GPRS"
            else -> "未知网络"
        }
    }
}
