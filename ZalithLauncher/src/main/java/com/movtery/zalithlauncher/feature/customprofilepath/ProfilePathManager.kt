package com.movtery.zalithlauncher.feature.customprofilepath

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.subassembly.customprofilepath.ProfileItem
import com.movtery.zalithlauncher.utils.path.PathManager
import com.movtery.zalithlauncher.utils.StoragePermissionsUtils
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileWriter
import java.io.IOException

typealias ProfilePathDataMap = Map<String, ProfilePathJsonObject>

class ProfilePathManager {
    companion object {
        private val defaultPath: String = PathManager.DIR_GAME_HOME

        @JvmStatic
        fun setCurrentPathId(id: String) {
            AllSettings.launcherProfile.put(id).save()
            VersionsManager.refresh()
        }

        @JvmStatic
        fun getCurrentPath(): String = StoragePermissionsUtils.checkPermissions().let { hasPermissions ->
            if (!hasPermissions) defaultPath
            else {
                //通过选中的id来获取当前路径
                val id = AllSettings.launcherProfile.getValue()
                if (id == "default") defaultPath
                else {
                    PathManager.FILE_PROFILE_PATH.takeIf { it.exists() }?.let { profilePath ->
                        runCatching {
                            val read = Tools.read(profilePath)
                            val dataMap = Tools.GLOBAL_GSON.fromJson<ProfilePathDataMap>(read,
                                object : TypeToken<Map<String, ProfilePathJsonObject>>() {}.type
                            )
                            dataMap[id]?.path
                        }.getOrElse {
                            Logging.e("Read Profile", "Failed to parse the game path", it)
                            defaultPath
                        } ?: defaultPath
                    } ?: defaultPath
                }
            }
        }.apply {
            //标记这个目录，让系统别在这里获取媒体项目（图片等）
            runCatching {
                File(this, ".nomedia").takeIf { !it.exists() }?.apply { createNewFile() }
            }.getOrElse {
                Logging.e("No Media", "Unable to create a .nomedia file in the current directory.", it)
            }
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
                Logging.e("Write Profile", "Failed to write to game path configuration", e)
            }
        }
    }
}
