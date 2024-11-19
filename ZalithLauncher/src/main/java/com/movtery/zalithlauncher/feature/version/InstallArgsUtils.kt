package com.movtery.zalithlauncher.feature.version

import android.content.Intent
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import net.kdt.pojavlaunch.JavaGUILauncherActivity
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class InstallArgsUtils(private val mcVersion: String, private val loaderVersion: String) {
    fun setFabric(intent: Intent, jarFile: File) {
        val args = "-jar ${jarFile.absolutePath} client -mcversion $mcVersion -loader $loaderVersion -dir ${ProfilePathHome.gameHome}"
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    fun setQuilt(intent: Intent, jarFile: File) {
        val args = "-jar ${jarFile.absolutePath} install client $mcVersion $loaderVersion --install-dir=${ProfilePathHome.gameHome}"
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    fun setForge(intent: Intent, jarFile: File) {
        val zipFile = ZipFile(jarFile)
        val entry: ZipEntry = zipFile.entries().asSequence()
            .firstOrNull { it.name == "install_profile.json" }
            ?: throw IOException("File \"install_profile.json\" not found in the Installer")

        zipFile.getInputStream(entry).use { inputStream ->
            inputStream.bufferedReader().use {
                val json = it.readText()

                val jsonObject = JsonParser.parseString(json).asJsonObject
                //通过查找install_profile.json内是否有spec键，来判断其是否为新版本Forge安装器
                if (jsonObject.has("spec")) {
                    //新版本安装器
                    val args = "-cp ${PathAndUrlManager.DIR_DATA}/forge_install_bootstrapper/forge-install-bootstrapper.jar:${jarFile.absolutePath} com.bangbang93.ForgeInstaller ${ProfilePathHome.gameHome}"
                    intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
                    intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
                    intent.putExtra("javaArgs", args)
                } else {
                    //旧版本安装器
                    val args = "-javaagent:${PathAndUrlManager.DIR_DATA}/forge_installer/forge_installer.jar=\"$loaderVersion\" -jar ${jarFile.absolutePath}"
                    intent.putExtra("javaArgs", args)
                }
            }
        }
    }

    fun setNeoForge(intent: Intent, jarFile: File) {
        val args = "-jar ${jarFile.absolutePath} --installClient ${ProfilePathHome.gameHome}"
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    fun setOptiFine(intent: Intent, jarFile: File) {
        val args = "-javaagent:${PathAndUrlManager.DIR_DATA}/forge_installer/forge_installer.jar=OFNPS -jar ${jarFile.absolutePath}"
        intent.putExtra("javaArgs", args)
    }
}