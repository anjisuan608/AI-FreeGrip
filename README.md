# 适人握持（AI FreeGrip Demo）

> 荣耀 AI 随心握（AI FreeGrip / SmartGrip Kit）能力演示程序：根据用户握姿
> （未握持 / 左手 / 右手 / 双手 / 未识别）实时调整 Compose UI 布局与交互入口位置，
> 并完整演示支持状态查询、监听器注册/解注册、错误码处理与合规披露。

| 项 | 值 |
|---|---|
| 包名 | `org.anjisuan608.hihonor.ai_freegrip` |
| 语言 / UI | Kotlin 2.2.10 + Jetpack Compose（Material3） |
| 构建 | AGP 9.4.1，Gradle Version Catalog，Java 17 |
| SDK 范围 | minSdk 34 / targetSdk 37 / compileSdk 37 |
| SDK | `com.hihonor.mcs:smartgripkit:1.0.0.300` |
| 许可证 | [MIT](./LICENSE) |

## 功能

- **握持驱动布局**：5 种握姿映射为演示界面的操作区排布（居中 / 靠左 / 靠右 / 两端对称 / 保持默认），250ms 淡入淡出切换。
- **三页面**：
  - **主页**——设备支持状态卡片 +「商品详情 · 演示」自适应布局（只跟随真实握姿）；
  - **模拟**——分段按钮手动模拟 5 种握姿 +「商品详情 · 模拟」演示区（仅跟随模拟器，与主页解耦）；
  - **设置**——主题模式（跟随系统 / 浅色 / 深色下拉三选一）、OLED 纯黑模式、应用语言、关于入口。
- **关于（合规披露）**：设置页入口打开，展示官方《SDK 合规使用说明》要求的 7 项披露字段、SDK 文档链接与开源信息。
- **错误码可视化**：支持状态 0~4 分别给出引导文案（开关路径、权限路径）；状态 2/3 提供「尝试重新检测」，状态 4 提供「重新检测 / 重启应用」。
- **多语言**：简体中文（默认，应用名「适人握持」）、English（AI FreeGrip）、繁體中文（AI 隨心握），通过 Android 原生应用语言设置切换。

## 支持设备

AI FreeGrip 为荣耀 MagicOS 设备能力，非荣耀设备上查询结果为「设备不支持」并按官方建议隐藏功能入口（SDK 依赖的荣耀私有 framework 类不在普通设备上，本程序对所有 SDK 调用做了降级防御）。

支持的具体机型以官方文档为准：

- [支持的设备列表](https://developer.honor.com/cn/docs/aifreegrip/guides/support-devices)

## 构建

```powershell
# Windows PowerShell（需 Android SDK + JDK 17）
./gradlew build
./gradlew test                 # 单元测试
./gradlew assembleDebug        # 调试 APK
```

无需荣耀设备即可编译与运行 UI（模拟器页可手动预览各握姿）；SDK 真实能力需在支持设备上验证。

## 接入说明（SmartGrip Kit）

以本仓库为参考实现，接入分四步：

1. **Maven 仓**：项目级 `settings.gradle.kts` 的 `dependencyResolutionManagement.repositories` 中添加（本项目用 `FAIL_ON_PROJECT_REPOS`，仓库必须在 settings 层）：

   ```kotlin
   maven { url = uri("https://developer.honor.com/repo") }
   ```

2. **依赖**：统一进 `gradle/libs.versions.toml`：

   ```kotlin
   hihonor-smartgripkit = { group = "com.hihonor.mcs", name = "smartgripkit", version = "1.0.0.300" }
   ```

3. **生命周期**（官方要求）：

   | 时机 | 动作 |
   |---|---|
   | `onCreate` | `getSmartGripSupportState(context)` 查询支持状态（0 支持 / 1 不支持 / 2 开关关 / 3 无权限 / 4 其他） |
   | `onResume` | 状态 0 时 `registerSmartGripMotionListener(context, listener)` 注册 |
   | `onPause` / `onDestroy` | `unregisterSmartGripMotionListener(context, listener)` 解注册（须传同一 listener 实例） |

4. **线程与常量注意**：
   - 监听器回调 `onSmartGripEventChanged(state)` 运行在**系统设备事件线程**，更新 UI 前必须切主线程；
   - 握持状态值不连续：`UNKNOWN = 16`（不是 4/5），映射必须按常量值处理；
   - `SmartGripEventListener` 是**抽象类**，Kotlin 中用 `object : SmartGripEventListener() { ... }` 继承；
   - SDK POM 零依赖、静态初始化块触碰荣耀私有类——非荣耀设备上首次调用即可能抛
     `ExceptionInInitializerError`，**每次 SDK 调用都应包 try/catch（`Throwable`）降级**，勿让其冒泡到 UI。

本仓库将全部 SDK 交互收敛在 `grip/SmartGripRepository` 单一出口，UI 只依赖 `GripState` / `GripSupportStatus`，单元测试无需真机。

官方文档：

- [接入指导](https://developer.honor.com/cn/docs/aifreegrip/guides/smartgrip-guide)
- [合规使用说明](https://developer.honor.com/cn/docs/aifreegrip/guides/compliance-instructions)
- [错误码](https://developer.honor.com/cn/docs/aifreegrip/reference/error-code)

## 合规

- App《隐私声明》按官方样例披露集成 AI FreeGrip 的 7 项字段（见「关于」页）；
- 演示程序纯本地将握姿映射为 UI 适配，**不收集、不上报任何个人信息**；
- 未实现任何自启动 / 关联启动逻辑；
- **未新增任何 manifest 权限**（开关/语言等均走系统设置页，无需声明）。

## 项目结构

```
app/src/main/java/org/anjisuan608/hihonor/ai_freegrip/
  grip/          # 数据层：GripState / GripSupportStatus / SmartGripRepository（SDK 唯一出口）
  ui/            # 主页 / 模拟 / 设置 / 关于 + 自适应布局 + 主题
  MainActivity.kt # 生命周期接线 + 导航 + 设置持久化
app/src/main/res/
  values*/       # 6 份 strings.xml（zh-CN 默认 / zh / zh-rCN / en / zh-rTW / zh-rHK，key 全量对齐）
  xml/           # locales_config.xml 等
```

架构与任务规划详见 [AGENTS.md](AGENTS.md)。

## 开源协议

本程序在 MIT 开源协议下发布，项目中的所有文档和媒体(包括但不限于图片、视频、音频)使用 [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/deed.zh-hans) 协议授权使用。
