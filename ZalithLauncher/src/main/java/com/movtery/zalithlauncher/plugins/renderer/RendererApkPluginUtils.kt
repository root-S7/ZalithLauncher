package com.movtery.zalithlauncher.plugins.renderer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.plugins.renderer.RendererPlugin.Companion.rendererPluginList

/**
 * FCL、ZalithLauncher 渲染器插件
 * [FCL Renderer Plugin](https://github.com/FCL-Team/FCLRendererPlugin)
 */
object RendererApkPluginUtils {
    private var isInitialized: Boolean = false
    private const val PACKAGE_FLAGS = PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES

    @JvmStatic
    @SuppressLint("QueryPermissionsNeeded")
    fun initRenderers(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val queryIntentActivities =
            context.packageManager.queryIntentActivities(Intent("android.intent.action.MAIN"), PACKAGE_FLAGS)
        queryIntentActivities.forEach {
            val activityInfo = it.activityInfo
            val packageName = activityInfo.packageName
            if (
                packageName.startsWith("com.movtery.zalithplugin.renderer") ||
                packageName.startsWith("com.mio.plugin.renderer")
            ) {
                //尝试进行解析渲染器插件
                parsePlugin(context, activityInfo.applicationInfo)
            }
        }
    }

    /**
     * 解析 ZalithLauncher、FCL 渲染器插件
     */
    private fun parsePlugin(context: Context, info: ApplicationInfo) {
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
                val rendererId = pojavEnvPair.find { it.first == "POJAV_RENDERER" }?.second ?: renderer[0]
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
}