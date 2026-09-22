package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.anjisuan608.hihonor.ai_freegrip.R
import org.anjisuan608.hihonor.ai_freegrip.grip.GripState
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.oledModuleBorder

/**
 * 核心演示区：一个「模拟的真实业务界面」，操作区随握姿实时重排——
 *
 * - 未握持/未识别：全宽对称，操作区居中
 * - 左手握持：交互入口靠左（左手拇指可达区）
 * - 右手握持：交互入口靠右
 * - 双手握持：主/次操作分列两端对称
 *
 * 切换用 250ms 淡入淡出，让「布局跟随握姿」在演示时看得见。
 *
 * 标题由调用方决定：主页传「商品详情 · 演示」（默认），模拟页传
 * 「商品详情 · 模拟」，两页的演示区由此解耦。
 */
@Composable
fun AdaptedLayout(
    grip: GripState,
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int = R.string.demo_screen_title,
) {
    Card(modifier = modifier, border = oledModuleBorder()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))

            DemoContentCard()
            Spacer(Modifier.height(8.dp))

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.demo_action_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = layoutHint(grip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            AnimatedContent(
                targetState = grip,
                transitionSpec = {
                    fadeIn(tween(250)) togetherWith fadeOut(tween(250))
                },
                label = "grip-layout",
            ) { target ->
                ActionRow(grip = target)
            }
        }
    }
}

/** 模拟业务内容卡片。 */
@Composable
private fun DemoContentCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.demo_card_title),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.demo_card_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 操作区排布策略——本演示的核心逻辑，直接对应 AGENTS.md 2.2 的 UI 策略列。
 *
 * 两个按钮并排会占满（或超出）整行时改为竖排（第二个按钮在下一行）：
 * 按钮横跨整行会让靠左/靠右/居中的对齐位移在视觉上看不出来，
 * en 等长文案下必现；放得下时保持横排原有对齐。
 */
@Composable
private fun ActionRow(grip: GripState) {
    val gap = 12.dp
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Button(onClick = {}) {
                Text(stringResource(R.string.cta_primary))
            }
            OutlinedButton(onClick = {}) {
                Text(stringResource(R.string.cta_secondary))
            }
        },
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val maxWidth = constraints.maxWidth
        // 先量出两个按钮各自的内容宽（单个按钮最多与整行同宽）
        val primary = measurables[0].measure(Constraints(maxWidth = maxWidth))
        val secondary = measurables[1].measure(Constraints(maxWidth = maxWidth))
        val total = primary.width + gapPx + secondary.width
        // 占满/超出整行 → 竖排；宽度无界（理论上不会发生在 fillMaxWidth 下）→ 横排
        val stacked = maxWidth != Constraints.Infinity && total >= maxWidth

        if (stacked) {
            // 竖排：水平对齐跟随握姿；双手/未握持/未识别居中，保持对称的默认观感
            val x1: Int
            val x2: Int
            when (grip) {
                GripState.LeftHand -> { x1 = 0; x2 = 0 }
                GripState.RightHand -> {
                    x1 = maxWidth - primary.width
                    x2 = maxWidth - secondary.width
                }
                GripState.NotHeld, GripState.Unknown, GripState.BothHands -> {
                    x1 = (maxWidth - primary.width) / 2
                    x2 = (maxWidth - secondary.width) / 2
                }
            }
            layout(maxWidth, primary.height + gapPx + secondary.height) {
                primary.place(x1, 0)
                secondary.place(x2, primary.height + gapPx)
            }
        } else {
            // 横排：与原 Row 相同的对齐策略（靠左/靠右/两端对称/居中）
            val x1: Int
            val x2: Int
            when (grip) {
                GripState.LeftHand -> { x1 = 0; x2 = primary.width + gapPx }
                GripState.RightHand -> { x1 = maxWidth - total; x2 = x1 + primary.width + gapPx }
                GripState.BothHands -> { x1 = 0; x2 = maxWidth - secondary.width }
                GripState.NotHeld, GripState.Unknown -> {
                    val start = (maxWidth - total) / 2
                    x1 = start
                    x2 = start + primary.width + gapPx
                }
            }
            val height = maxOf(primary.height, secondary.height)
            layout(maxWidth, height) {
                primary.place(x1, (height - primary.height) / 2)
                secondary.place(x2, (height - secondary.height) / 2)
            }
        }
    }
}

/** 当前握姿对应的布局说明文案（主页与模拟页共用，同包 internal）。 */
@Composable
internal fun layoutHint(grip: GripState): String = when (grip) {
    GripState.NotHeld -> stringResource(R.string.layout_hint_not_held)
    GripState.LeftHand -> stringResource(R.string.layout_hint_left)
    GripState.RightHand -> stringResource(R.string.layout_hint_right)
    GripState.BothHands -> stringResource(R.string.layout_hint_both)
    GripState.Unknown -> stringResource(R.string.layout_hint_unknown)
}

// ---------------------------------------------------------------------------
// 预览：5 种握姿下的布局（与单测一起覆盖 AGENTS.md P2 验收项）
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "布局 · 未握持")
@Composable
private fun LayoutNotHeldPreview() = LayoutPreview(GripState.NotHeld)

@Preview(showBackground = true, name = "布局 · 左手握持")
@Composable
private fun LayoutLeftHandPreview() = LayoutPreview(GripState.LeftHand)

@Preview(showBackground = true, name = "布局 · 右手握持")
@Composable
private fun LayoutRightHandPreview() = LayoutPreview(GripState.RightHand)

@Preview(showBackground = true, name = "布局 · 双手握持")
@Composable
private fun LayoutBothHandsPreview() = LayoutPreview(GripState.BothHands)

@Preview(showBackground = true, name = "布局 · 未识别")
@Composable
private fun LayoutUnknownPreview() = LayoutPreview(GripState.Unknown)

@Composable
private fun LayoutPreview(grip: GripState) {
    AIFreegripTheme {
        AdaptedLayout(grip = grip)
    }
}
