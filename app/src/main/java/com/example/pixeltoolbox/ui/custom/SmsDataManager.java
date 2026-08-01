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
