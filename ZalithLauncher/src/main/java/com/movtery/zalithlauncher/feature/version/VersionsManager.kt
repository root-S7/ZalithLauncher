package com.movtery.zalithlauncher.feature.version

import com.movtery.zalithlauncher.event.single.RefreshVersionsEvent
import com.movtery.zalithlauncher.event.sticky.InstallingVersionEvent
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.utils.file.FileTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileWriter

/**
 * 所有版本管理者
 * @see Version
 */
object VersionsManager {
    private val versions: MutableList<Version> = ArrayList()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val refreshMutex = Mutex()

    /**
     * @return 全部的版本数据
     */
    fun getVersions() = ArrayList(versions)

    /**
     * 检查版本是否已经存在
     */
    fun isVersionExists(versionName: String): Boolean {
        return File(ProfilePathHome.versionsHome, versionName).exists()
    }

    /**
     * 异步刷新当前的版本列表，刷新完成后，将使用一个事件进行通知，不过这个事件并不会在UI线程执行
     * @see com.movtery.zalithlauncher.event.single.RefreshVersionsEvent
     */
    fun refresh() {
        //如果版本正在安装中，则禁止刷新版本列表，这可能会提前触发版本文件夹合并机制，引发不必要的Bug
        EventBus.getDefault().getStickyEvent(InstallingVersionEvent::class.java)?.let { return }

        coroutineScope.launch {
            refreshWithMutex()
        }
    }

    private suspend fun refreshWithMutex() {
        refreshMutex.withLock {
            try {
                versions.clear()

                File(ProfilePathHome.versionsHome).listFiles()?.forEach { versionFile ->
                    if (versionFile.exists() && versionFile.isDirectory) {
                        var isVersion = false
                        var versionConfig: VersionConfig? = null
                        versionFile.listFiles()?.let checkVersionFolder@{ fileArray ->
                            val fileList = fileArray.toList()

                            //通过判断是否存在版本的.json文件，来确定其是否为一个版本
                            if (fileList.contains(File(versionFile, "${versionFile.name}.json"))) {
                                isVersion = true
                            }

                            val configFile = File(getZalithVersionPath(versionFile), "ZalithVersion.cfg")
                            if (configFile.exists()) versionConfig = runCatching {
                                //读取此文件的内容，并解析为VersionConfig
                                Tools.GLOBAL_GSON.fromJson(Tools.read(configFile), VersionConfig::class.java)
                            }.getOrElse { e ->
                                Logging.e("Refresh Versions", Tools.printToString(e))
                                null
                            }
                        }

                        versions.add(
                            Version(
                                versionFile.name,
                                versionConfig,
                                isVersion
                            )
                        )
                    }
                }

                GameInstaller.moveVersionFiles()
            } finally {
                //使用事件通知版本已刷新
                EventBus.getDefault().post(RefreshVersionsEvent())
            }
        }
    }

    /**
     * @return 获取当前的版本
     */
    fun getCurrentVersion(): Version? {
        if (versions.isEmpty()) return null

        getPathConfigFile().apply {
            return if (exists()) {
                runCatching {
                    val string = Tools.read(this)
                    getVersion(string) ?: run {
                        val version = versions[0]
                        saveCurrentVersion(version.getVersionName())
                        version
                    }
                }.getOrElse { e ->
                    Logging.e("Get Current Version", Tools.printToString(e))
                    null
                }
            } else null
        }
    }

    /**
     * @return 获取 Zalith 启动器版本标识文件夹
     */
    fun getZalithVersionPath(version: Version) = File(getVersionPath(version), "ZalithLauncher")

    /**
     * @return 通过目录获取 Zalith 启动器版本标识文件夹
     */
    fun getZalithVersionPath(folder: File) = File(folder, "ZalithLauncher")

    /**
     * @return 通过名称获取 Zalith 启动器版本标识文件夹
     */
    fun getZalithVersionPath(name: String) = File(getVersionPath(name), "ZalithLauncher")

    /**
     * @return 获取当前版本设置的图标
     */
    fun getVersionIconFile(version: Version) = File(getZalithVersionPath(version), "VersionIcon.png")

    /**
     * @return 通过名称获取当前版本设置的图标
     */
    fun getVersionIconFile(name: String) = File(getZalithVersionPath(name), "VersionIcon.png")

    /**
     * @return 获取版本的文件夹路径
     */
    fun getVersionPath(version: Version) = File(ProfilePathHome.versionsHome, version.getVersionName())

    /**
     * @return 通过名称获取版本的文件夹路径
     */
    fun getVersionPath(name: String) = File(ProfilePathHome.versionsHome, name)

    /**
     * 保存当前选择的版本
     */
    fun saveCurrentVersion(versionName: String) {
        getPathConfigFile().apply {
            runCatching {
                if (!exists()) createNewFile()
                FileWriter(this).use { it.write(versionName) }
            }.getOrElse { e -> Logging.e("Save Current Version", Tools.printToString(e)) }
        }
    }

    /**
     * 重命名当前版本，但并不会在这里对即将重命名的名称，进行非法性判断
     */
    fun renameVersion(version: Version, name: String) {
        val versionFolder = getVersionPath(version)
        val renameFolder = File(ProfilePathHome.versionsHome, name)

        val originalName = versionFolder.name

        FileTools.renameFile(versionFolder, renameFolder)

        val versionJsonFile = File(renameFolder, "$originalName.json")
        val versionJarFile = File(renameFolder, "$originalName.jar")
        val renameJsonFile = File(renameFolder, "$name.json")
        val renameJarFile = File(renameFolder, "$name.jar")

        FileTools.renameFile(versionJsonFile, renameJsonFile)
        FileTools.renameFile(versionJarFile, renameJarFile)

        FileUtils.deleteQuietly(versionFolder)

        //重命名后，需要刷新列表
        refresh()
    }

    /**
     * @return 获取当前路径的版本配置文件
     */
    private fun getPathConfigFile() = File(ProfilePathHome.gameHome, "CurrentVersion.cfg")

    private fun getVersion(name: String?): Version? {
        name?.let { versionName ->
            versions.forEach { if (it.getVersionName() == versionName) return it }
        }
        return null
    }
}