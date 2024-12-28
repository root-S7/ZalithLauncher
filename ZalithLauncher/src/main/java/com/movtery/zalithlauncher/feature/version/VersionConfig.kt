package com.movtery.zalithlauncher.feature.version

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.stringutils.StringUtils.getStringNotNull
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileWriter

class VersionConfig(private var versionPath: File) : Parcelable {
    private var isolationType: IsolationType = IsolationType.FOLLOW_GLOBAL
    private var javaDir: String = ""
    private var javaArgs: String = ""
    private var renderer: String = ""
    private var control: String = ""
    private var customPath: String = ""

    constructor(
        filePath: File,
        isolationType: IsolationType = IsolationType.FOLLOW_GLOBAL,
        javaDir: String = "",
        javaArgs: String = "",
        renderer: String = "",
        control: String = "",
        customPath: String = ""
    ) : this(filePath) {
        this.isolationType = isolationType
        this.javaDir = javaDir
        this.javaArgs = javaArgs
        this.renderer = renderer
        this.control = control
        this.customPath = customPath
    }

    fun copy(): VersionConfig = VersionConfig(versionPath, isolationType,
        getStringNotNull(javaDir),
        getStringNotNull(javaArgs),
        getStringNotNull(renderer),
        getStringNotNull(control),
        getStringNotNull(customPath)
    )

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

    fun isIsolation(): Boolean = when(isolationType) {
        IsolationType.FOLLOW_GLOBAL -> AllSettings.versionIsolation.getValue()
        IsolationType.ENABLE -> true
        IsolationType.DISABLE -> false
    }

    fun getIsolationType() = isolationType

    fun setIsolationType(isolationType: IsolationType) {
        this.isolationType = isolationType
    }

    fun getJavaDir(): String = getStringNotNull(javaDir)

    fun setJavaDir(dir: String) { this.javaDir = dir }

    fun getJavaArgs(): String = getStringNotNull(javaArgs)

    fun setJavaArgs(args: String) { this.javaArgs = args }

    fun getRenderer(): String = getStringNotNull(renderer)

    fun setRenderer(renderer: String) { this.renderer = renderer }

    fun getControl(): String = getStringNotNull(control)

    fun setControl(control: String) { this.control = control }

    fun getCustomPath(): String = getStringNotNull(customPath)

    fun setCustomPath(customPath: String) { this.customPath = customPath }

    fun checkDifferent(otherConfig: VersionConfig): Boolean {
        return !(this.isolationType == otherConfig.isolationType &&
                this.javaDir == otherConfig.javaDir &&
                this.javaArgs == otherConfig.javaArgs &&
                this.renderer == otherConfig.renderer &&
                this.control == otherConfig.control &&
                this.customPath == otherConfig.customPath)
    }

    override fun toString(): String {
        return "VersionConfig{" +
                "isolationType=$isolationType, " +
                "versionPath='$versionPath', " +
                "javaDir='${getStringNotNull(javaDir)}', " +
                "javaArgs='${getStringNotNull(javaArgs)}', " +
                "renderer='${getStringNotNull(renderer)}', " +
                "control='${getStringNotNull(control)}', " +
                "customPath='${getStringNotNull(customPath)}'}"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(versionPath.absolutePath)
        dest.writeInt(isolationType.ordinal)
        dest.writeString(getStringNotNull(javaDir))
        dest.writeString(getStringNotNull(javaArgs))
        dest.writeString(getStringNotNull(renderer))
        dest.writeString(getStringNotNull(control))
        dest.writeString(getStringNotNull(customPath))
    }

    companion object CREATOR : Parcelable.Creator<VersionConfig> {
        override fun createFromParcel(parcel: Parcel): VersionConfig {
            val versionPath = File(parcel.readString().orEmpty())
            val isolationType = IsolationType.entries.getOrNull(parcel.readInt()) ?: IsolationType.FOLLOW_GLOBAL
            val javaDir = parcel.readString().orEmpty()
            val javaArgs = parcel.readString().orEmpty()
            val renderer = parcel.readString().orEmpty()
            val control = parcel.readString().orEmpty()
            val customPath = parcel.readString().orEmpty()
            return VersionConfig(versionPath, isolationType, javaDir, javaArgs, renderer, control, customPath)
        }

        override fun newArray(size: Int): Array<VersionConfig?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        fun createIsolation(versionPath: File): VersionConfig {
            val config = VersionConfig(versionPath)
            config.setIsolationType(IsolationType.ENABLE)
            return config
        }

        @JvmStatic
        fun getIsolationString(context: Context, type: IsolationType): String = when (type) {
            IsolationType.FOLLOW_GLOBAL -> context.getString(R.string.version_manager_isolation_type_follow_global)
            IsolationType.ENABLE -> context.getString(R.string.generic_open)
            IsolationType.DISABLE -> context.getString(R.string.generic_close)
        }
    }

    enum class IsolationType {
        FOLLOW_GLOBAL, ENABLE, DISABLE
    }
}