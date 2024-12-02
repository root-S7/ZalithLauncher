package com.movtery.zalithlauncher.setting

import com.movtery.zalithlauncher.context.ContextExecutor
import com.movtery.zalithlauncher.setting.Settings.Manager.Companion.getBoolean
import com.movtery.zalithlauncher.setting.Settings.Manager.Companion.getInt
import com.movtery.zalithlauncher.setting.Settings.Manager.Companion.getLong
import com.movtery.zalithlauncher.setting.Settings.Manager.Companion.getString
import com.movtery.zalithlauncher.utils.path.PathManager
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class AllSettings {
    companion object {
        // Video
        @JvmStatic
        val renderer = SettingUnit("renderer", "opengles2") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val ignoreNotch = SettingUnit("ignoreNotch", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val resolutionRatio = SettingUnit("resolutionRatio", 100) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val sustainedPerformance = SettingUnit("sustainedPerformance", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val alternateSurface = SettingUnit("alternate_surface", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val forceVsync = SettingUnit("force_vsync", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val vsyncInZink = SettingUnit("vsync_in_zink", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val zinkPreferSystemDriver = SettingUnit("zinkPreferSystemDriver", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        // Control
        @JvmStatic
        val disableGestures = SettingUnit("disableGestures", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val disableDoubleTap = SettingUnit("disableDoubleTap", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val timeLongPressTrigger = SettingUnit("timeLongPressTrigger", 300) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val buttonScale = SettingUnit("buttonscale", 100) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val buttonAllCaps = SettingUnit("buttonAllCaps", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val mouseScale = SettingUnit("mousescale", 100) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val mouseSpeed = SettingUnit("mousespeed", 100) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val virtualMouseStart = SettingUnit("mouse_start", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val customMouse = SettingUnit("custom_mouse", "") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val enableGyro = SettingUnit("enableGyro", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val gyroSensitivity = SettingUnit("gyroSensitivity", 100) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val gyroSampleRate = SettingUnit("gyroSampleRate", 16) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val gyroSmoothing = SettingUnit("gyroSmoothing", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val gyroInvertX = SettingUnit("gyroInvertX", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val gyroInvertY = SettingUnit("gyroInvertY", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val deadZoneScale = SettingUnit("gamepad_deadzone_scale", 100) { key, defaultValue -> getInt(key, defaultValue) }

        // Game
        @JvmStatic
        val versionIsolation = SettingUnit("versionIsolation", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val autoSetGameLanguage = SettingUnit("autoSetGameLanguage", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val gameLanguageOverridden = SettingUnit("gameLanguageOverridden", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val setGameLanguage = SettingUnit("setGameLanguage", "system") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val javaArgs = SettingUnit("javaArgs", "") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val ramAllocation = lazy {
            //涉及到Context初始化，需要进行懒加载
            SettingUnit("allocation", LauncherPreferences.findBestRAMAllocation(ContextExecutor.getApplication())) { key, defaultValue -> getInt(key, defaultValue) }
        }

        @JvmStatic
        val javaSandbox = SettingUnit("java_sandbox", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val gameMenuShowMemory = SettingUnit("gameMenuShowMemory", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val gameMenuMemoryText = SettingUnit("gameMenuMemoryText", "M:") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val gameMenuLocation = SettingUnit("gameMenuLocation", "center") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val gameMenuAlpha = SettingUnit("gameMenuAlpha", 100) { key, defaultValue -> getInt(key, defaultValue) }

        // Launcher
        @JvmStatic
        val checkLibraries = SettingUnit("checkLibraries", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val verifyManifest = SettingUnit("verifyManifest", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val resourceImageCache = SettingUnit("resourceImageCache", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val downloadSource = SettingUnit("downloadSource", "default") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val modInfoSource = SettingUnit("modInfoSource", "original") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val modDownloadSource = SettingUnit("modDownloadSource", "original") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val launcherTheme = SettingUnit("launcherTheme", "system") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val animation = SettingUnit("animation", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val animationSpeed = SettingUnit("animationSpeed", 600) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val pageOpacity = SettingUnit("pageOpacity", 100) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val enableLogOutput = SettingUnit("enableLogOutput", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val quitLauncher = SettingUnit("quitLauncher", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        // Experimental
        @JvmStatic
        val dumpShaders = SettingUnit("dump_shaders", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val bigCoreAffinity = SettingUnit("bigCoreAffinity", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        // Other
        @JvmStatic
        val currentAccount = SettingUnit("currentAccount", "") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val launcherProfile = SettingUnit("launcherProfile", "default") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val defaultCtrl = SettingUnit("defaultCtrl", PathManager.FILE_CTRLDEF_FILE) { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val defaultRuntime = SettingUnit("defaultRuntime", "") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val notificationPermissionRequest = SettingUnit("notification_permission_request", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val skipNotificationPermissionCheck = SettingUnit("skipNotificationPermissionCheck", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val localAccountReminders = SettingUnit("localAccountReminders", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val updateCheck = SettingUnit("updateCheck", 0L) { key, defaultValue -> getLong(key, defaultValue) }

        @JvmStatic
        val ignoreUpdate = SettingUnit("ignoreUpdate", "") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val noticeCheck = SettingUnit("noticeCheck", 0L) { key, defaultValue -> getLong(key, defaultValue) }

        @JvmStatic
        val noticeNumbering = SettingUnit("noticeNumbering", 0) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val noticeDefault = SettingUnit("noticeDefault", false) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val buttonSnapping = SettingUnit("buttonSnapping", true) { key, defaultValue -> getBoolean(key, defaultValue) }

        @JvmStatic
        val buttonSnappingDistance = SettingUnit("buttonSnappingDistance", 8) { key, defaultValue -> getInt(key, defaultValue) }

        @JvmStatic
        val hotbarType = SettingUnit("hotbarType", "auto") { key, defaultValue -> getString(key, defaultValue) }

        @JvmStatic
        val hotbarWidth = lazy {
            SettingUnit("hotbarWidth", Tools.currentDisplayMetrics.widthPixels / 3) { key, defaultValue -> getInt(key, defaultValue) }
        }

        @JvmStatic
        val hotbarHeight = lazy {
            SettingUnit("hotbarHeight", Tools.currentDisplayMetrics.heightPixels / 4) { key, defaultValue -> getInt(key, defaultValue) }
        }
    }
}