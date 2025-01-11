package com.movtery.zalithlauncher.feature.customprofilepath

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.task.Task
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
        private var sIsLauncherProfileChecked: Boolean = false

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
                            if (!sIsLauncherProfileChecked) {
                                tryUnpackLauncherProfiles(defaultPath)
                                dataMap.forEach { (_, json) ->
                                    tryUnpackLauncherProfiles("${json.path}/.minecraft")
                                }
                                sIsLauncherProfileChecked = true
                            }
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

        /**
         * 写入一个默认的 launcher_profiles.json 文件，不存在将会导致 Forge、NeoForge 等无法正常安装
         */
        private fun tryUnpackLauncherProfiles(path: String) {
            Task.runTask {
                File(path, "launcher_profiles.json").run {
                    if (!exists()) {
                        createNewFile()
                        writeText(
                            """{"profiles":{"default":{"lastVersionId":"latest-release"}},"selectedProfile":"default"}""".trimIndent()
                        )
                    }
                }
            }.onThrowable { e ->
                Logging.e("Write launcher_profiles.json", Tools.printToString(e))
            }.execute()
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
