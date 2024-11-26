package com.movtery.zalithlauncher.event.value

/**
 * 切换下载页面时，使用这个事件通知Fragment播放动画
 * @param index Fragment的类别索引
 * @param classify 动画类型（IN：进入动画，OUT：退出动画）
 */
class DownloadPageSwapEvent(val index: Int, val classify: Int) {
    companion object {
        const val IN = 0
        const val OUT = 1
    }
}