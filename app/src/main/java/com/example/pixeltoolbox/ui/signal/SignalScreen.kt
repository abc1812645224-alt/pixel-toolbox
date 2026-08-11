package com.example.pixeltoolbox.ui.signal

import java.io.File
import java.io.FileOutputStream
import com.example.pixeltoolbox.ShizukuAuthCard
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.core.app.ActivityCompat
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import com.example.pixeltoolbox.shizuku.SimSlotInfo
import com.example.pixeltoolbox.ui.theme.GlassCard
import com.example.pixeltoolbox.ui.theme.iOSButton
import com.example.pixeltoolbox.ui.theme.iOSOutlineButton
import com.example.pixeltoolbox.ui.theme.iOSBackground
import com.example.pixeltoolbox.ui.theme.iOSLabel
import com.example.pixeltoolbox.ui.theme.iOSSecondaryLabel
import com.example.pixeltoolbox.ui.theme.iOSNavUnselected
import com.example.pixeltoolbox.ui.theme.iOSBlue
import com.example.pixeltoolbox.ui.theme.iOSGreen
import com.example.pixeltoolbox.ui.theme.iOSRed
import com.example.pixeltoolbox.ui.theme.iOSCardBackground
import com.example.pixeltoolbox.ui.theme.iOSSeparator
import com.example.pixeltoolbox.ui.theme.PixelToolboxTheme
import com.example.pixeltoolbox.signal.SignalMonitor
import com.example.pixeltoolbox.signal.SignalDashboardState
import com.example.pixeltoolbox.signal.SignalMetrics
import com.example.pixeltoolbox.signal.NetworkMetrics
import com.example.pixeltoolbox.signal.DeviceMetrics
import com.example.pixeltoolbox.signal.TrafficMetrics
import com.example.pixeltoolbox.signal.SystemMetrics
import com.example.pixeltoolbox.ui.signal.SignalDashboard
import com.example.pixeltoolbox.ui.geektools.GeekToolsCard
import com.example.pixeltoolbox.ui.geektools.SectionTitle
import rikka.shizuku.Shizuku
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.PrintWriter
import java.io.StringWriter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.pixeltoolbox.data.AppPreferences
import com.example.pixeltoolbox.services.recording.ManageOngoingCalls
import com.example.pixeltoolbox.ui.custom.CallRecordingSettingsActivity
import androidx.compose.material3.MaterialTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SignalScreen(
    hasShizuku: Boolean, updateShizuku: (Boolean) -> Unit,
    executionLogs: List<String>,
    signalMetrics: SignalMetrics,
    networkMetrics: NetworkMetrics,
    deviceMetrics: DeviceMetrics,
    trafficMetrics: TrafficMetrics,
    systemMetrics: SystemMetrics,
    context: Context, coroutineScope: CoroutineScope, addLog: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // SIM 卡列表与选中状态
        val simSlots = remember { mutableStateListOf<SimSlotInfo>() }
        var selectedSubId by remember { mutableStateOf(-1) }
        // 加载 SIM 卡列表
        LaunchedEffect(hasShizuku) {
            val slots = ShizukuUtils.getAvailableSimSlots(context)
            simSlots.clear()
            simSlots.addAll(slots)
            if (selectedSubId < 0 && slots.isNotEmpty()) {
                selectedSubId = slots.first().subId
            }
        }
        Text("像素工具箱", style = MaterialTheme.typography.headlineMedium, color = iOSLabel)
        Spacer(modifier = Modifier.height(16.dp))
        ShizukuAuthCard(hasShizuku, updateShizuku)
        // ===== Carrier IMS 高级配置已移除 =====
        Spacer(modifier = Modifier.height(16.dp))
        // Wrap SignalDashboard in a Box with weight(1f) to ensure it can scroll without pushing bottom button off screen
        SignalDashboard(
                signalMetrics = signalMetrics,
                networkMetrics = networkMetrics,
                deviceMetrics = deviceMetrics,
                trafficMetrics = trafficMetrics,
                systemMetrics = systemMetrics,
                simSlots = simSlots,
                selectedSubId = selectedSubId,
                onSelectSubId = { selectedSubId = it },
                addLog = addLog
            )
        // ========== 还原所有设置 ==========
        Spacer(modifier = Modifier.height(16.dp))
        var showRestoreDialog by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showRestoreDialog = true },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("还原所有网络设置", color = Color.Red.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge)
        }
        // 确认弹窗
        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("还原所有设置") },
                text = { 
                Text("将清除旧版 settings 残留、还原所有物理 SIM 的 CarrierConfig、重开 WiFi 并执行飞行模式切换。\n飞行模式切换为全局操作，eSIM 也将短暂断连。") 
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    coroutineScope.launch {
                        addLog("===== 开始还原所有设置 =====")
                        // 1. 清除旧版娉ㄥ叆鐨?settings 残留
                        addLog("清除旧版 settings 残留...")
                        ShizukuUtils.executeCommand("settings delete global vonr_enabled")
                        ShizukuUtils.executeCommand("settings delete global carrier_config_version")
                        addLog("settings 残留已清理")
                        // 2. 还原 CarrierConfig（跳过 eSIM）
                        for (slot in simSlots) {
                            if (slot.isEmbedded) {
                                addLog("跳过 eSIM (${slot.carrierName})")
                                continue
                            }
                            val sid = slot.subId
                            addLog("还原 SIM${slot.slotIndex + 1} CarrierConfig...")
                            val (ok, msg) = kotlin.coroutines.suspendCoroutine<Pair<Boolean, String>> { continuation ->
                                ShizukuUtils.restoreCarrierConfig(context, sid) { success, message ->
                                    continuation.resumeWith(Result.success(Pair(success, message)))
                                }
                            }
                            addLog(if (ok) "SIM${slot.slotIndex + 1} 已还原" else "SIM${slot.slotIndex + 1} 还原失败: $msg")
                        }
                        // 3. 还原 WiFi锛堥噸寮€锛?
                        addLog("还原 WiFi...")
                        ShizukuUtils.executeCommand("svc wifi disable")
                        delay(1000)
                        ShizukuUtils.executeCommand("cmd wifi set-wifi-enabled enabled")
                        addLog("WiFi 已重")
                        // 4. 飞行模式切换，强制 modem 重读配置
                        addLog("触发无线重置...")
                        ShizukuUtils.executeCommand("cmd connectivity airplane-mode enable")
                        delay(3000)
                        ShizukuUtils.executeCommand("cmd connectivity airplane-mode disable")
                        addLog("无线已重置")
                        addLog("===== 还原完成 =====")
                        Toast.makeText(context, "已彻底还原，网络已重置", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("还原", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = iOSCardBackground,
            titleContentColor = iOSLabel,
            textContentColor = iOSSecondaryLabel
        )
    }
    Spacer(modifier = Modifier.height(32.dp))
}
}
@Composable
private fun ImsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = iOSLabel)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iOSGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = iOSSeparator
            )
        )
    }
}
@Composable
fun ImsGroupSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = iOSLabel)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iOSBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = iOSSeparator
            )
        )
    }
}
