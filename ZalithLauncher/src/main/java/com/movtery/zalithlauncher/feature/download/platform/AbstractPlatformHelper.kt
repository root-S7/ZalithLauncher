package com.movtery.zalithlauncher.feature.download.platform

import android.content.Context
import android.widget.EditText
import com.kdt.mcgui.ProgressLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.ContextExecutor
import com.movtery.zalithlauncher.event.sticky.InstallingVersionEvent
import com.movtery.zalithlauncher.feature.download.Filters
import com.movtery.zalithlauncher.feature.download.enums.Classify
import com.movtery.zalithlauncher.feature.download.item.InfoItem
import com.movtery.zalithlauncher.feature.download.item.ModLoaderWrapper
import com.movtery.zalithlauncher.feature.download.item.ScreenshotItem
import com.movtery.zalithlauncher.feature.download.item.SearchResult
import com.movtery.zalithlauncher.feature.download.item.VersionItem
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.mod.modpack.install.ModPackUtils
import com.movtery.zalithlauncher.feature.version.VersionConfig
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.ui.dialog.EditTextDialog
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler
import net.kdt.pojavlaunch.utils.DownloadUtils
import org.greenrobot.eventbus.EventBus
import java.io.File

abstract class AbstractPlatformHelper(val api: ApiHandler) {
    @Throws(Throwable::class)
    fun search(classify: Classify, filters: Filters, lastResult: SearchResult): SearchResult? {
        return when (classify) {
            Classify.ALL -> throw IllegalArgumentException("Cannot be the enum value ${Classify.ALL}")
            Classify.MOD -> searchMod(filters, lastResult)
            Classify.MODPACK -> searchModPack(filters, lastResult)
            Classify.RESOURCE_PACK -> searchResourcePack(filters, lastResult)
            Classify.WORLD -> searchWorld(filters, lastResult)
            Classify.SHADER_PACK -> searchShaderPack(filters, lastResult)
        }
    }

    @Throws(Throwable::class)
    fun getVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return when (infoItem.classify) {
            Classify.ALL -> throw IllegalArgumentException("Cannot be the enum value ${Classify.ALL}")
            Classify.MOD -> getModVersions(infoItem, force)
            Classify.MODPACK -> getModPackVersions(infoItem, force)
            Classify.RESOURCE_PACK -> getResourcePackVersions(infoItem, force)
            Classify.WORLD -> getWorldVersions(infoItem, force)
            Classify.SHADER_PACK -> getShaderPackVersions(infoItem, force)
        }
    }

    @Throws(Throwable::class)
    fun install(context: Context, infoItem: InfoItem, version: VersionItem, isTaskRunning: () -> Boolean) {
        when (infoItem.classify) {
            Classify.ALL -> throw IllegalArgumentException("Cannot be the enum value ${Classify.ALL}")
            Classify.MOD -> {
                customPath(context, version, getModsPath(), taskRunning = isTaskRunning, install = { targetPath ->
                    installMod(infoItem, version, targetPath)
                })
            }
            Classify.MODPACK -> {
                EditTextDialog.Builder(context)
                    .setTitle(R.string.version_install_new)
                    .setEditText(infoItem.title)
                    .setConfirmListener { editText: EditText ->
                        val customName = editText.text.toString()
                        if (customName.contains("/")) {
                            editText.error = context.getString(R.string.generic_input_invalid_character, "/")
                            return@setConfirmListener false
                        }

                        if (VersionsManager.isVersionExists(customName, true)) {
                            editText.error = context.getString(R.string.version_install_exists)
                            return@setConfirmListener false
                        }

                        if (!isTaskRunning()) {
                            ProgressLayout.setProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.generic_waiting)
                            val installingVersionEvent = InstallingVersionEvent()
                            Task.runTask {
                                EventBus.getDefault().postSticky(installingVersionEvent)
                                val modloader = installModPack(version, customName) ?: return@runTask null

                                val versionPath = VersionsManager.getVersionPath(customName)
                                VersionConfig(versionPath).save()

                                infoItem.iconUrl?.let { DownloadUtils.downloadFile(it, VersionsManager.getVersionIconFile(customName)) }

                                modloader.getDownloadTask()?.let { downloadTask ->
                                    Logging.i("Install Version", "Installing ModLoader: ${modloader.modLoaderVersion}")
                                    downloadTask.run(customName)?.let { file ->
                                        return@runTask Pair(modloader, file)
                                    }
                                }

                                return@runTask null
                            }.ended(TaskExecutors.getAndroidUI()) ended@{ filePair ->
                                filePair?.let {
                                    ModPackUtils.startModLoaderInstall(it.first, ContextExecutor.getActivity(), it.second, customName)
                                    return@ended
                                }
                            }.onThrowable { e ->
                                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e)
                            }.finallyTask {
                                EventBus.getDefault().removeStickyEvent(installingVersionEvent)
                            }.execute()
                        }

                        true
                    }.buildDialog()
            }
            Classify.RESOURCE_PACK -> {
                customPath(context, version, getResourcePackPath(), taskRunning = isTaskRunning, install = { targetPath ->
                    installResourcePack(infoItem, version, targetPath)
                })
            }
            Classify.WORLD -> {
                customPath(context, version, getWorldPath(), taskRunning = isTaskRunning, install = { targetPath ->
                    installWorld(infoItem, version, targetPath)
                })
            }
            Classify.SHADER_PACK -> {
                customPath(context, version, getShaderPackPath(), taskRunning = isTaskRunning, install = { targetPath ->
                    installShaderPack(infoItem, version, targetPath)
                })
            }
        }
    }

    private fun customPath(context: Context, version: VersionItem, targetPath: File, taskRunning: () -> Boolean, install: (File) -> Unit) {
        val file = File(version.fileName)

        EditTextDialog.Builder(context)
            .setTitle(R.string.download_install_custom_name)
            .setEditText(file.nameWithoutExtension)
            .setConfirmListener { editText: EditText ->
                val string = editText.text.toString()
                if (string.contains("/")) {
                    editText.error = context.getString(R.string.generic_input_invalid_character, "/")
                    return@setConfirmListener false
                }

                val installFile = File(targetPath, "${string}.${file.extension}")

                if (installFile.exists()) {
                    editText.error = context.getString(R.string.import_control_invalid_name)
                    return@setConfirmListener false
                }

                if (!taskRunning()) {
                    install(installFile)
                    ProgressLayout.setProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.generic_waiting)
                }
                true
            }.buildDialog()
    }

    abstract fun copy(): AbstractPlatformHelper
    abstract fun getWebUrl(infoItem: InfoItem): String?
    abstract fun getScreenshots(projectId: String): List<ScreenshotItem>

    abstract fun searchMod(filters: Filters, lastResult: SearchResult): SearchResult?
    abstract fun searchModPack(filters: Filters, lastResult: SearchResult): SearchResult?
    abstract fun searchResourcePack(filters: Filters, lastResult: SearchResult): SearchResult?
    abstract fun searchWorld(filters: Filters, lastResult: SearchResult): SearchResult?
    abstract fun searchShaderPack(filters: Filters, lastResult: SearchResult): SearchResult?

    abstract fun getModVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?
    abstract fun getModPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?
    abstract fun getResourcePackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?
    abstract fun getWorldVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?
    abstract fun getShaderPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?

    abstract fun installMod(infoItem: InfoItem, version: VersionItem, targetPath: File?)
    abstract fun installModPack(version: VersionItem, customName: String): ModLoaderWrapper?
    abstract fun installResourcePack(infoItem: InfoItem, version: VersionItem, targetPath: File?)
    abstract fun installWorld(infoItem: InfoItem, version: VersionItem, targetPath: File?)
    abstract fun installShaderPack(infoItem: InfoItem, version: VersionItem, targetPath: File?)

    companion object {
        @JvmStatic
        fun getDir(): File {
            return VersionsManager.getCurrentVersion()?.getGameDir() ?: throw RuntimeException("There is no installed version")
        }

        @JvmStatic
        fun getModsPath() = File(getDir(), "/mods")

        @JvmStatic
        fun getResourcePackPath() = File(getDir(), "/resourcepacks")

        @JvmStatic
        fun getWorldPath() = File(getDir(), "/saves")

        @JvmStatic
        fun getShaderPackPath() = File(getDir(), "/shaderpacks")
    }
}