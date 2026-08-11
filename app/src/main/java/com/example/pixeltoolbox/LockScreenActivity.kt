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

package com.example.pixeltoolbox

import android.app.Activity
import android.os.Bundle
import com.example.pixeltoolbox.shizuku.ShizukuUtils

class LockScreenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Execute the power button event to lock the screen via Shizuku
        ShizukuUtils.executeCommandOrNull("input keyevent 26")
        
        // Finish the activity immediately so it remains completely transparent/invisible
        finish()
    }
}
