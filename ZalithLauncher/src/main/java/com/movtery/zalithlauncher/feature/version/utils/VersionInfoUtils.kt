package com.movtery.zalithlauncher.feature.version.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.VersionInfo
import net.kdt.pojavlaunch.Tools
import java.io.File

class VersionInfoUtils {
    companion object {
        private data class LoaderDetectionResult(val name: String, val version: String) {
            companion object {
                val UNKNOWN = LoaderDetectionResult("Unknown", "")
            }
        }

        // "1.20.4-OptiFine_HD_U_I7_pre3"       -> Pair("OptiFine", "HD_U_I7_pre3")
        // "1.21.3-OptiFine_HD_U_J2_pre6"       -> Pair("OptiFine", "HD_U_J2_pre6")
        private val OPTIFINE_ID_REGEX = """-OptiFine_([\w.]+)""".toRegex()
        // "1.20.2-forge-48.1.0"                -> Pair("Forge", "48.1.0")
        // "1.21.3-forge-53.0.23"               -> Pair("Forge", "53.0.23")
        private val FORGE_REGEX = """-forge-([\w.]+)""".toRegex()
        // "1.7.10-Forge10.13.4.1614-1.7.10"    -> Pair("Forge", "10.13.4.1614")
        private val FORGE_OLD_REGEX = """-Forge([^-]+)-""".toRegex()
        // "neoforge-21.1.8"                    -> Pair("NeoForge", "21.1.8")
        // "neoforge-21.3.36-beta"              -> Pair("NeoForge", "21.3.36-beta")
        private val NEOFORGE_REGEX = """^neoforge-([\w.-]+)${'$'}""".toRegex()
        // "fabric-loader-0.15.7-1.20.4"        -> Pair("Fabric", "0.15.7")
        // "fabric-loader-0.16.9-1.21.3"        -> Pair("Fabric", "0.16.9")
        private val FABRIC_REGEX = """fabric-loader-([\w.-]+)-\d+\.\d+""".toRegex()
        // "quilt-loader-0.23.1-1.20.4"         -> Pair("Quilt", "0.23.1")
        // "quilt-loader-0.27.1-beta.1-1.21.3"  -> Pair("Quilt", "0.27.1-beta.1")
        private val QUILT_REGEX = """quilt-loader-([\w.-]+)-\d+\.\d+""".toRegex()

        private val LOADER_DETECTORS = listOf<(String) -> LoaderDetectionResult?>(
            { id ->
                OPTIFINE_ID_REGEX.find(id)?.let {
                    LoaderDetectionResult("OptiFine", it.groupValues[1])
                }
            },
            { id ->
                FORGE_REGEX.find(id)?.let {
                    LoaderDetectionResult("Forge", it.groupValues[1])
                }
            },
            { id ->
                FORGE_OLD_REGEX.find(id)?.let {
                    LoaderDetectionResult("Forge", it.groupValues[1])
                }
            },
            { id ->
                NEOFORGE_REGEX.find(id)?.let {
                    LoaderDetectionResult("NeoForge", it.groupValues[1])
                }
            },
            { id ->
                FABRIC_REGEX.find(id)?.let {
                    LoaderDetectionResult("Fabric", it.groupValues[1])
                }
            },
            { id ->
                QUILT_REGEX.find(id)?.let {
                    LoaderDetectionResult("Quilt", it.groupValues[1])
                }
            }
        )

        /**
         * 在版本的json文件中，找到版本信息，识别其是否有id这个键
         * @return 版本号、ModLoader信息
         */
        fun parseJson(jsonFile: File): VersionInfo? {
            return runCatching {
                val json = Tools.read(jsonFile)
                val jsonObject = JsonParser.parseString(json).asJsonObject

                if (!jsonObject.has("id")) return@runCatching null

                val (versionId, loaderInfo) = processVersionInfo(jsonObject)
                VersionInfo(versionId, loaderInfo?.let { arrayOf(it) })
            }.onFailure {
                Logging.e("VersionInfoUtils", "Error parsing version json", it)
            }.getOrNull()
        }

        private fun processVersionInfo(jsonObject: JsonObject): Pair<String, VersionInfo.LoaderInfo?> {
            //由于已知的ModLoader都会把id更改为自己定义的版本字符串格式
            //使用inheritsFrom来存放原版的id
            //所以这里用检查inheritsFrom是否存在的方式来判断是否为ModLoader
            val id = jsonObject.get("id").asString
            val versionId = jsonObject.get("inheritsFrom")?.asString ?: id

            val loaderInfo = if (jsonObject.has("inheritsFrom")) {
                val libraries = jsonObject.getAsJsonArray("libraries")
                detectModLoader(id, libraries).also {
                    Logging.i("Parse version info", it.toString())
                }
            } else null

            return Pair(versionId, loaderInfo)
        }

        /**
         * 通过Id判断ModLoader信息：ModLoader名称、版本
         */
        private fun detectModLoader(id: String, libraries: JsonArray?): VersionInfo.LoaderInfo {
            val idResult = LOADER_DETECTORS.firstNotNullOfOrNull { it(id) }

            //以新版Zalith的安装方式，OptiFine版本可能不能正常识别
            //需要检查libraries列表，查看是否有OptiFine，以确认是否为OptiFine版本
            val libResult = idResult ?: checkOptiFineInLibraries(libraries)

            val finalResult = libResult ?: LoaderDetectionResult.UNKNOWN
            return VersionInfo.LoaderInfo(finalResult.name, finalResult.version)
        }

        /**
         * 通过库中是否有optifine来判断当前版本是否为一个OptiFine版本
         */
        private fun checkOptiFineInLibraries(libraries: JsonArray?): LoaderDetectionResult? {
            return libraries?.firstOrNull { library ->
                runCatching {
                    library.asJsonObject.get("name").asString.startsWith("optifine:OptiFine:")
                }.getOrNull() ?: false
            }?.let { library ->
                val version = library.asJsonObject.get("name").asString.split(':')[2]
                LoaderDetectionResult("OptiFine", version)
            }
        }
    }
}