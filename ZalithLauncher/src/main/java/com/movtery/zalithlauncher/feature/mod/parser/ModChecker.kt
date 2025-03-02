package com.movtery.zalithlauncher.feature.mod.parser

import android.content.Context
import com.mio.util.AndroidUtil
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.StringSettingUnit
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.ui.dialog.TipDialog
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Logger

class ModChecker {
    class ModCheckResult {
        var hasTouchController: Boolean = false
        var hasSodiumOrEmbeddium: Boolean = false
        var hasPhysics: Boolean = false
        var hasMCEF: Boolean = false
        var hasValkyrienSkies: Boolean = false
        var hasYesSteveModel: Boolean = false
    }

    /**
     * 检查所有模组，并对一些已知的模组进行判断
     */
    fun check(context: Context, modInfoList: List<ModInfo>, showResultDialog: Boolean = true): ModCheckResult {
        return runCatching {
            val modCheckSettings = mutableMapOf<StringSettingUnit, Pair<String, String>>()

            if (modInfoList.isNotEmpty()) {
                Logger.appendToLog("Mod Perception: ${modInfoList.size} Mods parsed successfully")
            }

            val modResult = ModCheckResult()

            modInfoList.forEach { mod ->
                when (mod.id) {
                    "touchcontroller" -> {
                        if (!modResult.hasTouchController) {
                            modResult.hasTouchController = true
                            modCheckSettings[AllSettings.modCheckTouchController] = Pair(
                                "1",
                                context.getString(R.string.mod_check_touch_controller, mod.file.name)
                            )
                        }
                    }
                    "sodium", "embeddium" -> {
                        if (!modResult.hasSodiumOrEmbeddium) {
                            modResult.hasSodiumOrEmbeddium = true
                            modCheckSettings[AllSettings.modCheckSodiumOrEmbeddium] = Pair(
                                "1",
                                context.getString(R.string.mod_check_sodium_or_embeddium, mod.file.name)
                            )
                        }
                    }
                    "physicsmod" -> {
                        if (!modResult.hasPhysics) {
                            modResult.hasPhysics = true
                            val arch = AndroidUtil.getElfArchFromZip(
                                mod.file,
                                "de/fabmax/physxjni/linux/libPhysXJniBindings_64.so"
                            )
                            if (arch.isBlank() or (!Architecture.isx86Device() and arch.contains("x86"))) {
                                modCheckSettings[AllSettings.modCheckPhysics] = Pair(
                                    "1",
                                    context.getString(R.string.mod_check_physics, mod.file.name)
                                )
                            }
                        }
                    }
                    "mcef" -> {
                        if (!modResult.hasMCEF) {
                            modResult.hasMCEF = true
                            modCheckSettings[AllSettings.modCheckMCEF] = Pair(
                                "1",
                                context.getString(R.string.mod_check_mcef, mod.file.name)
                            )
                        }
                    }
                    "valkyrienskies" -> {
                        if (!modResult.hasValkyrienSkies) {
                            modResult.hasValkyrienSkies = true
                            modCheckSettings[AllSettings.modCheckValkyrienSkies] = Pair(
                                "1",
                                context.getString(R.string.mod_check_valkyrien_skies, mod.file.name)
                            )
                        }
                    }
                    "yes_steve_model" -> {
                        if (!modResult.hasYesSteveModel) {
                            modResult.hasYesSteveModel = true
                            val arch = AndroidUtil.getElfArchFromZip(
                                mod.file,
                                "META-INF/native/libysm-core.so"
                            )
                            if (arch.isNotBlank()) {
                                modCheckSettings[AllSettings.modCheckYesSteveModel] = Pair(
                                    "1",
                                    context.getString(R.string.mod_check_yes_steve_model, mod.file.name)
                                )
                            }
                        }
                    }
                }
            }

            if (showResultDialog) showResultDialog(context, modCheckSettings)

            modResult
        }.getOrElse { e ->
            Logging.e("LaunchGame", "An error occurred while trying to process existing mod information", e)
            ModCheckResult()
        }
    }

    private fun showResultDialog(context: Context, modCheckSettings: MutableMap<StringSettingUnit, Pair<String, String>>) {
        if (modCheckSettings.isNotEmpty()) {
            var index = 1
            val messages = modCheckSettings
                .filter { (setting, valuePair) -> setting.getValue() != valuePair.first }
                .map { (_, valuePair) -> "${index++}. ${valuePair.second}" }

            if (messages.isNotEmpty()) {
                TaskExecutors.runInUIThread {
                    TipDialog.Builder(context)
                        .setTitle(R.string.mod_check_dialog_title)
                        .setMessage(messages.joinToString("\r\n\r\n"))
                        .setCheckBox(R.string.generic_no_more_reminders)
                        .setShowCheckBox(true)
                        .setCenterMessage(false)
                        .setCancelable(false)
                        .setShowCancel(false)
                        .setConfirmClickListener { check ->
                            if (check) {
                                modCheckSettings.forEach { (setting, valuePair) ->
                                    setting.put(valuePair.first).save()
                                }
                            }
                        }.showDialog()
                }
            }
        }
    }
}