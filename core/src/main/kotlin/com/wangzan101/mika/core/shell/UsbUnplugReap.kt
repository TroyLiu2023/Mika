package com.wangzan101.mika.core.shell

/**
 * 「这台手机拔线会回收 USB 启动的助手」的判定:电源断开和未配对助手的死亡先后发生在关联窗口内,就认定成立。
 * 拔线必然伴随断电;会回收助手的系统都是在拔线后很快动手,窗口给足裕量。认定一次就由平台持久化,
 * 之后再拔线也用不着重判(它描述的是手机系统的行为,不随配对状态变化)。
 */
class UsbUnplugReap(private val windowMs: Long = WINDOW_MS, private val now: () -> Long = System::currentTimeMillis) {
    /** 最近一次电源断开的时刻;没断过(或已被一次判定用掉)为 null。 */
    private var powerLostAt: Long? = null

    /** 电源断了(USB 拔线必然伴随)。 */
    fun onPowerDisconnected() {
        powerLostAt = now()
    }

    /**
     * 未配对的助手没了(之前见过它在):窗口内先断过电,就认定这台手机拔线回收助手。
     * 返回 true 表示这次成立,调用方应持久化;已认定过([alreadyKnown])或对不上时间返回 false。
     * 任何一次助手死亡都把待定的断电事件用掉:断电之后来的死亡才算新的判定。
     */
    fun onHelperLost(alreadyKnown: Boolean): Boolean {
        val lostAt = powerLostAt ?: return false
        powerLostAt = null
        if (alreadyKnown) return false
        return now() - lostAt <= windowMs
    }

    companion object {
        const val WINDOW_MS = 60_000L
    }
}
