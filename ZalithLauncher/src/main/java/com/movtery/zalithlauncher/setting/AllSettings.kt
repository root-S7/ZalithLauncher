package com.movtery.zalithlauncher.setting

import com.movtery.zalithlauncher.context.ContextExecutor
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class AllSettings {
    companion object {
        // Video
        @JvmStatic
        val renderer: String?
            get() = Settings.Manager.getString("renderer", "opengles2")

        @JvmStatic
        val ignoreNotch: Boolean
            get() = Settings.Manager.getBoolean("ignoreNotch", true)

        @JvmStatic
        val resolutionRatio: Int
            get() = Settings.Manager.getInt("resolutionRatio", 100)

        @JvmStatic
        val sustainedPerformance: Boolean
            get() = Settings.Manager.getBoolean("sustainedPerformance", false)

        @JvmStatic
        val alternateSurface: Boolean
            get() = Settings.Manager.getBoolean("alternate_surface", false)

        @JvmStatic
        val forceVsync: Boolean
            get() = Settings.Manager.getBoolean("force_vsync", false)

        @JvmStatic
        val vsyncInZink: Boolean
            get() = Settings.Manager.getBoolean("vsync_in_zink", false)

        @JvmStatic
        val zinkPreferSystemDriver: Boolean
            get() = Settings.Manager.getBoolean("zinkPreferSystemDriver", false)

        // Control
        @JvmStatic
        val disableGestures: Boolean
            get() = Settings.Manager.getBoolean("disableGestures", false)

        @JvmStatic
        val disableDoubleTap: Boolean
            get() = Settings.Manager.getBoolean("disableDoubleTap", false)

        @JvmStatic
        val timeLongPressTrigger: Int
            get() = Settings.Manager.getInt("timeLongPressTrigger", 300)

        @JvmStatic
        val buttonscale: Int
            get() = Settings.Manager.getInt("buttonscale", 100)

        @JvmStatic
        val buttonAllCaps: Boolean
            get() = Settings.Manager.getBoolean("buttonAllCaps", true)

        @JvmStatic
        val mouseScale: Int
            get() = Settings.Manager.getInt("mousescale", 100)

        @JvmStatic
        val mouseSpeed: Int
            get() = Settings.Manager.getInt("mousespeed", 100)

        @JvmStatic
        val virtualMouseStart: Boolean
            get() = Settings.Manager.getBoolean("mouse_start", true)

        @JvmStatic
        val customMouse: String?
            get() = Settings.Manager.getString("custom_mouse", null)

        @JvmStatic
        val enableGyro: Boolean
            get() = Settings.Manager.getBoolean("enableGyro", false)

        @JvmStatic
        val gyroSensitivity: Float
            get() = Settings.Manager.getInt("gyroSensitivity", 100) / 100f

        @JvmStatic
        val gyroSampleRate: Int
            get() = Settings.Manager.getInt("gyroSampleRate", 16)

        @JvmStatic
        val gyroSmoothing: Boolean
            get() = Settings.Manager.getBoolean("gyroSmoothing", true)

        @JvmStatic
        val gyroInvertX: Boolean
            get() = Settings.Manager.getBoolean("gyroInvertX", false)

        @JvmStatic
        val gyroInvertY: Boolean
            get() = Settings.Manager.getBoolean("gyroInvertY", false)

        @JvmStatic
        val deadzoneScale: Float
            get() = Settings.Manager.getInt("gamepad_deadzone_scale", 100) / 100f

        // Game
        @JvmStatic
        val autoSetGameLanguage: Boolean
            get() = Settings.Manager.getBoolean("autoSetGameLanguage", true)

        @JvmStatic
        val gameLanguageOverridden: Boolean
            get() = Settings.Manager.getBoolean("gameLanguageOverridden", false)

        @JvmStatic
        val setGameLanguage: String?
            get() = Settings.Manager.getString("setGameLanguage", "system")

        @JvmStatic
        val javaArgs: String?
            get() = Settings.Manager.getString("javaArgs", "")

        @JvmStatic
        val ramAllocation: Int
            get() = Settings.Manager.getInt("allocation", LauncherPreferences.findBestRAMAllocation(ContextExecutor.getApplication()))

        @JvmStatic
        val javaSandbox: Boolean
            get() = Settings.Manager.getBoolean("java_sandbox", true)

        @JvmStatic
        val gameMenuShowMemory: Boolean
            get() = Settings.Manager.getBoolean("gameMenuShowMemory", false)

        @JvmStatic
        val gameMenuMemoryText: String?
            get() = Settings.Manager.getString("gameMenuMemoryText", "M:")

        @JvmStatic
        val gameMenuLocation: String?
            get() = Settings.Manager.getString("gameMenuLocation", "center")

        @JvmStatic
        val gameMenuAlpha: Int
            get() = Settings.Manager.getInt("gameMenuAlpha", 100)

        // Launcher
        @JvmStatic
        val checkLibraries: Boolean
            get() = Settings.Manager.getBoolean("checkLibraries", true)

        @JvmStatic
        val verifyManifest: Boolean
            get() = Settings.Manager.getBoolean("verifyManifest", true)

        @JvmStatic
        val resourceImageCache: Boolean
            get() = Settings.Manager.getBoolean("resourceImageCache", false)

        @JvmStatic
        val downloadSource: String?
            get() = Settings.Manager.getString("downloadSource", "default")

        @JvmStatic
        val modInfoSource: String?
            get() = Settings.Manager.getString("modInfoSource", "original")

        @JvmStatic
        val modDownloadSource: String?
            get() = Settings.Manager.getString("modDownloadSource", "original")

        @JvmStatic
        val launcherTheme: String?
            get() = Settings.Manager.getString("launcherTheme", "system")

        @JvmStatic
        val animation: Boolean
            get() = Settings.Manager.getBoolean("animation", true)

        @JvmStatic
        val animationSpeed: Int
            get() = Settings.Manager.getInt("animationSpeed", 600)

        @JvmStatic
        val pageOpacity: Int
            get() = Settings.Manager.getInt("pageOpacity", 100)

        @JvmStatic
        val enableLogOutput: Boolean
            get() = Settings.Manager.getBoolean("enableLogOutput", false)

        @JvmStatic
        val quitLauncher: Boolean
            get() = Settings.Manager.getBoolean("quitLauncher", true)

        // Experimental
        @JvmStatic
        val dumpShaders: Boolean
            get() = Settings.Manager.getBoolean("dump_shaders", false)

        @JvmStatic
        val bigCoreAffinity: Boolean
            get() = Settings.Manager.getBoolean("bigCoreAffinity", false)

        // Other
        @JvmStatic
        val currentProfile: String?
            get() = Settings.Manager.getString("currentProfile", "")

        @JvmStatic
        val currentAccount: String?
            get() = Settings.Manager.getString("currentAccount", "")

        @JvmStatic
        val launcherProfile: String?
            get() = Settings.Manager.getString("launcherProfile", "default")

        @JvmStatic
        val defaultCtrl: String?
            get() = Settings.Manager.getString("defaultCtrl", PathAndUrlManager.FILE_CTRLDEF_FILE)

        @JvmStatic
        val defaultRuntime: String?
            get() = Settings.Manager.getString("defaultRuntime", "")

        @JvmStatic
        val skipNotificationPermissionCheck: Boolean
            get() = Settings.Manager.getBoolean("skipNotificationPermissionCheck", false)

        @JvmStatic
        val localAccountReminders: Boolean
            get() = Settings.Manager.getBoolean("localAccountReminders", true)

        @JvmStatic
        val ignoreUpdate: String?
            get() = Settings.Manager.getString("ignoreUpdate", null)

        @JvmStatic
        val noticeNumbering: Int
            get() = Settings.Manager.getInt("noticeNumbering", 0)

        @JvmStatic
        val noticeDefault: Boolean
            get() = Settings.Manager.getBoolean("noticeDefault", false)

        @JvmStatic
        val buttonSnapping: Boolean
            get() = Settings.Manager.getBoolean("buttonSnapping", true)

        @JvmStatic
        val buttonSnappingDistance: Int
            get() = Settings.Manager.getInt("buttonSnappingDistance", 8)
    }
}