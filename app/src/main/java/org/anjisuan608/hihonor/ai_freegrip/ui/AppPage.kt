package org.anjisuan608.hihonor.ai_freegrip.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector
import org.anjisuan608.hihonor.ai_freegrip.R

/**
 * 三个页面：主页（支持状态 + 演示）、模拟（模拟器 + 预期行为）、
 * 关于（合规披露 + SDK 链接 + 开源信息）。
 *
 * 导航只用 [AppPage] 枚举 + Compose state 切换，不引入导航库
 * （AGENTS.md 第 4 节：单 Activity 演示程序，避免过度设计）。
 */
enum class AppPage(
    @param:StringRes val labelRes: Int,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    /** 主页：设备支持模块 + 演示模块。 */
    Home(R.string.nav_home, R.string.main_title, Icons.Filled.Home),

    /** 模拟：演示模拟器 + 预期行为演示区。 */
    Simulator(R.string.nav_simulator, R.string.sim_page_title, Icons.Filled.Build),

    /** 关于：合规披露 + HONOR 开发者链接 + 作者/许可证/仓库。 */
    About(R.string.nav_about, R.string.about_title, Icons.Filled.Info),
}
