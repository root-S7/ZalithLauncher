package com.movtery.zalithlauncher.context

import android.content.Context
import android.content.ContextWrapper
import com.movtery.zalithlauncher.plugins.PluginLoader
import com.movtery.zalithlauncher.renderer.Renderers
import com.movtery.zalithlauncher.setting.Settings
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class LocaleHelper(context: Context) : ContextWrapper(context) {
    companion object {
        fun setLocale(context: Context): ContextWrapper {
            //初始化路径
            PathManager.initContextConstants(context)
            //刷新启动器设置
            Settings.refreshSettings()
            //加载渲染器
            Renderers.init()
            //加载插件
            PluginLoader.loadAllPlugins(context)

            LauncherPreferences.loadPreferences()
            return LocaleHelper(context)
        }
    }
}