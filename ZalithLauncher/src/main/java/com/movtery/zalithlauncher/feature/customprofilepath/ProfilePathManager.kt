package com.movtery.zalithlauncher.feature.customprofilepath

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.Settings
import com.movtery.zalithlauncher.ui.subassembly.customprofilepath.ProfileItem
import com.movtery.zalithlauncher.utils.path.PathManager
import com.movtery.zalithlauncher.utils.StoragePermissionsUtils
import net.kdt.pojavlaunch.Tools
import java.io.FileWriter
import java.io.IOException

class ProfilePathManager {
    companion object {
        private val defaultPath: String = PathManager.DIR_GAME_HOME

        @JvmStatic
        fun setCurrentPathId(id: String?) {
            Settings.Manager.put("launcherProfile", id).save()
            VersionsManager.refresh()
        }

        @JvmStatic
        val currentPath: String
            get() {
                if (StoragePermissionsUtils.checkPermissions()) {
                    //通过选中的id来获取当前路径
                    val id = AllSettings.launcherProfile
                    if (id == "default") return defaultPath

                    PathManager.FILE_PROFILE_PATH.apply {
                        if (exists()) {
                            runCatching {
                                val read = Tools.read(this)
                                val jsonObject = JsonParser.parseString(read).asJsonObject
                                if (jsonObject.has(id)) {
                                    val profilePathJsonObject = Tools.GLOBAL_GSON.fromJson(jsonObject[id], ProfilePathJsonObject::class.java)
                                    return profilePathJsonObject.path
                                }
                            }.getOrElse { e -> Logging.e("Read Profile", e.toString()) }
                        }
                    }
                }

                return defaultPath
            }

        @JvmStatic
        fun save(items: List<ProfileItem>) {
            val jsonObject = JsonObject()

            for (item in items) {
                if (item.id == "default") continue

                val profilePathJsonObject = ProfilePathJsonObject(item.title, item.path)
                jsonObject.add(item.id, Tools.GLOBAL_GSON.toJsonTree(profilePathJsonObject))
            }

            try {
                FileWriter(PathManager.FILE_PROFILE_PATH).use { fileWriter ->
                    Tools.GLOBAL_GSON.toJson(jsonObject, fileWriter)
                }
            } catch (e: IOException) {
                Logging.e("Write Profile", e.toString())
            }
        }
    }
}
