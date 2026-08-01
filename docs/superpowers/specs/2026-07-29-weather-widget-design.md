# 天气桌面小组件设计文档

**日期**: 2026-07-29  
**项目**: pixel-toolbox  
**目标**: 实现 Apple Weather 风格的 Android AppWidget 桌面天气小组件

---

## 1. 概述

在 pixel-toolbox 项目内新增天气桌面小组件模块。视觉风格参照用户提供的暗黑旗舰风截图（纯黑背景、橙金主色调、分区信息布局），支持 4×3 和 4×2 两种尺寸，数据源为和风天气（主）+ OpenWeather（备）。

---

## 2. 架构设计

采用 Repository + WorkManager 分层架构：

```
┌─────────────────────────────────────┐
│  WeatherWidget4x3    WeatherWidget4x2│  ← Glance Widget Provider (UI)
├─────────────────────────────────────┤
│         WeatherRepository           │  ← 数据仓库 (双源切换)
│    ┌──────────┐  ┌──────────┐       │
│    │ HeFengSrc│  │  OWSrc   │       │
│    └──────────┘  └──────────┘       │
├─────────────────────────────────────┤
│   WorkManager (30min 定时刷新)       │  ← 后台刷新调度
├─────────────────────────────────────┤
│   Local Cache (DataStore)           │  ← 本地缓存 (离线兜底)
└─────────────────────────────────────┘
```

### 2.1 模块划分

| 模块 | 职责 | 依赖 |
|------|------|------|
| `WeatherRepository` | 双源切换、数据聚合、缓存管理 | HeFengApi, OWApi, DataStore |
| `HeFengApi` | 和风天气 v7 API 调用 (实时+预报+农历) | OkHttp, Gson |
| `OWApi` | OpenWeather API 调用 (备用) | OkHttp, Gson |
| `WeatherWidget4x3` | 4×3 Glance Widget 渲染 | Glance, WeatherRepository |
| `WeatherWidget4x2` | 4×2 Glance Widget 渲染 | Glance, WeatherRepository |
| `WeatherWorker` | WorkManager 定时刷新任务 | WeatherRepository |

### 2.2 数据流

```
WorkManager 触发
  → WeatherRepository.refresh()
    → 优先调 HeFengApi.getWeather()
      → 成功: 缓存 + 通知 Widget 更新
      → 失败: 降级 OWApi.getWeather()
        → 成功: 缓存 + 通知 Widget 更新
        → 失败: 读 DataStore 缓存 → 通知 Widget 渲染缓存
```

---

## 3. UI 设计

### 3.1 视觉风格

- **背景**: 纯黑 (#000000)，模拟截图暗黑沉浸感
- **毛玻璃卡片**: 各信息区使用半透明深灰背景 (约 #1C1C1E 80%) + 圆角 (16dp) + 细边框 (1dp, #FFFFFF 10%)，模拟截图中的毛玻璃悬浮质感
- **主色调**: 橙金 (#FF9F0A) — 当前温度、日期高亮
- **辅助色**: 白色 (#FFFFFF) — 主要文字；浅灰 (#8E8E93) — 次要文字
- **强调色**: 青绿 (#30D158) — 天气图标；红色 (#FF3B30) — 今日日期
- **字体**: 系统无衬线 (sans-serif)，轻量/常规/中粗三层字重

> Glance/RemoteViews 不支持真正的 backdrop-blur，用半透明背景+细边框模拟毛玻璃效果。

### 3.2 5×4 完整版布局

```
┌══════════════════════════════════════┐  ← 外围毛玻璃卡片 (圆角24dp)
│ ┌──────────────────────────────────┐ │
│ │ ● 7月29日            29 ● TODAY │ │  卡片A: 日期信息 (毛玻璃)
│ │ ● 六月初四  广安      Good 早上好│ │
│ │ ● 中伏第八天                    │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ MON  TUE  WED (THU) FRI SAT SUN │ │  卡片B: 星期横条 (毛玻璃, 16dp圆角)
│ └──────────────────────────────────┘ │
│                                      │
│           🌙                         │  主温区: 不套卡片, 直接展示
│           29°                        │
│     最高 39°  /  最低 27°            │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ 周五   周六   周日   周一         │ │  卡片C: 4天预报 (毛玻璃)
│ │ ☀️     ☀️     ⛅     🌧          │ │
│ │ 39/27  38/26  36/25  34/24      │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ 📍广安市                        │ │  卡片D: 城市+指标 (毛玻璃)
│ │ 体感 35°  │  风速 2M/S          │ │
│ │ 湿度 63%  │  气压 1002MB        │ │
│ └──────────────────────────────────┘ │
└══════════════════════════════════════┘
```

**区域比例**: 顶部信息 20% / 星期横条 8% / 主温区 30% / 预报区 22% / 指标区 20%

### 3.3 5×3 精简版布局

裁剪掉未来4天预报和底部指标区：

```
┌──────────────────────────────────────┐
│ ● 7月29日              29  ● TODAY  │
│ ● 六月初四    广安        Good 早上好│
│ ● 中伏第八天                        │
├──────────────────────────────────────┤
│  MON  TUE  WED (THU) FRI  SAT  SUN  │
├──────────────────────────────────────┤
│           🌙                         │
│           29°                        │  主温区等比放大
│     最高 39°  /  最低 27°            │
└──────────────────────────────────────┘
```

**区域比例**: 顶部信息 25% / 星期横条 10% / 主温区 65%

### 3.4 分区点击路由

| 点击区域 | 路由目标 |
|----------|----------|
| 温度区 / 天气图标 | WeatherDetailActivity（详细天气页）|
| 日期区（公历/农历/节日）| 系统日历 App |
| 体感/风速/湿度/气压 | WeatherDetailActivity（定位到对应指标）|
| 未来4天预报 | WeatherDetailActivity |
| 城市名 | 城市选择页（或系统设置→定位）|

---

## 4. 数据模型

### 4.1 WeatherData（统一数据模型）

```kotlin
data class WeatherData(
    val city: String,              // 城市名
    val date: LocalDate,           // 公历日期
    val lunar: LunarInfo,          // 农历信息
    val greeting: String,          // 问候语 (Good 早上好/下午好/晚上好)
    val currentTemp: Int,          // 当前温度
    val highTemp: Int,             // 最高温
    val lowTemp: Int,              // 最低温
    val weatherIcon: WeatherIcon,  // 天气图标枚举
    val weatherText: String,       // 天气描述文本
    val feelsLike: Int,            // 体感温度
    val windSpeed: String,         // 风速 (如 "2M/S")
    val humidity: Int,             // 湿度百分比
    val pressure: String,          // 气压 (如 "1002MB")
    val weeklyForecast: List<DayForecast>,  // 周预报 (取前4天)
    val todayWeekday: Int,         // 今天星期几 (1=Mon..7=Sun)
)

data class LunarInfo(
    val monthName: String,  // "六月"
    val dayName: String,    // "初四"
    val solarTerm: String?, // 节气名 (如"中伏第八天")
    val yearName: String,   // "二〇二六年 / 马年"
)

data class DayForecast(
    val weekday: String,    // "周五"
    val icon: WeatherIcon,
    val high: Int,
    val low: Int,
)
```

### 4.2 天气图标枚举

```kotlin
enum class WeatherIcon {
    SUNNY, CLOUDY, OVERCAST, RAIN, THUNDERSTORM,
    SNOW, MOON_CLEAR, MOON_CLOUDY, MOON_RAIN
    // 各数据源映射到统一枚举
}
```

---

## 5. API 集成

### 5.1 和风天气 v7

| 接口 | 用途 | 关键字段 |
|------|------|----------|
| `/v7/weather/now` | 实时天气 | temp, feelsLike, icon, text, windSpeed, humidity, pressure |
| `/v7/weather/7d` | 7天预报 | daily[].tempMax, tempMin, iconDay, fxDate |
| `/v7/indices/1d` | 农历 | lunarMonth, lunarDay, solarTerm |

### 5.2 OpenWeather (备用)

| 接口 | 用途 | 映射 |
|------|------|------|
| `weather` | 当前天气 | main.temp, wind.speed, humidity, pressure |
| `onecall` | 预报+每日 | daily[].temp.max/min, weather[].icon |

### 5.3 网络层

- OkHttp + Gson
- 超时: connect 10s, read 10s
- 重试: 和风失败 → 立即降级 OW，不重试

---

## 6. 数据刷新策略

| 触发器 | 频率 | 备注 |
|--------|------|------|
| WorkManager 定时 | 每 30 分钟 | 后台刷新 |
| Widget onUpdate | 系统回调时 | 用缓存渲染，避免网络请求阻塞 |
| AppWidgetManager 主动通知 | 数据变更后 | 从 Worker 或手动刷新触发 |

缓存（DataStore）保留最近一次成功数据，作为离线/失败兜底。

---

## 7. 技术栈

- **UI 框架**: Jetpack Glance 1.x (RemoteViews 的 Compose DSL)
- **数据持久化**: DataStore Preferences
- **后台任务**: WorkManager
- **网络**: OkHttp 4.x + Gson
- **图片加载**: Glide（天气图标）
- **语言**: Kotlin 100%
- **最低 SDK**: 26 (Android 8.0，Glance 要求)

---

## 8. 文件组织

```
app/src/main/java/com/pixel/toolbox/weather/
├── api/
│   ├── HeFengApi.kt           // 和风天气 API
│   ├── OWApi.kt               // OpenWeather API
│   └── WeatherApi.kt          // 统一接口定义
├── data/
│   ├── WeatherData.kt         // 数据模型
│   ├── WeatherRepository.kt   // 数据仓库
│   └── WeatherCache.kt        // DataStore 缓存管理
├── widget/
│   ├── WeatherWidget4x3.kt    // 4×3 Widget Provider
│   ├── WeatherWidget4x2.kt    // 4×2 Widget Provider
│   └── WeatherWidgetRenderer.kt // 共享渲染逻辑
├── worker/
│   └── WeatherWorker.kt       // WorkManager Worker
└── ui/
    └── WeatherDetailActivity.kt // 详细天气页
```

---

## 9. 天气图标方案

不使用和风/OW 返回的图标 URL（质量参差），改用本地内置图标：

- 从截图提取/绘制 SVG 图标，转换为 Android Vector Drawable（24dp 基准）
- 和风 icon 码 → WeatherIcon 枚举 → 本地 Vector 资源映射
- OW icon 码 → 同一 WeatherIcon 枚举映射
- 大图标（主温区）36dp，小图标（预报区）20dp，统一青绿色 (#30D158)

## 10. 问候语生成

根据当前小时：
- 06:00-11:59 → "Good 早上好"
- 12:00-17:59 → "Good 下午好"
- 18:00-05:59 → "Good 晚上好"

## 11. 星期横条实现

- 使用 Row + 7 个等宽 Box，每个写入 "MON"~"SUN" 缩写
- 今日对应的 Box 加红色 (#FF3B30) 圆角矩形底色 + 白色文字
- 非今日则为灰色文字无背景
- 不实现滑动/点击切换（Widget 限制），纯静态高亮

## 12. 待定项

- 和风天气 API Key 和 OpenWeather API Key 需用户提供
- 城市选择页暂做简单列表，后续迭代加搜索

[memory_id: memory_00_vgkj34LrCVAdIFOc8HBs3406]