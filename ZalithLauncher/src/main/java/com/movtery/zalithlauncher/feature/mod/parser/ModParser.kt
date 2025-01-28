package com.movtery.zalithlauncher.feature.mod.parser

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.moandjiezana.toml.Toml
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.task.Task
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import kotlin.jvm.Throws

class ModParser {
    /**
     * 异步尝试解析当前模组文件夹内的所有模组，并将模组的关键信息记录在List集合中
     * @param modsFolder 需要检查的版本的模组文件夹
     * @param listener 解析监听器
     */
    fun parseAllMods(modsFolder: File, listener: ModParserListener) {
        val modInfoList: MutableList<ModInfo> = mutableListOf()

        Task.runTask {
            if (modsFolder.exists() && modsFolder.isDirectory) {
                val files = modsFolder.listFiles()?.filter { it.extension.equals("jar", true) } ?: return@runTask

                runBlocking {
                    val semaphore = Semaphore(calculateThreadCount(files.size))
                    val deferredResults = mutableListOf<Deferred<Unit>>()

                    files.forEach { modFile ->
                        val deferred = async(Dispatchers.IO) {
                            semaphore.withPermit {
                                try {
                                    JarInputStream(FileInputStream(modFile)).use { jarInputStream ->
                                        parseJarEntries(jarInputStream) { modInfo ->
                                            modInfoList.add(modInfo)
                                            listener.onProgress(modInfo)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Logging.e("ModParser", "Failed to parse the Mod file ${modFile.name}!", e)
                                }
                            }
                        }
                        deferredResults.add(deferred)
                    }

                    //等待所有解析任务完成
                    deferredResults.awaitAll()
                }
            }
        }.onThrowable { e ->
            Logging.e("ModParser", "An exception occurred while parsing all mods.!", e)
        }.finallyTask {
            listener.onParseEnded(modInfoList)
        }.execute()
    }

    @Throws(Throwable::class)
    private fun parseJarEntries(
        jarInputStream: JarInputStream,
        parseSuccessCallback: (ModInfo) -> Unit
    ) {
        fun parseAndAddIntoList(parse: (info: String) -> ModInfo?) {
            val modInfo = parse(jarInputStream.bufferedReader().use(BufferedReader::readText))
            modInfo?.let {
                parseSuccessCallback(it)
            }
        }

        var entry: JarEntry?
        while (jarInputStream.nextJarEntry.also { entry = it } != null) {
            entry?.let { file ->
                when (file.name) {
                    //Fabric
                    "fabric.mod.json" -> {
                        parseAndAddIntoList { parseFabricModJson(it) }
                        return
                    }
                    //Quilt
                    "quilt.mod.json" -> {
                        parseAndAddIntoList { parseQuiltModJson(it) }
                        return
                    }
                    //Forge、NeoForge
                    "META-INF/neoforge.mods.toml", "META-INF/mods.toml" -> {
                        parseAndAddIntoList { parseForgeLikeModToml(it) }
                        return
                    }
                    //旧版 Forge
                    "mcmod.info" -> {
                        parseAndAddIntoList { parseOldForgeModInfo(it) }
                        return
                    }
                    else -> {}
                }
            }
        }
    }

    private fun calculateThreadCount(fileCount: Int): Int {
        val maxThreads = 64
        //如果文件数少于最大线程数，则使用文件数量本身作为线程数
        return when {
            fileCount <= maxThreads -> fileCount
            else -> maxThreads
        }
    }

    @Throws(Throwable::class)
    private fun parseFabricModJson(jsonString: String): ModInfo {
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
        return ModInfo(
            jsonObject.get("id").asString,
            jsonObject.get("version").asString,
            jsonObject.get("name").asString,
            jsonObject.get("description").asString,
            jsonObject.get("authors").asJsonArray.let { authorsArray ->
                val authorsList = mutableListOf<String>()
                authorsArray.forEach { authorElement ->
                    val authorName: String = (
                            if (authorElement.isJsonObject) authorElement.asJsonObject?.get("name")?.asString
                            else authorElement.asString
                            ) ?: return@forEach
                    authorsList.add(authorName)
                }
                authorsList.toTypedArray()
            }
        )
    }

    @Throws(Throwable::class)
    private fun parseQuiltModJson(jsonString: String): ModInfo {
        val quiltLoader = JsonParser.parseString(jsonString).asJsonObject.get("quilt_loader").asJsonObject
        val metaData = quiltLoader.get("metadata").asJsonObject
        return ModInfo(
            quiltLoader.get("id").asString,
            quiltLoader.get("version").asString,
            metaData.get("name").asString,
            metaData.get("description").asString,
            metaData.get("contributors").asJsonObject.keySet().toTypedArray()
        )
    }

    @Throws(Throwable::class)
    private fun parseForgeLikeModToml(tomlString: String): ModInfo? {
        val toml = Toml().read(tomlString)
        val mod = toml.getTables("mods").firstOrNull() ?: return null

        val modId = mod.getString("modId") ?: return null
        val version = mod.getString("version") ?: return null
        val displayName = mod.getString("displayName") ?: return null
        val description = mod.getString("description") ?: ""

        val authors = runCatching {
            mod.getString("authors")
                ?.replace(", ", ",")
                ?.split(",")
                ?.toTypedArray()
        }.getOrElse {
            mod.getList<String>("authors")?.toTypedArray()
        } ?: emptyArray()

        return ModInfo(modId, version, displayName, description, authors)
    }

    @Throws(Throwable::class)
    private fun parseOldForgeModInfo(mcmodInfo: String): ModInfo? {
        val jsonObject = JsonParser.parseString(mcmodInfo).asJsonArray[0].asJsonObject
        val modId = jsonObject.get("modid")?.asString ?: return null
        val version = jsonObject.get("version")?.asString ?: return null
        val name = jsonObject.get("name")?.asString ?: return null
        val description = jsonObject.get("description")?.asString ?: ""

        val authors =
            if (jsonObject.has("authorList"))
                jsonObject.get("authorList").asJsonArray.toStringArray()
            else
                jsonObject.get("authors").asJsonArray.toStringArray()

        return ModInfo(modId, version, name, description, authors)
    }

    private fun JsonArray.toStringArray(): Array<String> {
        val list: MutableList<String> = mutableListOf()
        forEach { element ->
            list.add(element.asString)
        }
        return list.toTypedArray()
    }

    /**
     * 限制最大并发数
     */
    private suspend fun <T> Semaphore.withPermit(action: suspend () -> T): T {
        acquire()
        try {
            return action()
        } finally {
            release()
        }
    }
}
