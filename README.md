# Pixel 工具箱 (Pixel Toolbox)

这是一款专门为 Google Pixel 系列手机设计的免 Root 优化工具箱。它利用 [Shizuku](https://shizuku.rikka.app/) 获取系统权限，安全可靠，并且支持动态编译。

## 功能列表

1. **去除 WiFi 感叹号 (Captive Portal 修复)**：一键将验证服务器修改为国内可用的 MIUI 服务器，解决连上 WiFi 依然提示“网络无法连接”或带感叹号的问题。
2. **时区与时间同步修复**：将系统强制定位到亚洲/上海，并将 NTP 时间同步服务器修改为阿里云（`ntp.aliyun.com`），解决时间慢的问题。
3. **气密性检测 (防水检测)**：调用原生气压传感器实时监测气压变化，通过按压屏幕测试手机的气密性是否完好。
4. **一键 5G / VoLTE 开启**：通过底层属性注入和设置修改，强制开启国内四大运营商的 5G 与 VoLTE/VoWiFi 功能。

## 编译方法 (通过 GitHub Actions)

由于本项目支持通过 GitHub 云端直接编译，无需在本地安装复杂的 Android Studio：

1. 将本代码库上传或 Push 到您的 GitHub 仓库。
2. 转到 GitHub 仓库页面的 **Actions** 标签。
3. 点击 **Android Build CI**，然后点击右侧的 **Run workflow**。
4. 编译完成后（约几分钟），进入该次运行的详情页。
5. 在页面最下方的 **Artifacts** 区域，下载 `app-release` 压缩包。
6. 解压后将 APK 文件传到手机上进行安装。

## 依赖与准备
使用本软件的全部功能前，必须在手机上安装并激活 [Shizuku](https://shizuku.rikka.app/zh-hans/)。Shizuku 可以通过无线调试或连接电脑激活，具体激活方式请参考其官网。
