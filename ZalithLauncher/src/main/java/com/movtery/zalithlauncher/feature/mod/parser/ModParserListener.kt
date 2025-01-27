package com.movtery.zalithlauncher.feature.mod.parser

/**
 * 模组解析进度监听器，用于回调当前已经处理的模组和模组总数
 */
fun interface ModParserListener {
    /**
     * 解析完成后通过这个函数将解析的结果进行回调
     * @param modInfoList 所有模组信息列表
     */
    fun onParseEnded(modInfoList: List<ModInfo>)
}