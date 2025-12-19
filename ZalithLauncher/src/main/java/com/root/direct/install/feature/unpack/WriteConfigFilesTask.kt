package com.root.direct.install.feature.unpack

import android.app.Application
import com.movtery.zalithlauncher.feature.unpack.AbstractUnpackTask
import com.movtery.zalithlauncher.utils.path.PathManager.Companion.DIR_FILE
import com.root.direct.install.feature.check.FileFormat
import com.root.direct.install.utils.DIUtils.copyAssets
import com.root.direct.install.utils.path.*
import net.kdt.pojavlaunch.Tools.read
import java.io.File

class WriteConfigFilesTask(val application: Application, val component: DirectInstall) : AbstractUnpackTask() {
    private var internalVersion: Int = 0
    private val fileFormat: FileFormat = FileFormat()

    init {
        runCatching {
            internalVersion = read(application.assets.open(component.component)).trim().toInt()
        }.getOrElse {
            internalVersion = -1
        }
    }

    override fun isNeedUnpack(): Boolean {
        val file = File(DIR_FILE.absolutePath + "/version")

        return !file.exists() || try {
            val release = file.readText().trim().toIntOrNull() ?: 0
            internalVersion > release
        }catch(_: Exception){
            true
        }
    }

    override fun run() {
        listener?.onTaskStart()
        fileFormat.checkFiles
            .filter { it.outPath != null }
            .forEach { file -> copyAssets(application, file.assPath, file.outPath) }
        listener?.onTaskEnd()
    }
}