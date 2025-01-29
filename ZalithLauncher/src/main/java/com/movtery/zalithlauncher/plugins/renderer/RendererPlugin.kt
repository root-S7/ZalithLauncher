package com.movtery.zalithlauncher.plugins.renderer

data class RendererPlugin(
    val id: String,
    val des: String,
    val uniqueIdentifier: String,
    val glName: String,
    val eglName: String,
    val path: String,
    val env: Map<String, String>,
    val dlopen: List<String>
)
