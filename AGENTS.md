# AGENTS.md — AI-Freegrip 演示程序开发规划

本文件指导 AI/开发者在本仓库中完成「荣耀 AI 随心握（AI FreeGrip）」演示程序的开发。动手前请通读本文，尤其是「官方文档要点」「合规红线」「任务分期」三节。

---

## 1. 项目概述

| 项 | 值 |
|---|---|
| 项目名 | AI-Freegrip（单模块 `:app`） |
| 包名 | `org.anjisuan608.hihonor.ai_freegrip` |
| 语言/UI | Kotlin 2.2.10 + Jetpack Compose（Material3，BOM 2026.02.01） |
| 构建 | AGP 9.4.1，Gradle Version Catalog（`gradle/libs.versions.toml`），configuration-cache 已开启 |
| SDK 范围 | minSdk 34 / targetSdk 37 / compileSdk 37，**Java 17**（2026-09 由 11 升级；构建 daemon 用 JDK 25 LTS） |
| 现状 | 已完成 P0：接入 SmartGripKit SDK 依赖（坐标经官方 Maven 仓实测） |

**目标**：做一个演示程序，展示 AI FreeGrip 握持检测能力——根据用户握姿（未握持/左手/右手/双手/未识别）实时调整 Compose UI 布局与交互入口位置，并完整演示支持状态查询、监听器注册/解注册、错误码处理与合规披露。

---

## 2. 官方文档要点（已阅读，作为实现依据）

参考文档（内容以线上为准，若与本节冲突以文档链接为准）：

1. 接入指导：<https://developer.honor.com/cn/docs/aifreegrip/guides/smartgrip-guide>
2. 合规使用说明：<https://developer.honor.com/cn/docs/aifreegrip/guides/compliance-instructions>
3. 错误码：<https://developer.honor.com/cn/docs/aifreegrip/reference/error-code>
4. 附：SmartGrip Kit SDK 清单：<https://developer.honor.com/cn/docs/aifreegrip/sdk/smartgripkit>

### 2.1 SDK 集成（Maven）

- 项目级 `settings.gradle.kts` 的 `dependencyResolutionManagement.repositories` 中添加：

  ```kotlin
  maven { url = uri("https://developer.honor.com/repo") }
  ```

  注意：本项目 `settings.gradle.kts` 使用 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`，仓库**必须**加在 settings 层，禁止写进模块 build.gradle。

- 应用级依赖（**已实测验证**：直接 GET 官方 Maven 仓，pom/aar 均 200）：

  ```kotlin
  // libs.versions.toml 中统一管理
  hihonor-smartgripkit = { group = "com.hihonor.mcs", name = "smartgripkit", version = "1.0.0.300" }
  ```

  ⚠️ 坐标勘误：接入指南正文的 `com.hihonor.mcs:smartgripkit` **正确**；SDK 清单页所写的 `com.hihonor.mcs:smartgrip` 在 Maven 仓中为 404，系清单页笔误，不要使用。若未来版本变更，以 `https://developer.honor.com/repo/com/hihonor/mcs/smartgripkit/maven-metadata.xml` 为准。

- **运行时风险（已由 SDK 源码实测确认，P1 必须防御）**：
  - POM **零依赖**：SDK 依赖的 `com.hihonor.android.hwextdevice.*`、`com.hihonor.android.fsm.*`、`com.hihonor.android.os.SystemPropertiesEx` 等**荣耀私有 framework 类不随包分发**，只存在于 MagicOS 设备上。
  - `SmartGripEventManager` 的**静态初始化块**里就调用了 `SystemPropertiesEx.getInt()` / `new HWExtMotion()` / `HwFoldScreenManagerEx.isFoldable()`——在非荣耀设备上首次触碰该类即抛 `ExceptionInInitializerError`/`NoClassDefFoundError`。
  - 因此 `SmartGripRepository` 对 SDK 的每次调用都必须包 try/catch（捕 `Throwable`），失败时降级为「设备不支持」状态，绝不让 crash 冒泡到 UI；Compose `@Preview`、单元测试**一律不得触碰 SDK 类**。

### 2.2 API 速览（`SmartGripEventManager`，包名 `com.hihonor.smartgripkit`，全部静态方法，私有构造）

| 方法 | 返回 | 说明 |
|---|---|---|
| `getSmartGripSupportState(Context)` | `Int` | 查询设备支持状态，错误码见 2.3；Context 建议传 `ApplicationContext` |
| `registerSmartGripMotionListener(Context, SmartGripEventListener)` | `Boolean` | `true` 成功；`false` 失败（入参为空/系统服务不可用） |
| `unregisterSmartGripMotionListener(Context, SmartGripEventListener)` | `Boolean` | 解注册，返回是否成功；**须传同一 listener 实例** |
| 监听器回调 `onSmartGripEventChanged(int state)` | — | ⚠️ **运行在系统设备事件线程，非主线程**，更新 UI 必须切主线程 |

> ⚠️ `SmartGripEventListener` 是**抽象类**（`public abstract class`），Kotlin 中必须用 `object : SmartGripEventListener() { ... }` 继承，不能当接口实现。
> ⚠️ 握持状态值**不连续**：`GRIP_STATE_UNKNOWN = 16`（不是 4/5），映射必须显式按常量值处理，不能按序号推断。

SDK 源码（AAR 内 sources jar）实测行为，影响测试预期：

- **解注册非幂等**：`unregister` 对未注册的 listener 返回 `false` 并只打日志，不抛异常 —— 我方仍须自行保证 register/unregister 成对、不重复。
- **多监听器共享**：SDK 内部用 `CopyOnWriteArraySet` 管理多个 listener，仅首个注册时真正调系统接口、最后一个移除时才注销系统监听 —— 我方重复注册同一 listener 不会泄漏系统资源，但状态位要自己维护。
- **横屏可能收不到回调**：老版本（`SMART_GRIP_VERSION <= 1`）在非折叠屏上**仅竖屏分发事件**，横屏被 SDK 主动过滤。P4 验证「横竖屏切换」时，横屏无回调属预期行为，不是 bug。
- Context 传 `ApplicationContext`（SDK 内部也用它创建匿名监听器，避免 Activity 泄漏）。

握持状态常量（5 种，值见上）：

| 常量 | 值 | 含义 | 演示 UI 策略 |
|---|---|---|---|
| `GRIP_STATE_NOT_HELD` | 0 | 未握持 | 居中默认布局 |
| `GRIP_STATE_LEFT_HAND` | 1 | 左手握持 | 交互入口靠左 |
| `GRIP_STATE_RIGHT_HAND` | 2 | 右手握持 | 交互入口靠右 |
| `GRIP_STATE_BOTH_HANDS` | 3 | 双手握持 | 对称布局 |
| `GRIP_STATE_UNKNOWN` | 16 | 未识别 | 保持当前或恢复默认 |

### 2.3 错误码与处理（`getSmartGripSupportState` 返回值）

| 值 | 常量 | 含义 | 处理建议（必须照做） |
|---|---|---|---|
| 0 | `SMART_GRIP_SUPPORT` | 设备支持 | 继续注册监听器 |
| 1 | `SMART_GRIP_NOT_SUPPORT` | 设备不支持 | 隐藏功能入口，参考支持设备列表文档 |
| 2 | `SMART_GRIP_SETTING_OFF` | 用户关闭了开关 | 引导：**设置 > 智能辅助 > AI 随心握** 开启后再注册 |
| 3 | `SMART_GRIP_NO_PERMISSION` | 无权限 | 引导：**设置 > 隐私和安全 > 权限管理 > 设备动作与方向** 中将本应用设为「允许」，参见 FAQ |
| 4 | `SMART_GRIP_REGISTER_FAILED_OTHER` | 其他错误 | 检查 Context 是否为空、系统服务是否可用，参见 FAQ |

### 2.4 生命周期映射（官方建议，必须遵守）

| Activity 生命周期 | 动作 |
|---|---|
| `onCreate` | 调 `getSmartGripSupportState` 查询支持状态 |
| `onResume` | 设备支持（状态 0）时调 `registerSmartGripMotionListener` 注册 |
| `onPause` / `onDestroy` | 调 `unregisterSmartGripMotionListener` 解注册，避免监听器泄漏 |

---

## 3. 合规红线（来自合规使用说明，任何改动不得违反）

1. **隐私声明披露**：App《隐私声明》必须明确告知集成了 AI 随心握服务，按官方样例披露下列字段（`app/src/main/res/values/strings.xml` 或专门页面中提供可展示的文案）：
   - 第三方公司名称：**荣耀终端股份有限公司**
   - 第三方 SDK 名称：**AI FreeGrip**
   - SDK 使用目的：给三方应用提供荣耀 AI 随心握（握姿检测）开放能力
   - SDK 使用场景：应用可根据用户握姿（左手/右手/双手/未握持）调整 UI 布局、交互入口位置等
   - 处理的个人信息类型：**不涉及**
   - 实现 SDK 功能所需权限：**不涉及**
   - SDK 隐私声明链接：**不涉及**
2. **不收集个人信息**：获得用户同意前（本演示程序为纯本地握姿 → UI 适配，全程不收集、不上报任何个人信息），不得收集最终用户个人信息。
3. **不自启动/关联启动**：非服务所必需不得自启动或关联启动；本演示程序不得实现任何自启动逻辑。
4. **不新增权限**：SDK「所需权限：不涉及」。除非官方文档更新，**不要**在 `AndroidManifest.xml` 增加任何 `<uses-permission>`；状态 3 提到的「设备动作与方向」是系统侧权限管理入口，非 manifest 声明项。
5. **使用最新版 SDK**：版本升级时及时更新（当前 1.0.0.300）。
6. 合规接口配置说明一节官方标注「**不涉及**”，无需额外配置。

---

## 4. 架构规划

包结构（在 `app/src/main/java/org/anjisuan608/hihonor/ai_freegrip/` 下）：

```
grip/
  GripState.kt            # UI 层枚举：NotHeld/LeftHand/RightHand/BothHands/Unknown，与 SDK int 互转
  GripSupportStatus.kt    # 支持状态枚举 + 错误码 → 文案/引导动作 的映射
  SmartGripRepository.kt  # 对 SmartGripEventManager 的唯一封装（SDK 交互出口，便于测试替身）
ui/
  AppPage.kt              # 底部导航三页面枚举（主页/模拟/设置）：导航 label、顶栏标题、底部图标
  MainScreen.kt           # 主页：GripUiState + 设备支持模块 + 演示模块（含 GripUiState 定义）
  GripStatusCard.kt       # 展示当前支持状态/握持状态/错误引导
  AdaptedLayout.kt        # 根据 GripState 重排的演示布局（居中/靠左/靠右/对称）
  SimulatorScreen.kt      # 模拟页：演示模拟器 + 演示区「商品详情 · 模拟」（仅跟随模拟器，复用 AdaptedLayout）
  SettingsScreen.kt       # 设置页：主题模式下拉菜单三选一 + OLED 纯黑开关 + 应用语言入口 + 关于入口
  AboutScreen.kt          # 关于子页（设置内入口打开）：合规披露 7 字段 + HONOR 开发者链接 + 作者/MIT/仓库
  theme/DarkMode.kt       # 深色三态枚举：System/Light/Dark（SharedPreferences 持久化，脏数据回退 System）
MainActivity.kt           # 生命周期接线 + 外层 Scaffold（顶栏 + 底部导航）+ 页面切换
ui/theme/                 # 沿用模板主题，不引入第三方主题库
```

导航约定：单 Activity 三页面（主页/模拟/设置），`AppPage` 枚举 + `mutableStateOf` 切换，**不引入导航库**；
「关于」是设置页里的一个入口项，点开后作为**设置的子页面**叠加显示（顶栏换返回箭头、隐藏底部导航，返回键先回设置页）。
返回处理走 `BackHandler`（OnBackPressedDispatcher），manifest 已开 `enableOnBackInvokedCallback` 适配**预测性返回**：
子页/非主页拦截返回时为应用内回退，主页不拦截、交系统播放返回跟手动画后退出。
**App Shortcuts**：静态四入口（主页/模拟/设置/关于）声明于 `res/xml/shortcuts.xml`（activity 的 `android.app.shortcuts` meta-data），
经 `aifreegrip://page/{id}` 深链路由——`MainActivity.applyShortcutRoute` 在 `onCreate`/`onNewIntent` 解析切换页面，
「关于」路由到设置页并叠加子页；scheme 不注册 VIEW filter，外部应用无法借此启动本应用。
外层 `Scaffold` 统一持有 TopAppBar 与底部 `NavigationBar`，页面内容组件不再各自套 Scaffold（避免嵌套 inset 双重填充）。
外跳链接一律 `LocalUriHandler.openUri` 交给系统浏览器，**不新增 manifest 权限**。

显示与语言（设置页）：
- **主题模式**：设置行点开下拉菜单三选一（跟随系统/浅色/深色），**默认跟随系统**；状态存 SharedPreferences。
- **OLED 纯黑**：仅深色模式下把背景与各层级表面覆盖为纯黑（`Theme.kt` 中 `oledBlack` 参数），默认关；
  纯黑会让模块卡与背景融为一体，激活时经 `oledModuleBorder()` 给模块卡补 1dp `outlineVariant` 范围边框。
- **应用语言**：跳 Android 原生 `Settings.ACTION_APP_LOCALE_SETTINGS`（`res/xml/locales_config.xml` 声明
  zh-CN/zh-TW/zh-HK/en-US），个别 ROM 无该页时回退 `ACTION_APPLICATION_DETAILS_SETTINGS`；均不需要 manifest 权限。
- **多语言资源**：6 份 `strings.xml`（`values/` 默认 zh-CN、`values-zh/`、`values-zh-rCN/`、`values-en/`、
  `values-zh-rTW/`、`values-zh-rHK/`），**key 必须全量对齐**（lint MissingTranslation）。
  应用名/标题：zh-CN 为「适人握持」，en 为「AI FreeGrip」，zh-TW/HK 为「AI 隨心握」。

设计约定：

- **单一出口**：所有 SDK 调用只出现在 `SmartGripRepository`，UI 只依赖 `GripState`/`GripSupportStatus`，不直接 import `com.hihonor.*`。这样单元测试无需真机/SDK。
- **状态持有**：`MainActivity` 用 `mutableStateOf` 持有 `GripState` 与支持状态；Repository 以回调暴露状态，回调内**先切主线程**（`Handler(Looper.getMainLooper())` 或 `runOnUiThread`）再更新 Compose state。
- **注册幂等**：`onResume` 注册前判断当前支持状态为 0 且尚未注册，防止重复注册；`onPause` 解注册并置空 listener。
- **错误可视化**：状态 1/2/3/4 分别在 `GripStatusCard` 显示对应引导文案（含 2.3 表中的设置路径，状态 2 提供「智能辅助」与「荣耀 AI & YOYO」两条路径），状态 2、3 提供「尝试重新检测」按钮，状态 4 提供「重新检测 + 重启应用」按钮。
- **不引入** Hilt/Room/Retrofit/导航库——演示程序单 Activity + Compose 即可，避免过度设计。

---

## 5. 任务分期（按顺序执行，每期结束须可编译）

> 构建/测试命令（Windows PowerShell）：`./gradlew build`、`./gradlew test`、`./gradlew connectedAndroidTest`（需设备）。无荣耀真机时：SDK 相关代码保证编译通过，UI 用模拟状态预览。

- **P0 — 仓库与依赖**：settings.gradle.kts 加 Maven 仓；`libs.versions.toml` 加 smartgrip 依赖；验证坐标可解析（见 2.1 风险点）；提交一次。
- **P1 — 数据层**：实现 `GripState`、`GripSupportStatus`、`SmartGripRepository`（支持状态查询 + 注册/解注册 + 回调线程切换）；为状态映射写单元测试（`app/src/test`）。
- **P2 — 主界面**：`MainScreen` + `GripStatusCard` + `AdaptedLayout`，按 2.4 生命周期接到 `MainActivity`；5 种握姿 + 5 种支持状态均可在 Compose `@Preview` 中预览。
- **P3 — 合规页**：`AboutScreen` 展示第 3 节披露字段；从设置页「关于」入口可达（子页面 + 返回）。
- **P4 — 真机验证**：在支持设备（见支持的设备列表文档）上验证：支持状态 0 全流程、状态 2/3 的引导跳转文案、onPause/onResume 反复注册无泄漏、横竖屏/前后台切换稳定。
- **P5 — 收尾**：README（接入步骤 + 截图 + 支持设备说明）、`.gitignore` 复核（勿提交 aar/keystore/local.properties）、全量构建通过。

---

## 6. 通用约定

- 新增依赖一律进 `gradle/libs.versions.toml`，不写死坐标。
- Kotlin 官方代码风格（`kotlin.code.style=official`）；中文注释解释「为什么」，不复述代码。
- 敏感文件（`*.jks`/`*.keystore`/`keystore.properties`/`local.properties`/`.idea/`）已被 `.gitignore` 排除，禁止提交。
- 修改 SDK 交互时，回头核对第 2、3 节，确保生命周期、线程、错误处理、合规披露未被破坏。
- 文档要点与线上不一致时，以第 2 节列出的官方链接为准，并同步更新本文件。
