package com.movtery.zalithlauncher.plugins.renderer

import net.kdt.pojavlaunch.Tools

data class RendererPlugin(
    val id: String,
    val des: String,
    val glName: String,
    val eglName: String,
    val path: String,
    val env: List<Pair<String, String>>
) {
    companion object {
        @JvmStatic
        internal val rendererPluginList: MutableList<RendererPlugin> = mutableListOf()

        @JvmStatic
        fun getRendererList() = ArrayList(rendererPluginList)

        @JvmStatic
        fun isAvailable(): Boolean {
            return rendererPluginList.isNotEmpty()
        }

        @JvmStatic
        val selectedRendererPlugin: RendererPlugin?
            get() {
                return getRendererList().find { it.id == Tools.LOCAL_RENDERER }
            }
    }
}
