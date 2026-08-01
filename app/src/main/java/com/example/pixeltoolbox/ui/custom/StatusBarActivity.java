package com.example.pixeltoolbox.ui.custom;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes5.dex */
public class StatusBarActivity extends Activity {
    private String[] iconIds = {"alarm_clock", "bluetooth", "nfc", "wifi", "mobile", "battery", "vpn", "zen", "airplane", "hotspot"};
    private String[] iconNames = {"闹钟图标", "蓝牙图标", "NFC 图标", "WiFi 图标", "移动数据图标", "电池图标", "VPN 图标", "免打扰图标", "飞行模式图标", "热点图标"};
    private String[] iconDescs = {"隐藏状态栏顶部的闹钟提示图标", "隐藏状态栏顶部的蓝牙标志", "隐藏状态栏的 NFC 标志", "隐藏无线网络状态图标", "隐藏移动网络状态图标", "隐藏电池电量图标", "隐藏 VPN 钥匙图标", "隐藏免打扰月亮图标", "隐藏飞行模式飞机图标", "隐藏个人热点分享图标"};
    private boolean[] checkedStates = new boolean[10];
    private Switch[] switches = new Switch[10];
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(Color.parseColor("#F2F2F7"));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        int i = 48;
        linearLayout2.setPadding(64, 80, 64, 48);
        linearLayout2.setBackgroundColor(0);
        TextView textView = new TextView(this);
        textView.setText("状态栏净化");
        textView.setTextSize(28.0f);
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setTypeface(Typeface.create("sans-serif-medium", 0));
        TextView textView2 = new TextView(this);
        textView2.setText("选择你想要隐藏的系统图标");
        textView2.setTextSize(14.0f);
        textView2.setTextColor(Color.parseColor("#3C3C43"));
        int i2 = 16;
        textView2.setPadding(0, 16, 0, 0);
        linearLayout2.addView(textView);
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        ScrollView scrollView = new ScrollView(this);
        int i3 = -1;
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(48, 16, 48, 48);
        int i4 = 0;
        while (i4 < this.iconIds.length) {
            LinearLayout linearLayout4 = new LinearLayout(this);
            linearLayout4.setOrientation(0);
            linearLayout4.setPadding(i, i, i, i);
            linearLayout4.setGravity(i2);
            linearLayout4.setElevation(4.0f);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(i3, -2);
            layoutParams.bottomMargin = 24;
            linearLayout4.setLayoutParams(layoutParams);
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(Color.parseColor("#FFFFFF"));
            gradientDrawable.setCornerRadius(24.0f);
            gradientDrawable.setStroke(2, Color.parseColor("#1A3C3C43"));
            linearLayout4.setBackground(gradientDrawable);
            LinearLayout linearLayout5 = new LinearLayout(this);
            linearLayout5.setOrientation(1);
            linearLayout5.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView textView3 = new TextView(this);
            textView3.setText(this.iconNames[i4]);
            textView3.setTextSize(16.0f);
            textView3.setTextColor(Color.parseColor("#1A1A1A"));
            textView3.setTypeface(Typeface.create("sans-serif", 0));
            TextView textView4 = new TextView(this);
            textView4.setText(this.iconDescs[i4]);
            textView4.setTextSize(12.0f);
            textView4.setTextColor(Color.parseColor("#3C3C43"));
            linearLayout5.addView(textView3);
            linearLayout5.addView(textView4);
            linearLayout4.addView(linearLayout5);
            final Switch r4 = new Switch(this);
            this.switches[i4] = r4;
            linearLayout4.addView(r4);
            linearLayout4.setOnClickListener(new View.OnClickListener() { // from class: com.example.pixeltoolbox.ui.custom.StatusBarActivity$$ExternalSyntheticLambda5
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    r4.setChecked(!r4.isChecked());
                }
            });
            linearLayout3.addView(linearLayout4);
            i4++;
            i = 48;
            i3 = -1;
            i2 = 16;
        }
        scrollView.addView(linearLayout3);
        linearLayout.addView(scrollView);
        LinearLayout linearLayout6 = new LinearLayout(this);
        linearLayout6.setPadding(64, 48, 64, 64);
        GradientDrawable gradientDrawable2 = new GradientDrawable();
        gradientDrawable2.setColor(Color.parseColor("#FFFFFF"));
        gradientDrawable2.setStroke(2, Color.parseColor("#1A3C3C43"));
        gradientDrawable2.setCornerRadii(new float[]{36.0f, 36.0f, 36.0f, 36.0f, 0.0f, 0.0f, 0.0f, 0.0f});
        linearLayout6.setBackground(gradientDrawable2);
        linearLayout6.setElevation(16.0f);
        Button button = new Button(this);
        GradientDrawable gradientDrawable3 = new GradientDrawable();
        button.setText("保存并立即应用");
        gradientDrawable3.setColor(Color.parseColor("#007AFF"));
        gradientDrawable3.setCornerRadius(16.0f);
        button.setBackground(gradientDrawable3);
        button.setLayoutParams(new LinearLayout.LayoutParams(-1, 140));
        button.setOnClickListener(new View.OnClickListener() { // from class: com.example.pixeltoolbox.ui.custom.StatusBarActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                StatusBarActivity.this.m70x6d86f13b(view);
            }
        });
        linearLayout6.addView(button);
        linearLayout.addView(linearLayout6);
        setContentView(linearLayout);
        loadSettings();
    }

    /* renamed from: lambda$onCreate$1$com-example-pixeltoolbox-ui-custom-StatusBarActivity */
    /* synthetic */ void m70x6d86f13b(View view) {
        saveSettings();
    }

    private void loadSettings() {
        new Thread(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.StatusBarActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                StatusBarActivity.this.m69x9105b86a();
            }
        }).start();
    }

    /* renamed from: lambda$loadSettings$4$com-example-pixeltoolbox-ui-custom-StatusBarActivity */
    /* synthetic */ void m69x9105b86a() {
        try {
            int i = 0;
            Method declaredMethod = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            declaredMethod.setAccessible(true);
            Object objInvoke = declaredMethod.invoke(null, new String[]{"sh", "-c", "settings get secure icon_blacklist"}, null, null);
            String line = new BufferedReader(new InputStreamReader((InputStream) objInvoke.getClass().getMethod("getInputStream", new Class[0]).invoke(objInvoke, new Object[0]))).readLine();
            if (line == null || line.trim().equals("null")) {
                line = "";
            }
            List listAsList = Arrays.asList(line.split(","));
            while (true) {
                String[] strArr = this.iconIds;
                if (i < strArr.length) {
                    this.checkedStates[i] = listAsList.contains(strArr[i]);
                    i++;
                } else {
                    this.handler.post(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.StatusBarActivity$$ExternalSyntheticLambda3
                        @Override // java.lang.Runnable
                        public final void run() {
                            StatusBarActivity.this.m67x5ccebb2c();
                        }
                    });
                    return;
                }
            }
        } catch (Exception e) {
            this.handler.post(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.StatusBarActivity$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBarActivity.this.m68x76ea39cb(e);
                }
            });
        }
    }

    /* renamed from: lambda$loadSettings$2$com-example-pixeltoolbox-ui-custom-StatusBarActivity */
    /* synthetic */ void m67x5ccebb2c() {
        for (int i = 0; i < this.iconIds.length; i++) {
            this.switches[i].setChecked(this.checkedStates[i]);
        }
    }

    /* renamed from: lambda$loadSettings$3$com-example-pixeltoolbox-ui-custom-StatusBarActivity */
    /* synthetic */ void m68x76ea39cb(Exception exc) {
        Toast.makeText(this, "读取配置失败: " + exc.getMessage(), 1).show();
    }

    private void saveSettings() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.iconIds.length; i++) {
            if (this.switches[i].isChecked()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(this.iconIds[i]);
            }
        }
        final String str = "settings put secure icon_blacklist \"" + sb.toString() + "\"";
        new Thread(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.StatusBarActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                StatusBarActivity.this.m73xf21f00d0(str);
            }
        }).start();
    }

    /* renamed from: lambda$saveSettings$7$com-example-pixeltoolbox-ui-custom-StatusBarActivity */
    /* synthetic */ void m73xf21f00d0(String str) {
        try {
            Method declaredMethod = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            declaredMethod.setAccessible(true);
            Object objInvoke = declaredMethod.invoke(null, new String[]{"sh", "-c", str}, null, null);
            final int iIntValue = ((Integer) objInvoke.getClass().getMethod("waitFor", new Class[0]).invoke(objInvoke, new Object[0])).intValue();
            this.handler.post(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.StatusBarActivity$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBarActivity.this.m71xbde80392(iIntValue);
                }
            });
        } catch (Exception e) {
            this.handler.post(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.StatusBarActivity$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    StatusBarActivity.this.m72xd8038231(e);
                }
            });
        }
    }

    /* renamed from: lambda$saveSettings$5$com-example-pixeltoolbox-ui-custom-StatusBarActivity */
    /* synthetic */ void m71xbde80392(int i) {
        if (i != 0) {
            Toast.makeText(this, "保存失败，退出码: " + i, 0).show();
        } else {
            Toast.makeText(this, "状态栏净化配置已保存并应用！", 0).show();
            finish();
        }
    }

    /* renamed from: lambda$saveSettings$6$com-example-pixeltoolbox-ui-custom-StatusBarActivity */
    /* synthetic */ void m72xd8038231(Exception exc) {
        Toast.makeText(this, "应用错误: " + exc.getMessage(), 1).show();
    }
}
