package com.root.direct.install.utils

import android.os.Build
import com.movtery.zalithlauncher.BuildConfig

object DeviceInfo {
    val deviceData: Map<String, String> by lazy {
        mapOf(
            "Device" to "${Build.BRAND}(${Build.MODEL})",
            "Android-Version" to Build.VERSION.RELEASE,
            "Android-SDK" to Build.VERSION.SDK_INT.toString(),
            "CPU" to DIUtils.getSocName(),
            "ZHL-Version" to BuildConfig.VERSION_NAME,
            "ROM-Version" to Build.DISPLAY
        )
    }

    val deviceText: String by lazy {
        deviceData.entries.joinToString(", ") { "${it.key}=${it.value}" }
    }

    fun toText(): String = deviceText

    override fun toString(): String = deviceText
}