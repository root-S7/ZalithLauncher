package com.movtery.zalithlauncher.feature.version

import android.content.Intent
import com.google.gson.JsonParser
import com.kdt.mcgui.ProgressLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import net.kdt.pojavlaunch.JavaGUILauncherActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class InstallArgsUtils(private val mcVersion: String, private val loaderVersion: String) {
    fun setFabric(intent: Intent, jarFile: File, customName: String) {
        val args = "-DprofileName=\"$customName\" -javaagent:${PathAndUrlManager.DIR_DATA}/installer/MioFabricAgent.jar" +
                " -jar ${jarFile.absolutePath} client -mcversion \"$mcVersion\" -loader \"$loaderVersion\" -dir \"${ProfilePathHome.gameHome}\""
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    fun setQuilt(intent: Intent, jarFile: File) {
        val args = "-jar ${jarFile.absolutePath} install client \"$mcVersion\" \"$loaderVersion\" --install-dir=\"${ProfilePathHome.gameHome}\""
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    @Throws(Throwable::class)
    fun setForge(intent: Intent, jarFile: File, customName: String) {
        forgeLikeCustomVersionName(jarFile, customName)

        val args = "-javaagent:${PathAndUrlManager.DIR_DATA}/installer/forge_installer.jar=\"$loaderVersion\" -jar ${jarFile.absolutePath}"
        intent.putExtra("javaArgs", args)
    }

    @Throws(Throwable::class)
    fun setNeoForge(intent: Intent, jarFile: File, customName: String) {
        forgeLikeCustomVersionName(jarFile, customName)

        val args = "-jar ${jarFile.absolutePath} --installClient \"${ProfilePathHome.gameHome}\""
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    fun setOptiFine(intent: Intent, jarFile: File) {
        val args = "-javaagent:${PathAndUrlManager.DIR_DATA}/installer/forge_installer.jar=OFNPS -jar ${jarFile.absolutePath}"
        intent.putExtra("javaArgs", args)
    }

    /**
     * 将Forge或NeoForge安装器中的install_profile.json 文件中的 version 的键，修改为 customName
     * Forge安装器会根据 version 这个值，来生成对应的版本文件夹
     * 这样做是为了自定义版本 json 的安装位置
     */
    @Throws(Throwable::class)
    private fun forgeLikeCustomVersionName(jarFile: File, customName: String) {
        try {
            ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.mod_forge_custom_version)

            val zipFile = ZipFile(jarFile)
            val tempJarFile = File(jarFile.parentFile, "${jarFile.nameWithoutExtension}_temp.jar")
            if (tempJarFile.exists()) FileUtils.deleteQuietly(tempJarFile)

            val entry: ZipEntry = zipFile.entries().asSequence()
                .firstOrNull { it.name == "install_profile.json" }
                ?: throw IOException("File \"install_profile.json\" not found in the Installer")

            //解压出install_profile.json
            val profileJson = File(jarFile.parentFile, "install_profile.json")
            profileJson.parentFile?.mkdirs()
            if (profileJson.exists()) FileUtils.deleteQuietly(profileJson)

            zipFile.getInputStream(entry).use { inputStream ->
                FileOutputStream(profileJson).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 25, R.string.mod_forge_custom_version)

            val jsonObject = JsonParser.parseString(Tools.read(profileJson)).asJsonObject
            //通过检查是否有spec这个键，来判断是否为新版本的Installer
            if (jsonObject.has("spec")) { //新版安装器
                if (!jsonObject.has("version")) throw IOException("Unable to find version key!")

                //install_profile.json中，把version这个值改为customName，也就完成自定义版本名的效果
                jsonObject.addProperty("version", customName)
            } else { //旧版安装器
                if (!jsonObject.has("install")) throw IOException("Unable to find install key!")
                val install = jsonObject.get("install").asJsonObject
                if (!install.has("target")) throw IOException("Unable to find install-target key!")

                //把target这个值改为customName，也就完成旧版自定义版本名的效果
                install.addProperty("target", customName)
                jsonObject.add("install", install)
            }

            ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 50, R.string.mod_forge_custom_version)

            FileWriter(profileJson).use {
                it.write(Tools.GLOBAL_GSON.toJson(jsonObject))
            }

            ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 75, R.string.mod_forge_custom_version)

            ZipOutputStream(FileOutputStream(tempJarFile)).use { zos ->
                zipFile.entries().asSequence().forEach { originalEntry ->
                    zos.putNextEntry(ZipEntry(originalEntry.name))
                    if (originalEntry.name == "install_profile.json") {
                        FileInputStream(profileJson).use { fis -> fis.copyTo(zos) }
                    } else {
                        //写入原始文件
                        zipFile.getInputStream(originalEntry).use { inputStream ->
                            inputStream.copyTo(zos)
                        }
                    }
                    zos.closeEntry()
                }
            }

            ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 100, R.string.mod_forge_custom_version)

            if (!jarFile.delete()) throw IOException("Failed to delete original Installer file!")
            if (!tempJarFile.renameTo(jarFile)) throw IOException("Failed to rename temp Installer file to original!")

            FileUtils.deleteQuietly(tempJarFile)
            FileUtils.deleteQuietly(profileJson)
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
        }
    }
}