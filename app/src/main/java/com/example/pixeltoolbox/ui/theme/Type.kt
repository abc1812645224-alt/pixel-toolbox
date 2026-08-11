@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.example.pixeltoolbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 基础字号定义，以 Compact 设备（Pixel 9a / 9 Pro）为基准。
 *
 * 字号映射：
 *   displayLarge   - 超大字（如气压计数值）
 *   headlineMedium - 页面主标题
 *   titleLarge     - 卡片标题
 *   titleMedium    - 次级标题
 *   bodyLarge      - 正文 / 副标题强调
 *   bodyMedium     - 说明文字
 *   bodySmall      - 辅助注释
 *   labelLarge     - 按钮 / 强调标签
 *   labelMedium    - 普通标签 / 次要按钮
 *   labelSmall     - 小标签
 */
private val CompactTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.sp),
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private val MediumTypography = CompactTypography.copy(
    displayLarge = CompactTypography.displayLarge.copy(fontSize = 36.sp),
    headlineMedium = CompactTypography.headlineMedium.copy(fontSize = 24.sp),
    titleLarge = CompactTypography.titleLarge.copy(fontSize = 20.sp),
    titleMedium = CompactTypography.titleMedium.copy(fontSize = 17.sp),
    bodyLarge = CompactTypography.bodyLarge.copy(fontSize = 15.sp),
    bodyMedium = CompactTypography.bodyMedium.copy(fontSize = 14.sp),
    bodySmall = CompactTypography.bodySmall.copy(fontSize = 13.sp),
    labelLarge = CompactTypography.labelLarge.copy(fontSize = 15.sp),
    labelMedium = CompactTypography.labelMedium.copy(fontSize = 13.sp),
    labelSmall = CompactTypography.labelSmall.copy(fontSize = 12.sp),
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private val ExpandedTypography = CompactTypography.copy(
    displayLarge = CompactTypography.displayLarge.copy(fontSize = 40.sp),
    headlineMedium = CompactTypography.headlineMedium.copy(fontSize = 26.sp),
    titleLarge = CompactTypography.titleLarge.copy(fontSize = 22.sp),
    titleMedium = CompactTypography.titleMedium.copy(fontSize = 19.sp),
    bodyLarge = CompactTypography.bodyLarge.copy(fontSize = 16.sp),
    bodyMedium = CompactTypography.bodyMedium.copy(fontSize = 15.sp),
    bodySmall = CompactTypography.bodySmall.copy(fontSize = 14.sp),
    labelLarge = CompactTypography.labelLarge.copy(fontSize = 16.sp),
    labelMedium = CompactTypography.labelMedium.copy(fontSize = 14.sp),
    labelSmall = CompactTypography.labelSmall.copy(fontSize = 13.sp),
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun responsiveTypography(windowWidthSizeClass: WindowWidthSizeClass): Typography {
    return remember(windowWidthSizeClass) {
        when (windowWidthSizeClass) {
            WindowWidthSizeClass.Compact -> CompactTypography
            WindowWidthSizeClass.Medium -> MediumTypography
            WindowWidthSizeClass.Expanded -> ExpandedTypography
            else -> CompactTypography
        }
    }
}
