package com.movtery.zalithlauncher.feature.version.favorites

import com.movtery.zalithlauncher.feature.version.CurrentGameInfo

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
         * 添加一个收藏夹
         */
        fun addCategory(categoryName: String) {
            doInFavoritesMap { map ->
                map.takeIf { !it.containsKey(categoryName) }?.set(categoryName, HashSet())
            }
        }

        /**
         * 移除一个收藏夹
         */
        fun removeCategory(categoryName: String) {
            doInFavoritesMap { map ->
                map.remove(categoryName)
            }
        }

        /**
         * 将版本存入指定的收藏夹，同时从未被指定的收藏夹中移除
         */
        fun saveVersionToFavorites(versionName: String, categories: Set<String>) {
            doInFavoritesMap { map ->
                categories.forEach { category ->
                    map.getOrPut(category) { mutableSetOf() }.add(versionName)
                }

                //遍历未指定的收藏夹，从这些收藏夹中移除版本（如果存在）
                (getAllCategories().toSet() - categories).forEach { category ->
                    map[category]?.remove(versionName)
                }
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