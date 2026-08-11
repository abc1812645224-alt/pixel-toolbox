package com.example.pixeltoolbox.ui.custom

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import com.example.pixeltoolbox.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.MaterialTheme

class AppListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isExtractMode = intent.getStringExtra("MODE") == "EXTRACT"
        setContent {
            PixelToolboxTheme {
                AppListScreen(isExtractMode = isExtractMode, onBack = { finish() })
            }
        }
    }
}

data class AppItem(
    val name: String,
    val packageName: String,
    val sourceDir: String,
    val isSystem: Boolean,
    var isFrozen: Boolean,
    val icon: Drawable?
)

@Composable
fun AppListScreen(isExtractMode: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pm = context.packageManager

    var fullAppList by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showOnlyFrozen by remember { mutableStateOf(false) }
    var processingApp by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val list = mutableListOf<AppItem>()
            for (appInfo in installedApps) {
                if (appInfo.packageName == context.packageName) continue
                
                val name = appInfo.loadLabel(pm).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val icon = try { appInfo.loadIcon(pm) } catch (e: Exception) { null }
                val isFrozen = !appInfo.enabled
                
                list.add(AppItem(name, appInfo.packageName, appInfo.sourceDir, isSystem, isFrozen, icon))
            }
            
            list.sortWith(compareBy({ it.isSystem }, { it.name.lowercase() }))
            
            withContext(Dispatchers.Main) {
                fullAppList = list
                isLoading = false
            }
        }
    }

    val displayList = remember(fullAppList, showOnlyFrozen) {
        if (showOnlyFrozen) fullAppList.filter { it.isFrozen } else fullAppList
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
                text = if (isExtractMode) "应用 APK 提取器" else "极客冰箱",
                style = MaterialTheme.typography.headlineMedium,
                color = iOSLabel
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isExtractMode) "点击任意应用即可将其安装包提取到 Download 目录" else "点击任意应用即可冻结或解冻",
                style = MaterialTheme.typography.bodyMedium,
                color = iOSSecondaryLabel
            )
            
            if (!isExtractMode) {
                Spacer(modifier = Modifier.height(16.dp))
                iOSOutlineButton(
                    onClick = { showOnlyFrozen = !showOnlyFrozen },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                ) {
                    Text(if (showOnlyFrozen) "查看全部应用" else "只看已冻结应用", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = iOSBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                items(displayList, key = { it.packageName }) { app ->
                    val isProcessing = processingApp == app.packageName
                    
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .alpha(if (!isExtractMode && app.isFrozen) 0.6f else 1.0f)
                            .clickable(enabled = !isProcessing) {
                                if (isExtractMode) {
                                    processingApp = app.packageName
                                    coroutineScope.launch {
                                        val cleanName = app.name.replace(" ", "_").replace("/", "_")
                                        val dest = "/sdcard/Download/${cleanName}_${app.packageName}.apk"
                                        val cmd = "cp '${app.sourceDir}' '$dest' && chmod 644 '$dest'"
                                        
                                        val result = withContext(Dispatchers.IO) {
                                            ShizukuUtils.executeCommand(cmd)
                                        }
                                        processingApp = null
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "已提取到 Download 目录", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "提取失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    processingApp = app.packageName
                                    coroutineScope.launch {
                                        val targetFrozenState = !app.isFrozen
                                        val cmd = "pm ${if (targetFrozenState) "disable-user" else "enable"} --user 0 ${app.packageName}"
                                        
                                        val result = withContext(Dispatchers.IO) {
                                            ShizukuUtils.executeCommand(cmd)
                                        }
                                        processingApp = null
                                        if (result.isSuccess) {
                                            val newList = fullAppList.toMutableList()
                                            val index = newList.indexOfFirst { it.packageName == app.packageName }
                                            if (index != -1) {
                                                newList[index] = newList[index].copy(isFrozen = targetFrozenState)
                                                fullAppList = newList
                                            }
                                            Toast.makeText(context, "${app.name} ${if (targetFrozenState) "已冻结" else "已解冻"}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "操作失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                                    contentDescription = app.name,
                                    modifier = Modifier.size(48.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(48.dp).background(iOSSeparator, RoundedCornerShape(8.dp))
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = iOSLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${if (app.isSystem) "系统" else "用户"} · ${app.packageName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = iOSSecondaryLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = iOSBlue, strokeWidth = 2.dp)
                            } else {
                                val statusText = if (isExtractMode) "提取" else if (app.isFrozen) "已冻结" else "正常"
                                val statusColor = if (isExtractMode || app.isFrozen) iOSBlue else iOSGreen
                                val bgColor = if (isExtractMode || app.isFrozen) Color(0xFFE6F2FA) else Color(0xFFE8F5E9)
                                
                                Box(
                                    modifier = Modifier
                                        .background(bgColor, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
