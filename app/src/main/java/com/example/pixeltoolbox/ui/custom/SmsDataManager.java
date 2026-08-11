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

package com.example.pixeltoolbox.ui.custom;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

/* loaded from: classes5.dex */
public class SmsDataManager {
    public static void init(Context context) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            try {
                if (Build.VERSION.SDK_INT >= 30) {
                    activity.getWindow().setDecorFitsSystemWindows(false);
                }
                activity.getWindow().setStatusBarColor(0);
                activity.getWindow().setNavigationBarColor(0);
            } catch (Exception e) {
            }
            DashboardManager.init(activity);
        }
    }

    public static String getDashboardString() {
        return "";
    }
}
