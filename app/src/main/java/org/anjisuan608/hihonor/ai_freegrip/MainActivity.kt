package org.anjisuan608.hihonor.ai_freegrip

import android.content.Intent
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.anjisuan608.hihonor.ai_freegrip.grip.GripSupportStatus
import org.anjisuan608.hihonor.ai_freegrip.grip.SmartGripRepository
import org.anjisuan608.hihonor.ai_freegrip.ui.ComplianceScreen
import org.anjisuan608.hihonor.ai_freegrip.ui.GripUiState
import org.anjisuan608.hihonor.ai_freegrip.ui.MainScreen
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme

/**
 * 生命周期接线（官方 2.4）：
 * `onCreate` 查询支持状态 → `onResume` 注册 → `onPause` 解注册。
 *
 * UI 状态只有一处源头 [uiState]；SDK 回调与模拟器都只改它，Compose 自动重组。
 */
class MainActivity : ComponentActivity() {

    private lateinit var gripRepository: SmartGripRepository

    private var uiState by mutableStateOf(GripUiState())
    private var showCompliance by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        gripRepository = SmartGripRepository(applicationContext)

        // 官方要求：onCreate 查询设备支持状态
        uiState = uiState.copy(support = gripRepository.querySupportState())

        setContent {
            AIFreegripTheme {
                // 单 Activity 两屏，不用导航库（AGENTS.md 第 4 节）
                BackHandler(enabled = showCompliance) { showCompliance = false }

                if (showCompliance) {
                    ComplianceScreen(onBack = { showCompliance = false })
                } else {
                    MainScreen(
                        state = uiState,
                        onRecheck = ::recheckSupport,
                        onOpenSettings = ::openSystemSettings,
                        onSimulate = { simulated -> uiState = uiState.copy(simulatedGrip = simulated) },
                        onOpenCompliance = { showCompliance = true },
                    )
                }
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

    /**
     * 打开系统一级设置页。用户需自行进入开关/权限管理项
     * （官方引导文案已写明路径；深链到二级页依赖厂商私有 action，不通用）。
     * `ACTION_SETTINGS` 不需要任何权限，不违反「不新增权限」红线。
     */
    private fun openSystemSettings() {
        runCatching { startActivity(Intent(AndroidSettings.ACTION_SETTINGS)) }
            .onFailure { e -> Log.w(TAG, "cannot open system settings", e) }
    }

    private companion object {
        const val TAG = "AIFreegrip"
    }
}
