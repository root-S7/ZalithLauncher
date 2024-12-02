package com.movtery.zalithlauncher.setting

import androidx.annotation.CheckResult

class SettingUnit<V>(
    val key: String,
    val defaultValue: V,
    private val getValueFunc: (key: String, defaultValue: V) -> V
) {
    /**
     * @return 获取当前的设置值
     */
    fun getValue() = getValueFunc(key, defaultValue)

    /**
     * @return 存入值，并返回一个设置构建器
     */
    @CheckResult
    fun put(value: V): Settings.Manager.SettingBuilder = Settings.Manager.put(key, value!!)

    /**
     * 重置当前设置单元为默认值
     */
    fun reset() {
        Settings.Manager.put(key, defaultValue!!).save()
    }
}