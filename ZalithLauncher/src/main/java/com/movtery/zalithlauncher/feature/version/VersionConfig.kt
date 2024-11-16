package com.movtery.zalithlauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.feature.log.Logging
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileWriter

class VersionConfig(private val versionPath: File) : Parcelable {
    private var javaDir: String = ""
    private var javaArgs: String = ""
    private var renderer: String = ""
    private var control: String = ""

    constructor(
        filePath: File,
        javaDir: String = "",
        javaArgs: String = "",
        renderer: String = "",
        control: String = ""
    ) : this(filePath) {
        this.javaDir = javaDir
        this.javaArgs = javaArgs
        this.renderer = renderer
        this.control = control
    }

    fun save() {
        runCatching {
            saveWithThrowable()
        }.getOrElse { e ->
            Logging.e("Save Version Config", Tools.printToString(e))
        }
    }

    @Throws(Throwable::class)
    fun saveWithThrowable() {
        val configFile = File(versionPath, "ZalithVersion.cfg")
        if (!versionPath.exists()) versionPath.mkdirs()

        FileWriter(configFile, false).use {
            val json = Tools.GLOBAL_GSON.toJson(this)
            it.write(json)
        }
    }

    fun delete() {
        runCatching {
            File(versionPath, "ZalithVersion.cfg").let {
                if (it.exists()) FileUtils.deleteQuietly(it)
            }
        }
    }

    fun getJavaDir() = javaDir

    fun setJavaDir(dir: String) { this.javaDir = dir }

    fun getJavaArgs() = javaArgs

    fun setJavaArgs(args: String) { this.javaArgs = args }

    fun getRenderer() = renderer

    fun setRenderer(renderer: String) { this.renderer = renderer }

    fun getControl() = control

    fun setControl(control: String) { this.control = control }

    override fun toString(): String {
        return "VersionConfig{versionPath='$versionPath', javaDir='$javaDir', javaArgs='$javaArgs', renderer='$renderer', control='$control'}"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(javaDir)
        dest.writeString(javaArgs)
        dest.writeString(renderer)
        dest.writeString(control)
    }

    companion object CREATOR : Parcelable.Creator<VersionConfig> {
        override fun createFromParcel(parcel: Parcel): VersionConfig {
            val javaDir = parcel.readString().orEmpty()
            val javaArgs = parcel.readString().orEmpty()
            val renderer = parcel.readString().orEmpty()
            val control = parcel.readString().orEmpty()
            val versionPath = File(parcel.readString().orEmpty())
            return VersionConfig(versionPath, javaDir, javaArgs, renderer, control)
        }

        override fun newArray(size: Int): Array<VersionConfig?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        fun getVersionPath(name: String): File {
            return File(ProfilePathHome.versionsHome, name)
        }
    }
}