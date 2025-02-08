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
        /**
         * 在版本的json文件中，找到版本信息，识别其是否有id这个键
         * @return 版本号、ModLoader信息
         */
        fun parseJson(jsonFile: File): VersionInfo? {
            return runCatching {
                val json = Tools.read(jsonFile)
                val jsonObject = JsonParser.parseString(json).asJsonObject
                val (versionId, loaderInfo) = detectMinecraftAndLoader(jsonObject)
                VersionInfo(versionId, loaderInfo?.let { arrayOf(it) })
            }.getOrElse {
                Logging.e("VersionInfoUtils", "Error parsing version json", it)
                null
            }
        }

        private fun detectMinecraftAndLoader(versionJson: JsonObject): Pair<String, VersionInfo.LoaderInfo?> {
            val mcVersion = extractMinecraftVersion(versionJson).also {
                Logging.i("VersionInfoUtils", "Detected Minecraft version: $it")
            }
            val loaderInfo = detectModLoader(versionJson)?.also {
                Logging.i("VersionInfoUtils", "Detected ModLoader: $it")
            }
            return mcVersion to loaderInfo
        }

        private fun extractMinecraftVersion(json: JsonObject): String {
            //从minecraft库中获取
            json.getAsJsonArray("libraries")?.forEach { lib ->
                val (group, artifact, version) = lib.asJsonObject["name"].asString.split(":").let {
                    Triple(it[0], it[1], it.getOrNull(2) ?: "")
                }
                if (group == "net.minecraft" && (artifact == "client" || artifact == "server")) {
                    return version
                }
            }

            //从版本ID中解析
            val versionId = json["id"].asString
            return when {
                //1.19.2-forge-43.1.0 → 1.19.2
                versionId.contains('-') -> versionId.substringBefore('-')

                //快照版本：23w13a
                versionId.any { it.isLetter() } -> versionId

                else -> versionId
            }
        }

        /**
         * 通过库判断ModLoader信息：ModLoader名称、版本
         * @param versionJson 版本json对象
         */
        private fun detectModLoader(versionJson: JsonObject): VersionInfo.LoaderInfo? {
            versionJson.getAsJsonArray("libraries")?.forEach { libElement ->
                val lib = libElement.asJsonObject
                val (group, artifact, version) = lib.get("name").asString.split(":").let {
                    Triple(it[0], it[1], it.getOrNull(2) ?: "")
                }

                when {
                    //Fabric
                    group == "net.fabricmc" && artifact == "fabric-loader" ->
                        return VersionInfo.LoaderInfo("Fabric", version)

                    //Forge
                    group == "net.minecraftforge" && (artifact == "forge" || artifact == "fmlloader") -> {
                        val forgeVersion = when {
                            //新版：1.21.4-54.0.26                 -> 54.0.26
                            version.count { it == '-' } == 1 -> version.substringAfterLast('-')
                            //旧版：1.7.10-10.13.4.1614-1.7.10     -> 10.13.4.1614
                            version.count { it == '-' } >= 2 -> version.split("-").let { parts ->
                                when {
                                    parts.size >= 3 && parts[0] == parts.last() -> parts[1]
                                    else -> version
                                }
                            }
                            else -> version
                        }
                        return VersionInfo.LoaderInfo("Forge", forgeVersion)
                    }

                    //NeoForge
                    group == "net.neoforged.fancymodloader" && artifact == "loader" -> {
                        val neoVersion = versionJson.getAsJsonObject("arguments")
                            ?.getAsJsonArray("game")
                            ?.findNeoForgeVersion()
                            ?: version
                        return VersionInfo.LoaderInfo("NeoForge", neoVersion)
                    }

                    //OptiFine
                    (group == "optifine" || group == "net.optifine") && artifact == "OptiFine" ->
                        return VersionInfo.LoaderInfo("OptiFine", version)

                    //Quilt
                    group == "org.quiltmc" && artifact == "quilt-loader" ->
                        return VersionInfo.LoaderInfo("Quilt", version)

                    //LiteLoader
                    group == "com.mumfrey" && artifact == "liteloader" ->
                        return VersionInfo.LoaderInfo("LiteLoader", version)
                }
            }

            val mainClass = versionJson.get("mainClass")?.asString ?: ""
            val tweakers = versionJson.getAsJsonObject("arguments")
                ?.getAsJsonArray("game")
                ?.mapNotNull { it.asJsonPrimitive?.asString }

            return when {
                mainClass.startsWith("net.fabricmc") -> VersionInfo.LoaderInfo("Fabric", "unknown")
                mainClass.startsWith("cpw.mods") -> VersionInfo.LoaderInfo("Forge", "unknown")
                mainClass.startsWith("net.neoforged") -> VersionInfo.LoaderInfo("NeoForge", "unknown")
                tweakers?.any { it.contains("OptiFineTweaker") } == true -> VersionInfo.LoaderInfo("OptiFine", "unknown")
                else -> null
            }
        }

        /**
         * NeoForge会将版本号存放到游戏参数内
         * 尝试在 arguments: { "game": [] } 中寻找NeoForge的版本
         */
        private fun JsonArray.findNeoForgeVersion(): String? {
            for (i in 0 until this.size() - 1) {
                if (this[i].asString == "--fml.neoForgeVersion") {
                    return this[i + 1].asString
                }
            }
            return null
        }
    }
}