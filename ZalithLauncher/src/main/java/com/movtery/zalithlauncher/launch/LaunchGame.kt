package com.movtery.zalithlauncher.launch

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kdt.mcgui.ProgressLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.accounts.AccountUtils
import com.movtery.zalithlauncher.feature.accounts.AccountsManager
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.Version
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.ui.dialog.LifecycleAwareTipDialog
import com.movtery.zalithlauncher.ui.dialog.TipDialog
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.http.NetworkUtils
import com.movtery.zalithlauncher.utils.stringutils.StringUtils
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.Logger
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.microsoft.PresentedException
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.plugins.FFmpegPlugin
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.services.GameService.LocalBinder
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader
import net.kdt.pojavlaunch.tasks.MinecraftDownloader
import net.kdt.pojavlaunch.utils.JREUtils
import net.kdt.pojavlaunch.value.MinecraftAccount

class LaunchGame {
    companion object {
        /**
         * 改为启动游戏前进行登录，同时也能及时的刷新账号的信息（这明显更合理不是吗，PojavLauncher？）
         * @param version 选择的版本
         */
        @JvmStatic
        fun preLaunch(context: Context, version: Version) {
            fun launch() {
                val versionName = version.getVersionName()
                val mcVersion = AsyncMinecraftDownloader.getListedVersion(versionName)
                MinecraftDownloader().start(
                    mcVersion, versionName, ContextAwareDoneListener(context, version)
                )
            }

            fun setGameProgress(pull: Boolean) {
                if (pull) ProgressKeeper.submitProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0, R.string.newdl_downloading_game_files, 0, 0, 0)
                else ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT)
            }

            if (!NetworkUtils.isNetworkAvailable(context)) {
                //网络未链接，无法登录，但是依旧允许玩家启动游戏
                Toast.makeText(context, context.getString(R.string.account_login_no_network), Toast.LENGTH_SHORT).show()
                launch()
                return
            }

            val accountsManager = AccountsManager.getInstance()

            if (AccountUtils.isNoLoginRequired(accountsManager.currentAccount)) {
                launch()
                return
            }

            accountsManager.performLogin(
                accountsManager.currentAccount,
                { _ ->
                    TaskExecutors.runInUIThread {
                        Toast.makeText(context, context.getString(R.string.account_login_done), Toast.LENGTH_SHORT).show()
                    }
                    //登录完成，正式启动游戏！
                    launch()
                },
                { exception ->
                    val errorMessage = if (exception is PresentedException) exception.toString(context)
                    else exception.message

                    TaskExecutors.runInUIThread {
                        TipDialog.Builder(context)
                            .setTitle(R.string.generic_error)
                            .setMessage("${context.getString(R.string.account_login_skip)}\r\n$errorMessage")
                            .setConfirmClickListener { launch() }
                            .setCenterMessage(false)
                            .buildDialog()
                    }

                    setGameProgress(false)
                }
            )
            setGameProgress(true)
        }

        @Throws(Throwable::class)
        @JvmStatic
        fun runGame(activity: AppCompatActivity, serverBinder: LocalBinder, minecraftVersion: Version, version: JMinecraftVersionList.Version) {
            Tools.LOCAL_RENDERER ?: run { Tools.LOCAL_RENDERER = AllSettings.renderer }

            if (!Tools.checkRendererCompatible(activity, Tools.LOCAL_RENDERER)) {
                val renderersList = Tools.getCompatibleRenderers(activity)
                val firstCompatibleRenderer = renderersList.rendererIds[0]
                Logging.w("runGame", "Incompatible renderer ${Tools.LOCAL_RENDERER} will be replaced with $firstCompatibleRenderer")
                Tools.LOCAL_RENDERER = firstCompatibleRenderer
                Tools.releaseCache()
            }

            val customArgs = minecraftVersion.getJavaArgs().takeIf { it.isNotBlank() }
                ?: AllSettings.javaArgs?.takeIf { it.isNotBlank() }
                ?: ""
            val account = AccountsManager.getInstance().currentAccount
            printLauncherInfo(
                minecraftVersion,
                customArgs.takeIf { it.isNotBlank() } ?: "NONE",
                minecraftVersion.getJavaDir().takeIf { it.isNotBlank() } ?: "NONE",
                account
            )
            JREUtils.redirectAndPrintJRELog()

            val requiredJavaVersion = version.javaVersion?.majorVersion ?: 8
            launch(activity, account, minecraftVersion, requiredJavaVersion, customArgs)
            //Note that we actually stall in the above function, even if the game crashes. But let's be safe.
            activity.runOnUiThread { serverBinder.isActive = false }
        }

        private fun printLauncherInfo(
            minecraftVersion: Version,
            javaArguments: String,
            javaRuntime: String,
            account: MinecraftAccount
        ) {
            fun formatJavaRuntimeString(): String {
                val prefix = Tools.LAUNCHERPROFILES_RTPREFIX
                return if (javaRuntime.startsWith(prefix)) javaRuntime.removePrefix(prefix)
                else javaRuntime
            }

            var mcInfo = minecraftVersion.getVersionName()
            minecraftVersion.getVersionInfo()?.let { info ->
                mcInfo = info.getInfoString()
            }

            Logger.appendToLog("--------- Start launching the game")
            Logger.appendToLog("Info: Launcher version: ${ZHTools.getVersionName()} (${ZHTools.getVersionCode()})")
            Logger.appendToLog("Info: Architecture: ${Architecture.archAsString(Tools.DEVICE_ARCHITECTURE)}")
            Logger.appendToLog("Info: Device model: ${StringUtils.insertSpace(Build.MANUFACTURER, Build.MODEL)}")
            Logger.appendToLog("Info: API version: ${Build.VERSION.SDK_INT}")
            Logger.appendToLog("Info: Selected Minecraft version: ${minecraftVersion.getVersionName()}")
            Logger.appendToLog("Info: Minecraft Info: $mcInfo")
            Logger.appendToLog("Info: Custom Java arguments: $javaArguments")
            Logger.appendToLog("Info: Java Runtime: ${formatJavaRuntimeString()}")
            Logger.appendToLog("Info: Account: ${account.username} (${account.accountType})")
        }

        @Throws(Throwable::class)
        @JvmStatic
        private fun launch(
            activity: AppCompatActivity,
            account: MinecraftAccount,
            minecraftVersion: Version,
            versionJavaRequirement: Int,
            customArgs: String
        ) {
            checkMemory(activity)

            val runtime = MultiRTUtils.forceReread(
                Tools.pickRuntime(
                    activity,
                    minecraftVersion,
                    versionJavaRequirement
                )
            )

            val versionInfo = Tools.getVersionInfo(minecraftVersion)
            val gameDirPath = minecraftVersion.getGameDir()

            //预处理
            Tools.disableSplash(gameDirPath)
            val launchClassPath = Tools.generateLaunchClassPath(versionInfo, minecraftVersion)

            val launchArgs = LaunchArgs(
                activity,
                account,
                gameDirPath,
                minecraftVersion,
                versionInfo,
                minecraftVersion.getVersionName(),
                runtime,
                launchClassPath
            ).getAllArgs()

            FFmpegPlugin.discover(activity)
            JREUtils.launchJavaVM(activity, runtime, gameDirPath, launchArgs, customArgs)
        }

        private fun checkMemory(activity: AppCompatActivity) {
            var freeDeviceMemory = Tools.getFreeDeviceMemory(activity)
            val freeAddressSpace =
                if (Architecture.is32BitsDevice())
                    Tools.getMaxContinuousAddressSpaceSize()
                else -1
            Logging.i("MemStat",
                "Free RAM: $freeDeviceMemory Addressable: $freeAddressSpace")

            val stringId: Int = if (freeDeviceMemory > freeAddressSpace && freeAddressSpace != -1) {
                freeDeviceMemory = freeAddressSpace
                R.string.address_memory_warning_msg
            } else R.string.memory_warning_msg

            if (AllSettings.ramAllocation > freeDeviceMemory) {
                val builder = TipDialog.Builder(activity)
                    .setTitle(R.string.generic_warning)
                    .setMessage(activity.getString(stringId, freeDeviceMemory, AllSettings.ramAllocation))
                    .setCenterMessage(false)
                    .setShowCancel(false)
                if (LifecycleAwareTipDialog.haltOnDialog(activity.lifecycle, builder)) return
                // If the dialog's lifecycle has ended, return without
                // actually launching the game, thus giving us the opportunity
                // to start after the activity is shown again
            }
        }
    }
}