package org.anjisuan608.hihonor.ai_freegrip.ui.theme

import androidx.annotation.StringRes
import org.anjisuan608.hihonor.ai_freegrip.R

/**
 * 主题模式三态（设置页以下拉菜单切换）：
 * - [System]：跟随系统（默认）；
 * - [Light]：始终浅色；
 * - [Dark]：始终深色（与系统当前明暗无关）。
 *
 * 持久化用 [name] 存 SharedPreferences；[fromName] 对脏数据回退 [System]，
 * 保证首次启动与旧版本升级都不会抛异常。
 */
enum class DarkMode(@param:StringRes val labelRes: Int) {
    System(R.string.settings_dark_mode_system),
    Light(R.string.settings_dark_mode_light),
    Dark(R.string.settings_dark_mode_dark);

    /**
     * 结合系统当前明暗，解析出本 App 最终是否使用深色。
     *
     * @param systemDark 系统当前是否深色（`isSystemInDarkTheme()` 的值）
     */
    fun resolvesToDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    companion object {
        /** 从 SharedPreferences 恢复；未知/空值回退 [System]（默认跟随系统）。 */
        fun fromName(name: String?): DarkMode =
            entries.firstOrNull { it.name == name } ?: System
    }
}
