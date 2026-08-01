package com.example.pixeltoolbox.ui.custom;

import android.R;
import android.app.Activity;
import android.widget.FrameLayout;

/* loaded from: classes5.dex */
public class DashboardManager {
    private static Activity act;
    private static PremiumDashboardView dashboardView;
    private static boolean isSignalTab = true;

    public static void init(Activity activity) {
        if (dashboardView == null) {
            act = activity;
            dashboardView = new PremiumDashboardView(activity);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, Math.round(activity.getResources().getDisplayMetrics().density * 140.0f));
            layoutParams.gravity = 81;
            int iRound = Math.round(activity.getResources().getDisplayMetrics().density * 16.0f);
            layoutParams.setMargins(iRound, iRound, iRound, Math.round(activity.getResources().getDisplayMetrics().density * 110.0f));
            ((FrameLayout) activity.getWindow().getDecorView().findViewById(R.id.content)).addView(dashboardView, layoutParams);
            new Thread(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.DashboardManager.1
                @Override // java.lang.Runnable
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(2000L);
                            if (DashboardManager.dashboardView != null && DashboardManager.isSignalTab) {
                                DashboardManager.act.runOnUiThread(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.DashboardManager.1.1
                                    @Override // java.lang.Runnable
                                    public void run() {
                                        DashboardManager.dashboardView.updateData();
                                    }
                                });
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }).start();
        }
    }

    public static void onTabSelected(int i) {
        isSignalTab = i == 0;
        if (dashboardView != null) {
            act.runOnUiThread(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.DashboardManager.2
                @Override // java.lang.Runnable
                public void run() {
                    DashboardManager.dashboardView.setVisibility(DashboardManager.isSignalTab ? 0 : 8);
                    if (DashboardManager.isSignalTab) {
                        DashboardManager.dashboardView.updateData();
                    }
                }
            });
        }
    }
}
