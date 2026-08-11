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

package com.example.pixeltoolbox.ui

import androidx.lifecycle.ViewModel
import com.example.pixeltoolbox.shizuku.ShizukuUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _hasShizuku = MutableStateFlow(ShizukuUtils.hasShizukuPermission())
    val hasShizuku: StateFlow<Boolean> = _hasShizuku.asStateFlow()

    private val _terminalInput = MutableStateFlow("")
    val terminalInput: StateFlow<String> = _terminalInput.asStateFlow()

    private val _terminalOutput = MutableStateFlow("Ready.\n")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _dpiInput = MutableStateFlow("")
    val dpiInput: StateFlow<String> = _dpiInput.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<String>>(emptyList())
    val executionLogs: StateFlow<List<String>> = _executionLogs.asStateFlow()

    private val _showBarometerTest = MutableStateFlow(false)
    val showBarometerTest: StateFlow<Boolean> = _showBarometerTest.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _showBootManager = MutableStateFlow(false)
    val showBootManager: StateFlow<Boolean> = _showBootManager.asStateFlow()

    private val _showGpsTest = MutableStateFlow(false)
    val showGpsTest: StateFlow<Boolean> = _showGpsTest.asStateFlow()

    fun updateShizukuStatus() {
        _hasShizuku.value = ShizukuUtils.hasShizukuPermission()
    }

    fun setTerminalInput(input: String) {
        _terminalInput.value = input
    }

    fun setTerminalOutput(output: String) {
        _terminalOutput.value = output
    }

    fun setDpiInput(input: String) {
        _dpiInput.value = input
    }

    fun setShowBarometerTest(show: Boolean) {
        _showBarometerTest.value = show
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setShowBootManager(show: Boolean) {
        _showBootManager.value = show
    }

    fun setShowGpsTest(show: Boolean) {
        _showGpsTest.value = show
    }

    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = "[$time] $msg"
        val currentLogs = _executionLogs.value.toMutableList()
        currentLogs.add(0, newLog)
        if (currentLogs.size > 6) {
            currentLogs.removeLast()
        }
        _executionLogs.value = currentLogs
    }
}
