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

package com.example.pixeltoolbox.widget

import java.util.Calendar

object LunarDateUtils {

    data class DashboardDateInfo(
        val solarMonthYear: String, // e.g. "JULY 2026年"
        val lunarDate: String, // e.g. "农历六月十六"
        val lunarYearAnimal: String, // e.g. "二〇二六年 / 马年"
        val holidayInfo: String, // e.g. "今日：无节日"
        val weekDayInfo: String, // e.g. "今天是第31周 / 第210天"
        val largeDay: String, // e.g. "29"
        val greeting: String, // e.g. "Good 晚上好!"
        val weekDays: List<String>, // MON, TUE...
        val weekDates: List<Int>, // 27, 28, 29...
        val currentDayIndex: Int // 0-6 index for highlighting
    )

    fun getDashboardInfo(): DashboardDateInfo {
        val calendar = Calendar.getInstance()

        // Solar
        val monthNames = arrayOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")
        val solarMonthYear = "${monthNames[calendar.get(Calendar.MONTH)]} ${calendar.get(Calendar.YEAR)}年"
        val largeDay = calendar.get(Calendar.DAY_OF_MONTH).toString()

        // Real Lunar Calculation using Android ICU
        val cc = android.icu.util.ChineseCalendar(calendar.time)
        val isLeapMonth = cc.get(android.icu.util.ChineseCalendar.IS_LEAP_MONTH) == 1
        val lunarMonthNum = cc.get(android.icu.util.ChineseCalendar.MONTH) // 0-based
        val lunarDayNum = cc.get(android.icu.util.ChineseCalendar.DATE) // 1-based
        val cycleYear = cc.get(android.icu.util.ChineseCalendar.YEAR) // 1-60 cycle
        
        val lunarMonths = arrayOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
        val lunarDays = arrayOf(
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
        )
        val branches = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
        val zodiacs = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
        
        val branchIndex = (cycleYear - 1) % 12
        val animal = zodiacs[branchIndex]
        
        val leapPrefix = if (isLeapMonth) "闰" else ""
        val lunarDate = "农历" + leapPrefix + lunarMonths[lunarMonthNum] + lunarDays[lunarDayNum - 1]
        
        // Map solar year to Chinese string
        val solarYearStr = calendar.get(Calendar.YEAR).toString()
        val chineseNumbers = arrayOf("〇", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        val chineseYear = solarYearStr.map { chineseNumbers[it.toString().toInt()] }.joinToString("")
        
        val lunarYearAnimal = "${chineseYear}年 / ${animal}年"
        val holidayInfo = "今日：无节日"

        // Week and Day info
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val weekDayInfo = "今天是第${weekOfYear}周 / 第${dayOfYear}天"

        // Greeting
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..4 -> "凌晨，早点休息！"
            in 5..8 -> "清晨，早上好！"
            in 9..11 -> "上午，加油啊！"
            12, 13 -> "中午，吃顿好的！"
            in 14..17 -> "下午，继续冲！"
            18, 19 -> "傍晚，放松一下！"
            else -> "夜晚，晚安好梦！"
        }

        // Week dates (Monday to Sunday)
        val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val weekDates = mutableListOf<Int>()
        
        // Find Monday of this week
        val tempCal = calendar.clone() as Calendar
        tempCal.firstDayOfWeek = Calendar.MONDAY
        tempCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        var currentDayIndex = 0
        for (i in 0..6) {
            val day = tempCal.get(Calendar.DAY_OF_MONTH)
            weekDates.add(day)
            if (tempCal.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH)) {
                currentDayIndex = i
            }
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return DashboardDateInfo(
            solarMonthYear, lunarDate, lunarYearAnimal, holidayInfo, weekDayInfo, largeDay, greeting, weekDays, weekDates, currentDayIndex
        )
    }
}
