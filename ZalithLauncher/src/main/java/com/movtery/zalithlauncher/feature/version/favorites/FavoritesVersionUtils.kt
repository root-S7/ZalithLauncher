package com.movtery.zalithlauncher.feature.version.favorites

import com.movtery.zalithlauncher.feature.version.CurrentGameInfo
import com.movtery.zalithlauncher.feature.version.VersionsManager

class FavoritesVersionUtils {
    companion object {
        private fun CurrentGameInfo.CurrentInfo.initFavoritesMap() {
            favoritesMap ?: run {
                favoritesMap = mutableMapOf()
            }
        }

        private fun doInFavoritesMap(func: (MutableMap<String, MutableSet<String>>) -> Unit) {
            CurrentGameInfo.getCurrentInfo().apply {
                initFavoritesMap()
                favoritesMap?.let { func(it) }
                saveCurrentInfo()
            }
        }

        /**
         * 刷新收藏夹内的版本，版本不存在则清除
         */
        fun refreshFavoritesFolder() {
            doInFavoritesMap { map ->
                map.entries.forEach { (folderName, versionNames) ->
                    map[folderName] = versionNames.filter { VersionsManager.checkVersionExistsByName(it) }.toMutableSet()
                }
            }
        }

        /**
         * 重命名版本时，将收藏夹内的同名版本也进行重命名
         */
        fun renameVersion(versionName: String, newVersionName: String) {
            doInFavoritesMap { map ->
                map.forEach { (_, versionNames) ->
                    if (versionNames.remove(versionName)) {
                        versionNames.add(newVersionName)
                    }
                }
            }
        }

        /**
         * 添加一个收藏夹
         */
        fun addFolder(folderName: String) {
            doInFavoritesMap { map ->
                map.takeIf { !it.containsKey(folderName) }?.set(folderName, HashSet())
            }
        }

        /**
         * 移除一个收藏夹
         */
        fun removeFolder(folderName: String) {
            doInFavoritesMap { map ->
                map.remove(folderName)
            }
        }

        /**
         * 将版本存入指定的收藏夹，同时从未被指定的收藏夹中移除
         */
        fun saveVersionToFavorites(versionName: String, folders: Set<String>) {
            doInFavoritesMap { map ->
                folders.forEach { folder ->
                    map.getOrPut(folder) { mutableSetOf() }.add(versionName)
                }

                //遍历未指定的收藏夹，从这些收藏夹中移除版本（如果存在）
                (getAllFolders().toSet() - folders).forEach { folder ->
                    map[folder]?.remove(versionName)
                }
            }
        }

        /**
         * 获取全部的收藏夹名称
         */
        fun getAllFolders(): List<String> = getFavoritesMap().keys.toList()

        /**
         * 获得对应收藏夹的所有版本名称
         */
        fun getAllVersions(folderName: String): Set<String>? = getFavoritesMap()[folderName]

        fun getFavoritesMap() = CurrentGameInfo.getCurrentInfo().apply {
            initFavoritesMap()
        }.favoritesMap!!
    }
}