package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.anjisuan608.hihonor.ai_freegrip.R
import org.anjisuan608.hihonor.ai_freegrip.grip.GripState
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme

/**
 * 核心演示区：一个「模拟的真实业务界面」，操作区随握姿实时重排——
 *
 * - 未握持/未识别：全宽对称，操作区居中
 * - 左手握持：交互入口靠左（左手拇指可达区）
 * - 右手握持：交互入口靠右
 * - 双手握持：主/次操作分列两端对称
 *
 * 切换用 250ms 淡入淡出，让「布局跟随握姿」在演示时看得见。
 */
@Composable
fun AdaptedLayout(
    grip: GripState,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.demo_screen_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))

            repeat(3) { index ->
                DemoContentCard(index = index + 1)
                Spacer(Modifier.height(8.dp))
            }

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
private fun DemoContentCard(index: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.demo_card_title, index),
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
 */
@Composable
private fun ActionRow(grip: GripState) {
    val arrangement: Arrangement.Horizontal = when (grip) {
        GripState.LeftHand -> Arrangement.spacedBy(12.dp, Alignment.Start)
        GripState.RightHand -> Arrangement.spacedBy(12.dp, Alignment.End)
        GripState.BothHands -> Arrangement.SpaceBetween
        // 未握持与未识别都恢复居中默认布局
        GripState.NotHeld, GripState.Unknown -> Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = {}) {
            Text(stringResource(R.string.cta_primary))
        }
        OutlinedButton(onClick = {}) {
            Text(stringResource(R.string.cta_secondary))
        }
    }
}

/** 当前握姿对应的布局说明文案。 */
@Composable
private fun layoutHint(grip: GripState): String = when (grip) {
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
