package com.root.direct.install.feature.check

import android.os.Looper.getMainLooper
import android.os.Looper.myLooper
import com.movtery.zalithlauncher.utils.path.PathManager.Companion.FILE_CTRLDEF_FILE
import com.movtery.zalithlauncher.utils.path.PathManager.Companion.FILE_SETTINGS
import com.root.direct.install.feature.check.rule.FileCheckRule
import com.root.direct.install.feature.file.FileInfo
import com.root.direct.install.feature.file.FileType.Companion.fromExtension
import com.root.direct.install.utils.DIUtils.loadFromAssets
import com.root.direct.install.utils.path.*
import java.io.FileNotFoundException

class FileFormat(vararg extraNeedFile: String) {
    private val checkFiles: MutableSet<FileInfo> = mutableSetOf(
        FileInfo(LANG),
        FileInfo(RULES),
        FileInfo(SETTINGS),
        FileInfo(AUTH_SERVER),
        FileInfo(GAME_VERSION),
        FileInfo(CONFIG_VERSION),
        FileInfo(CUSTOM_RENDERER),
        FileInfo(LAUNCHER_CONFIG, FILE_SETTINGS.absolutePath),
        FileInfo(DEFAULT_CONTROL, FILE_CTRLDEF_FILE, controlCheck())
    )

    init {
        extraNeedFile
            .filter { it.isNotBlank() }
            .mapTo(checkFiles) { FileInfo(it.trim(), null, null) }
    }

    /**
     * 检测所有文件，如果执行到某个文件检测结果为false则抛出文件不合法异常
     * 必须在非主线程上执行，因为某些文件检测设计到网络请求
    **/
    fun checkFiles(): Boolean = checkFiles.filter {
        it.assPath.isNotBlank()
    }.also {
        if(myLooper() == getMainLooper()) throw IllegalStateException("checkFiles()方法不可以在主线程内执行！")
    }.all { it ->
        val path = it.assPath.trim()
        val fileType = fromExtension(path.substringAfterLast('.', ""))
        it.getCheckRule(fileType).check(path).also { ok ->
            if(!ok) throw IllegalStateException("文件『$path』未通过校验，请重新制作直装包！")
        }
    }

    /**
     * 仅限于APK内部按键文件的检测规则
    **/
    private fun controlCheck(): FileCheckRule = FileCheckRule { assPath ->
        runCatching {
            loadFromAssets(assPath) != null
        }.getOrElse { ex ->
            if(ex is FileNotFoundException) throw ex
            false
        }
    }
}