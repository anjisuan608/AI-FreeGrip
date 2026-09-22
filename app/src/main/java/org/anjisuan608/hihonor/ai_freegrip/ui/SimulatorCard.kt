package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.anjisuan608.hihonor.ai_freegrip.R
import org.anjisuan608.hihonor.ai_freegrip.grip.GripState
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.oledModuleBorder

/**
 * 演示模拟器：非荣耀设备/评审现场没有握持输入时，手动预览 5 种握姿的布局。
 * 选中的值由 MainActivity 写入 [GripUiState.simulatedGrip]，优先于真实回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorCard(
    current: GripState?,
    onSimulate: (GripState?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, border = oledModuleBorder()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.simulator_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                // 清除模拟 → 恢复跟随真实传感器
                TextButton(onClick = { onSimulate(null) }) {
                    Text(stringResource(R.string.simulator_follow_real))
                }
            }

            Text(
                text = if (current == null) {
                    stringResource(R.string.simulator_hint)
                } else {
                    stringResource(R.string.simulator_simulating, gripSimLabel(current))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 外层滚动兜底，防止窄屏 5 个分段按钮被裁切
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                GripState.entries.forEachIndexed { index, grip ->
                    SegmentedButton(
                        selected = current == grip,
                        onClick = { onSimulate(grip) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = GripState.entries.size,
                        ),
                        label = { Text(gripSimLabel(grip)) },
                    )
                }
            }
        }
    }
}

/** 模拟器内的 2 字短标签。 */
@Composable
private fun gripSimLabel(grip: GripState): String = when (grip) {
    GripState.NotHeld -> stringResource(R.string.grip_sim_not_held)
    GripState.LeftHand -> stringResource(R.string.grip_sim_left)
    GripState.RightHand -> stringResource(R.string.grip_sim_right)
    GripState.BothHands -> stringResource(R.string.grip_sim_both)
    GripState.Unknown -> stringResource(R.string.grip_sim_unknown)
}
