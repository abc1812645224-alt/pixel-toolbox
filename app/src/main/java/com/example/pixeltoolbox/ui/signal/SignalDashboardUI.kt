package com.example.pixeltoolbox.ui.signal

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.example.pixeltoolbox.shizuku.ConfigReaderInstrumentation
import com.example.pixeltoolbox.shizuku.ShizukuProviderWrapper
import com.example.pixeltoolbox.shizuku.SimSlotInfo
import com.example.pixeltoolbox.signal.SignalMetrics
import com.example.pixeltoolbox.signal.NetworkMetrics
import com.example.pixeltoolbox.signal.DeviceMetrics
import com.example.pixeltoolbox.signal.TrafficMetrics
import com.example.pixeltoolbox.signal.SystemMetrics
import com.example.pixeltoolbox.signal.SignalInfo
import com.example.pixeltoolbox.ui.theme.*
import com.example.pixeltoolbox.ExecutionLogCard
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalDashboard(
    signalMetrics: SignalMetrics,
    networkMetrics: NetworkMetrics,
    deviceMetrics: DeviceMetrics,
    trafficMetrics: TrafficMetrics,
    systemMetrics: SystemMetrics,
    simSlots: List<SimSlotInfo> = emptyList(),
    selectedSubId: Int = -1,
    onSelectSubId: (Int) -> Unit = {},
    addLog: (String) -> Unit = {},
    executionLogs: List<String> = emptyList()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. 实时执行日志（随页面滚动）
        ExecutionLogCard(executionLogs)

        // 1. 顶栏：运营商与网络模式卡片
        CarrierHeaderCard(signalMetrics)

        // 1.5 SIM 卡选择 (仅当有多张卡时显示)
        if (simSlots.size > 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SIM 卡选择",
                            style = MaterialTheme.typography.labelLarge,
                            color = iOSLabel,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            simSlots.forEach { slot ->
                                val isSelected = slot.subId == selectedSubId
                                val carrierName = slot.carrierName.takeIf { it.isNotBlank() } ?: "SIM ${slot.slotIndex + 1}"
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectSubId(slot.subId) },
                                    label = {
                                        Text(
                                            carrierName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) Color.White else iOSLabel
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = iOSBlue,
                                        containerColor = Color.Transparent
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) iOSBlue else iOSSeparator,
                                        selectedBorderColor = iOSBlue
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
        }

        // ================= 第 2 部分: 5G / IMS 注入控制台 =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ImsInjectionCard(selectedSubId = selectedSubId, coroutineScope = coroutineScope, context = context, addLog = addLog)
            }
        }

        // 2. 签约速率与 QCI 卡片
        SubscriptionSpeedCard(networkMetrics)

        // 4. 设备性能与 RSRP 信号强度环形仪表卡片
        DeviceAndSignalGaugeCard(signalMetrics, deviceMetrics)

        // 5. 载波聚合 2CC/多载波卡片
        CarrierAggregationCard(signalMetrics)

        // 6. 流量详情与系统运行时间卡片
        TrafficAndUptimeCard(trafficMetrics, systemMetrics)

        // 7. 数据更新时间
        if (systemMetrics.lastUpdateTime != "--") {
            Text(
                text = "更新时间：${systemMetrics.lastUpdateTime}",
                style = MaterialTheme.typography.labelSmall,
                color = iOSSecondaryLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ImsInjectionCard(
    selectedSubId: Int,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    addLog: (String) -> Unit
) {
    var groupBasic by remember { mutableStateOf(true) }
    var group5gCore by remember { mutableStateOf(true) }
    var groupUiEnhancement by remember { mutableStateOf(true) }
    var groupVoWiFi by remember { mutableStateOf(false) }

    // 进入时通过 ConfigReaderInstrumentation 回读系统当前 CarrierConfig，
    // 将开关组设置为与系统真实状态一致（读取失败/未授权时保持默认值）
    LaunchedEffect(selectedSubId) {
        if (selectedSubId == -1) return@LaunchedEffect
        val cfg = withContext(Dispatchers.IO) {
            runCatching {
                ShizukuProviderWrapper.readCarrierConfig(context, selectedSubId)
            }.getOrNull()
        } ?: return@LaunchedEffect
        if (!cfg.getBoolean(ConfigReaderInstrumentation.KEY_RESULT, false)) return@LaunchedEffect
        groupBasic = cfg.getBoolean(ConfigReaderInstrumentation.KEY_VOLTE, groupBasic)
        group5gCore = cfg.getBoolean(ConfigReaderInstrumentation.KEY_5G_NR, group5gCore) &&
            (Build.VERSION.SDK_INT < 34 || cfg.getBoolean(ConfigReaderInstrumentation.KEY_VONR, false))
        groupUiEnhancement = cfg.getBoolean(ConfigReaderInstrumentation.KEY_5G_SIGNAL, groupUiEnhancement) ||
            cfg.getBoolean(ConfigReaderInstrumentation.KEY_5GA_ICON, false)
        groupVoWiFi = cfg.getBoolean(ConfigReaderInstrumentation.KEY_VOWIFI, groupVoWiFi)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("5G & IMS 通信底层注入", style = MaterialTheme.typography.labelLarge, color = iOSLabel)
        Spacer(modifier = Modifier.height(12.dp))

        // Switch 1: Basic
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("VoLTE 高清通话", style = MaterialTheme.typography.labelLarge, color = iOSLabel, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("强制开启 VoLTE/视频通话、解锁 APN、显示 IMS 状态", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Switch(
                checked = groupBasic,
                onCheckedChange = { groupBasic = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = iOSGreen)
            )
        }

        // Switch 2: 5G Core
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("5G NR (SA+NSA)", style = MaterialTheme.typography.labelLarge, color = iOSLabel, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("独立 5G 组网/非组网 + VoNR 5G 语音 + 跨SIM通话", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Switch(
                checked = group5gCore,
                onCheckedChange = { group5gCore = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = iOSGreen)
            )
        }

        // Switch 3: UI Enhancement
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("5G 信号显示优化", style = MaterialTheme.typography.labelLarge, color = iOSLabel, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("5G+/5GA 图标、信号阈值增强", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Switch(
                checked = groupUiEnhancement,
                onCheckedChange = { groupUiEnhancement = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = iOSGreen)
            )
        }

        // Switch 4: VoWiFi (独立控制 — 开启后 IMS 会通过 WiFi 与核心网明文交互，可能触发系统"未加密网络"通知)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("VoWiFi", style = MaterialTheme.typography.labelLarge, color = iOSLabel, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("WiFi 通话 + 强制漫游优先开启", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Switch(
                checked = groupVoWiFi,
                onCheckedChange = { groupVoWiFi = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = iOSGreen)
            )
        }

        // VoWiFi 说明文字
        Text(
            "开启 VoWiFi 后，系统 IMS 栈会通过 WiFi 与运营商核心网建立连接。" +
            "部分运营商的 IMS 初始注册过程使用 HTTP 明文传输，" +
            "Android 系统检测到后会在通知栏反复弹出「未加密网络」警告。" +
            "此警告不影响正常使用，若不希望看到该通知请关闭此开关。",
            style = MaterialTheme.typography.labelSmall,
            color = iOSSecondaryLabel,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        iOSButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (selectedSubId == -1) {
                    android.widget.Toast.makeText(context, "请先在上方选择一张 SIM 卡", android.widget.Toast.LENGTH_SHORT).show()
                    return@iOSButton
                }
                coroutineScope.launch {
                    addLog("🚀 开始执行 5G/IMS 底层配置注入 (Shizuku UserService)...")
                    addLog("目标 SIM ID: $selectedSubId")
                    addLog("参数: 基础通信=$groupBasic, 5G核心=$group5gCore, 显示增强=$groupUiEnhancement, VoWiFi=$groupVoWiFi")

                    val toggleMap = mapOf(
                        "volte" to groupBasic, "vowifi" to groupVoWiFi, "vilte" to groupBasic,
                        "ut" to groupBasic, "lte_4g" to groupBasic,
                        "vonr" to group5gCore, "nr_5g" to group5gCore, "cross_sim" to group5gCore,
                        "5g_signal" to groupUiEnhancement, "5ga_icon" to groupUiEnhancement,
                        "show_5ga" to groupUiEnhancement,
                        "5g_icon_upgrade" to groupUiEnhancement
                    )

                    kotlin.coroutines.suspendCoroutine<Pair<Boolean, String>> { cont ->
                        com.example.pixeltoolbox.shizuku.ShizukuUtils.applyCarrierConfig(
                            context, selectedSubId, toggleMap
                        ) { ok, msg -> cont.resumeWith(Result.success(Pair(ok, msg))) }
                    }.let { (ok, msg) ->
                        if (ok) {
                            addLog("✅ 注入成功！请开启再关闭飞行模式以生效（切勿重启手机，重启后配置会被系统还原）。")
                            android.widget.Toast.makeText(context, "注入成功！", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            addLog("❌ 注入失败: $msg")
                            android.widget.Toast.makeText(context, "注入失败，请查看日志", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        ) {
            Text("一键注入以上配置", color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ================= 1. 运营商卡片 =================
@Composable
fun CarrierHeaderCard(signalMetrics: SignalMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：绿色原点 + 运营商名
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(iOSGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = signalMetrics.carrierName,
                    style = MaterialTheme.typography.titleLarge,
                    color = iOSLabel
                )
            }

            // 中间：模式与频段聚合 Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iOSBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = signalMetrics.networkMode,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = iOSBlue
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iOSFill
                ) {
                    Text(
                        text = signalMetrics.aggregatedBands,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = iOSSecondaryLabel
                    )
                }
            }

            // 右侧：信号图标
            Icon(
                imageVector = Icons.Filled.SignalCellularAlt,
                contentDescription = "信号格",
                tint = iOSBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ================= 3. 签约速率卡片 =================
@Composable
fun SubscriptionSpeedCard(networkMetrics: NetworkMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = networkMetrics.subscriptionDownlink.replace(" Mbps", ""),
                        style = MaterialTheme.typography.headlineMedium,
                        color = iOSLabel
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mbps", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("签约下行", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = networkMetrics.subscriptionUplink.replace(" Mbps", ""),
                        style = MaterialTheme.typography.headlineMedium,
                        color = iOSLabel
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mbps", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("签约上行", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = networkMetrics.qci,
                    style = MaterialTheme.typography.headlineMedium,
                    color = iOSLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("QCI", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
            }
        }

        // 说明文字
        Text(
            text = "签约速率为网络侧分配的最大带宽上限，非实时测速。QCI 为承载优先级，8/9 为默认数据承载，数值越小优先级越高。",
            style = MaterialTheme.typography.labelSmall,
            color = iOSSecondaryLabel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

// ================= 4. 设备性能与 RSRP 信号强度仪表盘 =================
@Composable
fun DeviceAndSignalGaugeCard(signalMetrics: SignalMetrics, deviceMetrics: DeviceMetrics) {
    val primaryCell = signalMetrics.servingCells.firstOrNull()
    val rsrp = primaryCell?.rsrp ?: -83

    // 4级标准分级逻辑与颜色定义
    val (statusText, activeColor) = when {
        rsrp >= -75 -> Pair("信号极佳", Color(0xFF34C759)) // 绿色
        rsrp >= -85 -> Pair("信号良好", Color(0xFF007AFF)) // 蓝色
        rsrp >= -100 -> Pair("信号一般", Color(0xFFFF9500)) // 橙色
        else -> Pair("信号较差", Color(0xFFFF3B30))       // 红色
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：设备信息与性能占用
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = deviceMetrics.deviceModel,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFC69250) // 金棕/棕橙高亮
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    DeviceInfoRow("设备型号", if (deviceMetrics.deviceModel == "Google Pixel") "LG6151M" else deviceMetrics.deviceModel)
                    DeviceInfoRow("系统版本", deviceMetrics.firmwareVersion)
                    DeviceInfoRow("CPU使用率", if (deviceMetrics.cpuUsage >= 0) "${deviceMetrics.cpuUsage.toInt()}%" else "--")
                    DeviceInfoRow("内存使用率", if (deviceMetrics.ramUsage >= 0) "${String.format("%.2f", deviceMetrics.ramUsage)}%" else "--")
                }

                // 右侧：自定义 Gauge 动态变色仪表盘
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("信号强度", style = MaterialTheme.typography.labelLarge, color = iOSLabel)
                    Spacer(modifier = Modifier.height(10.dp))

                    val gaugeTrackColor = iOSSeparator
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(110.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(100.dp)
                                .drawWithCache {
                                    // P2: 静态底色灰弧绘制指令缓存
                                    val strokeWidth = 10.dp.toPx()
                                    val arcSize = Size(size.width.toFloat() - strokeWidth, size.height.toFloat() - strokeWidth)
                                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                                    onDrawBehind {
                                        drawArc(
                                            color = gaugeTrackColor,
                                            startAngle = 135f,
                                            sweepAngle = 270f,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                        ) {
                            val strokeWidth = 10.dp.toPx()
                            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                            // 绘制动态变色进度弧
                            val rsrpClamped = rsrp.coerceIn(-140, -50)
                            val fraction = (rsrpClamped - (-140)).toFloat() / 90f
                            val sweep = fraction * 270f

                            drawArc(
                                color = activeColor,
                                startAngle = 135f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$rsrp",
                                style = MaterialTheme.typography.headlineMedium,
                                color = iOSLabel
                            )
                            Text(
                                text = "dBm",
                                style = MaterialTheme.typography.labelSmall,
                                color = iOSSecondaryLabel
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = activeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = iOSSeparator, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 底部 4 级信号标准分级颜色图例注释 (全宽独立展示，防止遮挡或被压缩)
            Text("信号等级说明", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignalLevelLegendBadge(color = Color(0xFF34C759), label = "极佳", range = "≥-75")
                SignalLevelLegendBadge(color = Color(0xFF007AFF), label = "良好", range = "-76~-85")
                SignalLevelLegendBadge(color = Color(0xFFFF9500), label = "一般", range = "-86~-100")
                SignalLevelLegendBadge(color = Color(0xFFFF3B30), label = "较差", range = "<-100")
            }
        }
    }
}

@Composable
fun SignalLevelLegendBadge(color: Color, label: String, range: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = iOSFill,
        border = androidx.compose.foundation.BorderStroke(1.dp, iOSSeparator)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = iOSLabel, maxLines = 1, softWrap = false)
            Spacer(modifier = Modifier.width(2.dp))
            Text(range, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${label}：",
            style = MaterialTheme.typography.bodySmall,
            color = iOSSecondaryLabel,
            modifier = Modifier.width(82.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = iOSLabel,
            modifier = Modifier.weight(1f)
        )
    }
}

// ================= 5. 载波聚合卡片 =================
@Composable
fun CarrierAggregationCard(signalMetrics: SignalMetrics) {
    val servingCells = signalMetrics.servingCells
    val caStateText = signalMetrics.caStateText
    var showCaHelpDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconColor = iOSBlue
                    Canvas(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.12f))
                    ) {
                        val strokeW = size.minDimension * 0.10f
                        val r = size.minDimension * 0.20f
                        val dx = size.width * 0.12f
                        val cx1 = size.width / 2 - dx
                        val cx2 = size.width / 2 + dx
                        val cy = size.height / 2
                        drawCircle(iconColor, r, Offset(cx1, cy), style = Stroke(width = strokeW, cap = StrokeCap.Round))
                        drawCircle(iconColor, r, Offset(cx2, cy), style = Stroke(width = strokeW, cap = StrokeCap.Round))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("载波聚合", style = MaterialTheme.typography.titleMedium, color = iOSLabel)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "说明",
                        tint = iOSSecondaryLabel.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { showCaHelpDialog = true }
                    )
                }

                Text(
                    text = caStateText,
                    style = MaterialTheme.typography.labelMedium,
                    color = iOSSecondaryLabel
                )
            }

            if (servingCells.isEmpty() || caStateText == "--") {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "暂无载波聚合数据",
                    style = MaterialTheme.typography.bodyLarge,
                    color = iOSSecondaryLabel,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                val primaryCell = servingCells[0]

                Spacer(modifier = Modifier.height(14.dp))

                // PCC 主载波区域
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = iOSFill,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = iOSBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "PCC",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = iOSBlue
                                )
                            }

                            val pccLabel = if (primaryCell.band.isNotEmpty()) {
                                val bw = primaryCell.bandwidth
                                if (bw.isNotEmpty()) "${primaryCell.band} $bw" else primaryCell.band
                            } else {
                                "未知频段"
                            }
                            Text(
                                pccLabel,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = iOSLabel
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricBadge("RSRP", "${primaryCell.rsrp}", iOSGreen)
                            MetricBadge("SINR", if (primaryCell.sinr != -999) "${primaryCell.sinr}" else "--", iOSGreen)
                            MetricBadge("RSRQ", "${primaryCell.rsrq}", iOSOrange)
                            MetricBadge("RSSI", if (primaryCell.rssi != -1) "${primaryCell.rssi}" else "--", iOSLabel)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PCI ${primaryCell.pci}", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
                            Text("ARFCN ${primaryCell.earfcn}", style = MaterialTheme.typography.bodySmall, color = iOSLabel, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // SCC1 辅载波区域（仅当存在第2个服务小区时显示）
                if (servingCells.size >= 2) {
                    val sccCell = servingCells[1]

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = iOSFill,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = iOSRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        "SCC1",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = iOSRed
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    sccCell.band.ifEmpty { "未知" },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = iOSLabel
                                )
                                if (sccCell.bandwidth.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        sccCell.bandwidth,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = iOSSecondaryLabel
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("PCI ${sccCell.pci}", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
                                Text("ARFCN ${sccCell.earfcn}", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            // 底部提示说明
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = iOSSeparator, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "需开启 GPS 方可读取详细频段与 PCI",
                    style = MaterialTheme.typography.labelSmall,
                    color = iOSSecondaryLabel,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Text(
                    text = "NR 模式下 SINR 由 RSRQ 推算，RSSI 由 RSRP + 带宽修正，非基带原始值",
                    style = MaterialTheme.typography.labelSmall,
                    color = iOSSecondaryLabel.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }

    if (showCaHelpDialog) {
        AlertDialog(
            onDismissRequest = { showCaHelpDialog = false },
            title = { Text("载波聚合参数说明", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• PCC (Primary Component Carrier)：主载波。负责与基站进行主要通信并传输数据，相当于“主干道”。", style = MaterialTheme.typography.bodyMedium)
                    Text("• SCC (Secondary Component Carrier)：辅载波。例如 SCC1 是“第一个辅载波”，为了提速额外开辟的“辅助车道”，专门帮忙传数据、增加带宽。", style = MaterialTheme.typography.bodyMedium)
                    Text("• 频段 (Band)：例如 N28（700MHz）。低频段覆盖极广、穿墙强，能保证偏僻处也有稳定 5G 信号；高频段则网速极快但覆盖较小。", style = MaterialTheme.typography.bodyMedium)
                    Text("• 频宽 (Bandwidth)：例如 30MHz。代表“车道”的宽度，频宽越大，同时能传输的数据就越多。", style = MaterialTheme.typography.bodyMedium)
                    Text("• PCI (Physical Cell Identity)：物理小区标识。基站由多个天线扇区组成，PCI（如 423）即当前为你提供该信号的“具体天线编号”。", style = MaterialTheme.typography.bodyMedium)
                    Text("• ARFCN (绝对无线频率信道号)：比频段更精确。它代表了当前连接频段中最精准的“中心频率值”。", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showCaHelpDialog = false }) {
                    Text("我知道了", color = iOSBlue)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = iOSCardBackground,
            titleContentColor = iOSLabel,
            textContentColor = iOSSecondaryLabel
        )
    }
}

@Composable
fun MetricBadge(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, maxLines = 1, softWrap = false)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
    }
}

// ================= 6. 流量详情与运行时间卡片 =================
@Composable
@kotlin.OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun TrafficAndUptimeCard(trafficMetrics: TrafficMetrics, systemMetrics: SystemMetrics) {
    var showTrafficDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("pixel_toolbox_prefs", android.content.Context.MODE_PRIVATE) }

    var customTotalInput by remember { mutableStateOf("") }
    var customUsedInput by remember { mutableStateOf("") }

    // P2: 短信回执识别输入与解析候选（原始片段, 换算 GB）
    var smsReceiptInput by remember { mutableStateOf("") }
    var smsParsedCandidates by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }

    // P2: 套餐总数/已用缓存（mutableState 驱动，保存/自动识别后立即刷新详情）
    var savedTotalGb by remember { mutableStateOf(prefs.getFloat("custom_total_traffic_quota_gb", 200f)) }
    var savedUsedGb by remember { mutableStateOf(prefs.getFloat("custom_used_traffic_gb", -1f)) }

    // P2: 弹窗折叠区状态（运营商查询区 / 短信回执识别区，默认收起）
    var carrierSectionExpanded by remember { mutableStateOf(false) }
    var receiptSectionExpanded by remember { mutableStateOf(false) }

    // P2: 短信读取权限与自动回执识别状态
    var smsPermissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var autoDetectStatus by remember { mutableStateOf<String?>(null) }
    val smsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsPermissionGranted = granted
        autoDetectStatus = if (granted) {
            "已获得短信读取权限：发送\"流量\"后回执将自动识别填入"
        } else {
            "未授予短信读取权限，可改用下方手动粘贴识别框保底"
        }
    }

    // 自动回执监听：弹窗打开且已有读取权限时注册，弹窗关闭/权限回收时注销
    if (showTrafficDialog && smsPermissionGranted) {
        DisposableEffect(Unit) {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            var pendingRunnable: Runnable? = null
            var lastProcessedId = -1L
            val observer = object : android.database.ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    pendingRunnable?.let { handler.removeCallbacks(it) }
                    val r = Runnable {
                        try {
                            val uri = android.net.Uri.parse("content://sms/inbox")
                            context.contentResolver.query(
                                uri,
                                arrayOf("_id", "address", "body"),
                                "address IN ('10086','10010','10001','10099')",
                                null,
                                "_id DESC"
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val id = cursor.getLong(0)
                                    if (id != lastProcessedId) {
                                        lastProcessedId = id
                                        val body = cursor.getString(2) ?: ""
                                        val usedMatch = Regex("已用.*?(\\d+(?:\\.\\d+)?)\\s*(gb|mb|g|m)(?![a-zA-Z0-9])", RegexOption.IGNORE_CASE).find(body)
                                        val remainMatch = Regex("(?:剩余|余量).*?(\\d+(?:\\.\\d+)?)\\s*(gb|mb|g|m)(?![a-zA-Z0-9])", RegexOption.IGNORE_CASE).find(body)
                                        
                                        var detectedUsed: Float? = null
                                        var autoDetectMsg = ""
                                        
                                        if (usedMatch != null) {
                                            val v = usedMatch.groupValues[1].toFloat()
                                            val u = usedMatch.groupValues[2].lowercase()
                                            detectedUsed = when (u) {
                                                "gb", "g" -> v
                                                "mb", "m" -> v / 1024f
                                                else -> v
                                            }
                                            autoDetectMsg = "已自动识别并填入已用流量 ${formatGb(detectedUsed)} GB"
                                        } else if (remainMatch != null) {
                                            val v = remainMatch.groupValues[1].toFloat()
                                            val u = remainMatch.groupValues[2].lowercase()
                                            val remain = when (u) {
                                                "gb", "g" -> v
                                                "mb", "m" -> v / 1024f
                                                else -> v
                                            }
                                            val hasTotal = prefs.contains("custom_total_traffic_quota_gb")
                                            val total = prefs.getFloat("custom_total_traffic_quota_gb", 200f)
                                            if (hasTotal && total > 0) {
                                                detectedUsed = (total - remain).coerceAtLeast(0f)
                                                autoDetectMsg = "已自动识别剩余流量 ${formatGb(remain)} GB，并填入已用流量 ${formatGb(detectedUsed)} GB"
                                            } else {
                                                autoDetectStatus = "已自动识别剩余流量 ${formatGb(remain)} GB，但未设置套餐总数，请先手动校准"
                                            }
                                        } else {
                                            val candidates = parseTrafficCandidates(body)
                                            if (candidates.size == 1) {
                                                val (raw, gb) = candidates.first()
                                                val hasTotal = prefs.contains("custom_total_traffic_quota_gb")
                                                val total = prefs.getFloat("custom_total_traffic_quota_gb", 200f)
                                                if (hasTotal && total > 0) {
                                                    detectedUsed = (total - gb).coerceAtLeast(0f)
                                                    autoDetectMsg = "已自动识别并填入剩余流量 ${formatGb(gb)} GB（原始：$raw）"
                                                } else {
                                                    autoDetectStatus = "已自动识别剩余流量 ${formatGb(gb)} GB，但未设置套餐总数，请先手动校准套餐总数"
                                                }
                                            }
                                        }
                                        
                                        if (detectedUsed != null) {
                                            prefs.edit().putFloat("custom_used_traffic_gb", detectedUsed).apply()
                                            savedUsedGb = detectedUsed
                                            autoDetectStatus = autoDetectMsg
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略回执查询异常
                        }
                    }
                    pendingRunnable = r
                    handler.postDelayed(r, 500)
                }
            }
            context.contentResolver.registerContentObserver(
                android.net.Uri.parse("content://sms/inbox"), true, observer
            )
            onDispose {
                pendingRunnable?.let { handler.removeCallbacks(it) }
                context.contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    // P2: 派生值 + String.format 用 remember 缓存，避免每次重组重复解析/格式化
    val currentUsedGb = remember(trafficMetrics.monthTotalTraffic, savedUsedGb) {
        if (savedUsedGb >= 0) {
            savedUsedGb
        } else {
            val raw = trafficMetrics.monthTotalTraffic
            val numStr = raw.replace(" GB", "").replace(" MB", "").replace(" KB", "").replace(" B", "").trim()
            val value = numStr.toFloatOrNull() ?: 0f
            when {
                raw.contains(" GB") -> value
                raw.contains(" MB") -> value / 1024f
                raw.contains(" KB") -> value / (1024f * 1024f)
                else -> 0f
            }
        }
    }
    val currentTotalGb = remember(savedTotalGb) { savedTotalGb.coerceAtLeast(1f) }
    val currentRemainingGb = remember(currentTotalGb, currentUsedGb) { (currentTotalGb - currentUsedGb).coerceAtLeast(0f) }
    val usagePercent = remember(currentUsedGb, currentTotalGb) { 
        if (currentTotalGb > 0) (currentUsedGb / currentTotalGb).coerceIn(0f, 1f) else 0f 
    }
    val usedText = remember(currentUsedGb) { String.format(Locale.US, "%.2f", currentUsedGb) }
    val totalText = remember(currentTotalGb) { String.format(Locale.US, "%.2f GB", currentTotalGb) }
    val remainingText = remember(currentRemainingGb) { String.format(Locale.US, "%.2f GB", currentRemainingGb) }
    val percentText = remember(usagePercent) { String.format(Locale.US, "已用 %.1f%%", usagePercent * 100f) }

    if (showTrafficDialog) {
        // P2: 打开弹窗时从 prefs 回填手动校准输入框（保证再次打开有回显、保存时输入框必有值）
        LaunchedEffect(showTrafficDialog) {
            val usedPref = prefs.getFloat("custom_used_traffic_gb", -1f)
            customUsedInput = if (usedPref >= 0) String.format(Locale.US, "%.2f", usedPref) else ""
            val totalPref = prefs.getFloat("custom_total_traffic_quota_gb", 200f)
            customTotalInput = String.format(Locale.US, "%.2f", totalPref)
        }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val localView = LocalView.current
        val dialogMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
        // 从 LocalContext 向上解包找到宿主 Activity（WindowCompat 需要 Activity.window）
        val hostActivity = remember {
            var c: android.content.Context? = context
            var act: android.app.Activity? = null
            while (c is android.content.ContextWrapper) {
                if (c is android.app.Activity) { act = c; break }
                c = c.baseContext
            }
            act
        }
        val hideKeyboard = {
            focusManager.clearFocus()
            keyboardController?.hide()
            // 延迟 150ms 执行：等待焦点切换/输入法动画完成后再强制收回
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val act = hostActivity
                    if (act != null) {
                        // 最强方案：WindowCompat 通过 InsetsController 直接隐藏 IME
                        androidx.core.view.WindowCompat.getInsetsController(act.window, act.window.decorView)
                            .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
                    }
                } catch (e: Exception) {
                    // 忽略 IME 收起异常
                }
                try {
                    // 兜底：InputMethodManager 直接隐藏输入法窗口
                    val act = hostActivity
                    val token = act?.window?.decorView?.windowToken ?: localView.windowToken
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(token, 0)
                } catch (e: Exception) {
                    // 忽略 IME 收起异常
                }
            }, 150)
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showTrafficDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(decorFitsSystemWindows = false, usePlatformDefaultWidth = false)
        ) {
            // 让 Dialog 窗口接收系统 Insets（不拦截键盘），配合 imePadding 保证键盘弹起时可滚动查看全部内容
            val dialogWindowProvider = LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider
            SideEffect {
                dialogWindowProvider?.window?.setDecorFitsSystemWindows(false)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = dialogMaxHeight)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .pointerInput(Unit) { detectTapGestures { hideKeyboard() } }
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("流量查询与套餐校准", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
                        IconButton(onClick = { showTrafficDialog = false }) {
                            Text("✕", style = MaterialTheme.typography.titleMedium, color = iOSSecondaryLabel)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 折叠区 1：运营商短信/电话查询（默认收起）
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = iOSFill.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { carrierSectionExpanded = !carrierSectionExpanded }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("运营商短信/电话查询", style = MaterialTheme.typography.labelLarge, color = iOSLabel)
                                Text(if (carrierSectionExpanded) "点击收起" else "发送\"流量\"查询余量 / 电话查询", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
                            }
                            Text(if (carrierSectionExpanded) "▾" else "▸", style = MaterialTheme.typography.titleMedium, color = iOSBlue)
                        }
                    }
                    if (carrierSectionExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("一键发送短信查询余量", style = MaterialTheme.typography.labelLarge, color = iOSLabel)
                        Text("发送汉字\"流量\"查询，全国/携号转网通用", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
                        if (!smsPermissionGranted) {
                            Text("未授予短信读取权限：点击发送后将请求授权以自动识别回执；拒绝后可用下方手动粘贴识别框保底", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                    // 四大运营商：仅保留汉字"流量"查询指令
                    val smsCarriers = listOf(
                        "中国移动 (CMCC)" to "10086",
                        "中国联通 (CUCC)" to "10010",
                        "中国电信 (CTCC)" to "10001",
                        "中国广电 (CBN)" to "10099"
                    )
                    smsCarriers.forEach { (carrier, number) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = iOSFill,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(carrier, style = MaterialTheme.typography.bodyMedium, color = iOSLabel)
                                Text("客服 $number", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = iOSBlue.copy(alpha = 0.12f),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (!smsPermissionGranted) {
                                                autoDetectStatus = "需要短信读取权限才能自动识别回执，正在请求授权…"
                                                smsPermissionLauncher.launch(android.Manifest.permission.READ_SMS)
                                            } else {
                                                autoDetectStatus = "已打开短信发送界面，发送\"流量\"后回执将自动识别填入"
                                            }
                                            try {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("smsto:$number")
                                                ).apply {
                                                    putExtra("sms_body", "流量")
                                                }
                                                context.startActivity(intent)
                                                // 保持弹窗打开，等待回执自动识别
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "打开短信失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("发送 \"流量\" 📩", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = iOSBlue)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF34C759).copy(alpha = 0.12f),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_DIAL,
                                                    android.net.Uri.parse("tel:$number")
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "打开拨号失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("电话查询 ☎", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = Color(0xFF34C759))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("短信不行可电话查询", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
                            }
                        }
                    }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 自动识别状态（回执到达/权限状态实时可见）
                    if (autoDetectStatus != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = iOSBlue.copy(alpha = 0.10f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                autoDetectStatus!!,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = iOSBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 折叠区 2：短信回执识别（默认收起，手动粘贴回执解析剩余流量）
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = iOSFill.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { receiptSectionExpanded = !receiptSectionExpanded }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("短信回执识别", style = MaterialTheme.typography.labelLarge, color = iOSLabel)
                                Text(if (receiptSectionExpanded) "点击收起" else "手动粘贴回执 / 识别剩余流量", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
                            }
                            Text(if (receiptSectionExpanded) "▾" else "▸", style = MaterialTheme.typography.titleMedium, color = iOSBlue)
                        }
                    }
                    if (receiptSectionExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("发送\"流量\"后，将运营商回复粘贴到下方，点击识别剩余流量", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel)
                        Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = smsReceiptInput,
                        onValueChange = { smsReceiptInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        label = { Text("粘贴回执短信内容", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("如：剩余流量12.5GB，通用10GB，定向2.5GB", style = MaterialTheme.typography.labelSmall) },
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { hideKeyboard() })
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    iOSButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val candidates = parseTrafficCandidates(smsReceiptInput.trim())
                            smsParsedCandidates = candidates
                            if (candidates.isEmpty()) {
                                android.widget.Toast.makeText(context, "未识别到流量数值，请检查粘贴内容", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("识别剩余流量 🔍", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }

                    if (smsParsedCandidates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("识别结果（点击应用为剩余流量）", style = MaterialTheme.typography.bodySmall, color = iOSLabel)
                        Spacer(modifier = Modifier.height(6.dp))
                        smsParsedCandidates.forEach { (raw, gb) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF34C759).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                onClick = {
                                    val hasTotal = prefs.contains("custom_total_traffic_quota_gb")
                                    val total = prefs.getFloat("custom_total_traffic_quota_gb", 200f)
                                    if (hasTotal && total > 0) {
                                        val used = (total - gb).coerceAtLeast(0f)
                                        prefs.edit().putFloat("custom_used_traffic_gb", used).apply()
                                        savedUsedGb = used
                                        android.widget.Toast.makeText(
                                            context,
                                            "已更新：剩余 ${String.format(Locale.US, "%.2f", gb)} GB，已用 ${String.format(Locale.US, "%.2f", used)} GB（总数 ${String.format(Locale.US, "%.2f", total)} GB）",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        showTrafficDialog = false
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "已识别剩余流量 ${String.format(Locale.US, "%.2f", gb)} GB，但未设置套餐总数，请先手动校准套餐总数",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(raw, style = MaterialTheme.typography.bodySmall, color = iOSLabel, modifier = Modifier.weight(1f))
                                    Text("≈ ${String.format(Locale.US, "%.2f", gb)} GB", style = MaterialTheme.typography.bodySmall, color = Color(0xFF34C759))
                                }
                            }
                        }
                    }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = iOSSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("手动校准套餐 (已用与总数 GB)", style = MaterialTheme.typography.labelLarge, color = iOSLabel)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customUsedInput,
                            onValueChange = { customUsedInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("已用流量 (GB)", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text(String.format(Locale.US, "%.2f", currentUsedGb)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { hideKeyboard() })
                        )
                        OutlinedTextField(
                            value = customTotalInput,
                            onValueChange = { customTotalInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("套餐总数 (GB)", style = MaterialTheme.typography.labelSmall) },
                            placeholder = { Text(String.format(Locale.US, "%.0f", currentTotalGb)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { hideKeyboard() })
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    iOSButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val total = customTotalInput.trim().toFloatOrNull()
                            var used = customUsedInput.trim().toFloatOrNull()

                            // 如果用户没填手动已用量，但粘贴了短信回执，尝试自动识别
                            if (used == null && smsReceiptInput.isNotBlank()) {
                                val text = smsReceiptInput.trim()
                                val usedMatch = Regex("已用.*?(\\d+(?:\\.\\d+)?)\\s*(gb|mb|g|m)(?![a-zA-Z0-9])", RegexOption.IGNORE_CASE).find(text)
                                val remainMatch = Regex("(?:剩余|余量).*?(\\d+(?:\\.\\d+)?)\\s*(gb|mb|g|m)(?![a-zA-Z0-9])", RegexOption.IGNORE_CASE).find(text)
                                
                                if (usedMatch != null) {
                                    val v = usedMatch.groupValues[1].toFloat()
                                    val u = usedMatch.groupValues[2].lowercase()
                                    used = when (u) {
                                        "gb", "g" -> v
                                        "mb", "m" -> v / 1024f
                                        else -> v
                                    }
                                } else if (remainMatch != null) {
                                    val v = remainMatch.groupValues[1].toFloat()
                                    val u = remainMatch.groupValues[2].lowercase()
                                    val remain = when (u) {
                                        "gb", "g" -> v
                                        "mb", "m" -> v / 1024f
                                        else -> v
                                    }
                                    val validTotal = total ?: prefs.getFloat("custom_total_traffic_quota_gb", 200f)
                                    if (validTotal > 0) {
                                        used = (validTotal - remain).coerceAtLeast(0f)
                                    }
                                } else {
                                    val candidates = parseTrafficCandidates(text)
                                    if (candidates.size == 1) {
                                        val gb = candidates.first().second
                                        val validTotal = total ?: prefs.getFloat("custom_total_traffic_quota_gb", 200f)
                                        if (validTotal > 0) {
                                            used = (validTotal - gb).coerceAtLeast(0f)
                                        }
                                    }
                                }
                            }

                            val validTotal = total != null && total > 0
                            val validUsed = used != null && used >= 0
                            if (!validTotal && !validUsed) {
                                android.widget.Toast.makeText(context, "请输入有效的已用/总数数值或粘贴短信回执后保存", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val editor = prefs.edit()
                                if (validTotal) {
                                    editor.putFloat("custom_total_traffic_quota_gb", total!!)
                                }
                                if (validUsed) {
                                    editor.putFloat("custom_used_traffic_gb", used!!)
                                }
                                editor.apply()
                                savedTotalGb = prefs.getFloat("custom_total_traffic_quota_gb", 200f)
                                savedUsedGb = prefs.getFloat("custom_used_traffic_gb", -1f)
                                android.widget.Toast.makeText(context, "套餐数据已成功更新！", android.widget.Toast.LENGTH_SHORT).show()
                                showTrafficDialog = false
                            }
                        }
                    ) {
                        Text("保存流量配置 💾", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showTrafficDialog = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PieChart,
                        contentDescription = "流量详情",
                        tint = iOSGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("流量详情", style = MaterialTheme.typography.titleMedium, color = iOSLabel, maxLines = 1, softWrap = false)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("(点击查询/校准 ⚙️)", style = MaterialTheme.typography.labelSmall, color = iOSBlue, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }

                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.DataUsage,
                        contentDescription = "今日",
                        tint = iOSBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("今日已用 ", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
                    Text(trafficMetrics.todayTraffic, style = MaterialTheme.typography.bodySmall, color = iOSBlue, maxLines = 1, softWrap = false)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧大字已用流量
                Column {
                    Text("本月已用", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = usedText,
                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
                            color = iOSLabel
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GB",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = iOSSecondaryLabel,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // 右侧已用 / 总数 / 剩余
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("总数: ", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
                        Text(totalText, style = MaterialTheme.typography.bodySmall, color = iOSLabel)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("剩余: ", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
                        Text(remainingText, style = MaterialTheme.typography.bodySmall, color = if (usagePercent >= 1f) iOSRed else iOSGreen)
                    }
                    val usageColor = if (usagePercent >= 1f) iOSRed else if (usagePercent > 0.8f) iOSOrange else iOSBlue
                    Text(percentText, style = MaterialTheme.typography.labelSmall, color = usageColor)
                }
            }

            // 用量进度条（校准配额）
            Spacer(modifier = Modifier.height(8.dp))
            val isAlmostUsedUp = usagePercent >= 0.9f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isAlmostUsedUp) iOSSeparator else iOSBlue) // 未使用部分（底色）
            ) {
                if (usagePercent > 0f) {
                    val barColor = if (isAlmostUsedUp) iOSRed else iOSGreen // 已用部分
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(usagePercent.coerceIn(0f, 1f))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 下载 / 上传 拆分（真实 NetworkStatsManager rx/tx 分项）
            val hasTraffic = !trafficMetrics.monthDlTraffic.startsWith("0 ") && !trafficMetrics.monthUlTraffic.startsWith("0 ")
            if (hasTraffic) {
                val dlPercent = trafficMetrics.monthDlPercent.coerceIn(0f, 1f)
                val ulPercent = (1f - dlPercent).coerceIn(0f, 1f)
                val dlText = "下载 ${trafficMetrics.monthDlTraffic}（${(dlPercent * 100).toInt()}%）"
                val ulText = "上传 ${trafficMetrics.monthUlTraffic}（${(ulPercent * 100).toInt()}%）"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(dlText, style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
                    Text(ulText, style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 蓝橙双色下载/上传进度条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(iOSSeparator)
                ) {
                    if (dlPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(dlPercent)
                                .background(iOSBlue)
                        )
                    }
                    if (ulPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(ulPercent)
                                .background(iOSOrange)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = iOSSeparator, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // WiFi 用量（NetworkStatsManager 真实统计）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = "WiFi",
                        tint = iOSSecondaryLabel,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WiFi 本月", style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel)
                }
                Text(
                    text = trafficMetrics.wifiMonthTotalTraffic,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = iOSLabel
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("下载 ${trafficMetrics.wifiMonthDlTraffic} · 上传 ${trafficMetrics.wifiMonthUlTraffic}", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
                Text("今日 ${trafficMetrics.wifiTodayTraffic}", style = MaterialTheme.typography.labelSmall, color = iOSSecondaryLabel, maxLines = 1, softWrap = false)
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 运行时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("运行时间", style = MaterialTheme.typography.bodyMedium, color = iOSSecondaryLabel)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "计时",
                        tint = iOSSecondaryLabel,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = systemMetrics.uptimeText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = iOSLabel,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// 流量回执解析：提取全部"数值+单位"候选并换算为 GB
private fun parseTrafficCandidates(text: String): List<Pair<String, Float>> {
    val regex = Regex("(?i)(\\d+(?:\\.\\d+)?)\\s*(gb|mb|kb|g|m|k)(?![a-zA-Z0-9])")
    return regex.findAll(text).map { m ->
        val v = m.groupValues[1].toFloat()
        val u = m.groupValues[2].lowercase()
        val gb = when (u) {
            "gb", "g" -> v
            "mb", "m" -> v / 1024f
            else -> v / (1024f * 1024f)
        }
        m.value.trim() to gb
    }.distinctBy { it.first to it.second }.toList()
}

private fun formatGb(v: Float): String = String.format(Locale.US, "%.2f", v)
