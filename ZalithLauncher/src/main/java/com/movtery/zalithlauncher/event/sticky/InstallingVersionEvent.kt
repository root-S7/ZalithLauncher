package com.movtery.zalithlauncher.event.sticky

/**
 * 用于标注当前是否正在安装版本，如果正在安装，则禁止VersionsManager刷新版本列表触发版本合并
 * @see com.movtery.zalithlauncher.feature.version.VersionsManager
 */
class InstallingVersionEvent