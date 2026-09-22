package org.anjisuan608.hihonor.ai_freegrip.grip

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.hihonor.smartgripkit.SmartGripEventListener
import com.hihonor.smartgripkit.SmartGripEventManager

/**
 * `SmartGripEventManager` 的唯一封装——本类是整个 App 触碰 SDK 的**唯一出口**。
 *
 * 之所以必须集中到一处（AGENTS.md 4. 架构规划）：
 * 1. SDK 的 POM 零依赖，它用到的荣耀私有 framework 类只存在于 MagicOS 设备上，
 *    非荣耀设备首次触碰 `SmartGripEventManager` 就会在静态初始化里抛
 *    `ExceptionInInitializerError`/`NoClassDefFoundError`——必须逐调用点 try/catch 兜底；
 * 2. UI 层只依赖 [GripState]/[GripSupportStatus]，单元测试和 Compose 预览因此完全不碰 SDK。
 *
 * 生命周期调用时机（官方 2.4）：`onCreate` 查询 → `onResume` 注册 → `onPause`/`onDestroy` 解注册。
 */
class SmartGripRepository(context: Context) {

    /** 用 ApplicationContext，避免持有 Activity 造成泄漏（官方建议，SDK 内部同样如此）。 */
    private val appContext: Context = context.applicationContext ?: context

    /** SDK 回调跑在系统设备事件线程，所有对外回调统一投递到主线程后再更新 Compose 状态。 */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 注册时创建、解注册时清空的 listener 实例。
     * 注册/解注册必须传**同一个实例**，所以要自己持有。
     */
    private var listener: SmartGripEventListener? = null

    /** 最近一次 register 传入的回调；解注册后置空，使已投递但未执行的主线程消息失效。 */
    private var callback: ((GripState) -> Unit)? = null

    /** 当前是否已注册（供 `onResume` 做幂等判断，防止重复注册）。 */
    val isRegistered: Boolean
        get() = listener != null

    /**
     * 查询设备支持状态（官方要求在注册前先调用）。
     *
     * 任何 [Throwable]（包括类初始化失败的 Error）都降级为 [GripSupportStatus.NotSupported]：
     * 在非荣耀设备上这正是官方定义的语义，且绝不让异常冒泡到 UI。
     */
    fun querySupportState(): GripSupportStatus = try {
        GripSupportStatus.fromSdk(
            SmartGripEventManager.getSmartGripSupportState(appContext)
        )
    } catch (t: Throwable) {
        Log.w(TAG, "getSmartGripSupportState failed, degrade to NotSupported", t)
        GripSupportStatus.NotSupported
    }

    /**
     * 注册握持监听器。重复调用是幂等的：已注册时只更新 [callback] 并返回 true。
     *
     * @param onGripChanged **主线程**回调，参数已转换为 [GripState]
     * @return 是否注册成功；`false` 表示入参/系统服务异常，调用方按状态 4 引导
     */
    fun register(onGripChanged: (GripState) -> Unit): Boolean {
        callback = onGripChanged
        listener?.let { return true } // onResume 可能重复触发，先做幂等

        val newListener = object : SmartGripEventListener() {
            override fun onSmartGripEventChanged(state: Int) {
                // 系统设备事件线程 → 主线程；unregister 后 callback 为空，此处自动 no-op
                mainHandler.post { callback?.invoke(GripState.fromSdk(state)) }
            }
        }
        return try {
            val ok = SmartGripEventManager.registerSmartGripMotionListener(appContext, newListener)
            if (ok) {
                listener = newListener
            } else {
                Log.w(TAG, "registerSmartGripMotionListener returned false (state/service unavailable)")
            }
            ok
        } catch (t: Throwable) {
            // 非荣耀设备走这里：静态初始化即炸，降级为注册失败
            Log.w(TAG, "registerSmartGripMotionListener failed", t)
            false
        }
    }

    /**
     * 解注册并释放系统资源（官方要求在 `onPause`/`onDestroy` 调用）。
     *
     * 幂等：未注册时直接返回 true。无论 SDK 是否抛错，都先清掉本地引用，
     * 保证不会出现「本地以为还注册着」的泄漏状态。
     */
    fun unregister(): Boolean {
        val current = listener ?: return true
        listener = null
        callback = null
        return try {
            val ok = SmartGripEventManager.unregisterSmartGripMotionListener(appContext, current)
            if (!ok) Log.w(TAG, "unregisterSmartGripMotionListener returned false")
            ok
        } catch (t: Throwable) {
            Log.w(TAG, "unregisterSmartGripMotionListener failed", t)
            false
        }
    }

    private companion object {
        const val TAG = "SmartGripRepository"
    }
}
