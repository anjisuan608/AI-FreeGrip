package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import org.anjisuan608.hihonor.ai_freegrip.ui.theme.LocalOledActive

/**
 * OLED 纯黑下底栏的区分色：微灰 #141414 与纯黑背景拉开层次，
 * 底栏面积小、点亮像素有限，省电收益几乎不受影响。
 */
private val OledNavigationBarColor = Color(0xFF141414)

/**
 * 主体三页（主页/模拟/设置）共用的外层脚手架：顶栏标题 + 底部三选一导航。
 *
 * 多 Activity 拆分后每个界面宿主 Activity 各自持有一份 Scaffold；
 * 底部导航的跨 Activity 跳转由宿主注入 [onNavigate]（本组件不感知 Activity）。
 * 「关于」是设置的子页（无底部导航），由 AboutActivity 自行组装。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    page: AppPage,
    onNavigate: (AppPage) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(page.titleRes)) })
        },
        bottomBar = {
            NavigationBar(
                // OLED 纯黑时底栏换区分色，避免与纯黑内容背景融为一体；
                // 非 OLED 维持默认 surfaceContainer 观感
                containerColor = if (LocalOledActive.current) {
                    OledNavigationBarColor
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                AppPage.entries.forEach { item ->
                    NavigationBarItem(
                        selected = page == item,
                        onClick = { onNavigate(item) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelRes)) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}
