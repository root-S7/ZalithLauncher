package com.movtery.zalithlauncher.feature.customprofilepath

import android.content.Context
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.task.Task
import net.kdt.pojavlaunch.Tools
import java.io.File

class ProfilePathHome {
    companion object {
        @JvmStatic
        val gameHome: String
            get() = "${ProfilePathManager.currentPath}/.minecraft"

        @JvmStatic
        val versionsHome: String
            get() = "$gameHome/versions"

        @JvmStatic
        val librariesHome: String
            get() = "$gameHome/libraries"

        @JvmStatic
        val assetsHome: String
            get() = "$gameHome/assets"

        @JvmStatic
        val resourcesHome: String
            get() = "$gameHome/resources"

        /**
         * 检查launcher_profiles.json文件是否存在，如果不存在，Forge将无法安装
         */
        @JvmStatic
        fun checkForLauncherProfiles(context: Context) {
            Task.runTask {
                val launcherProfiles = "launcher_profiles.json"
                val launcherProfilesFile = File(gameHome, launcherProfiles)
                if (!launcherProfilesFile.exists()) {
                    //如果这个配置文件不存在，那么久复制一份，Forge安装需要这个文件
                    Tools.copyAssetFile(context, "launcher_profiles.json", gameHome, false)
                }
            }.onThrowable { e ->
                Logging.e("Unpack Launcher Profiles", Tools.printToString(e))
            }.execute()
        }
    }
}
