package com.movtery.zalithlauncher.support.touch_controller;

import android.system.Os;

import net.kdt.pojavlaunch.Logger;

import java.util.concurrent.ThreadLocalRandom;

import top.fifthlight.touchcontroller.proxy.client.LauncherSocketProxyClient;
import top.fifthlight.touchcontroller.proxy.client.LauncherSocketProxyClientKt;

/**
 * 为适配 TouchController 模组
 * <a href="">https://modrinth.com/mod/touchcontroller</a>
 */
public final class ControllerProxy {
    private static LauncherSocketProxyClient proxyClient;
    private static int proxyPort = -1;

    /**
     * 启动控制代理
     */
    public static void startProxy() throws Throwable {
        if (proxyClient == null) {
            proxyPort = ThreadLocalRandom.current().nextInt(32768) + 32768;
            proxyClient = LauncherSocketProxyClientKt.localhostLauncherSocketProxyClient(proxyPort);
            Logger.appendToLog("LauncherSocketProxy: Created on port " + proxyPort);
            new Thread(() -> {
                Logger.appendToLog("LauncherSocketProxy: Listening on port " + proxyPort);
                LauncherSocketProxyClientKt.runProxy(proxyClient);
                Logger.appendToLog("LauncherSocketProxy: Stopped");
            }).start();
        }
        if (proxyPort > 0) {
            Os.setenv("TOUCH_CONTROLLER_PROXY", String.valueOf(proxyPort), true);
        }
    }

    static LauncherSocketProxyClient getProxyClient() {
        return proxyClient;
    }
}
