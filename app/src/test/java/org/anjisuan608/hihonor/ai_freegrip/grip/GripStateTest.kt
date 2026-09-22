package org.anjisuan608.hihonor.ai_freegrip.grip

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 状态映射单测：完全脱离真机/SDK 运行（这正是 GripState 不 import com.hihonor.* 的回报）。
 * 期望值抄自官方错误码与常量表，见 AGENTS.md 2.2。
 */
class GripStateTest {

    @Test
    fun `maps all official grip state codes`() {
        assertEquals(GripState.NotHeld, GripState.fromSdk(0))
        assertEquals(GripState.LeftHand, GripState.fromSdk(1))
        assertEquals(GripState.RightHand, GripState.fromSdk(2))
        assertEquals(GripState.BothHands, GripState.fromSdk(3))
        assertEquals(GripState.Unknown, GripState.fromSdk(16))
    }

    @Test
    fun `unknown code is 16 not the next ordinal`() {
        // 回归点：握持状态码不连续，4 和 5 都是**非法**值，绝不能被当成「未识别之后的下一态」
        assertEquals(GripState.Unknown, GripState.fromSdk(4))
        assertEquals(GripState.Unknown, GripState.fromSdk(5))
    }

    @Test
    fun `falls back to Unknown for any unmapped value`() {
        listOf(-1, 4, 5, 17, 99, Int.MIN_VALUE, Int.MAX_VALUE).forEach { raw ->
            assertEquals("raw=$raw 应降级为 Unknown", GripState.Unknown, GripState.fromSdk(raw))
        }
    }

    @Test
    fun `fromSdk is the inverse of sdkValue for every state`() {
        GripState.entries.forEach { state ->
            assertEquals(state, GripState.fromSdk(state.sdkValue))
        }
    }

    @Test
    fun `sdk values match the official constants`() {
        // 一旦 SDK 升级改了常量值，这里先红，避免 UI 把交互入口摆错侧
        assertEquals(0, GripState.NotHeld.sdkValue)
        assertEquals(1, GripState.LeftHand.sdkValue)
        assertEquals(2, GripState.RightHand.sdkValue)
        assertEquals(3, GripState.BothHands.sdkValue)
        assertEquals(16, GripState.Unknown.sdkValue)
    }
}
