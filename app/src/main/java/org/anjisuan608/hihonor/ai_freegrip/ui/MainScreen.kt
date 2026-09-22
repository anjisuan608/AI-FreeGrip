package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

/**
 * 主界面的唯一状态源（由 MainActivity 持有）。
 *
 * @param support 设备支持状态（错误码 0~4 映射结果）
 * @param grip SDK 回调驱动的真实握姿
 * @param registered 当前是否已注册监听器
 * @param simulatedGrip 模拟器接管时的握姿；null 表示跟随真实传感器
 */
data class GripUiState(
    val support: GripSupportStatus = GripSupportStatus.NotSupported,
    val grip: GripState = GripState.Unknown,
    val registered: Boolean = false,
    val simulatedGrip: GripState? = null,
) {
    /** 实际用于驱动布局的握姿：模拟器优先。 */
    val effectiveGrip: GripState
        get() = simulatedGrip ?: grip
}

/**
 * 演示主页：状态卡片 + 随握姿重排的演示区 + 模拟器 + 合规入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: GripUiState,
    onRecheck: () -> Unit,
    onOpenSettings: () -> Unit,
    onSimulate: (GripState?) -> Unit,
    onOpenCompliance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_title)) },
                actions = {
                    TextButton(onClick = onOpenCompliance) {
                        Text(stringResource(R.string.action_compliance))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GripStatusCard(
                support = state.support,
                grip = state.effectiveGrip,
                registered = state.registered,
                onRecheck = onRecheck,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.support == GripSupportStatus.NotSupported) {
                // 官方处理建议：设备不支持时隐藏功能入口
                HiddenEntry(modifier = Modifier.fillMaxWidth())
            } else {
                AdaptedLayout(
                    grip = state.effectiveGrip,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SimulatorCard(
                current = state.simulatedGrip,
                onSimulate = onSimulate,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.footer_text),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 设备不支持时的占位说明（功能入口本体已隐藏）。 */
@Composable
private fun HiddenEntry(modifier: Modifier = Modifier) {
    androidx.compose.material3.Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.entry_hidden_title), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.entry_hidden_desc),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 预览：主界面整体效果
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "主界面 · 支持 + 右手握持")
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
            onSimulate = {},
            onOpenCompliance = {},
        )
    }
}

@Preview(showBackground = true, name = "主界面 · 开关关闭（带引导）")
@Composable
private fun MainScreenSettingOffPreview() {
    AIFreegripTheme {
        MainScreen(
            state = GripUiState(support = GripSupportStatus.SettingOff),
            onRecheck = {},
            onOpenSettings = {},
            onSimulate = {},
            onOpenCompliance = {},
        )
    }
}

@Preview(showBackground = true, name = "主界面 · 设备不支持（隐藏入口）")
@Composable
private fun MainScreenNotSupportedPreview() {
    AIFreegripTheme {
        MainScreen(
            state = GripUiState(support = GripSupportStatus.NotSupported),
            onRecheck = {},
            onOpenSettings = {},
            onSimulate = {},
            onOpenCompliance = {},
        )
    }
}
