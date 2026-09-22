package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/**
 * 模拟页：
 * 1. 演示模拟器模块（[SimulatorCard]）——选择/清除握姿模拟；
 * 2. 演示区——**仅跟随模拟器选项**（未选择时跟随真实传感器）展示当前
 *    握姿、预期布局说明与自适应布局效果（复用 [AdaptedLayout]，标题为
 *    「商品详情 · 模拟」；主页的演示区用真实握姿，两页互相解耦）。
 *
 * 顶栏与底部导航由 MainActivity 的外层 Scaffold 统一提供。
 */
@Composable
fun SimulatorScreen(
    state: GripUiState,
    onSimulate: (GripState?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val effective = state.effectiveGrip
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 模块一：演示模拟器
        SimulatorCard(
            current = state.simulatedGrip,
            onSimulate = onSimulate,
            modifier = Modifier.fillMaxWidth(),
        )

        // 模块二：演示区——仅跟随模拟器选项（未选择时跟随真实传感器）
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "${stringResource(R.string.label_current_grip)}：" + gripLabel(effective),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 每种握姿的预期行为（对应 AGENTS.md 2.2 的 UI 策略列）
            Text(
                text = layoutHint(effective),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            AdaptedLayout(
                grip = effective,
                titleRes = R.string.demo_screen_sim_title,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "模拟页 · 模拟左手握持")
@Composable
private fun SimulatorScreenLeftPreview() {
    AIFreegripTheme {
        SimulatorScreen(
            state = GripUiState(
                support = GripSupportStatus.Supported,
                grip = GripState.RightHand,
                simulatedGrip = GripState.LeftHand,
            ),
            onSimulate = {},
        )
    }
}

@Preview(showBackground = true, name = "模拟页 · 跟随真实（未选择）")
@Composable
private fun SimulatorScreenRealPreview() {
    AIFreegripTheme {
        SimulatorScreen(
            state = GripUiState(
                support = GripSupportStatus.Supported,
                grip = GripState.BothHands,
            ),
            onSimulate = {},
        )
    }
}
