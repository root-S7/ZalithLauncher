package com.root.direct.install.feature.unpack

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import com.movtery.zalithlauncher.feature.unpack.AbstractUnpackTask
import com.movtery.zalithlauncher.utils.path.PathManager
import com.root.direct.install.utils.DIUtils.copyAssets
import com.root.direct.install.utils.path.*
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools.read
import org.apache.commons.io.FileUtils.deleteDirectory
import java.io.File

class UnpackGameDataTask(val application: Application, val component: DirectInstall, val privateDirectory: Boolean = false) : AbstractUnpackTask() {
    private lateinit var rootDir: String
    private lateinit var versionFile: File
    private var internalVersion: Int = 0
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            rootDir = if(privateDirectory) PathManager.DIR_DATA else PathManager.DIR_GAME_HOME
            versionFile = File("$rootDir/${component.component}/version")
            internalVersion = read(application.assets.open(GAME_VERSION)).trim().toInt()
        }.getOrElse {
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun isNeedUnpack(): Boolean {
        if(isCheckFailed) return false

        return !versionFile.exists() || try {
            val release = versionFile.readText().trim().toIntOrNull() ?: 0
            internalVersion > release
        }catch(_: Exception) {
            true
        }
    }

    override fun run() {
        listener?.onTaskStart()
        val dest = File(rootDir, "/${component.component}")
        deleteDirectory(dest)
        copyAssets(application, "components/${component.component}", "$rootDir/${component.component}")
        listener?.onTaskEnd()
    }
}