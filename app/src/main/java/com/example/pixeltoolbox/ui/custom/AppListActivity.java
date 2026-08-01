package com.example.pixeltoolbox.ui.custom;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.compose.material3.MenuKt;
import androidx.core.view.PointerIconCompat;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* loaded from: classes5.dex */
public class AppListActivity extends Activity {
    private AppAdapter adapter;
    private List<AppItem> appList = new ArrayList();
    private List<AppItem> fullAppList = new ArrayList();
    private boolean showOnlyFrozen = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ListView listView;

    class AppItem {
        Drawable icon;
        boolean isFrozen;
        boolean isSystem;
        String name;
        String packageName;
        String sourceDir;

        AppItem() {
        }
    }

    private boolean isExtractMode = false;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.isExtractMode = "EXTRACT".equals(getIntent().getStringExtra("MODE"));

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(Color.parseColor("#F2F2F7"));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setPadding(64, 80, 64, 48);
        linearLayout2.setBackgroundColor(0);
        TextView textView = new TextView(this);
        textView.setText(isExtractMode ? "应用 APK 提取器" : "极客冰箱");
        textView.setTextSize(28.0f);
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setTypeface(Typeface.create("sans-serif-medium", 0));
        TextView textView2 = new TextView(this);
        textView2.setText(isExtractMode ? "点击任意应用即可将其安装包提取到 Download 目录" : "点击任意应用即可冻结或解冻");
        textView2.setTextSize(14.0f);
        textView2.setTextColor(Color.parseColor("#3C3C43"));
        textView2.setPadding(0, 16, 0, 0);
        linearLayout2.addView(textView);
        linearLayout2.addView(textView2);

        if (!isExtractMode) {
            final TextView filterBtn = new TextView(this);
            filterBtn.setText("只看已冻结应用");
            filterBtn.setTextSize(16.0f); // 增大字体
            filterBtn.setTextColor(Color.parseColor("#007AFF"));
            filterBtn.setPadding(120, 36, 120, 36); // 大幅增加内边距，让按钮变得很大
            
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#E6F2FA"));
            bg.setCornerRadius(64.0f); // More rounded (capsule)
            filterBtn.setBackground(bg);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.topMargin = 48;
            params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            filterBtn.setLayoutParams(params);
            
            filterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showOnlyFrozen = !showOnlyFrozen;
                    filterBtn.setText(showOnlyFrozen ? "查看全部应用" : "只看已冻结应用");
                    updateFilter();
                }
            });
            linearLayout2.addView(filterBtn);
        }

        linearLayout.addView(linearLayout2);
        ListView listView = new ListView(this);
        this.listView = listView;
        listView.setDivider(null);
        this.listView.setDividerHeight(24);
        this.listView.setPadding(48, 16, 48, 48);
        this.listView.setClipToPadding(false);
        this.listView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
        linearLayout.addView(this.listView);
        setContentView(linearLayout);
        loadApps();
    }

    private void loadApps() {
        final ProgressDialog progressDialogShow = ProgressDialog.show(this, "", "正在加载应用列表...", true, false);
        new Thread(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.AppListActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                AppListActivity.this.m59x9a18acfc(progressDialogShow);
            }
        }).start();
    }

    /* renamed from: lambda$loadApps$1$com-example-pixeltoolbox-ui-custom-AppListActivity */
    /* synthetic */ void m59x9a18acfc(final ProgressDialog progressDialog) {
        PackageManager packageManager = getPackageManager();
        // 获取所有已安装应用（系统 + 用户），不再过滤
        List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        final ArrayList arrayList = new ArrayList();
        for (ApplicationInfo appInfo : installedApps) {
            // 跳过自身
            if (appInfo.packageName.equals(getPackageName())) continue;
            AppItem appItem = new AppItem();
            appItem.name = appInfo.loadLabel(packageManager).toString();
            appItem.packageName = appInfo.packageName;
            appItem.sourceDir = appInfo.sourceDir;
            appItem.isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            try {
                appItem.icon = appInfo.loadIcon(packageManager);
            } catch (Exception unused) {
            }
            appItem.isFrozen = !appInfo.enabled;
            arrayList.add(appItem);
        }
        // 排序：用户应用在前，系统应用在后；同类按名称排序
        Collections.sort(arrayList, new Comparator<AppItem>() {
            @Override // java.util.Comparator
            public int compare(AppItem appItem2, AppItem appItem3) {
                if (appItem2.isSystem != appItem3.isSystem) {
                    return appItem2.isSystem ? 1 : -1;
                }
                return appItem2.name.compareToIgnoreCase(appItem3.name);
            }
        });
        this.handler.post(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.AppListActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                AppListActivity.this.m58x6c40129d(arrayList, progressDialog);
            }
        });
    }

    /* renamed from: lambda$loadApps$0$com-example-pixeltoolbox-ui-custom-AppListActivity */
    /* synthetic */ void m58x6c40129d(List list, ProgressDialog progressDialog) {
        this.fullAppList.clear();
        this.fullAppList.addAll(list);
        updateFilter();
        progressDialog.dismiss();
    }

    private void updateFilter() {
        this.appList.clear();
        for (AppItem item : this.fullAppList) {
            if (showOnlyFrozen) {
                if (item.isFrozen) {
                    this.appList.add(item);
                }
            } else {
                this.appList.add(item);
            }
        }
        if (this.adapter == null) {
            this.adapter = new AppAdapter();
            this.listView.setAdapter((ListAdapter) this.adapter);
        } else {
            this.adapter.notifyDataSetChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void extractApp(final AppItem appItem) {
        final ProgressDialog progressDialogShow = ProgressDialog.show(this, "", "正在提取 " + appItem.name + " 的 APK...", true, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String cleanName = appItem.name.replace(" ", "_").replace("/", "_");
                    String dest = "/sdcard/Download/" + cleanName + "_" + appItem.packageName + ".apk";
                    String cmd = "cp '" + appItem.sourceDir + "' '" + dest + "' && chmod 644 '" + dest + "'";
                    
                    Method declaredMethod = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                    declaredMethod.setAccessible(true);
                    Object objInvoke = declaredMethod.invoke(null, new String[]{"sh", "-c", cmd}, null, null);
                    objInvoke.getClass().getMethod("waitFor", new Class[0]).invoke(objInvoke, new Object[0]);
                    
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialogShow.dismiss();
                            Toast.makeText(AppListActivity.this, "已提取到 Download 目录", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialogShow.dismiss();
                            Toast.makeText(AppListActivity.this, "提取失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggleApp(final AppItem appItem, int i) {
        final boolean z = !appItem.isFrozen;
        final String str = "pm " + (z ? "disable-user" : "enable") + " --user 0 " + appItem.packageName;
        final ProgressDialog progressDialogShow = ProgressDialog.show(this, "", (z ? "正在冻结 " : "正在解冻 ") + appItem.name + "...", true, false);
        new Thread(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.AppListActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                AppListActivity.this.m62xed78021e(str, appItem, z, progressDialogShow);
            }
        }).start();
    }

    /* renamed from: lambda$toggleApp$4$com-example-pixeltoolbox-ui-custom-AppListActivity */
    /* synthetic */ void m62xed78021e(String str, final AppItem appItem, final boolean z, final ProgressDialog progressDialog) {
        try {
            Method declaredMethod = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            declaredMethod.setAccessible(true);
            Object objInvoke = declaredMethod.invoke(null, new String[]{"sh", "-c", str}, null, null);
            objInvoke.getClass().getMethod("waitFor", new Class[0]).invoke(objInvoke, new Object[0]);
            appItem.isFrozen = z;
            this.handler.post(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.AppListActivity$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AppListActivity.this.m60x91c6cd60(progressDialog, appItem, z);
                }
            });
        } catch (Exception e) {
            this.handler.post(new Runnable() { // from class: com.example.pixeltoolbox.ui.custom.AppListActivity$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AppListActivity.this.m61xbf9f67bf(progressDialog, e);
                }
            });
        }
    }

    /* renamed from: lambda$toggleApp$2$com-example-pixeltoolbox-ui-custom-AppListActivity */
    /* synthetic */ void m60x91c6cd60(ProgressDialog progressDialog, AppItem appItem, boolean z) {
        progressDialog.dismiss();
        this.adapter.notifyDataSetChanged();
        Toast.makeText(this, appItem.name + (z ? " 已冻结" : " 已解冻"), 0).show();
    }

    /* renamed from: lambda$toggleApp$3$com-example-pixeltoolbox-ui-custom-AppListActivity */
    /* synthetic */ void m61xbf9f67bf(ProgressDialog progressDialog, Exception exc) {
        progressDialog.dismiss();
        Toast.makeText(this, "操作失败: " + exc.getMessage(), 1).show();
    }

    class AppAdapter extends ArrayAdapter<AppItem> {
        public AppAdapter() {
            super(AppListActivity.this, 0, AppListActivity.this.appList);
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(final int i, View view, ViewGroup viewGroup) {
            LinearLayout linearLayout;
            if (view == null) {
                LinearLayout linearLayout2 = new LinearLayout(getContext());
                linearLayout2.setOrientation(0);
                linearLayout2.setPadding(48, 48, 48, 48);
                linearLayout2.setGravity(16);
                linearLayout2.setElevation(4.0f);
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setColor(Color.parseColor("#FFFFFF"));
                gradientDrawable.setCornerRadius(24.0f);
                gradientDrawable.setStroke(2, Color.parseColor("#1A3C3C43"));
                linearLayout2.setBackground(gradientDrawable);
                ImageView imageView = new ImageView(getContext());
                imageView.setId(PointerIconCompat.TYPE_CONTEXT_MENU);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MenuKt.InTransitionDuration, MenuKt.InTransitionDuration);
                layoutParams.rightMargin = 32;
                imageView.setLayoutParams(layoutParams);
                linearLayout2.addView(imageView);
                LinearLayout linearLayout3 = new LinearLayout(getContext());
                linearLayout3.setOrientation(1);
                linearLayout3.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                TextView textView = new TextView(getContext());
                textView.setId(PointerIconCompat.TYPE_HAND);
                textView.setTextSize(16.0f);
                textView.setTextColor(Color.parseColor("#000000"));
                textView.setTypeface(Typeface.create("sans-serif", 0));
                linearLayout3.addView(textView);
                TextView textView2 = new TextView(getContext());
                textView2.setId(PointerIconCompat.TYPE_HELP);
                textView2.setTextSize(12.0f);
                textView2.setTextColor(Color.parseColor("#3C3C43"));
                linearLayout3.addView(textView2);
                linearLayout2.addView(linearLayout3);
                TextView textView3 = new TextView(getContext());
                textView3.setId(PointerIconCompat.TYPE_WAIT);
                textView3.setTextSize(14.0f);
                textView3.setPadding(32, 16, 32, 16);
                linearLayout2.addView(textView3);
                linearLayout = linearLayout2;
            } else {
                linearLayout = (LinearLayout) view;
            }
            final AppItem item = getItem(i);
            ImageView imageView2 = (ImageView) linearLayout.findViewById(PointerIconCompat.TYPE_CONTEXT_MENU);
            TextView textView4 = (TextView) linearLayout.findViewById(PointerIconCompat.TYPE_HAND);
            TextView textView5 = (TextView) linearLayout.findViewById(PointerIconCompat.TYPE_HELP);
            TextView textView6 = (TextView) linearLayout.findViewById(PointerIconCompat.TYPE_WAIT);
            imageView2.setImageDrawable(item.icon);
            textView4.setText(item.name);
            textView5.setText((item.isSystem ? "系统 · " : "用户 · ") + item.packageName);
            GradientDrawable gradientDrawable2 = new GradientDrawable();
            gradientDrawable2.setCornerRadius(16.0f);
            if (isExtractMode) {
                textView6.setText("提取");
                textView6.setTextColor(Color.parseColor("#007AFF"));
                gradientDrawable2.setColor(Color.parseColor("#E6F2FA"));
                linearLayout.setAlpha(1.0f);
            } else {
                if (item.isFrozen) {
                    textView6.setText("已冻结");
                    textView6.setTextColor(Color.parseColor("#007AFF"));
                    gradientDrawable2.setColor(Color.parseColor("#E6F2FA"));
                    linearLayout.setAlpha(0.6f);
                } else {
                    textView6.setText("正常");
                    textView6.setTextColor(Color.parseColor("#34C759"));
                    gradientDrawable2.setColor(Color.parseColor("#E8F5E9"));
                    linearLayout.setAlpha(1.0f);
                }
            }
            textView6.setBackground(gradientDrawable2);
            final int position = i;
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view2) {
                    if (isExtractMode) {
                        extractApp(item);
                    } else {
                        toggleApp(item, position);
                    }
                }
            });
            return linearLayout;
        }
    }
}
