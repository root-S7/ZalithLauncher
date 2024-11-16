package com.movtery.zalithlauncher.event.value

import com.movtery.zalithlauncher.feature.version.Addon
import com.movtery.zalithlauncher.feature.version.InstallTaskItem

/**
 * 安装任务开始时，将使用这个事件进行通知
 * @see com.movtery.zalithlauncher.ui.fragment.InstallGameFragment
 * @param minecraftVersion MC原版版本
 * @param customVersionName 自定义的版本文件夹名称
 * @param isIsolation 是否开启版本隔离
 * @param taskMap 安装任务
 */
class InstallGameEvent(
    val minecraftVersion: String,
    val customVersionName: String,
    val isIsolation: Boolean,
    val taskMap: Map<Addon, InstallTaskItem>
)