package com.movtery.zalithlauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.setting.AllSettings
import java.io.File

/**
 * Minecraft 版本，由版本名称进行区分
 * @param versionName 版本的名称
 * @param versionConfig 独立版本的配置，如果为空则代表其未开启版本隔离
 */
class Version(
    private val versionName: String,
    private val versionConfig: VersionConfig?
) :Parcelable {
    fun getVersionName() = versionName

    fun getVersionConfig() = versionConfig

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

    fun getControl(): String {
        versionConfig?.let {
            return it.getControl()
        }
        return AllSettings.defaultCtrl!!
    }

    override fun toString(): String {
        return "Version{versionName:'$versionName', versionConfig:'$versionConfig'}"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(versionName)
        dest.writeParcelable(versionConfig, flags)
    }

    companion object CREATOR : Parcelable.Creator<Version> {
        override fun createFromParcel(parcel: Parcel): Version {
            val versionName = parcel.readString().orEmpty()
            val versionConfig = parcel.readParcelable<VersionConfig>(VersionConfig::class.java.classLoader)
            return Version(versionName, versionConfig)
        }

        override fun newArray(size: Int): Array<Version?> {
            return arrayOfNulls(size)
        }
    }
}