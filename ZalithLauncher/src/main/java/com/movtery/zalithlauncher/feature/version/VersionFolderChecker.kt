package com.movtery.zalithlauncher.feature.version

import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files

class VersionFolderChecker {
    companion object {
        private const val CACHE_FILE_NAME = "folder_cache.txt"

        /**
         * 标记当前版本文件夹内的文件夹情况
         */
        @JvmStatic
        fun markVersionsFolder(
            versionName: String,
            loaderName: String,
            loaderVersion: String
        ) {
            val currentVersionsFolder = File(ProfilePathHome.versionsHome)
            val cacheFile = File(currentVersionsFolder, CACHE_FILE_NAME)

            FileUtils.deleteQuietly(cacheFile)

            val currentFolders = getCurrentFolders(currentVersionsFolder)
            writeCacheFile(currentFolders, cacheFile, "$versionName;&&;$loaderName;&&;$loaderVersion")
        }

        /**
         * 通过缓存当前的版本文件夹内的文件夹情况，检查是否多出了文件夹
         * @return 返回检查出的，多出来的文件夹的 File 对象集合以及信息
         */
        @JvmStatic
        fun checkVersionsFolder(): Pair<Set<File>, LoaderInfo>? {
            val currentVersionsFolder = File(ProfilePathHome.versionsHome)
            val cacheFile = File(currentVersionsFolder, CACHE_FILE_NAME)

            if (cacheFile.exists()) {
                //读取缓存文件并检查新增的文件夹
                val (cachedFolders, cachedIdentifier) = readCacheFile(cacheFile)
                val currentFolders = getCurrentFolders(currentVersionsFolder)
                val newFolders = currentFolders - cachedFolders

                FileUtils.deleteQuietly(cacheFile)

                val (version, loaderName, loaderVersion) = cachedIdentifier.split(";&&;")

                return Pair(newFolders, LoaderInfo(version, loaderName, loaderVersion))
            }
            return null
        }

        /**
         * 获取当前版本文件夹下的所有文件夹，并返回 File 对象集合
         */
        private fun getCurrentFolders(versionsFolder: File): Set<File> {
            val folders = mutableSetOf<File>()
            if (!versionsFolder.exists()) versionsFolder.mkdirs()
            Files.newDirectoryStream(versionsFolder.toPath()).use { stream ->
                for (entry in stream) {
                    if (Files.isDirectory(entry)) {
                        folders.add(entry.toFile())
                    }
                }
            }
            return folders
        }

        /**
         * 读取缓存文件中的文件夹名称和标识信息
         */
        private fun readCacheFile(cacheFile: File): Pair<Set<File>, String> {
            return if (cacheFile.exists()) {
                val lines = cacheFile.readLines()
                val cachedFolders = lines.dropLast(1)
                    .map { File(cacheFile.parent, it) }
                    .toSet()
                val cachedIdentifier = lines.lastOrNull() ?: ""
                Pair(cachedFolders, cachedIdentifier)
            } else {
                Pair(emptySet(), "")
            }
        }

        /**
         * 写入当前文件夹名称和标识信息到缓存文件
         */
        private fun writeCacheFile(folders: Set<File>, cacheFile: File, identifier: String) {
            val folderNames = folders.joinToString("\n") { it.name }
            val content = "$folderNames\n$identifier"
            cacheFile.writeText(content)
        }
    }

    class LoaderInfo(
        val customVersion: String,
        val name: String,
        val version: String
    )
}
