package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.anjisuan608.hihonor.ai_freegrip.R
import org.anjisuan608.hihonor.ai_freegrip.grip.GripState
import org.anjisuan608.hihonor.ai_freegrip.grip.GripSupportStatus
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.oledModuleBorder

/**
 * 主界面的唯一状态源（由 MainActivity 持有）。
 *
 * @param support 设备支持状态（错误码 0~4 映射结果）
 * @param grip SDK 回调驱动的真实握姿
 * @param registered 当前是否已注册监听器
 * @param simulatedGrip 模拟器接管时的握姿；null 表示跟随真实传感器。
 *   仅模拟页消费（[effectiveGrip]）；主页只用真实 [grip]，两页演示区互相解耦
 */
data class GripUiState(
    val support: GripSupportStatus = GripSupportStatus.NotSupported,
    val grip: GripState = GripState.Unknown,
    val registered: Boolean = false,
    val simulatedGrip: GripState? = null,
) {
    /** 模拟页演示区的握姿：模拟器选项优先，未选择时跟随真实传感器。 */
    val effectiveGrip: GripState
        get() = simulatedGrip ?: grip
}

/**
 * 主页：设备支持模块（[GripStatusCard]）+ 演示模块（[AdaptedLayout]）。
 * 演示区只跟随真实握姿（不消费模拟器选项），与模拟页的演示区互相解耦。
 * 顶栏与底部导航由 MainActivity 的外层 Scaffold 统一提供，本组件只负责内容。
 */
@Composable
fun MainScreen(
    state: GripUiState,
    onRecheck: () -> Unit,
    onOpenSettings: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 设备支持模块（当前握姿展示真实传感器值，不受模拟器影响）
        GripStatusCard(
            support = state.support,
            grip = state.grip,
            registered = state.registered,
            onRecheck = onRecheck,
            onOpenSettings = onOpenSettings,
            onRestart = onRestart,
            modifier = Modifier.fillMaxWidth(),
        )

        // 演示模块（商品详情 · 演示）：只跟随真实握姿，不受模拟器控制；
        // 设备不支持时按官方要求隐藏功能入口
        if (state.support == GripSupportStatus.NotSupported) {
            HiddenEntry(modifier = Modifier.fillMaxWidth())
        } else {
            AdaptedLayout(
                grip = state.grip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 设备不支持时的占位说明（功能入口本体已隐藏）。 */
@Composable
private fun HiddenEntry(modifier: Modifier = Modifier) {
    Card(modifier = modifier, border = oledModuleBorder()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.entry_hidden_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.entry_hidden_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 预览：主界面整体效果
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "主页 · 支持 + 右手握持")
@Composable
private fun MainScreenSupportedPreview() {
    AIFreegripTheme {
        MainScreen(
            state = GripUiState(
                support = GripSupportStatus.Supported,
                grip = GripState.RightHand,
                registered = true,
            ),
            onRecheck = {},
            onOpenSettings = {},
            onRestart = {},
        )
    }
}

@Preview(showBackground = true, name = "主页 · 开关关闭（带引导）")
@Composable
private fun MainScreenSettingOffPreview() {
    AIFreegripTheme {
        MainScreen(
            state = GripUiState(support = GripSupportStatus.SettingOff),
            onRecheck = {},
            onOpenSettings = {},
            onRestart = {},
        )
    }
}

@Preview(showBackground = true, name = "主页 · 设备不支持（隐藏入口）")
@Composable
private fun MainScreenNotSupportedPreview() {
    AIFreegripTheme {
        MainScreen(
            state = GripUiState(support = GripSupportStatus.NotSupported),
            onRecheck = {},
            onOpenSettings = {},
            onRestart = {},
        )
    }
}
