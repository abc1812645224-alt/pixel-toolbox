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

package com.example.pixeltoolbox.ui.geektools

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.example.pixeltoolbox.R
import com.example.pixeltoolbox.ui.theme.PixelToolboxTheme
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import com.example.pixeltoolbox.util.LogCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.android.apksig.ApkSigner
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import androidx.compose.material3.MaterialTheme

class AppCloneActivity : ComponentActivity() {

    data class AppInfo(val packageName: String, val appName: String, val icon: Drawable? = null)
    data class CloneRecord(val originalPkg: String, val clonePkg: String, val appName: String, val icon: Drawable? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogCollector.init(this)
        setContent { PixelToolboxTheme { AppCloneScreen() } }
    }

    private fun getDisclaimerAccepted(context: android.content.Context): Boolean {
        return context.getSharedPreferences("app_clone_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("clone_disclaimer_accepted", false)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppCloneScreen() {
        val context = LocalContext.current
        val disclaimerAccepted = remember { mutableStateOf(getDisclaimerAccepted(context)) }
        val hasScrolledToBottom = remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
        val pm = context.packageManager
        val scope = rememberCoroutineScope()

        var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
        var clones by remember { mutableStateOf<List<CloneRecord>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var isProcessing by remember { mutableStateOf(false) }
        var statusMsg by remember { mutableStateOf("正在加载...") }
        var currentTab by remember { mutableIntStateOf(0) }
        var pendingCloneApp by remember { mutableStateOf<AppInfo?>(null) }

        LaunchedEffect(disclaimerAccepted.value) {
            if (disclaimerAccepted.value) {
            LogCollector.log("成功", "打开应用分身页面")
            withContext(Dispatchers.IO) {
                try {
                    val all = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                        .filter { it.packageName != context.packageName && !it.packageName.endsWith(".clone") }
                        .sortedBy { it.loadLabel(pm).toString() }
                        .map { AppInfo(it.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }

                    val clonePkgs = all.mapNotNull { a ->
                        val cp = a.packageName + ".clone"
                        try { pm.getPackageInfo(cp, 0); cp } catch (_: Exception) { null }
                    }
                    clones = clonePkgs.map { cp ->
                        val orig = cp.removeSuffix(".clone")
                        val origApp = all.find { it.packageName == orig }
                        CloneRecord(orig, cp, origApp?.appName ?: orig, origApp?.icon)
                    }
                    apps = all.filter { (it.packageName + ".clone") !in clonePkgs.toSet() }
                    statusMsg = "就绪"
                    LogCollector.log("成功", "获取应用列表成功，共 ${all.size} 个应用，${clones.size} 个已分身")
                } catch (e: Exception) {
                    statusMsg = "错误: ${e.message}"
                    LogCollector.log("失败", "获取应用列表失败", "${e.javaClass.simpleName}: ${e.message}")
                }
                isLoading = false
            }
            }
        }

        if (!disclaimerAccepted.value) {
            AlertDialog(
                onDismissRequest = { /* 不可关闭 */ },
                title = { Text("免责声明与风险告知", fontWeight = FontWeight.Bold) },
                text = {
                    LaunchedEffect(scrollState.value) {
                        if (scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue) {
                            hasScrolledToBottom.value = true
                        }
                    }
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                        "本功能仅供技术学习、安全研究、漏洞挖掘与软件兼容性测试使用。严禁将本功能用于任何违法违规用途，包括但不限于：\n\n" +
                        "一、您确认并承诺：\n" +
                        "1. 您通过本功能创建的应用程序副本（以下简称\"克隆应用\"）仅用于个人技术研究目的，不得用于任何商业用途或盈利行为。\n" +
                        "2. 您不会利用克隆应用进行账号多开、薅取新用户福利、虚假注册、刷量刷单、欺诈、侵犯他人隐私、传播恶意软件、绕过应用内购买、规避安全检测或任何形式的违法行为。\n" +
                        "3. 您不会利用克隆应用干扰其他应用的正常服务、破坏平台秩序、侵害第三方合法权益，或违反目标应用的服务条款（TOS）与用户协议。\n" +
                        "4. 分身软件本身不违法，但利用其进行上述行为可能构成非法获取计算机信息系统数据罪、侵犯著作权罪、诈骗罪等刑事犯罪。\n\n" +
                        "二、使用限制与义务：\n" +
                        "1. 您必须在创建克隆应用后的 12 小时内彻底删除该克隆应用及其所有相关数据。超过 12 小时未删除，您将自行承担由此引发的全部法律后果。\n" +
                        "2. 克隆应用的数据安全性由您自行负责。本工具不对克隆应用的数据泄露、丢失、被第三方窃取等任何安全问题承担责任。\n" +
                        "3. 部分应用可能检测克隆行为并采取封禁账号、限制功能等措施，由此产生的一切损失（包括但不限于账号封禁、数据丢失、虚拟财产损失）均由您自行承担。\n\n" +
                        "三、责任豁免：\n" +
                        "1. 本工具开发者（以下简称\"开发者\"）不对您使用本功能所产生的任何直接或间接损失承担责任，包括但不限于数据丢失、设备损坏、账号封禁、法律纠纷、经济损失等。\n" +
                        "2. 开发者不保证本功能在所有设备、所有 Android 版本、所有应用上均能正常工作。克隆功能可能因系统更新、应用更新等原因失效。\n" +
                        "3. 您使用本功能即视为您已充分阅读、理解并同意本免责声明的全部条款。若您不同意，请立即停止使用并退出本页面。\n\n" +
                        "四、法律适用：\n" +
                        "1. 您使用本功能的行为应遵守您所在国家/地区的法律法规。若因您的使用行为违反相关法律法规，您应自行承担全部法律责任。\n" +
                        "2. 若因您的不当使用导致开发者面临任何索赔、诉讼或处罚，您应赔偿开发者的全部损失（包括但不限于律师费、诉讼费、赔偿金等）。\n\n" +
                        "五、特别声明：\n" +
                        "本工具为合法技术工具，开发者鼓励并仅支持您将其用于正途。若您将其用于任何违法违规用途，一切后果与开发者无关，开发者已通过本声明尽到充分告知与警示义务。",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "本工具为合法技术工具，开发者仅许可您将其用于合法、正当用途。若您将其用于任何违法违规活动，由此产生的一切法律责任与后果均由您自行承担，开发者不承担任何连带责任。",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.labelLarge,
                        lineHeight = 22.sp
                    )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                        context.getSharedPreferences("app_clone_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("clone_disclaimer_accepted", true).apply()
                        disclaimerAccepted.value = true
                    },
                        enabled = hasScrolledToBottom.value
                    ) {
                        Text("我同意")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        (context as? Activity)?.finish()
                    }) {
                        Text("我拒绝")
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            )
            return
        }

        // 克隆确认免责弹窗
        if (pendingCloneApp != null) {
            AlertDialog(
                onDismissRequest = { pendingCloneApp = null },
                title = { Text("免责声明与风险告知", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252)) },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                    Text(
                        "您即将对「${pendingCloneApp!!.appName}」创建应用分身。\n\n" +
                        "本功能仅供技术学习、安全研究、漏洞挖掘与软件兼容性测试使用。您确认并承诺：\n" +
                        "1. 您将在创建克隆应用后的 12 小时内彻底删除该克隆应用及其所有相关数据。\n" +
                        "2. 您不会利用克隆应用进行账号多开、薅取新用户福利、虚假注册、刷量刷单、欺诈、侵犯他人隐私、传播恶意软件、绕过应用内购买、规避安全检测或任何形式的违法行为。\n" +
                        "3. 您不会利用克隆应用干扰其他应用的正常服务、破坏平台秩序、侵害第三方合法权益，或违反目标应用的服务条款（TOS）与用户协议。\n" +
                        "4. 分身软件本身不违法，但利用其进行上述行为可能构成非法获取计算机信息系统数据罪、侵犯著作权罪、诈骗罪等刑事犯罪。\n" +
                        "5. 因使用本克隆应用导致的账号封禁、数据丢失、虚拟财产损失等一切后果，由您自行承担全部责任。\n" +
                        "6. 若因您的不当使用导致开发者面临任何索赔、诉讼或处罚，您应赔偿开发者的全部损失（包括但不限于律师费、诉讼费、赔偿金等）。\n" +
                        "7. 本工具开发者不对您使用本功能所产生的任何直接或间接损失承担责任。\n\n" +
                        "点击「同意并继续」即表示您已阅读并接受上述全部条款。",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "本工具为合法技术工具，开发者仅许可您将其用于合法、正当用途。若您将其用于任何违法违规活动，由此产生的一切法律责任与后果均由您自行承担，开发者不承担任何连带责任。",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.labelLarge,
                        lineHeight = 22.sp
                    )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val app = pendingCloneApp!!
                        pendingCloneApp = null
                        scope.launch {
                            LogCollector.log("信息", "点击克隆按钮: ${app.packageName}")
                            isProcessing = true; statusMsg = "正在克隆 ${app.appName}..."
                            val result = withContext(Dispatchers.IO) { cloneApp(context, app.packageName) }
                            if (result != null) {
                                clones = clones + CloneRecord(app.packageName, result, app.appName, app.icon)
                                apps = apps.filter { it.packageName != app.packageName }
                                statusMsg = "就绪"
                                Toast.makeText(context, "${app.appName} 分身创建成功", Toast.LENGTH_SHORT).show()
                                LogCollector.log("成功", "克隆完成: ${app.appName} -> $result")
                            } else {
                                statusMsg = "就绪"
                                Toast.makeText(context, "克隆失败", Toast.LENGTH_LONG).show()
                                LogCollector.log("失败", "克隆失败: ${app.appName}")
                            }
                            isProcessing = false
                        }
                    }) {
                        Text("同意并继续", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingCloneApp = null }) {
                        Text("拒绝", color = Color.Gray)
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            )
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("应用分身", color = Color.White) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A2E))) },
            containerColor = Color(0xFF0D0D1A)
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)), shape = RoundedCornerShape(10.dp)) {
                    Text(statusMsg, Modifier.padding(12.dp), color = if (isProcessing) Color(0xFFFF9800) else Color(0xFF4CAF50), style = MaterialTheme.typography.bodyLarge)
                }
                TabRow(selectedTabIndex = currentTab, containerColor = Color(0xFF1A1A2E)) {
                    Tab(currentTab == 0, { currentTab = 0 }) { Text("已分身 ${clones.size}", Modifier.padding(12.dp), color = Color.White) }
                    Tab(currentTab == 1, { currentTab = 1 }) { Text("应用列表 ${apps.size}", Modifier.padding(12.dp), color = Color.White) }
                }
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF4CAF50)) }
                } else if (currentTab == 0) {
                    if (clones.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无分身应用", color = Color.Gray, style = MaterialTheme.typography.titleMedium) }
                    } else {
                        LazyColumn(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(clones) { c ->
                                CloneCard(c,
                                    onLaunch = {
                                        val intent = pm.getLaunchIntentForPackage(c.clonePkg)
                                        if (intent != null) startActivity(intent)
                                        else Toast.makeText(context, "无法启动", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = {
                                        scope.launch {
                                            isProcessing = true
                                            withContext(Dispatchers.IO) { ShizukuUtils.executeCommand("pm uninstall ${c.clonePkg}") }
                                            clones = clones.filter { it.clonePkg != c.clonePkg }
                                            apps = apps + AppInfo(c.originalPkg, c.appName, c.icon)
                                            isProcessing = false
                                        }
                                    })
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(apps) { app ->
                            AppCard(app, onClone = { pendingCloneApp = app })
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AppCard(app: AppInfo, onClone: () -> Unit) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)), shape = RoundedCornerShape(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                app.icon?.let { Image(bitmap = it.toBitmap(48, 48).asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp)) }
                    ?: Box(Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.appName, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(app.packageName, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = onClone, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("克隆", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    @Composable
    private fun CloneCard(clone: CloneRecord, onLaunch: () -> Unit, onDelete: () -> Unit) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3A1B)), shape = RoundedCornerShape(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                clone.icon?.let { Image(bitmap = it.toBitmap(48, 48).asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp)) }
                    ?: Box(Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(clone.appName, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text("分身 · ${clone.clonePkg}", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onLaunch) { Text("启动", color = Color(0xFF64B5F6)) }
                TextButton(onClick = onDelete) { Text("删除", color = Color(0xFFFF5252)) }
            }
        }
    }

    // ========== APK Cloning Engine ==========

    // Reusable CRC32 to avoid allocation per stored entry
    private val crc32 = CRC32()

    private fun cloneApp(context: android.content.Context, pkgName: String): String? {
        val TAG = "AppClone"
        LogCollector.log("信息", "开始克隆: $pkgName")
        try {
            Log.i(TAG, "=== 开始克隆: $pkgName ===")
            Log.i(TAG, "Shizuku已安装: ${ShizukuUtils.isShizukuInstalled()}, 已授权: ${ShizukuUtils.hasShizukuPermission()}")
            
            val newPkg = pkgName + ".clone"
            LogCollector.log("信息", "pm path $pkgName")
            val pathResultRaw = ShizukuUtils.executeCommand("pm path $pkgName")
            Log.i(TAG, "pm path 结果: success=${pathResultRaw.isSuccess}, value=${pathResultRaw.getOrNull()}, error=${pathResultRaw.exceptionOrNull()?.message}")
            val pathResult = pathResultRaw.getOrNull() ?: run {
                Log.e(TAG, "!!! pm path 失败, 终止克隆")
                LogCollector.log("失败", "pm path 失败", pathResultRaw.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" })
                return null
            }
            val apkPaths = Regex("package:(.+)").findAll(pathResult).map { it.groupValues[1].trim() }.toList()
            Log.i(TAG, "APK路径列表: $apkPaths")
            if (apkPaths.isEmpty()) { Log.e(TAG, "!!! 未找到APK路径"); LogCollector.log("失败", "未找到 APK 路径"); return null }
            LogCollector.log("成功", "pm path 成功，找到 ${apkPaths.size} 个 APK")

            // Load keystore
            val ks = KeyStore.getInstance("BKS")
            context.resources.openRawResource(R.raw.clone_keystore).use { ks.load(it, "pixelclone".toCharArray()) }
            val privateKey = ks.getKey("clone", "pixelclone".toCharArray()) as PrivateKey
            val cert = ks.getCertificate("clone") as X509Certificate

            // Step 1: Copy APKs to /data/local/tmp/ via Shizuku (shell can write there)
            val tmpPath = "/data/local/tmp/clone_${pkgName.replace('.', '_')}"
            ShizukuUtils.executeCommand("mkdir -p $tmpPath")
            Log.i(TAG, "临时目录: $tmpPath")

            // Collect patched APK paths for install
            val installPaths = mutableListOf<String>()

            apkPaths.forEachIndexed { i, path ->
                val name = if (path.contains("base.apk") || apkPaths.size == 1) "base.apk" else "split_${i}.apk"
                val destPath = "$tmpPath/$name"
                LogCollector.log("信息", "cp APK: $path -> $destPath")
                val cpResult = ShizukuUtils.executeCommand("cp $path $destPath")
                Log.i(TAG, "cp $path -> $destPath: success=${cpResult.isSuccess}, error=${cpResult.exceptionOrNull()?.message}")
                if (cpResult.isFailure) {
                    LogCollector.log("失败", "cp APK 失败: $path", cpResult.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" })
                    Log.e(TAG, "!!! cp 失败"); return null
                }
                LogCollector.log("成功", "cp APK 完成: $name")

                // All APKs (base + splits) need manifest patching + re-signing.
                // Split APKs also contain a manifest with the original package name,
                // which must match the new package name used in the base APK.
                val rawFile = File(destPath)
                val outputName = if (apkPaths.size == 1) "patched.apk" else name
                val outputPath = "$tmpPath/patch_${outputName}"
                Log.i(TAG, "patch APK: $name (${rawFile.length()} bytes)")
                LogCollector.log("信息", "patch APK: $name ($pkgName -> $newPkg)")
                if (!patchApkFile(rawFile, pkgName, newPkg, privateKey, cert, outputPath)) {
                    Log.e(TAG, "!!! patchApkFile 失败: $name")
                    LogCollector.log("失败", "patch APK 失败: $name")
                    return null
                }
                LogCollector.log("成功", "patch APK 完成: $name")
                installPaths.add(outputPath)
            }

            // Step 2: Install via pm (session-based for multi-APK, simple for single)
            if (installPaths.isEmpty()) {
                Log.e(TAG, "!!! 没有可安装的 APK")
                LogCollector.log("失败", "没有可安装的 APK")
                return null
            }

            Log.i(TAG, "安装路径: $installPaths")

            val installResult = if (installPaths.size == 1) {
                LogCollector.log("信息", "pm install -r -t -d --bypass-low-target-sdk-block ${installPaths[0]}")
                ShizukuUtils.executeCommand("pm install -r -t -d --bypass-low-target-sdk-block ${installPaths[0]}")
            } else {
                // Multi-APK session install
                LogCollector.log("信息", "pm install-create (${installPaths.size} APKs)")
                val createResult = ShizukuUtils.executeCommand("pm install-create -r -t -d --bypass-low-target-sdk-block")
                Log.i(TAG, "install-create: success=${createResult.isSuccess}, value=${createResult.getOrNull()}")
                val sessionId = createResult.getOrNull()?.trim()?.let {
                    val m = Regex("\\[([0-9]+)\\]").find(it)
                    m?.groupValues?.get(1)?.toIntOrNull()
                }
                if (sessionId == null) {
                    Log.e(TAG, "!!! install-create 失败")
                    LogCollector.log("失败", "install-create 失败", createResult.getOrNull())
                    return null
                }
                Log.i(TAG, "Session ID: $sessionId")

                for ((idx, apkPath) in installPaths.withIndex()) {
                    val sz = File(apkPath).length()
                    val splitName = if (idx == 0) "base.apk" else "split_${idx}.apk"
                    val writeCmd = "cat $apkPath | pm install-write -S $sz $sessionId $splitName -"
                    Log.i(TAG, "install-write: $idx -> $apkPath ($sz bytes)")
                    val wResult = ShizukuUtils.executeCommand(writeCmd)
                    Log.i(TAG, "install-write $idx: success=${wResult.isSuccess}, error=${wResult.exceptionOrNull()?.message}")
                    if (wResult.isFailure || wResult.getOrNull()?.contains("Error") == true) {
                        Log.e(TAG, "!!! install-write $idx 失败")
                        LogCollector.log("失败", "install-write $idx 失败", wResult.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: wResult.getOrNull())
                        ShizukuUtils.executeCommand("pm install-abandon $sessionId")
                        return null
                    }
                }

                LogCollector.log("信息", "pm install-commit $sessionId")
                ShizukuUtils.executeCommand("pm install-commit $sessionId 2>&1")
            }

            Log.i(TAG, "pm install 结果: success=${installResult.isSuccess}, value=${installResult.getOrNull()}, error=${installResult.exceptionOrNull()?.message}")
            val installMsg = installResult.getOrNull() ?: run {
                Log.e(TAG, "!!! pm install 失败")
                LogCollector.log("失败", "pm install 失败", installResult.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" })
                return null
            }

            // Cleanup
            ShizukuUtils.executeCommand("rm -rf $tmpPath")
            Log.i(TAG, "=== 克隆完成: $newPkg ===")
            return if (installMsg.contains("Success") || installMsg.contains("成功")) {
                LogCollector.log("成功", "pm install 成功: $newPkg")
                newPkg
            } else {
                Log.e(TAG, "!!! 安装失败: $installMsg")
                LogCollector.log("失败", "pm install 失败: $installMsg")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "!!! 克隆异常: ${e.javaClass.simpleName}: ${e.message}", e)
            LogCollector.log("失败", "克隆异常: ${e.javaClass.simpleName}", e.message)
            return null
        }
    }

    /**
     * Stream-based APK patching: reads the source APK from disk, patches manifest/arsc
     * in-memory (these are small), and streams all other entries directly to a temp file.
     * Then signs the temp file and writes the result to outputPath.
     *
     * Avoids loading the entire APK into memory for large apps (e.g. Facebook 60MB+).
     */
    private fun patchApkFile(apkFile: File, oldPkg: String, newPkg: String, privateKey: PrivateKey, cert: X509Certificate, outputPath: String): Boolean {
        val TAG = "AppClone"
        val storedFiles = setOf("AndroidManifest.xml", "resources.arsc")
        fun shouldStore(name: String): Boolean = name in storedFiles || name.endsWith(".so")

        // Step 1: Stream through source ZIP, patch manifest/arsc, write to app cache
        val unsignedFile = File.createTempFile("unsigned", ".apk")
        try {
            java.io.FileOutputStream(unsignedFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    zos.setLevel(Deflater.BEST_COMPRESSION)
                    ZipFile(apkFile).use { zf ->
                        val entryList = zf.entries().toList()
                        for (entry in entryList) {
                            if (entry.isDirectory || entry.name.isEmpty() || entry.name.startsWith("META-INF/")) continue

                            val rawBytes = zf.getInputStream(entry).readBytes()
                            val patched = when (entry.name) {
                                "AndroidManifest.xml" -> patchManifest(rawBytes, oldPkg, newPkg)
                                "resources.arsc" -> patchArsc(rawBytes, oldPkg, newPkg)
                                else -> rawBytes
                            }

                            val isStored = shouldStore(entry.name)
                            val newEntry = java.util.zip.ZipEntry(entry.name).apply {
                                method = if (isStored) java.util.zip.ZipEntry.STORED else java.util.zip.ZipEntry.DEFLATED
                                if (isStored) {
                                    size = patched.size.toLong()
                                    compressedSize = patched.size.toLong()
                                    crc = { crc32.reset(); crc32.update(patched); crc32.value }()
                                }
                            }
                            zos.putNextEntry(newEntry)
                            zos.write(patched)
                            zos.closeEntry()
                        }
                    }
                }
            }

            // Step 2: Sign with apksig (v1 + v2) to app cache
            val signedFile = File.createTempFile("signed", ".apk")
            try {
                val signerConfig = ApkSigner.SignerConfig.Builder("clone", privateKey, listOf(cert)).build()
                ApkSigner.Builder(listOf(signerConfig))
                    .setV1SigningEnabled(true)
                    .setV2SigningEnabled(true)
                    .setV3SigningEnabled(false)
                    .setInputApk(unsignedFile)
                    .setOutputApk(signedFile)
                    .build()
                    .sign()
                // Stream signed file to outputPath via Shizuku (avoids OOM + cross-partition issues)
                val streamResult = ShizukuUtils.streamFileTo("cat > $outputPath", signedFile)
                if (streamResult.isFailure) {
                    Log.e(TAG, "!!! streamFileTo 失败: ${streamResult.exceptionOrNull()?.message}")
                    return false
                }
                return true
            } finally {
                signedFile.delete()
                unsignedFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "!!! patchApkFile 异常: ${e.javaClass.simpleName}: ${e.message}", e)
            unsignedFile.delete()
            return false
        }
    }

    /**
     * Re-signs an existing APK (split APK or any non-base APK) with the clone key.
     * Reads input from disk (e.g. /data/local/tmp/), signs to app cache,
     * then streams the result to outputPath via Shizuku to avoid OOM and cross-partition issues.
     */
    private fun signApkFile(apkFile: File, privateKey: PrivateKey, cert: X509Certificate, outputPath: String): Boolean {
        val TAG = "AppClone"
        val signedFile = File.createTempFile("signed", ".apk")
        try {
            val signerConfig = ApkSigner.SignerConfig.Builder("clone", privateKey, listOf(cert)).build()
            ApkSigner.Builder(listOf(signerConfig))
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(false)
                .setInputApk(apkFile)
                .setOutputApk(signedFile)
                .build()
                .sign()
            val streamResult = ShizukuUtils.streamFileTo("cat > $outputPath", signedFile)
            if (streamResult.isFailure) {
                Log.e(TAG, "!!! signApkFile streamFileTo 失败: ${streamResult.exceptionOrNull()?.message}")
                return false
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "!!! signApkFile 异常: ${e.javaClass.simpleName}: ${e.message}", e)
            return false
        } finally {
            signedFile.delete()
        }
    }

    /**
     * String pool entry descriptor.
     */
    private data class SpEntry(
        val index: Int,
        val absPos: Int,       // absolute start of string data in the buffer
        val relOffset: Int,    // offset stored in the offsets table
        val charLen: Int,      // character count
        val strValue: String,
        val byteLen: Int       // total byte length including header + null terminator
    )

    /**
     * Pure byte-level Binary XML StringPool patching — no ARSCLib dependency.
     *
     * Strategy:
     * 1. Find the StringPool chunk (type 0x001C0001) in the binary XML.
     * 2. Parse all strings from the pool.
     * 3. Apply whitelist: replace oldPkg with newPkg in strings that need it,
     *    but SKIP component class names (Activity/Service/Provider/Receiver)
     *    because DEX classes remain under the original package.
     * 4. Rebuild the entire StringPool chunk with new offsets, data, and sizes.
     */
    private fun patchManifest(data: ByteArray, oldPkg: String, newPkg: String): ByteArray {
        if (data.size < 40) return data

        // --- Step 1: Locate the StringPool chunk ---
        var spChunkOffset = -1
        var pos = 8
        while (pos + 8 <= data.size) {
            val chunkType = data.getIntLE(pos)
            val chunkSize = data.getIntLE(pos + 4)
            if (chunkType == 0x001C0001) {
                spChunkOffset = pos
                break
            }
            pos += chunkSize
        }
        if (spChunkOffset < 0) return data

        // --- Step 2: Parse StringPool header ---
        val stringCount  = data.getIntLE(spChunkOffset + 8)
        val styleCount   = data.getIntLE(spChunkOffset + 12)
        val flags        = data.getIntLE(spChunkOffset + 16)
        val stringsOff   = data.getIntLE(spChunkOffset + 20)  // offset from chunk start to string data
        val stylesOff    = data.getIntLE(spChunkOffset + 24)  // offset from chunk start to style data

        val isUtf8 = (flags and 0x0100) != 0

        // Header size: 28 is standard, but some tools use larger headers
        val spHeaderSize: Int
        if (styleCount > 0 && stylesOff > 0) {
            // Header ends at stylesOff or offset array end, whichever is first after offset arrays
            spHeaderSize = 28 // standard
        } else {
            spHeaderSize = 28
        }

        val offsetsBase    = spChunkOffset + spHeaderSize
        val stringDataBase = spChunkOffset + stringsOff

        // --- Step 3: Read all strings ---
        // We store: index, absolute position, relative offset, string value, byte length of encoded form
        val entries = mutableListOf<SpEntry>()

        // Auto-detect AAPT version from the first string's offset:
        // AAPT1: offset < stringsOff → offset is relative to string data start (stringDataBase)
        // AAPT2: offset >= stringsOff → offset is relative to chunk header (spChunkOffset)
        val offsetBase = if (stringCount > 0) {
            val firstRel = data.getIntLE(offsetsBase)
            if (firstRel < stringsOff) stringDataBase else spChunkOffset
        } else {
            stringDataBase
        }

        for (i in 0 until stringCount) {
            val rel = data.getIntLE(offsetsBase + i * 4)
            val absPos = offsetBase + rel
            if (absPos + 2 > data.size) break

            var charLen: Int
            var headerBytes: Int

            var utf8ByteLen = 0  // actual UTF-8 encoded byte count (only used when isUtf8)

            if (isUtf8) {
                // UTF-8 strings have TWO varint-encoded lengths: char count + byte count
                val b1 = data[absPos].toInt() and 0xFF
                val charHeaderBytes: Int
                if ((b1 and 0x80) != 0) {
                    val b2 = data[absPos + 1].toInt() and 0xFF
                    charLen = ((b1 and 0x7F) shl 8) or b2
                    charHeaderBytes = 2
                } else {
                    charLen = b1
                    charHeaderBytes = 1
                }
                // Read byte count (varint, stored right after char count)
                val bytePos = absPos + charHeaderBytes
                val bb1 = data[bytePos].toInt() and 0xFF
                val byteHeaderBytes: Int
                if ((bb1 and 0x80) != 0) {
                    val bb2 = data[bytePos + 1].toInt() and 0xFF
                    utf8ByteLen = ((bb1 and 0x7F) shl 8) or bb2
                    byteHeaderBytes = 2
                } else {
                    utf8ByteLen = bb1
                    byteHeaderBytes = 1
                }
                headerBytes = charHeaderBytes + byteHeaderBytes
            } else {
                charLen = ((data[absPos].toInt() and 0xFF) or
                          ((data[absPos + 1].toInt() and 0xFF) shl 8))
                headerBytes = 2
            }

            val charWidth = if (isUtf8) 1 else 2
            val nullTermSize = if (isUtf8) 1 else 2
            val dataLen = if (isUtf8) utf8ByteLen else charLen * 2
            if (absPos + headerBytes + dataLen > data.size) break

            val s = if (isUtf8)
                String(data, absPos + headerBytes, dataLen, Charsets.UTF_8)
            else
                String(data, absPos + 2, charLen * 2, Charsets.UTF_16LE)

            val byteLen = headerBytes + dataLen + nullTermSize
            entries.add(SpEntry(i, absPos, rel, charLen, s, byteLen))
        }

        // --- Step 4: Analyze XML tree to classify StringPool indices ---
        // classNameRefs: indices used as android:name values (class names, NO patch)
        // nonClassNameRefs: indices used in other attrs → MUTABLE byte offsets for rewriting
        val (classNameRefs, nonClassNameRefs) = analyzeXmlRefs(data, spChunkOffset)

        // --- Step 5: Classify each entry and build patch/clone plans ---
        // Standard in-place replacements (non-class-name, non-dual-use)
        val replacements = mutableMapOf<Int, String>()

        // Clone plan: for strings used as BOTH class name AND something else
        // Map: old SP index -> (new SP index, new patched string)
        val cloneMap = mutableMapOf<Int, Pair<Int, String>>()
        val cloneEntries = mutableListOf<String>()  // patched strings to append

        for (e in entries) {
            if (!e.strValue.startsWith(oldPkg)) continue

            val isClassName = e.index in classNameRefs
            val hasNonClassRef = (e.index in nonClassNameRefs) && nonClassNameRefs[e.index]!!.isNotEmpty()

            // DEBUG: trace SplitWindow entry
            if (e.strValue.contains("SplitWindow")) {
                Log.d("AppClone", "SplitWindow DEBUG idx=${e.index} str='${e.strValue}' isClassName=$isClassName hasNonClassRef=$hasNonClassRef inClassRefs=${e.index in classNameRefs} inNonClassRefs=${e.index in nonClassNameRefs}")
            }

            if (isClassName && hasNonClassRef) {
                // DUAL-USE: keep original for class name, clone for other attrs
                val replacement = e.strValue.replace(oldPkg, newPkg)
                val newIdx = stringCount + cloneEntries.size
                cloneMap[e.index] = Pair(newIdx, replacement)
                cloneEntries.add(replacement)
                Log.d("AppClone", "Manifest CLONE idx ${e.index}: '$e.strValue' → new idx $newIdx '$replacement'")
            } else if (!isClassName) {
                // Standard: non-class-name only → patch in-place
                replacements[e.index] = e.strValue.replace(oldPkg, newPkg)
                Log.d("AppClone", "Manifest PATCH idx ${e.index}: '$e.strValue' → '${replacements[e.index]}'")
            } else {
                // Class name only → skip (no patching)
                Log.d("AppClone", "Manifest SKIP  idx ${e.index}: '$e.strValue' (class name)")
            }
        }

        if (replacements.isEmpty() && cloneMap.isEmpty()) {
            Log.w("AppClone", "Manifest: no strings to patch!")
            return data
        }

        val newStringCount = stringCount + cloneEntries.size

        // --- Step 6: Rebuild StringPool with cloned entries ---
        val newStringData = java.io.ByteArrayOutputStream()
        val newOffsets = IntArray(newStringCount)
        val newStyleData: ByteArray?
        var dataPos = 0

        // Original entries (0 .. stringCount-1)
        for (i in 0 until stringCount) {
            newOffsets[i] = dataPos
            val replacement = replacements[i]
            val encoded: ByteArray
            if (replacement != null) {
                encoded = encodePoolString(replacement, isUtf8)
            } else {
                val e = entries.find { it.index == i }!!
                encoded = data.copyOfRange(e.absPos, e.absPos + e.byteLen)
            }
            newStringData.write(encoded)
            dataPos += encoded.size
        }

        // Cloned entries (stringCount .. newStringCount-1)
        for (i in 0 until cloneEntries.size) {
            newOffsets[stringCount + i] = dataPos
            val encoded = encodePoolString(cloneEntries[i], isUtf8)
            newStringData.write(encoded)
            dataPos += encoded.size
        }

        val newStrDataBytes = newStringData.toByteArray()

        // Preserve style data if present
        newStyleData = if (styleCount > 0 && stylesOff > 0) {
            val styleDataStart = spChunkOffset + stylesOff
            val styleDataEnd = spChunkOffset + data.getIntLE(spChunkOffset + 4)
            if (styleDataEnd > styleDataStart) data.copyOfRange(styleDataStart, styleDataEnd) else ByteArray(0)
        } else {
            ByteArray(0)
        }

        // --- Step 7: Assemble new StringPool chunk ---
        val newStringsOff = spHeaderSize + newStringCount * 4 + styleCount * 4
        val newStylesOff = if (newStyleData.isNotEmpty()) newStringsOff + newStrDataBytes.size else 0

        val unalignedSize = if (newStyleData.isNotEmpty()) {
            newStylesOff + newStyleData.size
        } else {
            newStringsOff + newStrDataBytes.size
        }
        val padding = (4 - unalignedSize % 4) % 4
        val newChunkSize = unalignedSize + padding

        val chunkBuf = java.nio.ByteBuffer.allocate(newChunkSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)

        chunkBuf.putInt(0x001C0001)
        chunkBuf.putInt(newChunkSize)
        chunkBuf.putInt(newStringCount)      // updated string count
        chunkBuf.putInt(styleCount)
        chunkBuf.putInt(flags)
        chunkBuf.putInt(newStringsOff)
        chunkBuf.putInt(newStylesOff)

        for (i in 0 until newStringCount) {
            chunkBuf.putInt(newOffsets[i])
        }

        if (styleCount > 0) {
            val oldStyleOffsetsStart = offsetsBase + stringCount * 4
            for (i in 0 until styleCount) {
                chunkBuf.putInt(data.getIntLE(oldStyleOffsetsStart + i * 4))
            }
        }

        chunkBuf.put(newStrDataBytes)

        if (newStyleData.isNotEmpty()) {
            chunkBuf.put(newStyleData)
        }

        for (i in 0 until padding) {
            chunkBuf.put(0)
        }

        val newChunkBytes = chunkBuf.array()

        // --- Step 8: Replace old StringPool chunk + build result ---
        val oldChunkSize = data.getIntLE(spChunkOffset + 4)
        val fileSize = data.getIntLE(4)
        val sizeDiff = newChunkSize - oldChunkSize

        val result = java.io.ByteArrayOutputStream()
        result.write(data, 0, spChunkOffset)
        result.write(newChunkBytes)
        result.write(data, spChunkOffset + oldChunkSize, data.size - spChunkOffset - oldChunkSize)
        var newData = result.toByteArray()

        // Update file-level size
        setIntLE(newData, 4, fileSize + sizeDiff)
        newData = fixChunkSize(newData)

        // --- Step 9: Patch XML attribute references for cloned strings ---
        // Offsets collected in nonClassNameRefs are relative to original data.
        // After StringPool replacement, offsets after the SP chunk shift by sizeDiff.
        for ((oldIdx, byteOffsets) in nonClassNameRefs) {
            val (newIdx, _) = cloneMap[oldIdx] ?: continue
            for (xmlOffset in byteOffsets) {
                // Only offsets after the StringPool chunk need adjustment
                val adjustedOffset = if (xmlOffset > spChunkOffset + oldChunkSize) {
                    xmlOffset + sizeDiff
                } else {
                    xmlOffset
                }
                if (adjustedOffset + 4 <= newData.size) {
                    val current = newData.getIntLE(adjustedOffset)
                    if (current == oldIdx) {
                        setIntLE(newData, adjustedOffset, newIdx)
                        Log.d("AppClone", "  XML ref patched: offset ${xmlOffset}→$adjustedOffset, idx $oldIdx→$newIdx")
                    }
                }
            }
        }

        return newData
    }

    /**
     * Single-pass XML tree analysis: identifies which StringPool indices are:
     * 1. Class name references (android:name on component elements, targetActivity, etc.) → must NOT be patched
     * 2. Non-class-name references that start with oldPkg → should be patched, keyed by byte offset
     *
     * Returns: Pair(classNameRefs, nonClassNameRefs)
     *   classNameRefs: Set of StringPool indices used as component-class-name attribute values
     *   nonClassNameRefs: Map of StringPool index → list of byte offsets in data array
     *                     (offsets point to 4-byte int32 StringPool index values in XML attributes)
     */
    private fun analyzeXmlRefs(data: ByteArray, spChunkOffset: Int): Pair<Set<Int>, Map<Int, MutableList<Int>>> {
        val classRefs = mutableSetOf<Int>()
        val nonClassRefs = mutableMapOf<Int, MutableList<Int>>()

        // Attributes that ALWAYS reference class names (regardless of element type)
        val ALWAYS_CLASS_ATTRS = setOf("targetActivity", "targetPackage", "parentActivityName")

        // "name" attribute is treated as a class name by default (safe).
        // EXCEPTION: these elements have "name" as a permission/identifier, NOT a class name,
        // so they MUST be patched (e.g. com.tencent.mm.permission.C2D_MESSAGE → .clone).
        val NON_CLASS_NAME_ELEMENTS = setOf(
            "permission", "uses-permission", "permission-group", "permission-tree"
        )

        // Step A: Skip over StringPool chunk to find StartElement chunks
        var pos = spChunkOffset + data.getIntLE(spChunkOffset + 4)

        // Step B: Walk XML StartElement chunks
        while (pos + 36 <= data.size) {
            val chunkType = data.getShortLE(pos).toInt() and 0xFFFF
            val headerSize = data.getShortLE(pos + 2).toInt() and 0xFFFF
            val chunkSize = data.getIntLE(pos + 4)

            if (chunkType != 0x0102) {
                pos += chunkSize
                continue
            }

            // Read element tag name from StringPool (offset pos+20 = element name SP index)
            val elementNameSpIdx = data.getIntLE(pos + 20)
            val elementName = readPoolStringByIndex(data, spChunkOffset, elementNameSpIdx)
            val isNonClassNameElement = elementName in NON_CLASS_NAME_ELEMENTS

            val attrStart = data.getShortLE(pos + 24).toInt() and 0xFFFF
            val attrCount = data.getShortLE(pos + 28).toInt() and 0xFFFF

            val attrsOffset = pos + headerSize + attrStart
            for (a in 0 until attrCount) {
                val attrOff = attrsOffset + a * 20
                if (attrOff + 20 > data.size) break

                val attrNameSpIdx = data.getIntLE(attrOff + 4)
                // Decode attribute name string from StringPool (robust for AAPT1/AAPT2)
                val attrName = readPoolStringByIndex(data, spChunkOffset, attrNameSpIdx)

                // "name" is a class-name by default (safe for all elements).
                // Only non-class-name elements (permission, etc.) get their "name" patched.
                val isClassNameAttr = attrName in ALWAYS_CLASS_ATTRS ||
                    (attrName == "name" && !isNonClassNameElement)

                // Check rawValue (offset +8): StringPool index of string value (-1 = none)
                val rawValueIdx = data.getIntLE(attrOff + 8)
                val rawValueOffset = attrOff + 8

                // Check typedValue (offset +12): for TYPE_STRING (0x03), data field is SP index
                val tvType = data[attrOff + 15].toInt() and 0xFF
                val tvData = data.getIntLE(attrOff + 16)
                val tvDataOffset = attrOff + 16

                if (isClassNameAttr) {
                    // Class name attribute → record as class ref
                    if (rawValueIdx != -1) classRefs.add(rawValueIdx)
                    if (tvType == 0x03 && tvData >= 0) classRefs.add(tvData)
                } else {
                    // Non-class-name attribute → record offset for potential patching
                    if (rawValueIdx != -1) {
                        nonClassRefs.getOrPut(rawValueIdx) { mutableListOf() }.add(rawValueOffset)
                    }
                    if (tvType == 0x03 && tvData >= 0) {
                        // Always record typedValue offset, even if same index as rawValue
                        // (both must be updated for correctness)
                        nonClassRefs.getOrPut(tvData) { mutableListOf() }.add(tvDataOffset)
                    }
                }
            }

            pos += chunkSize
        }

        Log.d("AppClone", "analyzeXmlRefs: classRefs=${classRefs.size}, nonClassRefs keys=${nonClassRefs.size}")
        Log.d("AppClone", "analyzeXmlRefs: 2478 in classRefs=${2478 in classRefs}, in nonClassRefs=${2478 in nonClassRefs}")
        return Pair(classRefs, nonClassRefs)
    }

    /**
     * Read a single string from the StringPool by index.
     * Handles both UTF-8 (flags & 0x0100) and UTF-16LE encoding.
     * For AAPT2 bag-encoded strings, returns only the first null-delimited segment
     * so that "name\0name" collapses to "name".
     */
    private fun readPoolStringByIndex(data: ByteArray, spChunkOffset: Int, index: Int): String {
        val stringCount = data.getIntLE(spChunkOffset + 8)
        if (index < 0 || index >= stringCount) return ""

        val flags = data.getIntLE(spChunkOffset + 16)
        val stringsOff = data.getIntLE(spChunkOffset + 20)
        val isUtf8 = (flags and 0x0100) != 0

        val offsetsBase = spChunkOffset + 28 // standard header
        val stringDataBase = spChunkOffset + stringsOff

        // Auto-detect offset base (AAPT1 vs AAPT2)
        val firstRel = data.getIntLE(offsetsBase)
        val offsetBase = if (firstRel < stringsOff) stringDataBase else spChunkOffset

        val rel = data.getIntLE(offsetsBase + index * 4)
        val absPos = offsetBase + rel
        if (absPos + 2 > data.size) return ""

        var charLen: Int
        var headerBytes: Int
        var dataLen: Int
        val fullStr: String

        if (isUtf8) {
            val b1 = data[absPos].toInt() and 0xFF
            if ((b1 and 0x80) != 0) {
                val b2 = data[absPos + 1].toInt() and 0xFF
                charLen = ((b1 and 0x7F) shl 8) or b2
                headerBytes = 2
            } else {
                charLen = b1
                headerBytes = 1
            }
            // Read byte count varint
            val bytePos = absPos + headerBytes
            val bb1 = data[bytePos].toInt() and 0xFF
            if ((bb1 and 0x80) != 0) {
                val bb2 = data[bytePos + 1].toInt() and 0xFF
                dataLen = ((bb1 and 0x7F) shl 8) or bb2
                headerBytes += 2
            } else {
                dataLen = bb1
                headerBytes += 1
            }
            if (absPos + headerBytes + dataLen > data.size) return ""
            fullStr = String(data, absPos + headerBytes, dataLen, Charsets.UTF_8)
        } else {
            charLen = ((data[absPos].toInt() and 0xFF) or
                      ((data[absPos + 1].toInt() and 0xFF) shl 8))
            dataLen = charLen * 2
            headerBytes = 2
            if (absPos + headerBytes + dataLen > data.size) return ""
            fullStr = String(data, absPos + headerBytes, dataLen, Charsets.UTF_16LE)
        }

        // For AAPT2 bag encoding, return only the first null-terminated segment.
        // "name\u0000name" → "name"
        val nullIdx = fullStr.indexOf('\u0000')
        return if (nullIdx >= 0) fullStr.substring(0, nullIdx) else fullStr
    }

    /**
     * Encode a Java string into Binary XML StringPool format.
     * UTF-8:  leb128 char count + UTF-8 bytes + null byte
     * UTF-16: uint16 char count + UTF-16LE bytes + null (2 bytes)
     */
    private fun encodePoolString(str: String, isUtf8: Boolean): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        if (isUtf8) {
            val utf8Bytes = str.toByteArray(Charsets.UTF_8)
            val charCount = str.length
            val byteCount = utf8Bytes.size
            // Write char count as leb128
            if (charCount < 0x80) {
                out.write(charCount)
            } else {
                out.write((charCount and 0x7F) or 0x80)
                out.write((charCount shr 7) and 0xFF)
            }
            // Write byte count as leb128 (MUST have for UTF-8 StringPool)
            if (byteCount < 0x80) {
                out.write(byteCount)
            } else {
                out.write((byteCount and 0x7F) or 0x80)
                out.write((byteCount shr 7) and 0xFF)
            }
            out.write(utf8Bytes)
            out.write(0) // null terminator
        } else {
            val utf16Bytes = str.toByteArray(Charsets.UTF_16LE)
            out.write(java.nio.ByteBuffer.allocate(2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .putShort(str.length.toShort()).array())
            out.write(utf16Bytes)
            out.write(0)
            out.write(0) // null terminator
        }
        return out.toByteArray()
    }

    /**
     * Patch resources.arsc using ARSCLib.
     */
    private fun patchArsc(data: ByteArray, oldPkg: String, newPkg: String): ByteArray {
        try {
            val table = com.reandroid.lib.arsc.chunk.TableBlock.load(java.io.ByteArrayInputStream(data))
            for (pkg in table.listPackages()) {
                if (pkg.name == oldPkg) {
                    pkg.name = newPkg
                    Log.i("AppClone", "ARSC patched: '$oldPkg' → '$newPkg'")
                }
            }
            return fixChunkSize(table.bytes)
        } catch (e: Exception) {
            Log.e("AppClone", "ARSC patch failed (ARSCLib may be stripped by ProGuard), falling back to byte-level search-replace: ${e.message}")
            // Fallback: byte-level search & replace (only works when lengths match)
            val oldBytes = oldPkg.toByteArray(Charsets.UTF_8)
            val newBytes = newPkg.toByteArray(Charsets.UTF_8)
            if (newBytes.size != oldBytes.size) {
                Log.w("AppClone", "ARSC fallback: package name length mismatch (old=${oldBytes.size}B, new=${newBytes.size}B), cannot patch via byte-level search-replace. ARSC data returned unchanged — install may fail due to package name mismatch.")
                return data
            }
            val result = data.copyOf()
            var i = 0
            while (i <= result.size - oldBytes.size) {
                if (oldBytes.indices.all { j -> result[i + j] == oldBytes[j] }) {
                    System.arraycopy(newBytes, 0, result, i, newBytes.size)
                    i += newBytes.size
                } else i++
            }
            return result
        }
    }

    /**
     * Post-process a ZIP byte array to 4-byte align STORED entries.
     * Inserts zero-padding into extra fields of misaligned STORED entries,
     * and updates all local headers, central directory, and EOCD accordingly.
     */
    private fun realignZip(zipBytes: ByteArray): ByteArray {
        val TAG = "AppClone"

        // Find EOCD
        var eocdOff = -1
        for (i in zipBytes.size - 22 downTo maxOf(0, zipBytes.size - 65557)) {
            if (zipBytes[i] == 0x50.toByte() && zipBytes[i + 1] == 0x4b.toByte() &&
                zipBytes[i + 2] == 0x05.toByte() && zipBytes[i + 3] == 0x06.toByte()
            ) {
                eocdOff = i; break
            }
        }
        if (eocdOff < 0) return zipBytes

        val buf = java.nio.ByteBuffer.wrap(zipBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val cdOff = buf.getInt(eocdOff + 16)

        data class Pad(val lfhOff: Int, val dataOff: Int, val amount: Int)

        // Scan local file headers for misaligned STORED entries
        val pads = mutableListOf<Pad>()
        var pos = 0
        while (pos + 30 <= cdOff) {
            if (buf.getInt(pos) != 0x04034b50.toInt()) break
            val method = buf.getShort(pos + 8).toInt() and 0xFFFF
            val compSize = buf.getInt(pos + 18)
            val fnLen = buf.getShort(pos + 26).toInt() and 0xFFFF
            val exLen = buf.getShort(pos + 28).toInt() and 0xFFFF
            val dOff = pos + 30 + fnLen + exLen
            if (method == 0 && dOff % 4 != 0) {
                pads.add(Pad(pos, dOff, 4 - dOff % 4))
            }
            pos = dOff + compSize
        }
        if (pads.isEmpty()) return zipBytes

        val totalPad = pads.sumOf { it.amount }

        // ---- Part 1: Data section (before CD) with padding inserted ----
        val dataOS = java.io.ByteArrayOutputStream()
        var src = 0
        for (p in pads) {
            dataOS.write(zipBytes, src, p.dataOff - src)
            dataOS.write(ByteArray(p.amount))
            src = p.dataOff
        }
        dataOS.write(zipBytes, src, cdOff - src)
        val dataBytes = dataOS.toByteArray()

        // Fix local header extra field lengths in dataBytes
        val dBuf = java.nio.ByteBuffer.wrap(dataBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (p in pads) {
            val oldExLen = buf.getShort(p.lfhOff + 28).toInt() and 0xFFFF
            dBuf.putShort(p.lfhOff + 28, (oldExLen + p.amount).toShort())
        }

        // ---- Part 2: Central Directory ----
        data class CdEntry(
            val origOff: Int, val entrySize: Int, val lhOff: Int,
            val extraOff: Int, val extraLen: Int, val padAmt: Int
        )
        val cdEntries = mutableListOf<CdEntry>()
        pos = cdOff
        while (pos + 46 <= eocdOff) {
            if (buf.getInt(pos) != 0x02014b50.toInt()) break
            val fnLen = buf.getShort(pos + 28).toInt() and 0xFFFF
            val exLen = buf.getShort(pos + 30).toInt() and 0xFFFF
            val cmLen = buf.getShort(pos + 32).toInt() and 0xFFFF
            val lhOff = buf.getInt(pos + 42)
            val entrySz = 46 + fnLen + exLen + cmLen
            val exOff = pos + 46 + fnLen
            val pad = pads.find { it.lfhOff == lhOff }?.amount ?: 0
            cdEntries.add(CdEntry(pos, entrySz, lhOff, exOff, exLen, pad))
            pos += entrySz
        }

        val cdOS = java.io.ByteArrayOutputStream()
        for (ce in cdEntries) {
            val prefixLen = ce.extraOff - ce.origOff + ce.extraLen
            cdOS.write(zipBytes, ce.origOff, prefixLen)
            if (ce.padAmt > 0) cdOS.write(ByteArray(ce.padAmt))
            val cmOff = ce.extraOff + ce.extraLen
            val cmLen = buf.getShort(ce.origOff + 32).toInt() and 0xFFFF
            if (cmLen > 0) cdOS.write(zipBytes, cmOff, cmLen)
        }
        val cdBytes = cdOS.toByteArray()

        // Fix CD extra field lengths + local header offsets
        val cBuf = java.nio.ByteBuffer.wrap(cdBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        var cdPos = 0
        for (ce in cdEntries) {
            cBuf.putShort(cdPos + 30, (ce.extraLen + ce.padAmt).toShort())
            val lhShift = pads.filter { it.lfhOff < ce.lhOff }.sumOf { it.amount }
            cBuf.putInt(cdPos + 42, ce.lhOff + lhShift)
            cdPos += 46 + (buf.getShort(ce.origOff + 28).toInt() and 0xFFFF) +
                     ce.extraLen + ce.padAmt + (buf.getShort(ce.origOff + 32).toInt() and 0xFFFF)
        }

        // ---- Part 3: EOCD ----
        val eocdOS = java.io.ByteArrayOutputStream()
        val cmLen = buf.getShort(eocdOff + 20).toInt() and 0xFFFF
        eocdOS.write(zipBytes, eocdOff, 22 + cmLen)
        val eocdBytes = eocdOS.toByteArray()
        val eBuf = java.nio.ByteBuffer.wrap(eocdBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        eBuf.putInt(16, cdOff + totalPad)

        // ---- Combine ----
        val result = java.io.ByteArrayOutputStream()
        result.write(dataBytes)
        result.write(cdBytes)
        result.write(eocdBytes)

        Log.d(TAG, "realignZip: aligned ${pads.size} STORED entries, total padding $totalPad bytes")
        return result.toByteArray()
    }

    private fun ByteArray.getIntLE(pos: Int): Int =
        (this[pos].toInt() and 0xFF) or ((this[pos+1].toInt() and 0xFF) shl 8) or ((this[pos+2].toInt() and 0xFF) shl 16) or ((this[pos+3].toInt() and 0xFF) shl 24)

    private fun ByteArray.getShortLE(pos: Int): Short =
        ((this[pos].toInt() and 0xFF) or ((this[pos+1].toInt() and 0xFF) shl 8)).toShort()

    private fun setIntLE(buf: ByteArray, pos: Int, value: Int) {
        buf[pos] = (value and 0xFF).toByte()
        buf[pos+1] = ((value shr 8) and 0xFF).toByte()
        buf[pos+2] = ((value shr 16) and 0xFF).toByte()
        buf[pos+3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun fixChunkSize(bytes: ByteArray): ByteArray {
        val buf = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val chunkSize = buf.getInt(4)
        if (chunkSize != bytes.size) {
            buf.putInt(4, bytes.size)
            Log.d("AppClone", "Chunk size fixed: $chunkSize → ${bytes.size}")
        }
        return bytes
    }
}
