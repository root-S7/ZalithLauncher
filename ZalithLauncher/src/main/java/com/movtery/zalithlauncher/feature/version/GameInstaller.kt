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

                        modloaderTask.get()?.let { taskPair ->
                            //开始安装ModLoader，可能会创建新的版本文件夹，所以在这一步开始打个标记
                            VersionFolderChecker.markVersionsFolder(customVersionName, taskPair.first.addonName, taskPair.second.selectedVersion)

                            Logging.i("Install Version", "Installing ModLoader: ${taskPair.second.selectedVersion}")
                            val file = taskPair.second.task.run()
                            return@runTask Pair(file, taskPair.second)
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
                            //在这里解除一下限制，刷新一下
                            EventBus.getDefault().removeStickyEvent(installModVersion)
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
        /**
         * 检查当前版本目录下的文件夹缓存文件，查看当前是否有多出来的文件夹
         * 并自动解析新安装的版本需要的版本核心文件，从新的版本文件夹移动，或从已有的版本文件夹内复制
         */
        @JvmStatic
        fun moveVersionFiles() {
            val foldersPair = VersionFolderChecker.checkVersionsFolder() ?: return //缓存文件不存在，不检查
            val versionFolder = File(ProfilePathHome.versionsHome, foldersPair.second.customVersion)

            val loaderInfo = foldersPair.second
            if (foldersPair.first.isNotEmpty()) {
                //如果有多出来的文件夹，则查找哪一个文件夹可能是新安装的版本
                foldersPair.first.forEach { folder ->
                    if (folder.exists() && folder.isDirectory) {
                        val jarFile = File(folder, "${folder.name}.jar")
                        val jsonFile = File(folder, "${folder.name}.json")

                        if (jsonFile.exists()) {
                            //解析json文件，根据得到的Loader信息，来判断其是否为需要的版本
                            VersionInfoUtils.parseJson(jsonFile)?.loaderInfo?.let { info ->
                                if (info.isNotEmpty() && checkVersion(loaderInfo.name, loaderInfo.version, info[0])) {
                                    //由于是新安装的，所以这里直接移动核心文件，然后移除这个版本文件，就算安装完成了
                                    val originalJarFile = File(versionFolder, "${versionFolder.name}.jar")
                                    val originalJsonFile = File(versionFolder, "${versionFolder.name}.json")

                                    //移除原本的核心文件
                                    FileUtils.deleteQuietly(originalJarFile)
                                    FileUtils.deleteQuietly(originalJsonFile)

                                    if (jarFile.exists() && jarFile.parentFile != versionFolder) FileUtils.moveFile(jarFile, originalJarFile)
                                    if (jsonFile.exists() && jsonFile.parentFile != versionFolder) FileUtils.moveFile(jsonFile, originalJsonFile)
                                    FileUtils.deleteQuietly(folder)
                                    return
                                }
                            }
                        }
                    }
                }
            } else {
                //如果没有新增的文件夹，那么可能版本文件夹内已经存在一个同名的版本了
                //所以需要遍历现有的版本文件夹，并读取json文件，找到需要的版本，并复制过来使用
                VersionsManager.getVersions().forEach { version ->
                    if (version.isValid()) { //确保有效（即json文件必须存在）
                        val versionPath = version.getVersionPath()
                        val jarFile = File(versionPath, "${versionPath.name}.jar")
                        val jsonFile = File(versionPath, "${versionPath.name}.json")

                        version.getVersionInfo()?.loaderInfo?.let { info ->
                            if (info.isNotEmpty() && checkVersion(loaderInfo.name, loaderInfo.version, info[0])) {
                                val originalJarFile = File(versionFolder, "${versionFolder.name}.jar")
                                val originalJsonFile = File(versionFolder, "${versionFolder.name}.json")

                                if (jarFile.exists() && jarFile.parentFile != versionFolder) FileUtils.copyFile(jarFile, originalJarFile)
                                if (jsonFile.exists() && jsonFile.parentFile != versionFolder) FileUtils.copyFile(jsonFile, originalJsonFile)
                                return
                            }
                        }
                    }
                }
            }
        }

        /**
         * 检查版本json的ModLoader版本是否为传入的版本
         * @param loaderName 需要符合的ModLoader名称
         * @param loaderVersion 需要符合的版本
         */
        private fun checkVersion(loaderName: String, loaderVersion: String, jsonVersion: VersionInfo.LoaderInfo): Boolean {
            val normalizedLoaderName = if (loaderName.equals("NeoForge", true)) {
                //1.20.1的NeoForge比较特殊，因为其json文件里记录的名称实际上是Forge
                if (loaderVersion.startsWith("1.20.1")) "forge"
                else loaderName.lowercase()
            } else loaderName.lowercase()
            val normalizedLoaderVersion = when (normalizedLoaderName) {
                // OptiFine HD U J2 pre6 -> HD_U_J2_pre6
                "optifine" -> loaderVersion.removePrefix("OptiFine").trim().replace(" ", "_")
                // 1.21.3-53.0.23 -> 53.0.23
                // 1.7.10-10.13.4.1614-1.7.10 -> 10.13.4.1614
                "forge" -> {
                    val (_, version) = loaderVersion.split("-")
                    version
                }
                else -> loaderVersion
            }
            Logging.i("Check Version", "loaderName='$normalizedLoaderVersion', jsonName='${jsonVersion.name}', jsonVersion='${jsonVersion.version}'")
            return normalizedLoaderName.equals(jsonVersion.name, true) && jsonVersion.version.contains(normalizedLoaderVersion)
        }
    }
}