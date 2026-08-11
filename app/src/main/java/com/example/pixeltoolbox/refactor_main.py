import os

file_path = r'D:\Zhuo Mian\pixel-toolbox\app\src\main\java\com\example\pixeltoolbox\ui\MainScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace signature
text = text.replace(
    'fun PixelToolboxApp(batTemp: Float, batVolt: Int, batteryStatus: Int, batCurrentNA: Int, initialCrashLog: String?) {',
    'fun PixelToolboxApp(initialCrashLog: String?, viewModel: MainViewModel) {'
)

# Remove local states that were moved to ViewModel
text = text.replace('var hasShizuku by remember { mutableStateOf(ShizukuUtils.hasShizukuPermission()) }', 
    'val hasShizuku by viewModel.hasShizuku')

text = text.replace('val executionLogs = remember { mutableStateListOf<String>() }',
    'val executionLogs = viewModel.executionLogs')

text = text.replace('''val addLog = { msg: String ->
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        executionLogs.add(0, "[$time] $msg")
        if (executionLogs.size > 6) {
            executionLogs.removeLast()
        }
    }''', 'val addLog = { msg: String -> viewModel.addLog(msg) }')

# Replace parameters passed to screens
text = text.replace('batTemp, batVolt, batteryStatus, batCurrentNA,',
    'viewModel.batteryTemp.value, viewModel.batteryVoltage.value, viewModel.batteryStatus.value, viewModel.batteryCurrent.value,')

text = text.replace('hasShizuku, { hasShizuku = it },',
    'hasShizuku, { viewModel.updateShizuku(it) },')

# Remove DisposableEffect for Shizuku as it's now in ViewModel
import re
text = re.sub(r'DisposableEffect\(Unit\) \{[\s\S]*?onDispose \{[\s\S]*?\}\s*\}', '', text, count=1)
text = re.sub(r'val lifecycleOwner = androidx\.compose\.ui\.platform\.LocalLifecycleOwner\.current[\s\S]*?DisposableEffect\(lifecycleOwner\) \{[\s\S]*?onDispose \{[\s\S]*?\}\s*\}', '', text, count=1)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(text)

# Now MainActivity.kt
ma_path = r'D:\Zhuo Mian\pixel-toolbox\app\src\main\java\com\example\pixeltoolbox\MainActivity.kt'
new_ma_text = """package com.example.pixeltoolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.example.pixeltoolbox.ui.PixelToolboxApp
import com.example.pixeltoolbox.ui.theme.PixelToolboxTheme
import com.example.pixeltoolbox.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var startupCrashLog: String? = null
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val crashMsg = "--- CRASH LOG ---\\nTime: ${Date()}\\nThread: ${thread.name}\\nException:\\n"
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val fullLog = crashMsg + sw.toString()
                
                val privateDir = getExternalFilesDir(null)
                if (privateDir != null) {
                    File(privateDir, "crash_$timestamp.txt").writeText(fullLog)
                }
                
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir != null) {
                    File(downloadDir, "PixelToolbox_crash_$timestamp.txt").writeText(fullLog)
                }
            } catch (e: Exception) {
            }
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                exitProcess(1)
            }
        }
        
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        try {
            val privateDir = getExternalFilesDir(null)
            if (privateDir != null && privateDir.exists()) {
                val crashFiles = privateDir.listFiles { _, name -> name.startsWith("crash_") && name.endsWith(".txt") }
                if (crashFiles != null && crashFiles.isNotEmpty()) {
                    val latestCrash = crashFiles.maxByOrNull { it.lastModified() }
                    if (latestCrash != null) {
                        startupCrashLog = latestCrash.readText()
                        crashFiles.forEach { it.delete() }
                    }
                }
            }
        } catch (e: Exception) {}

        setContent {
            PixelToolboxTheme {
                PixelToolboxApp(initialCrashLog = startupCrashLog, viewModel = viewModel)
            }
        }
    }
}
"""
with open(ma_path, 'w', encoding='utf-8') as f:
    f.write(new_ma_text)

print("Done refactoring MainScreen and MainActivity")
