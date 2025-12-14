package com.root.direct.install.config

import com.root.direct.install.utils.DIUtils.*
import com.root.direct.install.utils.path.*
import org.w3c.dom.*
import java.io.*
import java.util.Collections.unmodifiableMap
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

object ConfigManager {
    @JvmStatic
    val langMap = runCatching {
        openAssets(null, LANG)?.use {
            unmodifiableMap(it.parseLangXml())
        } ?: error("语言文件错误！")
    }.getOrElse { emptyMap() }

    @JvmStatic
    val settings: Properties = runCatching {
        openAssets(null, SETTINGS)?.use { input ->
            Properties().apply { load(input) }
        } ?: error("文件异常！")
    }.getOrElse { Properties() }
}

// InputStream → Map<String,String>
private fun InputStream.parseLangXml(): Map<String, String> =
    DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(this)
        .documentElement
        .childNodes
        .toSequence()
        .parseNodes()
        .toMap()

// NodeList → Sequence<Node>
private fun NodeList.toSequence(): Sequence<Node> = sequence {
    for (i in 0 until length) yield(item(i))
}

// Pair(key,value)
private fun Sequence<Node>.parseNodes(path: String = ""): Sequence<Pair<String, String>> = flatMap { node ->
    if (node.nodeType != Node.ELEMENT_NODE) return@flatMap emptySequence()
    val element = node as Element
    when (element.tagName) {
        "string" -> {
            val name = element.getAttribute("name").takeIf { it.isNotEmpty() } ?: return@flatMap emptySequence()
            val key = listOf(path, name).filter { it.isNotEmpty() }.joinToString(".")
            sequenceOf(key to element.textContent.trim())
        }
        else -> {
            val nextPath = listOf(path, element.tagName).filter { it.isNotEmpty() }.joinToString(".")
            element.childNodes.toSequence().parseNodes(nextPath)
        }
    }
}