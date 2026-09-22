package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.anjisuan608.hihonor.ai_freegrip.R
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.AIFreegripTheme
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.DarkMode
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.oledModuleBorder

/**
 * MD3 `unfold_more`（收起/展开双向箭头）。
 * material-icons-core 不含该图标，按 Material 官方路径用 materialIcon/materialPath
 * 自绘，避免为一个图标引入 material-icons-extended 全集。
 */
private val UnfoldMore = materialIcon(name = "Filled.UnfoldMore") {
    // 上：尖朝上（3→9），表示向上展开
    materialPath {
        moveTo(12f, 5.83f)
        lineTo(15.17f, 9f)
        lineTo(16.58f, 7.59f)
        lineTo(12f, 3f)
        lineTo(7.41f, 7.59f)
        lineTo(8.83f, 9f)
        close()
    }
    // 下：尖朝下（15→21），表示向下展开
    materialPath {
        moveTo(12f, 18.17f)
        lineTo(8.83f, 15f)
        lineTo(7.41f, 16.41f)
        lineTo(12f, 21f)
        lineTo(16.58f, 16.41f)
        lineTo(15.17f, 15f)
        close()
    }
}

/**
 * 设置页（底部第三 tab），三部分：
 * 1. 显示——主题模式（下拉菜单三选一，默认跟随系统）+ OLED 纯黑开关；
 * 2. 语言——跳 Android 原生「应用语言」页（`Settings.ACTION_APP_LOCALE_SETTINGS`）；
 * 3. 关于——入口项，点开后进入关于子页面（由 MainActivity 叠加展示）。
 *
 * 设置值由 MainActivity 持有并持久化，本组件只负责展示与回调。
 */
@Composable
fun SettingsScreen(
    darkMode: DarkMode,
    oledBlack: Boolean,
    onDarkModeChange: (DarkMode) -> Unit,
    onOledBlackChange: (Boolean) -> Unit,
    onOpenLanguageSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 旋转屏幕时保留下拉菜单展开状态（进程内状态，不落盘）
    var themeMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- 显示 ----
        SectionTitle(stringResource(R.string.settings_section_display))
        Card(modifier = Modifier.fillMaxWidth(), border = oledModuleBorder()) {
            Column {
                // 主题模式：点行展开下拉菜单（当前值 + 箭头，选中项带勾）
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themeMenuExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_dark_mode_title),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(darkMode.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = UnfoldMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false },
                    ) {
                        DarkMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(mode.labelRes),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                onClick = {
                                    onDarkModeChange(mode)
                                    themeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                HorizontalDivider()
                // OLED 纯黑：行内开关，默认关；仅深色模式下生效（描述已写明）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_oled_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.settings_oled_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = oledBlack, onCheckedChange = onOledBlackChange)
                }
            }
        }

        // ---- 语言 ----
        SectionTitle(stringResource(R.string.settings_section_language))
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLanguageSettings),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_language_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.settings_language_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---- 关于：设置里的一个入口项（用户要求的形态） ----
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAbout),
        ) {
            Text(
                text = stringResource(R.string.nav_about),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

/** 小节标题（与关于页样式一致）。 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 6.dp),
    )
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "设置页 · 跟随系统")
@Composable
private fun SettingsScreenSystemPreview() {
    AIFreegripTheme {
        SettingsScreen(
            darkMode = DarkMode.System,
            oledBlack = false,
            onDarkModeChange = {},
            onOledBlackChange = {},
            onOpenLanguageSettings = {},
            onOpenAbout = {},
        )
    }
}

@Preview(showBackground = true, name = "设置页 · 深色 + OLED 开")
@Composable
private fun SettingsScreenDarkPreview() {
    AIFreegripTheme(darkTheme = true, oledBlack = true) {
        SettingsScreen(
            darkMode = DarkMode.Dark,
            oledBlack = true,
            onDarkModeChange = {},
            onOledBlackChange = {},
            onOpenLanguageSettings = {},
            onOpenAbout = {},
        )
    }
}
