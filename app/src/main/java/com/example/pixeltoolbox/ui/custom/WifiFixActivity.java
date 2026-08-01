package com.example.pixeltoolbox.ui.custom;

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

public class WifiFixActivity extends Activity {
    private LinearLayout[] items;
    private TextView[] latencyTexts;
    private Thread testThread;
    
    // 已彻底剔除失效的“腾讯云”节点 (http://captive.qq.com/generate_204)
    private String[] serverNames = {
        "小米服务 (国内极速)", "华为服务 (国内稳定)", "Vivo 服务 (国内备用)",
        "V2EX 社区 (公益节点)", "高通中国 (官方节点)", "Cloudflare (国际节点)",
        "Google 国际服务 (默认)", "百度 204", "腾讯QQ 204", "阿里 204", "网易 204", "Microsoft 204"
    };
    private String[] serverUrls = {
        "http://connect.rom.miui.com/generate_204", "http://connectivitycheck.platform.hicloud.com/generate_204", "http://wifi.vivo.com.cn/generate_204",
        "http://captive.v2ex.co/generate_204", "http://www.qualcomm.cn/generate_204", "https://cp.cloudflare.com/generate_204",
        "http://connectivitycheck.gstatic.com/generate_204", "http://connect.rom.miui.com/generate_204", "http://connect.rom.miui.com/generate_204", "http://connect.rom.miui.com/generate_204", "http://connect.rom.miui.com/generate_204", "http://www.msftconnecttest.com/connecttest.txt"
    };

    private int selectedPosition = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long[] latencies;
    private int testsCompleted = 0;
    private boolean autoApplied = false;
    private boolean isAutoApply = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        
        // 整体背景：信号页面同款浅灰底色 #F2F4F7
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#F2F4F7"));

        // Header 区域
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setPadding(48, 64, 48, 32);

        TextView titleText = new TextView(this);
        titleText.setText("去除 WiFi 感叹号");
        titleText.setTextSize(24.0f);
        titleText.setTextColor(Color.parseColor("#1C1C1E"));
        titleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        TextView subTitleText = new TextView(this);
        subTitleText.setText("一键部署能正常连通的验证服务器，秒除网络图标小感叹号");
        subTitleText.setTextSize(13.0f);
        subTitleText.setTextColor(Color.parseColor("#6C6C70"));
        subTitleText.setPadding(0, 12, 0, 0);

        headerLayout.addView(titleText);
        headerLayout.addView(subTitleText);
        rootLayout.addView(headerLayout);

        // 节点列表区域
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        LinearLayout containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        containerLayout.setPadding(36, 8, 36, 36);

        this.items = new LinearLayout[this.serverNames.length];
        this.latencyTexts = new TextView[this.serverNames.length];

        for (int i = 0; i < this.serverNames.length; i++) {
            LinearLayout itemCard = new LinearLayout(this);
            this.items[i] = itemCard;
            itemCard.setOrientation(LinearLayout.HORIZONTAL);
            itemCard.setPadding(40, 36, 40, 36);
            itemCard.setGravity(android.view.Gravity.CENTER_VERTICAL);
            itemCard.setElevation(2.0f);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = 20;
            itemCard.setLayoutParams(lp);

            LinearLayout textContainer = new LinearLayout(this);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView nameTv = new TextView(this);
            nameTv.setText(this.serverNames[i]);
            nameTv.setTextSize(15.0f);
            nameTv.setTextColor(Color.parseColor("#1C1C1E"));
            nameTv.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

            TextView latencyTv = new TextView(this);
            this.latencyTexts[i] = latencyTv;
            latencyTv.setText("测速中...");
            latencyTv.setTextSize(12.0f);
            latencyTv.setTextColor(Color.parseColor("#007AFF"));
            latencyTv.setPadding(0, 6, 0, 0);

            textContainer.addView(nameTv);
            textContainer.addView(latencyTv);
            itemCard.addView(textContainer);

            final int capturedIndex = i;
            itemCard.setOnClickListener(v -> selectItem(capturedIndex));

            containerLayout.addView(itemCard);
        }

        selectItem(0);
        scrollView.addView(containerLayout);
        rootLayout.addView(scrollView);

        // 底部按钮区域
        LinearLayout bottomLayout = new LinearLayout(this);
        bottomLayout.setOrientation(LinearLayout.HORIZONTAL);
        bottomLayout.setPadding(36, 24, 36, 36);
        bottomLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        bottomLayout.setElevation(12.0f);

        Button btnRefresh = new Button(this);
        btnRefresh.setText("刷新测速");
        btnRefresh.setTextSize(15.0f);
        btnRefresh.setTextColor(Color.parseColor("#007AFF"));
        btnRefresh.setAllCaps(false);

        GradientDrawable bgRefresh = new GradientDrawable();
        bgRefresh.setColor(Color.parseColor("#F2F4F7"));
        bgRefresh.setCornerRadius(30.0f);
        btnRefresh.setBackground(bgRefresh);

        LinearLayout.LayoutParams lpBtn1 = new LinearLayout.LayoutParams(0, 120, 1.0f);
        lpBtn1.rightMargin = 16;
        btnRefresh.setLayoutParams(lpBtn1);
        btnRefresh.setOnClickListener(v -> runTests());

        Button btnApply = new Button(this);
        btnApply.setText("应用该配置");
        btnApply.setTextSize(15.0f);
        btnApply.setTextColor(Color.WHITE);
        btnApply.setAllCaps(false);

        GradientDrawable bgApply = new GradientDrawable();
        bgApply.setColor(Color.parseColor("#007AFF"));
        bgApply.setCornerRadius(30.0f);
        btnApply.setBackground(bgApply);

        LinearLayout.LayoutParams lpBtn2 = new LinearLayout.LayoutParams(0, 120, 1.0f);
        lpBtn2.leftMargin = 16;
        btnApply.setLayoutParams(lpBtn2);
        btnApply.setOnClickListener(v -> applyServer());

        bottomLayout.addView(btnRefresh);
        bottomLayout.addView(btnApply);
        rootLayout.addView(bottomLayout);

        setContentView(rootLayout);
        runTests();
    }

    private void selectItem(int pos) {
        this.selectedPosition = pos;
        for (int i = 0; i < this.items.length; i++) {
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(24.0f);
            if (i == pos) {
                gd.setColor(Color.parseColor("#FFFFFF"));
                gd.setStroke(4, Color.parseColor("#007AFF"));
            } else {
                gd.setColor(Color.parseColor("#FFFFFF"));
                gd.setStroke(2, Color.parseColor("#E5E5EA"));
            }
            this.items[i].setBackground(gd);
        }
    }

    private void runTests() {
        if (this.testThread != null && this.testThread.isAlive()) {
            return;
        }
        this.latencies = new long[this.serverUrls.length];
        this.testsCompleted = 0;
        this.autoApplied = false;

        for (int i = 0; i < this.latencyTexts.length; i++) {
            this.latencyTexts[i].setText("测速中...");
            this.latencyTexts[i].setTextColor(Color.parseColor("#007AFF"));
        }

        this.testThread = new Thread(() -> {
            for (int i = 0; i < serverUrls.length; i++) {
                final String urlStr = serverUrls[i];
                final int index = i;
                new Thread(() -> performPing(urlStr, index)).start();
            }
        });
        this.testThread.start();
    }

    private void performPing(String urlStr, final int index) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        long latency = Long.MAX_VALUE;
        HttpURLConnection conn = null;
        String errType = "";

        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "*/*");

            int code = conn.getResponseCode();
            try {
                java.io.InputStream is = conn.getInputStream();
                byte[] buf = new byte[256];
                while (is.read(buf) > 0) {}
                is.close();
            } catch (Exception ignored) {}

            long elapsed = System.currentTimeMillis() - startTime;
            if (code == 204 || code == 200 || code == 301 || code == 302) {
                latency = elapsed;
                success = true;
            }
        } catch (Exception e) {
            errType = e.getClass().getSimpleName();
            success = false;
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }

        final boolean finalSuccess = success;
        final long finalLatency = success ? latency : Long.MAX_VALUE;
        final String finalErr = errType;

        this.handler.post(() -> onTestFinished(finalSuccess, index, finalLatency, finalErr));
    }

    private void onTestFinished(boolean success, int index, long latency, String errType) {
        if (success) {
            this.latencyTexts[index].setText("延迟: " + latency + " ms");
            this.latencyTexts[index].setTextColor(Color.parseColor("#34C759"));
            this.latencies[index] = latency;
        } else {
            String label = (errType != null && !errType.isEmpty()) ? ("失败: " + errType) : "超时或无法连接";
            this.latencyTexts[index].setText(label);
            this.latencyTexts[index].setTextColor(Color.parseColor("#8E8E93"));
            this.latencies[index] = Long.MAX_VALUE;
        }
        this.testsCompleted++;

        if (this.testsCompleted >= this.serverUrls.length && !this.autoApplied) {
            this.autoApplied = true;
            // 彻底去除后台自动私自应用，仅当用户手动点击“应用该配置”时才应用，杜绝通知栏跳出阿里云登录提示！
        }
    }

    private void autoSelectAndApply() {
        int bestIdx = -1;
        long bestLatency = Long.MAX_VALUE;
        for (int i = 0; i < this.latencies.length; i++) {
            if (this.latencies[i] < bestLatency) {
                bestLatency = this.latencies[i];
                bestIdx = i;
            }
        }
        if (bestIdx >= 0) {
            this.selectedPosition = bestIdx;
            selectItem(bestIdx);
            applyServerAuto(bestIdx, bestLatency);
        }
    }

    private void applyServerAuto(final int index, final long latency) {
        final String str = this.serverUrls[index];
        this.isAutoApply = true;
        final ProgressDialog progressDialog = ProgressDialog.show(this, "", "正在自动应用最快节点...", true, false);
        new Thread(() -> executeCaptivePortalConfig(str, progressDialog)).start();
    }

    private void applyServer() {
        final String str = this.serverUrls[this.selectedPosition];
        final ProgressDialog progressDialog = ProgressDialog.show(this, "", "正在应用验证服务器并刷新网络...", true, false);
        new Thread(() -> executeCaptivePortalConfig(str, progressDialog)).start();
    }

    private void executeCaptivePortalConfig(String urlStr, final ProgressDialog progressDialog) {
        try {
            Method declaredMethod = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            declaredMethod.setAccessible(true);

            String host = urlStr.replace("http://", "").replace("https://", "");
            int slashIdx = host.indexOf('/');
            if (slashIdx >= 0) {
                host = host.substring(0, slashIdx);
            }

            String[] shellCmds = {
                "settings delete global captive_portal_mode",
                "settings delete global captive_portal_server",
                "settings delete global captive_portal_use_https",
                "settings delete global captive_portal_detection_enabled",
                "settings delete global captive_portal_http_url",
                "settings delete global captive_portal_https_url",
                "settings put global captive_portal_detection_enabled 1",
                "settings put global captive_portal_mode 1",
                "settings put global captive_portal_use_https 1",
                "settings put global captive_portal_server " + host,
                "settings put global captive_portal_http_url " + urlStr,
                "settings put global captive_portal_https_url " + urlStr,
                "svc wifi disable",
                "sleep 1",
                "svc wifi enable"
            };

            for (String cmd : shellCmds) {
                Object proc = declaredMethod.invoke(null, new String[]{"sh", "-c", cmd}, null, null);
                proc.getClass().getMethod("waitFor").invoke(proc);
            }

            this.handler.post(() -> {
                progressDialog.dismiss();
                if (this.isAutoApply) {
                    Toast.makeText(this, "已自动配置最快节点：" + this.serverNames[this.selectedPosition] + "（" + this.latencies[this.selectedPosition] + "ms）", Toast.LENGTH_LONG).show();
                    this.isAutoApply = false;
                } else {
                    Toast.makeText(this, "去除 WiFi 感叹号成功，网络已重新连通！", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } catch (Exception e) {
            this.handler.post(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }
}
