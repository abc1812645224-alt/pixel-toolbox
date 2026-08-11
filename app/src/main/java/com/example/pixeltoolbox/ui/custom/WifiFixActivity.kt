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

package com.example.pixeltoolbox.ui.custom

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import com.example.pixeltoolbox.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.material3.MaterialTheme

class WifiFixActivity : ComponentActivity() {

    private val serverNames = listOf(
        "Google 国际服务 (原生默认)",
        "小米服务 (国内极速)",
        "华为服务 (国内稳定)",
        "Vivo 服务 (国内备用)",
        "高通中国 (官方节点)",
        "V2EX 社区 (公益节点)",
        "阿里云 (国内节点)",
        "Apple (跨平台节点)",
        "Microsoft (Windows默认)",
        "Cloudflare (国际节点)"
    )
    private val serverUrls = listOf(
        "http://connectivitycheck.gstatic.com/generate_204",
        "http://connect.rom.miui.com/generate_204",
        "http://connectivitycheck.platform.hicloud.com/generate_204",
        "http://wifi.vivo.com.cn/generate_204",
        "http://www.qualcomm.cn/generate_204",
        "http://captive.v2ex.co/generate_204",
        "http://alidns.com/generate_204",
        "http://captive.apple.com/hotspot-detect.html",
        "http://www.msftconnecttest.com/connecttest.txt",
        "https://cp.cloudflare.com/generate_204"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelToolboxTheme {
                WifiFixScreen(
                    serverNames = serverNames,
                    serverUrls = serverUrls,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun WifiFixScreen(
    serverNames: List<String>,
    serverUrls: List<String>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedIndex by remember { mutableIntStateOf(0) }
    val latencies = remember { mutableStateListOf<Long>().apply { 
        repeat(serverUrls.size) { add(-1L) } 
    } }
    val errors = remember { mutableStateListOf<String>().apply { 
        repeat(serverUrls.size) { add("") } 
    } }
    var isTesting by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }

    val runTests = {
        if (!isTesting) {
            isTesting = true
            for (i in serverUrls.indices) {
                latencies[i] = -1L
                errors[i] = ""
            }
            coroutineScope.launch {
                val jobs = serverUrls.mapIndexed { index, urlStr ->
                    launch(Dispatchers.IO) {
                        val startTime = System.currentTimeMillis()
                        var conn: HttpURLConnection? = null
                        try {
                            val url = URL(urlStr)
                            conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 4000
                            conn.readTimeout = 4000
                            conn.requestMethod = "GET"
                            conn.instanceFollowRedirects = false
                            conn.useCaches = false
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                            conn.setRequestProperty("Accept", "*/*")
                            
                            val code = conn.responseCode
                            try {
                                conn.inputStream.use { it.readBytes() }
                            } catch (e: Exception) {}
                            
                            val elapsed = System.currentTimeMillis() - startTime
                            if (code == 204 || code == 200 || code == 301 || code == 302) {
                                latencies[index] = elapsed
                            } else {
                                errors[index] = "状态码: $code"
                            }
                        } catch (e: Exception) {
                            errors[index] = e.javaClass.simpleName
                        } finally {
                            conn?.disconnect()
                        }
                    }
                }
                jobs.forEach { it.join() }
                isTesting = false
            }
        }
    }

    LaunchedEffect(Unit) {
        runTests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
            .statusBarsPadding()
    ) {
        // Header
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "去除 WiFi 感叹号",
                style = MaterialTheme.typography.headlineMedium,
                color = iOSLabel
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "一键部署能正常连通的验证服务器，秒除网络图标小感叹号",
                style = MaterialTheme.typography.bodyMedium,
                color = iOSSecondaryLabel
            )
        }

        // List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            itemsIndexed(serverNames) { index, name ->
                val isSelected = selectedIndex == index
                val latency = latencies[index]
                val error = errors[index]
                
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { selectedIndex = index }
                        .let { 
                            if (isSelected) it.background(Color.Transparent).then(Modifier.border(2.dp, iOSBlue, RoundedCornerShape(20.dp))) else it 
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = iOSLabel
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isTesting && latency == -1L && error.isEmpty()) {
                            Text("测速中...", style = MaterialTheme.typography.bodySmall, color = iOSBlue)
                        } else if (latency != -1L) {
                            Text("延迟: $latency ms", style = MaterialTheme.typography.bodySmall, color = iOSGreen)
                        } else if (error.isNotEmpty()) {
                            Text("失败: $error", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
                        } else {
                            Text("等待测速", style = MaterialTheme.typography.bodySmall, color = iOSSecondaryLabel)
                        }
                    }
                }
            }
        }

        // Bottom Actions
        Surface(
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                iOSOutlineButton(
                    onClick = { runTests() },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("刷新测速", fontWeight = FontWeight.SemiBold)
                }

                iOSButton(
                    onClick = {
                        isApplying = true
                        coroutineScope.launch {
                            val urlStr = serverUrls[selectedIndex]
                            var host = urlStr.replace("http://", "").replace("https://", "")
                            val slashIdx = host.indexOf('/')
                            if (slashIdx >= 0) {
                                host = host.substring(0, slashIdx)
                            }

                            val cmds = listOf(
                                "settings delete global captive_portal_mode",
                                "settings delete global captive_portal_server",
                                "settings delete global captive_portal_use_https",
                                "settings delete global captive_portal_detection_enabled",
                                "settings delete global captive_portal_http_url",
                                "settings delete global captive_portal_https_url",
                                "settings put global captive_portal_detection_enabled 1",
                                "settings put global captive_portal_mode 1",
                                "settings put global captive_portal_use_https 1",
                                "settings put global captive_portal_server $host",
                                "settings put global captive_portal_http_url $urlStr",
                                "settings put global captive_portal_https_url $urlStr",
                                "svc wifi disable",
                                "sleep 1",
                                "svc wifi enable"
                            )

                            withContext(Dispatchers.IO) {
                                for (cmd in cmds) {
                                    ShizukuUtils.executeCommand(cmd)
                                }
                            }
                            isApplying = false
                            Toast.makeText(context, "去除 WiFi 感叹号成功，网络已重新连通！", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("应用该配置", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
