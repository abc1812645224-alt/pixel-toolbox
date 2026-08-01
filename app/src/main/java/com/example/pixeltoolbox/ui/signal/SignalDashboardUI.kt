package com.example.pixeltoolbox.ui.signal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.pixeltoolbox.shizuku.SimSlotInfo
import com.example.pixeltoolbox.signal.SignalDashboardState
import com.example.pixeltoolbox.signal.SignalInfo
import com.example.pixeltoolbox.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalDashboard(
    state: SignalDashboardState,
    simSlots: List<SimSlotInfo> = emptyList(),
    selectedSubId: Int = -1,
    onSelectSubId: (Int) -> Unit = {},
    addLog: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 顶栏：运营商与网络模式卡片
        CarrierHeaderCard(state)

        // 1.5 SIM 卡选择 (仅当有多张卡时显示)
        if (simSlots.size > 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                        fontSize = 13.sp,
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

        // ================= 新增：5G / IMS 注入控制台 =================
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
        SubscriptionSpeedCard(state)

        // 4. 设备性能与 RSRP 信号强度环形仪表卡片
        DeviceAndSignalGaugeCard(state)

        // 5. 载波聚合 2CC/多载波卡片
        CarrierAggregationCard(state)

        // 6. 流量详情与系统运行时间卡片
        TrafficAndUptimeCard(state)

        // 7. 数据更新时间
        if (state.lastUpdateTime != "--") {
            Text(
                text = "更新时间：${state.lastUpdateTime}",
                fontSize = 11.sp,
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


    Column(modifier = Modifier.fillMaxWidth()) {
        Text("5G & IMS 通信底层注入", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iOSLabel)
        Spacer(modifier = Modifier.height(12.dp))

        // Switch 1: Basic
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("VoLTE 高清通话", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iOSLabel)
                Text("4G 高清语音 + 视频通话，LTE 显示为 4G", fontSize = 11.sp, color = iOSSecondaryLabel)
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
                Text("5G NR (SA+NSA)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iOSLabel)
                Text("独立 5G 组网/非组网 + VoNR 5G 语音 + 跨SIM通话", fontSize = 11.sp, color = iOSSecondaryLabel)
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
                Text("5G 信号显示优化", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iOSLabel)
                Text("5G+/5GA 图标 + 信号格数阈值增强", fontSize = 11.sp, color = iOSSecondaryLabel)
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
                Text("VoWiFi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iOSLabel)
                Text("WiFi 通话（视运营商支持）", fontSize = 11.sp, color = iOSSecondaryLabel)
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
            fontSize = 11.sp,
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
            Text("一键注入以上配置", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ================= 1. 运营商卡片 =================
@Composable
fun CarrierHeaderCard(state: SignalDashboardState) {
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
                    text = state.carrierName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
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
                        text = state.networkMode,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOSBlue
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF2F4F7)
                ) {
                    Text(
                        text = state.aggregatedBands,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF667085)
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
fun SubscriptionSpeedCard(state: SignalDashboardState) {
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
                        text = state.subscriptionDownlink.replace(" Mbps", ""),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOSLabel
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mbps", fontSize = 12.sp, color = iOSSecondaryLabel, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("签约下行", fontSize = 12.sp, color = iOSSecondaryLabel)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.subscriptionUplink.replace(" Mbps", ""),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOSLabel
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mbps", fontSize = 12.sp, color = iOSSecondaryLabel, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("签约上行", fontSize = 12.sp, color = iOSSecondaryLabel)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.qci,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("QCI", fontSize = 12.sp, color = iOSSecondaryLabel)
            }
        }

        // 说明文字
        Text(
            text = "签约速率为网络侧分配的最大带宽上限，非实时测速。QCI 为承载优先级，8/9 为默认数据承载，数值越小优先级越高。",
            fontSize = 10.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

// ================= 4. 设备性能与 RSRP 信号强度仪表盘 =================
@Composable
fun DeviceAndSignalGaugeCard(state: SignalDashboardState) {
    val primaryCell = state.servingCells.firstOrNull()
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
                        text = state.deviceModel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC69250) // 金棕/棕橙高亮
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    DeviceInfoRow("设备型号", if (state.deviceModel == "Google Pixel") "LG6151M" else state.deviceModel)
                    DeviceInfoRow("系统版本", state.firmwareVersion)
                    DeviceInfoRow("CPU使用率", if (state.cpuUsage >= 0) "${state.cpuUsage.toInt()}%" else "--")
                    DeviceInfoRow("内存使用率", if (state.ramUsage >= 0) "${String.format("%.2f", state.ramUsage)}%" else "--")
                }

                // 右侧：自定义 Gauge 动态变色仪表盘
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("信号强度", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = iOSLabel)
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(110.dp)
                    ) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            val strokeWidth = 10.dp.toPx()
                            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                            // 绘制底色灰弧
                            drawArc(
                                color = Color(0xFFEAECF0),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

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
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSLabel
                            )
                            Text(
                                text = "dBm",
                                fontSize = 11.sp,
                                color = iOSSecondaryLabel
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF2F4F7), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 底部 4 级信号标准分级颜色图例注释 (全宽独立展示，防止遮挡或被压缩)
            Text("信号等级说明", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = iOSSecondaryLabel)
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
        color = Color(0xFFF8F9FA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAECF0))
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
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = iOSLabel)
            Spacer(modifier = Modifier.width(2.dp))
            Text(range, fontSize = 9.sp, color = iOSSecondaryLabel)
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
            fontSize = 12.sp,
            color = iOSSecondaryLabel,
            modifier = Modifier.width(82.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSLabel,
            modifier = Modifier.weight(1f)
        )
    }
}

// ================= 5. 载波聚合卡片 =================
@Composable
fun CarrierAggregationCard(state: SignalDashboardState) {
    val servingCells = state.servingCells
    val caStateText = state.caStateText

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
                    Text("载波聚合", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = iOSLabel)
                }

                Text(
                    text = caStateText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSSecondaryLabel
                )
            }

            if (servingCells.isEmpty() || caStateText == "--") {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "暂无载波聚合数据",
                    fontSize = 14.sp,
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
                    color = Color(0xFFF9FAFB),
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
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
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
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
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
                            Text("PCI ${primaryCell.pci}", fontSize = 12.sp, color = iOSSecondaryLabel, fontWeight = FontWeight.Medium)
                            Text("ARFCN ${primaryCell.earfcn}", fontSize = 12.sp, color = iOSLabel, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SCC1 辅载波区域（仅当存在第2个服务小区时显示）
                if (servingCells.size >= 2) {
                    val sccCell = servingCells[1]

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF9FAFB),
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
                                    color = Color(0xFFFEF3F2)
                                ) {
                                    Text(
                                        "SCC1",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD92D20)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    sccCell.band.ifEmpty { "未知" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iOSLabel
                                )
                                if (sccCell.bandwidth.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        sccCell.bandwidth,
                                        fontSize = 12.sp,
                                        color = iOSSecondaryLabel
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("PCI ${sccCell.pci}", fontSize = 12.sp, color = iOSSecondaryLabel)
                                Text("ARFCN ${sccCell.earfcn}", fontSize = 12.sp, color = iOSSecondaryLabel)
                            }
                        }
                    }
                }
            }

            // 底部提示说明
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF2F4F7), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "需开启 GPS 方可读取详细频段与 PCI",
                    fontSize = 11.sp,
                    color = iOSSecondaryLabel,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Text(
                    text = "NR 模式下 SINR 由 RSRQ 推算，RSSI 由 RSRP + 带宽修正，非基带原始值",
                    fontSize = 10.sp,
                    color = iOSSecondaryLabel.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MetricBadge(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = iOSSecondaryLabel)
    }
}

// ================= 6. 流量详情与运行时间卡片 =================
@Composable
fun TrafficAndUptimeCard(state: SignalDashboardState) {
    var showTrafficDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("pixel_toolbox_prefs", android.content.Context.MODE_PRIVATE) }

    var customTotalInput by remember { mutableStateOf("") }
    var customUsedInput by remember { mutableStateOf("") }

    val savedTotalGb = prefs.getFloat("custom_total_traffic_quota_gb", 200f)
    val savedUsedGb = prefs.getFloat("custom_used_traffic_gb", -1f)

    val currentUsedGb = if (savedUsedGb >= 0) savedUsedGb else (state.monthTotalTraffic.replace(" GB", "").replace(" MB", "").toFloatOrNull() ?: 0f)
    val currentTotalGb = savedTotalGb.coerceAtLeast(1f)
    val currentRemainingGb = (currentTotalGb - currentUsedGb).coerceAtLeast(0f)
    val usagePercent = (currentUsedGb / currentTotalGb).coerceIn(0.01f, 1f)

    if (showTrafficDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showTrafficDialog = false }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("流量查询与套餐校准", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = iOSLabel)
                        IconButton(onClick = { showTrafficDialog = false }) {
                            Text("✕", fontSize = 16.sp, color = iOSSecondaryLabel, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("一键发送短信查询余量", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = iOSLabel)
                    Text("自动识别运营商并准备查询短信，一键发送：", fontSize = 11.sp, color = iOSSecondaryLabel)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 四大运营商短信查询项
                    listOf(
                        Triple("中国移动 (CMCC)", "10086", "CXLL"),
                        Triple("中国联通 (CUCC)", "10010", "1071"),
                        Triple("中国电信 (CTCC)", "10001", "108"),
                        Triple("中国广电 (CBN)", "10099", "CXLL")
                    ).forEach { (carrier, number, code) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF2F4F7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            onClick = {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("smsto:$number")
                                    ).apply {
                                        putExtra("sms_body", code)
                                    }
                                    context.startActivity(intent)
                                    showTrafficDialog = false
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "打开短信失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(carrier, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = iOSLabel)
                                    Text("发送 \"$code\" 到 $number", fontSize = 11.sp, color = iOSSecondaryLabel)
                                }
                                Text("发送 📩", fontSize = 12.sp, color = iOSBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("手动校准套餐 (已用与总数 GB)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = iOSLabel)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customUsedInput,
                            onValueChange = { customUsedInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("已用流量 (GB)", fontSize = 11.sp) },
                            placeholder = { Text(String.format(Locale.US, "%.2f", currentUsedGb)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customTotalInput,
                            onValueChange = { customTotalInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("套餐总数 (GB)", fontSize = 11.sp) },
                            placeholder = { Text(String.format(Locale.US, "%.0f", currentTotalGb)) },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    iOSButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val total = customTotalInput.toFloatOrNull()
                            val used = customUsedInput.toFloatOrNull()
                            val editor = prefs.edit()
                            if (total != null && total > 0) {
                                editor.putFloat("custom_total_traffic_quota_gb", total)
                            }
                            if (used != null && used >= 0) {
                                editor.putFloat("custom_used_traffic_gb", used)
                            }
                            editor.apply()
                            android.widget.Toast.makeText(context, "套餐数据已成功更新！", android.widget.Toast.LENGTH_SHORT).show()
                            showTrafficDialog = false
                        }
                    ) {
                        Text("保存流量配置 💾", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PieChart,
                        contentDescription = "流量详情",
                        tint = iOSGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("流量详情", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = iOSLabel)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("(点击查询/校准 ⚙️)", fontSize = 11.sp, color = iOSBlue)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今日已用 ", fontSize = 12.sp, color = iOSSecondaryLabel)
                    Text(state.todayTraffic, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
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
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.2f", currentUsedGb),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = iOSLabel
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GB",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = iOSLabel,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text("本月已用", fontSize = 11.sp, color = iOSSecondaryLabel)
                }

                // 右侧已用 / 总数 / 剩余
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("总数: ", fontSize = 12.sp, color = iOSSecondaryLabel)
                        Text(String.format(Locale.US, "%.2f GB", currentTotalGb), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iOSLabel)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("剩余: ", fontSize = 12.sp, color = iOSSecondaryLabel)
                        Text(String.format(Locale.US, "%.2f GB", currentRemainingGb), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iOSGreen)
                    }
                    Text(String.format(Locale.US, "已用 %.1f%%", usagePercent * 100f), fontSize = 11.sp, color = iOSBlue, fontWeight = FontWeight.SemiBold)
                }
            }

            // 用量进度条（校准配额）
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                if (usagePercent > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(usagePercent.coerceIn(0f, 1f))
                            .background(iOSGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 下载 / 上传 拆分（比例取自系统，绝对值按校准用量分配）
            val dlPercent = state.monthDlPercent.coerceIn(0f, 1f)
            val ulPercent = 1f - dlPercent
            val calDlGb = currentUsedGb * dlPercent
            val calUlGb = currentUsedGb * ulPercent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("下载 ${String.format(Locale.US, "%.2f GB", calDlGb)}（${(dlPercent * 100).toInt()}%）", fontSize = 11.sp, color = iOSSecondaryLabel)
                Text("上传 ${String.format(Locale.US, "%.2f GB", calUlGb)}（${(ulPercent * 100).toInt()}%）", fontSize = 11.sp, color = iOSSecondaryLabel)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 蓝橙双色下载/上传进度条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE5E7EB))
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

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFF2F4F7), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 运行时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("运行时间", fontSize = 13.sp, color = iOSSecondaryLabel)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "计时",
                        tint = iOSSecondaryLabel,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.uptimeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOSLabel
                    )
                }
            }
        }
    }
}
