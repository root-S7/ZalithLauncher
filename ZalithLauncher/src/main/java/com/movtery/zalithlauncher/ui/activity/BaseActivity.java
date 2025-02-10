package com.movtery.zalithlauncher.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.movtery.zalithlauncher.context.ContextExecutor;
import com.movtery.zalithlauncher.context.LocaleHelper;
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathManager;
import com.movtery.zalithlauncher.feature.version.VersionsManager;
import com.movtery.zalithlauncher.plugins.PluginLoader;
import com.movtery.zalithlauncher.renderer.Renderers;
import com.movtery.zalithlauncher.utils.StoragePermissionsUtils;

import net.kdt.pojavlaunch.MissingStorageActivity;
import net.kdt.pojavlaunch.Tools;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.Companion.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleHelper.Companion.setLocale(this);
        Tools.setFullscreen(this);
        Tools.updateWindowSize(this);

        checkStoragePermissions();
        //加载渲染器
        Renderers.INSTANCE.init(false);
        //加载插件
        PluginLoader.loadAllPlugins(this, false);
        //刷新游戏路径
        ProfilePathManager.INSTANCE.refreshPath();
        refreshVersions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        if (!Tools.checkStorageRoot()) {
            startActivity(new Intent(this, MissingStorageActivity.class));
            finish();
        }

        checkStoragePermissions();
        refreshVersions();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Tools.setFullscreen(this);
        Tools.ignoreNotch(shouldIgnoreNotch(),this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Tools.getDisplayMetrics(this);
    }

    /** @return Whether or not the notch should be ignored */
    public boolean shouldIgnoreNotch() {
        return true;
    }

    private void checkStoragePermissions() {
        //检查所有文件管理权限
        StoragePermissionsUtils.checkPermissions(this);
    }

    private void refreshVersions() {
        //刷新版本
        if (canRefreshVersions()) VersionsManager.INSTANCE.refresh(getVersionsRefreshTag(), false);
    }

    protected String getVersionsRefreshTag() {
        return "BaseActivity";
    }

    protected boolean canRefreshVersions() {
        return false;
    }
}
