
# 致谢与参考项目

本项目在开发过程中参考、借鉴并集成了以下开源项目。衷心感谢所有开源作者与社区贡献者。

## 直接参考与集成项目

| 项目 | 作者 / 组织 | 许可证 | 用途 |
| --- | --- | --- | --- |
| [ShizuCallRecorder](https://github.com/kitsumed/ShizuCallRecorder) | kitsumed | GPL-3.0 | 基于 Shizuku 的通话录音核心实现思路（来电/去电自动录音） |
| [scrcpy](https://github.com/Genymobile/scrcpy) | Genymobile | Apache-2.0 | 通话录音的音频源 / 音频编码参考实现（Opus/AAC 编码、语音通信/通话音频源） |
| [Shizuku](https://github.com/RikkaApps/Shizuku) | RikkaApps | Apache-2.0 | 系统权限桥接方案，免 Root 获取系统级能力 |
| [MiPushFramework](https://github.com/Trumeet/MiPushFramework) | Trumeet | GPL-3.0 | xmsf 服务框架 |
| [AndResGuard](https://github.com/shwenzhang/AndResGuard) | 360（shwenzhang） | Apache-2.0 | 资源混淆与压缩方案（构建产物） |
| [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) | LSPosed | Apache-2.0 | 绕过 Android 隐藏 API 限制 |
| [ARSCLib](https://github.com/REAndroid/ARSCLib) | REAndroid | Apache-2.0 | 读写 Android 二进制资源文件（resources.arsc） |
| [carrier-ims](https://github.com/ryfineZ/carrier-ims-for-pixel) | ryfineZ | Apache-2.0 | Pixel 5G / VoLTE / VoWiFi 蜂窝网络全特性优化的核心实现思路 |

> 注：应用内打包的 `assets/scrcpy-server` 二进制来自 [scrcpy](https://github.com/Genymobile/scrcpy)（Apache-2.0），随应用按原许可证分发，版权归 Genymobile 所有。

## 使用的主要开源组件与框架

| 组件 | 作者 / 组织 | 许可证 | 用途 |
| --- | --- | --- | --- |
| Jetpack Compose | Google / AOSP | Apache-2.0 | Android 声明式 UI 框架 |
| Material 3 | Google / AOSP | Apache-2.0 | 设计系统与视觉语言 |
| Kotlin | JetBrains | Apache-2.0 | 主要开发语言 |
| apksig | Google | Apache-2.0 | APK v1 + v2 签名方案 |

## 许可证说明

- 本项目自身遵循 **GPL-3.0**（详见根目录 `LICENSE` 文件）。
- 引用的开源组件大多遵循 **Apache License 2.0**（允许自由使用、修改与分发，需保留版权声明）。
- 其中 **ShizuCallRecorder** 遵循 **GPL-3.0**，因其为直接参考的通话录音核心实现来源，本项目整体以 GPL-3.0 开源以保持兼容。

再次感谢以上所有项目及其作者的开源奉献。

## 素材与资源
- `app/src/main/assets/earth_texture.jpg`：GPS 测试页卫星地球 3D 视图贴图素材，版权归原作者所有，仅用于应用内功能展示。
- `app/src/main/res/drawable/donate_qr.jpg`、`wechat_donate.jpg`：开发者收款码，版权归开发者所有。
