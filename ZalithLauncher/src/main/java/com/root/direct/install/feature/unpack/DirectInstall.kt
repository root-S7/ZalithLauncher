package com.root.direct.install.feature.unpack

import android.app.Application
import com.movtery.zalithlauncher.feature.unpack.AbstractUnpackTask
import com.root.direct.install.utils.path.*

enum class DirectInstall(val component: String, val displayName: String, val tips: String, private val taskFactory: (Application, DirectInstall) -> AbstractUnpackTask) {
    GAME_DATA(".minecraft", "Game Data", "Minecraft 游戏文件", { ctx, comp -> UnpackGameDataTask(ctx, comp) }),
    CONFIG_FILES(PATH, "Installer Config", "直装器配置", {ctx, comp -> WriteConfigFilesTask(ctx, comp)});
    //AUTH_SERVERS("app_config/auth_servers.json", "Auth Servers", "认证服务器（皮肤站、统一同行者）", true, { ctx, comp -> UnpackAuthServersTask() });

    fun createTask(application: Application): AbstractUnpackTask {
        return taskFactory(application, this)
    }
}