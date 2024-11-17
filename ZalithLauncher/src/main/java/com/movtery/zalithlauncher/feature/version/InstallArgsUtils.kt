package com.movtery.zalithlauncher.feature.version

import android.content.Intent
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import net.kdt.pojavlaunch.JavaGUILauncherActivity
import java.io.File

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
        val args = "-cp ${PathAndUrlManager.DIR_DATA}/forge_install_bootstrapper/forge-install-bootstrapper.jar:${jarFile.absolutePath} com.bangbang93.ForgeInstaller ${ProfilePathHome.gameHome}"
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
        intent.putExtra("javaArgs", args)
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