package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.anjisuan608.hihonor.ai_freegrip.R
import org.anjisuan608.hihonor.ai_freegrip.grip.GripState
import org.anjisuan608.hihonor.ai_freegrip.grip.GripSupportStatus
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme

/**
 * 展示支持状态 / 持有状态 / 注册状态，并按错误码给出官方引导（AGENTS.md 2.3、2.4）。
 *
 * 容器配色语义：
 * - 支持 → secondaryContainer（正常）
 * - 状态 2/3 → tertiaryContainer（可行动的警告，带「重新检测/打开设置」按钮）
 * - 状态 4 → errorContainer（带「重新检测/重启应用」按钮）
 * - 状态 1 → errorContainer（不可行动的错误，隐藏入口）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GripStatusCard(
    support: GripSupportStatus,
    grip: GripState,
    registered: Boolean,
    onRecheck: () -> Unit,
    onOpenSettings: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColorFor(support)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(support.titleRes),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(support.guideRes),
                style = MaterialTheme.typography.bodyMedium,
            )

            // FlowRow：en 等长文案下两个胶囊并排放不下时，第二个胶囊换到下一行
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusPill(
                    text = "${stringResource(R.string.label_current_grip)}：" + gripLabel(grip),
                )
                StatusPill(
                    text = stringResource(
                        if (registered) R.string.label_registered_on else R.string.label_registered_off,
                    ),
                )
            }

            if (support.retryable) {
                // AGENTS.md：状态 2、3 提供「尝试重新检测」按钮
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRecheck) {
                        Text(stringResource(R.string.action_recheck))
                    }
                    OutlinedButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                }
            }

            if (support == GripSupportStatus.OtherError) {
                // 状态 4：提供自救手段——先重试查询/注册，仍失败则重启应用
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRecheck) {
                        Text(stringResource(R.string.action_recheck))
                    }
                    OutlinedButton(onClick = onRestart) {
                        Text(stringResource(R.string.action_restart))
                    }
                }
            }
        }
    }
}

/** 语义化容器色，见类文档注释。 */
@Composable
private fun containerColorFor(support: GripSupportStatus) = when {
    support.canRegister -> MaterialTheme.colorScheme.secondaryContainer
    support.retryable -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.errorContainer
}

/** 状态小胶囊（握姿、注册状态）。 */
@Composable
private fun StatusPill(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** 握姿 → 中文标签。状态卡片与演示区共用（同包 internal）。 */
@Composable
internal fun gripLabel(grip: GripState): String = when (grip) {
    GripState.NotHeld -> stringResource(R.string.grip_state_not_held)
    GripState.LeftHand -> stringResource(R.string.grip_state_left_hand)
    GripState.RightHand -> stringResource(R.string.grip_state_right_hand)
    GripState.BothHands -> stringResource(R.string.grip_state_both_hands)
    GripState.Unknown -> stringResource(R.string.grip_state_unknown)
}

// ---------------------------------------------------------------------------
// 预览：5 种支持状态（握姿固定为右手握持）
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "状态 0 · 支持")
@Composable
private fun StatusSupportedPreview() = StatusPreview(GripSupportStatus.Supported)

@Preview(showBackground = true, name = "状态 1 · 不支持")
@Composable
private fun StatusNotSupportedPreview() = StatusPreview(GripSupportStatus.NotSupported)

@Preview(showBackground = true, name = "状态 2 · 开关关闭")
@Composable
private fun StatusSettingOffPreview() = StatusPreview(GripSupportStatus.SettingOff)

@Preview(showBackground = true, name = "状态 3 · 无权限")
@Composable
private fun StatusNoPermissionPreview() = StatusPreview(GripSupportStatus.NoPermission)

@Preview(showBackground = true, name = "状态 4 · 其他错误")
@Composable
private fun StatusOtherErrorPreview() = StatusPreview(GripSupportStatus.OtherError)

@Composable
private fun StatusPreview(support: GripSupportStatus) {
    AIFreegripTheme {
        GripStatusCard(
            support = support,
            grip = GripState.RightHand,
            registered = support.canRegister,
            onRecheck = {},
            onOpenSettings = {},
            onRestart = {},
        )
    }
}
