package org.anjisuan608.hihonor.ai_freegrip

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.anjisuan608.hihonor.ai_freegrip.grip.GripState
import org.anjisuan608.hihonor.ai_freegrip.grip.GripSupportStatus
import org.anjisuan608.hihonor.ai_freegrip.grip.SmartGripRepository
import org.anjisuan608.hihonor.ai_freegrip.ui.AboutScreen
import org.anjisuan608.hihonor.ai_freegrip.ui.AppPage
import org.anjisuan608.hihonor.ai_freegrip.ui.GripUiState
import org.anjisuan608.hihonor.ai_freegrip.ui.MainScreen
import org.anjisuan608.hihonor.ai_freegrip.ui.SettingsScreen
import org.anjisuan608.hihonor.ai_freegrip.ui.SimulatorScreen
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.DarkMode

/**
 * 单 Activity 三页面：
 * - 生命周期接线（官方 2.4）：`onCreate` 查询支持状态 → `onResume` 注册 → `onPause` 解注册；
 * - 页面切换：外层 [Scaffold] 统一持有 TopAppBar + 底部导航，内容区按 [AppPage] 切换
 *   （页面内不再各自套 Scaffold，避免嵌套 Scaffold 的 inset 双重填充）；
 * - 「关于」是设置页里的入口项：`showAbout` 为 true 时叠加显示关于子页
 *   （顶栏换返回箭头、隐藏底部导航，返回键先回设置页）。
 *
 * 设置项持久化：深色模式三态与 OLED 纯黑开关存 SharedPreferences（仅本机偏好，
 * 不涉及任何个人信息，不触碰合规红线）。
 */
class MainActivity : ComponentActivity() {

    private lateinit var gripRepository: SmartGripRepository
    private lateinit var prefs: SharedPreferences

    private var uiState by mutableStateOf(GripUiState())
    private var currentPage by mutableStateOf(AppPage.Home)

    /** 关于子页是否打开（从设置页入口进入）。 */
    private var showAbout by mutableStateOf(false)

    /** 深色模式三态，默认跟随系统。 */
    private var darkMode by mutableStateOf(DarkMode.System)

    /** OLED 纯黑开关，默认关。 */
    private var oledBlack by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        gripRepository = SmartGripRepository(applicationContext)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // 恢复设置项；脏数据由 DarkMode.fromName 回退「跟随系统」
        darkMode = DarkMode.fromName(prefs.getString(KEY_DARK_MODE, null))
        oledBlack = prefs.getBoolean(KEY_OLED_BLACK, false)

        // 官方要求：onCreate 查询设备支持状态
        uiState = uiState.copy(support = gripRepository.querySupportState())

        setContent {
            AIFreegripTheme(
                darkTheme = darkMode.resolvesToDark(isSystemInDarkTheme()),
                oledBlack = oledBlack,
            ) {
                // 返回键：关于 → 回设置页；主页以外的 tab → 回主页；主页保留系统默认行为
                BackHandler(enabled = showAbout || currentPage != AppPage.Home) {
                    if (showAbout) {
                        showAbout = false
                    } else {
                        currentPage = AppPage.Home
                    }
                }
                AppScaffold(
                    page = currentPage,
                    state = uiState,
                    showAbout = showAbout,
                    darkMode = darkMode,
                    oledBlack = oledBlack,
                    onPageChange = { currentPage = it },
                    onCloseAbout = { showAbout = false },
                    onRecheck = ::recheckSupport,
                    onOpenSettings = ::openSystemSettings,
                    onRestart = ::restartApp,
                    onDarkModeChange = ::applyDarkMode,
                    onOledBlackChange = ::applyOledBlack,
                    onOpenLanguageSettings = ::openAppLocaleSettings,
                    onOpenAbout = { showAbout = true },
                    onSimulate = { simulated ->
                        uiState = uiState.copy(simulatedGrip = simulated)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 官方要求：设备支持时注册；repository 内部对重复注册幂等
        if (uiState.support.canRegister) registerIfPossible()
    }

    override fun onPause() {
        // 官方要求：onPause/onDestroy 解注册，避免监听器泄漏
        gripRepository.unregister()
        uiState = uiState.copy(registered = false)
        super.onPause()
    }

    /** 外层脚手架：顶栏（标题随页面变）+ 底部三选一导航 + 内容区。 */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppScaffold(
        page: AppPage,
        state: GripUiState,
        showAbout: Boolean,
        darkMode: DarkMode,
        oledBlack: Boolean,
        onPageChange: (AppPage) -> Unit,
        onCloseAbout: () -> Unit,
        onRecheck: () -> Unit,
        onOpenSettings: () -> Unit,
        onRestart: () -> Unit,
        onDarkModeChange: (DarkMode) -> Unit,
        onOledBlackChange: (Boolean) -> Unit,
        onOpenLanguageSettings: () -> Unit,
        onOpenAbout: () -> Unit,
        onSimulate: (GripState?) -> Unit,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (showAbout) {
                    // 关于子页：顶栏换返回箭头（不打断设置页的 tab 选择）
                    TopAppBar(
                        title = { Text(stringResource(R.string.about_title)) },
                        navigationIcon = {
                            IconButton(onClick = onCloseAbout) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                        },
                    )
                } else {
                    TopAppBar(title = { Text(stringResource(page.titleRes)) })
                }
            },
            bottomBar = {
                // 关于子页期间隐藏底部导航（它是设置的子页面，不是第四个 tab）
                if (!showAbout) {
                    NavigationBar {
                        AppPage.entries.forEach { item ->
                            NavigationBarItem(
                                selected = page == item,
                                onClick = { onPageChange(item) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.labelRes)) },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            if (showAbout) {
                AboutScreen(modifier = Modifier.padding(innerPadding))
            } else {
                when (page) {
                    AppPage.Home -> MainScreen(
                        state = state,
                        onRecheck = onRecheck,
                        onOpenSettings = onOpenSettings,
                        onRestart = onRestart,
                        modifier = Modifier.padding(innerPadding),
                    )

                    AppPage.Simulator -> SimulatorScreen(
                        state = state,
                        onSimulate = onSimulate,
                        modifier = Modifier.padding(innerPadding),
                    )

                    AppPage.Settings -> SettingsScreen(
                        darkMode = darkMode,
                        oledBlack = oledBlack,
                        onDarkModeChange = onDarkModeChange,
                        onOledBlackChange = onOledBlackChange,
                        onOpenLanguageSettings = onOpenLanguageSettings,
                        onOpenAbout = onOpenAbout,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    /** 「尝试重新检测」：重新查询支持状态，若恢复支持则立即补注册。 */
    private fun recheckSupport() {
        val status = gripRepository.querySupportState()
        uiState = uiState.copy(support = status, registered = false)
        if (status.canRegister) registerIfPossible()
    }

    private fun registerIfPossible() {
        val ok = gripRepository.register { grip ->
            // 回调已在 repository 内切到主线程
            uiState = uiState.copy(grip = grip)
        }
        uiState = uiState.copy(
            registered = ok,
            // 注册失败按官方映射为「其他错误」（状态 4），展示对应引导
            support = if (ok) uiState.support else GripSupportStatus.OtherError,
        )
    }

    /** 深色模式三态：写内存状态（即时换肤）+ 落盘（重启保留）。 */
    private fun applyDarkMode(mode: DarkMode) {
        darkMode = mode
        prefs.edit().putString(KEY_DARK_MODE, mode.name).apply()
    }

    /** OLED 纯黑开关：写内存状态 + 落盘，默认关。 */
    private fun applyOledBlack(enabled: Boolean) {
        oledBlack = enabled
        prefs.edit().putBoolean(KEY_OLED_BLACK, enabled).apply()
    }

    /**
     * 重启应用（状态 4「其他错误」的自救入口）：用启动器 Intent 以
     * `CLEAR_TASK + NEW_TASK` 重建任务栈——等效冷启动 UI，重新走一遍
     * onCreate → 支持状态查询 → 注册流程。用户主动点击，不属于「自启动」；
     * 不需要任何 manifest 权限。
     */
    private fun restartApp() {
        runCatching {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?: error("launch intent unavailable")
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            )
            startActivity(intent)
        }.onFailure { e -> Log.w(TAG, "cannot restart app", e) }
    }

    /**
     * 打开系统一级设置页。用户需自行进入开关/权限管理项
     * （官方引导文案已写明路径；深链到二级页依赖厂商私有 action，不通用）。
     * `ACTION_SETTINGS` 不需要任何权限，不违反「不新增权限」红线。
     */
    private fun openSystemSettings() {
        runCatching { startActivity(Intent(AndroidSettings.ACTION_SETTINGS)) }
            .onFailure { e -> Log.w(TAG, "cannot open system settings", e) }
    }

    /**
     * 打开 Android 原生「应用语言」页（`ACTION_APP_LOCALE_SETTINGS`，API 33+，本项目 minSdk 34）。
     * 该 action 不需要任何权限；个别 ROM 移除入口时回退到本应用的详情页
     * （`ACTION_APPLICATION_DETAILS_SETTINGS`），用户仍可在系统页中管理语言/权限。
     */
    private fun openAppLocaleSettings() {
        val packageUri = Uri.fromParts("package", packageName, null)
        runCatching {
            startActivity(Intent(AndroidSettings.ACTION_APP_LOCALE_SETTINGS, packageUri))
        }.onFailure { e ->
            Log.w(TAG, "locale settings unavailable, fallback to app details", e)
            runCatching {
                startActivity(
                    Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
                )
            }.onFailure { e2 -> Log.w(TAG, "cannot open app details settings", e2) }
        }
    }

    private companion object {
        const val TAG = "AIFreegrip"

        /** 偏好文件名与 key：仅存本机 UI 偏好，不含任何个人信息。 */
        const val PREFS_NAME = "app_settings"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_OLED_BLACK = "oled_black"
    }
}
