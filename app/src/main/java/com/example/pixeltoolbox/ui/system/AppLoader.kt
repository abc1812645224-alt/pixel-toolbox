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
