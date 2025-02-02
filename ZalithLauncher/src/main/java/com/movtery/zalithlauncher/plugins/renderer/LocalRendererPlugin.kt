package com.movtery.zalithlauncher.plugins.renderer

import java.io.File

data class LocalRendererPlugin(
    val uniqueIdentifier: String,
    val rendererId: String,
    val rendererName: String,
    val folderPath: File
)