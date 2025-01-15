package com.movtery.zalithlauncher.plugins.renderer

import com.movtery.zalithlauncher.feature.update.UpdateUtils
import com.movtery.zalithlauncher.plugins.renderer.RendererPlugin.Companion.rendererPluginList
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import java.io.File

/**
 * 从本地 `/files/renderer_plugins/` 目录下尝试解析渲染器插件
 * 只是一个备用方案，暂时不支持直接导入
 *
 * 渲染器文件夹格式
 * renderer_plugins/
 * ----文件夹名称/
 * --------renderer_config.json (存放渲染器具体信息的配置文件)
 * --------libs/ (渲染器`.so`文件的存放目录)
 * ------------arm64-v8a/ (arm64架构)
 * ----------------渲染器库文件.so
 * ------------armeabi-v7a/ (arm32架构)
 * ----------------渲染器库文件.so
 * ------------x86/ (x86架构)
 * ----------------渲染器库文件.so
 * ------------x86_64/ (x86_64架构)
 * ----------------渲染器库文件.so
 */
object InstalledRendererPluginUtils {
    data class RendererConfig(
        val rendererId: String,
        val rendererDisplayName: String,
        val glName: String,
        val eglName: String,
        val env: Map<String, String>
    )

    private var isInitialized: Boolean = false

    @JvmStatic
    fun initRenderers() {
        if (isInitialized) return
        isInitialized = true

        PathManager.DIR_INSTALLED_RENDERER_PLUGIN.listFiles()?.let { files ->
            files.forEach { file ->
                if (file.isDirectory) {
                    parsePlugin(file)
                }
            }
        }
    }

    /**
     * 尝试解析渲染器插件
     */
    private fun parsePlugin(directory: File) {
        val archModel: String = UpdateUtils.getArchModel(Architecture.getDeviceArchitecture()) ?: return
        val libsDirectory: File = File(directory, "libs/$archModel").takeIf { it.exists() && it.isDirectory } ?: return
        val rendererConfigFile: File = File(directory, "renderer_config.json").takeIf { it.exists() && it.isFile } ?: return
        val rendererConfig: RendererConfig = runCatching {
            Tools.GLOBAL_GSON.fromJson(Tools.read(rendererConfigFile), RendererConfig::class.java)
        }.getOrElse { return }
        rendererConfig.run {
            if (!rendererPluginList.any { it.id == rendererId }) {
                rendererPluginList.add(
                    RendererPlugin(
                        rendererId, rendererDisplayName, glName, eglName, libsDirectory.absolutePath,
                        env.toList()
                    )
                )
            }
        }
    }
}