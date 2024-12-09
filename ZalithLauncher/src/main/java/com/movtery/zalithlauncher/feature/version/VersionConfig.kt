package com.movtery.zalithlauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.feature.log.Logging
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileWriter

class VersionConfig(private var versionPath: File) : Parcelable {
    private var isolation: Boolean = false
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

    fun copy(): VersionConfig = VersionConfig(versionPath, javaDir, javaArgs, renderer, control)

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
        val zalithVersionPath = VersionsManager.getZalithVersionPath(versionPath)
        val configFile = File(zalithVersionPath, "VersionConfig.json")
        if (!zalithVersionPath.exists()) zalithVersionPath.mkdirs()

        FileWriter(configFile, false).use {
            val json = Tools.GLOBAL_GSON.toJson(this)
            it.write(json)
        }
        Logging.i("Save Version Config", "Saved: $this")
    }

    fun getVersionPath() = versionPath

    fun setVersionPath(versionPath: File) {
        this.versionPath = versionPath
    }

    fun isIsolation() = isolation

    fun setIsolation(isolation: Boolean) {
        this.isolation = isolation
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
        dest.writeInt(if (isolation) 1 else 0)
        dest.writeString(versionPath.absolutePath)
        dest.writeString(javaDir)
        dest.writeString(javaArgs)
        dest.writeString(renderer)
        dest.writeString(control)
    }

    companion object CREATOR : Parcelable.Creator<VersionConfig> {
        override fun createFromParcel(parcel: Parcel): VersionConfig {
            val isolation = parcel.readInt() > 0
            val versionPath = File(parcel.readString().orEmpty())
            val javaDir = parcel.readString().orEmpty()
            val javaArgs = parcel.readString().orEmpty()
            val renderer = parcel.readString().orEmpty()
            val control = parcel.readString().orEmpty()
            return VersionConfig(versionPath, javaDir, javaArgs, renderer, control).apply {
                setIsolation(isolation)
            }
        }

        override fun newArray(size: Int): Array<VersionConfig?> {
            return arrayOfNulls(size)
        }

        fun createIsolation(versionPath: File): VersionConfig {
            val config = VersionConfig(versionPath)
            config.setIsolation(true)
            return config
        }
    }
}