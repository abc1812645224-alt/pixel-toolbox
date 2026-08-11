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

@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.example.pixeltoolbox.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun PixelToolboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF0A84FF),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF1A3448),
            onPrimaryContainer = Color(0xFFD6E4FF),
            secondary = Color(0xFF30D158),
            onSecondary = Color.Black,
            background = Color(0xFF000000),
            onBackground = Color.White,
            surface = Color(0xFF1C1C1E),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2C2C2E),
            onSurfaceVariant = Color(0xFFEBEBF5).copy(alpha = 0.6f),
            outline = Color(0xFF545458).copy(alpha = 0.6f),
        )
    } else {
        lightColorScheme(
            primary = iOSBlue,
            onPrimary = Color.White,
            primaryContainer = iOSBlue.copy(alpha = 0.12f),
            onPrimaryContainer = iOSBlue,
            secondary = iOSGreen,
            onSecondary = Color.White,
            background = iOSBackground,
            onBackground = iOSLabel,
            surface = iOSCardBackground,
            onSurface = iOSLabel,
            surfaceVariant = iOSBackground,
            onSurfaceVariant = iOSSecondaryLabel,
            outline = iOSSeparator,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = responsiveTypography(windowWidthSizeClass),
        content = content
    )
}
