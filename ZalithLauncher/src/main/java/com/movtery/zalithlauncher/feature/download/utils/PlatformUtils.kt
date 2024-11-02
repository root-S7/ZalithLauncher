package com.movtery.zalithlauncher.feature.download.utils

import com.movtery.zalithlauncher.context.ContextExecutor
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler

class PlatformUtils {
    companion object {
        fun createCurseForgeApi() = ApiHandler(
            "https://api.curseforge.com/v1",
            ContextExecutor.getString(R.string.curseforge_api_key)
        )
    }
}