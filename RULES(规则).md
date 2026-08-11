# Pixel Toolbox 开发规则

> 基于 Google Android 官方开发者文档制定，适用于本项目的开发、构建与发布全流程。

---

## 一、签名与构建

| 规则 | 要求 | 本项目状态 |
|------|------|:---:|
| **强制签名** | 所有 APK 必须先使用证书签名，否则无法安装 | ✅ |
| **Release 签名** | release buildType 必须配置 signingConfigs，禁止使用 debug 签名发布 | ✅ |
| **R8 优化** | release 必须启用 minifyEnabled + shrinkResources | ✅ |
| **Gradle Wrapper** | 统一使用 gradlew，禁止本地 Gradle | ✅ |
| **minSdk / targetSdk** | minSdk=28，targetSdk ≥ 34（Google Play 要求） | ✅ |

---

## 二、架构规范（Architecture Components）

### 2.1 分层架构

```
ui/          → Composable UI + ViewModel（UI 层）
data/        → Repository + DataSource（数据层）
di/          → Hilt Module（依赖注入）
```

### 2.2 核心原则

| 规则 | 说明 |
|------|------|
| **ViewModel 持有状态** | 所有 UI 状态存储在 ViewModel，禁止在 Activity/Composable 顶层存状态 |
| **单向数据流 (UDF)** | ViewModel → StateFlow → Composable；事件通过回调 → ViewModel |
| **Repository 模式** | 数据访问统一通过 Repository，不直接调用 Android API |
| **依赖注入 (Hilt)** | 使用 Hilt 管理依赖树，禁止手动 new 依赖 |
| **ApplicationContext 优先** | 长生命周期对象（Repository、Preferences）使用 applicationContext |

### 2.3 包结构约定

```
com.example.pixeltoolbox
├── MainActivity.kt              # 入口（< 200 行）
├── PixelToolboxApp.kt           # 应用级 Application + Hilt 入口
├── di/                          # Hilt Module
├── data/
│   ├── repository/              # Repository 实现
│   └── model/                   # 数据模型
├── ui/
│   ├── theme/                   # 主题、颜色、组件
│   ├── signal/                  # 信号 Tab
│   │   └── components/          # 拆分的子组件
│   ├── system/                  # 系统 Tab
│   ├── tools/                   # 工具 Tab
│   ├── about/                   # 关于 Tab
│   ├── common/                  # 通用组件
│   ├── custom/                  # 独立 Activity（Java）
│   └── geektools/               # 极客工具
├── shizuku/                     # Shizuku 集成
├── services/                    # Service
└── util/                        # 工具类
```

---

## 三、Compose 最佳实践

| 规则 | 说明 |
|------|------|
| **LazyColumn 必须传 key** | 每个 LazyColumn/items 必须提供稳定 key 参数 |
| **remember 缓存昂贵计算** | SimpleDateFormat、正则、复杂对象创建必须 remember |
| **禁止向后写入** | Composable 中不得写入已读取的 state，避免无限重组 |
| **derivedStateOf 限重组** | 高频变化状态用 derivedStateOf 减少不必要重组 |
| **单文件 ≤ 500 行** | 超过需拆分为独立 Composable 子组件 |

---

## 四、AndroidManifest 规范

| 规则 | 说明 |
|------|------|
| **四大组件全声明** | Activity/Service/Receiver/Provider 必须在 manifest 中声明 |
| **uses-feature 显式声明** | GPS、气压计等硬件需声明 required=false 扩大兼容 |
| **权限最小化** | 只声明实际使用的权限 |
| **元素顺序** | uses-permission → application → 组件声明 |

---

## 五、UI 风格规范

| 规则 | 说明 |
|------|------|
| **色彩体系** | Apple iOS HIG（#F2F2F7 背景、#007AFF 主色、#34C759 绿色、#FF3B30 红色） |
| **卡片风格** | Glass Morphism（20dp 圆角、白色半透明、轻微阴影） |
| **按钮风格** | iOS 27 Flat Button（14dp 圆角） |
| **UI 框架** | Google AOSP: Jetpack Compose + Material 3 组件底层框架 |
| **颜色 token** | 统一使用 iOSBackground / iOSLabel / iOSBlue 等，禁止硬编码 |

---

## 六、代码质量

| 规则 | 说明 |
|------|------|
| **Kotlin 优先** | 新代码使用 Kotlin，Java 仅维护旧 Activity |
| **避免内存泄漏** | 非 Activity 对象使用 applicationContext |
| **崩溃处理链式** | UncaughtExceptionHandler 处理后必须委托给原 handler |

---

## 七、构建命令

```bash
# Debug 构建
.\gradlew.bat assembleDebug

# Release 构建
.\gradlew.bat assembleRelease

# 安装到设备
adb -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk
```

---

*最后更新: 2026-08-09 | 基于 developer.android.google.cn 官方文档*
