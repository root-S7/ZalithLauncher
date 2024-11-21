package com.movtery.zalithlauncher.feature.version

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.event.single.RefreshVersionsEvent
import com.movtery.zalithlauncher.event.sticky.InstallingVersionEvent
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.ui.dialog.EditTextDialog
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
    fun isVersionExists(versionName: String, checkJson: Boolean = false): Boolean {
        val folder = File(ProfilePathHome.versionsHome, versionName)
        //保证版本文件夹存在的同时，也应保证其版本json文件存在
        return if (checkJson) File(folder, "${folder.name}.json").exists()
        else folder.exists()
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

                val versionsHome = ProfilePathHome.versionsHome
                File(versionsHome).listFiles()?.forEach { versionFile ->
                    if (versionFile.exists() && versionFile.isDirectory) {
                        var isVersion = false
                        var versionConfig: VersionConfig? = null

                        //通过判断是否存在版本的.json文件，来确定其是否为一个版本
                        val jsonFile = File(versionFile, "${versionFile.name}.json")
                        if (jsonFile.exists() && jsonFile.isFile) {
                            isVersion = true
                            if (!File(getZalithVersionPath(versionFile), "VersionInfo.json").exists()) {
                                VersionInfoUtils.parseJson(jsonFile)?.save(versionFile)
                            }
                        }

                        val configFile = File(getZalithVersionPath(versionFile), "ZalithVersion.cfg")
                        if (configFile.exists() && configFile.isFile) versionConfig = runCatching {
                            //读取此文件的内容，并解析为VersionConfig
                            val config = Tools.GLOBAL_GSON.fromJson(Tools.read(configFile), VersionConfig::class.java)
                            config.setVersionPath(versionFile)
                            config
                        }.getOrElse { e ->
                            Logging.e("Refresh Versions", Tools.printToString(e))
                            null
                        }

                        versions.add(
                            Version(
                                versionsHome,
                                versionFile.absolutePath,
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
                        versions.forEach { version ->
                            if (version.isValid()) {
                                //确保版本有效
                                saveCurrentVersion(version.getVersionName())
                                return version
                            }
                        }
                        //如果所有版本都无效，或者没有版本，那么久返回空
                        null
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
    fun getZalithVersionPath(version: Version) = File(version.getVersionPath(), "ZalithLauncher")

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
     * 打开重命名版本的弹窗，需要确保在UI线程运行
     * @param beforeRename 在重命名前一步的操作
     */
    fun openRenameDialog(context: Context, version: Version, beforeRename: (() -> Unit)? = null) {
        EditTextDialog.Builder(context)
            .setTitle(R.string.version_manager_rename)
            .setEditText(version.getVersionName())
            .setConfirmListener { editText ->
                val string = editText.text.toString()

                //与原始名称一致
                if (string == version.getVersionName()) return@setConfirmListener true

                if (isVersionExists(string, true)) {
                    editText.error = context.getString(R.string.version_install_exists)
                    return@setConfirmListener false
                }

                version.getVersionInfo()?.let { info ->
                    //如果这个版本是有ModLoader加载器信息的，则不允许修改为与原版名称一致的名称，防止冲突
                    info.loaderInfo?.let { loaderInfo ->
                        if (loaderInfo.isNotEmpty() && string == info.minecraftVersion) {
                            editText.error = context.getString(R.string.version_install_cannot_use_mc_name)
                            return@setConfirmListener false
                        }
                    }
                }

                beforeRename?.invoke()
                renameVersion(version, string)

                true
            }.buildDialog()
    }

    /**
     * 重命名当前版本，但并不会在这里对即将重命名的名称，进行非法性判断
     */
    private fun renameVersion(version: Version, name: String) {
        val versionFolder = version.getVersionPath()
        val renameFolder = File(ProfilePathHome.versionsHome, name)

        //不管重命名之后的文件夹是什么，只要这个文件夹存在，那么就必须删除
        //否则将出现问题
        FileUtils.deleteQuietly(renameFolder)

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
            versions.forEach { version ->
                if (version.getVersionName() == versionName) {
                    return version.takeIf { it.isValid() }
                }
            }
        }
        return null
    }
}