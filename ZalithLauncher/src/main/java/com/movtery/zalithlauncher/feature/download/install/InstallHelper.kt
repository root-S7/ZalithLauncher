package com.movtery.zalithlauncher.feature.download.install

import com.kdt.mcgui.ProgressLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.download.item.ModLoaderWrapper
import com.movtery.zalithlauncher.feature.download.item.VersionItem
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper
import net.kdt.pojavlaunch.utils.DownloadUtils
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

class InstallHelper {
    companion object {
        @Throws(Throwable::class)
        fun downloadFile(version: VersionItem, targetFile: File?) {
            downloadFile(version, targetFile, null)
        }

        @Throws(Throwable::class)
        fun downloadFile(
            version: VersionItem,
            targetFile: File?,
            listener: OnFileDownloadedListener?
        ) {
            Task.runTask {
                try {
                    val downloadBuffer = ByteArray(8192)
                    DownloadUtils.ensureSha1<Void?>(targetFile, version.fileHash) {
                        Logging.i(
                            "CurseForgeModPackInstallHelper",
                            "Download Url: ${version.fileUrl}"
                        )
                        DownloadUtils.downloadFileMonitored(
                            version.fileUrl, targetFile, downloadBuffer,
                            DownloaderProgressWrapper(
                                R.string.download_install_download_file,
                                ProgressLayout.INSTALL_RESOURCE
                            )
                        )
                        null
                    }
                } finally {
                    targetFile?.let { listener?.onEnded(it) }
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
                }
            }.execute()
        }

        @Throws(IOException::class)
        fun installModPack(
            version: VersionItem,
            customName: String,
            installFunction: ModPackInstallFunction
        ): ModLoaderWrapper? {
            val modpackFile = File(
                PathManager.DIR_CACHE, "$customName.cf"
            ) // Cache File

            val modLoaderInfo: ModLoaderWrapper?
            try {
                val downloadBuffer = ByteArray(8192)
                DownloadUtils.ensureSha1<Void?>(
                    modpackFile, version.fileHash
                ) {
                    Logging.i("InstallHelper", "Download Url: ${version.fileUrl}")
                    DownloadUtils.downloadFileMonitored(
                        version.fileUrl, modpackFile, downloadBuffer,
                        DownloaderProgressWrapper(
                            R.string.modpack_download_downloading_metadata,
                            ProgressLayout.INSTALL_RESOURCE
                        )
                    )
                    null
                }

                // Install the modpack
                modLoaderInfo = installFunction.install(modpackFile, VersionsManager.getVersionPath(customName))
            } finally {
                FileUtils.deleteQuietly(modpackFile)
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
            }
            modLoaderInfo ?: return null
            Logging.i("InstallHelper", "ModLoader is ${modLoaderInfo.nameById}")

            return modLoaderInfo
        }
    }
}