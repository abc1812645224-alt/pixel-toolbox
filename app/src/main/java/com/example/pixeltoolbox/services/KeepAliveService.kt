package com.example.pixeltoolbox.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log

class KeepAliveService : Service() {
    companion object {
        var isRunning = false
    }
    
    private val CHANNEL_ID = "KeepAliveChannel"
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageNames = intent?.getStringArrayExtra("PACKAGE_NAMES") ?: arrayOf("com.tencent.mm")
        val intervalMins = intent?.getIntExtra("INTERVAL_MINS", 3) ?: 3

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("强力保活运行中")
            .setContentText("目标: ${packageNames.joinToString(", ")} (每 $intervalMins 分钟唤醒)")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(1001, notification)

        startKeepAliveLoop(packageNames, intervalMins)
        isRunning = true
        return START_NOT_STICKY // 不自动重启，关机重启失效
    }

    private fun startKeepAliveLoop(packageNames: Array<String>, intervalMins: Int) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PixelToolbox::KeepAlive")
            
            while (true) {
                try {
                    wakeLock?.acquire(10 * 1000L) // 保护性 acquiring
                    
                    // 尝试向目标应用发送一个无害的启动广播，刺激其心跳
                    for (packageName in packageNames) {
                        val pingIntent = Intent(Intent.ACTION_MAIN)
                        pingIntent.setPackage(packageName)
                        pingIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        sendBroadcast(pingIntent)
                    }
                    
                } catch (e: Exception) {
                    Log.e("KeepAlive", "Error waking app", e)
                } finally {
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                    }
                }
                delay(intervalMins * 60 * 1000L)
            }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "强力保活服务 (可手动关闭通知)",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.description = "用于在后台定期唤醒 CPU，防止消息延迟。您可以关闭此通知以保持状态栏清爽。"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
