package com.movtery.zalithlauncher.feature.version

import com.google.gson.annotations.SerializedName
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.favorites.FavoritesVersionUtils
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import java.io.File

class CurrentGameInfo {
    companion object {
        private lateinit var currentInfo: CurrentInfo

        /**
         * 获取当前游戏路径内的信息配置文件
         */
        private fun getInfoFile() = File(ProfilePathHome.getGameHome(), "CurrentInfo.cfg")

        fun refreshCurrentInfo() {
            fun createDefault() = CurrentInfo().apply {
                saveCurrentInfo()
            }

            /**
             * 检查旧的“当前版本”配置文件，如果存在，那么将其同步为新的格式
             * 同步完成后，删除旧的“当前版本”配置文件
             */
            fun CurrentInfo.syncOldVersion(): CurrentInfo {
                File(ProfilePathHome.getGameHome(), "CurrentVersion.cfg").let { oldConfigFile ->
                    if (oldConfigFile.exists()) {
                        runCatching {
                            val versionString = Tools.read(oldConfigFile)
                            version = versionString
                            saveCurrentInfo()
                            oldConfigFile.delete()
                        }
                    }
                }
                return this
            }

            currentInfo = runCatching {
                val infoFile = getInfoFile()
                if (infoFile.exists()) {
                    Tools.GLOBAL_GSON.fromJson(Tools.read(infoFile), CurrentInfo::class.java)
                } else createDefault()
            }.getOrElse { e ->
                Logging.e("getCurrentInfo", "Failed to identify the current game information!", e)
                createDefault()
            }.syncOldVersion()

            FavoritesVersionUtils.refreshFavoritesFolder()
        }

        /**
         * 获取当前游戏信息（当前版本、收藏夹内容）
         */
        fun getCurrentInfo(): CurrentInfo = currentInfo
    }

    class CurrentInfo {
        @SerializedName("version")
        var version: String? = null
        @SerializedName("favoritesInfo")
        var favoritesMap: MutableMap<String, MutableSet<String>>? = null

        /**
         * 保存当前的游戏信息
         */
        fun saveCurrentInfo() {
            runCatching {
                val jsonString = Tools.GLOBAL_GSON.toJson(this)
                FileUtils.write(getInfoFile(), jsonString)
            }.getOrElse { e ->
                Logging.e("saveCurrentInfo", "Failed to save current game info!", e)
            }
        }

        override fun toString(): String = "CurrentInfo{version='$version', favoritesMap='$favoritesMap'}"
    }
}