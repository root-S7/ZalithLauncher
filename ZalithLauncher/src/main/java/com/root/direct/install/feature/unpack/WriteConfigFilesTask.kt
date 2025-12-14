package com.root.direct.install.feature.unpack

import android.app.Application
import com.movtery.zalithlauncher.feature.unpack.AbstractUnpackTask
import com.movtery.zalithlauncher.utils.path.PathManager.Companion.DIR_FILE
import com.root.direct.install.utils.DIUtils.copyAssets
import com.root.direct.install.utils.path.*
import net.kdt.pojavlaunch.Tools.read
import java.io.File

class WriteConfigFilesTask(val application: Application, val component: DirectInstall) : AbstractUnpackTask() {
    private lateinit var versionFile: File
    private var internalVersion: Int = 0
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            versionFile = File(DIR_FILE.absolutePath + "/version")
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
        }catch(_: Exception){
            true
        }
    }

    override fun run() {
        listener?.onTaskStart()
        // 临时性的操作，目前配置文件写入规则暂不确定
        copyAssets(application, CONFIG_VERSION, DIR_FILE.absolutePath + "/version")
        copyAssets(application, LAUNCHER_CONFIG, DIR_FILE.absolutePath + "/launcher_settings.json")
        listener?.onTaskEnd()
    }
}