package com.movtery.zalithlauncher.setting

import androidx.annotation.CheckResult
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.event.single.SettingsChangeEvent
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.setting.unit.AbstractSettingUnit
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.lang.reflect.Type
import java.util.Objects

class Settings {
    companion object {
        private val GSON: Gson = GsonBuilder().disableHtmlEscaping().create()

        private var settings: List<SettingAttribute> = refresh()

        private fun refresh(): List<SettingAttribute> {
            return PathManager.FILE_SETTINGS.takeIf { it.exists() }?.let { file ->
                try {
                    val jsonString = Tools.read(file)
                    val listType: Type = object : TypeToken<List<SettingAttribute>>() {}.type
                    GSON.fromJson(jsonString, listType)
                } catch (e: Throwable) {
                    Logging.e("Settings", Tools.printToString(e))
                    emptyList()
                }
            } ?: emptyList()
        }

        fun refreshSettings() {
            settings = refresh()
        }
    }

    class Manager {
        companion object {
            fun <T> getValue(key: String, defaultValue: T, parser: (String) -> T?): T {
                settings.forEach {
                    if (Objects.equals(it.key, key)) {
                        return it.value?.let { value -> parser(value) } ?: defaultValue
                    }
                }
                return defaultValue
            }

            @JvmStatic
            fun contains(key: String): Boolean {
                return settings.any { it.key == key }
            }

            @JvmStatic
            @CheckResult
            fun put(key: String, value: Any) = SettingBuilder().put(key, value)
        }

        class SettingBuilder {
            private val valueMap: MutableMap<String, Any?> = HashMap()

            @CheckResult
            fun put(key: String, value: Any): SettingBuilder {
                valueMap[key] = value
                return this
            }

            @CheckResult
            fun put(unit: AbstractSettingUnit<*>, value: Any): SettingBuilder {
                valueMap[unit.key] = value
                return this
            }

            fun save() {
                val settingsFile = PathManager.FILE_SETTINGS
                if (!settingsFile.exists()) settingsFile.createNewFile()

                val currentSettings = settings.toMutableList()

                valueMap.forEach { (key, value) ->
                    val attribute = currentSettings.find { it.key == key }

                    if (attribute != null) {
                        attribute.value = value.toString()
                    } else {
                        val newAttribute = SettingAttribute().apply {
                            this.key = key
                            this.value = value.toString()
                        }
                        currentSettings.add(newAttribute)
                    }
                }

                val json = GSON.toJson(currentSettings)

                runCatching {
                    FileUtils.write(settingsFile, json)
                    refreshSettings()
                }.getOrElse { e ->
                    Logging.e("SettingBuilder", Tools.printToString(e))
                }

                EventBus.getDefault().post(SettingsChangeEvent())
            }
        }
    }
}
