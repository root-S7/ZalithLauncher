package com.root.direct.install.feature.unpack

import android.app.Application
import com.movtery.zalithlauncher.feature.unpack.AbstractUnpackTask
import com.movtery.zalithlauncher.utils.path.PathManager.Companion.DIR_FILE
import net.kdt.pojavlaunch.Tools.read
import java.io.File

class UnpackAuthServersTask(val application: Application, val component: DirectInstall) : AbstractUnpackTask() {

    private var internalVersion: Int = 0

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

        listener?.onTaskEnd()
    }
}