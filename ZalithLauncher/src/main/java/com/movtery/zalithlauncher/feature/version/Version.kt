package com.movtery.zalithlauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Tools
import java.io.File

/**
 * Minecraft 版本，由版本名称进行区分
 * @param versionsFolder 版本所属的版本文件夹
 * @param versionPath 版本的路径
 * @param versionConfig 独立版本的配置
 * @param isValid 版本的有效性
 */
class Version(
    private val versionsFolder: String,
    private val versionPath: String,
    private val versionConfig: VersionConfig,
    private val isValid: Boolean
) :Parcelable {
    /**
     * @return 获取版本所属的版本文件夹
     */
    fun getVersionsFolder(): String = versionsFolder

    /**
     * @return 获取版本文件夹
     */
    fun getVersionPath(): File = File(versionPath)

    /**
     * @return 获取版本名称
     */
    fun getVersionName(): String = getVersionPath().name

    /**
     * @return 获取版本隔离配置
     */
    fun getVersionConfig() = versionConfig

    /**
     * @return 版本的有效性
     */
    fun isValid() = isValid

    /**
     * @return 是否开启了版本隔离
     */
    fun isIsolation() = versionConfig.isIsolation()

    /**
     * @return 获取版本的游戏文件夹路径（若开启了版本隔离，则路径为版本文件夹）
     */
    fun getGameDir(): File {
        return if (versionConfig.isIsolation()) versionConfig.getVersionPath()
        //未开启版本隔离可以使用自定义路径，如果自定义路径为空（则为未设置），那么返回默认游戏路径（.minecraft/）
        else if (versionConfig.getCustomPath().isNotEmpty()) File(versionConfig.getCustomPath())
        else File(ProfilePathHome.gameHome)
    }

    private fun String.getValueOrDefault(default: String): String = this.takeIf { it.isNotEmpty() } ?: default

    fun getRenderer(): String = versionConfig.getRenderer().getValueOrDefault(AllSettings.renderer.getValue())

    fun getJavaDir(): String = versionConfig.getJavaDir().getValueOrDefault(AllSettings.defaultRuntime.getValue())

    fun getJavaArgs(): String = versionConfig.getJavaArgs().getValueOrDefault(AllSettings.javaArgs.getValue())

    fun getControl(): String {
        val configControl = versionConfig.getControl().removeSuffix("./")
        return if (configControl.isNotEmpty()) File(PathManager.DIR_CTRLMAP_PATH, configControl).absolutePath
        else File(AllSettings.defaultCtrl.getValue()).absolutePath
    }

    fun getVersionInfo(): VersionInfo? {
        return runCatching {
            val infoFile = File(VersionsManager.getZalithVersionPath(this), "VersionInfo.json")
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
            val versionConfig = parcel.readParcelable<VersionConfig>(VersionConfig::class.java.classLoader)!!
            val isValid = parcel.readInt() != 0
            return Version(stringList[0], stringList[1], versionConfig, isValid)
        }

        override fun newArray(size: Int): Array<Version?> {
            return arrayOfNulls(size)
        }
    }
}