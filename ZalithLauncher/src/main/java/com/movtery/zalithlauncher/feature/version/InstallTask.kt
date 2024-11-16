package com.movtery.zalithlauncher.feature.version

import java.io.File

interface InstallTask {
    @Throws(Exception::class)
    fun run(): File?
}