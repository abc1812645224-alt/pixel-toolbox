# ========== Shizuku ==========
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# ========== Instrumentation 子类（通过反射 startInstrumentation 调用）==========
-keep class com.example.pixeltoolbox.shizuku.CarrierConfigInstrumentation { *; }
-keep class com.example.pixeltoolbox.shizuku.ConfigReaderInstrumentation { *; }

# ========== ShizukuProvider（Manifest 声明）==========
-keep class com.example.pixeltoolbox.shizuku.ShizukuProviderWrapper { *; }

# ========== IMS 相关 ==========
-keep class com.example.pixeltoolbox.shizuku.ImsConfigServiceImpl { *; }
-keep class com.example.pixeltoolbox.shizuku.CarrierConfigHelper { *; }

# ========== Java View Activity（Intent 跳转）==========
-keep class com.example.pixeltoolbox.ui.custom.AppListActivity { *; }
-keep class com.example.pixeltoolbox.ui.custom.StatusBarActivity { *; }
-keep class com.example.pixeltoolbox.ui.custom.WifiFixActivity { *; }

# ========== Java View 辅助类 ==========
-keep class com.example.pixeltoolbox.ui.custom.** { *; }

# ========== 桌面小部件 ==========
-keep class com.example.pixeltoolbox.ModeWidgetProvider { *; }

# ========== 主 Activity ==========
-keep class com.example.pixeltoolbox.MainActivity { *; }
-keep class com.example.pixeltoolbox.MainActivity$* { *; }

# ========== Signal Monitor ==========
-keep class com.example.pixeltoolbox.signal.** { *; }

# ========== 反射相关：ServiceManager ==========
# NOTE: android.app.IActivityManager / IActivityManager$Stub MUST NOT be kept -
#   they are compile-time stubs; runtime must use system framework.jar version.
#   All calls in CarrierConfigInstrumentation now use reflection to avoid signature mismatch on Android 15+.
-keep class android.os.ServiceManager { *; }

# ========== app_process 入口类（通过 shell 启动）==========
-keep class com.example.pixeltoolbox.ims.ImsModifier { *; }
-keep class com.example.pixeltoolbox.shizuku.ConfigReaderHelper { *; }

# ========== 隐藏 API 豁免 ==========
-keep class dalvik.system.VMRuntime { *; }

# ========== CarrierConfigManager 反射 ==========
-keep class android.telephony.CarrierConfigManager { *; }
-keep class android.telephony.SubscriptionManager { *; }

# ========== Compose 运行时 ==========
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ========== ARSCLib + apksig ==========
-keep class com.reandroid.** { *; }
-keep class com.android.apksig.** { *; }

# ========== 通用保底 ==========
-keep class com.example.pixeltoolbox.** { *; }

# ========== stub AIDL ==========
-keep class com.example.pixeltoolbox.stub.** { *; }

# ========== Application ==========
-keep class * extends android.app.Application

# ========== Kotlin serialization ==========
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ========== 保留 R 类 ==========
-keep class **.R$* { *; }

# ========== 反射相关 ==========
-keepattributes Signature
-keepattributes *Annotation*
