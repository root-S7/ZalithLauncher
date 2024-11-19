package com.movtery.zalithlauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.feature.log.Logging
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileWriter

class VersionConfig private constructor() : Parcelable {
    private var versionPath: File? = null
    private var javaDir: String = ""
    private var javaArgs: String = ""
    private var renderer: String = ""
    private var control: String = ""

    constructor(versionPath: File): this() {
        this.versionPath = versionPath
    }

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
            Logging.e("Save Version Config", "$this\n${Tools.printToString(e)}")
        }
    }

    @Throws(Throwable::class)
    fun saveWithThrowable() {
        Logging.i("Save Version Config", "Trying to save: $this")
        val zalithVersionPath = VersionsManager.getZalithVersionPath(versionPath!!)
        val configFile = File(zalithVersionPath, "ZalithVersion.cfg")
        if (!zalithVersionPath.exists()) zalithVersionPath.mkdirs()

        FileWriter(configFile, false).use {
            val json = Tools.GLOBAL_GSON.toJson(this)
            it.write(json)
        }
        Logging.i("Save Version Config", "Saved: $this")
    }

    fun delete() {
        runCatching {
            File(VersionsManager.getZalithVersionPath(versionPath!!), "ZalithVersion.cfg").let {
                if (it.exists()) FileUtils.deleteQuietly(it)
            }
        }
    }

    fun getVersionPath() = versionPath

    fun setVersionPath(versionPath: File) {
        this.versionPath = versionPath
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
    }
}