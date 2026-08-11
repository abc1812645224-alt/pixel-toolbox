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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.MaterialTheme

class StatusBarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelToolboxTheme {
                StatusBarScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun StatusBarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val iconIds = listOf("alarm_clock", "bluetooth", "nfc", "wifi", "mobile", "battery", "vpn", "zen", "airplane", "hotspot")
    val iconNames = listOf("闹钟图标", "蓝牙图标", "NFC 图标", "WiFi 图标", "移动数据图标", "电池图标", "VPN 图标", "免打扰图标", "飞行模式图标", "热点图标")
    val iconDescs = listOf("隐藏状态栏顶部的闹钟提示图标", "隐藏状态栏顶部的蓝牙标志", "隐藏状态栏的 NFC 标志", "隐藏无线网络状态图标", "隐藏移动网络状态图标", "隐藏电池电量图标", "隐藏 VPN 钥匙图标", "隐藏免打扰月亮图标", "隐藏飞行模式飞机图标", "隐藏个人热点分享图标")

    val checkedStates = remember { mutableStateListOf<Boolean>().apply { repeat(iconIds.size) { add(false) } } }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val result = ShizukuUtils.executeCommand("settings get secure icon_blacklist")
            val blacklist = result.getOrDefault("").toString().trim()
            val blacklistedItems = if (blacklist == "null" || blacklist.isEmpty()) {
                emptyList()
            } else {
                blacklist.split(",")
            }
            
            withContext(Dispatchers.Main) {
                for (i in iconIds.indices) {
                    checkedStates[i] = blacklistedItems.contains(iconIds[i])
                }
                isLoading = false
            }
        }
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
                text = "状态栏净化",
                style = MaterialTheme.typography.headlineMedium,
                color = iOSLabel
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "选择你想要隐藏的系统图标",
                style = MaterialTheme.typography.bodyMedium,
                color = iOSSecondaryLabel
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = iOSBlue)
            }
        } else {
            // List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                itemsIndexed(iconIds) { index, id ->
                    val isChecked = checkedStates[index]
                    
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = iconNames[index],
                                    style = MaterialTheme.typography.titleMedium,
                                    color = iOSLabel
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = iconDescs[index],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = iOSSecondaryLabel
                                )
                            }
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checkedStates[index] = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = iOSBlue,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = iOSSeparator
                                )
                            )
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
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                iOSButton(
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            val activeList = iconIds.filterIndexed { index, _ -> checkedStates[index] }
                            val blacklistStr = activeList.joinToString(",")
                            val cmd = "settings put secure icon_blacklist \"$blacklistStr\""
                            
                            val result = withContext(Dispatchers.IO) {
                                ShizukuUtils.executeCommand(cmd)
                            }
                            
                            isSaving = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "状态栏净化配置已保存并应用！", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "保存失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("保存并立即应用", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
