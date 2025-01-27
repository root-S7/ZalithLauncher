package com.movtery.zalithlauncher.launch

/**
 * 添加参数回调，用于在启动时额外添加一些用户参数
 */
fun interface UserArgsCallBack {
    fun callback(list: MutableList<String>)
}