package com.movtery.zalithlauncher.plugins.renderer

data class RendererConfig(
    val rendererId: String,
    val rendererDisplayName: String,
    val glName: String,
    val eglName: String,
    val env: Map<String, String>
)