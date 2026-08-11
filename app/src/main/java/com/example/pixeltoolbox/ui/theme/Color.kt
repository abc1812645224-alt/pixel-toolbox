/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.example.pixeltoolbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Material Design legacy (keep for compat) ──
// Deep green used by call-recording notifications (ported from ShizuCallRecorder)
val Green40 = Color(0xFF386B20)
val md_theme_light_primary = Color(0xFF006C4C)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF89F8C7)
val md_theme_light_onPrimaryContainer = Color(0xFF002114)
val md_theme_dark_primary = Color(0xFF6CDBAC)
val md_theme_dark_onPrimary = Color(0xFF003826)
val md_theme_dark_primaryContainer = Color(0xFF005239)
val md_theme_dark_onPrimaryContainer = Color(0xFF89F8C7)

// ── iOS 27 Color Palette (Light) ──
private val LightBlue = Color(0xFF007AFF)
private val LightGreen = Color(0xFF34C759)
private val LightRed = Color(0xFFFF3B30)
private val LightOrange = Color(0xFFFF9500)

private val LightLabel = Color(0xFF000000)
private val LightSecondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.6f)
private val LightTertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.3f)
private val LightNavUnselected = Color(0xFF555557)

private val LightBackground = Color(0xFFF2F2F7)
private val LightCardBackground = Color(0xFFFFFFFF)
private val LightSeparator = Color(0xFF3C3C43).copy(alpha = 0.2f)
private val LightFill = Color(0xFFF2F4F7)

private val LightGlassWhite = Color.White.copy(alpha = 0.72f)
private val LightGlassBorder = Color(0xFFE5E5EA)

// ── iOS 27 Color Palette (Dark) ──
private val DarkBlue = Color(0xFF0A84FF)
private val DarkGreen = Color(0xFF30D158)
private val DarkRed = Color(0xFFFF453A)
private val DarkOrange = Color(0xFFFF9F0A)

private val DarkLabel = Color.White
private val DarkSecondaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.6f)
private val DarkTertiaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.3f)
private val DarkNavUnselected = Color(0xFF98989D)

private val DarkBackground = Color(0xFF000000)
private val DarkCardBackground = Color(0xFF1C1C1E)
private val DarkSeparator = Color(0xFF545458).copy(alpha = 0.6f)
private val DarkFill = Color(0xFF2C2C2E)

private val DarkGlassWhite = Color.White.copy(alpha = 0.08f)
private val DarkGlassBorder = Color(0xFF38383A)

// ── Composable color getters (automatically follow system dark mode) ──
val iOSBlue: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkBlue else LightBlue
val iOSGreen: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkGreen else LightGreen
val iOSRed: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkRed else LightRed
val iOSOrange: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkOrange else LightOrange

val iOSLabel: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkLabel else LightLabel
val iOSSecondaryLabel: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkSecondaryLabel else LightSecondaryLabel
val iOSTertiaryLabel: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkTertiaryLabel else LightTertiaryLabel
val iOSNavUnselected: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkNavUnselected else LightNavUnselected

val iOSBackground: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkBackground else LightBackground
val iOSCardBackground: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkCardBackground else LightCardBackground
val iOSSeparator: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkSeparator else LightSeparator
val iOSFill: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkFill else LightFill

val iOSGlassWhite: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkGlassWhite else LightGlassWhite
val iOSGlassBorder: Color
    @Composable get() = if (isSystemInDarkTheme()) DarkGlassBorder else LightGlassBorder
