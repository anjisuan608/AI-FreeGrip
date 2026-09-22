package org.anjisuan608.hihonor.ai_freegrip

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.anjisuan608.hihonor.ai_freegrip.ui.AppPage
import org.anjisuan608.hihonor.ai_freegrip.ui.AppScaffold
import org.anjisuan608.hihonor.ai_freegrip.ui.MainScreen
import org.anjisuan608.hihonor.ai_freegrip.ui.SettingsScreen
import org.anjisuan608.hihonor.ai_freegrip.ui.SimulatorScreen

/**
 * 主活动——承载主体三页（主页/模拟/设置）：`AppPage` 枚举 + `mutableStateOf`
 * 内存切换（singleTask，避免 shortcut 多次点击堆叠实例），**不引入导航库**；
 * 「关于」是独立的 [AboutActivity]（从设置入口或深链压栈打开）。
 *
 * 持有 MAIN/LAUNCHER、App Shortcuts meta-data 与深链
 * `aifreegrip://page/{home|simulator|settings|about}` 的 VIEW intent-filter——
 * scheme 必须可被隐式 resolve，否则 launcher 点静态 shortcut 报「未找到应用」。
 * 深链在 [onCreate]/[onNewIntent] 解析：simulator/settings 切 tab，
 * about 先切设置 tab 再压入 AboutActivity（返回链 关于→设置→主页）。
 *
 * 握持监听生命周期由 [BaseAppActivity] 桥接到 [AIFreegripApp]；
 * 设置项/模拟器/握姿状态都在 Application 级共享。
 */
class MainActivity : BaseAppActivity() {

    /** 当前 tab；默认主页。 */
    private var currentPage by mutableStateOf(AppPage.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // App Shortcuts / 深链入口（冷启动即路由）
        applyShortcutRoute(intent)

        setContent {
            AppTheme {
                // 返回键：非主页 tab 先回主页；主页交系统（finish + 预测性返回动画）
                BackHandler(enabled = currentPage != AppPage.Home) {
                    currentPage = AppPage.Home
                }
                AppScaffold(
                    page = currentPage,
                    onNavigate = { currentPage = it },
                ) { padding ->
                    // 页面切换滑动过渡：前进（序号增，如 主页→设置）新页自右滑入、
                    // 旧页向左滑出，后退方向相反；padding 挂在动画容器外避免抖动
                    AnimatedContent(
                        targetState = currentPage,
                        modifier = padding.fillMaxSize(),
                        transitionSpec = {
                            // receiver 是 AnimatedContentTransitionScope（实现 Transition.Segment）：
                            // 属性为 initialState/targetState（无 currentState，API 已改名）
                            val enterDir =
                                if (targetState.ordinal >= initialState.ordinal) 1 else -1
                            (slideInHorizontally(tween(300)) { full -> full * enterDir } +
                                fadeIn(tween(300)))
                                .togetherWith(
                                    slideOutHorizontally(tween(300)) { full -> -full * enterDir } +
                                        fadeOut(tween(160)),
                                )
                        },
                        label = "page-transition",
                    ) { page ->
                        when (page) {
                            AppPage.Home -> MainScreen(
                                state = app.uiState,
                                onRecheck = app::recheckSupport,
                                onOpenSettings = { app.openSystemSettings(this@MainActivity) },
                                onRestart = { app.restartApp(this@MainActivity) },
                            )

                            AppPage.Simulator -> SimulatorScreen(
                                state = app.uiState,
                                onSimulate = app::setSimulatedGrip,
                            )

                            AppPage.Settings -> SettingsScreen(
                                darkMode = app.darkMode,
                                oledBlack = app.oledBlack,
                                onDarkModeChange = app::applyDarkMode,
                                onOledBlackChange = app::applyOledBlack,
                                onOpenLanguageSettings = {
                                    app.openAppLocaleSettings(this@MainActivity)
                                },
                                onOpenAbout = {
                                    startActivity(
                                        Intent(this@MainActivity, AboutActivity::class.java),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 应用已在前台时点 shortcut：singleTask 实例走 onNewIntent 而非新建
        applyShortcutRoute(intent)
    }

    /**
     * 解析深链 `aifreegrip://page/{id}`：simulator/settings 切对应 tab；
     * about 先切设置 tab 再压入 [AboutActivity]，保持返回链 关于→设置→主页；
     * home/未知 data 回主页，静默忽略未知值。
     */
    private fun applyShortcutRoute(intent: Intent?) {
        when (intent?.data?.lastPathSegment) {
            "simulator" -> currentPage = AppPage.Simulator
            "settings" -> currentPage = AppPage.Settings
            "about" -> {
                currentPage = AppPage.Settings
                startActivity(Intent(this, AboutActivity::class.java))
            }
            else -> currentPage = AppPage.Home
        }
    }
}
