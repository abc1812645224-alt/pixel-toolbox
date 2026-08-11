package com.example.pixeltoolbox
import java.io.File
import java.io.FileOutputStream
import com.example.pixeltoolbox.ui.signal.ImsGroupSwitchRow
import com.example.pixeltoolbox.ui.signal.SignalScreen
import com.example.pixeltoolbox.ui.system.SystemScreen
import com.example.pixeltoolbox.ui.about.AboutScreen
import com.example.pixeltoolbox.ui.system.BootManagerScreen
import com.example.pixeltoolbox.ui.tools.ToolboxScreen
import com.example.pixeltoolbox.ui.about.DisclaimerScreen
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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
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
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    private val batteryLevel = mutableStateOf(0)
    private val batteryTemp = mutableStateOf(0f)
    private val batteryVoltage = mutableStateOf(0)
    private val batteryStatus = mutableStateOf(BatteryManager.BATTERY_STATUS_UNKNOWN)
    private val batteryCurrent = mutableStateOf(0)
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                val volt = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
                batteryTemp.value = temp / 10f
                batteryVoltage.value = volt
                batteryStatus.value = status
                android.util.Log.d("BatteryDebug", "status=$status level=$level/$scale temp=$temp volt=$volt")
                // 优先方式 1: BatteryManager.getIntProperty
                try {
                    val bm = context?.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    batteryCurrent.value = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
                } catch (_: Exception) {
                    batteryCurrent.value = 0
                }
            }
        }
    }
    private var isBound = false
    private lateinit var shizukuListener: Shizuku.OnBinderReceivedListener
    // Store crash log to display on next startup
    private var startupCrashLog: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        // 设置全局崩溃捕获日志
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val crashMsg = "--- CRASH LOG ---\nTime: ${java.util.Date()}\nThread: ${thread.name}\nException:\n"
                val sw = java.io.StringWriter()
                val pw = java.io.PrintWriter(sw)
                throwable.printStackTrace(pw)
                val fullLog = crashMsg + sw.toString()
                // 写入私有目录        
        val privateDir = getExternalFilesDir(null)
                if (privateDir != null) {
                    java.io.File(privateDir, "crash_$timestamp.txt").writeText(fullLog)
                }
                // 写入 Download 目录 (Android 10+ 可以直接)
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir != null) {
                    java.io.File(downloadDir, "PixelToolbox_crash_$timestamp.txt").writeText(fullLog)
                }
            } catch (e: Exception) {
                // ignore
            }
            // 交给系统默认处理器（弹出崩溃框并记录系统日志）
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                kotlin.system.exitProcess(1)
            }
        }
        super.onCreate(savedInstanceState)
        // 沉浸式透明状态栏与导航栏配置
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        // 检查是否有最新崩溃日志 (从私有目录读取)
        try {
            val privateDir = getExternalFilesDir(null)
            if (privateDir != null && privateDir.exists()) {
                val crashFiles = privateDir.listFiles { _, name -> name.startsWith("crash_") && name.endsWith(".txt") }
                if (crashFiles != null && crashFiles.isNotEmpty()) {
                    // 取最新的一个
                    val latestCrash = crashFiles.maxByOrNull { it.lastModified() }
                    if (latestCrash != null) {
                        startupCrashLog = latestCrash.readText()
                        // 为了避免每次启动都提示，可以将它们移到一个 old_crashes 文件夹或者直接删除
                        crashFiles.forEach { it.delete() }
                    }
                }
            }
        } catch (e: Exception) {}
        Shizuku.addRequestPermissionResultListener(permissionListener)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            PixelToolboxTheme(windowWidthSizeClass = windowSizeClass.widthSizeClass) {
                PixelToolboxApp(
                    batTemp = batteryTemp.value,
                    batVolt = batteryVoltage.value,
                    batteryStatus = batteryStatus.value,
                    batCurrentNA = batteryCurrent.value,
                    initialCrashLog = startupCrashLog
                )
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == 1000 && grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
              Toast.makeText(this, "Shizuku 权限授权成功", Toast.LENGTH_SHORT).show()
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelToolboxApp(batTemp: Float, batVolt: Int, batteryStatus: Int, batCurrentNA: Int, initialCrashLog: String?) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bgColor = iOSBackground
    val textColor = iOSLabel
    var hasShizuku by remember { mutableStateOf(ShizukuUtils.hasShizukuPermission()) }
    // 极客终端状态
    val (terminalInput, setTerminalInput) = remember { mutableStateOf("") }
    val (terminalOutput, setTerminalOutput) = remember { mutableStateOf("Ready.\n") }
    var dpiInput by remember { mutableStateOf("") }
    // 全局执行日志
    val executionLogs = remember { mutableStateListOf<String>() }
    val addLog = { msg: String ->
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        executionLogs.add(0, "[$time] $msg")
        if (executionLogs.size > 6) {
            executionLogs.removeLast()
        }
    }
    androidx.compose.runtime.LaunchedEffect(initialCrashLog) {
        if (!initialCrashLog.isNullOrEmpty()) {
            addLog("⚠️ 发现上次崩溃日志，已保存至 下载(Download) 文件夹：\n${initialCrashLog.take(200)}...")
        }
    }
    var showBarometerTest by remember { mutableStateOf(false) }
    var signalMetrics by remember { mutableStateOf(SignalMetrics()) }
    var networkMetrics by remember { mutableStateOf(NetworkMetrics()) }
    var deviceMetrics by remember { mutableStateOf(DeviceMetrics()) }
    var trafficMetrics by remember { mutableStateOf(TrafficMetrics()) }
    var systemMetrics by remember { mutableStateOf(SystemMetrics()) }
    val signalMonitor = remember { SignalMonitor(context) }
    var selectedTab by remember { mutableStateOf(0) }
    var showBootManager by remember { mutableStateOf(false) }
    var showGpsTest by remember { mutableStateOf(false) }
    // 权限请求标记
    var permissionRequested by remember { mutableStateOf(false) }
    // 权限请求启动
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionRequested = true
        // SignalMonitor flow 会在下一轮自动重新检测权限状态
    }
    // 信号页权限请求
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && !permissionRequested) {
            val perms = mutableListOf<String>()
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_PHONE_STATE)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (perms.isNotEmpty()) {
                permissionLauncher.launch(perms.toTypedArray())
            } else {
                permissionRequested = true
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(selectedTab, lifecycleOwner) {
        if (selectedTab == 0) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                signalMonitor.startMonitoring().collect { state ->

                signalMetrics = SignalMetrics(
                    servingCells = state.servingCells,
                    neighborCells = state.neighborCells,
                    networkMode = state.networkMode,
                    dataState = state.dataState,
                    carrierName = state.carrierName,
                    serviceState = state.serviceState,
                    aggregatedBands = state.aggregatedBands,
                    caStateText = state.caStateText
                )
                networkMetrics = NetworkMetrics(
                    subscriptionDownlink = state.subscriptionDownlink,
                    subscriptionUplink = state.subscriptionUplink,
                    qci = state.qci
                )
                deviceMetrics = DeviceMetrics(
                    deviceModel = state.deviceModel,
                    firmwareVersion = state.firmwareVersion,
                    cpuUsage = state.cpuUsage,
                    ramUsage = state.ramUsage
                )
                trafficMetrics = TrafficMetrics(
                    todayTraffic = state.todayTraffic,
                    todayDlTraffic = state.todayDlTraffic,
                    todayUlTraffic = state.todayUlTraffic,
                    monthTotalTraffic = state.monthTotalTraffic,
                    monthDlTraffic = state.monthDlTraffic,
                    monthUlTraffic = state.monthUlTraffic,
                    monthDlPercent = state.monthDlPercent,
                    wifiTodayTraffic = state.wifiTodayTraffic,
                    wifiMonthTotalTraffic = state.wifiMonthTotalTraffic,
                    wifiMonthDlTraffic = state.wifiMonthDlTraffic,
                    wifiMonthUlTraffic = state.wifiMonthUlTraffic
                )
                systemMetrics = SystemMetrics(
                    uptimeText = state.uptimeText,
                    lastUpdateTime = state.lastUpdateTime
                )

            }
        }
    }
}
    DisposableEffect(Unit) {
        val listener = Shizuku.OnBinderReceivedListener {
            hasShizuku = ShizukuUtils.hasShizukuPermission()
        }
        val pListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
            hasShizuku = ShizukuUtils.hasShizukuPermission()
        }
        Shizuku.addBinderReceivedListener(listener)
        Shizuku.addRequestPermissionResultListener(pListener)
        onDispose {
            Shizuku.removeBinderReceivedListener(listener)
            Shizuku.removeRequestPermissionResultListener(pListener)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasShizuku = ShizukuUtils.hasShizukuPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // SensorManager logic removed, handled in BarometerScreen
    // ═══════════════════════════════════════════
    // 免责声明检查
    // ═══════════════════════════════════════════
    val prefs = context.getSharedPreferences("pixel_toolbox_prefs", Context.MODE_PRIVATE)
    var disclaimerAccepted by remember { mutableStateOf(prefs.getBoolean("disclaimer_accepted", false)) }
    if (!disclaimerAccepted) {
        DisclaimerScreen(
            onAgree = {
                prefs.edit().putBoolean("disclaimer_accepted", true).apply()
                disclaimerAccepted = true
            },
            onRefuse = {
                (context as? android.app.Activity)?.finish()
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        )
    } else if (showGpsTest) {
        com.example.pixeltoolbox.ui.geektools.GpsTestScreen(context = context, onBack = { showGpsTest = false })
    } else if (showBarometerTest) {
        com.example.pixeltoolbox.ui.geektools.BarometerScreen(context = context, onBack = { showBarometerTest = false })
    } else if (showBootManager) {
        BootManagerScreen(context = context, addLog = addLog, onBack = { showBootManager = false })
    } else {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = iOSCardBackground,
                tonalElevation = 0.dp,
                contentColor = iOSBlue
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.SignalCellularAlt, contentDescription = "信号") },
                    label = { Text("信号", style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iOSBlue,
                        selectedTextColor = iOSBlue,
                        unselectedIconColor = iOSNavUnselected,
                        unselectedTextColor = iOSNavUnselected,
                        indicatorColor = iOSBlue.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "系统") },
                    label = { Text("系统", style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iOSBlue,
                        selectedTextColor = iOSBlue,
                        unselectedIconColor = iOSNavUnselected,
                        unselectedTextColor = iOSNavUnselected,
                        indicatorColor = iOSBlue.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Build, contentDescription = "工具") },
                    label = { Text("工具", style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Normal)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iOSBlue,
                        selectedTextColor = iOSBlue,
                        unselectedIconColor = iOSNavUnselected,
                        unselectedTextColor = iOSNavUnselected,
                        indicatorColor = iOSBlue.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "关于") },
                    label = { Text("关于", style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selectedTab == 3) FontWeight.SemiBold else FontWeight.Normal)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iOSBlue,
                        selectedTextColor = iOSBlue,
                        unselectedIconColor = iOSNavUnselected,
                        unselectedTextColor = iOSNavUnselected,
                        indicatorColor = iOSBlue.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(bgColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .then(
                        if (selectedTab == 1 || selectedTab == 2) Modifier.verticalScroll(rememberScrollState())
                        else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                if (selectedTab != 0) {
                    Text(
                        text = when(selectedTab) {
                            1 -> "系统优化中心"
                            2 -> "极客工具箱"
                            3 -> "关于 像素工具箱"
                            else -> ""
                        },
                        style = MaterialTheme.typography.headlineMedium, color = iOSLabel
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                when (selectedTab) {
                    0 -> SignalScreen(
                        hasShizuku, { hasShizuku = it },
                        executionLogs, 
                        signalMetrics, networkMetrics, deviceMetrics, trafficMetrics, systemMetrics,
                        context, coroutineScope, addLog
                    )
                    1 -> SystemScreen(
                        executionLogs,
                        batTemp, batVolt, batteryStatus, batCurrentNA,
                        dpiInput, { dpiInput = it },
                        context, coroutineScope, addLog,
                        onOpenGpsTest = { showGpsTest = true },
                        onOpenBarometerTest = { showBarometerTest = true }
                    )
                    2 -> ToolboxScreen(
                        executionLogs,
                        terminalInput, setTerminalInput,
                        terminalOutput, setTerminalOutput,
                        context, coroutineScope, addLog,
                        onOpenBootManager = { showBootManager = true }
                    )
                    3 -> AboutScreen()
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    }
}
// ======================= REUSABLE COMPONENTS =======================
@Composable
fun ShizukuAuthCard(hasShizuku: Boolean, updateShizuku: (Boolean) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Shizuku 核心服务", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (hasShizuku) "服务已连接" else "未授权",
                    color = if (hasShizuku) iOSGreen else iOSRed,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
                iOSButton(
                    onClick = {
                        ShizukuUtils.requestShizukuPermission(1000)
                        updateShizuku(ShizukuUtils.hasShizukuPermission())
                    }
                ) {
                    Text(if (hasShizuku) "重新获取" else "获取权限", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
@Composable
fun ExecutionLogCard(executionLogs: List<String>) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("实时执行日志 (全部)", style = MaterialTheme.typography.titleLarge, color = iOSLabel)
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("PixelToolbox_Logs", executionLogs.joinToString("\n"))
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "日志已成功制到剴板！", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("复制日志 📋", color = iOSBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (executionLogs.isEmpty()) {
                        Text("暂无操作日志...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        Column {
                            executionLogs.forEach { log ->
                                Text(log, style = MaterialTheme.typography.bodySmall, color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("关闭", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("实时执行日志", style = MaterialTheme.typography.titleMedium, color = iOSLabel)
                Text("点击展开全屏 🔍", style = MaterialTheme.typography.bodySmall, color = iOSBlue)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(iOSBackground, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (executionLogs.isEmpty()) {
                    Text("暂无操作...", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
                } else {
                    Column {
                        executionLogs.forEach { log ->
                            Text(log, style = MaterialTheme.typography.bodySmall, color = iOSLabel, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}
// ======================= SCREENS =======================
private fun querySystemPowerMode(): String? {
    if (!ShizukuUtils.hasShizukuPermission()) return null
    val candidates = listOf(
        "cmd power get-mode 2>/dev/null",
        "dumpsys power 2>/dev/null | grep -oE 'mPowerMode=[0-9]+' | head -n1 | cut -d= -f2",
        "dumpsys power 2>/dev/null | grep -oE 'PowerMode=[0-9]+' | head -n1 | cut -d= -f2"
    )
    for (cmd in candidates) {
        val out = ShizukuUtils.executeCommandOrNull(cmd)?.trim() ?: continue
        val mode = parsePowerModeNumber(out)
        if (mode != null) return mode
    }
    // 辅助判断：系统低电耗（省电）开关开启时归为省电
    val lowPower = ShizukuUtils.executeCommandOrNull("settings get global low_power 2>/dev/null")?.trim()
    if (lowPower == "1") return "saver"
    return null
}
private fun parsePowerModeNumber(raw: String): String? = when (raw.trim()) {
    "0" -> "saver"
    "1" -> "balanced"
    "2" -> "performance"
    else -> null
}
