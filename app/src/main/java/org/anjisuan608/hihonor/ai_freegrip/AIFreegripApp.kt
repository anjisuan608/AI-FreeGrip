package org.anjisuan608.hihonor.ai_freegrip

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.anjisuan608.hihonor.ai_freegrip.grip.GripState
import org.anjisuan608.hihonor.ai_freegrip.grip.GripSupportStatus
import org.anjisuan608.hihonor.ai_freegrip.grip.SmartGripRepository
import org.anjisuan608.hihonor.ai_freegrip.ui.GripUiState
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.DarkMode

/**
 * 应用级共享容器——多 Activity 拆分后的**唯一状态源**。
 *
 * 四个界面（主页/模拟/设置/关于）各自是独立导出 Activity，但观察同一份
 * [uiState]（支持状态/握姿/注册位/模拟器选项）与主题设置；握持监听的
 * 注册/解注册由 [BaseAppActivity] 在每个前台 Activity 的 onResume/onPause
 * 桥接到这里（前台同时只有一个，系统切换顺序 pause→resume 保证监听不断档，
 * repository 对重复注册幂等）。
 *
 * 设置项持久化：深色模式三态与 OLED 纯黑开关存 SharedPreferences（仅本机偏好，
 * 不涉及任何个人信息，不触碰合规红线）。
 */
class AIFreegripApp : Application() {

    lateinit var gripRepository: SmartGripRepository
        private set

    lateinit var prefs: android.content.SharedPreferences
        private set

    /** 全应用共享的 UI 状态；所有 Activity 订阅同一实例（Compose state 自动重组）。 */
    var uiState by mutableStateOf(GripUiState())
        private set

    /** 深色模式三态，默认跟随系统。 */
    var darkMode by mutableStateOf(DarkMode.System)
        private set

    /** OLED 纯黑开关，默认关。 */
    var oledBlack by mutableStateOf(false)
        private set

    override fun onCreate() {
        super.onCreate()
        gripRepository = SmartGripRepository(applicationContext)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // 恢复设置项；脏数据由 DarkMode.fromName 回退「跟随系统」
        darkMode = DarkMode.fromName(prefs.getString(KEY_DARK_MODE, null))
        oledBlack = prefs.getBoolean(KEY_OLED_BLACK, false)

        // 官方要求：应用启动即查询设备支持状态（Application 创建早于任何界面）
        uiState = uiState.copy(support = gripRepository.querySupportState())
    }

    // ------------------------------------------------------------------
    // 握持监听生命周期（官方 2.4；由 BaseAppActivity 对每个前台界面调用）
    // ------------------------------------------------------------------

    /** 前台界面 onResume：设备支持时注册（内部幂等）。 */
    fun onResumeRegister() {
        if (uiState.support.canRegister) registerIfPossible()
    }

    /** 前台界面 onPause：解注册，避免监听器泄漏。 */
    fun onPauseUnregister() {
        gripRepository.unregister()
        uiState = uiState.copy(registered = false)
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

    /** 「尝试重新检测」：重新查询支持状态，若恢复支持则立即补注册。 */
    fun recheckSupport() {
        val status = gripRepository.querySupportState()
        uiState = uiState.copy(support = status, registered = false)
        if (status.canRegister) registerIfPossible()
    }

    /** 模拟器选项（null = 跟随真实传感器）；仅模拟页写入，模拟页演示区消费。 */
    fun setSimulatedGrip(grip: GripState?) {
        uiState = uiState.copy(simulatedGrip = grip)
    }

    // ------------------------------------------------------------------
    // 设置项与页面无关的动作
    // ------------------------------------------------------------------

    /** 深色模式三态：写内存状态（即时换肤）+ 落盘（重启保留）。 */
    fun applyDarkMode(mode: DarkMode) {
        darkMode = mode
        prefs.edit().putString(KEY_DARK_MODE, mode.name).apply()
    }

    /** OLED 纯黑开关：写内存状态 + 落盘，默认关。 */
    fun applyOledBlack(enabled: Boolean) {
        oledBlack = enabled
        prefs.edit().putBoolean(KEY_OLED_BLACK, enabled).apply()
    }

    /**
     * 重启应用（状态 4「其他错误」的自救入口）：用启动器 Intent 以
     * `CLEAR_TASK + NEW_TASK` 重建任务栈——等效冷启动 UI，重新走一遍
     * 启动 → 支持状态查询 → 注册流程。用户主动点击，不属于「自启动」；
     * 不需要任何 manifest 权限。
     */
    fun restartApp(context: Context) {
        runCatching {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?: error("launch intent unavailable")
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            )
            context.startActivity(intent)
        }.onFailure { e -> Log.w(TAG, "cannot restart app", e) }
    }

    /**
     * 打开系统一级设置页。用户需自行进入开关/权限管理项
     * （官方引导文案已写明路径；深链到二级页依赖厂商私有 action，不通用）。
     * `ACTION_SETTINGS` 不需要任何权限，不违反「不新增权限」红线。
     */
    fun openSystemSettings(context: Context) {
        runCatching { context.startActivity(Intent(AndroidSettings.ACTION_SETTINGS)) }
            .onFailure { e -> Log.w(TAG, "cannot open system settings", e) }
    }

    /**
     * 打开 Android 原生「应用语言」页（`ACTION_APP_LOCALE_SETTINGS`，API 33+，本项目 minSdk 34）。
     * 该 action 不需要任何权限；个别 ROM 移除入口时回退到本应用的详情页
     * （`ACTION_APPLICATION_DETAILS_SETTINGS`），用户仍可在系统页中管理语言/权限。
     */
    fun openAppLocaleSettings(context: Context) {
        val packageUri = Uri.fromParts("package", packageName, null)
        runCatching {
            context.startActivity(Intent(AndroidSettings.ACTION_APP_LOCALE_SETTINGS, packageUri))
        }.onFailure { e ->
            Log.w(TAG, "locale settings unavailable, fallback to app details", e)
            runCatching {
                context.startActivity(
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
