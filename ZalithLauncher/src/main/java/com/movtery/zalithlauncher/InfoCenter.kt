package com.movtery.zalithlauncher

import android.content.Context

class InfoCenter {
    companion object {
        const val LAUNCHER_NAME: String = "ZalithLauncher"
        const val APP_NAME: String = "Zalith Launcher"
        const val QQ_GROUP: String = "435667089"

        @JvmStatic
        fun replaceName(context: Context, resString: Int): String = context.getString(resString, APP_NAME)
    }
}
