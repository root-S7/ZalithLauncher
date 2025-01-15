package com.movtery.zalithlauncher.plugins.renderer

import android.content.Context
import android.content.pm.ApplicationInfo
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.update.UpdateUtils
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import java.io.File

/**
 * FCL、ZalithLauncher 渲染器插件，同时支持使用本地渲染器插件（暂时没有启动器内导入方案）
 * [FCL Renderer Plugin](https://github.com/FCL-Team/FCLRendererPlugin)
 */
object RendererPluginManager {
    private val rendererPluginList: MutableList<RendererPlugin> = mutableListOf()

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

    /**
     * 解析 ZalithLauncher、FCL 渲染器插件
     */
    internal fun parseApkPlugin(context: Context, info: ApplicationInfo) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (
                metaData.getBoolean("fclPlugin", false) ||
                metaData.getBoolean("zalithRendererPlugin", false)
            ) {
                val rendererString = metaData.getString("renderer") ?: return
                val des = metaData.getString("des") ?: return
                val pojavEnvString = metaData.getString("pojavEnv") ?: return
                val nativeLibraryDir = info.nativeLibraryDir
                val renderer = rendererString.split(":")
                val pojavEnvPair = pojavEnvString.split(":").run {
                    val envPairList = mutableListOf<Pair<String, String>>()
                    forEach { envString ->
                        if (envString.contains("=")) {
                            val stringList = envString.split("=")
                            envPairList.add(Pair(stringList[0], stringList[1]))
                        }
                    }
                    envPairList
                }
                val rendererId = pojavEnvPair.find { it.first == "POJAV_RENDERER" || it.first == "SELECT_RENDERER" }?.second ?: renderer[0]
                if (!rendererPluginList.any { it.id == rendererId }) {
                    rendererPluginList.add(
                        RendererPlugin(
                            rendererId,
                            "$des (${
                                context.getString(
                                    R.string.setting_renderer_from_plugins,
                                    runCatching {
                                        context.packageManager.getApplicationLabel(info)
                                    }.getOrElse {
                                        context.getString(R.string.generic_unknown)
                                    }
                                )
                            })",
                            renderer[1],
                            renderer[2],
                            nativeLibraryDir,
                            pojavEnvPair
                        )
                    )
                }
            }
        }
    }

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
    internal fun parseLocalPlugin(context: Context, directory: File) {
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
                        rendererId,
                        "$rendererDisplayName (${
                            context.getString(
                                R.string.setting_renderer_from_plugins,
                                directory.name
                            )
                        })", glName, eglName,
                        libsDirectory.absolutePath,
                        env.toList()
                    )
                )
            }
        }
    }
}