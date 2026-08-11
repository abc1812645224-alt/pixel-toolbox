package com.example.pixeltoolbox.ui.system

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap

data class AppItem(val name: String, val packageName: String, val icon: ImageBitmap? = null)

suspend fun loadInstalledApps(pm: PackageManager): List<AppItem> = withContext(Dispatchers.IO) {
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    apps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        .map { 
            val bmp = runCatching { it.loadIcon(pm).toBitmap().asImageBitmap() }.getOrNull()
            AppItem(it.loadLabel(pm).toString(), it.packageName, bmp) 
        }
        .sortedBy { it.name }
}
