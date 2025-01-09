package com.movtery.zalithlauncher.feature.version.favorites

import com.movtery.zalithlauncher.feature.version.CurrentGameInfo

class FavoritesVersionUtils {
    companion object {
        private fun CurrentGameInfo.CurrentInfo.initFavoritesMap() {
            favoritesMap ?: run {
                favoritesMap = mutableMapOf()
            }
        }

        /**
         * 添加一个收藏夹
         */
        fun addCategory(categoryName: String) {
            CurrentGameInfo.getCurrentInfo().apply {
                initFavoritesMap()
                favoritesMap?.set(categoryName, HashSet())
                saveCurrentInfo()
            }
        }

        /**
         * 移除一个收藏夹
         */
        fun removeCategory(categoryName: String) {
            CurrentGameInfo.getCurrentInfo().apply {
                initFavoritesMap()
                favoritesMap?.remove(categoryName)
                saveCurrentInfo()
            }
        }

        /**
         * 将一个版本添加到一些收藏夹中
         */
        fun addVersionToCategory(versionName: String, vararg categories: String) {
            operateVersionFromCategory(categories) { versionsList ->
                versionsList.add(versionName)
            }
        }

        /**
         * 将一个版本从一些收藏夹内移除
         */
        fun removeVersionFromCategory(versionName: String, vararg categories: String) {
            operateVersionFromCategory(categories) { versionsList ->
                versionsList.remove(versionName)
            }
        }

        private fun operateVersionFromCategory(categories: Array<out String>, operate: (MutableSet<String>) -> Unit) {
            CurrentGameInfo.getCurrentInfo().apply {
                initFavoritesMap()
                favoritesMap?.let { map ->
                    categories.forEach { category ->
                        map[category] ?: run {
                            map[category] = mutableSetOf()
                        }
                        map[category]?.let {
                            operate(it)
                        }
                    }
                }
                saveCurrentInfo()
            }
        }

        /**
         * 获取全部的收藏夹名称
         */
        fun getAllCategories(): List<String> = getFavoritesMap().keys.toList()

        /**
         * 获得对应收藏夹的所有版本名称
         */
        fun getAllVersions(categoryName: String): Set<String>? = getFavoritesMap()[categoryName]

        fun getFavoritesMap() = CurrentGameInfo.getCurrentInfo().apply {
            initFavoritesMap()
        }.favoritesMap!!
    }
}