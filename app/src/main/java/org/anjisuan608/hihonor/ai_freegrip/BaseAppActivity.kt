package org.anjisuan608.hihonor.ai_freegrip

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme

/**
 * 四个界面 Activity 的公共基类：
 *
 * - 主题包裹 [AppTheme]：读 [AIFreegripApp] 的 darkMode/oledBlack 全局设置；
 * - 握持监听生命周期桥接（官方 2.4）：每个前台界面 onResume 注册、onPause 解注册。
 *   同屏同时只有一个 Activity，系统切换顺序为 A.pause → B.resume，
 *   repository 对重复注册幂等，界面轮换不会打断监听。
 */
abstract class BaseAppActivity : ComponentActivity() {

    protected val app: AIFreegripApp
        get() = application as AIFreegripApp

    override fun onResume() {
        super.onResume()
        app.onResumeRegister()
    }

    override fun onPause() {
        app.onPauseUnregister()
        super.onPause()
    }

    /** 全应用统一主题：深色三态解析 + OLED 纯黑覆盖。 */
    @Composable
    protected fun AppTheme(content: @Composable () -> Unit) {
        AIFreegripTheme(
            darkTheme = app.darkMode.resolvesToDark(isSystemInDarkTheme()),
            oledBlack = app.oledBlack,
            content = content,
        )
    }
}
