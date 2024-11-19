package com.movtery.zalithlauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import net.kdt.pojavlaunch.Tools
import java.io.File

/**
 * Minecraft 版本，由版本名称进行区分
 * @param versionName 版本的名称
 * @param versionConfig 独立版本的配置，如果为空则代表其未开启版本隔离
 * @param isValid 版本的有效性
 */
class Version(
    private val versionName: String,
    private val versionConfig: VersionConfig?,
    private val isValid: Boolean
) :Parcelable {
    fun getVersionName() = versionName

    fun getVersionConfig() = versionConfig

    fun isValid() = isValid

    fun getGameDir(): File {
        versionConfig?.let {
            return VersionsManager.getVersionPath(this)
        }
        return File(ProfilePathHome.gameHome)
    }

    fun getRenderer(): String {
        versionConfig?.let {
            return it.getRenderer()
        }
        return AllSettings.renderer!!
    }

    fun getJavaDir(): String {
        versionConfig?.let {
            return it.getJavaDir()
        }
        return AllSettings.defaultRuntime!!
    }

    fun getJavaArgs(): String {
        versionConfig?.let {
            return it.getJavaArgs()
        }
        return AllSettings.javaArgs!!
    }

    fun getControl(): String? {
        val configControl = versionConfig?.getControl()?.removeSuffix("./") ?: ""
        return if (configControl.isNotEmpty()) File(PathAndUrlManager.DIR_CTRLMAP_PATH, configControl).absolutePath
        else File(AllSettings.defaultCtrl!!).absolutePath
    }

    fun getVersionInfo(): VersionInfo? {
        return runCatching {
            val infoFile = File(VersionsManager.getZalithVersionPath(this), "VersionInfo.json")
            Tools.GLOBAL_GSON.fromJson(Tools.read(infoFile), VersionInfo::class.java)
        }.getOrElse { null }
    }

    override fun toString(): String {
        return "Version{versionName:'$versionName', versionConfig:'$versionConfig'}"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(versionName)
        dest.writeParcelable(versionConfig, flags)
        dest.writeInt(if (isValid) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<Version> {
        override fun createFromParcel(parcel: Parcel): Version {
            val versionName = parcel.readString().orEmpty()
            val versionConfig = parcel.readParcelable<VersionConfig>(VersionConfig::class.java.classLoader)
            val isValid = parcel.readInt() != 0
            return Version(versionName, versionConfig, isValid)
        }

        override fun newArray(size: Int): Array<Version?> {
            return arrayOfNulls(size)
        }
    }
}