package com.movtery.zalithlauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import net.kdt.pojavlaunch.Tools
import java.io.File

/**
 * Minecraft 版本，由版本名称进行区分
 * @param versionsFolder 版本所属的版本文件夹
 * @param versionPath 版本的路径
 * @param versionConfig 独立版本的配置，如果为空则代表其未开启版本隔离
 * @param isValid 版本的有效性
 */
class Version(
    private val versionsFolder: String,
    private val versionPath: String,
    private val versionConfig: VersionConfig?,
    private val isValid: Boolean
) :Parcelable {
    /**
     * @return 获取版本所属的版本文件夹
     */
    fun getVersionsFolder(): String {
        Logging.d("Version", "Versions Folder is $versionsFolder")
        return versionsFolder
    }

    fun getVersionPath():File {
        val path = File(versionPath)
        Logging.d("Version", "Version Path is ${path.absolutePath}")
        return path
    }

    fun getVersionName(): String {
        val name = getVersionPath().name
        Logging.d("Version", "Version Name is $name")
        return name
    }

    fun getVersionConfig(): VersionConfig? {
        Logging.d("Version", "Version Config is $versionConfig")
        return versionConfig
    }

    fun isValid(): Boolean {
        Logging.d("Version", "Version Valid: $isValid")
        return isValid
    }

    fun getGameDir(): File {
        versionConfig?.let {
            return getVersionPath()
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
            Logging.d("Version", "Version Info file path : ${infoFile.absolutePath}")
            Tools.GLOBAL_GSON.fromJson(Tools.read(infoFile), VersionInfo::class.java)
        }.getOrElse { null }
    }

    override fun toString(): String {
        return "Version{versionPath:'$versionPath', versionConfig:'$versionConfig'}"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStringList(listOf(versionsFolder, versionPath))
        dest.writeParcelable(versionConfig, flags)
        dest.writeInt(if (isValid) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<Version> {
        override fun createFromParcel(parcel: Parcel): Version {
            val stringList = ArrayList<String>()
            parcel.readStringList(stringList)
            val versionConfig = parcel.readParcelable<VersionConfig>(VersionConfig::class.java.classLoader)
            val isValid = parcel.readInt() != 0
            return Version(stringList[0], stringList[1], versionConfig, isValid)
        }

        override fun newArray(size: Int): Array<Version?> {
            return arrayOfNulls(size)
        }
    }
}