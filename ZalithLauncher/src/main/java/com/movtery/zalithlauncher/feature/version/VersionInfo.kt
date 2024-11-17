package com.movtery.zalithlauncher.feature.version

import com.movtery.zalithlauncher.feature.log.Logging
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileWriter

class VersionInfo(
    val minecraftVersion: String,
    val loaderInfo: Array<LoaderInfo>
) {
    class LoaderInfo(
        val name: String,
        val version: String
    )

    fun save(versionFolder: File) {
        runCatching {
            val zalithVersionPath = VersionsManager.getZalithVersionPath(versionFolder)
            val infoFile = File(zalithVersionPath, "VersionInfo.json")
            if (!zalithVersionPath.exists()) zalithVersionPath.mkdirs()

            FileWriter(infoFile, false).use {
                val json = Tools.GLOBAL_GSON.toJson(this)
                it.write(json)
            }
        }.getOrElse { e -> Logging.e("Save Version Info", Tools.printToString(e)) }
    }
}