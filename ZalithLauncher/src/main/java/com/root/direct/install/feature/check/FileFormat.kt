package com.root.direct.install.feature.check

import android.os.Looper.getMainLooper
import android.os.Looper.myLooper
import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.utils.path.PathManager.Companion.DIR_FILE
import com.movtery.zalithlauncher.utils.path.PathManager.Companion.FILE_CTRLDEF_FILE
import com.movtery.zalithlauncher.utils.path.PathManager.Companion.FILE_SETTINGS
import com.root.direct.install.feature.check.rule.FileCheckRule
import com.root.direct.install.feature.file.FileInfo
import com.root.direct.install.feature.file.FileType.Companion.fromExtension
import com.root.direct.install.utils.DIUtils.loadFromAssets
import com.root.direct.install.utils.DIUtils.openAssets
import com.root.direct.install.utils.path.*
import net.kdt.pojavlaunch.Tools.GLOBAL_GSON
import java.io.FileNotFoundException

class FileFormat(vararg extraNeedFile: String) {
    val checkFiles: MutableSet<FileInfo> = mutableSetOf(
        FileInfo(LANG),
        FileInfo(RULES),
        FileInfo(SETTINGS),
        FileInfo(GAME_VERSION),
        FileInfo(CUSTOM_RENDERER),
        FileInfo(AUTH_SERVER, rule = authServersCheck()),
        FileInfo(LAUNCHER_CONFIG, FILE_SETTINGS.absolutePath),
        FileInfo(CONFIG_VERSION, DIR_FILE.absolutePath + "/version"),
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

    private fun authServersCheck(): FileCheckRule = FileCheckRule { assPath ->
        val rules = arrayOf(
            Regex("^[0-9a-fA-F]{32}$"),
            Regex("^(?=.{1,253}\$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*\\.[A-Za-z]{2,}$"),
            Regex("^https?://.+", RegexOption.IGNORE_CASE)
        )

        runCatching {
            openAssets(null, assPath).bufferedReader().use {
                GLOBAL_GSON.fromJson<LinkedHashSet<String>>(
                    it, object : TypeToken<LinkedHashSet<String>>() {}.type
                )!!.forEach { v ->
                    require(v.isNotBlank() && rules.any { r -> r.matches(v.trim()) })
                }
            }
            true
        }.getOrElse { ex ->
            if(ex is FileNotFoundException) throw ex
            false
        }
    }
}