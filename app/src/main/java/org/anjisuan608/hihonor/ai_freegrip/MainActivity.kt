package org.anjisuan608.hihonor.ai_freegrip

import android.content.Intent
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import org.anjisuan608.hihonor.ai_freegrip.ui.SimulatorScreen
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme

/**
 * 单 Activity 三页面：
 * - 生命周期接线（官方 2.4）：`onCreate` 查询支持状态 → `onResume` 注册 → `onPause` 解注册；
 * - 页面切换：外层 [Scaffold] 统一持有 TopAppBar + 底部导航，内容区按 [AppPage] 切换
 *   （页面内不再各自套 Scaffold，避免嵌套 Scaffold 的 inset 双重填充）。
 *
 * UI 状态只有两处源头：[uiState] 与 [currentPage]；SDK 回调与模拟器只改状态，Compose 自动重组。
 */
class MainActivity : ComponentActivity() {

    private lateinit var gripRepository: SmartGripRepository

    private var uiState by mutableStateOf(GripUiState())
    private var currentPage by mutableStateOf(AppPage.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        gripRepository = SmartGripRepository(applicationContext)

        // 官方要求：onCreate 查询设备支持状态
        uiState = uiState.copy(support = gripRepository.querySupportState())

        setContent {
            AIFreegripTheme {
                // 返回键：模拟/关于页 → 回主页；主页保留系统默认行为
                BackHandler(enabled = currentPage != AppPage.Home) {
                    currentPage = AppPage.Home
                }
                AppScaffold(
                    page = currentPage,
                    state = uiState,
                    onPageChange = { currentPage = it },
                    onRecheck = ::recheckSupport,
                    onOpenSettings = ::openSystemSettings,
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
        onPageChange: (AppPage) -> Unit,
        onRecheck: () -> Unit,
        onOpenSettings: () -> Unit,
        onSimulate: (GripState?) -> Unit,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(title = { Text(stringResource(page.titleRes)) })
            },
            bottomBar = {
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
            },
        ) { innerPadding ->
            when (page) {
                AppPage.Home -> MainScreen(
                    state = state,
                    onRecheck = onRecheck,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(innerPadding),
                )

                AppPage.Simulator -> SimulatorScreen(
                    state = state,
                    onSimulate = onSimulate,
                    modifier = Modifier.padding(innerPadding),
                )

                AppPage.About -> AboutScreen(
                    modifier = Modifier.padding(innerPadding),
                )
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
