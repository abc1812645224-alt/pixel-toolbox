package com.example.pixeltoolbox.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* loaded from: classes5.dex */
public class ModernSignalDashboardLayout extends ScrollView {
    public ModernSignalDashboardLayout(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setOverScrollMode(2);
        setVerticalScrollBarEnabled(false);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(m65dp(16.0f), m65dp(16.0f), m65dp(16.0f), m65dp(16.0f));
        linearLayout.addView(createStatusCard(context));
        linearLayout.addView(createCACard(context));
        linearLayout.addView(createRingChartCard(context));
        linearLayout.addView(createDataUsageCard(context));
        linearLayout.addView(createInjectionCard(context));
        addView(linearLayout, new ViewGroup.LayoutParams(-1, -2));
    }

    private View createStatusCard(Context context) {
        LinearLayout linearLayoutCreateCard = createCard(context);
        TextView textView = new TextView(context);
        textView.setText("[?]需要定位和电话权限\n未知");
        textView.setTextColor(Color.parseColor("#333333"));
        textView.setTextSize(16.0f);
        linearLayoutCreateCard.addView(textView);
        return linearLayoutCreateCard;
    }

    private View createCACard(Context context) {
        LinearLayout linearLayoutCreateCard = createCard(context);
        TextView textView = new TextView(context);
        textView.setText("载波聚合 (CA) 无服务小区数");
        textView.setTextColor(Color.parseColor("#333333"));
        linearLayoutCreateCard.addView(textView);
        return linearLayoutCreateCard;
    }

    private View createRingChartCard(Context context) {
        LinearLayout linearLayoutCreateCard = createCard(context);
        TextView textView = new TextView(context);
        textView.setText(Build.BRAND + " - " + Build.MODEL);
        textView.setTextSize(16.0f);
        textView.setTypeface(null, 1);
        textView.setTextColor(ViewCompat.MEASURED_STATE_MASK);
        textView.setGravity(17);
        linearLayoutCreateCard.addView(textView);
        RingChartView ringChartView = new RingChartView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(m65dp(200.0f), m65dp(200.0f));
        layoutParams.gravity = 17;
        layoutParams.topMargin = m65dp(16.0f);
        ringChartView.setLayoutParams(layoutParams);
        linearLayoutCreateCard.addView(ringChartView);
        return linearLayoutCreateCard;
    }

    private View createDataUsageCard(Context context) {
        LinearLayout linearLayoutCreateCard = createCard(context);
        TextView textView = new TextView(context);
        textView.setText("流量详情看板\n本月已用: 读取中... / 总计: 读取中...");
        textView.setTextColor(Color.parseColor("#333333"));
        linearLayoutCreateCard.addView(textView);
        View view = new View(context);
        view.setBackgroundColor(Color.parseColor("#E5E5EA"));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, m65dp(12.0f));
        layoutParams.topMargin = m65dp(8.0f);
        view.setLayoutParams(layoutParams);
        linearLayoutCreateCard.addView(view);
        return linearLayoutCreateCard;
    }

    private View createInjectionCard(final Context context) {
        LinearLayout linearLayoutCreateCard = createCard(context);
        TextView textView = new TextView(context);
        textView.setText("�?5G 底层注入引擎 (待注�?");
        textView.setTypeface(null, 1);
        textView.setTextSize(16.0f);
        textView.setTextColor(ViewCompat.MEASURED_STATE_MASK);
        linearLayoutCreateCard.addView(textView);
        TextView textView2 = new TextView(context);
        textView2.setText("采用专业�?UserService 提权架构，将 VoLTE �?5G 参数直接硬写入运营商底层配置。一次点击，永久生效，零耗电，防断流。\n💡 温馨提示：重启手机后可能被系统还原，若失效请重新注入�");
        textView2.setTextColor(Color.parseColor("#666666"));
        textView2.setPadding(0, m65dp(8.0f), 0, m65dp(16.0f));
        linearLayoutCreateCard.addView(textView2);
        Button button = new Button(context);
        button.setText("一键硬核注[?]5G / VoLTE");
        button.setTextColor(-1);
        button.setBackgroundColor(Color.parseColor("#007AFF"));
        button.setOnClickListener(new View.OnClickListener() { // from class: com.example.pixeltoolbox.ui.custom.ModernSignalDashboardLayout$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                try { ModernSignalDashboardLayout.lambda$createInjectionCard$0(context, view); } catch (Exception e) { e.printStackTrace(); }
            }
        });
        linearLayoutCreateCard.addView(button);
        return linearLayoutCreateCard;
    }

    static /* synthetic */ void lambda$createInjectionCard$0(Context context, View view) {
        boolean z;
        try {
            String str = "am instrument -e subId " + SubscriptionManager.getDefaultDataSubscriptionId() + " -w io.github.vvb2060.ims.mod/io.github.vvb2060.ims.privileged.ImsModifier";
            Class<?> cls = Class.forName("com.example.pixeltoolbox.shizuku.ShizukuUtils");
            Object obj = cls.getField("INSTANCE").get(null);
            Method[] declaredMethods = cls.getDeclaredMethods();
            int length = declaredMethods.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    z = false;
                    break;
                }
                Method method = declaredMethods[i];
                if (!method.getName().startsWith("executeCommand")) {
                    i++;
                } else {
                    method.invoke(obj, str);
                    z = true;
                    break;
                }
            }
            if (!z) {
                Runtime.getRuntime().exec("su -c " + str);
            }
            Toast.makeText(context, "注入指令已下[?]", 0).show();
        } catch (Exception e) {
            Toast.makeText(context, "注入失败: " + e.getMessage(), 1).show();
        }
    }

    private LinearLayout createCard(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-1);
        linearLayout.setPadding(m65dp(16.0f), m65dp(16.0f), m65dp(16.0f), m65dp(16.0f));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = m65dp(16.0f);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setElevation(m65dp(2.0f));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(m65dp(16.0f));
        linearLayout.setBackground(gradientDrawable);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: dp */
    public int m65dp(float f) {
        return (int) TypedValue.applyDimension(1, f, getResources().getDisplayMetrics());
    }

    class RingChartView extends View {
        private Paint bgPaint;
        private Paint progressPaint;
        private Paint textPaint;

        public RingChartView(Context context) {
            super(context);
            this.bgPaint = new Paint(1);
            this.bgPaint.setColor(Color.parseColor("#E5E5EA"));
            this.bgPaint.setStyle(Paint.Style.STROKE);
            this.bgPaint.setStrokeWidth(ModernSignalDashboardLayout.this.m65dp(16.0f));
            this.progressPaint = new Paint(1);
            this.progressPaint.setColor(Color.parseColor("#4CD964"));
            this.progressPaint.setStyle(Paint.Style.STROKE);
            this.progressPaint.setStrokeWidth(ModernSignalDashboardLayout.this.m65dp(16.0f));
            this.progressPaint.setStrokeCap(Paint.Cap.ROUND);
            this.textPaint = new Paint(1);
            this.textPaint.setColor(ViewCompat.MEASURED_STATE_MASK);
            this.textPaint.setTextSize(ModernSignalDashboardLayout.this.m65dp(24.0f));
            this.textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, 1));
            this.textPaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            float fM65dp = ModernSignalDashboardLayout.this.m65dp(16.0f);
            RectF rectF = new RectF(fM65dp, fM65dp, width - fM65dp, height - fM65dp);
            canvas.drawArc(rectF, 135.0f, 270.0f, false, this.bgPaint);
            canvas.drawArc(rectF, 135.0f, 202.5f, false, this.progressPaint);
            canvas.drawText("-83 dBm", width / 2.0f, (height / 2.0f) + ModernSignalDashboardLayout.this.m65dp(8.0f), this.textPaint);
        }
    }
}
