package org.anjisuan608.hihonor.ai_freegrip.grip

/**
 * 握持状态（UI 层枚举）。
 *
 * 取值对应 SmartGripEventManager 的握持状态码（见 AGENTS.md 2.2）。
 * 刻意不 import `com.hihonor.*`：本类会在本地单元测试/Compose 预览中加载，
 * 而 SDK 类的静态初始化依赖荣耀私有 framework，非荣耀设备上一碰就崩。
 */
enum class GripState(val sdkValue: Int) {
    /** 未握持 → 居中默认布局。 */
    NotHeld(0),

    /** 左手握持 → 交互入口靠左。 */
    LeftHand(1),

    /** 右手握持 → 交互入口靠右。 */
    RightHand(2),

    /** 双手握持 → 对称布局。 */
    BothHands(3),

    /** 未识别 → 保持当前或恢复默认。注意官方取值是 16，不连续。 */
    Unknown(16);

    companion object {
        /**
         * 把 SDK 回调的原始 int 转成枚举。
         *
         * 未收录的取值（例如历史版本新增状态、或脏数据）一律降级为 [Unknown]，
         * 宁可「保持当前布局」也不能按错误的序号把交互入口摆到错误的一侧。
         */
        fun fromSdk(raw: Int): GripState =
            entries.firstOrNull { it.sdkValue == raw } ?: Unknown
    }
}
