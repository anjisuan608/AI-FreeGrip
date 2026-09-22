package org.anjisuan608.hihonor.ai_freegrip.grip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 错误码映射单测。期望值抄自官方错误码表与处理建议，见 AGENTS.md 2.3。
 */
class GripSupportStatusTest {

    @Test
    fun `maps all official support state codes`() {
        assertEquals(GripSupportStatus.Supported, GripSupportStatus.fromSdk(0))
        assertEquals(GripSupportStatus.NotSupported, GripSupportStatus.fromSdk(1))
        assertEquals(GripSupportStatus.SettingOff, GripSupportStatus.fromSdk(2))
        assertEquals(GripSupportStatus.NoPermission, GripSupportStatus.fromSdk(3))
        assertEquals(GripSupportStatus.OtherError, GripSupportStatus.fromSdk(4))
    }

    @Test
    fun `falls back to OtherError for unmapped values`() {
        // 官方只定义 0~4；越界值按最保守的「其他错误」兜底
        listOf(-1, 5, 6, 99, Int.MIN_VALUE, Int.MAX_VALUE).forEach { raw ->
            assertEquals("raw=$raw 应降级为 OtherError", GripSupportStatus.OtherError, GripSupportStatus.fromSdk(raw))
        }
    }

    @Test
    fun `only supported state may register listener`() {
        // 只有状态 0 能注册，其余必须先按引导处理（官方接入指导）
        GripSupportStatus.entries.forEach { status ->
            assertEquals(
                "status=${status.name} 的 canRegister 不符预期",
                status == GripSupportStatus.Supported,
                status.canRegister,
            )
        }
    }

    @Test
    fun `only setting off and no permission offer retry button`() {
        // AGENTS.md 2.4：状态 2、3 提供「尝试重新检测」按钮
        val retryable = GripSupportStatus.entries.filter { it.retryable }.toSet()
        assertEquals(
            setOf(GripSupportStatus.SettingOff, GripSupportStatus.NoPermission),
            retryable,
        )
    }

    @Test
    fun `every status has title and guide text`() {
        // 5 种状态都必须有可展示的文案（错误可视化要求）
        GripSupportStatus.entries.forEach { status ->
            assertTrue("status=${status.name} 缺标题", status.titleRes != 0)
            assertTrue("status=${status.name} 缺引导文案", status.guideRes != 0)
        }
        assertEquals(5, GripSupportStatus.entries.size)
        assertFalse(GripSupportStatus.Supported.retryable)
    }
}
