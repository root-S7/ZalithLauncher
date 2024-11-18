package com.movtery.zalithlauncher.feature.version

import android.app.Activity
import com.movtery.zalithlauncher.event.sticky.InstallingVersionEvent
import com.movtery.zalithlauncher.event.value.InstallGameEvent
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.task.TaskExecutors
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader
import net.kdt.pojavlaunch.tasks.MinecraftDownloader
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class GameInstaller(
    private val activity: Activity,
    installEvent: InstallGameEvent
) {
    private val realVersion: String = installEvent.minecraftVersion
    private val customVersionName: String = installEvent.customVersionName
    private val taskMap: Map<Addon, InstallTaskItem> = installEvent.taskMap
    private val isolation = installEvent.isIsolation
    private val targetVersionFolder = VersionsManager.getVersionPath(customVersionName)
    private val vanillaVersionFolder = VersionsManager.getVersionPath(realVersion)

    fun installGame() {
        Logging.i("Minecraft Downloader", "Start downloading the version: $realVersion")

        val mcVersion = AsyncMinecraftDownloader.getListedVersion(realVersion)
        MinecraftDownloader().start(
            mcVersion,
            realVersion,
            object : AsyncMinecraftDownloader.DoneListener {
                override fun onDownloadDone() {
                    val installModVersion = InstallingVersionEvent()
                    Task.runTask {
                        if (isolation) {
                            if (!targetVersionFolder.exists() && !targetVersionFolder.mkdirs()) throw IOException("Failed to create version folder!")
                            VersionConfig(targetVersionFolder).saveWithThrowable() //保存版本隔离的特征文件
                        }

                        if (taskMap.isNotEmpty()) EventBus.getDefault().postSticky(installModVersion)
                        else {
                            //如果附加附件是空的，则表明只需要安装原版，需要确保这个自定义的版本文件夹内必定有原版的.json文件
                            if (VersionsManager.isVersionExists(realVersion)) {
                                //找到原版的.json文件，在MinecraftDownloader开始时，已经下载了
                                val vanillaJsonFile = File(vanillaVersionFolder, "${vanillaVersionFolder.name}.json")
                                if (vanillaJsonFile.exists() && vanillaJsonFile.isFile) {
                                    //如果原版的.json文件存在，则直接复制过来用
                                    FileUtils.copyFile(vanillaJsonFile, File(targetVersionFolder, "$customVersionName.json"))
                                }
                            }
                        }

                        //将Mod与Modloader的任务分离出来，应该先安装Mod
                        val modTask: MutableList<InstallTaskItem> = ArrayList()
                        val modloaderTask = AtomicReference<Pair<Addon, InstallTaskItem>>() //暂时只允许同时安装一个ModLoader
                        taskMap.forEach { (addon, taskItem) ->
                            if (taskItem.isMod) modTask.add(taskItem)
                            else modloaderTask.set(Pair(addon, taskItem))
                        }

                        //下载Mod文件
                        modTask.forEach { task ->
                            Logging.i("Install Version", "Installing Mod: ${task.selectedVersion}")
                            val file = task.task.run()
                            val endTask = task.endTask
                            file?.let { endTask?.endTask(activity, it) }
                        }

                        //开始安装ModLoader，可能会创建新的版本文件夹，所以在这一步开始打个标记
                        VersionFolderChecker.checkVersionsFolder(forceCheck = true, identifier = customVersionName)

                        modloaderTask.get()?.let { taskPair ->
                            VersionInfo(
                                realVersion,
                                arrayOf(
                                    VersionInfo.LoaderInfo(
                                        taskPair.first.addonName,
                                        taskPair.second.selectedVersion
                                    )
                                )
                            ).save(targetVersionFolder)

                            Logging.i("Install Version", "Installing ModLoader: ${taskPair.second.selectedVersion}")
                            val file = taskPair.second.task.run()
                            return@runTask Pair(file, taskPair.second)
                        }

                        if (customVersionName != realVersion) {
                            VersionInfo(realVersion, emptyArray()).save(targetVersionFolder)
                        }
                        null
                    }.onThrowable { e ->
                        Tools.showErrorRemote(e)
                    }.ended ended@{ taskPair ->
                        taskPair?.let { pair ->
                            val file = pair.first
                            val taskItem = pair.second

                            file?.let {
                                taskItem.endTask?.let { endTask ->
                                    TaskExecutors.runInUIThread {
                                        endTask.endTask(activity, it)
                                    }
                                }
                                return@ended
                            }

                            //Quilt使用直接下载版本json文件的方式进行安装
                            moveVersionFiles()
                            VersionsManager.refresh()
                        }
                    }.finallyTask {
                        EventBus.getDefault().removeStickyEvent(installModVersion)
                    }.execute()
                }

                override fun onDownloadFailed(throwable: Throwable) {
                    Tools.showErrorRemote(throwable)
                }
            }
        )
    }

    companion object {
        @JvmStatic
        fun moveVersionFiles() {
            val foldersPair = VersionFolderChecker.checkVersionsFolder(read = true)
            val versionFolder = File(ProfilePathHome.versionsHome, foldersPair.second)

            if (foldersPair.first.isNotEmpty()) {
                val originalJarFile = File(versionFolder, "${foldersPair.second}.jar")
                val originalJsonFile = File(versionFolder, "${foldersPair.second}.json")

                foldersPair.first.forEach { folder ->
                    //需要确保两个文件夹并不是同一个文件夹，因为那根本不需要进行移动
                    if (folder.exists() && folder.isDirectory && versionFolder.absolutePath != folder.absolutePath) {
                        //移除原本的核心文件
                        FileUtils.deleteQuietly(originalJarFile)
                        FileUtils.deleteQuietly(originalJsonFile)

                        val jarFile = File(folder, "${folder.name}.jar")
                        val jsonFile = File(folder, "${folder.name}.json")

                        if (jarFile.exists()) FileUtils.moveFile(jarFile, originalJarFile)
                        if (jsonFile.exists()) FileUtils.moveFile(jsonFile, originalJsonFile)

                        FileUtils.deleteQuietly(folder)
                    }
                }
            }
        }
    }
}