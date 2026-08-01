package com.example.pixeltoolbox.ui.custom;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.TelephonyManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.internal.view.SupportMenu;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* compiled from: DashboardManager.java */
/* loaded from: classes5.dex */
class PremiumDashboardView extends LinearLayout {
    private Context ctx;
    private TextView dataText;
    private RingChartView ringChart;
    private TextView signalText;
    private TextView titleText;

    public PremiumDashboardView(Context context) {
        super(context);
        this.ctx = context;
        setOrientation(0);
        setPadding(m66dp(16.0f), m66dp(16.0f), m66dp(16.0f), m66dp(16.0f));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(m66dp(16.0f));
        setBackground(gradientDrawable);
        setElevation(m66dp(4.0f));
        this.ringChart = new RingChartView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(m66dp(80.0f), m66dp(80.0f));
        layoutParams.gravity = 16;
        addView(this.ringChart, layoutParams);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(m66dp(16.0f), 0, 0, 0);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.gravity = 16;
        addView(linearLayout, layoutParams2);
        this.titleText = new TextView(context);
        this.titleText.setText("流量与信号看板");
        this.titleText.setTextSize(16.0f);
        this.titleText.setTextColor(Color.parseColor("#1a1a1a"));
        this.titleText.getPaint().setFakeBoldText(true);
        linearLayout.addView(this.titleText);
        this.dataText = new TextView(context);
        this.dataText.setText("正在获取数据...");
        this.dataText.setTextSize(13.0f);
        this.dataText.setTextColor(Color.parseColor("#666666"));
        this.dataText.setPadding(0, m66dp(4.0f), 0, m66dp(4.0f));
        linearLayout.addView(this.dataText);
        this.signalText = new TextView(context);
        this.signalText.setText("信号: 获取�?..");
        this.signalText.setTextSize(13.0f);
        this.signalText.setTextColor(Color.parseColor("#666666"));
        linearLayout.addView(this.signalText);
        updateData();
    }

    /* renamed from: dp */
    private int m66dp(float f) {
        return Math.round(f * getContext().getResources().getDisplayMetrics().density);
    }

    /* JADX WARN: Removed duplicated region for block: B:42:0x00fa A[Catch: Exception -> 0x013c, TryCatch #1 {Exception -> 0x013c, blocks: (B:17:0x006f, B:19:0x0079, B:21:0x0085, B:23:0x008c, B:24:0x0090, B:26:0x0096, B:28:0x00a3, B:30:0x00a9, B:32:0x00ad, B:38:0x00ca, B:42:0x00fa, B:33:0x00b8, B:35:0x00bc, B:44:0x011b), top: B:52:0x006f }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void updateData() {
        int dbm;
        boolean z = true;
        if (this.ctx.checkSelfPermission("android.permission.READ_SMS") != 0) {
            this.dataText.setText("⚠️ 未授权读取短信\n请在弹窗中允许权�");
            this.dataText.setTextColor(SupportMenu.CATEGORY_MASK);
            this.ringChart.setPercent(0);
        } else {
            this.dataText.setTextColor(Color.parseColor("#666666"));
            try {
                Cursor cursorQuery = this.ctx.getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"body"}, "address IN ('10086','10010','10000','10099')", null, "date DESC LIMIT 1");
                if (cursorQuery != null && cursorQuery.moveToFirst()) {
                    String string = cursorQuery.getString(0);
                    cursorQuery.close();
                    parseAndSetData(string);
                } else {
                    this.dataText.setText("无运营商短信，请发送查询短信");
                    this.ringChart.setPercent(0);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            } catch (Exception e) {
            }
        }
        try {
            if (this.ctx.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != 0) {
                this.signalText.setText("设备: " + getCleanModel() + "\n信号: [缺少定位权限]");
                return;
            }
            TelephonyManager telephonyManager = (TelephonyManager) this.ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                List<CellInfo> allCellInfo = telephonyManager.getAllCellInfo();
                if (allCellInfo != null) {
                    for (CellInfo cellInfo : allCellInfo) {
                        if (cellInfo.isRegistered()) {
                            if (Build.VERSION.SDK_INT >= 29 && (cellInfo instanceof CellInfoNr)) {
                                dbm = ((CellInfoNr) cellInfo).getCellSignalStrength().getDbm();
                            } else if (!(cellInfo instanceof CellInfoLte)) {
                                dbm = 0;
                            } else {
                                dbm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                            }
                            if (dbm != 0) {
                                this.signalText.setText("设备: " + getCleanModel() + "\n信号: [" + dbm + " dBm]");
                                break;
                            }
                        }
                    }
                    z = false;
                    if (!z) {
                        this.signalText.setText("设备: " + getCleanModel() + "\n信号: 无连�");
                    }
                } else {
                    z = false;
                    if (!z) {
                    }
                }
            }
        } catch (Exception e2) {
            this.signalText.setText("设备: " + getCleanModel());
        }
    }

    private String getCleanModel() {
        String str = Build.MODEL;
        if (str == null) {
            return "Pixel";
        }
        String lowerCase = str.toLowerCase();
        if (lowerCase.contains("sdk") || lowerCase.contains("emulator") || lowerCase.contains("gphone")) {
            return "Pixel 9 Pro (Emulator)";
        }
        if (lowerCase.startsWith(Build.MANUFACTURER.toLowerCase())) {
            return str;
        }
        return Build.MANUFACTURER + " " + str;
    }

    private void parseAndSetData(String str) throws NumberFormatException {
        double d;
        double d2;
        Matcher matcher = Pattern.compile("(?:已使用|已用|累计使用|使用)[^\\d]*?(\\d+(?:\\.\\d+)\")\\s*([MGT]B)", 2).matcher(str);
        if (matcher.find()) {
            double d3 = Double.parseDouble(matcher.group(1));
            double d4 = matcher.group(2).toUpperCase().equals("GB") ? 1024 : 1;
            Double.isNaN(d4);
            d = d3 * d4;
        } else {
            d = 0.0d;
        }
        Matcher matcher2 = Pattern.compile("(?:剩余|可用)[^\\d]*?(\\d+(?:\\.\\d+)\")\\s*([MGT]B)", 2).matcher(str);
        if (matcher2.find()) {
            double d5 = Double.parseDouble(matcher2.group(1));
            double d6 = matcher2.group(2).toUpperCase().equals("GB") ? 1024 : 1;
            Double.isNaN(d6);
            d2 = d5 * d6;
        } else {
            d2 = 0.0d;
        }
        if (d > 0.0d) {
            double d7 = d2 + d;
            int i = (int) ((d / d7) * 100.0d);
            if (i > 100) {
                i = 100;
            }
            this.dataText.setText("本月已用: " + String.format("%.2f", Double.valueOf(d / 1024.0d)) + "GB\n总计流量: " + String.format("%.2f", Double.valueOf(d7 / 1024.0d)) + "GB");
            this.ringChart.setPercent(i);
        }
    }
}
