package org.anjisuan608.hihonor.ai_freegrip.grip

import androidx.annotation.StringRes
import org.anjisuan608.hihonor.ai_freegrip.R

/**
 * 设备支持状态（错误码 0~4），取值与处理建议见 AGENTS.md 2.3 / 官方错误码文档。
 *
 * 与 [GripState] 同理，本类不 import `com.hihonor.*`，保证单元测试可脱离真机运行。
 *
 * @param sdkValue `SmartGripEventManager.getSmartGripSupportState()` 的返回值
 * @param titleRes 状态卡片标题
 * @param guideRes 状态卡片引导文案（按官方「处理建议」原文措辞）
 * @param canRegister 是否可以注册监听器，仅状态 0 为 true
 * @param retryable 是否展示「尝试重新检测」按钮（AGENTS.md：状态 2、3 提供该按钮）
 */
enum class GripSupportStatus(
    val sdkValue: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val guideRes: Int,
    val canRegister: Boolean,
    val retryable: Boolean,
) {
    /** 设备支持。 */
    Supported(
        sdkValue = 0,
        titleRes = R.string.grip_status_supported,
        guideRes = R.string.grip_status_supported_guide,
        canRegister = true,
        retryable = false,
    ),

    /** 设备不支持 → 隐藏功能入口。 */
    NotSupported(
        sdkValue = 1,
        titleRes = R.string.grip_status_not_support,
        guideRes = R.string.grip_status_not_support_guide,
        canRegister = false,
        retryable = false,
    ),

    /** 用户关闭了开关 → 引导：设置 > 智能辅助 > AI 随心握。 */
    SettingOff(
        sdkValue = 2,
        titleRes = R.string.grip_status_setting_off,
        guideRes = R.string.grip_status_setting_off_guide,
        canRegister = false,
        retryable = true,
    ),

    /** 无权限 → 引导：设置 > 隐私和安全 > 权限管理 > 设备动作与方向。 */
    NoPermission(
        sdkValue = 3,
        titleRes = R.string.grip_status_no_permission,
        guideRes = R.string.grip_status_no_permission_guide,
        canRegister = false,
        retryable = true,
    ),

    /** 其他错误（Context 为空、系统服务不可用等）。 */
    OtherError(
        sdkValue = 4,
        titleRes = R.string.grip_status_other_error,
        guideRes = R.string.grip_status_other_error_guide,
        canRegister = false,
        retryable = false,
    );

    companion object {
        /**
         * 把支持状态原始 int 转成枚举。
         *
         * 官方文档只定义了 0~4；未收录的取值按「其他错误」处理——
         * 它的引导文案（检查 Context/系统服务）是最保守、最不误导的兜底。
         */
        fun fromSdk(raw: Int): GripSupportStatus =
            entries.firstOrNull { it.sdkValue == raw } ?: OtherError
    }
}
