package com.root.direct.install.feature.unpack

import android.app.Application
import com.movtery.zalithlauncher.feature.unpack.AbstractUnpackTask
import com.root.direct.install.utils.path.*

enum class DirectInstall(val component: String, val displayName: String, val tips: String, private val taskFactory: (Application, DirectInstall) -> AbstractUnpackTask) {
    GAME_DATA(".minecraft", "Game Data", "Minecraft 游戏文件", { ctx, comp -> UnpackGameDataTask(ctx, comp) }),
    CONFIG_FILES(CONFIG_VERSION, "Installer Config", "直装器配置", {ctx, comp -> WriteConfigFilesTask(ctx, comp)}),
    AUTH_SERVERS(AUTH_SERVER, "Auth Servers", "认证服务器（皮肤站、统一通行证）", { ctx, comp -> UnpackAuthServersTask(ctx, comp) });

    fun createTask(application: Application): AbstractUnpackTask {
        return taskFactory(application, this)
    }
}