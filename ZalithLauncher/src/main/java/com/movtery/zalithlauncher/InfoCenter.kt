package com.movtery.zalithlauncher

import android.content.Context

class InfoCenter {
    companion object {
        const val LAUNCHER_NAME: String = "ZalithLauncher"
        const val APP_NAME: String = "Zalith Launcher"

        @JvmStatic
        fun replaceName(context: Context, resString: Int): String = context.getString(resString, APP_NAME)
    }
}
