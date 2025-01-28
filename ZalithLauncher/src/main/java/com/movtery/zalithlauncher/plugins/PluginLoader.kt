package com.movtery.zalithlauncher.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.movtery.zalithlauncher.plugins.driver.DriverPluginManager
import com.movtery.zalithlauncher.plugins.renderer.RendererPluginManager
import com.movtery.zalithlauncher.renderer.AbstractRenderer
import com.movtery.zalithlauncher.renderer.Renderers
import com.movtery.zalithlauncher.utils.path.PathManager
import org.apache.commons.io.FileUtils

/**
 * 统一插件的加载，保证仅获取一次应用列表
 */
object PluginLoader {
    private var isInitialized: Boolean = false
    private const val PACKAGE_FLAGS =
        PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES

    @JvmStatic
    @SuppressLint("QueryPermissionsNeeded")
    fun loadAllPlugins(context: Context) {
        if (isInitialized) return
        isInitialized = true

        DriverPluginManager.initDriver(context)

        val queryIntentActivities =
            context.packageManager.queryIntentActivities(
                Intent("android.intent.action.MAIN"),
                PACKAGE_FLAGS
            )
        queryIntentActivities.forEach {
            val applicationInfo = it.activityInfo.applicationInfo
            DriverPluginManager.parsePlugin(applicationInfo)
            RendererPluginManager.parseApkPlugin(context, applicationInfo)
        }

        //尝试解析本地渲染器插件
        PathManager.DIR_INSTALLED_RENDERER_PLUGIN.listFiles()?.let { files ->
            files.forEach { file ->
                if (!(file.isDirectory && RendererPluginManager.parseLocalPlugin(context, file))) {
                    //不符合要求的渲染器插件，将被删除！
                    FileUtils.deleteQuietly(file)
                }
            }
        }

        if (RendererPluginManager.isAvailable()) {
            RendererPluginManager.getRendererList().forEach { rendererPlugin ->
                Renderers.addRenderer(
                    object : AbstractRenderer {
                        override fun getRendererId(): String = rendererPlugin.id

                        override fun getUniqueIdentifier(): String = rendererPlugin.uniqueIdentifier

                        override fun getRendererName(): String = rendererPlugin.des

                        override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
                            RendererPluginManager.progressEnvMap(rendererPlugin)
                        }

                        override fun getDlopenLibrary(): Lazy<List<String>> = lazy { rendererPlugin.dlopen }

                        override fun getRendererLibrary(): String = rendererPlugin.glName

                        override fun getRendererEGL(): String = rendererPlugin.eglName
                    }
                )
            }
        }
    }
}